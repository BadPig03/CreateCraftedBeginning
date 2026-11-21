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
import java.util.Collections;
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
    private final List<Pass> devices;
    private final Set<Link> corridors;
    private final Map<BlockPos, Pass> stops;
    private final Set<BlockPos> pumps;
    private final long capacity;
    final List<DevicePort> devicePorts;

    GasZoneRoute(Level level, List<Pass> passes, long capacity) {
        this.capacity = capacity;
        Builder builder = new Builder(passes.size());
        List<Pass> devices = new ArrayList<>();
        Set<BlockPos> pumps = null;
        for (Pass pass : passes) {
            builder.add(pass);
            if (!pass.vertex.device) {
                continue;
            }

            devices.add(pass);
            GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pass.position());
            if (behaviour == null || !behaviour.passesNaturalForwardFlow()) {
                continue;
            }

            if (pumps == null) {
                pumps = new HashSet<>();
            }
            pumps.add(pass.position());
        }
        this.passes = Collections.unmodifiableList(builder.passes);
        stops = builder.stops;
        corridors = builder.corridors;
        this.devices = devices;
        devicePorts = devices.isEmpty() ? List.of() : devices.stream().map(pass -> new DevicePort(pass.position(), pass.entry, pass.exit)).toList();
        this.pumps = pumps == null ? Set.of() : pumps;
    }

    private GasZoneRoute(@Nullable Builder builder, GasZoneRoute original, long capacity) {
        this.capacity = capacity;
        passes = builder == null ? original.passes : Collections.unmodifiableList(builder.passes);
        stops = builder == null ? original.stops : builder.stops;
        corridors = builder == null ? original.corridors : builder.corridors;
        devices = original.devices;
        devicePorts = original.devicePorts;
        pumps = original.pumps;
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

    public boolean samePath(GasZoneRoute other) {
        return passes.size() == other.passes.size() && commonPrefix(other) == passes.size();
    }

    @Nullable
    public ParallelSpan parallelSpan(GasZoneRoute other) {
        int split = commonPrefix(other);
        if (split >= Math.min(passes.size(), other.passes.size()) - 1) {
            return null;
        }

        Pass start = passes.get(split);
        Pass otherStart = other.passes.get(split);
        if (start.vertex != otherStart.vertex || start.entry != otherStart.entry || start.vertex.device) {
            return null;
        }

        int join = passes.size() - 1;
        int otherJoin = other.passes.size() - 1;
        while (join > split && otherJoin > split) {
            Pass end = passes.get(join);
            Pass otherEnd = other.passes.get(otherJoin);
            if (end.vertex != otherEnd.vertex || end.next != otherEnd.next || end.exit != otherEnd.exit) {
                return null;
            }

            if (end.entry != otherEnd.entry) {
                if (end.vertex.device || !unrestrictedArm(split, join) || !other.unrestrictedArm(split, otherJoin)) {
                    return null;
                }

                return new ParallelSpan(start.vertex.pos, end.vertex.pos);
            }

            join--;
            otherJoin--;
        }

        return null;
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

    public Quote quotePassiveMesh(Level level, GasStack gas, GasNetworkPressureEndpoint source, GasNetworkPressureEndpoint target, long maximum, Usage usage, @Nullable Sharing sharing) {
        return quote(level, gas, source, target, maximum, usage, sharing, false, true);
    }

    GasZoneRoute branch(@Nullable Builder geometry, long limit) {
        if (geometry == null && limit == capacity) {
            return this;
        }

        return new GasZoneRoute(geometry, this, limit);
    }

    private Quote quote(Level level, GasStack gas, GasNetworkPressureEndpoint source, GasNetworkPressureEndpoint target, long maximum, @Nullable Usage usage, @Nullable Sharing sharing, boolean assist) {
        return quote(level, gas, source, target, maximum, usage, sharing, assist, false);
    }

    private Quote quote(Level level, GasStack gas, GasNetworkPressureEndpoint source, GasNetworkPressureEndpoint target, long maximum, @Nullable Usage usage, @Nullable Sharing sharing, boolean assist, boolean mesh) {
        if (assist && pumps.isEmpty()) {
            return Quote.CLOSED;
        }

        long alreadyDrained = 0;
        long alreadyFilled = 0;
        if (usage != null) {
            alreadyDrained = usage.drained(source);
            alreadyFilled = usage.filled(target);
        }
        if (usage != null && !mesh) {
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

        long limit = Math.min(mesh ? maximum : Math.min(capacity, maximum), Math.min(source.transferLimits().drainLimit(), target.transferLimits().fillLimit()));
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

    private int commonPrefix(GasZoneRoute other) {
        int count = Math.min(passes.size(), other.passes.size());
        for (int index = 0; index < count; index++) {
            Pass pass = passes.get(index);
            Pass alternate = other.passes.get(index);
            if (pass.vertex != alternate.vertex || pass.entry != alternate.entry || pass.exit != alternate.exit || pass.next != alternate.next) {
                return index;
            }
        }

        return count;
    }

    private boolean unrestrictedArm(int split, int join) {
        for (int index = split; index < join; index++) {
            Pass pass = passes.get(index);
            Link link = pass.next;
            if (link == null || link.capacity != Long.MAX_VALUE) {
                return false;
            }

            if (index == split) {
                continue;
            }

            Vertex vertex = pass.vertex;
            if (vertex.device || Integer.bitCount(vertex.links.neighbors()) != 2 || (vertex.links.endpoints() | vertex.links.atmosphere()) != 0) {
                return false;
            }
        }

        return true;
    }

    record DevicePort(BlockPos pos, Direction entry, Direction exit) {}

    static final class Builder {
        private final List<Pass> passes;
        private final Map<BlockPos, Pass> stops;
        private final Set<Link> corridors;

        Builder(int size) {
            passes = new ArrayList<>(size);
            stops = HashMap.newHashMap(size);
            corridors = HashSet.newHashSet(size);
        }

        void add(Pass pass) {
            passes.add(pass);
            stops.put(pass.position(), pass);
            if (pass.next == null) {
                return;
            }

            corridors.add(pass.next);
        }
    }

    public record ParallelSpan(BlockPos split, BlockPos join) {}

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
        final Vertex vertex;
        final Direction entry;
        final Direction exit;
        final @Nullable Link next;

        Pass(Vertex vertex, Direction entry, Direction exit, @Nullable Link next) {
            this.vertex = vertex;
            this.entry = entry;
            this.exit = exit;
            this.next = next;
        }

        public BlockPos position() {
            return vertex.pos;
        }

        long vertexCapacity() {
            return vertex.capacity;
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
