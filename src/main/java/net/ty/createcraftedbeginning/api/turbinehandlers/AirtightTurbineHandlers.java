package net.ty.createcraftedbeginning.api.turbinehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureHandlerRegistry;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightTurbineHandlers {
    private static final GameplayPressureHandlerRegistry<AirtightTurbineHandler> HANDLERS = GameplayPressureHandlerRegistry.create();

    private AirtightTurbineHandlers() {
    }

    public static AirtightTurbineHandler resolve(GasStack gasStack, long sourcePressurePa) throws IllegalArgumentException {
        return resolve(gasStack.getGasType(), GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static AirtightTurbineHandler resolve(GasStack gasStack, GameplayPressureProfile profile) throws IllegalArgumentException {
        return resolve(gasStack.getGasType(), profile);
    }

    public static AirtightTurbineHandler resolve(Gas gasType, long sourcePressurePa) throws IllegalArgumentException {
        return resolve(gasType, GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static AirtightTurbineHandler resolve(Gas gasType, GameplayPressureProfile profile) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException("Airtight turbine handler resolution requires a non-empty gas.");
        }

        AirtightTurbineHandler turbineHandler = HANDLERS.get(gasType, profile);
        if (turbineHandler == null) {
            return DefaultTurbineHandler.INSTANCE;
        }

        return turbineHandler;
    }

    public static void register(ResourceLocation location, GameplayPressureProfile profile, float maxLevel) {
        Gas gasType = Gas.findById(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register airtight turbine handler: gas '{}' does not exist.", location);
            return;
        }

        if (HANDLERS.containsExact(gasType, profile)) {
            CCBAPI.LOGGER.error("Failed to register airtight turbine handler for gas '{}' and gameplay pressure profile '{}': a handler is already registered.", location, profile.id());
            return;
        }

        if (!Float.isFinite(maxLevel) || maxLevel < 0 || maxLevel > AirtightTurbineHandler.MAX_LEVEL) {
            CCBAPI.LOGGER.error("Failed to register airtight turbine handler for gas '{}': maximum level must be finite and in [0, {}], got {}.", location, AirtightTurbineHandler.MAX_LEVEL, maxLevel);
            return;
        }

        HANDLERS.register(gasType, profile, () -> maxLevel);
    }
}
