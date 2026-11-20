package net.ty.createcraftedbeginning.gas.network.solver;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.solver.GasOrdinaryPathSearch.Path;
import net.ty.createcraftedbeginning.gas.network.solver.GasZoneLayout.Link;
import net.ty.createcraftedbeginning.gas.network.solver.GasZoneLayout.Vertex;
import net.ty.createcraftedbeginning.gas.network.solver.GasZoneRoute.Pass;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasRouteTree implements GasRouteSearch {
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

    GasRouteTree(Level level, GasZoneLayout layout, BlockFace root) {
        this.level = level;
        this.layout = layout;
        this.root = root;
        Vertex start = layout.vertex(root.getPos());
        if (start != null) {
            Hop first = new Hop(start, null, null, 0);
            reached.put(start, first);
            frontier.addLast(first);
        }
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
            Path path = route();
            target = null;
            alternative = path != null;
            return path;
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

    boolean rootedAt(BlockFace face) {
        return root.equals(face);
    }

    void seek(BlockFace destination, GasStack gas, GasNetworkPressureEndpoint supply, GasNetworkPressureEndpoint demand) {
        seek(root, destination, gas, supply, demand);
    }

    void seek(BlockFace origin, BlockFace destination, GasStack gas, GasNetworkPressureEndpoint supply, GasNetworkPressureEndpoint demand) {
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

    boolean hasLongDetour(int limit) {
        return detourSteps >= limit && detour != null && detour.pending();
    }

    private boolean needsStep(Vertex target) {
        return !reached.containsKey(target) && !frontier.isEmpty();
    }

    private void step() {
        Hop here = frontier.removeFirst();
        layout.connect(here.vertex);
        for (Link link : here.vertex.edges.values()) {
            if (here.parent == null && link.exit == root.getFace() || reached.containsKey(link.end)) {
                continue;
            }

            Hop next = new Hop(link.end, here, link, here.depth + 1);
            reached.put(link.end, next);
            frontier.addLast(next);
        }
    }

    @Nullable
    private Path route() {
        Hop start = reached.get(supplier);
        Hop end = reached.get(target);
        if (start == null || end == null) {
            return null;
        }
        List<Pass> tail = new ArrayList<>();
        Link next = null;
        Hop left = start;
        Hop right = end;
        List<Pass> passes = new ArrayList<>();
        Direction entry = origin.getFace();
        long capacity = start.vertex.capacity;
        while (left != right) {
            if (left.depth > right.depth) {
                Link up = left.vertex.edges.get(left.incoming.entry);
                passes.add(new Pass(left.vertex, entry, up.exit, up));
                entry = up.entry;
                capacity = Math.min(capacity, up.capacity);
                left = left.parent;
                continue;
            }

            tail.add(new Pass(right.vertex, right.incoming.entry, next == null ? destination.getFace() : next.exit, next));
            next = right.incoming;
            capacity = Math.min(capacity, next.capacity);
            right = right.parent;
        }
        passes.add(new Pass(left.vertex, entry, next == null ? destination.getFace() : next.exit, next));
        Collections.reverse(tail);
        passes.addAll(tail);
        return new Path(end.vertex.pos, capacity, null, new GasZoneRoute(passes, capacity));
    }

    private record Hop(Vertex vertex, @Nullable Hop parent, @Nullable Link incoming, int depth) {}
}
