package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.item.CustomRenderedArmorItem;
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
import net.ty.createcraftedbeginning.mixin.accessor.HumanoidArmorLayerAtlasAccessor;
import net.ty.createcraftedbeginning.registry.CCBArmorMaterials;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

public class AirtightChestplateArmorItem extends ArmorItem implements CustomRenderedArmorItem {
    protected final ResourceLocation textureLoc = CreateCraftedBeginning.asResource("airtight");

    public AirtightChestplateArmorItem(Type type, @NotNull Properties properties) {
        super(CCBArmorMaterials.AIRTIGHT, type, properties.stacksTo(1));
    }

    private static void renderModel(PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int light, @NotNull Model model, ResourceLocation armorResource) {
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.armorCutoutNoCull(armorResource));
        model.renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, -1);
    }

    private static void renderTrim(@NotNull TextureAtlas armorTrimAtlas, PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int light, @NotNull ArmorTrim trim, @NotNull Model model, boolean inner) {
        Holder<ArmorMaterial> materialHolder = CCBArmorMaterials.AIRTIGHT;
        TextureAtlasSprite sprite = armorTrimAtlas.getSprite(inner ? trim.innerTexture(materialHolder) : trim.outerTexture(materialHolder));
        VertexConsumer vertexConsumer = sprite.wrap(bufferSource.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal())));
        model.renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
    }

    private static void renderGlint(PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int light, @NotNull Model model) {
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.armorEntityGlint());
        model.renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
    }

    public String getArmorTextureLocation(int layer) {
        return String.format(Locale.ROOT, "%s:textures/models/armor/%s_layer_%d.png", textureLoc.getNamespace(), textureLoc.getPath(), layer);
    }

    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void renderArmorPiece(HumanoidArmorLayer<?, ?, ?> layer, PoseStack poseStack, MultiBufferSource bufferSource, LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<?> originalModel, @NotNull ItemStack stack) {
        if (!stack.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return;
        }

        HumanoidArmorLayerAtlasAccessor accessor = (HumanoidArmorLayerAtlasAccessor) layer;
        Map<String, ResourceLocation> locationCache = HumanoidArmorLayerAtlasAccessor.getArmorLocationCache();
        HumanoidModel<?> parentModel = layer.getParentModel();

        HumanoidModel<?> innerModel = accessor.getInnerModel();
        parentModel.copyPropertiesTo((HumanoidModel) innerModel);
        accessor.callSetPartVisibility(innerModel, slot);
        renderModel(poseStack, bufferSource, light, innerModel, locationCache.computeIfAbsent(getArmorTextureLocation(2), ResourceLocation::parse));

        HumanoidModel<?> outerModel = accessor.getOuterModel();
        parentModel.copyPropertiesTo((HumanoidModel) outerModel);
        accessor.callSetPartVisibility(outerModel, slot);
        renderModel(poseStack, bufferSource, light, outerModel, locationCache.computeIfAbsent(getArmorTextureLocation(1), ResourceLocation::parse));

        ArmorTrim trim = stack.get(DataComponents.TRIM);
        if (trim != null) {
            TextureAtlas textureAtlas = accessor.getArmorTrimAtlas();
            renderTrim(textureAtlas, poseStack, bufferSource, light, trim, outerModel, false);
            renderTrim(textureAtlas, poseStack, bufferSource, light, trim, innerModel, true);
        }

        if (stack.hasFoil()) {
            renderGlint(poseStack, bufferSource, light, outerModel);
            renderGlint(poseStack, bufferSource, light, innerModel);
        }
    }
}
