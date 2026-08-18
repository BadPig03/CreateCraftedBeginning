package net.ty.createcraftedbeginning.content.photostresses.photosail;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.photostresses.network.PhotoStressNetworkManager;
import net.ty.createcraftedbeginning.content.photostresses.network.PhotoStressSource;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import org.jetbrains.annotations.Nullable;

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
public class PhotoSailBlock extends Block implements PhotoStressSource {
    protected static final int MAX_SIDE = 5;
    protected static final int MAX_AREA = MAX_SIDE * MAX_SIDE;

    private static final int MAX_SAIL_LEVEL = 8;
    private static final int STRESS_POINTS_PER_SAIL_LEVEL = 2;
    private static final int DAY_LENGTH = 24000;
    private static final int SUNRISE = 0;
    private static final int NOON = 6000;
    private static final int SUNSET = 12000;
    private static final int SOLAR_LEVELS = 8;
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator.comparingInt((BlockPos pos) -> pos.getX()).thenComparingInt(Vec3i::getZ);

    public PhotoSailBlock(Properties properties) {
        super(properties);
    }

    private static SailGeometry findGeometry(Level level, BlockPos origin) {
        if (!level.isLoaded(origin) || !isSail(level.getBlockState(origin))) {
            return SailGeometry.invalid(origin, List.of());
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

            if (visited.size() > MAX_AREA || maxX - minX + 1 > MAX_SIDE || maxZ - minZ + 1 > MAX_SIDE) {
                List<BlockPos> dependencies = sortedDependencies(visited);
                BlockPos anchor = dependencies.isEmpty() ? origin.immutable() : dependencies.getFirst();
                return SailGeometry.invalid(anchor, dependencies);
            }

            for (Direction direction : Plane.HORIZONTAL) {
                BlockPos neighbor = current.relative(direction);
                if (!level.isLoaded(neighbor) || !isSail(level.getBlockState(neighbor)) || visited.contains(neighbor)) {
                    continue;
                }

                frontier.addLast(neighbor.immutable());
            }
        }

        List<BlockPos> dependencies = sortedDependencies(visited);
        SailRectangle activeRectangle = findLargestRectangle(dependencies);
        BlockPos anchor = dependencies.getFirst();
        if (activeRectangle == null) {
            return SailGeometry.invalid(anchor, dependencies);
        }

        List<BlockPos> powerDependencies = rectangleDependencies(activeRectangle);
        return new SailGeometry(anchor, dependencies, powerDependencies, activeRectangle, true);
    }

    private static List<BlockPos> sortedDependencies(Set<BlockPos> positions) {
        List<BlockPos> dependencies = new ArrayList<>(positions);
        dependencies.sort(POSITION_ORDER);
        return List.copyOf(dependencies);
    }

    private static @Nullable SailRectangle findLargestRectangle(List<BlockPos> dependencies) {
        if (dependencies.isEmpty() || dependencies.size() > MAX_AREA) {
            return null;
        }

        BlockPos first = dependencies.getFirst();
        int y = first.getY();
        int minX = first.getX();
        int maxX = first.getX();
        int minZ = first.getZ();
        int maxZ = first.getZ();
        for (BlockPos dependency : dependencies) {
            if (dependency.getY() != y) {
                return null;
            }

            minX = Math.min(minX, dependency.getX());
            maxX = Math.max(maxX, dependency.getX());
            minZ = Math.min(minZ, dependency.getZ());
            maxZ = Math.max(maxZ, dependency.getZ());
        }

        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        if (width > MAX_SIDE || depth > MAX_SIDE) {
            return null;
        }

        int occupiedMask = 0;
        for (BlockPos dependency : dependencies) {
            int localX = dependency.getX() - minX;
            int localZ = dependency.getZ() - minZ;
            occupiedMask |= 1 << localZ * MAX_SIDE + localX;
        }

        SailRectangle best = null;
        for (int rectangleMinX = 0; rectangleMinX < width; rectangleMinX++) {
            for (int rectangleMinZ = 0; rectangleMinZ < depth; rectangleMinZ++) {
                for (int rectangleMaxX = rectangleMinX; rectangleMaxX < width; rectangleMaxX++) {
                    for (int rectangleMaxZ = rectangleMinZ; rectangleMaxZ < depth; rectangleMaxZ++) {
                        if (!isFilledRectangle(occupiedMask, rectangleMinX, rectangleMaxX, rectangleMinZ, rectangleMaxZ)) {
                            continue;
                        }

                        SailRectangle candidate = new SailRectangle(new BlockPos(minX + rectangleMinX, y, minZ + rectangleMinZ), minX + rectangleMinX, minX + rectangleMaxX, minZ + rectangleMinZ, minZ + rectangleMaxZ);
                        if (isBetterRectangle(candidate, best)) {
                            best = candidate;
                        }
                    }
                }
            }
        }
        return best;
    }

    private static boolean isFilledRectangle(int occupiedMask, int minX, int maxX, int minZ, int maxZ) {
        int rowWidth = maxX - minX + 1;
        int rowMask = (1 << rowWidth) - 1 << minX;
        for (int z = minZ; z <= maxZ; z++) {
            int occupiedRow = occupiedMask >>> z * MAX_SIDE;
            if ((occupiedRow & rowMask) == rowMask) {
                continue;
            }

            return false;
        }
        return true;
    }

