package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTType;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

public class CCBSpriteShifts {
    public static final CTSpriteShiftEntry CINDER_CASING = omni("cinder_casing");

    public static final CTSpriteShiftEntry AIRTIGHT_TANK_TOP = rectangle("airtight_tank/top");
    public static final CTSpriteShiftEntry AIRTIGHT_TANK = rectangle("airtight_tank/vertical");

    public static final SpriteShiftEntry CHAMBER_FLAME = get("block/breeze_chamber_flame", "block/breeze_chamber_flame_scroll");
    public static final SpriteShiftEntry SUPER_CHAMBER_FLAME = get("block/breeze_chamber_flame", "block/breeze_chamber_flame_superheated_scroll");

    private static SpriteShiftEntry get(String originalLocation, String targetLocation) {
        return SpriteShifter.get(Create.asResource(originalLocation), Create.asResource(targetLocation));
    }

    private static CTSpriteShiftEntry omni(String name) {
        return getCT(AllCTTypes.OMNIDIRECTIONAL, name);
    }

    private static CTSpriteShiftEntry rectangle(String blockTextureName) {
        return CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, CreateCraftedBeginning.asResource("block/" + blockTextureName), CreateCraftedBeginning.asResource("block/" + blockTextureName + "_connected"));
    }

    private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName, String connectedTextureName) {
        return CTSpriteShifter.getCT(type, CreateCraftedBeginning.asResource("block/" + blockTextureName), CreateCraftedBeginning.asResource("block/" + connectedTextureName + "_connected"));
    }

    private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName) {
        return getCT(type, blockTextureName, blockTextureName);
    }
}
