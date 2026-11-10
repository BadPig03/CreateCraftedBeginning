package net.ty.createcraftedbeginning.mixin.common.minecraft;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor.AmethystSoundSuppressorGameEvents;
import net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor.AmethystSoundSuppressorGameEvents.Dispatch;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(GameEventDispatcher.class)
public abstract class GameEventDispatcherMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @WrapMethod(method = "post")
    private void ccb$post(Holder<GameEvent> gameEvent, Vec3 sourcePosition, Context context, Operation<Void> original) {
        Dispatch dispatch = AmethystSoundSuppressorGameEvents.beginDispatch(level, gameEvent, sourcePosition, context);
        if (dispatch == null) {
            original.call(gameEvent, sourcePosition, context);
            return;
        }

        try (dispatch) {
            if (dispatch.suppressAt(sourcePosition)) {
                return;
            }

            original.call(gameEvent, sourcePosition, context);
        }
    }

    @SuppressWarnings("MethodMayBeStatic")
    @WrapOperation(method = {"lambda$post$0", "handleGameEventMessagesInQueue"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gameevent/GameEventListener;handleGameEvent(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/Holder;Lnet/minecraft/world/level/gameevent/GameEvent$Context;Lnet/minecraft/world/phys/Vec3;)Z"), require = 2)
    private boolean ccb$lambda$post$0$handleGameEventMessagesInQueue(GameEventListener listener, ServerLevel serverLevel, Holder<GameEvent> gameEvent, Context context, Vec3 sourcePosition, Operation<Boolean> original) {
        return !AmethystSoundSuppressorGameEvents.suppressRecipient(serverLevel, gameEvent, listener) && original.call(listener, serverLevel, gameEvent, context, sourcePosition);
    }
}