    private static boolean isBetterRectangle(SailRectangle candidate, @Nullable SailRectangle currentBest) {
        if (currentBest == null || candidate.area() != currentBest.area()) {
            return currentBest == null || candidate.area() > currentBest.area();
        }

        int candidateShortSide = Math.min(candidate.width(), candidate.depth());
        int bestShortSide = Math.min(currentBest.width(), currentBest.depth());
        if (candidateShortSide != bestShortSide) {
            return candidateShortSide > bestShortSide;
        }

        if (candidate.minX() != currentBest.minX()) {
            return candidate.minX() < currentBest.minX();
        }
        if (candidate.minZ() != currentBest.minZ()) {
            return candidate.minZ() < currentBest.minZ();
        }
        if (candidate.width() != currentBest.width()) {
            return candidate.width() > currentBest.width();
        }
        return candidate.depth() > currentBest.depth();
    }

    private static List<BlockPos> rectangleDependencies(SailRectangle rectangle) {
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

    private static @Nullable SailRectangle rectangleFromKnownValidDependencies(List<BlockPos> dependencies) {
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
        return new SailRectangle(new BlockPos(minX, first.getY(), minZ), minX, maxX, minZ, maxZ);
    }

    private static boolean hasOpenSky(Level level, SailRectangle rectangle) {
        if (!level.dimensionType().hasSkyLight()) {
            return false;
        }

        MutableBlockPos skyPos = new MutableBlockPos();
        int y = rectangle.anchor().getY() + 1;
        for (int x = rectangle.minX(); x <= rectangle.maxX(); x++) {
            for (int z = rectangle.minZ(); z <= rectangle.maxZ(); z++) {
                skyPos.set(x, y, z);
                if (level.canSeeSky(skyPos)) {
                    continue;
                }

                return false;
            }
        }
        return true;
    }

    private static boolean isRainingOn(Level level, SailRectangle rectangle) {
        if (!level.isRaining()) {
            return false;
        }

        MutableBlockPos rainPos = new MutableBlockPos();
        int y = rectangle.anchor().getY() + 1;
        for (int x = rectangle.minX(); x <= rectangle.maxX(); x++) {
            for (int z = rectangle.minZ(); z <= rectangle.maxZ(); z++) {
                rainPos.set(x, y, z);
                if (!level.isRainingAt(rainPos)) {
                    continue;
                }

                return true;
            }
        }
        return false;
    }

    private static int getSailLevel(int area) {
        return Mth.clamp((area * MAX_SAIL_LEVEL + MAX_AREA - 1) / MAX_AREA, 1, MAX_SAIL_LEVEL);
    }

    private static int getSolarLevel(Level level) {
        long dayTime = Math.floorMod(level.getDayTime(), DAY_LENGTH);
        if (dayTime <= SUNRISE || dayTime >= SUNSET) {
            return 0;
        }

        int daylightTicks = NOON - (int) Math.abs(dayTime - NOON);
        return Mth.clamp((daylightTicks * SOLAR_LEVELS + NOON - 1) / NOON, 1, SOLAR_LEVELS);
    }

    private static int calculateStressPoints(Level level, SailRectangle rectangle) {
        int solarLevel = getSolarLevel(level);
        if (solarLevel <= 0 || !hasOpenSky(level, rectangle)) {
            return 0;
        }

        int sailLevel = getSailLevel(rectangle.area());
        int maximumStressPoints = sailLevel * STRESS_POINTS_PER_SAIL_LEVEL;
        int stressPoints = (maximumStressPoints * solarLevel + SOLAR_LEVELS - 1) / SOLAR_LEVELS;
        if (isRainingOn(level, rectangle)) {
            stressPoints /= 2;
        }
        return stressPoints;
    }

    private static boolean isSail(BlockState state) {
        return state.getBlock() instanceof PhotoSailBlock;
    }

    @Override
    public Source getPhotoStressSource(Level level, BlockPos pos, BlockState state) {
        SailGeometry geometry = findGeometry(level, pos);
        SailRectangle rectangle = geometry.activeRectangle();
        int stressPoints = geometry.topologyValid() && rectangle != null ? calculateStressPoints(level, rectangle) : 0;
        return new Source(geometry.anchor(), stressPoints, geometry.dependencies(), geometry.powerDependencies(), true, geometry.topologyValid());
    }

    @Override
    public int getCurrentPhotoStressPoints(Level level, BlockPos pos, BlockState state, Source discoveredSource) {
        if (!discoveredSource.topologyValid()) {
            return 0;
        }

        SailRectangle rectangle = rectangleFromKnownValidDependencies(discoveredSource.powerDependencies());
        return rectangle == null ? 0 : calculateStressPoints(level, rectangle);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide || oldState.is(state.getBlock())) {
            return;
        }

        PhotoStressNetworkManager.invalidateSailChange(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            PhotoStressNetworkManager.invalidateSailChange(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CCBShapes.PHOTO_SAIL_SHAPE;
    }

    protected record SailGeometry(BlockPos anchor, List<BlockPos> dependencies, List<BlockPos> powerDependencies, @Nullable SailRectangle activeRectangle, boolean topologyValid) {
        protected SailGeometry {
            anchor = anchor.immutable();
            dependencies = List.copyOf(dependencies);
            powerDependencies = List.copyOf(powerDependencies);
        }

        private static SailGeometry invalid(BlockPos anchor, List<BlockPos> dependencies) {
            return new SailGeometry(anchor, dependencies, List.of(), null, false);
        }
    }

    protected record SailRectangle(BlockPos anchor, int minX, int maxX, int minZ, int maxZ) {
        protected SailRectangle {
            anchor = anchor.immutable();
        }

        private int width() {
            return maxX - minX + 1;
        }

        private int depth() {
            return maxZ - minZ + 1;
        }

        private int area() {
            return width() * depth();
        }
    }
}
