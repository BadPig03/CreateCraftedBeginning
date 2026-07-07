package net.ty.createcraftedbeginning.mixin.server.create;

import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = PackageEntity.class, remap = false)
@SuppressWarnings("DataFlowIssue")
public abstract class PackageEntityMixin {
    @Inject(method = "onInsideBlock", at = @At("HEAD"), cancellable = true)
    private void ccb$onInsideBlock(BlockState state, CallbackInfo ci) {
        PackageEntity entity = (PackageEntity) (Object) this;
        if (!BalloonUtils.isBalloonPackage(entity) || !BalloonUtils.isInWater(state)) {
            return;
        }

        ci.cancel();
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void ccb$hurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PackageEntity entity = (PackageEntity) (Object) this;
        if (!BalloonUtils.isBalloonPackage(entity) || !source.is(DamageTypes.DROWN)) {
            return;
        }

        cir.setReturnValue(false);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void ccb$tick(CallbackInfo ci) {
        PackageEntity entity = (PackageEntity) (Object) this;
        if (!BalloonUtils.isBalloonPackage(entity)) {
            return;
        }

        BalloonUtils.tickInWater(entity);
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void ccb$dropAllDeathLoot(ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        PackageEntity entity = (PackageEntity) (Object) this;
        if (!BalloonUtils.isBalloonPackage(entity)) {
            return;
        }

        ci.cancel();
        entity.setInvulnerable(true);
        BalloonUtils.windBurst(entity);
    }
}
