package net.ty.createcraftedbeginning.datagen.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.datagen.recipe.generator.ChillingRecipeGen;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBChillingRecipes extends ChillingRecipeGen {
    private final GeneratedRecipe ICE_FROM_WATER_BUCKET = create("ice_from_water_bucket", builder -> builder.require(Items.WATER_BUCKET).output(Blocks.ICE).output(Items.BUCKET));
    private final GeneratedRecipe ICE_FROM_SNOW_BLOCK = create("ice_from_snow_block", b -> b.require(Blocks.SNOW_BLOCK).output(Blocks.ICE));
    private final GeneratedRecipe PACKED_ICE = create("packed_ice", b -> b.require(Blocks.ICE).output(Blocks.PACKED_ICE));
    private final GeneratedRecipe BLUE_ICE = create("blue_ice", b -> b.require(Blocks.PACKED_ICE).output(Blocks.BLUE_ICE));
    private final GeneratedRecipe OBSIDIAN_FROM_MAGMA_BLOCK = create("obsidian_from_magma_block", builder -> builder.require(Blocks.MAGMA_BLOCK).output(Blocks.OBSIDIAN));
    private final GeneratedRecipe OBSIDIAN_FROM_LAVA_BUCKET = create("obsidian_from_lava_bucket", builder -> builder.require(Items.LAVA_BUCKET).output(Blocks.OBSIDIAN).output(Items.BUCKET));

    public CCBChillingRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CCBAPI.MOD_ID);
    }
}
