package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.data.CCBShapes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightForgingPressVoxelShapes {
    private static final EnumMap<AirtightForgingPressStructuralPosition, VoxelShape> SHAPES_MAP = new EnumMap<>(AirtightForgingPressStructuralPosition.class);

    static {
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.TOP_LEFT_UP, CCBShapes.AIRTIGHT_FORGING_PRESS_TOP_CORNER.get(Direction.NORTH));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.TOP_MID_UP, CCBShapes.AIRTIGHT_FORGING_PRESS_TOP_MID.get(Direction.NORTH));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.TOP_RIGHT_UP, CCBShapes.AIRTIGHT_FORGING_PRESS_TOP_CORNER.get(Direction.EAST));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.TOP_LEFT_MID, CCBShapes.AIRTIGHT_FORGING_PRESS_TOP_MID.get(Direction.WEST));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.TOP_CENTER, CCBShapes.AIRTIGHT_FORGING_PRESS_TOP_CENTER_SHAPE);
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.TOP_RIGHT_MID, CCBShapes.AIRTIGHT_FORGING_PRESS_TOP_MID.get(Direction.EAST));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.TOP_LEFT_DOWN, CCBShapes.AIRTIGHT_FORGING_PRESS_TOP_CORNER.get(Direction.WEST));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.TOP_MID_DOWN, CCBShapes.AIRTIGHT_FORGING_PRESS_TOP_MID.get(Direction.SOUTH));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.TOP_RIGHT_DOWN, CCBShapes.AIRTIGHT_FORGING_PRESS_TOP_CORNER.get(Direction.SOUTH));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.MID_LEFT_UP, CCBShapes.AIRTIGHT_FORGING_PRESS_MID_CORNER.get(Direction.NORTH));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.MID_MID_UP, Shapes.empty());
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.MID_RIGHT_UP, CCBShapes.AIRTIGHT_FORGING_PRESS_MID_CORNER.get(Direction.EAST));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.MID_LEFT_MID, Shapes.empty());
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.MID_RIGHT_MID, Shapes.empty());
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.MID_LEFT_DOWN, CCBShapes.AIRTIGHT_FORGING_PRESS_MID_CORNER.get(Direction.WEST));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.MID_MID_DOWN, Shapes.empty());
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.MID_RIGHT_DOWN, CCBShapes.AIRTIGHT_FORGING_PRESS_MID_CORNER.get(Direction.SOUTH));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.BOTTOM_LEFT_UP, CCBShapes.AIRTIGHT_FORGING_PRESS_BOTTOM_CORNER.get(Direction.NORTH));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.BOTTOM_MID_UP, CCBShapes.AIRTIGHT_FORGING_PRESS_BOTTOM_MID.get(Direction.NORTH));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.BOTTOM_RIGHT_UP, CCBShapes.AIRTIGHT_FORGING_PRESS_BOTTOM_CORNER.get(Direction.EAST));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.BOTTOM_LEFT_MID, CCBShapes.AIRTIGHT_FORGING_PRESS_BOTTOM_MID.get(Direction.WEST));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.BOTTOM_CENTER, CCBShapes.AIRTIGHT_FORGING_PRESS_BOTTOM_CENTER_SHAPE);
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.BOTTOM_RIGHT_MID, CCBShapes.AIRTIGHT_FORGING_PRESS_BOTTOM_MID.get(Direction.EAST));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.BOTTOM_LEFT_DOWN, CCBShapes.AIRTIGHT_FORGING_PRESS_BOTTOM_CORNER.get(Direction.WEST));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.BOTTOM_MID_DOWN, CCBShapes.AIRTIGHT_FORGING_PRESS_BOTTOM_MID.get(Direction.SOUTH));
        SHAPES_MAP.put(AirtightForgingPressStructuralPosition.BOTTOM_RIGHT_DOWN, CCBShapes.AIRTIGHT_FORGING_PRESS_BOTTOM_CORNER.get(Direction.SOUTH));
    }

    public static VoxelShape getShape(AirtightForgingPressStructuralPosition structuralPosition) {
        return SHAPES_MAP.getOrDefault(structuralPosition, Shapes.block());
    }
}
