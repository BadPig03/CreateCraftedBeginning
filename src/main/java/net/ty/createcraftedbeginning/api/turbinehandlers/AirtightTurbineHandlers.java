package net.ty.createcraftedbeginning.api.turbinehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightTurbineHandlers {
    private static final Map<Gas, AirtightTurbineHandler> HANDLERS = new HashMap<>();

    private AirtightTurbineHandlers() {
    }

    public static AirtightTurbineHandler resolve(GasStack gasStack) throws IllegalArgumentException {
        return resolve(gasStack.getGasType());
    }

    public static AirtightTurbineHandler resolve(Gas gasType) throws IllegalArgumentException {
        if (gasType.isEmpty()) {
            throw new IllegalArgumentException("Airtight turbine handler resolution requires a non-empty gas.");
        }

        AirtightTurbineHandler turbineHandler = HANDLERS.get(gasType);
        if (turbineHandler == null) {
            return DefaultTurbineHandler.INSTANCE;
        }

        return turbineHandler;
    }

    public static void register(ResourceLocation location, float baseLevel, float maxLevel) {
        Gas gasType = Gas.findById(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register airtight turbine handler: gas '{}' does not exist.", location);
            return;
        }

        if (HANDLERS.containsKey(gasType)) {
            CCBAPI.LOGGER.error("Failed to register airtight turbine handler for gas '{}': a handler is already registered.", location);
            return;
        }

        if (!Float.isFinite(baseLevel) || !Float.isFinite(maxLevel) || baseLevel < 0 || baseLevel > maxLevel || maxLevel > AirtightTurbineHandler.MAX_LEVEL) {
            CCBAPI.LOGGER.error("Failed to register airtight turbine handler for gas '{}': levels must be finite and satisfy 0 <= base <= maximum <= {}, got base {} and maximum {}.", location, AirtightTurbineHandler.MAX_LEVEL, baseLevel, maxLevel);
            return;
        }

        HANDLERS.put(gasType, new PressureCurve(baseLevel, maxLevel));
    }

    private record PressureCurve(float baseLevel, float maxLevel) implements AirtightTurbineHandler {
        @Override
        public float getBaseLevel() {
            return baseLevel;
        }

        @Override
        public float getMaxLevel() {
            return maxLevel;
        }
    }
}
