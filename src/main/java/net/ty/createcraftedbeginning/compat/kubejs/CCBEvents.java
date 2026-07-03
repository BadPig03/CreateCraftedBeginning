package net.ty.createcraftedbeginning.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightDrainageHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightEngineHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightFillHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightTurbineHandlerEvent;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public interface CCBEvents {
    EventGroup GROUP = EventGroup.of("CCBEvents");

    EventHandler AIRTIGHT_DRAINAGE_HANDLER = GROUP.startup("airtightDrainageHandler", () -> AirtightDrainageHandlerEvent.class);
    EventHandler AIRTIGHT_ENGINE_HANDLER = GROUP.startup("airtightEngineHandler", () -> AirtightEngineHandlerEvent.class);
    EventHandler AIRTIGHT_FILL_HANDLER = GROUP.startup("airtightFillHandler", () -> AirtightFillHandlerEvent.class);
    EventHandler AIRTIGHT_TURBINE_HANDLER = GROUP.startup("airtightTurbineHandler", () -> AirtightTurbineHandlerEvent.class);
}
