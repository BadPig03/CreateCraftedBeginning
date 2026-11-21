package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.GasTransportEdgeProperties;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Link;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Vertex;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute.Pass;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasZoneRouteSearch implements GasRouteSearch {
    private final Level level;
    private final GasZoneLayout layout;
    private final BlockFace destination;
    private final GasStack gas;
    private final GasNetworkPressureEndpoint supply;
    private final GasNetworkPressureEndpoint demand;
    private final ArrayDeque<Frame> stack = new ArrayDeque<>();
    private final Set<Vertex> visited = new HashSet<>();
    private int driveProbe;

    GasZoneRouteSearch(Level level, GasZoneLayout layout, BlockFace from, BlockFace to, GasStack gas, GasNetworkPressureEndpoint supply, GasNetworkPressureEndpoint demand) {
        this.level = level;
        this.layout = layout;
        destination = to;
        this.gas = gas;
        this.supply = supply;
        this.demand = demand;
        Vertex start = layout.vertex(from.getPos());
        if (start == null || layout.vertex(to.getPos()) == null || layout.complete() && layout.deviceCount() == 0 && layout.node(from) != layout.node(to)) {
            return;
        }

        driveProbe = !layout.complete() || GasPassiveQuantum.limit(supply.pressureState(), demand.pressureState(), 1) > 0 ? -1 : 0;
        layout.connect(start);
        stack.add(new Frame(start, from.getFace(), start.capacity));
        visited.add(start);
    }

    @Override
    public boolean pending() {
        return !stack.isEmpty();
    }

    @Override
    public @Nullable Path expand() {
        if (driveProbe >= 0) {
            if (driveProbe == layout.deviceCount()) {
                stack.clear();
            }
            else if (canLift(layout.devicePosition(driveProbe++))) {
                driveProbe = -1;
            }
            return null;
        }

        Frame frame = stack.getLast();
        if (frame.vertex.pos.equals(destination.getPos())) {
            List<Pass> passes = new ArrayList<>(stack.size());
            for (Frame part : stack) {
                Link link = part == frame ? null : part.links.get(part.cursor - 1);
                passes.add(new Pass(part.vertex, part.entry, link == null ? destination.getFace() : link.exit, link));
            }
            GasZoneRoute route = new GasZoneRoute(level, passes, frame.capacity);
            stack.removeLast();
            visited.remove(frame.vertex);
            if (route.quote(level, gas, supply, demand, 1).amount() == 0) {
                return null;
            }

            return new Path(frame.vertex.pos, frame.capacity, null, route);
        }

        if (frame.cursor == frame.links.size()) {
            visited.remove(frame.vertex);
            stack.removeLast();
            return null;
        }

        Link link = frame.links.get(frame.cursor++);
        if (link.exit != frame.entry && !visited.contains(link.end) && allowsStep(frame, link.exit)) {
            visited.add(link.end);
            layout.connect(link.end);
            stack.addLast(new Frame(link.end, link.entry, Math.min(frame.capacity, link.capacity)));
        }
        return null;
    }

    private boolean canLift(BlockPos pos) {
        GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pos);
        if (behaviour == null) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        for (Direction in : Iterate.directions) {
            if (layout.node(new BlockFace(pos, in)) < 0 || !behaviour.acceptsInboundGas(state, in, gas)) {
                continue;
            }

            for (Direction out : Iterate.directions) {
                if (in == out || layout.node(new BlockFace(pos, out)) < 0 || !behaviour.allowsOutboundGas(state, out, gas)) {
                    continue;
                }

                GasTransportEdgeProperties edge = behaviour.getTransportEdgeProperties(state, in, out, gas);
                if (edge.flowRateLimit() > 0 && edge.pressureDrive().drivenOutletPressurePa(0) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean allowsStep(Frame frame, Direction exit) {
        if (!frame.vertex.device) {
            return true;
        }

        GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, frame.vertex.pos);
        if (behaviour == null) {
            return false;
        }

        BlockState state = level.getBlockState(frame.vertex.pos);
        return behaviour.acceptsInboundGas(state, frame.entry, gas) && behaviour.allowsOutboundGas(state, exit, gas) && behaviour.getTransportEdgeProperties(state, frame.entry, exit, gas).flowRateLimit() > 0;
    }

    private static final class Frame {
        private final Vertex vertex;
        private final Direction entry;
        private final long capacity;
        private final List<Link> links;
        private int cursor;

        private Frame(Vertex vertex, Direction entry, long capacity) {
            this.vertex = vertex;
            this.entry = entry;
            this.capacity = capacity;
            links = new ArrayList<>(vertex.edges.values());
            links.sort(Comparator.comparingLong(link -> link.turns));
        }
    }
}
