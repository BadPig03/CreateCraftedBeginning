package net.ty.createcraftedbeginning.datagen.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.datagen.recipe.generator.CoolingRecipeGen;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBCoolingRecipes extends CoolingRecipeGen {
    private final GeneratedRecipe ICE_CREAMS = create("ice_creams", builder -> builder.require(CCBItemTags.ICE_CREAMS.tag).duration(400));
    private final GeneratedRecipe SNOW = create("snow", builder -> builder.require(Blocks.SNOW).duration(10));
    private final GeneratedRecipe SNOW_BLOCK = create("snow_block", builder -> builder.require(Blocks.SNOW_BLOCK).duration(80));
    private final GeneratedRecipe ICE = create("ice", builder -> builder.require(Blocks.ICE).duration(200));
    private final GeneratedRecipe PACKED_ICE = create("packed_ice", builder -> builder.require(Blocks.PACKED_ICE).duration(1800));
    private final GeneratedRecipe BLUE_ICE = create("blue_ice", builder -> builder.require(Blocks.BLUE_ICE).duration(16200));
    private final GeneratedRecipe SLUSH = create("slush", builder -> builder.require(CCBFluids.SLUSH.get(), 1000).duration(160));
    private final GeneratedRecipe POWDER_SNOW_BUCKET = create("powder_snow_bucket", builder -> builder.require(Items.POWDER_SNOW_BUCKET).duration(160));
    private final GeneratedRecipe CREATIVE_ICE_CREAM = create("creative_ice_cream", builder -> builder.require(CCBItems.CREATIVE_ICE_CREAM).duration(32767));

    public CCBCoolingRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CCBAPI.MOD_ID);
    }
}
