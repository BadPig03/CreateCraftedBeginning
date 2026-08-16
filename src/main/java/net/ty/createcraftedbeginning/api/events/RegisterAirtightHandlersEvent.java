package net.ty.createcraftedbeginning.api.events;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandler;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandlerUtils;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlerUtils;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlerUtils;
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandler;
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandlerUtils;
import net.ty.createcraftedbeginning.api.drainagehandlers.AirtightDrainageHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.AirtightDrainageHandlerUtils;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlerUtils;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlerUtils;
import net.ty.createcraftedbeginning.api.fillhandlers.AirtightFillHandler;
import net.ty.createcraftedbeginning.api.fillhandlers.AirtightFillHandlerUtils;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandlerUtils;
import net.ty.createcraftedbeginning.api.turbinehandlers.AirtightTurbineHandlerUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public final class RegisterAirtightHandlersEvent extends Event {
    public void registerArm(ResourceLocation gas, AirtightArmHandler handler) {
        AirtightArmHandlerUtils.register(gas, handler);
    }

    public void registerArm(ResourceLocation gas, float consumption, float blockRange, float entityRange, float knockback) {
        AirtightArmHandlerUtils.register(gas, consumption, blockRange, entityRange, knockback);
    }

    public void registerArmors(ResourceLocation gas, AirtightArmorsHandler handler) {
        AirtightArmorsHandlerUtils.register(gas, handler);
    }

    public void registerCannon(ResourceLocation gas, AirtightCannonHandler handler) {
        AirtightCannonHandlerUtils.register(gas, handler);
    }

    public void registerCoolant(Block block, AirtightCoolantHandler handler) {
        AirtightCoolantHandlerUtils.register(block, handler);
    }

    public void registerDrainage(ResourceLocation gas, AirtightDrainageHandler handler) {
        AirtightDrainageHandlerUtils.register(gas, handler);
    }

    public void registerDrill(ResourceLocation gas, AirtightDrillHandler handler) {
        AirtightDrillHandlerUtils.register(gas, handler);
    }

    public void registerDrill(ResourceLocation gas, int damage, float consumption) {
        AirtightDrillHandlerUtils.register(gas, damage, consumption);
    }

    public void registerEngine(ResourceLocation gas, double workFactor) {
        AirtightEngineHandlerUtils.register(gas, workFactor);
    }

    public void registerEngine(ResourceLocation gas, double workFactor, int maxLevel) {
        AirtightEngineHandlerUtils.register(gas, workFactor, maxLevel);
    }

    public void registerFill(Block block, AirtightFillHandler handler) {
        AirtightFillHandlerUtils.register(block, handler);
    }

    public void registerThermoregulator(Block block, AirtightThermoregulatorHandler handler) {
        AirtightThermoregulatorHandlerUtils.register(block, handler);
    }

    public void registerTurbine(ResourceLocation gas, int maxLevel) {
        AirtightTurbineHandlerUtils.register(gas, maxLevel);
    }
}
