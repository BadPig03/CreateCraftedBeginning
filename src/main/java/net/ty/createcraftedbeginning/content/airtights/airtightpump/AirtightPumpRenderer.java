package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPumpRenderer extends KineticBlockEntityRenderer<AirtightPumpBlockEntity> {
    public AirtightPumpRenderer(Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(AirtightPumpBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(CCBPartialModels.AIRTIGHT_PUMP_COGS, state);
    }
}
