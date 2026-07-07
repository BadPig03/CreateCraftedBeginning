package net.ty.createcraftedbeginning.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightArmHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightArmorsHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightCannonHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightCoolantHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightDrainageHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightDrillHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightEngineHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightFillHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightThermoregulatorHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightTurbineHandlerEvent;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public interface CCBEvents {
    EventGroup GROUP = EventGroup.of("CCBEvents");

    EventHandler AIRTIGHT_ARM_HANDLER = GROUP.startup("airtightArmHandler", () -> AirtightArmHandlerEvent.class);
    EventHandler AIRTIGHT_ARMORS_HANDLER = GROUP.startup("airtightArmorsHandler", () -> AirtightArmorsHandlerEvent.class);
    EventHandler AIRTIGHT_CANNON_HANDLER = GROUP.startup("airtightCannonHandler", () -> AirtightCannonHandlerEvent.class);
    EventHandler AIRTIGHT_COOLANT_HANDLER = GROUP.startup("airtightCoolantHandler", () -> AirtightCoolantHandlerEvent.class);
    EventHandler AIRTIGHT_DRAINAGE_HANDLER = GROUP.startup("airtightDrainageHandler", () -> AirtightDrainageHandlerEvent.class);
    EventHandler AIRTIGHT_DRILL_HANDLER = GROUP.startup("airtightDrillHandler", () -> AirtightDrillHandlerEvent.class);
    EventHandler AIRTIGHT_ENGINE_HANDLER = GROUP.startup("airtightEngineHandler", () -> AirtightEngineHandlerEvent.class);
    EventHandler AIRTIGHT_FILL_HANDLER = GROUP.startup("airtightFillHandler", () -> AirtightFillHandlerEvent.class);
    EventHandler AIRTIGHT_THERMOREGULATOR_HANDLER = GROUP.startup("airtightThermoregulatorHandler", () -> AirtightThermoregulatorHandlerEvent.class);
    EventHandler AIRTIGHT_TURBINE_HANDLER = GROUP.startup("airtightTurbineHandler", () -> AirtightTurbineHandlerEvent.class);
}
