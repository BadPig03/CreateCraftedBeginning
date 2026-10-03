package net.ty.createcraftedbeginning.content.breezes.breezechamber;

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
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeChamberRenderer extends SmartBlockEntityRenderer<BreezeChamberBlockEntity> {
    private static final String COMPOUND_KEY_GOGGLES = "Goggles";
    private static final String COMPOUND_KEY_TRAIN_HAT = "TrainHat";

    public BreezeChamberRenderer(Context context) {
        super(context);
    }

    static void renderInContraption(MovementContext context, ContraptionMatrices matrices, MultiBufferSource bufferSource, LerpedFloat headAngle, boolean isConductor, VirtualRenderWorld renderWorld) {
        Level level = context.world;
        boolean drawGoggles = NbtValues.getBooleanOrDefault(context.blockEntityData, COMPOUND_KEY_GOGGLES, false);
        boolean shouldDrawHat = isConductor || NbtValues.getBooleanOrDefault(context.blockEntityData, COMPOUND_KEY_TRAIN_HAT, false);
        renderShared(matrices.getViewProjection(), matrices.getModel(), bufferSource, level, context.state, WindLevel.GALE, 0, AngleHelper.rad(headAngle.getValue(AnimationTickHolder.getPartialTicks(level))), drawGoggles, shouldDrawHat ? CCBPartialModels.BREEZE_TRAIN_HAT : null, false, 0, context.hashCode(), LevelRenderer.getLightColor(renderWorld, context.localPos), matrices.getWorld());
    }

    static void renderShared(PoseStack poseStack, @Nullable PoseStack modelTransform, MultiBufferSource bufferSource, Level level, BlockState blockState, WindLevel windLevel, float animation, float horizontalAngle, boolean drawGoggles, @Nullable PartialModel hatModel, boolean drawWind, float windSpeed, int animationSeed, int light, @Nullable Matrix4f matrixWorld) {
        float renderTime = AnimationTickHolder.getRenderTime(level);
        float headY = Mth.sin((renderTime + animationSeed % 13 * 16) / 16 % Mth.TWO_PI) / (windLevel.isActive() ? 64 : 16) - animation * 0.75F;

        poseStack.pushPose();

        PartialModel breezeModel = getBreezeModel(windLevel, animation > 0.125F);
        SuperByteBuffer breezeBuffer = CachedBuffers.partial(breezeModel, blockState);
        if (modelTransform != null) {
            breezeBuffer.transform(modelTransform);
        }
        breezeBuffer.translate(0, headY - 0.125, 0);
        breezeBuffer.rotateCentered(horizontalAngle, Direction.UP).light(light).renderInto(poseStack, bufferSource.getBuffer(RenderType.cutoutMipped()));
        if (matrixWorld != null) {
            breezeBuffer.useLevelLight(level, matrixWorld);
        }

        if (drawGoggles) {
            SuperByteBuffer gogglesBuffer = CachedBuffers.partial(windLevel.isActive() ? CCBPartialModels.BREEZE_CHAMBER_GOGGLES : CCBPartialModels.BREEZE_CHAMBER_GOGGLES_SMALL, blockState);
            if (modelTransform != null) {
                gogglesBuffer.transform(modelTransform);
            }
            gogglesBuffer.translate(0, headY + 0.375, 0);
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
            hatBuffer.translate(0, headY - 0.125, 0);
            if (breezeModel == CCBPartialModels.BREEZE_CALM) {
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

        if (drawWind) {
            SuperByteBuffer windBuffer = CachedBuffers.partial(CCBPartialModels.BREEZE_CHAMBER_WIND, blockState);
            if (modelTransform != null) {
                windBuffer.transform(modelTransform);
            }
            windBuffer.translate(0, headY - 0.125, 0);
            windBuffer.translate(0.5, 0.5, 0.5).rotateY(horizontalAngle + AngleHelper.rad(renderTime * windSpeed % 360)).translate(-0.5, -0.5, -0.5).light(light).renderInto(poseStack, bufferSource.getBuffer(RenderType.cutout()));
            if (matrixWorld != null) {
                windBuffer.useLevelLight(level, matrixWorld);
            }
        }

        poseStack.popPose();
    }

    static PartialModel getBreezeModel(WindLevel windLevel, boolean useActiveModel) {
        if (windLevel.isActive()) {
            if (useActiveModel) {
                return CCBPartialModels.BREEZE_GALE_ACTIVE;
            }

            return CCBPartialModels.BREEZE_GALE;
        }

        if (windLevel == WindLevel.CALM) {
            return CCBPartialModels.BREEZE_CALM;
        }

        return CCBPartialModels.BREEZE_ILL;
    }

    @Override
    protected void renderSafe(BreezeChamberBlockEntity chamber, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        Level level = chamber.getLevel();
        if (level == null) {
            return;
        }

        WindLevel windLevel = chamber.getWindLevel();
        boolean isGale = windLevel.isActive();
        renderShared(poseStack, null, bufferSource, level, chamber.getBlockState(), chamber.getWindLevelForRender(), chamber.getHeadAnimation().getValue(partialTicks) * 0.175F, AngleHelper.rad(chamber.getHeadAngle().getValue(partialTicks)), chamber.hasGoggles(), chamber.hasTrainHat() ? CCBPartialModels.BREEZE_TRAIN_HAT : null, isGale, isGale ? 24 : 0, chamber.hashCode(), light, null);
    }
}
