package net.ty.createcraftedbeginning.mixin.server.create;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = SmartObserverBlock.class, remap = false)
public abstract class SmartObserverBlockMixin {
    @ModifyVariable(method = "getStateForPlacement", at = @At("LOAD"), ordinal = 0)
    private static boolean ccb$canDetect(boolean canDetect, BlockPlaceContext context, @Local @Nullable BlockEntity blockEntity) {
        if (canDetect || blockEntity == null) {
            return canDetect;
        }
        return BlockEntityBehaviour.get(blockEntity, GasTransportBehaviour.TYPE) != null || context.getLevel().getCapability(GasHandler.BLOCK, blockEntity.getBlockPos(), null) != null;
    }
}
