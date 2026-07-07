package net.ty.createcraftedbeginning.api.armhandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public interface AirtightArmHandler {
    SimpleRegistry<Gas, AirtightArmHandler> REGISTRY = SimpleRegistry.create();

    float getGasConsumptionMultiplier();

    float getIncreasedBlockInteractionRange();

    float getIncreasedEntityInteractionRange();

    float getIncreasedKnockback();

    default String getRenderStr(float n) {
        return String.format("%.2f", n).replaceAll("\\.?0+$", "");
    }
}
