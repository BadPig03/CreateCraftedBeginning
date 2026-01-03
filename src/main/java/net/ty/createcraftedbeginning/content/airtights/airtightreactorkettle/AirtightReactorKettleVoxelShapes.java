package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.data.CCBShapes;

import java.util.EnumMap;

public final class AirtightReactorKettleVoxelShapes {
    private static final EnumMap<AirtightReactorKettleStructuralPosition, VoxelShape> shapes = new EnumMap<>(AirtightReactorKettleStructuralPosition.class);

    static {
        shapes.put(AirtightReactorKettleStructuralPosition.TOP_LEFT_UP, CCBShapes.AIRTIGHT_REACTOR_KETTLE_TOP_CORNER.get(Direction.NORTH));
        shapes.put(AirtightReactorKettleStructuralPosition.TOP_MID_UP, CCBShapes.AIRTIGHT_REACTOR_KETTLE_TOP_MID.get(Direction.NORTH));
        shapes.put(AirtightReactorKettleStructuralPosition.TOP_RIGHT_UP, CCBShapes.AIRTIGHT_REACTOR_KETTLE_TOP_CORNER.get(Direction.EAST));
        shapes.put(AirtightReactorKettleStructuralPosition.TOP_LEFT_MID, CCBShapes.AIRTIGHT_REACTOR_KETTLE_TOP_MID.get(Direction.WEST));
        shapes.put(AirtightReactorKettleStructuralPosition.TOP_CENTER, CCBShapes.AIRTIGHT_REACTOR_KETTLE_TOP_CENTER);
        shapes.put(AirtightReactorKettleStructuralPosition.TOP_RIGHT_MID, CCBShapes.AIRTIGHT_REACTOR_KETTLE_TOP_MID.get(Direction.EAST));
        shapes.put(AirtightReactorKettleStructuralPosition.TOP_LEFT_DOWN, CCBShapes.AIRTIGHT_REACTOR_KETTLE_TOP_CORNER.get(Direction.WEST));
        shapes.put(AirtightReactorKettleStructuralPosition.TOP_MID_DOWN, CCBShapes.AIRTIGHT_REACTOR_KETTLE_TOP_MID.get(Direction.SOUTH));
        shapes.put(AirtightReactorKettleStructuralPosition.TOP_RIGHT_DOWN, CCBShapes.AIRTIGHT_REACTOR_KETTLE_TOP_CORNER.get(Direction.SOUTH));
        shapes.put(AirtightReactorKettleStructuralPosition.MID_LEFT_UP, CCBShapes.AIRTIGHT_REACTOR_KETTLE_MID_CORNER.get(Direction.NORTH));
        shapes.put(AirtightReactorKettleStructuralPosition.MID_MID_UP, CCBShapes.AIRTIGHT_REACTOR_KETTLE_MID_MID.get(Direction.NORTH));
        shapes.put(AirtightReactorKettleStructuralPosition.MID_RIGHT_UP, CCBShapes.AIRTIGHT_REACTOR_KETTLE_MID_CORNER.get(Direction.EAST));
        shapes.put(AirtightReactorKettleStructuralPosition.MID_LEFT_MID, CCBShapes.AIRTIGHT_REACTOR_KETTLE_MID_MID.get(Direction.WEST));
        shapes.put(AirtightReactorKettleStructuralPosition.MID_RIGHT_MID, CCBShapes.AIRTIGHT_REACTOR_KETTLE_MID_MID.get(Direction.EAST));
        shapes.put(AirtightReactorKettleStructuralPosition.MID_LEFT_DOWN, CCBShapes.AIRTIGHT_REACTOR_KETTLE_MID_CORNER.get(Direction.WEST));
        shapes.put(AirtightReactorKettleStructuralPosition.MID_MID_DOWN, CCBShapes.AIRTIGHT_REACTOR_KETTLE_MID_MID.get(Direction.SOUTH));
        shapes.put(AirtightReactorKettleStructuralPosition.MID_RIGHT_DOWN, CCBShapes.AIRTIGHT_REACTOR_KETTLE_MID_CORNER.get(Direction.SOUTH));
        shapes.put(AirtightReactorKettleStructuralPosition.BOTTOM_LEFT_UP, CCBShapes.AIRTIGHT_REACTOR_KETTLE_BOTTOM_CORNER.get(Direction.NORTH));
        shapes.put(AirtightReactorKettleStructuralPosition.BOTTOM_MID_UP, CCBShapes.AIRTIGHT_REACTOR_KETTLE_BOTTOM_MID.get(Direction.NORTH));
        shapes.put(AirtightReactorKettleStructuralPosition.BOTTOM_RIGHT_UP, CCBShapes.AIRTIGHT_REACTOR_KETTLE_BOTTOM_CORNER.get(Direction.EAST));
        shapes.put(AirtightReactorKettleStructuralPosition.BOTTOM_LEFT_MID, CCBShapes.AIRTIGHT_REACTOR_KETTLE_BOTTOM_MID.get(Direction.WEST));
        shapes.put(AirtightReactorKettleStructuralPosition.BOTTOM_CENTER, CCBShapes.AIRTIGHT_REACTOR_KETTLE_BOTTOM_CENTER);
        shapes.put(AirtightReactorKettleStructuralPosition.BOTTOM_RIGHT_MID, CCBShapes.AIRTIGHT_REACTOR_KETTLE_BOTTOM_MID.get(Direction.EAST));
        shapes.put(AirtightReactorKettleStructuralPosition.BOTTOM_LEFT_DOWN, CCBShapes.AIRTIGHT_REACTOR_KETTLE_BOTTOM_CORNER.get(Direction.WEST));
        shapes.put(AirtightReactorKettleStructuralPosition.BOTTOM_MID_DOWN, CCBShapes.AIRTIGHT_REACTOR_KETTLE_BOTTOM_MID.get(Direction.SOUTH));
        shapes.put(AirtightReactorKettleStructuralPosition.BOTTOM_RIGHT_DOWN, CCBShapes.AIRTIGHT_REACTOR_KETTLE_BOTTOM_CORNER.get(Direction.SOUTH));
    }

    public static VoxelShape getShape(AirtightReactorKettleStructuralPosition structuralPosition) {
        return shapes.getOrDefault(structuralPosition, Shapes.block());
    }
}
