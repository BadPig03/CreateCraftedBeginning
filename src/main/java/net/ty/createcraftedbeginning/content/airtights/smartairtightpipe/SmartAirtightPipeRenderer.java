package net.ty.createcraftedbeginning.content.airtights.smartairtightpipe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasFilteringBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SmartAirtightPipeRenderer extends SmartBlockEntityRenderer<SmartAirtightPipeBlockEntity> {
    public SmartAirtightPipeRenderer(Context context) {
        super(context);
    }

    private static boolean isBeyondRenderDistance(SmartAirtightPipeBlockEntity pipe, Level level, BlockPos pos, GasFilteringBehaviour behaviour) {
        if (pipe.isVirtual()) {
            return false;
        }

        Entity camera = Minecraft.getInstance().cameraEntity;
        if (camera == null || level != camera.level()) {
            return false;
        }

        float maxDistance = behaviour.getRenderDistance();
        return camera.position().distanceToSqr(VecHelper.getCenterOf(pos)) > maxDistance * maxDistance;
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
        GasFilteringBehaviour behaviour = be.getFilter();
        if (isBeyondRenderDistance(be, level, pos, behaviour)) {
            return;
        }

        if (!behaviour.isActive()) {
            return;
        }

        ItemStack filter = behaviour.getFilter();
        if (filter.isEmpty()) {
            return;
        }

        ValueBoxTransform slot = behaviour.getSlotPositioning();
        BlockState state = be.getBlockState();
        if (!slot.shouldRender(level, pos, state)) {
            return;
        }

        ms.pushPose();

        slot.transform(level, pos, state, ms);
        ValueBoxRenderer.renderItemIntoValueBox(filter, ms, buffer, light, overlay);

        ms.popPose();
    }
}
