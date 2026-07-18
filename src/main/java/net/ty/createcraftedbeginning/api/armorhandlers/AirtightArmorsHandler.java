package net.ty.createcraftedbeginning.api.armorhandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightArmorsHandler {
    SimpleRegistry<Gas, AirtightArmorsHandler> REGISTRY = SimpleRegistry.create();

    /**
     * Checks whether the supplied status effect can be cured.
     *
     * @param effectInstance the status effect instance to inspect
     * @return {@code true} if the supplied status effect can be cured; otherwise {@code false}
     */
    boolean canCureEffect(MobEffectInstance effectInstance);

    /**
     * Returns the consumption multiplier.
     *
     * @param slot the zero-based slot index
     * @return the consumption multiplier
     */
    float getConsumptionMultiplier(EquipmentSlot slot);

    /**
     * Returns the multiplier for boosting elytra.
     *
     * @return the multiplier for boosting elytra
     */
    float getMultiplierForBoostingElytra();
}
