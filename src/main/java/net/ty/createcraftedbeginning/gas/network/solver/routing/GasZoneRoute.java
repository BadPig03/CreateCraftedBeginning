package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.GasFlowResistance;
import net.ty.createcraftedbeginning.gas.network.GasTransportEdgeProperties;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive.OutletPressureTarget;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive.PressureBoost;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.PressureState;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Interior;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Link;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Vertex;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasZoneRoute {
    private final List<Pass> passes;
    private final List<Pass> devices = new ArrayList<>();
    private final Set<Link> corridors = new HashSet<>();
    private final Map<BlockPos, Pass> stops = new HashMap<>();
    private final Set<BlockPos> pumps;
    private final long capacity;

    GasZoneRoute(Level level, List<Pass> passes, long capacity) {
        this.capacity = capacity;
        this.passes = List.copyOf(passes);
        Set<BlockPos> pumpPositions = null;
        for (Pass pass : passes) {
            stops.put(pass.vertex.pos, pass);
            if (pass.vertex.device) {
                devices.add(pass);
                GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pass.vertex.pos);
                if (behaviour != null && behaviour.passesNaturalForwardFlow()) {
                    if (pumpPositions == null) {
                        pumpPositions = new HashSet<>();
                    }
                    pumpPositions.add(pass.vertex.pos);
                }
            }
            if (pass.next == null) {
                continue;
            }

            corridors.add(pass.next);
        }
        pumps = pumpPositions == null ? Set.of() : Set.copyOf(pumpPositions);
    }

    private static double downstream(List<GasTransportEdgeProperties> edges, double pressure, long amount, double sourceResistance, boolean losses, long @Nullable [] priorFlow, long alreadyDrained, int sourceCount, int @Nullable [] counts) {
        pressure -= (sharedFlow(amount, sourceCount) + alreadyDrained) * sourceResistance;
        for (int index = 0; index < edges.size(); index++) {
            GasTransportEdgeProperties edge = edges.get(index);
            double flow = sharedFlow(amount, counts == null ? 1 : counts[index]) + (priorFlow == null ? 0.0 : (double) priorFlow[index]);
            if (pressure < 0 || edge.flowLimitReferencePressurePa() > 0 && flow > edge.flowRateLimit() * (pressure / edge.flowLimitReferencePressurePa())) {
                return -1;
            }

            pressure = edge.pressureDrive().drivenOutletPressurePa(pressure);
            if (!losses) {
                continue;
            }

            double conductance = edge.conductancePerPascal();
            if (!Double.isFinite(conductance)) {
                long resistanceUnits = edge.resistanceUnits();
                if (resistanceUnits == 0) {
                    continue;
                }

                conductance = GasFlowResistance.conductancePerPascal(resistanceUnits);
            }
            pressure -= flow / conductance;
        }
        return pressure;
    }

    private static double sharedFlow(long amount, int count) {
        if (amount <= 1 || count == 1) {
            return amount;
        }

        return (double) (amount - 1) * count + 1;
    }

    private static int direction(Direction side, Direction entry, Direction exit) {
        if (side == entry) {
            return -1;
        }

        if (side == exit) {
            return 1;
        }

        return 0;
    }

    public List<Pass> passes() {
        return passes;
    }

    public boolean hasPumpAt(BlockPos pos) {
        return pumps.contains(pos);
    }

    public int deviceCount() {
        return devices.size();
    }

    public Pass device(int index) {
        return devices.get(index);
    }

    public void served() {
        for (Link link : corridors) {
            link.turns++;
        }
    }

    public int direction(GasZoneLayout layout, BlockFace face) {
        BlockPos pos = face.getPos();
        Pass pass = stops.get(pos);
        if (pass != null) {
            return direction(face.getFace(), pass.entry, pass.exit);
        }

        Interior span = layout.interior(pos);
        if (span == null) {
            return 0;
        }

        int sign = direction(face.getFace(), span.entry(), span.exit());
        if (corridors.contains(span.forward())) {
            return sign;
        }

        if (corridors.contains(span.reverse())) {
            return -sign;
        }

        return 0;
    }

    public Quote quote(Level level, GasStack gas, GasNetworkPressureEndpoint source, GasNetworkPressureEndpoint target, long maximum) {
        return quote(level, gas, source, target, maximum, null, null, PumpUse.BEST);
    }

    public Quote quote(Level level, GasStack gas, GasNetworkPressureEndpoint source, GasNetworkPressureEndpoint target, long maximum, PumpUse pumpUse) {
        return quote(level, gas, source, target, maximum, null, null, pumpUse);
    }

    public Quote quote(Level level, GasStack gas, GasNetworkPressureEndpoint source, GasNetworkPressureEndpoint target, long maximum, @Nullable Usage usage, @Nullable Sharing sharing, PumpUse pumpUse) {
        if (pumpUse != PumpUse.BEST || pumps.isEmpty()) {
            return quote(level, gas, source, target, maximum, usage, sharing, pumpUse == PumpUse.ASSIST);
        }

        Quote natural = quote(level, gas, source, target, maximum, usage, sharing, false);
        if (natural.amount() >= Math.min(maximum, natural.assistRoom())) {
            return natural;
        }

        Quote assist = quote(level, gas, source, target, maximum, usage, sharing, true);
        if (assist.amount() <= natural.amount()) {
            return natural;
        }

        return assist;
    }

    private Quote quote(Level level, GasStack gas, GasNetworkPressureEndpoint source, GasNetworkPressureEndpoint target, long maximum, @Nullable Usage usage, @Nullable Sharing sharing, boolean assist) {
        if (assist && pumps.isEmpty()) {
            return Quote.CLOSED;
        }

        long alreadyDrained = 0;
        long alreadyFilled = 0;
        if (usage != null) {
            alreadyDrained = usage.drained(source);
            alreadyFilled = usage.filled(target);
            for (Pass pass : passes) {
                long room = pass.vertex.capacity - usage.used(pass.vertex);
                maximum = Math.min(maximum, sharing == null ? room : Math.ceilDiv(room, sharing.count(pass.vertex)));
                Link link = pass.next;
                if (link == null) {
                    continue;
                }

                long linkRoom = link.capacity - usage.used(link.shared);
                maximum = Math.min(maximum, sharing == null ? linkRoom : Math.ceilDiv(linkRoom, sharing.count(link.shared)));
            }
        }
        List<GasTransportEdgeProperties> edges = new ArrayList<>(devices.size());
        long[] priorFlow = usage == null || devices.isEmpty() ? null : new long[devices.size()];
        int[] counts = sharing == null || devices.isEmpty() ? null : new int[devices.size()];
        int sourceCount = sharing == null ? 1 : sharing.count(source.transferIdentity());
        long assistRoom = pumps.isEmpty() ? 0 : Long.MAX_VALUE;
        long availableBoostPa = 0;
        boolean otherDrive = false;
        boolean driven = false;
        for (Pass pass : devices) {
            GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pass.vertex.pos);
            if (behaviour == null) {
                return Quote.CLOSED;
            }

            BlockState state = level.getBlockState(pass.vertex.pos);
            if (!behaviour.acceptsInboundGas(state, pass.entry, gas) || pass.entry != pass.exit && !behaviour.allowsOutboundGas(state, pass.exit, gas)) {
                return Quote.CLOSED;
            }

            if (pass.entry == pass.exit) {
                if (behaviour.getInternalPressureBoundary(pass.exit) != target.access().fillHandler()) {
                    return Quote.CLOSED;
                }

                continue;
            }

            GasTransportEdgeProperties edge = behaviour.getTransportEdgeProperties(state, pass.entry, pass.exit, gas);
            if (edge.flowRateLimit() <= 0) {
                return Quote.CLOSED;
            }

            boolean pump = hasPumpAt(pass.vertex.pos);
            long carried = usage == null ? 0 : usage.used(pass.vertex);
            if (pump) {
                assistRoom = Math.min(assistRoom, edge.flowRateLimit() - carried);
                if (edge.pressureDrive() instanceof PressureBoost(long pa)) {
                    availableBoostPa += pa;
                }
                if (!assist) {
                    continue;
                }
            }
            if (edge.isLossless()) {
                continue;
            }

            if (priorFlow != null) {
                priorFlow[edges.size()] = carried;
            }
            int count = 1;
            if (counts != null) {
                count = sharing.count(pass.vertex);
                counts[edges.size()] = count;
            }
            maximum = Math.min(maximum, Math.ceilDiv(edge.flowRateLimit() - carried, count));
            edges.add(edge);
            boolean providesDrive = edge.pressureDrive() instanceof OutletPressureTarget || edge.pressureDrive() instanceof PressureBoost(long pa) && pa > 0;
            driven |= providesDrive;
            otherDrive |= !pump && providesDrive;
        }

        PressureState supply = source.pressureState();
        PressureState demand = target.pressureState();
        assistRoom = availableBoostPa > 0 && (otherDrive || supply.pressurePa() + availableBoostPa > demand.pressurePa()) ? Math.max(0, assistRoom) : 0;
        if (assist && assistRoom == 0) {
            return Quote.CLOSED;
        }

        long limit = Math.min(capacity, Math.min(maximum, Math.min(source.transferLimits().drainLimit(), target.transferLimits().fillLimit())));
        if (!driven) {
            limit = GasPassiveQuantum.limit(supply, demand, limit);
        }
        if (limit <= 0) {
            return Quote.withoutFlow(assistRoom);
        }

        double inlet = supply.pressurePa();
        double outlet = demand.pressurePa();
        double sourceResistance = GasFlowResistance.endpointResistance(source.flowResistance().drainResistanceUnits());
        double targetResistance = GasFlowResistance.endpointResistance(target.flowResistance().fillResistanceUnits());
        long lower = 0;
        long upper = limit;
        while (lower < upper) {
            long delta = upper - lower;
            long amount = lower + delta / 2 + delta % 2;
            double delivered = downstream(edges, inlet, amount, sourceResistance, true, priorFlow, alreadyDrained, sourceCount, counts);
            boolean permitted = delivered >= outlet + (amount + (double) alreadyFilled) * targetResistance;
            if (amount == 1 && !permitted && alreadyDrained == 0 && alreadyFilled == 0) {
                double head = downstream(edges, inlet, 1, 0, false, priorFlow, 0, sourceCount, counts);
                permitted = head > outlet || !driven && head == outlet && GasPassiveQuantum.limit(supply, demand, 1) > 0;
            }
            if (permitted && driven) {
                double afterSource = supply.fixedPressure() ? inlet : GasPressure.pressureExact(supply.currentAmount() - amount, supply.volume());
                double afterTarget = demand.fixedPressure() ? outlet : GasPressure.pressureExact(demand.currentAmount() + amount, demand.volume());
                double ceiling = downstream(edges, afterSource, 0, 0, false, priorFlow, 0, sourceCount, counts);
                permitted = afterTarget <= ceiling && downstream(edges, inlet, 0, 0, false, priorFlow, 0, sourceCount, counts) > outlet;
            }
            if (permitted) {
                lower = amount;
            }
            else {
                upper = amount - 1;
            }
        }
        if (lower == 0) {
            return Quote.withoutFlow(assistRoom);
        }

        double pressure = Math.max(outlet, downstream(edges, inlet, lower, sourceResistance, true, priorFlow, alreadyDrained, 1, null));
        if (assist && !otherDrive) {
            double required = outlet + (lower + (double) alreadyFilled) * targetResistance;
            if (!demand.fixedPressure()) {
                required = Math.max(required, GasPressure.pressureExact(demand.currentAmount() + lower, demand.volume()));
            }

            pressure = Math.min(pressure, required);
        }
        return new Quote(lower, GasPressure.round(GasPressureLimits.clampToHardLimit(pressure)), assistRoom, assist);
    }

    public enum PumpUse {
        NATURAL, ASSIST, BEST
    }

    public interface Usage {
        long used(Object segment);

        long drained(GasNetworkPressureEndpoint endpoint);

        long filled(GasNetworkPressureEndpoint endpoint);
    }

    @FunctionalInterface
    public interface Sharing {
        int count(Object segment);
    }

    public static final class Pass {
        private final Vertex vertex;
        private final Direction entry;
        private final Direction exit;
        private final @Nullable Link next;

        Pass(Vertex vertex, Direction entry, Direction exit, @Nullable Link next) {
            this.vertex = vertex;
            this.entry = entry;
            this.exit = exit;
            this.next = next;
        }

        public BlockPos position() {
            return vertex.pos;
        }

        public Object vertexKey() {
            return vertex;
        }

        @Nullable
        public Object corridorKey() {
            if (next == null) {
                return null;
            }

            return next.shared;
        }
    }

    public record Quote(long amount, long pressure, long assistRoom, boolean assisted) {
        static final Quote CLOSED = new Quote(0, 0, 0, false);

        private static Quote withoutFlow(long assistRoom) {
            if (assistRoom == 0) {
                return CLOSED;
            }

            return new Quote(0, 0, assistRoom, false);
        }
    }
}
