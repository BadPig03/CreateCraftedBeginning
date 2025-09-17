package net.ty.createcraftedbeginning.content.airtightcannon;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AirtightCannonProjectileRenderer extends EntityRenderer<AirtightCannonProjectileEntity> {
    public AirtightCannonProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull AirtightCannonProjectileEntity entity, float yaw, float pt, @NotNull PoseStack ms, @NotNull MultiBufferSource buffer, int light) {
        ItemStack item = entity.getItem();
        if (item.isEmpty()) {
            return;
        }
        ms.pushPose();
        ms.translate(0, entity.getBoundingBox().getYsize() / 2 - 1 / 8f, 0);
        entity.getRenderMode().transform(ms, entity, pt);

        Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, ms, buffer, entity.level(), 0);
        ms.popPose();
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AirtightCannonProjectileEntity entity) {
        return null;
    }
}
