package net.ty.createcraftedbeginning.mixin;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.content.airtightintakeport.AirtightIntakePortBlock;
import net.ty.createcraftedbeginning.data.CCBTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidTransportBehaviour.class, remap = false)
public abstract class FluidTransportBehaviourMixin extends BlockEntityBehaviourMixin {
    @Unique
    private boolean isCompressedAir(FluidStack fluidStack) {
        return fluidStack.is(CCBTags.commonFluidTag("compressed_air"));
    }

    @Inject(method = "canPullFluidFrom(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z", at = @At("HEAD"), cancellable = true)
    private void preventCompressedAirPulling(FluidStack fluid, BlockState state, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!isCompressedAir(fluid)) {
            return;
        }

        BlockState otherState = getWorld().getBlockState(getPos().relative(direction));
        Block otherBlock = otherState.getBlock();

        if (otherBlock instanceof AirtightIntakePortBlock) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        cir.setReturnValue(true);
        cir.cancel();
    }
}

