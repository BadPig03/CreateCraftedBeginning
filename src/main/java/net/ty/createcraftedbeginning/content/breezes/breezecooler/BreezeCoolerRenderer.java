package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeCoolerRenderer extends SmartBlockEntityRenderer<BreezeCoolerBlockEntity> {
    private static final String COMPOUND_KEY_GOGGLES = "Goggles";
    private static final String COMPOUND_KEY_TRAIN_HAT = "TrainHat";

    public BreezeCoolerRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(BreezeCoolerBlockEntity cooler, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        Level level = cooler.getLevel();
        if (level == null) {
            return;
        }

        boolean isChilled = cooler.getFrostLevelFromBlock().isAtLeast(FrostLevel.CHILLED);
        PartialModel hatModel = null;
        if (cooler.hasTrainHat()) {
            hatModel = CCBPartialModels.BREEZE_TRAIN_HAT;
        }
        else if (cooler.isStockKeeper()) {
            hatModel = CCBPartialModels.BREEZE_LOGISTICS_HAT;
        }
        renderShared(poseStack, null, bufferSource, level, cooler.getBlockState(), cooler.getFrostLevelForRender(), cooler.getHeadAnimation().getValue(partialTicks) * 0.175F, AngleHelper.rad(cooler.getHeadAngle().getValue(partialTicks)), cooler.hasGoggles(), hatModel, isChilled, isChilled ? 24 : 0, cooler.hashCode(), light, null);
    }

    public static void renderShared(PoseStack poseStack, @Nullable PoseStack modelTransform, MultiBufferSource bufferSource, Level level, BlockState blockState, FrostLevel frostLevel, float animation, float horizontalAngle, boolean shouldDrawGoggles, @Nullable PartialModel hatModel, boolean shouldDrawWind, float windSpeed, int animationSeed, int light, @Nullable Matrix4f matrixWorld) {
        float renderTime = AnimationTickHolder.getRenderTime(level);
        float headY = Mth.sin((renderTime + animationSeed % 13 * 16) / 16 % Mth.TWO_PI) / (frostLevel.isAtLeast(FrostLevel.CHILLED) ? 64 : 16) - animation * 0.75F;

        poseStack.pushPose();

        PartialModel breezeModel = getBreezeModel(frostLevel, animation > 0.125F);
        SuperByteBuffer breezeBuffer = CachedBuffers.partial(breezeModel, blockState);
        if (modelTransform != null) {
            breezeBuffer.transform(modelTransform);
        }
        breezeBuffer.translate(0, headY, 0);
        breezeBuffer.rotateCentered(horizontalAngle, Direction.UP).light(light).renderInto(poseStack, bufferSource.getBuffer(RenderType.cutoutMipped()));
        if (matrixWorld != null) {
            breezeBuffer.useLevelLight(level, matrixWorld);
        }

        if (shouldDrawGoggles) {
            SuperByteBuffer gogglesBuffer = CachedBuffers.partial(frostLevel.isAtLeast(FrostLevel.CHILLED) ? CCBPartialModels.BREEZE_COOLER_GOGGLES : CCBPartialModels.BREEZE_COOLER_GOGGLES_SMALL, blockState);
            if (modelTransform != null) {
                gogglesBuffer.transform(modelTransform);
            }
            gogglesBuffer.translate(0, headY + 0.5, 0);
            gogglesBuffer.rotateCentered(horizontalAngle, Direction.UP).light(light).renderInto(poseStack, bufferSource.getBuffer(RenderType.solid()));
            if (matrixWorld != null) {
                gogglesBuffer.useLevelLight(level, matrixWorld);
            }
        }

        if (hatModel != null) {
            SuperByteBuffer hatBuffer = CachedBuffers.partial(hatModel, blockState);
            if (modelTransform != null) {
                hatBuffer.transform(modelTransform);
            }
            hatBuffer.translate(0, headY, 0);
            if (breezeModel == CCBPartialModels.BREEZE_RIMING) {
                hatBuffer.translateY(0.5F).center().scale(0.75F).uncenter();
            }
            else {
                hatBuffer.translateY(0.75F);
            }
            hatBuffer.rotateCentered(horizontalAngle + Mth.PI, Direction.UP).translate(0.5, 0, 0.5).light(light).renderInto(poseStack, bufferSource.getBuffer(RenderType.cutoutMipped()));
            if (matrixWorld != null) {
                hatBuffer.useLevelLight(level, matrixWorld);
            }
        }

        if (shouldDrawWind) {
            SuperByteBuffer windBuffer = CachedBuffers.partial(CCBPartialModels.BREEZE_COOLER_WIND, blockState);
            if (modelTransform != null) {
                windBuffer.transform(modelTransform);
            }
            windBuffer.translate(0, headY, 0);
            windBuffer.translate(0.5, 0.5, 0.5).rotateY(horizontalAngle + AngleHelper.rad(renderTime * windSpeed % 360)).translate(-0.5, -0.5, -0.5).light(light).renderInto(poseStack, bufferSource.getBuffer(RenderType.cutout()));
            if (matrixWorld != null) {
                windBuffer.useLevelLight(level, matrixWorld);
            }
        }

        poseStack.popPose();
    }

    static void renderInContraption(MovementContext context, ContraptionMatrices matrices, MultiBufferSource bufferSource, LerpedFloat headAngle, boolean isConductor, VirtualRenderWorld renderWorld) {
        Level level = context.world;
        boolean shouldDrawGoggles = NbtValues.getBooleanOrDefault(context.blockEntityData, COMPOUND_KEY_GOGGLES, false);
        boolean shouldDrawHat = isConductor || NbtValues.getBooleanOrDefault(context.blockEntityData, COMPOUND_KEY_TRAIN_HAT, false);
        renderShared(matrices.getViewProjection(), matrices.getModel(), bufferSource, level, context.state, FrostLevel.CHILLED, 0, AngleHelper.rad(headAngle.getValue(AnimationTickHolder.getPartialTicks(level))), shouldDrawGoggles, shouldDrawHat ? CCBPartialModels.BREEZE_TRAIN_HAT : null, false, 0, context.hashCode(), LevelRenderer.getLightColor(renderWorld, context.localPos), matrices.getWorld());
    }

    static PartialModel getBreezeModel(FrostLevel frostLevel, boolean hasBlockAbove) {
        if (frostLevel.isAtLeast(FrostLevel.CHILLED)) {
            if (hasBlockAbove) {
                return CCBPartialModels.BREEZE_CHILLED_ACTIVE;
            }

            return CCBPartialModels.BREEZE_CHILLED;
        }

        return CCBPartialModels.BREEZE_RIMING;
    }
}
