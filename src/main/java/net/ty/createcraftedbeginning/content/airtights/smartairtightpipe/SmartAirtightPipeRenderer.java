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

    @Override
    protected void renderSafe(SmartAirtightPipeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (be.isRemoved()) {
            return;
        }

        Level level = be.getLevel();
        BlockPos blockPos = be.getBlockPos();
        GasFilteringBehaviour behaviour = be.getFilter();
        if (!be.isVirtual()) {
            Entity cameraEntity = Minecraft.getInstance().cameraEntity;
            if (cameraEntity != null && level == cameraEntity.level()) {
                float max = behaviour.getRenderDistance();
                if (cameraEntity.position().distanceToSqr(VecHelper.getCenterOf(blockPos)) > max * max) {
                    return;
                }
            }
        }

        if (!behaviour.isActive() || behaviour.getFilter().isEmpty()) {
            return;
        }

        ValueBoxTransform positioning = behaviour.getSlotPositioning();
        BlockState blockState = be.getBlockState();
        if (!positioning.shouldRender(level, blockPos, blockState)) {
            return;
        }

        ms.pushPose();

        positioning.transform(level, blockPos, blockState, ms);
        ValueBoxRenderer.renderItemIntoValueBox(behaviour.getFilter(), ms, buffer, light, overlay);

        ms.popPose();
    }
}
