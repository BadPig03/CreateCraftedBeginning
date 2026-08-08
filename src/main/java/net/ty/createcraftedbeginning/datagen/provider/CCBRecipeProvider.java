package net.ty.createcraftedbeginning.datagen.provider;

import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeGen;
import net.ty.createcraftedbeginning.datagen.recipe.CCBChillingRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBCoolingRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBDissipationRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBEnergizationRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBForgingPressRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBGasInjectionRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBPressurizationRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBReactorKettleRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBResidueGenerationRecipes;
import net.ty.createcraftedbeginning.datagen.recipe.CCBWindChargingRecipes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBRecipeProvider extends RecipeProvider {
    private static final List<BaseRecipeProvider> GENERATORS = new ArrayList<>();
    private static final List<ProcessingWithGasRecipeGen<?, ?, ?>> GENERATORS_WITH_GAS = new ArrayList<>();

    public CCBRecipeProvider(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries);
    }

    public static void registerAllProcessing(DataGenerator generator, PackOutput output, CompletableFuture<Provider> registries) {
        GENERATORS.add(new CCBChillingRecipes(output, registries));
        GENERATORS.add(new CCBCoolingRecipes(output, registries));
        GENERATORS.add(new CCBWindChargingRecipes(output, registries));
        generator.addProvider(true, new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput cachedOutput) {
                return CompletableFuture.allOf(GENERATORS.stream().map(gen -> gen.run(cachedOutput)).toArray(CompletableFuture[]::new));
            }

            @Override
            public String getName() {
                return "Create Crafted Beginning's Processing Recipes";
            }
        });
    }

    public static void registerAllProcessingWithGas(DataGenerator generator, PackOutput output, CompletableFuture<Provider> registries) {
        GENERATORS_WITH_GAS.add(new CCBDissipationRecipes(output, registries));
        GENERATORS_WITH_GAS.add(new CCBEnergizationRecipes(output, registries));
        GENERATORS_WITH_GAS.add(new CCBForgingPressRecipes(output, registries));
        GENERATORS_WITH_GAS.add(new CCBGasInjectionRecipes(output, registries));
        GENERATORS_WITH_GAS.add(new CCBPressurizationRecipes(output, registries));
        GENERATORS_WITH_GAS.add(new CCBReactorKettleRecipes(output, registries));
        GENERATORS_WITH_GAS.add(new CCBResidueGenerationRecipes(output, registries));

        generator.addProvider(true, new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput cachedOutput) {
                return CompletableFuture.allOf(GENERATORS_WITH_GAS.stream().map(gen -> gen.run(cachedOutput)).toArray(CompletableFuture[]::new));
            }

            @Override
            public String getName() {
                return "Create Crafted Beginning's Processing Recipes With Gas";
            }
        });
    }
}
