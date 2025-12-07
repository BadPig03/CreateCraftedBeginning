package net.ty.createcraftedbeginning.api.gas.armorhandlers.creative;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCures;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import org.jetbrains.annotations.NotNull;

public class CreativeAirArmorsHandler implements AirtightArmorsHandler {
    @Override
    public boolean canCureEffect(@NotNull MobEffectInstance effectInstance) {
        return effectInstance.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL && effectInstance.getCures().contains(EffectCures.MILK);
    }

    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0, 0, 0, 0};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return 3;
    }
}
