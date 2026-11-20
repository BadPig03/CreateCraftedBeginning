package net.ty.createcraftedbeginning.gas.network.solver;

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
import net.ty.createcraftedbeginning.gas.network.solver.GasZoneLayout.Interior;
import net.ty.createcraftedbeginning.gas.network.solver.GasZoneLayout.Link;
import net.ty.createcraftedbeginning.gas.network.solver.GasZoneLayout.Vertex;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.PressureState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasZoneRoute {
    private final List<Pass> passes;
    private final List<Pass> devices = new ArrayList<>();
    private final Set<Link> corridors = new HashSet<>();
    private final Map<BlockPos, Pass> stops = new HashMap<>();
    private final long capacity;

    GasZoneRoute(List<Pass> passes, long capacity) {
        this.capacity = capacity;
        this.passes = List.copyOf(passes);
        for (Pass pass : passes) {
            stops.put(pass.vertex.pos, pass);
            if (pass.vertex.device) {
                devices.add(pass);
            }
            if (pass.next == null) {
                continue;
            }

            corridors.add(pass.next);
        }
    }

    static double contact(long extraResistance) {
        double segments = 0.02 + (double) extraResistance / GasFlowResistance.UNITS_PER_STANDARD_SEGMENT;
        return segments / GasFlowResistance.conductancePerPascal(GasFlowResistance.UNITS_PER_STANDARD_SEGMENT);
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

            double conductance = Double.isFinite(edge.conductancePerPascal()) ? edge.conductancePerPascal() : GasFlowResistance.conductancePerPascal(edge.resistanceUnits());
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

    List<Pass> passes() {
        return passes;
    }

    List<Pass> devices() {
        return devices;
    }

    long capacity() {
        return capacity;
    }

    void served() {
        for (Link link : corridors) {
            link.turns++;
        }
    }

    int direction(GasZoneLayout layout, BlockFace face) {
        Pass pass = stops.get(face.getPos());
        if (pass != null) {
            return direction(face.getFace(), pass.entry, pass.exit);
        }

        Interior span = layout.interior(face.getPos());
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

    Quote quote(Level level, GasStack gas, GasNetworkPressureEndpoint source, GasNetworkPressureEndpoint target, long maximum) {
        return quote(level, gas, source, target, maximum, null);
    }

    Quote quote(Level level, GasStack gas, GasNetworkPressureEndpoint source, GasNetworkPressureEndpoint target, long maximum, @Nullable GasRouteBatch batch) {
        return quote(level, gas, source, target, maximum, batch, null);
    }

    Quote quote(Level level, GasStack gas, GasNetworkPressureEndpoint source, GasNetworkPressureEndpoint target, long maximum, @Nullable GasRouteBatch batch, @Nullable GasBranchRound sharing) {
        long alreadyDrained = 0;
        long alreadyFilled = 0;
        if (batch != null) {
            alreadyDrained = batch.drained(source);
            alreadyFilled = batch.filled(target);
            for (Pass pass : passes) {
                long room = pass.vertex.capacity - batch.used(pass.vertex);
                maximum = Math.min(maximum, sharing == null ? room : Math.ceilDiv(room, sharing.count(pass.vertex)));
                if (pass.next != null) {
                    long linkRoom = pass.next.capacity - batch.used(pass.next.shared);
                    maximum = Math.min(maximum, sharing == null ? linkRoom : Math.ceilDiv(linkRoom, sharing.count(pass.next.shared)));
                }
            }
        }
        List<GasTransportEdgeProperties> edges = new ArrayList<>(devices.size());
        long[] priorFlow = batch == null || devices.isEmpty() ? null : new long[devices.size()];
        int[] counts = sharing == null || devices.isEmpty() ? null : new int[devices.size()];
        int sourceCount = sharing == null ? 1 : sharing.count(source.transferIdentity());
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
            if (edge.isLossless()) {
                continue;
            }

            long carried = 0;
            if (priorFlow != null) {
                carried = batch.used(pass.vertex);
                priorFlow[edges.size()] = carried;
            }
            int count = 1;
            if (counts != null) {
                count = sharing.count(pass.vertex);
                counts[edges.size()] = count;
            }
            maximum = Math.min(maximum, Math.ceilDiv(edge.flowRateLimit() - carried, count));
            edges.add(edge);
            driven |= edge.pressureDrive() instanceof OutletPressureTarget || edge.pressureDrive() instanceof PressureBoost boost && boost.pressureBoostPa() > 0;
        }

        PressureState supply = source.pressureState();
        PressureState demand = target.pressureState();
        long limit = Math.min(capacity, Math.min(maximum, Math.min(source.transferLimits().drainLimit(), target.transferLimits().fillLimit())));
        if (!driven) {
            limit = GasPassiveQuantum.limit(supply, demand, limit);
        }
        if (limit <= 0) {
            return Quote.CLOSED;
        }
        double inlet = supply.pressurePa();
        double outlet = demand.pressurePa();
        double sourceResistance = contact(source.flowResistance().drainResistanceUnits());
        double targetResistance = contact(target.flowResistance().fillResistanceUnits());
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
            return Quote.CLOSED;
        }
        double pressure = Math.max(outlet, downstream(edges, inlet, lower, sourceResistance, true, priorFlow, alreadyDrained, 1, null));
        return new Quote(lower, GasPressure.round(GasPressureLimits.clampToHardLimit(pressure)));
    }

    record Pass(Vertex vertex, Direction entry, Direction exit, @Nullable Link next) {}

    record Quote(long amount, long pressure) {
        static final Quote CLOSED = new Quote(0, 0);
    }
}
