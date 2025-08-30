package net.ty.createcraftedbeginning.data;

import com.simibubi.create.api.data.recipe.ProcessingRecipeGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.ty.createcraftedbeginning.recipe.CCBCoolingRecipes;
import net.ty.createcraftedbeginning.recipe.CCBGasInjectionRecipes;
import net.ty.createcraftedbeginning.recipe.CCBPressurizationRecipes;
import net.ty.createcraftedbeginning.recipe.CCBSuperCoolingRecipes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CCBRecipeProvider extends RecipeProvider {
    static final List<ProcessingRecipeGen<?, ?, ?>> GENERATORS = new ArrayList<>();

    public CCBRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    public static void registerAllProcessing(DataGenerator gen, PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        GENERATORS.add(new CCBPressurizationRecipes(output, registries));
        GENERATORS.add(new CCBCoolingRecipes(output, registries));
        GENERATORS.add(new CCBSuperCoolingRecipes(output, registries));
        GENERATORS.add(new CCBGasInjectionRecipes(output, registries));

        gen.addProvider(true, new DataProvider() {

            @Override
            public @NotNull CompletableFuture<?> run(@NotNull CachedOutput dc) {
                return CompletableFuture.allOf(GENERATORS.stream().map(gen -> gen.run(dc)).toArray(CompletableFuture[]::new));
            }

            @Override
            public @NotNull String getName() {
                return "Create Crafted Beginning's Processing Recipes";
            }
        });
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
    }
}
