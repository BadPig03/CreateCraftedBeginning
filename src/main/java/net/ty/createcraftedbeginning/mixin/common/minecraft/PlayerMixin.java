package net.ty.createcraftedbeginning.mixin.common.minecraft;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.HasteUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentTraversal;
import net.ty.createcraftedbeginning.platform.access.AirVentCrawlingAccess;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Player.class)
public abstract class PlayerMixin implements AirVentCrawlingAccess {
    @Unique
    private boolean ccb$ventCrawling;

    @Override
    public boolean ccb$isVentCrawling() {
        return ccb$ventCrawling;
    }

    @SuppressWarnings("DataFlowIssue")
    @ModifyExpressionValue(method = "getDigSpeed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)F", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;onGround()Z"))
    private boolean ccb$getDigSpeed(boolean onGround) {
        Player player = (Player) (Object) this;
        return onGround || HasteUpgrade.INSTANCE.canApply(player);
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void ccb$updatePlayerPose(CallbackInfo callback) {
        Player player = (Player) (Object) this;
        ccb$ventCrawling = AirVentTraversal.shouldCrawl(player, ccb$ventCrawling);
        if (!ccb$ventCrawling) {
            return;
        }

        player.setPose(Pose.SWIMMING);
        callback.cancel();
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "getCurrentItemAttackStrengthDelay", at = @At("RETURN"), cancellable = true)
    private void ccb$getCurrentItemAttackStrengthDelay(CallbackInfoReturnable<Float> callback) {
        Player player = (Player) (Object) this;
        if (!player.getMainHandItem().is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return;
        }

        callback.setReturnValue(0.0F);
    }
}
