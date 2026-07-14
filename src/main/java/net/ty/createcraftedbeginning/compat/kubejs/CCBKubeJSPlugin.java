package net.ty.createcraftedbeginning.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.registry.BuilderTypeRegistry;
import dev.latvian.mods.kubejs.script.ScriptType;
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
import net.ty.createcraftedbeginning.compat.kubejs.registry.GasKubeJSBuilder;
import net.ty.createcraftedbeginning.registry.CCBRegistries;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void afterInit() {
        CCBEvents.AIRTIGHT_ARM_HANDLER.post(ScriptType.STARTUP, new AirtightArmHandlerEvent());
        CCBEvents.AIRTIGHT_ARMORS_HANDLER.post(ScriptType.STARTUP, new AirtightArmorsHandlerEvent());
        CCBEvents.AIRTIGHT_CANNON_HANDLER.post(ScriptType.STARTUP, new AirtightCannonHandlerEvent());
        CCBEvents.AIRTIGHT_COOLANT_HANDLER.post(ScriptType.STARTUP, new AirtightCoolantHandlerEvent());
        CCBEvents.AIRTIGHT_DRAINAGE_HANDLER.post(ScriptType.STARTUP, new AirtightDrainageHandlerEvent());
        CCBEvents.AIRTIGHT_DRILL_HANDLER.post(ScriptType.STARTUP, new AirtightDrillHandlerEvent());
        CCBEvents.AIRTIGHT_ENGINE_HANDLER.post(ScriptType.STARTUP, new AirtightEngineHandlerEvent());
        CCBEvents.AIRTIGHT_FILL_HANDLER.post(ScriptType.STARTUP, new AirtightFillHandlerEvent());
        CCBEvents.AIRTIGHT_THERMOREGULATOR_HANDLER.post(ScriptType.STARTUP, new AirtightThermoregulatorHandlerEvent());
        CCBEvents.AIRTIGHT_TURBINE_HANDLER.post(ScriptType.STARTUP, new AirtightTurbineHandlerEvent());
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(CCBEvents.GROUP);
    }

    @Override
    public void registerBuilderTypes(BuilderTypeRegistry registry) {
        registry.addDefault(CCBRegistries.GAS_REGISTRY_KEY, GasKubeJSBuilder.class, GasKubeJSBuilder::new);
    }
}