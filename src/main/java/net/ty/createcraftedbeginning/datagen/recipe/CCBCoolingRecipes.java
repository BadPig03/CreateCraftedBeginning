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
    private final GeneratedRecipe ICE_CREAMS = create("ice_creams", b -> b.require(CCBItemTags.ICE_CREAMS.tag).duration(450));
    private final GeneratedRecipe ICE = create("ice", b -> b.require(Blocks.ICE).duration(100));
    private final GeneratedRecipe PACKED_ICE = create("packed_ice", b -> b.require(Blocks.PACKED_ICE).duration(900));
    private final GeneratedRecipe BLUE_ICE = create("blue_ice", b -> b.require(Blocks.BLUE_ICE).duration(8100));
    private final GeneratedRecipe SLUSH = create("slush", b -> b.require(CCBFluids.SLUSH.get(), 1000).duration(1800));
    private final GeneratedRecipe POWDER_SNOW_BUCKET = create("powder_snow_bucket", b -> b.require(Items.POWDER_SNOW_BUCKET).duration(1800));
    private final GeneratedRecipe CREATIVE_ICE_CREAM = create("creative_ice_cream", b -> b.require(CCBItems.CREATIVE_ICE_CREAM).duration(32767));

    public CCBCoolingRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CCBAPI.MOD_ID);
    }
}