package net.ty.createcraftedbeginning.content.airtights.smartairtightpipe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.gas.behaviour.GasFilteringBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SmartAirtightPipeRenderer extends SmartBlockEntityRenderer<SmartAirtightPipeBlockEntity> {
    public SmartAirtightPipeRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SmartAirtightPipeBlockEntity pipe, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        if (pipe.isRemoved()) {
            return;
        }

        Level level = pipe.getLevel();
        if (level == null) {
            return;
        }

        BlockPos pos = pipe.getBlockPos();
        GasFilteringBehaviour gasFilter = pipe.getBehaviour(GasFilteringBehaviour.TYPE);
        if (gasFilter == null || !gasFilter.isActive()) {
            return;
        }

        ItemStack filterStack = gasFilter.getFilter();
        if (filterStack.isEmpty()) {
            return;
        }

        if (isBeyondRenderDistance(pipe, level, pos, gasFilter)) {
            return;
        }

        ValueBoxTransform filterSlot = gasFilter.getSlotPositioning();
        BlockState state = pipe.getBlockState();
        if (!filterSlot.shouldRender(level, pos, state)) {
            return;
        }

        poseStack.pushPose();

        filterSlot.transform(level, pos, state, poseStack);
        ValueBoxRenderer.renderItemIntoValueBox(filterStack, poseStack, buffer, light, overlay);

        poseStack.popPose();
    }

    private static boolean isBeyondRenderDistance(SmartAirtightPipeBlockEntity pipe, Level level, BlockPos pos, GasFilteringBehaviour gasFilter) {
        if (pipe.isVirtual()) {
            return false;
        }

        Entity camera = Minecraft.getInstance().cameraEntity;
        if (camera == null || level != camera.level()) {
            return false;
        }

        float renderDistance = gasFilter.getRenderDistance();
        return camera.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > Mth.square(renderDistance);
    }
}
