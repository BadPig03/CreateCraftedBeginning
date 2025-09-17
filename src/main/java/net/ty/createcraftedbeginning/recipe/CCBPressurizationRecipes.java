package net.ty.createcraftedbeginning.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBFluids;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBPressurizationRecipes extends PressurizationRecipeGen {
    GeneratedRecipe LOW_TO_MEDIUM = create("low_to_medium", b -> b.require(CCBFluids.LOW_PRESSURE_COMPRESSED_AIR.get(), 20).output(CCBFluids.MEDIUM_PRESSURE_COMPRESSED_AIR.get(), 1));
    GeneratedRecipe MEDIUM_TO_HIGH = create("medium_to_high", b -> b.require(CCBFluids.MEDIUM_PRESSURE_COMPRESSED_AIR.get(), 10).output(CCBFluids.HIGH_PRESSURE_COMPRESSED_AIR.get(), 1));

    public CCBPressurizationRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
