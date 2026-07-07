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
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
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

    public static void renderInContraption(MovementContext context, ContraptionMatrices matrices, MultiBufferSource bufferSource, LerpedFloat headAngle, boolean conductor, VirtualRenderWorld renderWorld) {
        Level level = context.world;
        boolean drawGoggles = context.blockEntityData.contains(COMPOUND_KEY_GOGGLES) && context.blockEntityData.getBoolean(COMPOUND_KEY_GOGGLES);
        boolean drawHat = conductor || context.blockEntityData.contains(COMPOUND_KEY_TRAIN_HAT) && context.blockEntityData.getBoolean(COMPOUND_KEY_TRAIN_HAT);
        renderShared(matrices.getViewProjection(), matrices.getModel(), bufferSource, level, context.state, FrostLevel.CHILLED, 0, AngleHelper.rad(headAngle.getValue(AnimationTickHolder.getPartialTicks(level))), drawGoggles, drawHat ? CCBPartialModels.BREEZE_TRAIN_HAT : null, false, 0, context.hashCode(), LevelRenderer.getLightColor(renderWorld, context.localPos), matrices.getWorld());
    }

    public static void renderShared(PoseStack ms, @Nullable PoseStack modelTransform, MultiBufferSource bufferSource, Level level, BlockState blockState, FrostLevel frostLevel, float animation, float horizontalAngle, boolean drawGoggles, @Nullable PartialModel drawHat, boolean drawWind, float windSpeed, int hashCode, int light, @Nullable Matrix4f matrixWorld) {
        float renderTime = AnimationTickHolder.getRenderTime(level);
        float headY = Mth.sin((renderTime + (hashCode % 13) * 16.0f) / 16.0f % Mth.TWO_PI) / (frostLevel.isAtLeast(FrostLevel.CHILLED) ? 64 : 16) - animation * 0.75f;

        ms.pushPose();

        PartialModel breezeModel = getBreezeModel(frostLevel, animation > 0.125f);
        SuperByteBuffer breezeBuffer = CachedBuffers.partial(breezeModel, blockState);
        if (modelTransform != null) {
            breezeBuffer.transform(modelTransform);
        }
        breezeBuffer.translate(0, headY, 0);
        breezeBuffer.rotateCentered(horizontalAngle, Direction.UP).light(light).renderInto(ms, bufferSource.getBuffer(RenderType.cutoutMipped()));
        if (matrixWorld != null) {
            breezeBuffer.useLevelLight(level, matrixWorld);
        }

        if (drawGoggles) {
            SuperByteBuffer gogglesBuffer = CachedBuffers.partial(frostLevel.isAtLeast(FrostLevel.CHILLED) ? CCBPartialModels.BREEZE_COOLER_GOGGLES : CCBPartialModels.BREEZE_COOLER_GOGGLES_SMALL, blockState);
            if (modelTransform != null) {
                gogglesBuffer.transform(modelTransform);
            }
            gogglesBuffer.translate(0, headY + 0.5f, 0);
            gogglesBuffer.rotateCentered(horizontalAngle, Direction.UP).light(light).renderInto(ms, bufferSource.getBuffer(RenderType.solid()));
            if (matrixWorld != null) {
                gogglesBuffer.useLevelLight(level, matrixWorld);
            }
        }

        if (drawHat != null) {
            SuperByteBuffer hatBuffer = CachedBuffers.partial(drawHat, blockState);
            if (modelTransform != null) {
                hatBuffer.transform(modelTransform);
            }
            hatBuffer.translate(0, headY, 0);
            if (breezeModel == CCBPartialModels.BREEZE_RIMING) {
                hatBuffer.translateY(0.5f).center().scale(0.75f).uncenter();
            }
            else {
                hatBuffer.translateY(0.75f);
            }
            hatBuffer.rotateCentered(horizontalAngle + Mth.PI, Direction.UP).translate(0.5f, 0, 0.5f).light(light).renderInto(ms, bufferSource.getBuffer(RenderType.cutoutMipped()));
            if (matrixWorld != null) {
                hatBuffer.useLevelLight(level, matrixWorld);
            }
        }

        if (drawWind) {
            SuperByteBuffer windBuffer = CachedBuffers.partial(CCBPartialModels.BREEZE_COOLER_WIND, blockState);
            if (modelTransform != null) {
                windBuffer.transform(modelTransform);
            }
            windBuffer.translate(0, headY, 0);
            windBuffer.translate(0.5f, 0.5f, 0.5f).rotateY(horizontalAngle + AngleHelper.rad(renderTime * windSpeed % 360)).translate(-0.5f, -0.5f, -0.5f).light(light).renderInto(ms, bufferSource.getBuffer(RenderType.cutout()));
            if (matrixWorld != null) {
                windBuffer.useLevelLight(level, matrixWorld);
            }
        }

        ms.popPose();
    }

    public static PartialModel getBreezeModel(FrostLevel frostLevel, boolean blockAbove) {
        if (frostLevel.isAtLeast(FrostLevel.CHILLED)) {
            return blockAbove ? CCBPartialModels.BREEZE_CHILLED_ACTIVE : CCBPartialModels.BREEZE_CHILLED;
        }
        else {
            return CCBPartialModels.BREEZE_RIMING;
        }
    }

    @Override
    protected void renderSafe(BreezeCoolerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }

        boolean isChilled = be.getFrostLevel().isAtLeast(FrostLevel.CHILLED);
        renderShared(ms, null, bufferSource, level, be.getBlockState(), be.getFrostLevelForRender(), be.getHeadAnimation().getValue(partialTicks) * 0.175f, AngleHelper.rad(be.headAngle.getValue(partialTicks)), be.hasGoggles(), be.hasTrainHat() ? CCBPartialModels.BREEZE_TRAIN_HAT : be.isStockKeeper() ? CCBPartialModels.BREEZE_LOGISTICS_HAT : null, isChilled, isChilled ? 24 : 0, be.hashCode(), light, null);
    }
}
