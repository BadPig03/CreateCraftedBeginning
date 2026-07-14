package net.ty.createcraftedbeginning.mixin.server.create;

import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
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
public abstract class PackageEntityMixin extends LivingEntity {
    private PackageEntityMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

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
        BalloonUtils.renderGasEffects(entity);
    }

    @Inject(method = "destroy", at = @At("TAIL"))
    private void ccb$destroy(DamageSource source, CallbackInfo ci) {
        PackageEntity entity = (PackageEntity) (Object) this;
        if (!BalloonUtils.isBalloonPackage(entity) || level().isClientSide) {
            return;
        }

        entity.setInvulnerable(true);
        BalloonUtils.windBurst(entity);
    }
}
