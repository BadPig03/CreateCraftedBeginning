package net.ty.createcraftedbeginning.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.registry.BuilderTypeRegistry;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightDrainageHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightEngineHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightFillHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightTurbineHandlerEvent;
import net.ty.createcraftedbeginning.compat.kubejs.registry.GasKubeJSBuilder;
import net.ty.createcraftedbeginning.registry.CCBRegistries;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void afterInit() {
        CCBEvents.AIRTIGHT_DRAINAGE_HANDLER.post(ScriptType.STARTUP, new AirtightDrainageHandlerEvent());
        CCBEvents.AIRTIGHT_ENGINE_HANDLER.post(ScriptType.STARTUP, new AirtightEngineHandlerEvent());
        CCBEvents.AIRTIGHT_FILL_HANDLER.post(ScriptType.STARTUP, new AirtightFillHandlerEvent());
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