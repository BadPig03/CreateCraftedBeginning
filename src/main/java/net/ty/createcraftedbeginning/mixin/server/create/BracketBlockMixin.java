package net.ty.createcraftedbeginning.mixin.server.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.decoration.bracket.BracketBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AxisGasPipeBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = BracketBlock.class, remap = false)
public abstract class BracketBlockMixin {
    @SuppressWarnings("MethodMayBeStatic")
    @WrapOperation(method = "getSuitableBracket(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Ljava/util/Optional;", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/FluidPropagator;getStraightPipeAxis(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/core/Direction$Axis;"))
    private Axis ccb$getSuitableBracket(BlockState state, Operation<Axis> original) {
        if (!(state.getBlock() instanceof AxisGasPipeBlock)) {
            return original.call(state);
        }
        return state.getValue(AxisGasPipeBlock.AXIS);
    }
}
