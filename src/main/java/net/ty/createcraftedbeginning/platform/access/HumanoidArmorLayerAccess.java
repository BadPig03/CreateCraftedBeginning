package net.ty.createcraftedbeginning.platform.access;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.EquipmentSlot;

public interface HumanoidArmorLayerAccess {
    HumanoidModel<?> getInnerModel();

    HumanoidModel<?> getOuterModel();

    TextureAtlas getArmorTrimAtlas();

    void ccb$setPartVisibility(HumanoidModel<?> model, EquipmentSlot slot);
}
