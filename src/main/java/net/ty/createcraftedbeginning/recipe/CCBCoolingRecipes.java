package net.ty.createcraftedbeginning.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.CCBItems;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBCoolingRecipes extends CoolingRecipeGen {
    GeneratedRecipe SNOWBALL = create("snowball", b -> b.require(Items.SNOWBALL).output(CCBFluids.VIRTUAL_TIME.get(), 1));
    GeneratedRecipe SNOW = create("snow", b -> b.require(Blocks.SNOW).output(CCBFluids.VIRTUAL_TIME.get(), 2));
    GeneratedRecipe SNOW_BLOCK = create("snow_block", b -> b.require(Blocks.SNOW_BLOCK).output(CCBFluids.VIRTUAL_TIME.get(), 4));
    GeneratedRecipe ICE = create("ice", b -> b.require(Blocks.ICE).output(CCBFluids.VIRTUAL_TIME.get(), 36));
    GeneratedRecipe PACKED_ICE = create("packed_ice", b -> b.require(Blocks.PACKED_ICE).output(CCBFluids.VIRTUAL_TIME.get(), 324));
    GeneratedRecipe BLUE_ICE = create("blue_ice", b -> b.require(Blocks.BLUE_ICE).output(CCBFluids.VIRTUAL_TIME.get(), 2916));
    GeneratedRecipe SLUSH = create("slush", b -> b.require(CCBFluids.SLUSH.get(), 1000).output(CCBFluids.VIRTUAL_TIME.get(), 3200));
    GeneratedRecipe POWDER_SNOW_BUCKET = create("powder_snow_bucket", b -> b.require(Items.POWDER_SNOW_BUCKET).output(CCBFluids.VIRTUAL_TIME.get(), 3200));
    GeneratedRecipe CREATIVE_ICE_CREAM = create("creative_ice_cream", b -> b.require(CCBItems.CREATIVE_ICE_CREAM).output(CCBFluids.VIRTUAL_TIME.get(), 32767));

    public CCBCoolingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
