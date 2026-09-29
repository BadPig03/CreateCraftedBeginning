package net.ty.createcraftedbeginning.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightArmHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightArmorsHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightCannonHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightDrillHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightEngineHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightThermoregulatorHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightTurbineHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightUpgradeMaterialsEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AtmosphereProviderEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.GasReleaseHandlerEvent;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public interface CCBEvents {
    EventGroup GROUP = EventGroup.of("CCBEvents");

    EventHandler ATMOSPHERE_PROVIDER = GROUP.startup("atmosphereProvider", () -> AtmosphereProviderEvent.class);

    EventHandler AIRTIGHT_ARM_HANDLER = GROUP.startup("airtightArmHandler", () -> AirtightArmHandlerEvent.class);
    EventHandler AIRTIGHT_ARMORS_HANDLER = GROUP.startup("airtightArmorsHandler", () -> AirtightArmorsHandlerEvent.class);
    EventHandler AIRTIGHT_CANNON_HANDLER = GROUP.startup("airtightCannonHandler", () -> AirtightCannonHandlerEvent.class);
    EventHandler AIRTIGHT_DRILL_HANDLER = GROUP.startup("airtightDrillHandler", () -> AirtightDrillHandlerEvent.class);
    EventHandler AIRTIGHT_ENGINE_HANDLER = GROUP.startup("airtightEngineHandler", () -> AirtightEngineHandlerEvent.class);
    EventHandler AIRTIGHT_THERMOREGULATOR_HANDLER = GROUP.startup("airtightThermoregulatorHandler", () -> AirtightThermoregulatorHandlerEvent.class);
    EventHandler AIRTIGHT_TURBINE_HANDLER = GROUP.startup("airtightTurbineHandler", () -> AirtightTurbineHandlerEvent.class);
    EventHandler AIRTIGHT_UPGRADE_MATERIALS = GROUP.startup("airtightUpgradeMaterials", () -> AirtightUpgradeMaterialsEvent.class);
    EventHandler GAS_RELEASE_HANDLER = GROUP.startup("gasReleaseHandler", () -> GasReleaseHandlerEvent.class);
}
