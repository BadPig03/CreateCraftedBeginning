package net.ty.createcraftedbeginning.data;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.recipe.generators.FreezingRecipeGen;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBFreezingRecipes extends FreezingRecipeGen {
    GeneratedRecipe ICE_FROM_WATER_BUCKET = create("ice_from_water_bucket", b -> b.require(Items.WATER_BUCKET).output(Blocks.ICE));
    GeneratedRecipe ICE_FROM_SNOW_BLOCK = create("ice_from_snow_block", b -> b.require(Blocks.SNOW_BLOCK).output(Blocks.ICE));
    GeneratedRecipe PACKED_ICE = create("packed_ice", b -> b.require(Blocks.ICE).output(Blocks.PACKED_ICE));
    GeneratedRecipe BLUE_ICE = create("blue_ice", b -> b.require(Blocks.PACKED_ICE).output(Blocks.BLUE_ICE));
    GeneratedRecipe OBSIDIAN_FROM_MAGMA_BLOCK = create("obsidian_from_magma_block", b -> b.require(Blocks.MAGMA_BLOCK).output(Blocks.OBSIDIAN));
    GeneratedRecipe OBSIDIAN_FROM_LAVA_BUCKET = create("obsidian_from_lava_bucket", b -> b.require(Items.LAVA_BUCKET).output(Blocks.OBSIDIAN));

    public CCBFreezingRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
