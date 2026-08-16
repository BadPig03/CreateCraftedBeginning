package net.ty.createcraftedbeginning.api.armorhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.common.EffectCures;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DefaultArmorsHandler implements AirtightArmorsHandler {
    public static final DefaultArmorsHandler INSTANCE = new DefaultArmorsHandler();

    private DefaultArmorsHandler() {
    }

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
