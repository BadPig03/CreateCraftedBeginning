package net.ty.createcraftedbeginning.api.armorhandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffectInstance;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightArmorsHandler {
    SimpleRegistry<Gas, AirtightArmorsHandler> REGISTRY = SimpleRegistry.create();

    boolean canCureEffect(MobEffectInstance effectInstance);

    float[] getConsumptionMultiplier();

    float getMultiplierForBoostingElytra();

    default String getRenderStr(float n) {
        return String.format("%.2f", n).replaceAll("\\.?0+$", "");
    }
}
