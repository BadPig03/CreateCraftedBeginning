package net.ty.createcraftedbeginning.mixin.server.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet.BoilerSteamOutletBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.ParametersAreNonnullByDefault;

@SuppressWarnings("MethodMayBeStatic")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = BoilerData.class, remap = false)
public abstract class BoilerDataMixin {
    @WrapOperation(method = "evaluate", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 0))
    private boolean ccb$evaluate(BlockEntry<?> entry, BlockState state, Operation<Boolean> original) {
        return original.call(entry, state) || BoilerSteamOutletBlock.isActive(state);
    }
}
