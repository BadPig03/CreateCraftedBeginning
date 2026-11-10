package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import com.google.common.collect.AbstractIterator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record BeamVolumeCells(List<List<Vec3>> volumes, @Nullable AABB bounds) implements Iterable<BlockPos> {
    private static final double BOUNDARY_TOLERANCE = 1.0E-7;

    @Override
    public Iterator<BlockPos> iterator() {
        int lowerX = Integer.MIN_VALUE;
        int lowerY = Integer.MIN_VALUE;
        int lowerZ = Integer.MIN_VALUE;
        int upperX = Integer.MAX_VALUE;
        int upperY = Integer.MAX_VALUE;
        int upperZ = Integer.MAX_VALUE;
        if (bounds != null) {
            lowerX = (int) Math.floor(bounds.minX);
            lowerY = (int) Math.floor(bounds.minY);
            lowerZ = (int) Math.floor(bounds.minZ);
            upperX = (int) Math.ceil(bounds.maxX) - 1;
            upperY = (int) Math.ceil(bounds.maxY) - 1;
            upperZ = (int) Math.ceil(bounds.maxZ) - 1;
            if (lowerX > upperX || lowerY > upperY || lowerZ > upperZ) {
                return Collections.emptyIterator();
            }
        }
        BlockPos minimum = new BlockPos(lowerX, lowerY, lowerZ);
        BlockPos maximum = new BlockPos(upperX, upperY, upperZ);
        return new AbstractIterator<>() {
            private final Iterator<List<Vec3>> remaining = volumes.iterator();
            private final Long2ObjectOpenHashMap<Row> rows = new Long2ObjectOpenHashMap<>();
            private final IntArrayList gaps = new IntArrayList();
            private List<Vec3> points = List.of();
            private Axis axis = Axis.X;
            private int slab = 1;
            private int lastSlab;
            private int minX;
            private int maxX;
            private int minY;
            private int maxY;
            private int maxZ;
            private int nextY;
            private int nextZ = 1;
            private int y;
            private int z;
            private int x = 1;
            private int endX;
            private int gap;

            private static AABB slice(List<Vec3> points, Axis axis, double low, double high) {
                double[] bounds = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
                int size = points.size();
                for (int first = 0; first < size; first++) {
                    Vec3 start = points.get(first);
                    double from = axis.choose(start.x, start.y, start.z);
                    if (from >= low && from <= high) {
                        include(bounds, start);
                    }
                    for (int second = first + 1; second < size; second++) {
                        Vec3 end = points.get(second);
                        double to = axis.choose(end.x, end.y, end.z);
                        if (from == to) {
                            continue;
                        }

                        double minimum = Math.min(from, to);
                        double maximum = Math.max(from, to);
                        if (minimum < low && maximum > low) {
                            include(bounds, start.lerp(end, Math.clamp((low - from) / (to - from), 0, 1)));
                        }
                        if (minimum >= high || maximum <= high) {
                            continue;
                        }

                        include(bounds, start.lerp(end, Math.clamp((high - from) / (to - from), 0, 1)));
                    }
                }
                return new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
            }

            private static void include(double[] bounds, Vec3 point) {
                bounds[0] = Math.min(bounds[0], point.x);
                bounds[1] = Math.min(bounds[1], point.y);
                bounds[2] = Math.min(bounds[2], point.z);
                bounds[3] = Math.max(bounds[3], point.x);
                bounds[4] = Math.max(bounds[4], point.y);
                bounds[5] = Math.max(bounds[5], point.z);
            }

            @Override
            @Nullable
            protected BlockPos computeNext() {
                int lowerX = minimum.getX();
                int lowerY = minimum.getY();
                int lowerZ = minimum.getZ();
                int upperX = maximum.getX();
                int upperY = maximum.getY();
                int upperZ = maximum.getZ();
                while (true) {
                    if (x <= endX) {
                        return new BlockPos(x++, y, z);
                    }

                    if (gap < gaps.size()) {
                        x = gaps.getInt(gap++);
                        endX = gaps.getInt(gap++);
                        continue;
                    }

                    if (nextZ <= maxZ) {
                        y = nextY;
                        z = nextZ;
                        nextY++;
                        if (nextY > maxY) {
                            nextY = minY;
                            nextZ++;
                        }
                        long key = (long) z << 32 | y & 0xFFFFFFFFL;
                        Row row = rows.get(key);
                        if (row == null) {
                            row = new Row();
                            rows.put(key, row);
                        }
                        gaps.clear();
                        gap = 0;
                        row.add(minX, maxX, gaps);
                        continue;
                    }

                    if (slab > lastSlab) {
                        if (!remaining.hasNext()) {
                            return endOfData();
                        }

                        points = remaining.next();
                        Vec3 first = points.getFirst();
                        AABB bounds = new AABB(first, first);
                        for (Vec3 point : points) {
                            bounds = bounds.minmax(new AABB(point, point));
                        }
                        axis = Axis.X;
                        double span = bounds.getXsize();
                        double ySpan = bounds.getYsize();
                        if (ySpan > span) {
                            axis = Axis.Y;
                            span = ySpan;
                        }
                        if (bounds.getZsize() > span) {
                            axis = Axis.Z;
                        }
                        slab = (int) Math.floor(axis.choose(bounds.minX, bounds.minY, bounds.minZ));
                        lastSlab = (int) Math.floor(axis.choose(bounds.maxX, bounds.maxY, bounds.maxZ));
                        if (BeamVolumeCells.this.bounds != null) {
                            slab = Math.max(slab, axis.choose(lowerX, lowerY, lowerZ) - 3);
                            lastSlab = Math.min(lastSlab, axis.choose(upperX, upperY, upperZ) + 3);
                        }
                        if (slab > lastSlab) {
                            continue;
                        }
                    }
                    AABB bounds = slice(points, axis, slab, slab + 1.0).inflate(LaserBlockClip.NEIGHBOR_REACH + BOUNDARY_TOLERANCE);
                    slab++;
                    int left = Math.max((int) Math.floor(bounds.minX), lowerX);
                    int right = Math.min((int) Math.floor(bounds.maxX), upperX);
                    int bottom = Math.max((int) Math.floor(bounds.minY), lowerY);
                    int top = Math.min((int) Math.floor(bounds.maxY), upperY);
                    int back = Math.max((int) Math.floor(bounds.minZ), lowerZ);
                    int front = Math.min((int) Math.floor(bounds.maxZ), upperZ);
                    if (left > right || bottom > top || back > front) {
                        continue;
                    }

                    minX = left;
                    maxX = right;
                    minY = bottom;
                    maxY = top;
                    maxZ = front;
                    nextY = bottom;
                    nextZ = back;
                }
            }
        };
    }

    private static final class Row {
        private final IntArrayList covered = new IntArrayList(2);

        private void add(int minimum, int maximum, IntArrayList gaps) {
            int size = covered.size();
            int first = 0;
            while (first < size) {
                int finish = covered.getInt(first + 1);
                if (finish + 1 < minimum) {
                    first += 2;
                    continue;
                }

                if (covered.getInt(first) <= minimum && finish >= maximum) {
                    return;
                }

                break;
            }

            int end = first;
            int cursor = minimum;
            int mergedMinimum = minimum;
            int mergedMaximum = maximum;
            while (end < size) {
                int start = covered.getInt(end);
                if (start > maximum + 1) {
                    break;
                }

                int finish = covered.getInt(end + 1);
                if (cursor < start) {
                    gaps.add(cursor);
                    gaps.add(Math.min(maximum, start - 1));
                }
                cursor = Math.max(cursor, finish + 1);
                mergedMinimum = Math.min(mergedMinimum, start);
                mergedMaximum = Math.max(mergedMaximum, finish);
                end += 2;
            }
            if (cursor <= maximum) {
                gaps.add(cursor);
                gaps.add(maximum);
            }
            covered.removeElements(first, end);
            covered.add(first, mergedMinimum);
            covered.add(first + 1, mergedMaximum);
        }
    }
}
