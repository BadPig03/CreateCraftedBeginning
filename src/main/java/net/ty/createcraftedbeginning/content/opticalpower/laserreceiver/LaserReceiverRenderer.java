package net.ty.createcraftedbeginning.content.opticalpower.laserreceiver;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LaserReceiverRenderer extends KineticBlockEntityRenderer<LaserReceiverBlockEntity> {
    public LaserReceiverRenderer(Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(LaserReceiverBlockEntity blockEntity, BlockState blockState) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, blockState, blockEntity.getOutputDirection());
    }
}
