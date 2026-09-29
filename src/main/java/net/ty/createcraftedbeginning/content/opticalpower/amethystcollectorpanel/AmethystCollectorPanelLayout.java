package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record AmethystCollectorPanelLayout(int x, int z, int width, int depth) {
    static final AmethystCollectorPanelLayout SINGLE = new AmethystCollectorPanelLayout(0, 0, 1, 1);

    static AmethystCollectorPanelLayout at(BlockGetter level, BlockPos pos) {
        AmethystCollectorPanelGeometry geometry = AmethystCollectorPanelGeometry.findGeometry(level, pos);
        AmethystCollectorPanelRectangle rectangle = geometry.activeRectangle();
        if (!geometry.isActive(pos) || rectangle == null) {
            return SINGLE;
        }
        return new AmethystCollectorPanelLayout(pos.getX() - rectangle.minX(), pos.getZ() - rectangle.minZ(), rectangle.width(), rectangle.depth());
    }

    boolean edge(Direction direction) {
        return switch (direction) {
            case WEST -> x == 0;
            case EAST -> x == width - 1;
            case NORTH -> z == 0;
            case SOUTH -> z == depth - 1;
            default -> true;
        };
    }

    @Unmodifiable
    List<Part> parts() {
        List<Part> parts = new ArrayList<>();
        parts.add(new Part(0, 8, 0, 16, 10, 16));
        boolean west = edge(Direction.WEST);
        boolean east = edge(Direction.EAST);
        boolean north = edge(Direction.NORTH);
        boolean south = edge(Direction.SOUTH);
        for (int cornerX : new int[]{0, 13}) {
            for (int cornerZ : new int[]{0, 13}) {
                if ((cornerX == 0 ? !west : !east) || (cornerZ == 0 ? !north : !south)) {
                    continue;
                }
                parts.add(new Part(cornerX, 5, cornerZ, cornerX + 3, 8, cornerZ + 3));
                int footX = cornerX == 0 ? 0 : 14;
                int footZ = cornerZ == 0 ? 0 : 14;
                parts.add(new Part(footX, 0, footZ, footX + 2, 5, footZ + 2));
            }
        }
        int minX = west ? 3 : 0;
        int maxX = east ? 13 : 16;
        int minZ = north ? 3 : 0;
        int maxZ = south ? 13 : 16;
        if (north) {
            parts.add(new Part(minX, 6, 0, maxX, 8, 2));
        }
        if (south) {
            parts.add(new Part(minX, 6, 14, maxX, 8, 16));
        }
        if (west) {
            parts.add(new Part(0, 6, minZ, 2, 8, maxZ));
        }
        if (east) {
            parts.add(new Part(14, 6, minZ, 16, 8, maxZ));
        }
        return List.copyOf(parts);
    }

    VoxelShape shape() {
        VoxelShape result = Shapes.empty();
        for (Part part : parts()) {
            result = Shapes.or(result, Block.box(part.minX, part.minY, part.minZ, part.maxX, part.maxY, part.maxZ));
        }
        return result;
    }

    record Part(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        boolean panel() {
            return minY == 8;
        }
    }
}
