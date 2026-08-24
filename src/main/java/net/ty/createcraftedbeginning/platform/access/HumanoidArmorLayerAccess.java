package net.ty.createcraftedbeginning.platform.access;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.EquipmentSlot;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface HumanoidArmorLayerAccess {
    HumanoidModel<?> ccb$getInnerModel();

    HumanoidModel<?> ccv$getOuterModel();

    TextureAtlas ccb$getArmorTrimAtlas();

    void ccb$setPartVisibility(HumanoidModel<?> model, EquipmentSlot slot);
}
