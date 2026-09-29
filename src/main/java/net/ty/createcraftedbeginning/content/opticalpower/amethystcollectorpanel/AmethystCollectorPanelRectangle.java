package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record AmethystCollectorPanelRectangle(BlockPos anchor, int minX, int maxX, int minZ, int maxZ) {
    private static final float BASE_RAIN_OUTPUT_MULTIPLIER = 0.5F;
    private static final double SKY_SAMPLE_HEIGHT = 0.6251;

    AmethystCollectorPanelRectangle {
        anchor = anchor.immutable();
    }

    static int calculatePowerPoints(Level level, AmethystCollectorPanelRectangle rectangle) {
        if (!level.isDay() || !hasOpenSky(level, rectangle)) {
            return 0;
        }

        int powerPoints = getRatedPowerPoints(rectangle);
        BlockPos anchor = rectangle.anchor();
        if (!SubLevelBridge.isRainingAtWorld(level, getSkySample(anchor.getX(), anchor.getY(), anchor.getZ()))) {
            return powerPoints;
        }

        float rainMultiplier = Mth.clamp(BASE_RAIN_OUTPUT_MULTIPLIER * CCBConfig.server().opticalPower.amethystCollectorPanel.rainOutputMultiplier.getF(), 0.0F, 1.0F);
        return Mth.floor(powerPoints * rainMultiplier);
    }

    static @Nullable AmethystCollectorPanelRectangle findLargestRectangle(List<BlockPos> dependencies) {
        if (dependencies.isEmpty() || dependencies.size() > AmethystCollectorPanelBlock.MAX_AREA) {
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
        if (width > AmethystCollectorPanelBlock.MAX_SIDE || depth > AmethystCollectorPanelBlock.MAX_SIDE) {
            return null;
        }

        int occupiedMask = 0;
        for (BlockPos dependency : dependencies) {
            int localX = dependency.getX() - minX;
            int localZ = dependency.getZ() - minZ;
            occupiedMask |= 1 << localZ * AmethystCollectorPanelBlock.MAX_SIDE + localX;
        }

        AmethystCollectorPanelRectangle best = null;
        for (int rectangleMinX = 0; rectangleMinX < width; rectangleMinX++) {
            for (int rectangleMinZ = 0; rectangleMinZ < depth; rectangleMinZ++) {
                for (int rectangleMaxX = rectangleMinX; rectangleMaxX < width; rectangleMaxX++) {
                    for (int rectangleMaxZ = rectangleMinZ; rectangleMaxZ < depth; rectangleMaxZ++) {
                        if (!isFilledRectangle(occupiedMask, rectangleMinX, rectangleMaxX, rectangleMinZ, rectangleMaxZ)) {
                            continue;
                        }

                        AmethystCollectorPanelRectangle candidate = new AmethystCollectorPanelRectangle(new BlockPos(minX + rectangleMinX, y, minZ + rectangleMinZ), minX + rectangleMinX, minX + rectangleMaxX, minZ + rectangleMinZ, minZ + rectangleMaxZ);
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

    boolean contains(BlockPos pos) {
        return pos.getY() == anchor.getY() && pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ;
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

    private static boolean isFilledRectangle(int occupiedMask, int minX, int maxX, int minZ, int maxZ) {
        int rowWidth = maxX - minX + 1;
        int rowMask = (1 << rowWidth) - 1 << minX;
        for (int z = minZ; z <= maxZ; z++) {
            int occupiedRow = occupiedMask >>> z * AmethystCollectorPanelBlock.MAX_SIDE;
            if ((occupiedRow & rowMask) == rowMask) {
                continue;
            }

            return false;
        }
        return true;
    }

    private static boolean isBetterRectangle(AmethystCollectorPanelRectangle candidate, @Nullable AmethystCollectorPanelRectangle currentBest) {
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

    private static boolean hasOpenSky(Level level, AmethystCollectorPanelRectangle rectangle) {
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

    private static Vec3 getSkySample(int x, int y, int z) {
        return new Vec3(x + 0.5, y + SKY_SAMPLE_HEIGHT, z + 0.5);
    }

    private static int getRatedPowerPoints(AmethystCollectorPanelRectangle rectangle) {
        int shortSide = Math.min(rectangle.width(), rectangle.depth());
        return 1 << Mth.clamp(shortSide - 1, 0, 4);
    }
}
