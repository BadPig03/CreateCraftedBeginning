package net.ty.createcraftedbeginning.mixin.client.accessor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.EquipmentSlot;
import net.ty.createcraftedbeginning.platform.access.client.HumanoidArmorLayerAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(HumanoidArmorLayer.class)
public interface HumanoidArmorLayerAtlasAccessor extends HumanoidArmorLayerAccess {
    @Override
    @Accessor("innerModel")
    HumanoidModel<?> ccb$getInnerModel();

    @Override
    @Accessor("outerModel")
    HumanoidModel<?> ccb$getOuterModel();

    @Override
    @Accessor("armorTrimAtlas")
    TextureAtlas ccb$getArmorTrimAtlas();

    @Override
    @Invoker("setPartVisibility")
    void ccb$setPartVisibility(HumanoidModel<?> model, EquipmentSlot slot);
}
