package net.ty.createcraftedbeginning.mixin.common.minecraft;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem.Listener;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.end.endsculksilencer.EndSculkSilencerEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Listener.class)
public abstract class VibrationSystemListenerMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "handleGameEvent", at = @At("HEAD"), cancellable = true)
    private void ccb$handleGameEvent(ServerLevel level, Holder<GameEvent> gameEvent, Context context, Vec3 sourcePosition, CallbackInfoReturnable<Boolean> callback) {
        if (!EndSculkSilencerEvents.isSilenceableGameEvent(gameEvent) || !EndSculkSilencerEvents.hasSilencerCoverage(level)) {
            return;
        }

        if (EndSculkSilencerEvents.isWithinSilencedArea(level, sourcePosition)) {
            callback.setReturnValue(false);
            return;
        }

        Listener listener = (Listener) (Object) this;
        listener.getListenerSource().getPosition(level).ifPresent(pos -> {
            if (!EndSculkSilencerEvents.isWithinSilencedArea(level, pos)) {
                return;
            }

            callback.setReturnValue(false);
        });
    }
}
