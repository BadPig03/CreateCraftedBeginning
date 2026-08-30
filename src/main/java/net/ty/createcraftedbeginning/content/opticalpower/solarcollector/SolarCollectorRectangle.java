package net.ty.createcraftedbeginning.content.opticalpower.solarcollector;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record SolarCollectorRectangle(BlockPos anchor, int minX, int maxX, int minZ, int maxZ) {
    private static final double SKY_SAMPLE_HEIGHT = 0.5626;

    SolarCollectorRectangle {
        anchor = anchor.immutable();
    }

    static int calculatePowerPoints(Level level, SolarCollectorRectangle rectangle) {
        if (!level.isDay() || !hasOpenSky(level, rectangle)) {
            return 0;
        }

        int powerPoints = getRatedPowerPoints(rectangle);
        if (!SubLevelBridge.isRainingAtWorld(level, getSkySample(rectangle.anchor()))) {
            return powerPoints;
        }

        float rainMultiplier = CCBMathUtils.clampUnit(CCBConfig.server().opticalPower.rainOutputMultiplier.getF());
        return (int) Math.floor(powerPoints * rainMultiplier);
    }

    static @Nullable SolarCollectorRectangle findLargestRectangle(List<BlockPos> dependencies) {
        if (dependencies.isEmpty() || dependencies.size() > SolarCollectorBlock.MAX_AREA) {
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
        if (width > SolarCollectorBlock.MAX_SIDE || depth > SolarCollectorBlock.MAX_SIDE) {
            return null;
        }

        int occupiedMask = 0;
        for (BlockPos dependency : dependencies) {
            int localX = dependency.getX() - minX;
            int localZ = dependency.getZ() - minZ;
            occupiedMask |= 1 << localZ * SolarCollectorBlock.MAX_SIDE + localX;
        }

        SolarCollectorRectangle best = null;
        for (int rectangleMinX = 0; rectangleMinX < width; rectangleMinX++) {
            for (int rectangleMinZ = 0; rectangleMinZ < depth; rectangleMinZ++) {
                for (int rectangleMaxX = rectangleMinX; rectangleMaxX < width; rectangleMaxX++) {
                    for (int rectangleMaxZ = rectangleMinZ; rectangleMaxZ < depth; rectangleMaxZ++) {
                        if (!isFilledRectangle(occupiedMask, rectangleMinX, rectangleMaxX, rectangleMinZ, rectangleMaxZ)) {
                            continue;
                        }

                        SolarCollectorRectangle candidate = new SolarCollectorRectangle(new BlockPos(minX + rectangleMinX, y, minZ + rectangleMinZ), minX + rectangleMinX, minX + rectangleMaxX, minZ + rectangleMinZ, minZ + rectangleMaxZ);
                        if (!isBetterRectangle(candidate, best)) {
                            continue;
                        }

                        best = candidate;
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
            int occupiedRow = occupiedMask >>> z * SolarCollectorBlock.MAX_SIDE;
            if ((occupiedRow & rowMask) == rowMask) {
                continue;
            }

            return false;
        }
        return true;
    }

    private static boolean isBetterRectangle(SolarCollectorRectangle candidate, @Nullable SolarCollectorRectangle currentBest) {
        int candidateArea = candidate.area();
        if (currentBest == null) {
            return true;
        }

        int bestArea = currentBest.area();
        if (candidateArea != bestArea) {
            return candidateArea > bestArea;
        }

        int candidateWidth = candidate.width();
        int bestWidth = currentBest.width();
        int candidateDepth = candidate.depth();
        int bestDepth = currentBest.depth();
        int candidateShortSide = Math.min(candidateWidth, candidateDepth);
        int bestShortSide = Math.min(bestWidth, bestDepth);
        if (candidateShortSide != bestShortSide) {
            return candidateShortSide > bestShortSide;
        }

        int candidateMinX = candidate.minX();
        int bestMinX = currentBest.minX();
        if (candidateMinX != bestMinX) {
            return candidateMinX < bestMinX;
        }

        int candidateMinZ = candidate.minZ();
        int bestMinZ = currentBest.minZ();
        if (candidateMinZ != bestMinZ) {
            return candidateMinZ < bestMinZ;
        }

        if (candidateWidth != bestWidth) {
            return candidateWidth > bestWidth;
        }
        return candidateDepth > bestDepth;
    }

    private static boolean hasOpenSky(Level level, SolarCollectorRectangle rectangle) {
        if (!level.dimensionType().hasSkyLight()) {
            return false;
        }

        int y = rectangle.anchor().getY();
        for (int x = rectangle.minX(); x <= rectangle.maxX(); x++) {
            for (int z = rectangle.minZ(); z <= rectangle.maxZ(); z++) {
                if (SubLevelBridge.canSeeWorldSky(level, getSkySample(x, y, z))) {
                    continue;
                }

                return false;
            }
        }
        return true;
    }

    private static Vec3 getSkySample(BlockPos pos) {
        return getSkySample(pos.getX(), pos.getY(), pos.getZ());
    }

    private static Vec3 getSkySample(int x, int y, int z) {
        return new Vec3(x + 0.5, y + SKY_SAMPLE_HEIGHT, z + 0.5);
    }

    private static int getRatedPowerPoints(SolarCollectorRectangle rectangle) {
        int shortSide = Math.min(rectangle.width(), rectangle.depth());
        return 1 << CCBMathUtils.clampNonNegative(shortSide - 1, 4);
    }

    int width() {
        return maxX - minX + 1;
    }

    int depth() {
        return maxZ - minZ + 1;
    }

    int area() {
        return width() * depth();
    }
}
