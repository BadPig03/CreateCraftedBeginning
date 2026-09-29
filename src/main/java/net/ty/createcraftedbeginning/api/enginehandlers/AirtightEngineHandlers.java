package net.ty.createcraftedbeginning.api.enginehandlers;

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
public final class AirtightEngineHandlers {
    public static final double MAX_WORK_FACTOR = 32;
    private static final GameplayPressureHandlerRegistry<AirtightEngineHandler> HANDLERS = GameplayPressureHandlerRegistry.create();

    private AirtightEngineHandlers() {
    }

    public static AirtightEngineHandler resolve(GasStack gasStack, long sourcePressurePa) throws IllegalArgumentException {
        return resolve(gasStack.getGasType(), GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static AirtightEngineHandler resolve(GasStack gasStack, GameplayPressureProfile profile) throws IllegalArgumentException {
        return resolve(gasStack.getGasType(), profile);
    }

    public static AirtightEngineHandler resolve(Gas gasType, long sourcePressurePa) throws IllegalArgumentException {
        return resolve(gasType, GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static AirtightEngineHandler resolve(Gas gasType, GameplayPressureProfile profile) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException("Airtight engine handler resolution requires a non-empty gas.");
        }

        AirtightEngineHandler engineHandler = HANDLERS.get(gasType, profile);
        if (engineHandler == null) {
            return DefaultEngineHandler.INSTANCE;
        }

        return engineHandler;
    }

    public static void register(ResourceLocation location, GameplayPressureProfile profile, double workFactor) {
        register(location, profile, workFactor, AirtightEngineHandler.MAX_LEVEL);
    }

    public static void register(ResourceLocation location, GameplayPressureProfile profile, double workFactor, int maxLevel) {
        Gas gasType = Gas.findById(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register airtight engine handler: gas '{}' does not exist.", location);
            return;
        }

        if (HANDLERS.containsExact(gasType, profile)) {
            CCBAPI.LOGGER.error("Failed to register airtight engine handler for gas '{}' and gameplay pressure profile '{}': a handler is already registered.", location, profile.id());
            return;
        }

        if (!GasConsumptionMath.isFinite(workFactor) || workFactor <= 0 || workFactor > MAX_WORK_FACTOR) {
            CCBAPI.LOGGER.error("Failed to register airtight engine handler for gas '{}': work factor must be finite and in (0, {}], got {}.", location, MAX_WORK_FACTOR, workFactor);
            return;
        }

        if (maxLevel < 0 || maxLevel > AirtightEngineHandler.MAX_LEVEL) {
            CCBAPI.LOGGER.error("Failed to register airtight engine handler for gas '{}': maximum level must be in [0, {}], got {}.", location, AirtightEngineHandler.MAX_LEVEL, maxLevel);
            return;
        }

        AirtightEngineHandler handler = new AirtightEngineHandler() {
            @Override
            public double getWorkFactor() {
                return workFactor;
            }

            @Override
            public int getMaxLevel() {
                return maxLevel;
            }
        };
        HANDLERS.register(gasType, profile, handler);
    }
}
