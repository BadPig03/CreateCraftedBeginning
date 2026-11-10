package net.ty.createcraftedbeginning.mixin.common.minecraft;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.AirtightHandheldDrillControls;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.AirtightHandheldDrillMining;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.DrillMiningProgressAccess;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin implements DrillMiningProgressAccess {
    @Shadow
    protected ServerLevel level;
    @Shadow
    @Final
    protected ServerPlayer player;
    @Shadow
    private boolean isDestroyingBlock;
    @Shadow
    private boolean hasDelayedDestroy;
    @Shadow
    private BlockPos destroyPos;
    @Shadow
    private BlockPos delayedDestroyPos;
    @Shadow
    private int lastSentState;
    @Unique
    private boolean ccb$requiresNewMiningStart;

    @Override
    public void ccb$resetDrillMiningProgress() {
        if (isDestroyingBlock) {
            level.destroyBlockProgress(player.getId(), destroyPos, -1);
        }
        if (hasDelayedDestroy) {
            level.destroyBlockProgress(player.getId(), delayedDestroyPos, -1);
        }
        isDestroyingBlock = false;
        hasDelayedDestroy = false;
        lastSentState = -1;
        ccb$requiresNewMiningStart = true;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void ccb$tick(CallbackInfo callback) {
        AirtightHandheldDrillControls.validateServerState(player);
    }

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"), cancellable = true)
    private void ccb$handleBlockBreakAction$head(BlockPos pos, Action action, Direction direction, int maxBuildHeight, int sequence, CallbackInfo callback) {
        if (!ccb$requiresNewMiningStart || action != Action.STOP_DESTROY_BLOCK && action != Action.ABORT_DESTROY_BLOCK) {
            return;
        }

        callback.cancel();
    }

    @Inject(method = "handleBlockBreakAction", at = @At("RETURN"))
    private void ccb$handleBlockBreakAction$return(BlockPos pos, Action action, Direction direction, int maxBuildHeight, int sequence, CallbackInfo callback) {
        if (!ccb$requiresNewMiningStart || action != Action.START_DESTROY_BLOCK || !isDestroyingBlock) {
            return;
        }

        ccb$requiresNewMiningStart = false;
    }

    @WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;removeBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Z"))
    private boolean ccb$destroyBlock$remove(ServerPlayerGameMode gameMode, BlockPos pos, BlockState state, boolean canHarvest, Operation<Boolean> original, @Share("ccb$drillMining") LocalBooleanRef drillMining, @Share("ccb$removed") LocalBooleanRef removed) {
        ItemStack drill = player.getMainHandItem();
        if (player.isCreative() || !drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return original.call(gameMode, pos, state, canHarvest);
        }

        drillMining.set(true);
        boolean wasRemoved = AirtightHandheldDrillMining.removeBlock(drill, level, state, pos, player, () -> original.call(gameMode, pos, state, canHarvest));
        removed.set(wasRemoved);
        return wasRemoved;
    }

    @SuppressWarnings("MethodMayBeStatic")
    @ModifyReturnValue(method = "destroyBlock", at = @At("RETURN"))
    private boolean ccb$destroyBlock$return(boolean result, @Share("ccb$drillMining") LocalBooleanRef drillMining, @Share("ccb$removed") LocalBooleanRef removed) {
        return result && (!drillMining.get() || removed.get());
    }
}
