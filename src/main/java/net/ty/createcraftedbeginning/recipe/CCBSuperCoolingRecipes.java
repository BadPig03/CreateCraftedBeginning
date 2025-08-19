package net.ty.createcraftedbeginning.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.CCBItems;

import java.util.concurrent.CompletableFuture;

public class CCBSuperCoolingRecipes extends SuperCoolingRecipeGen {
    GeneratedRecipe

    SLUSH = create("slush", b -> b.require(CCBFluids.SLUSH.get(), 5).output(CCBFluids.COOLING_TIME.get(), 12)),

    POWDER_SNOW_BUCKET = create("powder_snow_bucket", b -> b.require(Items.POWDER_SNOW_BUCKET).output(CCBFluids.COOLING_TIME.get(), 2400)),

    CREATIVE_ICE_CREAM = create("creative_ice_cream", b -> b.require(CCBItems.CREATIVE_ICE_CREAM).output(CCBFluids.COOLING_TIME.get(), 50000));

    public CCBSuperCoolingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
