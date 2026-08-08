package net.ty.createcraftedbeginning.content.airtights.handlers.armor.sculk;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.common.EffectCures;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SculkAirArmorsHandler implements AirtightArmorsHandler {
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canCureEffect(MobEffectInstance effectInstance) {
        return effectInstance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && effectInstance.getCures().contains(EffectCures.MILK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getConsumptionMultiplier(EquipmentSlot slot) {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getMultiplierForBoostingElytra() {
        return 0.5f;
    }
}
