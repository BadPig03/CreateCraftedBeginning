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

    boolean canCureEffect(MobEffectInstance effectInstance);

    float getConsumptionMultiplier(EquipmentSlot slot);

    float getMultiplierForBoostingElytra();
}
