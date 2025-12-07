package net.ty.createcraftedbeginning.api.gas.armorhandlers.ultrawarm;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCures;
import org.jetbrains.annotations.NotNull;

public class PressurizedEnergizedUltrawarmAirArmorsHandler extends UltrawarmAirArmorsHandler {
    @Override
    public boolean canCureEffect(@NotNull MobEffectInstance effectInstance) {
        return effectInstance.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL && effectInstance.getCures().contains(EffectCures.MILK);
    }

    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.3f, 0.3f, 0.3f, 0.3f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return super.getMultiplierForBoostingElytra() * 3;
    }
}
