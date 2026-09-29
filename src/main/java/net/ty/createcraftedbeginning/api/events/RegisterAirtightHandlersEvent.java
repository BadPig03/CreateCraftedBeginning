package net.ty.createcraftedbeginning.api.events;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandler;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandlers;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlers;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlers;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlers;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlers;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandler;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandlers;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandlers;
import net.ty.createcraftedbeginning.api.turbinehandlers.AirtightTurbineHandlers;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Registers additional handlers during common setup, after pressure profiles are sealed
 * and built-in handlers are registered. Subscribe on your own mod event bus using
 * {@code modBus.addListener(...)} or
 * {@code @EventBusSubscriber(modid = "your_mod", bus = EventBusSubscriber.Bus.MOD)}.
 * Register directly in the synchronous callback; atmosphere provider registration follows.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public final class RegisterAirtightHandlersEvent extends Event implements IModBusEvent {
    public void registerArm(ResourceLocation gas, GameplayPressureProfile profile, AirtightArmHandler handler) {
        AirtightArmHandlers.register(gas, profile, handler);
    }

    public void registerArm(ResourceLocation gas, GameplayPressureProfile profile, float consumption, float blockRange, float entityRange, float knockback) {
        AirtightArmHandlers.register(gas, profile, consumption, blockRange, entityRange, knockback);
    }

    public void registerArmors(ResourceLocation gas, GameplayPressureProfile profile, AirtightArmorsHandler handler) {
        AirtightArmorsHandlers.register(gas, profile, handler);
    }

    public void registerCannon(ResourceLocation gas, GameplayPressureProfile profile, AirtightCannonHandler handler) {
        AirtightCannonHandlers.register(gas, profile, handler);
    }

    public void registerGasRelease(ResourceLocation gas, GameplayPressureProfile profile, GasReleaseHandler handler) {
        GasReleaseHandlers.register(gas, profile, handler);
    }

    public void registerDrill(ResourceLocation gas, GameplayPressureProfile profile, AirtightDrillHandler handler) {
        AirtightDrillHandlers.register(gas, profile, handler);
    }

    public void registerDrill(ResourceLocation gas, GameplayPressureProfile profile, int damage, float consumption) {
        AirtightDrillHandlers.register(gas, profile, damage, consumption);
    }

    public void registerEngine(ResourceLocation gas, GameplayPressureProfile profile, double workFactor) {
        AirtightEngineHandlers.register(gas, profile, workFactor);
    }

    public void registerEngine(ResourceLocation gas, GameplayPressureProfile profile, double workFactor, int maxLevel) {
        AirtightEngineHandlers.register(gas, profile, workFactor, maxLevel);
    }

    public void registerThermoregulator(Block block, AirtightThermoregulatorHandler handler) {
        AirtightThermoregulatorHandlers.register(block, handler);
    }

    public void registerTurbine(ResourceLocation gas, GameplayPressureProfile profile, float maxLevel) {
        AirtightTurbineHandlers.register(gas, profile, maxLevel);
    }
}
