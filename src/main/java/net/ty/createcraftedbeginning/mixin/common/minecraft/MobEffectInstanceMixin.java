package net.ty.createcraftedbeginning.mixin.common.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin {
    @SuppressWarnings("MethodMayBeStatic")
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;tickDownDuration()I"))
    private int ccb$tick(MobEffectInstance instance, Operation<Integer> original, LivingEntity entity, Runnable onExpirationRunnable) {
        if (instance.is(CCBMobEffects.STEAM_SCALD) && !entity.isInWater()) {
            return instance.getDuration();
        }

        return original.call(instance);
    }
}
