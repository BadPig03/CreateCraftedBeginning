package net.ty.createcraftedbeginning.api.armorhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptionUtils;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightArmorsHandlerEvent.ArmorsHandler;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides lookup and registration helpers for gas-specific airtight armor behavior.
 * Registered handlers control effect curing, per-slot gas consumption, and elytra boosting.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightArmorsHandlerUtils {
    private AirtightArmorsHandlerUtils() {
    }

    /**
     * Resolves the airtight armors handler associated with the supplied input.
     *
     * @param gasStack the gas stack to inspect or process
     * @return the resolved airtight armors handler
     * @throws IllegalArgumentException if an argument is invalid
     */
    public static AirtightArmorsHandler of(GasStack gasStack) throws IllegalArgumentException {
        return of(gasStack.getGasType());
    }

    /**
     * Resolves the airtight armors handler associated with the supplied input.
     *
     * @param gasType the gas type to inspect or process
     * @return the resolved airtight armors handler
     * @throws IllegalArgumentException if an argument is invalid
     */
    public static AirtightArmorsHandler of(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException();
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasType);
        if (armorsHandler == null) {
            return DefaultArmorsHandler.INSTANCE;
        }
        return armorsHandler;
    }

    /**
     * Registers a custom airtight armors handler for the supplied target.
     *
     * @param location   the resource location identifying the target value
     * @param handler    the handler to register or invoke
     * @param helmet     the helmet value to use
     * @param chestplate the chestplate value to use
     * @param leggings   the leggings value to use
     * @param boots      the boots value to use
     * @param elytra     the elytra value to use
     */
    public static void register(ResourceLocation location, ArmorsHandler handler, float helmet, float chestplate, float leggings, float boots, float elytra) {
        register(location, new AirtightArmorsHandler() {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean canCureEffect(MobEffectInstance effectInstance) {
                return handler.apply(effectInstance);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public float getConsumptionMultiplier(EquipmentSlot slot) {
                return switch (slot) {
                    case HEAD -> helmet;
                    case CHEST -> chestplate;
                    case LEGS -> leggings;
                    case FEET -> boots;
                    default -> 1;
                };
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public float getMultiplierForBoostingElytra() {
                return elytra;
            }
        });
    }

    /**
     * Registers a custom airtight armors handler for the supplied target.
     *
     * @param location the resource location identifying the target value
     * @param handler  the handler to register or invoke
     */
    public static void register(ResourceLocation location, AirtightArmorsHandler handler) {
        Gas gasType = Gas.getGasTypeByName(location);
        if (gasType.isEmpty()) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Armors Handler: gas '{}' does not exist.", location);
            return;
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasType);
        if (armorsHandler != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Armors Handler for gas '{}': a handler is already registered.", location);
            return;
        }

        if (!validateHandler(location, handler)) {
            return;
        }

        AirtightArmorsHandler.REGISTRY.register(gasType, handler);
    }

    private static boolean validateHandler(ResourceLocation location, AirtightArmorsHandler handler) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            float multiplier = handler.getConsumptionMultiplier(slot);
            if (!GasConsumptionUtils.isNonNegativeFinite(multiplier)) {
                CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Armors Handler for gas '{}': {} multiplier must be finite and non-negative, got {}.", location, slot, multiplier);
                return false;
            }
        }

        float elytraMultiplier = handler.getMultiplierForBoostingElytra();
        if (!GasConsumptionUtils.isNonNegativeFinite(elytraMultiplier)) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Armors Handler for gas '{}': elytra multiplier must be finite and non-negative, got {}.", location, elytraMultiplier);
            return false;
        }
        return true;
    }

}
