package net.ty.createcraftedbeginning.compat.sable;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorCollisionShape;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorCollisionShape.CubeIntersection;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorVoxelShape extends VoxelShape {
    private static final int RESOLUTION = 32;
    private final DoubleList xCoordinates;
    private final DoubleList yCoordinates;
    private final DoubleList zCoordinates;

    private AmethystMirrorVoxelShape(BitSetDiscreteVoxelShape cells, int x, int y, int z) {
        super(cells);
        xCoordinates = coordinates(x, cells.getXSize());
        yCoordinates = coordinates(y, cells.getYSize());
        zCoordinates = coordinates(z, cells.getZSize());
    }

    static AmethystMirrorVoxelShape create(AmethystMirrorCollisionShape mirror) {
        AABB bounds = mirror.bounds();
        int minX = Mth.floor(bounds.minX * RESOLUTION);
        int minY = Mth.floor(bounds.minY * RESOLUTION);
        int minZ = Mth.floor(bounds.minZ * RESOLUTION);
        int sizeX = Mth.ceil(bounds.maxX * RESOLUTION) - minX;
        int sizeY = Mth.ceil(bounds.maxY * RESOLUTION) - minY;
        int sizeZ = Mth.ceil(bounds.maxZ * RESOLUTION) - minZ;
        BitSetDiscreteVoxelShape cells = new BitSetDiscreteVoxelShape(sizeX, sizeY, sizeZ);
        CubeIntersection intersection = mirror.prepareCubeIntersection(0.5 / RESOLUTION);
        for (int x = 0; x < sizeX; x++) {
            double centerX = (minX + x + 0.5) / RESOLUTION;
            for (int y = 0; y < sizeY; y++) {
                double centerY = (minY + y + 0.5) / RESOLUTION;
                for (int z = 0; z < sizeZ; z++) {
                    double centerZ = (minZ + z + 0.5) / RESOLUTION;
                    if (!intersection.intersects(centerX, centerY, centerZ)) {
                        continue;
                    }

                    cells.fill(x, y, z);
                }
            }
        }
        return new AmethystMirrorVoxelShape(cells, minX, minY, minZ);
    }

    private static DoubleList coordinates(int start, int size) {
        DoubleList coordinates = new DoubleArrayList(size + 1);
        for (int index = 0; index <= size; index++) {
            coordinates.add((start + index) / (double) RESOLUTION);
        }
        return coordinates;
    }

    @Override
    public DoubleList getCoords(Axis axis) {
        return switch (axis) {
            case X -> xCoordinates;
            case Y -> yCoordinates;
            case Z -> zCoordinates;
        };
    }

    Map<BlockPos, List<AABB>> split(BlockPos source) {
        Map<BlockPos, List<AABB>> divided = new HashMap<>();
        forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            int endX = Mth.ceil(maxX);
            int endY = Mth.ceil(maxY);
            int endZ = Mth.ceil(maxZ);
            for (int x = Mth.floor(minX); x < endX; x++) {
                for (int y = Mth.floor(minY); y < endY; y++) {
                    for (int z = Mth.floor(minZ); z < endZ; z++) {
                        AABB clipped = new AABB(Math.max(minX, x) - x, Math.max(minY, y) - y, Math.max(minZ, z) - z, Math.min(maxX, x + 1) - x, Math.min(maxY, y + 1) - y, Math.min(maxZ, z + 1) - z);
                        divided.computeIfAbsent(source.offset(x, y, z), ignored -> new ArrayList<>()).add(clipped);
                    }
                }
            }
        });
        return divided;
    }
}
