package net.ty.createcraftedbeginning.mixin;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.ty.createcraftedbeginning.content.end.endsculksilencer.GlobalEndSculkSilencerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(targets = "net.minecraft.world.level.block.entity.SculkSensorBlockEntity$VibrationUser")
public abstract class SculkSensorBlockEntityMixin {
    @SuppressWarnings("MethodMayBeStatic")
    @Inject(method = "canReceiveVibration", at = @At("HEAD"), cancellable = true)
    private void ccb$canReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> gameEvent, Context context, CallbackInfoReturnable<Boolean> cir) {
        Entity sourceEntity = context.sourceEntity();
        if (sourceEntity == null) {
            return;
        }

        BlockPos sourcePos = sourceEntity.blockPosition();
        if (!GlobalEndSculkSilencerManager.checkWithinRange(sourcePos, level.dimension().location().toString())) {
            return;
        }

        cir.setReturnValue(false);
    }
}
