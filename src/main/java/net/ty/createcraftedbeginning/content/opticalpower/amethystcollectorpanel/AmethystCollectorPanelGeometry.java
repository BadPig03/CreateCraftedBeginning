package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record AmethystCollectorPanelGeometry(BlockPos anchor, List<BlockPos> dependencies, List<BlockPos> powerDependencies, @Nullable AmethystCollectorPanelRectangle activeRectangle, boolean topologyValid) {
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator.comparingInt((BlockPos pos) -> pos.getX()).thenComparingInt(Vec3i::getZ);

    AmethystCollectorPanelGeometry {
        anchor = anchor.immutable();
        dependencies = List.copyOf(dependencies);
        powerDependencies = List.copyOf(powerDependencies);
    }

    static AmethystCollectorPanelGeometry findGeometry(Level level, BlockPos origin) {
        return findGeometry(origin, pos -> level.isLoaded(pos) && isCollector(level, pos));
    }

    static AmethystCollectorPanelGeometry findGeometry(BlockGetter level, BlockPos origin) {
        return findGeometry(origin, pos -> (!(level instanceof Level world) || world.isLoaded(pos)) && isCollector(level, pos));
    }

    static AmethystCollectorPanelGeometry withPlacement(Level level, BlockPos origin, BlockPos placementPos) {
        return findGeometry(origin, pos -> level.isLoaded(pos) && (pos.equals(placementPos) || isCollector(level, pos)));
    }

    static @Nullable AmethystCollectorPanelRectangle rectangleFromKnownValidDependencies(List<BlockPos> dependencies) {
        if (dependencies.isEmpty()) {
            return null;
        }

        BlockPos first = dependencies.getFirst();
        int minX = first.getX();
        int maxX = minX;
        int minZ = first.getZ();
        int maxZ = minZ;
        for (BlockPos dependency : dependencies) {
            int x = dependency.getX();
            int z = dependency.getZ();
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }
        return new AmethystCollectorPanelRectangle(new BlockPos(minX, first.getY(), minZ), minX, maxX, minZ, maxZ);
    }

    private static AmethystCollectorPanelGeometry findGeometry(BlockPos origin, Predicate<BlockPos> isCollector) {
        if (!isCollector.test(origin)) {
            return invalid(origin, List.of());
        }

        Deque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        frontier.add(origin.immutable());
        int minX = origin.getX();
        int maxX = minX;
        int minZ = origin.getZ();
        int maxZ = minZ;
        while (!frontier.isEmpty()) {
            BlockPos current = frontier.removeFirst();
            if (!visited.add(current)) {
                continue;
            }

            int x = current.getX();
            int z = current.getZ();
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
            if (visited.size() > AmethystCollectorPanelBlock.MAX_AREA || maxX - minX + 1 > AmethystCollectorPanelBlock.MAX_SIDE || maxZ - minZ + 1 > AmethystCollectorPanelBlock.MAX_SIDE) {
                List<BlockPos> dependencies = sortedDependencies(visited);
                BlockPos anchor = dependencies.isEmpty() ? origin.immutable() : dependencies.getFirst();
                return invalid(anchor, dependencies);
            }

            for (Direction direction : Plane.HORIZONTAL) {
                BlockPos neighbor = current.relative(direction);
                if (visited.contains(neighbor) || !isCollector.test(neighbor)) {
                    continue;
                }

                frontier.addLast(neighbor.immutable());
            }
        }

        List<BlockPos> dependencies = sortedDependencies(visited);
        AmethystCollectorPanelRectangle activeRectangle = AmethystCollectorPanelRectangle.findLargestRectangle(dependencies);
        BlockPos anchor = dependencies.getFirst();
        if (activeRectangle == null) {
            return invalid(anchor, dependencies);
        }

        List<BlockPos> powerDependencies = rectangleDependencies(activeRectangle);
        return new AmethystCollectorPanelGeometry(anchor, dependencies, powerDependencies, activeRectangle, true);
    }

    @SuppressWarnings("ConstantValue")
    private static boolean isCollector(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state != null && state.getBlock() instanceof AmethystCollectorPanelBlock;
    }

    private static @Unmodifiable List<BlockPos> sortedDependencies(Set<BlockPos> positions) {
        List<BlockPos> dependencies = new ArrayList<>(positions);
        dependencies.sort(POSITION_ORDER);
        return List.copyOf(dependencies);
    }

    private static @Unmodifiable List<BlockPos> rectangleDependencies(AmethystCollectorPanelRectangle rectangle) {
        List<BlockPos> dependencies = new ArrayList<>(rectangle.area());
        int y = rectangle.anchor().getY();
        for (int x = rectangle.minX(); x <= rectangle.maxX(); x++) {
            for (int z = rectangle.minZ(); z <= rectangle.maxZ(); z++) {
                dependencies.add(new BlockPos(x, y, z));
            }
        }
        dependencies.sort(POSITION_ORDER);
        return List.copyOf(dependencies);
    }

    @Contract("_, _ -> new")
    private static AmethystCollectorPanelGeometry invalid(BlockPos anchor, List<BlockPos> dependencies) {
        return new AmethystCollectorPanelGeometry(anchor, dependencies, List.of(), null, false);
    }

    boolean isActive(BlockPos pos) {
        return topologyValid && activeRectangle != null && activeRectangle.contains(pos);
    }

}
