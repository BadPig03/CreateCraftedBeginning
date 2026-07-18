package net.ty.createcraftedbeginning.api.armhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides lookup and registration helpers for gas-specific airtight arm behavior.
 * Handlers define gas consumption and the interaction-range and knockback modifiers granted by a gas.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightArmHandlerUtils {
    private static final AirtightArmHandler DEFAULT_HANDLER = DefaultArmHandler.INSTANCE;

    private AirtightArmHandlerUtils() {
    }

    /**
     * Resolves the airtight arm handler associated with the supplied input.
     *
     * @param gasStack the gas stack to inspect or process
     * @return the resolved airtight arm handler
     * @throws IllegalArgumentException if an argument is invalid
     */
    public static AirtightArmHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    /**
     * Resolves the airtight arm handler associated with the supplied input.
     *
     * @param gasType the gas type to inspect or process
     * @return the resolved airtight arm handler
     * @throws IllegalArgumentException if an argument is invalid
     */
    public static AirtightArmHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightArmHandler armHandler = AirtightArmHandler.REGISTRY.get(gasType);
        if (armHandler == null) {
            return DEFAULT_HANDLER;
        }
        return armHandler;
    }

    /**
     * Registers a custom airtight arm handler for the supplied target.
     *
     * @param location    the resource location identifying the target value
     * @param consumption the consumption value to use
     * @param blockRange  the block range value to use
     * @param entityRange the entity range value to use
     * @param knockback   the knockback value to use
     */
    public static void register(ResourceLocation location, float consumption, float blockRange, float entityRange, float knockback) {
        register(location, new AirtightArmStats(consumption, blockRange, entityRange, knockback));
    }

    /**
     * Registers a custom airtight arm handler for the supplied target.
     *
     * @param location the resource location identifying the target value
     * @param handler  the handler to register or invoke
     */
    public static void register(ResourceLocation location, AirtightArmHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Arm Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightArmHandler armHandler = AirtightArmHandler.REGISTRY.get(gasType);
        if (armHandler != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Arm Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        try {
            AirtightArmStats stats = handler instanceof AirtightArmStats existing ? existing : new AirtightArmStats(handler.getGasConsumptionMultiplier(), handler.getIncreasedBlockInteractionRange(), handler.getIncreasedEntityInteractionRange(), handler.getIncreasedKnockback());
            AirtightArmHandler.REGISTRY.register(gasType, stats);
        } catch (IllegalArgumentException exception) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Arm Handler for gas '{}': {}", location, exception.getMessage());
        }
    }
}
