package net.ty.createcraftedbeginning.content.opticalpower.solarcollector;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
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

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record SolarCollectorGeometry(BlockPos anchor, List<BlockPos> dependencies, List<BlockPos> powerDependencies, @Nullable SolarCollectorRectangle activeRectangle, boolean topologyValid) {
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator.comparingInt((BlockPos pos) -> pos.getX()).thenComparingInt(Vec3i::getZ);

    SolarCollectorGeometry {
        anchor = anchor.immutable();
        dependencies = List.copyOf(dependencies);
        powerDependencies = List.copyOf(powerDependencies);
    }

    static SolarCollectorGeometry findGeometry(Level level, BlockPos origin) {
        if (!level.isLoaded(origin) || !(level.getBlockState(origin).getBlock() instanceof SolarCollectorBlock)) {
            return invalid(origin, List.of());
        }

        Deque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        frontier.add(origin.immutable());
        int minX = origin.getX();
        int maxX = origin.getX();
        int minZ = origin.getZ();
        int maxZ = origin.getZ();
        while (!frontier.isEmpty()) {
            BlockPos current = frontier.removeFirst();
            if (!visited.add(current)) {
                continue;
            }

            minX = Math.min(minX, current.getX());
            maxX = Math.max(maxX, current.getX());
            minZ = Math.min(minZ, current.getZ());
            maxZ = Math.max(maxZ, current.getZ());
            if (visited.size() > SolarCollectorBlock.MAX_AREA || maxX - minX + 1 > SolarCollectorBlock.MAX_SIDE || maxZ - minZ + 1 > SolarCollectorBlock.MAX_SIDE) {
                List<BlockPos> dependencies = sortedDependencies(visited);
                BlockPos anchor = dependencies.isEmpty() ? origin.immutable() : dependencies.getFirst();
                return invalid(anchor, dependencies);
            }

            for (Direction direction : Plane.HORIZONTAL) {
                BlockPos neighbor = current.relative(direction);
                if (!level.isLoaded(neighbor) || !(level.getBlockState(neighbor).getBlock() instanceof SolarCollectorBlock) || visited.contains(neighbor)) {
                    continue;
                }

                frontier.addLast(neighbor.immutable());
            }
        }

        List<BlockPos> dependencies = sortedDependencies(visited);
        SolarCollectorRectangle activeRectangle = SolarCollectorRectangle.findLargestRectangle(dependencies);
        BlockPos anchor = dependencies.getFirst();
        if (activeRectangle == null) {
            return invalid(anchor, dependencies);
        }

        List<BlockPos> powerDependencies = rectangleDependencies(activeRectangle);
        return new SolarCollectorGeometry(anchor, dependencies, powerDependencies, activeRectangle, true);
    }

    static @Nullable SolarCollectorRectangle rectangleFromKnownValidDependencies(List<BlockPos> dependencies) {
        if (dependencies.isEmpty()) {
            return null;
        }

        BlockPos first = dependencies.getFirst();
        int minX = first.getX();
        int maxX = first.getX();
        int minZ = first.getZ();
        int maxZ = first.getZ();
        for (BlockPos dependency : dependencies) {
            minX = Math.min(minX, dependency.getX());
            maxX = Math.max(maxX, dependency.getX());
            minZ = Math.min(minZ, dependency.getZ());
            maxZ = Math.max(maxZ, dependency.getZ());
        }
        return new SolarCollectorRectangle(new BlockPos(minX, first.getY(), minZ), minX, maxX, minZ, maxZ);
    }

    private static @Unmodifiable List<BlockPos> sortedDependencies(Set<BlockPos> positions) {
        List<BlockPos> dependencies = new ArrayList<>(positions);
        dependencies.sort(POSITION_ORDER);
        return List.copyOf(dependencies);
    }

    private static @Unmodifiable List<BlockPos> rectangleDependencies(SolarCollectorRectangle rectangle) {
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
    private static SolarCollectorGeometry invalid(BlockPos anchor, List<BlockPos> dependencies) {
        return new SolarCollectorGeometry(anchor, dependencies, List.of(), null, false);
    }
}
