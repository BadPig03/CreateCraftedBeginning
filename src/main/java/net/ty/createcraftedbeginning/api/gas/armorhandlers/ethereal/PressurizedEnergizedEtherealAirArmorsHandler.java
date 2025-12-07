package net.ty.createcraftedbeginning.api.gas.armorhandlers.ethereal;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCures;
import org.jetbrains.annotations.NotNull;

public class PressurizedEnergizedEtherealAirArmorsHandler extends EtherealAirArmorsHandler {
    @Override
    public boolean canCureEffect(@NotNull MobEffectInstance effectInstance) {
        return effectInstance.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL && effectInstance.getCures().contains(EffectCures.MILK);
    }

    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.1f, 0.1f, 0.1f, 0.1f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 3;
    }
}
