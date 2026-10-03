package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record AmethystCollectorPanelRectangle(BlockPos anchor, int minX, int maxX, int minZ, int maxZ) {
    AmethystCollectorPanelRectangle {
        anchor = anchor.immutable();
    }

    static @Nullable AmethystCollectorPanelRectangle findLargestRectangle(List<BlockPos> dependencies) {
        if (dependencies.isEmpty() || dependencies.size() > AmethystCollectorPanelBlock.MAX_AREA) {
            return null;
        }

        BlockPos first = dependencies.getFirst();
        int y = first.getY();
        int minX = first.getX();
        int maxX = minX;
        int minZ = first.getZ();
        int maxZ = minZ;
        for (BlockPos dependency : dependencies) {
            if (dependency.getY() != y) {
                return null;
            }

            int x = dependency.getX();
            int z = dependency.getZ();
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
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
}
