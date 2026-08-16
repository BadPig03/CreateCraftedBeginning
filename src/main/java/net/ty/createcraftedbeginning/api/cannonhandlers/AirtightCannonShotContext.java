package net.ty.createcraftedbeginning.api.cannonhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightCannonShotContext(Entity projectile, @Nullable Entity owner, Holder<Gas> gasHolder, float effectMultiplier, float knockbackMultiplier, boolean flame) {
    public static AirtightCannonShotContext external(Entity source, Holder<Gas> gasHolder, float effectMultiplier) {
        return new AirtightCannonShotContext(source, source, gasHolder, effectMultiplier, 1, false);
    }

    public boolean isFriendlyTarget(Entity target) {
        return isFriendlyTarget(owner, target);
    }

    public static boolean isFriendlyTarget(@Nullable Entity owner, Entity target) {
        if (owner == null || target == owner) {
            return false;
        }

        if (owner.isAlliedTo(target) || target.isAlliedTo(owner)) {
            return true;
        }

        if (!(target instanceof TamableAnimal animal)) {
            return false;
        }

        LivingEntity petOwner = animal.getOwner();
        return petOwner != null && (petOwner == owner || owner.isAlliedTo(petOwner) || petOwner.isAlliedTo(owner));
    }

    public static boolean isProtectedTarget(@Nullable Entity owner, Entity target) {
        return owner != null && (target == owner || isFriendlyTarget(owner, target));
    }
}
