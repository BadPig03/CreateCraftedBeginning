package net.ty.createcraftedbeginning.client.render;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBSpriteShifts {
    public static final CTSpriteShiftEntry CONNECTED_INDUSTRIAL_IRON_BLOCK = omni("industrial_iron/connected_industrial_iron_block");
    public static final CTSpriteShiftEntry INDUSTRIAL_IRON_PILLAR = pillar("industrial_iron_pillar");
    public static final CTSpriteShiftEntry EXPOSED_INDUSTRIAL_IRON_PILLAR = pillar("exposed_industrial_iron_pillar");
    public static final CTSpriteShiftEntry WEATHERED_INDUSTRIAL_IRON_PILLAR = pillar("weathered_industrial_iron_pillar");
    public static final CTSpriteShiftEntry END_CASING = omni("end_casing");
    public static final CTSpriteShiftEntry INDUSTRIAL_IRON_BLOCK_SCAFFOLD = horizontal("industrial_iron_block_scaffolding/side");
    public static final CTSpriteShiftEntry INDUSTRIAL_IRON_BLOCK_SCAFFOLD_INSIDE = horizontal("industrial_iron_block_scaffolding/inside");
    public static final CTSpriteShiftEntry INDUSTRIAL_IRON_BLOCK_SCAFFOLD_TOP = omni("industrial_iron_block_scaffolding/top");
    public static final CTSpriteShiftEntry AIR_VENT = omni("air_vent/air_vent");

    public static final CTSpriteShiftEntry AIRTIGHT_PIPE_CASING = getCT("airtight_pipe/casing_single", "airtight_pipe/casing");
    public static final CTSpriteShiftEntry AIRTIGHT_METER_CASING = AIRTIGHT_PIPE_CASING;

    public static final CTSpriteShiftEntry AIRTIGHT_TANK_TOP = rectangle("airtight_tank/top");
    public static final CTSpriteShiftEntry AIRTIGHT_TANK = rectangle("airtight_tank/vertical");
    public static final CTSpriteShiftEntry AIRTIGHT_FRACTIONATION_TOWER_TOP = CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, CCBAPI.asResource("block/airtight_fractionation_tower/top_connected"), CCBAPI.asResource("block/airtight_fractionation_tower/top_connected"));
    public static final CTSpriteShiftEntry AIRTIGHT_FRACTIONATION_TOWER = CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, CCBAPI.asResource("block/airtight_fractionation_tower/vertical_connected"), CCBAPI.asResource("block/airtight_fractionation_tower/vertical_connected"));
    public static final CTSpriteShiftEntry HORIZONTAL_AIRTIGHT_TANK = rectangle("airtight_tank/horizontal/vertical");
    public static final CTSpriteShiftEntry CREATIVE_AIRTIGHT_TANK_TOP = rectangle("airtight_tank/creative_top");
    public static final CTSpriteShiftEntry CREATIVE_AIRTIGHT_TANK = rectangle("airtight_tank/creative_vertical");

    private static CTSpriteShiftEntry omni(String blockTextureName) {
        return getCT(blockTextureName, blockTextureName);
    }

    private static CTSpriteShiftEntry getCT(String blockTextureName, String connectedTextureName) {
        return CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL, CCBAPI.asResource("block/" + blockTextureName), CCBAPI.asResource("block/" + connectedTextureName + "_connected"));
    }

    private static CTSpriteShiftEntry horizontal(String blockTextureName) {
        return CTSpriteShifter.getCT(AllCTTypes.HORIZONTAL, CCBAPI.asResource("block/" + blockTextureName), CCBAPI.asResource("block/" + blockTextureName + "_connected"));
    }

    private static CTSpriteShiftEntry rectangle(String blockTextureName) {
        return CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, CCBAPI.asResource("block/" + blockTextureName), CCBAPI.asResource("block/" + blockTextureName + "_connected"));
    }

    private static CTSpriteShiftEntry pillar(String name) {
        String texturePath = "block/industrial_iron/" + name + '/';
        return CTSpriteShifter.getCT(AllCTTypes.VERTICAL, CCBAPI.asResource(texturePath + "single"), CCBAPI.asResource(texturePath + "side_connected"));
    }
}
