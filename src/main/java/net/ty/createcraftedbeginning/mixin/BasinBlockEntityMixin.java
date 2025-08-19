package net.ty.createcraftedbeginning.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.data.CCBTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BasinBlockEntity.class)
public abstract class BasinBlockEntityMixin {
    @SuppressWarnings("all")
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        BasinBlockEntity self = (BasinBlockEntity) (Object) this;
        Level level = self.getLevel();

        if (level != null && !level.isClientSide) {
            processCompressedAir(self);
        }
    }

    @Unique
    private void processCompressedAir(BasinBlockEntity basin) {
        boolean changed = false;

        SmartFluidTankBehaviour inputTank = basin.getTanks().getFirst();
        IFluidHandler fluidHandler = inputTank.getCapability();

        for (int i = 0; i < fluidHandler.getTanks(); i++) {
            FluidStack fluidStack = fluidHandler.getFluidInTank(i);
            if (!fluidStack.isEmpty() && fluidStack.is(CCBTags.commonFluidTag("compressed_air"))) {
                fluidHandler.drain(100, IFluidHandler.FluidAction.EXECUTE);
                changed = true;
            }
        }

        if (changed) {
            basin.notifyChangeOfContents();
        }
    }
}