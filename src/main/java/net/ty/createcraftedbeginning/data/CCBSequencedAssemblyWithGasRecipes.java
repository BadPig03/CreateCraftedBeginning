package net.ty.createcraftedbeginning.data;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.recipes.CuttingWithGasRecipe;
import net.ty.createcraftedbeginning.api.gas.recipes.DeployerApplicationWithGasRecipe;
import net.ty.createcraftedbeginning.api.gas.recipes.FillingWithGasRecipe;
import net.ty.createcraftedbeginning.api.gas.recipes.PressingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.generators.SequencedAssemblyWithGasRecipeGen;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBTags;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBSequencedAssemblyWithGasRecipes extends SequencedAssemblyWithGasRecipeGen {
    GeneratedRecipe HEAVY_CORE = create("heavy_core", b -> b.require(CCBTags.commonItemTag("obsidians/crying")).transitionTo(CCBItems.INCOMPLETE_HEAVY_CORE).addOutput(Items.HEAVY_CORE, 75).addOutput(Items.NETHERITE_INGOT, 4, 25).loops(4).addStep(DeployerApplicationWithGasRecipe::new, rb -> rb.require(Items.NETHERITE_INGOT)).addStep(FillingWithGasRecipe::new, rb -> rb.require(Fluids.LAVA, 500)).addStep(DeployerApplicationWithGasRecipe::new, rb -> rb.require(CCBItems.AIRTIGHT_SHEET)).addStep(GasInjectionRecipe::new, rb -> rb.require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 125)).addStep(PressingWithGasRecipe::new, rb -> rb));
    GeneratedRecipe BREEZE_CORE = create("breeze_core", b -> b.require(Items.HEAVY_CORE).transitionTo(CCBItems.INCOMPLETE_BREEZE_CORE).addOutput(CCBItems.BREEZE_CORE, 100).loops(1).addStep(CuttingWithGasRecipe::new, rb -> rb.duration(100)).addStep(FillingWithGasRecipe::new, rb -> rb.require(CCBFluids.AMETHYST_SUSPENSION.get(), 250)).addStep(GasInjectionRecipe::new, rb -> rb.require(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 250)).addStep(PressingWithGasRecipe::new, rb -> rb));
    GeneratedRecipe ANCHOR_FLARE = create("anchor_flare", b -> b.require(CCBItems.UNFILLED_WEATHER_FLARE).transitionTo(CCBItems.INCOMPLETE_ANCHOR_FLARE).addOutput(CCBItems.ANCHOR_FLARE, 80).addOutput(CCBItems.UNFILLED_WEATHER_FLARE, 20).loops(4).addStep(DeployerApplicationWithGasRecipe::new, rb -> rb.require(Items.GUNPOWDER)).addStep(GasInjectionRecipe::new, rb -> rb.require(CCBGases.ULTRAWARM_AIR.get(), 500)).addStep(GasInjectionRecipe::new, rb -> rb.require(CCBGases.MOIST_AIR.get(), 500)).addStep(GasInjectionRecipe::new, rb -> rb.require(CCBGases.ETHEREAL_AIR.get(), 500)));

    public CCBSequencedAssemblyWithGasRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
