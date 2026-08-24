package net.ty.createcraftedbeginning.content.airtights.airvents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirVentVoxelShapes {
    private AirVentVoxelShapes() {
    }

    private static final double THICKNESS = 0.0125;
    private static final VoxelShape NORTH_FACE = Shapes.box(0, 0, 0, 1, 1, THICKNESS);
    private static final VoxelShape SOUTH_FACE = Shapes.box(0, 0, 1 - THICKNESS, 1, 1, 1);
    private static final VoxelShape WEST_FACE = Shapes.box(0, 0, 0, THICKNESS, 1, 1);
    private static final VoxelShape EAST_FACE = Shapes.box(1 - THICKNESS, 0, 0, 1, 1, 1);
    private static final VoxelShape DOWN_FACE = Shapes.box(0, 0, 0, 1, THICKNESS, 1);
    private static final VoxelShape UP_FACE = Shapes.box(0, 1 - THICKNESS, 0, 1, 1, 1);
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        for (int openingMask = 0; openingMask < SHAPES.length; openingMask++) {
            SHAPES[openingMask] = createShape(hasOpening(openingMask, Direction.NORTH), hasOpening(openingMask, Direction.SOUTH), hasOpening(openingMask, Direction.EAST), hasOpening(openingMask, Direction.WEST), hasOpening(openingMask, Direction.UP), hasOpening(openingMask, Direction.DOWN));
        }
    }

    private static boolean hasOpening(int openingMask, Direction direction) {
        return (openingMask & 1 << direction.get3DDataValue()) != 0;
    }

    private static VoxelShape createShape(boolean northOpen, boolean southOpen, boolean eastOpen, boolean westOpen, boolean upOpen, boolean downOpen) {
        VoxelShape shape = Shapes.empty();
        if (!northOpen) {
            shape = Shapes.or(shape, NORTH_FACE);
        }
        if (!southOpen) {
            shape = Shapes.or(shape, SOUTH_FACE);
        }
        if (!eastOpen) {
            shape = Shapes.or(shape, EAST_FACE);
        }
        if (!westOpen) {
            shape = Shapes.or(shape, WEST_FACE);
        }
        if (!upOpen) {
            shape = Shapes.or(shape, UP_FACE);
        }
        if (!downOpen) {
            shape = Shapes.or(shape, DOWN_FACE);
        }
        return shape.optimize();
    }

    static VoxelShape getShape(int mask) {
        if (mask < 0 || mask >= SHAPES.length) {
            return SHAPES[0];
        }
        return SHAPES[mask];
    }
}
