package net.ty.createcraftedbeginning.datagen.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.datagen.recipe.generator.SequencedAssemblyWithGasRecipeGen;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.gas.CuttingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.DeployerApplicationWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.FillingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.PressingWithGasRecipe;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBTags;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBSequencedAssemblyWithGasRecipes extends SequencedAssemblyWithGasRecipeGen {
    private final GeneratedRecipe HEAVY_CORE = create("heavy_core", builder -> builder.require(CCBTags.commonItemTag("obsidians/crying")).transitionTo(CCBItems.INCOMPLETE_HEAVY_CORE).addOutput(Items.HEAVY_CORE, 75).addOutput(Items.NETHERITE_INGOT, 4, 25).loops(4).addStep(DeployerApplicationWithGasRecipe::new, step -> step.require(Items.NETHERITE_INGOT)).addStep(FillingWithGasRecipe::new, step -> step.require(Fluids.LAVA, 500)).addStep(DeployerApplicationWithGasRecipe::new, step -> step.require(CCBItems.AIRTIGHT_SHEET)).addStep(GasInjectionRecipe::new, step -> step.require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 125)).addStep(PressingWithGasRecipe::new, step -> step));
    private final GeneratedRecipe BREEZE_CORE = create("breeze_core", builder -> builder.require(Items.HEAVY_CORE).transitionTo(CCBItems.INCOMPLETE_BREEZE_CORE).addOutput(CCBItems.BREEZE_CORE, 100).loops(1).addStep(CuttingWithGasRecipe::new, step -> step.duration(100)).addStep(FillingWithGasRecipe::new, step -> step.require(CCBFluids.AMETHYST_SUSPENSION.get(), 250)).addStep(GasInjectionRecipe::new, step -> step.require(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 250)).addStep(PressingWithGasRecipe::new, step -> step));
    private final GeneratedRecipe ANCHOR_FLARE = create("anchor_flare", builder -> builder.require(CCBItems.UNFILLED_WEATHER_FLARE).transitionTo(CCBItems.INCOMPLETE_ANCHOR_FLARE).addOutput(CCBItems.ANCHOR_FLARE, 80).addOutput(CCBItems.UNFILLED_WEATHER_FLARE, 20).loops(4).addStep(DeployerApplicationWithGasRecipe::new, step -> step.require(Items.GUNPOWDER)).addStep(GasInjectionRecipe::new, step -> step.require(CCBGases.ULTRAWARM_AIR.get(), 500)).addStep(GasInjectionRecipe::new, step -> step.require(CCBGases.MOIST_AIR.get(), 500)).addStep(GasInjectionRecipe::new, step -> step.require(CCBGases.ETHEREAL_AIR.get(), 500)));

    public CCBSequencedAssemblyWithGasRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CCBAPI.MOD_ID);
    }
}
