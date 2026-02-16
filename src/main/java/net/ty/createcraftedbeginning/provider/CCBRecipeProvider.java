package net.ty.createcraftedbeginning.provider;

import com.simibubi.create.api.data.recipe.ProcessingRecipeGen;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeGen;
import net.ty.createcraftedbeginning.data.CCBCoolingRecipes;
import net.ty.createcraftedbeginning.data.CCBEnergizationRecipes;
import net.ty.createcraftedbeginning.data.CCBChillingRecipes;
import net.ty.createcraftedbeginning.data.CCBGasInjectionRecipes;
import net.ty.createcraftedbeginning.data.CCBPressurizationRecipes;
import net.ty.createcraftedbeginning.data.CCBReactorKettleRecipes;
import net.ty.createcraftedbeginning.data.CCBResidueGenerationRecipes;
import net.ty.createcraftedbeginning.data.CCBWindChargingRecipes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CCBRecipeProvider extends RecipeProvider {
    private static final List<ProcessingRecipeGen<?, ?, ?>> GENERATORS = new ArrayList<>();
    private static final List<ProcessingWithGasRecipeGen<?, ?, ?>> GENERATORS_WITH_GAS = new ArrayList<>();

    public CCBRecipeProvider(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries);
    }

    public static void registerAllProcessing(@NotNull DataGenerator generator, PackOutput output, CompletableFuture<Provider> registries) {
        GENERATORS.add(new CCBCoolingRecipes(output, registries));
        GENERATORS.add(new CCBChillingRecipes(output, registries));
        GENERATORS.add(new CCBWindChargingRecipes(output, registries));
        generator.addProvider(true, new DataProvider() {
            @Override
            public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
                return CompletableFuture.allOf(GENERATORS.stream().map(gen -> gen.run(cachedOutput)).toArray(CompletableFuture[]::new));
            }

            @Override
            public @NotNull String getName() {
                return "Create Crafted Beginning's Processing Recipes";
            }
        });
    }

    public static void registerAllProcessingWithGas(@NotNull DataGenerator generator, PackOutput output, CompletableFuture<Provider> registries) {
        GENERATORS_WITH_GAS.add(new CCBPressurizationRecipes(output, registries));
        GENERATORS_WITH_GAS.add(new CCBEnergizationRecipes(output, registries));
        GENERATORS_WITH_GAS.add(new CCBGasInjectionRecipes(output, registries));
        GENERATORS_WITH_GAS.add(new CCBReactorKettleRecipes(output, registries));
        GENERATORS_WITH_GAS.add(new CCBResidueGenerationRecipes(output, registries));

        generator.addProvider(true, new DataProvider() {
            @Override
            public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
                return CompletableFuture.allOf(GENERATORS_WITH_GAS.stream().map(gen -> gen.run(cachedOutput)).toArray(CompletableFuture[]::new));
            }

            @Override
            public @NotNull String getName() {
                return "Create Crafted Beginning's Processing Recipes With Gas";
            }
        });
    }
}
