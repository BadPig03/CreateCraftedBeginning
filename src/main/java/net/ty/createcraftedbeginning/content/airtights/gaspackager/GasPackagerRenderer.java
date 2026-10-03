package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonRenderHelper;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasPackagerRenderer extends SmartBlockEntityRenderer<GasPackagerBlockEntity> {
    public GasPackagerRenderer(Context context) {
        super(context);
    }

    static PartialModel getTrayModel(BlockState state) {
        if (!(state.getBlock() instanceof GasPackagerBlock)) {
            return CCBPartialModels.GAS_PACKAGER_TRAY_DEFRAG;
        }

        return CCBPartialModels.GAS_PACKAGER_TRAY_REGULAR;
    }

    static PartialModel getHatchModel(GasPackagerBlockEntity packager) {
        if (!isHatchOpen(packager)) {
            return CCBPartialModels.GAS_PACKAGER_HATCH_CLOSED;
        }

        return CCBPartialModels.GAS_PACKAGER_HATCH_OPEN;
    }

    private static boolean isHatchOpen(GasPackagerBlockEntity packager) {
        if (packager.animationInward) {
            return packager.animationTicks > 1 && packager.animationTicks < PackagerBlockEntity.CYCLE - 5;
        }

        return packager.animationTicks > 5 && packager.animationTicks < PackagerBlockEntity.CYCLE - 1;
    }

    @Override
    protected void renderSafe(GasPackagerBlockEntity packager, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(packager, partialTicks, poseStack, buffer, light, overlay);

        float trayOffset = packager.getTrayOffset(partialTicks);
        BlockState blockState = packager.getBlockState();
        Direction facing = blockState.getValue(PackagerBlock.FACING).getOpposite();
        Level level = packager.getLevel();
        if (!VisualizationManager.supportsVisualization(level)) {
            SuperByteBuffer hatch = CachedBuffers.partial(getHatchModel(packager), blockState);
            hatch.translate(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.5)).rotateYCenteredDegrees(AngleHelper.horizontalAngle(facing)).rotateXCenteredDegrees(AngleHelper.verticalAngle(facing)).light(light).renderInto(poseStack, buffer.getBuffer(RenderType.solid()));

            SuperByteBuffer tray = CachedBuffers.partial(getTrayModel(blockState), blockState);
            tray.translate(Vec3.atLowerCornerOf(facing.getNormal()).scale(trayOffset)).rotateYCenteredDegrees(facing.toYRot()).light(light).renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
        }

        ItemStack renderedBox = packager.getRenderedBox();
        if (renderedBox.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        float balloonScale = BalloonRenderHelper.getLinearScale(renderedBox, level, packager.getBlockPos());
        TransformStack.of(poseStack).translate(Vec3.atLowerCornerOf(facing.getNormal()).scale(trayOffset)).translate(0.5, 0.5, 0.5).rotateYDegrees(facing.toYRot()).translate(0, 0.125, 0).scale(1.5F, 1.5F, 1.5F);
        BalloonRenderHelper.applyBottomAnchoredFixedItemScale(poseStack, balloonScale);
        Minecraft.getInstance().getItemRenderer().renderStatic(null, renderedBox, ItemDisplayContext.FIXED, false, poseStack, buffer, level, light, overlay, 0);
        poseStack.popPose();
    }
}
