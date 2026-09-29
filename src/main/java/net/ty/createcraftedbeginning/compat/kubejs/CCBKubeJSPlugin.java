package net.ty.createcraftedbeginning.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentTypeRegistry;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import dev.latvian.mods.kubejs.registry.BuilderTypeRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptType;
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
import net.ty.createcraftedbeginning.compat.kubejs.recipe.CCBRecipeComponents;
import net.ty.createcraftedbeginning.compat.kubejs.recipe.CCBRecipeSchemas;
import net.ty.createcraftedbeginning.compat.kubejs.recipe.GasRecipeValue;
import net.ty.createcraftedbeginning.compat.kubejs.recipe.ItemRecipeOutput;
import net.ty.createcraftedbeginning.compat.kubejs.registry.GasKubeJSBuilder;
import net.ty.createcraftedbeginning.registry.CCBRegistries;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("CCBGas", GasRecipeValue.class);
        bindings.add("CCBItem", ItemRecipeOutput.class);
    }

    @Override
    public void registerRecipeComponents(RecipeComponentTypeRegistry registry) {
        CCBRecipeComponents.register(registry);
    }

    @Override
    public void registerRecipeSchemas(RecipeSchemaRegistry registry) {
        CCBRecipeSchemas.register(registry);
    }

    @Override
    public void afterInit() {
        CCBEvents.ATMOSPHERE_PROVIDER.post(ScriptType.STARTUP, new AtmosphereProviderEvent());
        CCBEvents.AIRTIGHT_ARM_HANDLER.post(ScriptType.STARTUP, new AirtightArmHandlerEvent());
        CCBEvents.AIRTIGHT_ARMORS_HANDLER.post(ScriptType.STARTUP, new AirtightArmorsHandlerEvent());
        CCBEvents.AIRTIGHT_CANNON_HANDLER.post(ScriptType.STARTUP, new AirtightCannonHandlerEvent());
        CCBEvents.AIRTIGHT_DRILL_HANDLER.post(ScriptType.STARTUP, new AirtightDrillHandlerEvent());
        CCBEvents.AIRTIGHT_ENGINE_HANDLER.post(ScriptType.STARTUP, new AirtightEngineHandlerEvent());
        CCBEvents.AIRTIGHT_THERMOREGULATOR_HANDLER.post(ScriptType.STARTUP, new AirtightThermoregulatorHandlerEvent());
        CCBEvents.AIRTIGHT_TURBINE_HANDLER.post(ScriptType.STARTUP, new AirtightTurbineHandlerEvent());
        CCBEvents.AIRTIGHT_UPGRADE_MATERIALS.post(ScriptType.STARTUP, new AirtightUpgradeMaterialsEvent());
        CCBEvents.GAS_RELEASE_HANDLER.post(ScriptType.STARTUP, new GasReleaseHandlerEvent());
    }

    @Override
    public void registerBuilderTypes(BuilderTypeRegistry registry) {
        registry.addDefault(CCBRegistries.GAS_REGISTRY_KEY, GasKubeJSBuilder.class, GasKubeJSBuilder::new);
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(CCBEvents.GROUP);
    }
}
