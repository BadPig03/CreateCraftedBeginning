package net.ty.createcraftedbeginning.api.armorhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

/** Omitted entries delegate to the original handler; supplied multipliers are absolute values. */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightArmorsHandlerPatch(Map<EquipmentSlot, Float> consumption, @Nullable Float elytraBoostMultiplier, @Nullable Predicate<MobEffectInstance> canCureEffect) {
    public AirtightArmorsHandlerPatch {
        consumption = Map.copyOf(consumption);
        if (consumption.isEmpty() && elytraBoostMultiplier == null && canCureEffect == null) {
            throw new IllegalArgumentException("An airtight armor handler patch must change at least one field.");
        }

        for (Entry<EquipmentSlot, Float> entry : consumption.entrySet()) {
            EquipmentSlot slot = entry.getKey();
            if (slot != EquipmentSlot.HEAD && slot != EquipmentSlot.CHEST && slot != EquipmentSlot.LEGS && slot != EquipmentSlot.FEET) {
                throw new IllegalArgumentException("Airtight armor consumption cannot be configured for slot '" + slot + "'.");
            }
            if (!GasConsumptionMath.isNonNegativeFinite(entry.getValue())) {
                throw new IllegalArgumentException("Airtight armor consumption for slot '" + slot + "' must be finite and non-negative, got " + entry.getValue() + '.');
            }
        }
        if (elytraBoostMultiplier != null && !GasConsumptionMath.isNonNegativeFinite(elytraBoostMultiplier)) {
            throw new IllegalArgumentException("Airtight armor elytra boost multiplier must be finite and non-negative, got " + elytraBoostMultiplier + '.');
        }
    }

    AirtightArmorsHandler applyTo(AirtightArmorsHandler original) {
        return new AirtightArmorsHandler() {
            @Override
            public boolean canCureEffect(MobEffectInstance effectInstance) {
                if (canCureEffect == null) {
                    return original.canCureEffect(effectInstance);
                }

                return canCureEffect.test(effectInstance);
            }

            @Override
            public float getConsumptionMultiplier(EquipmentSlot slot) {
                Float multiplier = consumption.get(slot);
                if (multiplier == null) {
                    return original.getConsumptionMultiplier(slot);
                }

                return multiplier;
            }

            @Override
            public float getMultiplierForBoostingElytra() {
                if (elytraBoostMultiplier == null) {
                    return original.getMultiplierForBoostingElytra();
                }

                return elytraBoostMultiplier;
            }
        };
    }
}
