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
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.AirtightCannonVisualHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.AirtightCannonVisualHandlerUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.IdentityHashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class AirtightCannonWindChargeProjectileEntityRenderer extends EntityRenderer<AirtightCannonWindChargeProjectileEntity> {
    private static final double MIN_CAMERA_DISTANCE_SQUARED = 16.0d;
    private final Map<AirtightCannonVisualHandler, AirtightCannonWindChargeModel> models = new IdentityHashMap<>();

    public AirtightCannonWindChargeProjectileEntityRenderer(Context context) {
        super(context);
    }

    @Override
    public void render(AirtightCannonWindChargeProjectileEntity windCharge, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        if (windCharge.tickCount < 4 && entityRenderDispatcher.camera.getEntity().distanceToSqr(windCharge) < MIN_CAMERA_DISTANCE_SQUARED) {
            return;
        }

        AirtightCannonVisualHandler handler = AirtightCannonVisualHandlerUtils.of(windCharge.getGasHolder().value());
        AirtightCannonWindChargeModel model = models.computeIfAbsent(handler, visualHandler -> new AirtightCannonWindChargeModel(AirtightCannonWindChargeModel.createLayerDefinition(visualHandler.getModelType()).bakeRoot()));
        float animationTick = windCharge.tickCount + partialTick;
        model.setupAnimation(handler.getAnimationType(), handler.getRotationSpeed(), animationTick);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.breezeWind(handler.getTextureLocation(), animationTick * 0.03f % 1.0f, 0.0f));
        model.renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
        super.render(windCharge, entityYaw, partialTick, poseStack, bufferSource, light);
    }

    @Override
    public ResourceLocation getTextureLocation(AirtightCannonWindChargeProjectileEntity entity) {
        return AirtightCannonVisualHandlerUtils.of(entity.getGasHolder().value()).getTextureLocation();
    }
}
