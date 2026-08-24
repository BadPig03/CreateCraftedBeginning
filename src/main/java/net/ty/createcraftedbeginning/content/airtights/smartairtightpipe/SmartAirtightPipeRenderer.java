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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasFilteringBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SmartAirtightPipeRenderer extends SmartBlockEntityRenderer<SmartAirtightPipeBlockEntity> {
    public SmartAirtightPipeRenderer(Context context) {
        super(context);
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
        return camera.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > renderDistance * renderDistance;
    }

    @Override
    protected void renderSafe(SmartAirtightPipeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (be.isRemoved()) {
            return;
        }

        Level level = be.getLevel();
        if (level == null) {
            return;
        }

        BlockPos pos = be.getBlockPos();
        GasFilteringBehaviour gasFilter = be.getFilter();
        if (gasFilter == null || !gasFilter.isActive()) {
            return;
        }

        ItemStack filterStack = gasFilter.getFilter();
        if (filterStack.isEmpty()) {
            return;
        }

        if (isBeyondRenderDistance(be, level, pos, gasFilter)) {
            return;
        }

        ValueBoxTransform filterSlot = gasFilter.getSlotPositioning();
        BlockState state = be.getBlockState();
        if (!filterSlot.shouldRender(level, pos, state)) {
            return;
        }

        ms.pushPose();

        filterSlot.transform(level, pos, state, ms);
        ValueBoxRenderer.renderItemIntoValueBox(filterStack, ms, buffer, light, overlay);

        ms.popPose();
    }
}
