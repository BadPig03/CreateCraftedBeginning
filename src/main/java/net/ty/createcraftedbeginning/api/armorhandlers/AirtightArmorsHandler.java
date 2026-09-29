package net.ty.createcraftedbeginning.api.armorhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightArmorsHandler {
    boolean canCureEffect(MobEffectInstance effectInstance);

    float getConsumptionMultiplier(EquipmentSlot slot);

    float getMultiplierForBoostingElytra();
}
