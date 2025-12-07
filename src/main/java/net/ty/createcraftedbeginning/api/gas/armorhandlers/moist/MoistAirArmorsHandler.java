package net.ty.createcraftedbeginning.api.gas.armorhandlers.moist;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCures;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import org.jetbrains.annotations.NotNull;

public class MoistAirArmorsHandler implements AirtightArmorsHandler {
    @Override
    public boolean canCureEffect(@NotNull MobEffectInstance effectInstance) {
        return effectInstance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && effectInstance.getCures().contains(EffectCures.MILK);
    }

    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{1, 1, 1, 1};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return 1;
    }
}
