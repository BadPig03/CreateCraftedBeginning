package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record AmethystCollectorPanelRectangle(BlockPos anchor, int minX, int maxX, int minZ, int maxZ) {
    private static final List<List<Candidate>> CANDIDATES_BY_SIZE = createCandidates();

    AmethystCollectorPanelRectangle {
        anchor = anchor.immutable();
    }

    static @Nullable AmethystCollectorPanelRectangle findHighestPowerRectangle(List<BlockPos> dependencies) {
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
        if (width <= 0 || depth <= 0 || width > AmethystCollectorPanelBlock.MAX_SIDE || depth > AmethystCollectorPanelBlock.MAX_SIDE) {
            return null;
        }

        int occupiedMask = 0;
        for (BlockPos dependency : dependencies) {
            int localX = dependency.getX() - minX;
            int localZ = dependency.getZ() - minZ;
            occupiedMask |= 1 << localZ * AmethystCollectorPanelBlock.MAX_SIDE + localX;
        }

        List<Candidate> candidates = CANDIDATES_BY_SIZE.get((width - 1) * AmethystCollectorPanelBlock.MAX_SIDE + depth - 1);
        for (Candidate candidate : candidates) {
            int mask = candidate.mask();
            if ((occupiedMask & mask) != mask) {
                continue;
            }

            AmethystCollectorPanelRectangle rectangle = candidate.rectangle();
            int selectedMinX = minX + rectangle.minX();
            int selectedMinZ = minZ + rectangle.minZ();
            return new AmethystCollectorPanelRectangle(new BlockPos(selectedMinX, y, selectedMinZ), selectedMinX, minX + rectangle.maxX(), selectedMinZ, minZ + rectangle.maxZ());
        }
        return null;
    }

    private static @Unmodifiable List<List<Candidate>> createCandidates() {
        List<Candidate> candidates = new ArrayList<>();
        for (int minX = 0; minX < AmethystCollectorPanelBlock.MAX_SIDE; minX++) {
            for (int minZ = 0; minZ < AmethystCollectorPanelBlock.MAX_SIDE; minZ++) {
                for (int maxX = minX; maxX < AmethystCollectorPanelBlock.MAX_SIDE; maxX++) {
                    int rowWidth = maxX - minX + 1;
                    int rowMask = (1 << rowWidth) - 1 << minX;
                    int mask = 0;
                    for (int maxZ = minZ; maxZ < AmethystCollectorPanelBlock.MAX_SIDE; maxZ++) {
                        mask |= rowMask << maxZ * AmethystCollectorPanelBlock.MAX_SIDE;
                        candidates.add(new Candidate(new AmethystCollectorPanelRectangle(new BlockPos(minX, 0, minZ), minX, maxX, minZ, maxZ), mask));
                    }
                }
            }
        }
        candidates.sort((first, second) -> compareRectangles(first.rectangle(), second.rectangle()));
        List<List<Candidate>> candidatesBySize = new ArrayList<>(AmethystCollectorPanelBlock.MAX_AREA);
        for (int width = 1; width <= AmethystCollectorPanelBlock.MAX_SIDE; width++) {
            for (int depth = 1; depth <= AmethystCollectorPanelBlock.MAX_SIDE; depth++) {
                List<Candidate> fitting = new ArrayList<>();
                for (Candidate candidate : candidates) {
                    AmethystCollectorPanelRectangle rectangle = candidate.rectangle();
                    if (rectangle.maxX() >= width || rectangle.maxZ() >= depth) {
                        continue;
                    }

                    fitting.add(candidate);
                }
                candidatesBySize.add(List.copyOf(fitting));
            }
        }
        return List.copyOf(candidatesBySize);
    }

    private static int compareRectangles(AmethystCollectorPanelRectangle first, AmethystCollectorPanelRectangle second) {
        int powerOrder = Integer.compare(AmethystCollectorPanelPower.getRatedFluxLm(second), AmethystCollectorPanelPower.getRatedFluxLm(first));
        if (powerOrder != 0) {
            return powerOrder;
        }

        int areaOrder = Integer.compare(second.area(), first.area());
        if (areaOrder != 0) {
            return areaOrder;
        }

        int firstWidth = first.width();
        int secondWidth = second.width();
        int firstDepth = first.depth();
        int secondDepth = second.depth();
        int shortSideOrder = Integer.compare(Math.min(secondWidth, secondDepth), Math.min(firstWidth, firstDepth));
        if (shortSideOrder != 0) {
            return shortSideOrder;
        }

        int minXOrder = Integer.compare(first.minX(), second.minX());
        if (minXOrder != 0) {
            return minXOrder;
        }

        int minZOrder = Integer.compare(first.minZ(), second.minZ());
        if (minZOrder != 0) {
            return minZOrder;
        }

        int widthOrder = Integer.compare(secondWidth, firstWidth);
        if (widthOrder != 0) {
            return widthOrder;
        }

        return Integer.compare(secondDepth, firstDepth);
    }

    boolean contains(BlockPos pos) {
        if (pos.getY() != anchor.getY()) {
            return false;
        }

        int x = pos.getX();
        if (x < minX || x > maxX) {
            return false;
        }

        int z = pos.getZ();
        return z >= minZ && z <= maxZ;
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

    private record Candidate(AmethystCollectorPanelRectangle rectangle, int mask) {
    }
}
