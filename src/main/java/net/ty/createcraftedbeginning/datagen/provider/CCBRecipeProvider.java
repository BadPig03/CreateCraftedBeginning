package net.ty.createcraftedbeginning.datagen.provider;

import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.ty.createcraftedbeginning.datagen.recipe.CCBChillingRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBCoolingRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBDissipationRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBEnergizationRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBForgingPressRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBFractionationTowerRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBGasInjectionRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBReactorKettleRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBResidueGenerationRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBWindChargingRecipes;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipeGen;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBRecipeProvider extends RecipeProvider {
    private static final List<BaseRecipeProvider> PROCESSING_GENERATORS = new ArrayList<>();
    private static final List<GasProcessingRecipeGen<?, ?, ?>> GAS_PROCESSING_GENERATORS = new ArrayList<>();

    public CCBRecipeProvider(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries);
    }

    public static void registerAllProcessing(DataGenerator generator, PackOutput output, CompletableFuture<Provider> registries) {
        PROCESSING_GENERATORS.add(new CCBChillingRecipes(output, registries));
        PROCESSING_GENERATORS.add(new CCBCoolingRecipes(output, registries));
        PROCESSING_GENERATORS.add(new CCBWindChargingRecipes(output, registries));
        generator.addProvider(true, new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput cachedOutput) {
                return CompletableFuture.allOf(PROCESSING_GENERATORS.stream().map(gen -> gen.run(cachedOutput)).toArray(CompletableFuture[]::new));
            }

            @Override
            public String getName() {
                return "Create Crafted Beginning's Processing Recipes";
            }
        });
    }

    public static void registerAllGasProcessing(DataGenerator generator, PackOutput output, CompletableFuture<Provider> registries) {
        GAS_PROCESSING_GENERATORS.add(new CCBDissipationRecipes(output, registries));
        GAS_PROCESSING_GENERATORS.add(new CCBEnergizationRecipes(output, registries));
        GAS_PROCESSING_GENERATORS.add(new CCBForgingPressRecipes(output, registries));
        GAS_PROCESSING_GENERATORS.add(new CCBGasInjectionRecipes(output, registries));
        GAS_PROCESSING_GENERATORS.add(new CCBReactorKettleRecipes(output, registries));
        GAS_PROCESSING_GENERATORS.add(new CCBFractionationTowerRecipes(output, registries));
        GAS_PROCESSING_GENERATORS.add(new CCBResidueGenerationRecipes(output, registries));

        generator.addProvider(true, new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput cachedOutput) {
                return CompletableFuture.allOf(GAS_PROCESSING_GENERATORS.stream().map(gen -> gen.run(cachedOutput)).toArray(CompletableFuture[]::new));
            }

            @Override
            public String getName() {
                return "Create Crafted Beginning's Gas Processing Recipes";
            }
        });
    }
}
