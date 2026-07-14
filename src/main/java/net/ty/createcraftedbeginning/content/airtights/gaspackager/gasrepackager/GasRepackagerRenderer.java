package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasRepackagerRenderer extends SmartBlockEntityRenderer<GasRepackagerBlockEntity> {
    public GasRepackagerRenderer(Context context) {
        super(context);
    }

    public static PartialModel getTrayModel(BlockState state) {
        if (state.getBlock() instanceof GasRepackagerBlock) {
            return CCBPartialModels.GAS_PACKAGER_TRAY_REGULAR;
        }
        return CCBPartialModels.GAS_PACKAGER_TRAY_DEFRAG;
    }

    public static PartialModel getHatchModel(GasRepackagerBlockEntity be) {
        return isHatchOpen(be) ? CCBPartialModels.GAS_PACKAGER_HATCH_OPEN : CCBPartialModels.GAS_PACKAGER_HATCH_CLOSED;
    }

    public static boolean isHatchOpen(GasRepackagerBlockEntity be) {
        return be.animationTicks > (be.animationInward ? 1 : 5) && be.animationTicks < PackagerBlockEntity.CYCLE - (be.animationInward ? 5 : 1);
    }

    @Override
    protected void renderSafe(GasRepackagerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        float trayOffset = be.getTrayOffset(partialTicks);
        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(PackagerBlock.FACING).getOpposite();
        if (!VisualizationManager.supportsVisualization(be.getLevel())) {
            SuperByteBuffer hatch = CachedBuffers.partial(getHatchModel(be), blockState);
            hatch.translate(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.5f)).rotateYCenteredDegrees(AngleHelper.horizontalAngle(facing)).rotateXCenteredDegrees(AngleHelper.verticalAngle(facing)).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));

            SuperByteBuffer tray = CachedBuffers.partial(getTrayModel(blockState), blockState);
            tray.translate(Vec3.atLowerCornerOf(facing.getNormal()).scale(trayOffset)).rotateYCenteredDegrees(facing.toYRot()).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        }

        ItemStack renderedBox = be.getRenderedBox();
        if (renderedBox.isEmpty()) {
            return;
        }

        ms.pushPose();

        TransformStack.of(ms).translate(Vec3.atLowerCornerOf(facing.getNormal()).scale(trayOffset)).translate(0.5f, 0.5f, 0.5f).rotateYDegrees(facing.toYRot()).translate(0, 0.125f, 0).scale(1.5f, 1.5f, 1.5f);
        Minecraft.getInstance().getItemRenderer().renderStatic(null, renderedBox, ItemDisplayContext.FIXED, false, ms, buffer, be.getLevel(), light, overlay, 0);

        ms.popPose();
    }
}
