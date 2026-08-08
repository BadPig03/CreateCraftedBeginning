package net.ty.createcraftedbeginning.mixin.client.accessor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.ty.createcraftedbeginning.platform.access.HumanoidArmorLayerAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(HumanoidArmorLayer.class)
public interface HumanoidArmorLayerAtlasAccessor extends HumanoidArmorLayerAccess {
    @Accessor("ARMOR_LOCATION_CACHE")
    static Map<String, ResourceLocation> getArmorLocationCache() {
        throw new RuntimeException();
    }

    @Override
    @Accessor("innerModel")
    HumanoidModel<?> getInnerModel();

    @Override
    @Accessor("outerModel")
    HumanoidModel<?> getOuterModel();

    @Override
    @Accessor("armorTrimAtlas")
    TextureAtlas getArmorTrimAtlas();

    @Override
    @Invoker("setPartVisibility")
    void ccb$setPartVisibility(HumanoidModel<?> model, EquipmentSlot slot);
}
