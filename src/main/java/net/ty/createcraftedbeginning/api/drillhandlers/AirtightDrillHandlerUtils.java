package net.ty.createcraftedbeginning.api.drillhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightDrillHandlerUtils {
    private AirtightDrillHandlerUtils() {
    }

    public static AirtightDrillHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    public static AirtightDrillHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightDrillHandler drillHandler = AirtightDrillHandler.REGISTRY.get(gasType);
        if (drillHandler == null) {
            return DefaultDrillHandler.INSTANCE;
        }
        return drillHandler;
    }

    public static void register(ResourceLocation location, int damage, float consumption) {
        register(location, new AirtightDrillHandler() {

            @Override
            public int getDamageAddition() {
                return damage;
            }

            @Override
            public float getConsumptionMultiplier() {
                return consumption;
            }
        });
    }

    public static void register(ResourceLocation location, AirtightDrillHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register Airtight Drill Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightDrillHandler drillHandler = AirtightDrillHandler.REGISTRY.get(gasType);
        if (drillHandler != null) {
            CCBAPI.LOGGER.error("Failed to register Airtight Drill Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        if (!GasConsumptions.isNonNegative(handler.getDamageAddition())) {
            CCBAPI.LOGGER.error("Failed to register Airtight Drill Handler for gas '{}': damage addition must be non-negative, got {}.", location, handler.getDamageAddition());
            return;
        }

        if (!GasConsumptions.isNonNegativeFinite(handler.getConsumptionMultiplier())) {
            CCBAPI.LOGGER.error("Failed to register Airtight Drill Handler for gas '{}': consumption multiplier must be finite and non-negative, got {}.", location, handler.getConsumptionMultiplier());
            return;
        }

        AirtightDrillHandler.REGISTRY.register(gasType, handler);
    }
}
