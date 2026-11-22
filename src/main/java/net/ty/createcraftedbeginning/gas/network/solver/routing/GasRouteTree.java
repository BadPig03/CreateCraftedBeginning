package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Link;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Vertex;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute.Pass;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRouteTree implements GasRouteSearch {
    private final Level level;
    private final GasZoneLayout layout;
    private final BlockFace root;
    private final Map<Vertex, Hop> reached = new HashMap<>();
    private final ArrayDeque<Hop> frontier = new ArrayDeque<>();

    private BlockFace origin;
    private BlockFace destination;
    private @Nullable Vertex supplier;
    private @Nullable Vertex target;
    private GasStack gas = GasStack.EMPTY;
    private GasNetworkPressureEndpoint supply;
    private GasNetworkPressureEndpoint demand;
    private boolean alternative;
    private int detourSteps;
    private @Nullable GasZoneRouteSearch detour;

    public GasRouteTree(Level level, GasZoneLayout layout, BlockFace root) {
        this.level = level;
        this.layout = layout;
        this.root = root;
        Vertex start = layout.vertex(root.getPos());
        if (start == null) {
            return;
        }

        Hop first = new RootHop(start);
        reached.put(start, first);
        frontier.addLast(first);
    }

    @Override
    public boolean pending() {
        return target != null || alternative || detour != null && detour.pending();
    }

    @Override
    public @Nullable Path expand() {
        if (target != null) {
            if (supplier != null && (needsStep(supplier) || needsStep(target))) {
                step();
                return null;
            }

            Hop start = reached.get(supplier);
            Hop end = reached.get(target);
            target = null;
            if (start == null || end == null) {
                return null;
            }

            alternative = true;
            return route(start, end);
        }

        if (alternative) {
            detour = new GasZoneRouteSearch(level, layout, origin, destination, gas, supply, demand);
            alternative = false;
        }
        if (detour != null && detour.pending()) {
            detourSteps++;
            return detour.expand();
        }

        return null;
    }

    public boolean rootedAt(BlockFace face) {
        return root.equals(face);
    }

    public void seek(BlockFace destination, GasStack gas, GasNetworkPressureEndpoint supply, GasNetworkPressureEndpoint demand) {
        seek(root, destination, gas, supply, demand);
    }

    public void seek(BlockFace origin, BlockFace destination, GasStack gas, GasNetworkPressureEndpoint supply, GasNetworkPressureEndpoint demand) {
        this.origin = origin;
        supplier = layout.vertex(origin.getPos());
        this.destination = destination;
        this.gas = gas;
        this.supply = supply;
        this.demand = demand;
        target = layout.vertex(destination.getPos());
        alternative = false;
        detourSteps = 0;
        detour = null;
    }

    public boolean hasLongDetour(int limit) {
        return detourSteps >= limit && detour != null && detour.pending();
    }

    private boolean needsStep(Vertex target) {
        return !reached.containsKey(target) && !frontier.isEmpty();
    }

    private void step() {
        Hop here = frontier.removeFirst();
        Vertex vertex = here.vertex();
        layout.connect(vertex);
        for (Link link : vertex.edges.values()) {
            if (here instanceof RootHop && link.exit == root.getFace() || reached.containsKey(link.end)) {
                continue;
            }

            Hop next = new ChildHop(here, link, here.depth() + 1);
            reached.put(link.end, next);
            frontier.addLast(next);
        }
    }

    @Nullable
    private Path route(Hop start, Hop end) {
        List<Pass> tail = new ArrayList<>();
        Link next = null;
        Hop left = start;
        Hop right = end;
        List<Pass> passes = new ArrayList<>();
        Direction entry = origin.getFace();
        long capacity = start.vertex().capacity;
        while (left != right) {
            if (left instanceof ChildHop child && child.depth > right.depth()) {
                Link up = child.incoming.opposite;
                passes.add(new Pass(child.vertex(), entry, up.exit, up));
                entry = up.entry;
                capacity = Math.min(capacity, up.capacity);
                left = child.parent;
                continue;
            }

            if (!(right instanceof ChildHop child)) {
                throw new IllegalStateException("Route tree did not reach a common ancestor for " + origin + " and " + destination + '.');
            }

            Link incoming = child.incoming;
            tail.add(new Pass(child.vertex(), incoming.entry, next == null ? destination.getFace() : next.exit, next));
            next = incoming;
            capacity = Math.min(capacity, incoming.capacity);
            right = child.parent;
        }
        passes.add(new Pass(left.vertex(), entry, next == null ? destination.getFace() : next.exit, next));
        Collections.reverse(tail);
        passes.addAll(tail);
        if (OutletSideRelief.crossesInlet(demand, passes.getLast().entry, destination.getFace())) {
            return null;
        }

        return new Path(end.vertex().pos, capacity, null, new GasZoneRoute(level, passes, capacity));
    }

    private sealed interface Hop permits RootHop, ChildHop {
        Vertex vertex();

        int depth();
    }

    private record RootHop(Vertex vertex) implements Hop {
        @Override
        public int depth() {
            return 0;
        }
    }

    private record ChildHop(Hop parent, Link incoming, int depth) implements Hop {
        @Override
        public Vertex vertex() {
            return incoming.end;
        }
    }
}
