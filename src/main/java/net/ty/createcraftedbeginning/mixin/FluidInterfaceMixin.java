package net.ty.createcraftedbeginning.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.data.CCBTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.simibubi.create.content.contraptions.actors.psi.PortableFluidInterfaceBlockEntity$InterfaceFluidHandler")
public abstract class FluidInterfaceMixin {
    @ModifyReturnValue(method = "change(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I", at = @At("RETURN"), remap = false)
    private int blockCompressedAirFill(int original, FluidStack resource, IFluidHandler.FluidAction action) {
        if (isCompressedAir(resource)) {
            return 0;
        }
        return original;
    }

    @ModifyReturnValue(method = "drain(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;", at = @At("RETURN"), remap = false)
    private FluidStack blockCompressedAirDrain(FluidStack original, FluidStack resource, IFluidHandler.FluidAction action) {
        if (isCompressedAir(original)) {
            return FluidStack.EMPTY;
        }
        return original;
    }

    @Unique
    private boolean isCompressedAir(FluidStack fluidStack) {
        return !fluidStack.isEmpty() && fluidStack.is(CCBTags.commonFluidTag("compressed_air"));
    }
}