package net.ty.createcraftedbeginning.datagen.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.datagen.recipe.generator.ChillingRecipeGen;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBChillingRecipes extends ChillingRecipeGen {
    private final GeneratedRecipe SNOWBALL_FROM_WATER_BOTTLE = create("snowball_from_water_bottle", builder -> builder.require(DataComponentIngredient.of(false, DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER), Items.POTION)).output(Items.SNOWBALL).output(Items.GLASS_BOTTLE));
    private final GeneratedRecipe ICE_FROM_POWDER_SNOW_BUCKET = create("ice_from_powder_snow_bucket", builder -> builder.require(Items.POWDER_SNOW_BUCKET).output(Blocks.ICE).output(Items.BUCKET));
    private final GeneratedRecipe ICE_FROM_WATER_BUCKET = create("ice_from_water_bucket", builder -> builder.require(Items.WATER_BUCKET).output(Blocks.ICE).output(Items.BUCKET));
    private final GeneratedRecipe ICE_FROM_SNOW_BLOCK = create("ice_from_snow_block", builder -> builder.require(Blocks.SNOW_BLOCK).output(Blocks.ICE));
    private final GeneratedRecipe PACKED_ICE = create("packed_ice", builder -> builder.require(Blocks.ICE).output(Blocks.PACKED_ICE));
    private final GeneratedRecipe BLUE_ICE = create("blue_ice", builder -> builder.require(Blocks.PACKED_ICE).output(Blocks.BLUE_ICE));
    private final GeneratedRecipe OBSIDIAN_FROM_MAGMA_BLOCK = create("obsidian_from_magma_block", builder -> builder.require(Blocks.MAGMA_BLOCK).output(Blocks.OBSIDIAN));
    private final GeneratedRecipe OBSIDIAN_FROM_LAVA_BUCKET = create("obsidian_from_lava_bucket", builder -> builder.require(Items.LAVA_BUCKET).output(Blocks.OBSIDIAN).output(Items.BUCKET));
    private final GeneratedRecipe NETHERRACK_FROM_BRIMSTONE_BUCKET = create("netherrack_from_brimstone_bucket", builder -> builder.require(CCBFluids.BRIMSTONE.getBucket().orElseThrow()).output(Blocks.NETHERRACK).output(Items.BUCKET));
    private final GeneratedRecipe SLIME_BALL_FROM_MAGMA_CREAM = create("slime_ball_from_magma_cream", builder -> builder.require(Items.MAGMA_CREAM).output(Items.SLIME_BALL));
    private final GeneratedRecipe POWDERED_AMETHYST_FROM_SUSPENSION_BUCKET = create("powdered_amethyst_from_suspension_bucket", builder -> builder.require(CCBFluids.AMETHYST_SUSPENSION.getBucket().orElseThrow()).output(CCBItems.POWDERED_AMETHYST, 4).output(Items.BUCKET));

    public CCBChillingRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CCBAPI.MOD_ID);
    }
}
