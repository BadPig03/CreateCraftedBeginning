package net.ty.createcraftedbeginning.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.block.entity.SculkShriekerBlockEntity$VibrationUser")
public abstract class SculkShriekerBlockEntityMixin {
    @SuppressWarnings("MethodMayBeStatic")
    @Inject(method = "canReceiveVibration", at = @At("HEAD"), cancellable = true)
    private void ccb$canReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> gameEvent, @NotNull Context context, CallbackInfoReturnable<Boolean> cir) {
        Entity sourceEntity = context.sourceEntity();
        if (sourceEntity == null) {
            return;
        }

        BlockPos sourcePos = sourceEntity.blockPosition();
        if (!CreateCraftedBeginning.GLOBAL_END_SCULK_SILENCER_MANAGER.checkWithinRange(sourcePos, level.dimension().location().toString())) {
            return;
        }

        cir.setReturnValue(false);
    }
}
