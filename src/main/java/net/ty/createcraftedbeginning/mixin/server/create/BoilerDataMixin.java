package net.ty.createcraftedbeginning.mixin.server.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet.BoilerSteamOutletBlock;
import net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet.BoilerSteamOutletCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = BoilerData.class, remap = false)
public abstract class BoilerDataMixin {
    @Unique
    private int ccb$attachedSteamOutlets;

    @Unique
    private boolean ccb$steamOutletCountChanged;

    @SuppressWarnings("MethodMayBeStatic")
    @WrapOperation(method = "evaluate", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 0))
    private boolean ccb$evaluate(BlockEntry<?> entry, BlockState state, Operation<Boolean> original) {
        boolean originalResult = original.call(entry, state);
        if (entry != AllBlocks.STEAM_ENGINE) {
            return originalResult;
        }

        BoilerSteamOutletCompat.markVerified();
        return originalResult || BoilerSteamOutletBlock.isActive(state);
    }

    @Inject(method = "evaluate", at = @At("HEAD"))
    private void ccb$evaluateHead(FluidTankBlockEntity controller, CallbackInfoReturnable<Boolean> cir) {
        int previousCount = ccb$attachedSteamOutlets;
        ccb$attachedSteamOutlets = BoilerSteamOutletCompat.scanAttachedSteamOutlets(controller);
        ccb$steamOutletCountChanged = previousCount != ccb$attachedSteamOutlets;
    }

    @Inject(method = "evaluate", at = @At("RETURN"), cancellable = true)
    private void ccb$evaluateReturn(FluidTankBlockEntity controller, CallbackInfoReturnable<Boolean> cir) {
        if (!BoilerSteamOutletCompat.isVerified()) {
            ccb$attachedSteamOutlets = 0;
        }

        boolean changed = ccb$steamOutletCountChanged;
        ccb$steamOutletCountChanged = false;
        cir.setReturnValue(cir.getReturnValue() || changed);
    }
}
