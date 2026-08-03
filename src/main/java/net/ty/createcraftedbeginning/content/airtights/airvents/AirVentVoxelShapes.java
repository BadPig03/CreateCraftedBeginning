package net.ty.createcraftedbeginning.content.airtights.airvents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirVentVoxelShapes {
    private static final VoxelShape SOUTH_FACE = Shapes.box(0, 0, 0.875, 1, 1, 1);
    private static final VoxelShape EAST_FACE = Shapes.box(0.875, 0, 0, 1, 1, 1);
    private static final VoxelShape UP_FACE = Shapes.box(0, 0.875, 0, 1, 1, 1);
    private static final VoxelShape CENTER = Shapes.box(0.125, 0.125, 0.125, 0.875, 0.875, 0.875);
    private static final VoxelShape SHELL = Shapes.join(Shapes.block(), CENTER, BooleanOp.ONLY_FIRST);
    private static final int SHAPE_COUNT = 1 << Direction.values().length;
    private static final VoxelShape NORTH_FACE = Shapes.box(0, 0, 0, 1, 1, 0.125);
    private static final VoxelShape WEST_FACE = Shapes.box(0, 0, 0, 0.125, 1, 1);
    private static final VoxelShape DOWN_FACE = Shapes.box(0, 0, 0, 1, 0.125, 1);
    private static final VoxelShape[] SHAPES = new VoxelShape[SHAPE_COUNT];

    static {
        for (int mask = 0; mask < SHAPES.length; mask++) {
            SHAPES[mask] = createShape(hasOpening(mask, Direction.NORTH), hasOpening(mask, Direction.SOUTH), hasOpening(mask, Direction.EAST), hasOpening(mask, Direction.WEST), hasOpening(mask, Direction.UP), hasOpening(mask, Direction.DOWN));
        }
    }

    private static boolean hasOpening(int mask, Direction direction) {
        return (mask & 1 << direction.get3DDataValue()) != 0;
    }

    private static VoxelShape createShape(boolean northOpen, boolean southOpen, boolean eastOpen, boolean westOpen, boolean upOpen, boolean downOpen) {
        if (northOpen && southOpen && eastOpen && westOpen && upOpen && downOpen) {
            return Shapes.empty();
        }

        VoxelShape shape = SHELL;
        if (northOpen) {
            shape = Shapes.join(shape, NORTH_FACE, BooleanOp.ONLY_FIRST);
        }
        if (southOpen) {
            shape = Shapes.join(shape, SOUTH_FACE, BooleanOp.ONLY_FIRST);
        }
        if (eastOpen) {
            shape = Shapes.join(shape, EAST_FACE, BooleanOp.ONLY_FIRST);
        }
        if (westOpen) {
            shape = Shapes.join(shape, WEST_FACE, BooleanOp.ONLY_FIRST);
        }
        if (upOpen) {
            shape = Shapes.join(shape, UP_FACE, BooleanOp.ONLY_FIRST);
        }
        if (!downOpen) {
            return shape;
        }

        shape = Shapes.join(shape, DOWN_FACE, BooleanOp.ONLY_FIRST);
        return shape;
    }

    public static VoxelShape getShape(int mask) {
        if (mask < 0 || mask >= SHAPES.length) {
            return SHELL;
        }
        return SHAPES[mask];
    }
}
