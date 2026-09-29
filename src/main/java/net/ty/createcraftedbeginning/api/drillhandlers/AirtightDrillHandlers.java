package net.ty.createcraftedbeginning.api.drillhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureHandlerRegistry;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightDrillHandlers {
    private static final GameplayPressureHandlerRegistry<AirtightDrillHandler> HANDLERS = GameplayPressureHandlerRegistry.create();

    private AirtightDrillHandlers() {
    }

    /** Equipment uses the normal gas baseline; pressure-specific handlers remain available to other systems. */
    public static AirtightDrillHandler resolveForEquipment(Gas gasType) {
        return resolve(gasType, GameplayPressureProfiles.NORMAL);
    }

    public static AirtightDrillHandler resolve(GasStack gasStack, long sourcePressurePa) throws IllegalArgumentException {
        return resolve(gasStack.getGasType(), GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static AirtightDrillHandler resolve(GasStack gasStack, GameplayPressureProfile profile) throws IllegalArgumentException {
        return resolve(gasStack.getGasType(), profile);
    }

    public static AirtightDrillHandler resolve(Gas gasType, long sourcePressurePa) throws IllegalArgumentException {
        return resolve(gasType, GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static AirtightDrillHandler resolve(Gas gasType, GameplayPressureProfile profile) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException("Airtight drill handler resolution requires a non-empty gas.");
        }

        AirtightDrillHandler drillHandler = HANDLERS.get(gasType, profile);
        if (drillHandler == null) {
            return DefaultDrillHandler.INSTANCE;
        }

        return drillHandler;
    }

    public static void register(ResourceLocation location, GameplayPressureProfile profile, int damage, float consumption) {
        register(location, profile, createHandler(damage, consumption));
    }

    public static void register(ResourceLocation location, GameplayPressureProfile profile, AirtightDrillHandler handler) {
        Gas gasType = Gas.findById(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register airtight drill handler: gas '{}' does not exist.", location);
            return;
        }

        if (HANDLERS.containsExact(gasType, profile)) {
            CCBAPI.LOGGER.error("Failed to register airtight drill handler for gas '{}' and gameplay pressure profile '{}': a handler is already registered.", location, profile.id());
            return;
        }

        if (!GasConsumptionMath.isNonNegative(handler.getDamageAddition())) {
            CCBAPI.LOGGER.error("Failed to register airtight drill handler for gas '{}': damage addition must be non-negative, got {}.", location, handler.getDamageAddition());
            return;
        }

        if (!GasConsumptionMath.isNonNegativeFinite(handler.getConsumptionMultiplier())) {
            CCBAPI.LOGGER.error("Failed to register airtight drill handler for gas '{}': consumption multiplier must be finite and non-negative, got {}.", location, handler.getConsumptionMultiplier());
            return;
        }

        HANDLERS.register(gasType, profile, handler);
    }

    private static AirtightDrillHandler createHandler(int damage, float consumption) {
        return new AirtightDrillHandler() {
            @Override
            public int getDamageAddition() {
                return damage;
            }

            @Override
            public float getConsumptionMultiplier() {
                return consumption;
            }
        };
    }
}
