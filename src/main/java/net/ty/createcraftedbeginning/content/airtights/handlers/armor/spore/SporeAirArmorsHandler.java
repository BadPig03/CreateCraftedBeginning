package net.ty.createcraftedbeginning.content.airtights.handlers.armor.spore;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.common.EffectCures;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SporeAirArmorsHandler implements AirtightArmorsHandler {
    @Override
    public boolean canCureEffect(MobEffectInstance effectInstance) {
        return effectInstance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && effectInstance.getCures().contains(EffectCures.MILK);
    }

    @Override
    public float getConsumptionMultiplier(EquipmentSlot slot) {
        return 1;
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return 0.5f;
    }
}
