package net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlerUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class AirtightCannonWindChargeProjectileEntityRenderer extends EntityRenderer<AirtightCannonWindChargeProjectileEntity> {
    private static final double MIN_CAMERA_DISTANCE_SQUARED = 16.0d;

    public AirtightCannonWindChargeProjectileEntityRenderer(Context context) {
        super(context);
    }

    @Override
    public void render(AirtightCannonWindChargeProjectileEntity windCharge, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        if (windCharge.tickCount < 4 && entityRenderDispatcher.camera.getEntity().distanceToSqr(windCharge) < MIN_CAMERA_DISTANCE_SQUARED) {
            return;
        }

        AirtightCannonWindChargeModel model = windCharge.getWindChargeModel();
        if (model == null) {
            return;
        }

        float tick = (float) windCharge.tickCount + partialTick;
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.breezeWind(getTextureLocation(windCharge), tick * 0.03f % 1.0f, 0.0f));
        model.setupAnim(windCharge, 0.0f, 0.0f, tick, 0.0f, 0.0f);
        model.renderToBuffer(poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
        super.render(windCharge, entityYaw, partialTick, poseStack, bufferSource, light);
    }

    @Override
    public ResourceLocation getTextureLocation(AirtightCannonWindChargeProjectileEntity entity) {
        AirtightCannonHandler cannonHandler = AirtightCannonHandlerUtils.of(entity.getGasHolder().value());
        return cannonHandler.getTextureLocation();
    }
}
