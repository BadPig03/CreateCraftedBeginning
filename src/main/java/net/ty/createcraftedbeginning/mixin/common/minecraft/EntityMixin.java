package net.ty.createcraftedbeginning.mixin.common.minecraft;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorSet;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentTraversal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Entity.class)
public class EntityMixin {
    @Shadow
    @Final
    protected static EntityDataAccessor<Pose> DATA_POSE;

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V", at = @At("HEAD"), cancellable = true)
    private void ccb$onSyncedDataUpdated(EntityDataAccessor<?> key, CallbackInfo callback) {
        if (!DATA_POSE.equals(key)) {
            return;
        }

        if (!((Entity) (Object) this instanceof Player player) || !AirVentTraversal.shouldPreserveLocalCrawling(player)) {
            return;
        }

        player.setPose(Pose.SWIMMING);
        callback.cancel();
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "fireImmune", at = @At("RETURN"), cancellable = true)
    private void ccb$fireImmune(CallbackInfoReturnable<Boolean> callback) {
        if (!((Entity) (Object) this instanceof Player player) || !AirtightArmorSet.isEntireArmoredUp(player)) {
            return;
        }

        callback.setReturnValue(true);
    }
}
