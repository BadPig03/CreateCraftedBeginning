package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTType;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

public class CCBSpriteShifts {
    public static final CTSpriteShiftEntry CINDER_CASING = omni("cinder_casing");

    public static final CTSpriteShiftEntry AIRTIGHT_TANK_TOP = rectangle("airtight_tank/top");
    public static final CTSpriteShiftEntry AIRTIGHT_TANK = rectangle("airtight_tank/vertical");
    public static final CTSpriteShiftEntry CREATIVE_AIRTIGHT_TANK_TOP = rectangle("airtight_tank/creative_top");
    public static final CTSpriteShiftEntry CREATIVE_AIRTIGHT_TANK = rectangle("airtight_tank/creative_vertical");

    @SuppressWarnings("SameParameterValue")
    private static CTSpriteShiftEntry omni(String name) {
        return getCT(AllCTTypes.OMNIDIRECTIONAL, name);
    }

    private static CTSpriteShiftEntry rectangle(String blockTextureName) {
        return CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, CreateCraftedBeginning.asResource("block/" + blockTextureName), CreateCraftedBeginning.asResource("block/" + blockTextureName + "_connected"));
    }

    private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName, String connectedTextureName) {
        return CTSpriteShifter.getCT(type, CreateCraftedBeginning.asResource("block/" + blockTextureName), CreateCraftedBeginning.asResource("block/" + connectedTextureName + "_connected"));
    }

    @SuppressWarnings("SameParameterValue")
    private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName) {
        return getCT(type, blockTextureName, blockTextureName);
    }
}
