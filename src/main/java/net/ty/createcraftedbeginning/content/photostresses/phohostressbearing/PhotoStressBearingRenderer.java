package net.ty.createcraftedbeginning.content.photostresses.phohostressbearing;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PhotoStressBearingRenderer extends KineticBlockEntityRenderer<PhotoStressBearingBlockEntity> {
    public PhotoStressBearingRenderer(Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(PhotoStressBearingBlockEntity be, BlockState blockState) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, blockState, Direction.DOWN);
    }
}
