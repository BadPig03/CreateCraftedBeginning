package net.ty.createcraftedbeginning.mixin.server.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.ParametersAreNonnullByDefault;

@SuppressWarnings("MethodMayBeStatic")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = ThresholdSwitchBlock.class, remap = false)
public abstract class ThresholdSwitchBlockMixin {
    @WrapOperation(method = "getStateForPlacement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getCapability(Lnet/neoforged/neoforge/capabilities/BlockCapability;Lnet/minecraft/core/BlockPos;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 1))
    private @Nullable Object ccb$getStateForPlacement(Level level, BlockCapability<?, ?> capability, BlockPos pos, Object context, Operation<Object> original) {
        Object fluid = original.call(level, capability, pos, context);
        if (fluid != null) {
            return fluid;
        }

        Direction side = context instanceof Direction direction ? direction : null;
        return level.getCapability(GasHandler.BLOCK, pos, side);
    }
}
