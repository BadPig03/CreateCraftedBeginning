package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkState;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasOrdinaryPathSearch implements GasRouteSearch {
    private final GasNetworkState state;
    private final Map<BlockPos, Long> limits;
    private final BlockPos target;
    private final Deque<Path> frontier = new LinkedList<>();
    private final Set<BlockPos> visited = new TreeSet<>(GasNetworkTopology::compareBlockPositions);

    GasOrdinaryPathSearch(GasNetworkState state, Map<BlockPos, Long> limits, BlockPos start, BlockPos target) {
        this.state = state;
        this.limits = limits;
        this.target = target;
        long capacity = limits.getOrDefault(start, Long.MAX_VALUE);
        if (capacity > 0) {
            visited.add(start);
            frontier.add(new Path(start, capacity, null));
        }
    }

    @Override
    public boolean pending() {
        return !frontier.isEmpty();
    }

    @Override
    public @Nullable Path expand() {
        Path path = frontier.removeFirst();
        if (path.pos().equals(target)) {
            return path;
        }

        int neighbors = state.ordinaryNeighbors(path.pos());
        for (Direction side : Iterate.directions) {
            if ((neighbors & 1 << side.ordinal()) == 0) {
                continue;
            }

            BlockPos next = path.pos().relative(side);
            if (!visited.add(next)) {
                continue;
            }

            long capacity = Math.min(path.capacity(), limits.getOrDefault(next, Long.MAX_VALUE));
            if (capacity <= 0) {
                continue;
            }

            frontier.addLast(new Path(next, capacity, path));
        }
        return null;
    }
}
