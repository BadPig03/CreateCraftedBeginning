package net.ty.createcraftedbeginning.platform.client;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.platform.access.client.HumanoidArmorLayerAccess;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class ArmorLayerRenderBridge {
    private ArmorLayerRenderBridge() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static @Nullable RenderState prepare(HumanoidArmorLayer<?, ?, ?> layer, EquipmentSlot slot) {
        if (!(layer instanceof HumanoidArmorLayerAccess access)) {
            return null;
        }

        HumanoidModel<?> parentModel = layer.getParentModel();
        HumanoidModel<?> innerModel = access.ccb$getInnerModel();
        parentModel.copyPropertiesTo((HumanoidModel) innerModel);
        access.ccb$setPartVisibility(innerModel, slot);

        HumanoidModel<?> outerModel = access.ccb$getOuterModel();
        parentModel.copyPropertiesTo((HumanoidModel) outerModel);
        access.ccb$setPartVisibility(outerModel, slot);
        return new RenderState(innerModel, outerModel, access.ccb$getArmorTrimAtlas());
    }

    public record RenderState(HumanoidModel<?> innerModel, HumanoidModel<?> outerModel, TextureAtlas armorTrimAtlas) {}
}
