package net.ty.createcraftedbeginning.content.airtights.airvents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirVentVoxelShapes {
    private static final double THICKNESS = 0.0125;
    private static final VoxelShape NORTH_FACE = Shapes.box(0, 0, 0, 1, 1, THICKNESS);
    private static final VoxelShape SOUTH_FACE = Shapes.box(0, 0, 1 - THICKNESS, 1, 1, 1);
    private static final VoxelShape WEST_FACE = Shapes.box(0, 0, 0, THICKNESS, 1, 1);
    private static final VoxelShape EAST_FACE = Shapes.box(1 - THICKNESS, 0, 0, 1, 1, 1);
    private static final VoxelShape DOWN_FACE = Shapes.box(0, 0, 0, 1, THICKNESS, 1);
    private static final VoxelShape UP_FACE = Shapes.box(0, 1 - THICKNESS, 0, 1, 1, 1);
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        for (int mask = 0; mask < SHAPES.length; mask++) {
            SHAPES[mask] = createShape(hasOpening(mask, Direction.NORTH), hasOpening(mask, Direction.SOUTH), hasOpening(mask, Direction.EAST), hasOpening(mask, Direction.WEST), hasOpening(mask, Direction.UP), hasOpening(mask, Direction.DOWN));
        }
    }

    private static boolean hasOpening(int mask, Direction direction) {
        return (mask & 1 << direction.get3DDataValue()) != 0;
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

    public static VoxelShape getShape(int mask) {
        if (mask < 0 || mask >= SHAPES.length) {
            return SHAPES[0];
        }
        return SHAPES[mask];
    }
}
