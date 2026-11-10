package net.ty.createcraftedbeginning.api.cannonhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/** Replaces only gas consumption; all other behavior delegates to the previous handler. */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightCannonHandlerPatch(double consumptionMultiplier) {
    public AirtightCannonHandlerPatch {
        if (!GasConsumptionMath.isNonNegativeFinite(consumptionMultiplier)) {
            throw new IllegalArgumentException("Airtight cannon consumption multiplier must be finite and non-negative, got " + consumptionMultiplier + '.');
        }
    }

    AirtightCannonHandler applyTo(AirtightCannonHandler original) {
        return new AirtightCannonHandler() {
            @Override
            public void explode(Level level, Vec3 pos, AirtightCannonShotContext context) {
                original.explode(level, pos, context);
            }

            @Override
            public double getGasConsumptionMultiplier() {
                return consumptionMultiplier;
            }

            @Override
            public void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                original.appendHoverText(cannon, context, tooltip, flag);
            }

            @Override
            public float getDirectHitDamage() {
                return original.getDirectHitDamage();
            }

            @Override
            public DamageSource createDirectHitDamageSource(Entity projectile, @Nullable LivingEntity owner) {
                return original.createDirectHitDamageSource(projectile, owner);
            }

            @Override
            public void applyAdditionalEffects(Level level, List<LivingEntity> entities, DamageSource explosionDamageSource, AirtightCannonShotContext context) {
                original.applyAdditionalEffects(level, entities, explosionDamageSource, context);
            }
        };
    }
}
