package net.ty.createcraftedbeginning.gas.visual;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPipeOrientation {
    public static final DirectionProperty PANEL_FACING = DirectionProperty.create("panel_facing");
    private static final VoxelShape[][] SHAPES = new VoxelShape[3][6];

    static {
        for (Axis axis : Iterate.axes) {
            for (Direction panel : Iterate.directions) {
                double x = 8 + panel.getStepX() * 5;
                double y = 8 + panel.getStepY() * 5;
                double z = 8 + panel.getStepZ() * 5;
                double dx = panel.getAxis() == Axis.X ? 2 : 4;
                double dy = panel.getAxis() == Axis.Y ? 2 : 4;
                double dz = panel.getAxis() == Axis.Z ? 2 : 4;
                SHAPES[axis.ordinal()][panel.ordinal()] = Shapes.or(CCBShapes.AIRTIGHT_PIPE.get(axis), Block.box(3, 3, 3, 13, 13, 13), Block.box(x - dx, y - dy, z - dz, x + dx, y + dy, z + dz));
            }
        }
    }

    private GasPipeOrientation() {
    }

    public static Direction panel(BlockState state, Axis axis) {
        Direction panel = state.getValue(PANEL_FACING);
        if (panel.getAxis() != axis) {
            return panel;
        }

        if (axis == Axis.Y) {
            return Direction.NORTH;
        }

        return Direction.UP;
    }

    public static Axis shaft(Axis axis, Direction panel) {
        for (Axis candidate : Iterate.axes) {
            if (candidate != axis && candidate != panel.getAxis()) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Pipe and panel axes must differ.");
    }

    public static VoxelShape shape(Axis axis, Direction panel) {
        return SHAPES[axis.ordinal()][panel.ordinal()];
    }

    public static int rotationX(Axis axis, Direction panel) {
        if (axis == Axis.Y) {
            return 0;
        }

        if (panel == Direction.DOWN) {
            return 270;
        }

        return 90;
    }

    public static int rotationY(Axis axis, Direction panel) {
        if (panel.getAxis() == Axis.Y) {
            if (axis == Axis.X) {
                return 90;
            }

            return 0;
        }

        int rotation = switch (panel) {
            case EAST -> 270;
            case NORTH -> 180;
            case WEST -> 90;
            default -> 0;
        };
        if (axis != Axis.Y) {
            return (rotation + 90) % 360;
        }

        return rotation;
    }
}
