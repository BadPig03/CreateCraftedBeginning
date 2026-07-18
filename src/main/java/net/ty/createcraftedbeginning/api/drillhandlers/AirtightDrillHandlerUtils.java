package net.ty.createcraftedbeginning.api.drillhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptionUtils;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides lookup and registration helpers for gas-specific airtight drill behavior.
 * Handlers define damage, gas consumption, tooltip content, and optional extra behavior.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightDrillHandlerUtils {
    private AirtightDrillHandlerUtils() {
    }

    /**
     * Resolves the airtight drill handler associated with the supplied input.
     *
     * @param gasStack the gas stack to inspect or process
     * @return the resolved airtight drill handler
     * @throws IllegalArgumentException if an argument is invalid
     */
    public static AirtightDrillHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    /**
     * Resolves the airtight drill handler associated with the supplied input.
     *
     * @param gasType the gas type to inspect or process
     * @return the resolved airtight drill handler
     * @throws IllegalArgumentException if an argument is invalid
     */
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

    /**
     * Registers a custom airtight drill handler for the supplied target.
     *
     * @param location    the resource location identifying the target value
     * @param damage      the damage value to use
     * @param consumption the consumption value to use
     */
    public static void register(ResourceLocation location, int damage, float consumption) {
        register(location, new AirtightDrillHandler() {
            /**
             * {@inheritDoc}
             */
            @Override
            public int getDamageAddition() {
                return damage;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public float getConsumptionMultiplier() {
                return consumption;
            }
        });
    }

    /**
     * Registers a custom airtight drill handler for the supplied target.
     *
     * @param location the resource location identifying the target value
     * @param handler  the handler to register or invoke
     */
    public static void register(ResourceLocation location, AirtightDrillHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Drill Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightDrillHandler drillHandler = AirtightDrillHandler.REGISTRY.get(gasType);
        if (drillHandler != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Drill Handler for gas '{}': a handler is already registered.", location);
            return;
        }
        if (!GasConsumptionUtils.isNonNegative(handler.getDamageAddition())) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Drill Handler for gas '{}': damage addition must be non-negative, got {}.", location, handler.getDamageAddition());
            return;
        }
        if (!GasConsumptionUtils.isNonNegativeFinite(handler.getConsumptionMultiplier())) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Drill Handler for gas '{}': consumption multiplier must be finite and non-negative, got {}.", location, handler.getConsumptionMultiplier());
            return;
        }

        AirtightDrillHandler.REGISTRY.register(gasType, handler);
    }
}
