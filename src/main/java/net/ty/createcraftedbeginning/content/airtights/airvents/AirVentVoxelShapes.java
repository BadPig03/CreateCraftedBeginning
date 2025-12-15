package net.ty.createcraftedbeginning.content.airtights.airvents;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

public final class AirVentVoxelShapes {
    private static final VoxelShape NORTH_FACE = Shapes.box(0, 0, 0, 1, 1, 0.000125);
    private static final VoxelShape SOUTH_FACE = Shapes.box(0, 0, 0.999875, 1, 1, 1);
    private static final VoxelShape EAST_FACE = Shapes.box(0.999875, 0, 0, 1, 1, 1);
    private static final VoxelShape WEST_FACE = Shapes.box(0, 0, 0, 0.000125, 1, 1);
    private static final VoxelShape UP_FACE = Shapes.box(0, 0.999875, 0, 1, 1, 1);
    private static final VoxelShape DOWN_FACE = Shapes.box(0, 0, 0, 1, 0.000125, 1);
    private static final VoxelShape CENTER = Shapes.box(0.000125, 0.000125, 0.000125, 0.999875, 0.999875, 0.999875);
    private static final VoxelShape SHELL = Shapes.join(Shapes.block(), CENTER, BooleanOp.ONLY_FIRST);
    private static final Map<Integer, VoxelShape> shapes = new HashMap<>();

    static {
        for (int mask = 0; mask < 64; mask++) {
            boolean north = (mask & 1 << Direction.NORTH.get3DDataValue()) != 0;
            boolean south = (mask & 1 << Direction.SOUTH.get3DDataValue()) != 0;
            boolean east = (mask & 1 << Direction.EAST.get3DDataValue()) != 0;
            boolean west = (mask & 1 << Direction.WEST.get3DDataValue()) != 0;
            boolean up = (mask & 1 << Direction.UP.get3DDataValue()) != 0;
            boolean down = (mask & 1 << Direction.DOWN.get3DDataValue()) != 0;
            shapes.put(mask, formShapes(north, south, east, west, up, down));
        }
    }

    private static VoxelShape formShapes(boolean north, boolean south, boolean east, boolean west, boolean up, boolean down) {
        if (north && south && east && west && up & down) {
            return Shapes.empty();
        }

        VoxelShape frame = SHELL;
        if (north) {
            frame = Shapes.join(frame, NORTH_FACE, BooleanOp.ONLY_FIRST);
        }
        if (south) {
            frame = Shapes.join(frame, SOUTH_FACE, BooleanOp.ONLY_FIRST);
        }
        if (east) {
            frame = Shapes.join(frame, EAST_FACE, BooleanOp.ONLY_FIRST);
        }
        if (west) {
            frame = Shapes.join(frame, WEST_FACE, BooleanOp.ONLY_FIRST);
        }
        if (up) {
            frame = Shapes.join(frame, UP_FACE, BooleanOp.ONLY_FIRST);
        }
        if (down) {
            frame = Shapes.join(frame, DOWN_FACE, BooleanOp.ONLY_FIRST);
        }
        return frame;
    }

    public static VoxelShape getShape(int mask) {
        return shapes.getOrDefault(mask, SHELL);
    }
}
