package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightPipeShapes {
    private static final VoxelShape[] SHAPES = createShapes();

    private AirtightPipeShapes() {
    }

    static VoxelShape getShape(BlockState state) {
        int mask = 0;
        for (Direction direction : Iterate.directions) {
            if (!AirtightPipeConnections.isOpen(state, direction)) {
                continue;
            }

            mask |= 1 << direction.get3DDataValue();
        }
        return SHAPES[mask];
    }

    private static VoxelShape[] createShapes() {
        VoxelShape[] shapes = new VoxelShape[64];
        for (int mask = 0; mask < shapes.length; mask++) {
            int axes = 0;
            VoxelShape shape = Block.box(4, 4, 4, 12, 12, 12);
            for (Direction direction : Iterate.directions) {
                if ((mask & 1 << direction.get3DDataValue()) == 0) {
                    continue;
                }

                int axis = direction.getAxis().ordinal();
                axes |= 1 << axis;
                int[] from = {4, 4, 4};
                int[] to = {12, 12, 12};
                if (direction.getAxisDirection() == AxisDirection.POSITIVE) {
                    to[axis] = 16;
                }
                else {
                    from[axis] = 0;
                }
                shape = Shapes.or(shape, Block.box(from[0], from[1], from[2], to[0], to[1], to[2]));
            }
            if (Integer.bitCount(axes) > 1) {
                shape = Shapes.or(shape, Block.box(3, 3, 3, 13, 13, 13));
            }
            shapes[mask] = shape.optimize();
        }
        return shapes;
    }
}
