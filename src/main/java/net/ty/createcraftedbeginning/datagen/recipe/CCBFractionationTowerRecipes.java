package net.ty.createcraftedbeginning.datagen.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.datagen.recipe.generator.FractionationTowerRecipeGen;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public final class CCBFractionationTowerRecipes extends FractionationTowerRecipeGen {
    private final GeneratedRecipe BRIMSTONE = create("brimstone_fractionation", builder -> builder.require(new SizedFluidIngredient(FluidIngredient.of(CCBFluids.BRIMSTONE.get().getSource()), 100)).temperatureCondition(TemperatureCondition.SUPERHEATED).duration(200).outputAtLayer(1, new FluidStack(Fluids.LAVA, 500)).outputAtLayer(2, new GasStack(CCBGases.ULTRAWARM_AIR.get(), 500)));
    private final GeneratedRecipe MOIST_AIR = create("moist_air_condensation", builder -> builder.require(CCBGases.MOIST_AIR.get(), 10000).temperatureCondition(TemperatureCondition.CHILLED).duration(100).outputAtLayer(1, new GasStack(CCBGases.NATURAL_AIR.get(), 9800)).outputAtLayer(2, new FluidStack(Fluids.WATER, 100)));

    public CCBFractionationTowerRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CCBAPI.MOD_ID);
    }
}
