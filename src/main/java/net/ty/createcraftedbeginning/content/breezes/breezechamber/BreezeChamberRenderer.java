package net.ty.createcraftedbeginning.content.breezes.breezechamber;

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
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BreezeChamberRenderer extends SafeBlockEntityRenderer<BreezeChamberBlockEntity> {
    private static final String COMPOUND_KEY_GOGGLES = "Goggles";
    private static final String COMPOUND_KEY_TRAIN_HAT = "TrainHat";

    public BreezeChamberRenderer(Context ignored) {
    }

    public static void renderInContraption(@NotNull MovementContext context, @NotNull ContraptionMatrices matrices, MultiBufferSource bufferSource, @NotNull LerpedFloat headAngle, boolean conductor) {
        Level level = context.world;
        boolean drawGoggles = context.blockEntityData.contains(COMPOUND_KEY_GOGGLES) && context.blockEntityData.getBoolean(COMPOUND_KEY_GOGGLES);
        boolean drawHat = conductor || context.blockEntityData.contains(COMPOUND_KEY_TRAIN_HAT) && context.blockEntityData.getBoolean(COMPOUND_KEY_TRAIN_HAT);
        renderShared(matrices.getViewProjection(), matrices.getModel(), bufferSource, level, context.state, WindLevel.GALE, 0, AngleHelper.rad(headAngle.getValue(AnimationTickHolder.getPartialTicks(level))), drawGoggles, drawHat ? CCBPartialModels.BREEZE_TRAIN_HAT : null, false, 0, context.hashCode());
    }

    public static void renderShared(@NotNull PoseStack ms, @Nullable PoseStack modelTransform, MultiBufferSource bufferSource, Level level, BlockState blockState, @NotNull WindLevel windLevel, float animation, float horizontalAngle, boolean drawGoggles, PartialModel drawHat, boolean drawWind, float windSpeed, int hashCode) {
        boolean active = animation > 0.125f;
        float renderTime = AnimationTickHolder.getRenderTime(level);
        float headY = Mth.sin((renderTime + (hashCode % 13) * 16.0f) / 16.0f % Mth.TWO_PI) / (windLevel.isAtLeast(WindLevel.GALE) ? 64 : 16) - animation * 0.75f;

        ms.pushPose();

        PartialModel breezeModel = getBreezeModel(windLevel, active);
        SuperByteBuffer breezeBuffer = CachedBuffers.partial(breezeModel, blockState);
        if (modelTransform != null) {
            breezeBuffer.transform(modelTransform);
        }
        breezeBuffer.translate(0, headY - 0.125f, 0);
        draw(breezeBuffer, horizontalAngle, ms, bufferSource.getBuffer(RenderType.cutoutMipped()));

        if (drawGoggles) {
            PartialModel gogglesModel = windLevel.isAtLeast(WindLevel.GALE) ? CCBPartialModels.BREEZE_CHAMBER_GOGGLES : CCBPartialModels.BREEZE_CHAMBER_GOGGLES_SMALL;
            SuperByteBuffer gogglesBuffer = CachedBuffers.partial(gogglesModel, blockState);
            if (modelTransform != null) {
                gogglesBuffer.transform(modelTransform);
            }
            gogglesBuffer.translate(0, headY + 0.375f, 0);
            draw(gogglesBuffer, horizontalAngle, ms, bufferSource.getBuffer(RenderType.solid()));
        }
        if (drawHat != null) {
            SuperByteBuffer hatBuffer = CachedBuffers.partial(drawHat, blockState);
            if (modelTransform != null) {
                hatBuffer.transform(modelTransform);
            }
            hatBuffer.translate(0, headY - 0.125f, 0);
            if (breezeModel == CCBPartialModels.BREEZE_CALM) {
                hatBuffer.translateY(0.5f).center().scale(0.75f).uncenter();
            }
            else {
                hatBuffer.translateY(0.75f);
            }
            VertexConsumer cutout = bufferSource.getBuffer(RenderType.cutoutMipped());
            hatBuffer.rotateCentered(horizontalAngle + Mth.PI, Direction.UP).translate(0.5f, 0, 0.5f).light(LightTexture.FULL_BRIGHT).renderInto(ms, cutout);
        }
        if (drawWind) {
            PartialModel windModel = CCBPartialModels.BREEZE_CHAMBER_WIND;
            SuperByteBuffer windBuffer = CachedBuffers.partial(windModel, blockState);
            if (modelTransform != null) {
                windBuffer.transform(modelTransform);
            }
            windBuffer.translate(0, headY - 0.125f, 0);
            float totalRotation = horizontalAngle + AngleHelper.rad(renderTime * windSpeed % 360);
            windBuffer.translate(0.5f, 0.5f, 0.5f).rotateY(totalRotation).translate(-0.5f, -0.5f, -0.5f).light(LightTexture.FULL_BRIGHT).renderInto(ms, bufferSource.getBuffer(RenderType.cutout()));
        }

        ms.popPose();
    }

    public static PartialModel getBreezeModel(@NotNull WindLevel windLevel, boolean blockBelow) {
        if (windLevel.isAtLeast(WindLevel.GALE)) {
            return blockBelow ? CCBPartialModels.BREEZE_GALE_ACTIVE : CCBPartialModels.BREEZE_GALE;
        }
        else if (windLevel == WindLevel.CALM) {
            return CCBPartialModels.BREEZE_CALM;
        }
        else {
            return CCBPartialModels.BREEZE_ILL;
        }
    }

    private static void draw(@NotNull SuperByteBuffer buffer, float horizontalAngle, PoseStack ms, VertexConsumer consumer) {
        buffer.rotateCentered(horizontalAngle, Direction.UP).light(LightTexture.FULL_BRIGHT).renderInto(ms, consumer);
    }

    @Override
    protected void renderSafe(@NotNull BreezeChamberBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        renderShared(ms, null, bufferSource, be.getLevel(), be.getBlockState(), be.getWindLevelForRender(), be.getHeadAnimation().getValue(partialTicks) * 0.175f, AngleHelper.rad(be.headAngle.getValue(partialTicks)), be.hasGoggles(), be.hasTrainHat() ? CCBPartialModels.BREEZE_TRAIN_HAT : null, be.getWindLevel().isAtLeast(WindLevel.GALE), be.getWindLevel().isAtLeast(WindLevel.GALE) ? 24.0f : 0, be.hashCode());
    }
}
