package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.item.CustomRenderedArmorItem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.mixin.client.accessor.HumanoidArmorLayerAtlasAccessor;
import net.ty.createcraftedbeginning.registry.CCBArmorMaterials;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightChestplateArmorItem extends ArmorItem implements CustomRenderedArmorItem {
    private static final ResourceLocation INNER_TEXTURE = CreateCraftedBeginning.asResource("textures/models/armor/airtight_layer_2.png");
    private static final ResourceLocation OUTER_TEXTURE = CreateCraftedBeginning.asResource("textures/models/armor/airtight_layer_1.png");

    public AirtightChestplateArmorItem(Type type, Properties properties) {
        super(CCBArmorMaterials.AIRTIGHT, type, properties.stacksTo(1));
    }

    private static void renderModel(PoseStack poseStack, MultiBufferSource bufferSource, int light, Model model, ResourceLocation texture) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.armorCutoutNoCull(texture));
        model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, -1);
    }

    private static void renderTrim(TextureAtlas trimAtlas, PoseStack poseStack, MultiBufferSource bufferSource, int light, ArmorTrim trim, Model model, boolean inner) {
        Holder<ArmorMaterial> material = CCBArmorMaterials.AIRTIGHT;
        ResourceLocation texture = inner ? trim.innerTexture(material) : trim.outerTexture(material);
        TextureAtlasSprite sprite = trimAtlas.getSprite(texture);
        VertexConsumer consumer = sprite.wrap(bufferSource.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal())));
        model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
    }

    private static void renderGlint(PoseStack poseStack, MultiBufferSource bufferSource, int light, Model model) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.armorEntityGlint());
        model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
    }

    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void renderArmorPiece(HumanoidArmorLayer<?, ?, ?> layer, PoseStack poseStack, MultiBufferSource bufferSource, LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<?> originalModel, ItemStack stack) {
        if (!stack.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return;
        }

        HumanoidArmorLayerAtlasAccessor accessor = (HumanoidArmorLayerAtlasAccessor) layer;
        HumanoidModel<?> parentModel = layer.getParentModel();

        HumanoidModel<?> innerModel = accessor.getInnerModel();
        parentModel.copyPropertiesTo((HumanoidModel) innerModel);
        accessor.ccb$setPartVisibility(innerModel, slot);
        renderModel(poseStack, bufferSource, light, innerModel, INNER_TEXTURE);

        HumanoidModel<?> outerModel = accessor.getOuterModel();
        parentModel.copyPropertiesTo((HumanoidModel) outerModel);
        accessor.ccb$setPartVisibility(outerModel, slot);
        renderModel(poseStack, bufferSource, light, outerModel, OUTER_TEXTURE);

        ArmorTrim trim = stack.get(DataComponents.TRIM);
        if (trim != null) {
            TextureAtlas trimAtlas = accessor.getArmorTrimAtlas();
            renderTrim(trimAtlas, poseStack, bufferSource, light, trim, outerModel, false);
            renderTrim(trimAtlas, poseStack, bufferSource, light, trim, innerModel, true);
        }

        if (!stack.hasFoil()) {
            return;
        }

        renderGlint(poseStack, bufferSource, light, outerModel);
        renderGlint(poseStack, bufferSource, light, innerModel);
    }
}
