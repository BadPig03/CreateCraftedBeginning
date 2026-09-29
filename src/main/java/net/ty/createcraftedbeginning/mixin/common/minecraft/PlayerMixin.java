package net.ty.createcraftedbeginning.mixin.common.minecraft;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentTraversal;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Player.class)
public abstract class PlayerMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void ccb$maintainVentCrawling(CallbackInfo callback) {
        Player player = (Player) (Object) this;
        if (!AirVentTraversal.shouldCrawl(player)) {
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
