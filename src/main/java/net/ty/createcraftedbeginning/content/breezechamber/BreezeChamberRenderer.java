package net.ty.createcraftedbeginning.content.breezechamber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlock.FrostLevel;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.Nullable;

public class BreezeChamberRenderer extends SafeBlockEntityRenderer<BreezeChamberBlockEntity> {
    public BreezeChamberRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    public static void renderInContraption(MovementContext context, ContraptionMatrices matrices, MultiBufferSource bufferSource, LerpedFloat headAngle, boolean conductor) {
        BlockState state = context.state;
        FrostLevel frostLevel = BreezeChamberBlock.getFrostLevelOf(state);
        if (!frostLevel.isAtLeast(FrostLevel.CHILLED)) {
            frostLevel = FrostLevel.CHILLED;
        }

        Level level = context.world;
        float horizontalAngle = AngleHelper.rad(headAngle.getValue(AnimationTickHolder.getPartialTicks(level)));
        boolean drawGoggles = context.blockEntityData.contains("Goggles");
        boolean drawHat = conductor || context.blockEntityData.contains("TrainHat");
        int hashCode = context.hashCode();

        renderShared(matrices.getViewProjection(), matrices.getModel(), bufferSource, level, state, frostLevel, 0, horizontalAngle, drawGoggles, drawHat ? CCBPartialModels.BREEZE_TRAIN_HAT : null, false, 0, hashCode);
    }

    public static void renderShared(PoseStack ms, @Nullable PoseStack modelTransform, MultiBufferSource bufferSource, Level level, BlockState blockState, FrostLevel frostLevel, float animation, float horizontalAngle, boolean drawGoggles, PartialModel drawHat, boolean drawWind, float windSpeed, int hashCode) {
        boolean blockAbove = animation > 0.125f;
        float time = AnimationTickHolder.getRenderTime(level);
        float renderTick = time + (hashCode % 13) * 16f;
        float offsetMultiplier = frostLevel.isAtLeast(FrostLevel.WANING) ? 64 : 16;
        float offset = Mth.sin((float) ((renderTick / 16f) % (2 * Math.PI))) / offsetMultiplier;
        float headY = offset - (animation * .75f);

        ms.pushPose();

        var breezeModel = getBreezeModel(frostLevel, blockAbove);

        SuperByteBuffer breezeBuffer = CachedBuffers.partial(breezeModel, blockState);
        if (modelTransform != null) {
            breezeBuffer.transform(modelTransform);
        }
        breezeBuffer.translate(0, headY, 0);
        draw(breezeBuffer, horizontalAngle, ms, bufferSource.getBuffer(RenderType.cutoutMipped()));

        if (drawGoggles) {
            PartialModel gogglesModel = frostLevel.isAtLeast(FrostLevel.WANING) ? CCBPartialModels.BREEZE_GOGGLES : CCBPartialModels.BREEZE_GOGGLES_SMALL;

            SuperByteBuffer gogglesBuffer = CachedBuffers.partial(gogglesModel, blockState);
            if (modelTransform != null) {
                gogglesBuffer.transform(modelTransform);
            }
            gogglesBuffer.translate(0, headY + 8 / 16f, 0);
            draw(gogglesBuffer, horizontalAngle, ms, bufferSource.getBuffer(RenderType.solid()));
        }

        if (drawHat != null) {
            SuperByteBuffer hatBuffer = CachedBuffers.partial(drawHat, blockState);
            if (modelTransform != null) {
                hatBuffer.transform(modelTransform);
            }
            hatBuffer.translate(0, headY, 0);
            if (breezeModel == CCBPartialModels.BREEZE_RIMING) {
                hatBuffer.translateY(0.5f).center().scale(0.75f).uncenter();
            } else {
                hatBuffer.translateY(0.75f);
            }
            VertexConsumer cutout = bufferSource.getBuffer(RenderType.cutoutMipped());
            hatBuffer.rotateCentered(horizontalAngle + Mth.PI, Direction.UP).translate(0.5f, 0, 0.5f).light(LightTexture.FULL_BRIGHT).renderInto(ms, cutout);
        }

        if (drawWind) {
            PartialModel windModel = CCBPartialModels.BREEZE_WIND;

            SuperByteBuffer windBuffer = CachedBuffers.partial(windModel, blockState);
            if (modelTransform != null) {
                windBuffer.transform(modelTransform);
            }

            windBuffer.translate(0, headY, 0);

            float windRotation = (AnimationTickHolder.getRenderTime(level) * windSpeed) % 360;
            float totalRotation = horizontalAngle + AngleHelper.rad(windRotation);
            windBuffer.translate(0.5f, 0.5f, 0.5f).rotateY(totalRotation).translate(-0.5f, -0.5f, -0.5f).light(LightTexture.FULL_BRIGHT).renderInto(ms, bufferSource.getBuffer(RenderType.cutout()));
        }

        ms.popPose();
    }

    public static PartialModel getBreezeModel(FrostLevel frostLevel, boolean blockAbove) {
        if (frostLevel.isAtLeast(FrostLevel.GALLING)) {
            return blockAbove ? CCBPartialModels.BREEZE_GALLING_ACTIVE : CCBPartialModels.BREEZE_GALLING;
        } else if (frostLevel.isAtLeast(FrostLevel.WANING)) {
            return blockAbove ? CCBPartialModels.BREEZE_CHILLED_ACTIVE : CCBPartialModels.BREEZE_CHILLED;
        } else {
            return CCBPartialModels.BREEZE_RIMING;
        }
    }

    private static void draw(SuperByteBuffer buffer, float horizontalAngle, PoseStack ms, VertexConsumer vc) {
        buffer.rotateCentered(horizontalAngle, Direction.UP).light(LightTexture.FULL_BRIGHT).renderInto(ms, vc);
    }

    @Override
    protected void renderSafe(BreezeChamberBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        FrostLevel frostLevel = be.getFrostLevelForRender();
        Level level = be.getLevel();
        BlockState blockState = be.getBlockState();
        float animation = be.headAnimation.getValue(partialTicks) * .175f;
        float horizontalAngle = AngleHelper.rad(be.headAngle.getValue(partialTicks));
        boolean drawGoggles = be.goggles;
        PartialModel drawHat = be.hat ? CCBPartialModels.BREEZE_TRAIN_HAT : null;
        boolean drawWind = be.wind;
        float windSpeed = be.windRotationSpeed;
        int hashCode = be.hashCode();

        renderShared(ms, null, bufferSource, level, blockState, frostLevel, animation, horizontalAngle, drawGoggles, drawHat, drawWind, windSpeed, hashCode);
    }
}
