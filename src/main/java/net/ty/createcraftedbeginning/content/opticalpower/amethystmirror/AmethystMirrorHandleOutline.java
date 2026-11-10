package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorHandleOutline extends AmethystMirrorControlOutline {
    private final AABB bounds;

    AmethystMirrorHandleOutline(Vec3 center, AABB bounds) {
        super(center);
        this.bounds = bounds;
    }

    private static boolean isFrameCell(int x, int y, int z) {
        if (x < 0 || x > 2 || y < 0 || y > 2 || z < 0 || z > 2) {
            return false;
        }

        int outerAxes = 0;
        if (x != 1) {
            outerAxes++;
        }
        if (y != 1) {
            outerAxes++;
        }
        if (z != 1) {
            outerAxes++;
        }
        return outerAxes >= 2;
    }

    @Override
    MeshKey meshKey(float width) {
        return new HandleMeshKey(bounds, width);
    }

    @Override
    List<Vec3> createVertices(float width) {
        double halfWidth = width / 2.0;
        double[] xs = {bounds.minX - halfWidth, bounds.minX + halfWidth, bounds.maxX - halfWidth, bounds.maxX + halfWidth};
        double[] ys = {bounds.minY - halfWidth, bounds.minY + halfWidth, bounds.maxY - halfWidth, bounds.maxY + halfWidth};
        double[] zs = {bounds.minZ - halfWidth, bounds.minZ + halfWidth, bounds.maxZ - halfWidth, bounds.maxZ + halfWidth};
        Vec3[][][] points = new Vec3[4][4][4];
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    points[x][y][z] = new Vec3(xs[x], ys[y], zs[z]);
                }
            }
        }
        List<Vec3> vertices = new ArrayList<>();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    if (!isFrameCell(x, y, z)) {
                        continue;
                    }

                    for (Direction direction : Iterate.directions) {
                        if (isFrameCell(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ())) {
                            continue;
                        }

                        switch (direction) {
                            case DOWN -> vertices.addAll(List.of(points[x][y][z], points[x + 1][y][z], points[x + 1][y][z + 1], points[x][y][z + 1]));
                            case UP -> vertices.addAll(List.of(points[x][y + 1][z], points[x][y + 1][z + 1], points[x + 1][y + 1][z + 1], points[x + 1][y + 1][z]));
                            case NORTH -> vertices.addAll(List.of(points[x][y][z], points[x][y + 1][z], points[x + 1][y + 1][z], points[x + 1][y][z]));
                            case SOUTH -> vertices.addAll(List.of(points[x][y][z + 1], points[x + 1][y][z + 1], points[x + 1][y + 1][z + 1], points[x][y + 1][z + 1]));
                            case WEST -> vertices.addAll(List.of(points[x][y][z], points[x][y][z + 1], points[x][y + 1][z + 1], points[x][y + 1][z]));
                            case EAST -> vertices.addAll(List.of(points[x + 1][y][z], points[x + 1][y + 1][z], points[x + 1][y + 1][z + 1], points[x + 1][y][z + 1]));
                        }
                    }
                }
            }
        }
        return List.copyOf(vertices);
    }

    private record HandleMeshKey(AABB bounds, float width) implements MeshKey {
    }
}
