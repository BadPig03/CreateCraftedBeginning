package net.ty.createcraftedbeginning.data;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBSpriteShifts {
    public static final CTSpriteShiftEntry END_CASING = omni("end_casing");
    public static final CTSpriteShiftEntry AIR_VENT = omni("air_vent/air_vent");

    public static final CTSpriteShiftEntry AIRTIGHT_TANK_TOP = rectangle("airtight_tank/top");
    public static final CTSpriteShiftEntry AIRTIGHT_TANK = rectangle("airtight_tank/vertical");
    public static final CTSpriteShiftEntry CREATIVE_AIRTIGHT_TANK_TOP = rectangle("airtight_tank/creative_top");
    public static final CTSpriteShiftEntry CREATIVE_AIRTIGHT_TANK = rectangle("airtight_tank/creative_vertical");

    private static CTSpriteShiftEntry omni(String name) {
        return getCT(name);
    }

    private static CTSpriteShiftEntry getCT(String blockTextureName) {
        return getCT(blockTextureName, blockTextureName);
    }

    private static CTSpriteShiftEntry getCT(String blockTextureName, String connectedTextureName) {
        return CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL, CreateCraftedBeginning.asResource("block/" + blockTextureName), CreateCraftedBeginning.asResource("block/" + connectedTextureName + "_connected"));
    }

    private static CTSpriteShiftEntry rectangle(String blockTextureName) {
        return CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, CreateCraftedBeginning.asResource("block/" + blockTextureName), CreateCraftedBeginning.asResource("block/" + blockTextureName + "_connected"));
    }
}
