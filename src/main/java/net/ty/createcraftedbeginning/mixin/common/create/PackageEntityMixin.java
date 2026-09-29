package net.ty.createcraftedbeginning.mixin.common.create;

import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonEntityBehaviour;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonWorldPhysics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = PackageEntity.class, remap = false)
public abstract class PackageEntityMixin extends LivingEntity {
    @Unique
    private float ccb$dimensionScale = Float.NaN;

    private PackageEntityMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "onInsideBlock", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/box/PackageEntity;destroy(Lnet/minecraft/world/damagesource/DamageSource;)V"), cancellable = true)
    private void ccb$onInsideBlock(BlockState state, CallbackInfo callback) {
        PackageEntity entity = (PackageEntity) (Object) this;
        if (!BalloonEntityBehaviour.isBalloon(entity) || !(state.getFluidState().is(FluidTags.WATER) || state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED))) {
            return;
        }

        callback.cancel();
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void ccb$hurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        PackageEntity entity = (PackageEntity) (Object) this;
        if (!BalloonEntityBehaviour.isBalloon(entity) || !source.is(DamageTypes.DROWN)) {
            return;
        }

        callback.setReturnValue(false);
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "getDefaultDimensions", at = @At("RETURN"), cancellable = true)
    private void ccb$getDefaultDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> callback) {
        PackageEntity entity = (PackageEntity) (Object) this;
        ItemStack box = entity.getBox();
        if (box == null || !BalloonItem.isBalloon(box)) {
            return;
        }

        float scale = BalloonWorldPhysics.of(box, entity.level(), entity.blockPosition()).linearScale();
        EntityDimensions baseDimensions = callback.getReturnValue();
        ccb$dimensionScale = scale;
        callback.setReturnValue(EntityDimensions.fixed(baseDimensions.width() * scale, baseDimensions.height() * scale));
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "tick", at = @At("TAIL"))
    private void ccb$tick(CallbackInfo callback) {
        PackageEntity entity = (PackageEntity) (Object) this;
        if (!BalloonEntityBehaviour.isBalloon(entity)) {
            return;
        }

        BalloonWorldPhysics physics = BalloonWorldPhysics.of(entity.getBox(), entity.level(), entity.blockPosition());
        float scale = physics.linearScale();
        if (Float.compare(scale, ccb$dimensionScale) != 0) {
            entity.refreshDimensions();
        }
        BalloonEntityBehaviour.tick(entity, physics);
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "destroy", at = @At("TAIL"))
    private void ccb$destroy(DamageSource source, CallbackInfo callback) {
        BalloonEntityBehaviour.destroy((PackageEntity) (Object) this);
    }
}
