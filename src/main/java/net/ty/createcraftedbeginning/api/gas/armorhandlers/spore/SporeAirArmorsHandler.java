package net.ty.createcraftedbeginning.api.gas.armorhandlers.spore;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCures;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import org.jetbrains.annotations.NotNull;

public class SporeAirArmorsHandler implements AirtightArmorsHandler {
    @Override
    public boolean canCureEffect(@NotNull MobEffectInstance effectInstance) {
        return effectInstance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && effectInstance.getCures().contains(EffectCures.MILK);
    }

    @Override
    public float[] getConsumptionMultiplier() {
        return new float[]{0.95f, 0.95f, 0.95f, 0.95f};
    }

    @Override
    public float getMultiplierForBoostingElytra() {
        return 0.55f;
    }
}
