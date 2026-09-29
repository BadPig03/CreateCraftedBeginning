package net.ty.createcraftedbeginning.api.cannonhandlers.visual;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.cannonhandlers.DefaultCannonHandler;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureHandlerRegistry;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightCannonVisualHandlers {
    private static final GameplayPressureHandlerRegistry<AirtightCannonVisualHandler> HANDLERS = GameplayPressureHandlerRegistry.create();

    private AirtightCannonVisualHandlers() {
    }

    /** Equipment uses the normal gas baseline; pressure-specific handlers remain available to other systems. */
    public static AirtightCannonVisualHandler resolveForEquipment(Gas gasType) {
        return resolve(gasType, GameplayPressureProfiles.NORMAL);
    }

    public static AirtightCannonVisualHandler resolve(GasStack gasStack, long sourcePressurePa) {
        return resolve(gasStack.getGasType(), GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static AirtightCannonVisualHandler resolve(GasStack gasStack, GameplayPressureProfile profile) {
        return resolve(gasStack.getGasType(), profile);
    }

    public static AirtightCannonVisualHandler resolve(Gas gasType, long sourcePressurePa) {
        return resolve(gasType, GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    public static AirtightCannonVisualHandler resolve(Gas gasType, GameplayPressureProfile profile) {
        if (gasType.isEmpty()) {
            return DefaultCannonHandler.INSTANCE;
        }

        AirtightCannonVisualHandler handler = HANDLERS.get(gasType, profile);
        if (handler == null) {
            return DefaultCannonHandler.INSTANCE;
        }

        return handler;
    }

    public static void register(ResourceLocation location, GameplayPressureProfile profile, AirtightCannonVisualHandler handler) {
        Gas gasType = Gas.findById(location);
        if (gasType.isEmpty()) {
            CCBAPI.LOGGER.error("Failed to register airtight cannon visual handler: gas '{}' does not exist.", location);
            return;
        }

        if (HANDLERS.containsExact(gasType, profile)) {
            CCBAPI.LOGGER.error("Failed to register airtight cannon visual handler for gas '{}' and gameplay pressure profile '{}': a handler is already registered.", location, profile.id());
            return;
        }

        HANDLERS.register(gasType, profile, handler);
    }
}
