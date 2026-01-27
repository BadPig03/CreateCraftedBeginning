package net.ty.createcraftedbeginning.data;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.recipe.generators.GasInjectionRecipeGen;
import net.ty.createcraftedbeginning.registry.CCBItems;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBGasInjectionRecipes extends GasInjectionRecipeGen {
    GeneratedRecipe WIND_CHARGE = create("wind_charge", b -> b.require(Items.BLAZE_POWDER).require(CCBGases.NATURAL_AIR.get(), 500).output(Items.WIND_CHARGE, 2));
    GeneratedRecipe WIND_CHARGE_PRESSURIZED = create("wind_charge_pressurized", b -> b.require(Items.BLAZE_POWDER).require(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 25).output(Items.WIND_CHARGE, 2));
    GeneratedRecipe BREEZE_ROD = create("breeze_rod", b -> b.require(Tags.Items.RODS_BLAZE).require(CCBGases.NATURAL_AIR.get(), 500).output(Items.BREEZE_ROD));
    GeneratedRecipe BREEZE_ROD_PRESSURIZED = create("breeze_rod_pressurized", b -> b.require(Tags.Items.RODS_BLAZE).require(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 25).output(Items.BREEZE_ROD));
    GeneratedRecipe MUSHROOM_STEM_FROM_BROWN = create("mushroom_stem_from_brown", b -> b.require(Blocks.BROWN_MUSHROOM_BLOCK).require(CCBGases.NATURAL_AIR.get(), 500).output(Blocks.MUSHROOM_STEM));
    GeneratedRecipe MUSHROOM_STEM_FROM_BROWN_PRESSURIZED = create("mushroom_stem_from_brown_pressurized", b -> b.require(Blocks.BROWN_MUSHROOM_BLOCK).require(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 25).output(Blocks.MUSHROOM_STEM));
    GeneratedRecipe MUSHROOM_STEM_FROM_RED = create("mushroom_stem_from_red", b -> b.require(Blocks.RED_MUSHROOM_BLOCK).require(CCBGases.NATURAL_AIR.get(), 500).output(Blocks.MUSHROOM_STEM));
    GeneratedRecipe MUSHROOM_STEM_FROM_RED_PRESSURIZED = create("mushroom_stem_from_red_pressurized", b -> b.require(Blocks.RED_MUSHROOM_BLOCK).require(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 25).output(Blocks.MUSHROOM_STEM));

    GeneratedRecipe GRASS_BLOCK = create("grass_block", b -> b.require(Blocks.DIRT).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.GRASS_BLOCK));
    GeneratedRecipe GRASS_BLOCK_PRESSURIZED = create("grass_block_pressurized", b -> b.require(Blocks.DIRT).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.GRASS_BLOCK));
    GeneratedRecipe PODZOL = create("podzol", b -> b.require(Blocks.GRASS_BLOCK).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.PODZOL));
    GeneratedRecipe PODZOL_PRESSURIZED = create("podzol_pressurized", b -> b.require(Blocks.GRASS_BLOCK).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.PODZOL));
    GeneratedRecipe CALCITE = create("calcite", b -> b.require(Blocks.DRIPSTONE_BLOCK).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.CALCITE));
    GeneratedRecipe CALCITE_PRESSURIZED = create("calcite_pressurized", b -> b.require(Blocks.DRIPSTONE_BLOCK).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.CALCITE));
    GeneratedRecipe OAK_LEAVES = create("oak_leaves", b -> b.require(Blocks.OAK_SAPLING).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.OAK_LEAVES));
    GeneratedRecipe OAK_LEAVES_PRESSURIZED = create("oak_leaves_pressurized", b -> b.require(Blocks.OAK_SAPLING).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.OAK_LEAVES));
    GeneratedRecipe SPRUCE_LEAVES = create("spruce_leaves", b -> b.require(Blocks.SPRUCE_SAPLING).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.SPRUCE_LEAVES));
    GeneratedRecipe SPRUCE_LEAVES_PRESSURIZED = create("spruce_leaves_pressurized", b -> b.require(Blocks.SPRUCE_SAPLING).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.SPRUCE_LEAVES));
    GeneratedRecipe BIRCH_LEAVES = create("birch_leaves", b -> b.require(Blocks.BIRCH_SAPLING).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.BIRCH_LEAVES));
    GeneratedRecipe BIRCH_LEAVES_PRESSURIZED = create("birch_leaves_pressurized", b -> b.require(Blocks.BIRCH_SAPLING).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.BIRCH_LEAVES));
    GeneratedRecipe JUNGLE_LEAVES = create("jungle_leaves", b -> b.require(Blocks.JUNGLE_SAPLING).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.JUNGLE_LEAVES));
    GeneratedRecipe JUNGLE_LEAVES_PRESSURIZED = create("jungle_leaves_pressurized", b -> b.require(Blocks.JUNGLE_SAPLING).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.JUNGLE_LEAVES));
    GeneratedRecipe ACACIA_LEAVES = create("acacia_leaves", b -> b.require(Blocks.ACACIA_SAPLING).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.ACACIA_LEAVES));
    GeneratedRecipe ACACIA_LEAVES_PRESSURIZED = create("acacia_leaves_pressurized", b -> b.require(Blocks.ACACIA_SAPLING).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.ACACIA_LEAVES));
    GeneratedRecipe DARK_OAK_LEAVES = create("dark_oak_leaves", b -> b.require(Blocks.DARK_OAK_SAPLING).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.DARK_OAK_LEAVES));
    GeneratedRecipe DARK_OAK_LEAVES_PRESSURIZED = create("dark_oak_leaves_pressurized", b -> b.require(Blocks.DARK_OAK_SAPLING).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.DARK_OAK_LEAVES));
    GeneratedRecipe CHERRY_LEAVES = create("cherry_leaves", b -> b.require(Blocks.CHERRY_LEAVES).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.CHERRY_LEAVES));
    GeneratedRecipe CHERRY_LEAVES_PRESSURIZED = create("cherry_leaves_pressurized", b -> b.require(Blocks.CHERRY_LEAVES).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.CHERRY_LEAVES));
    GeneratedRecipe MANGROVE_LEAVES = create("mangrove_leaves", b -> b.require(Blocks.MANGROVE_PROPAGULE).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.MANGROVE_LEAVES));
    GeneratedRecipe MANGROVE_LEAVES_PRESSURIZED = create("mangrove_leaves_pressurized", b -> b.require(Blocks.MANGROVE_PROPAGULE).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.MANGROVE_LEAVES));
    GeneratedRecipe AZALEA_LEAVES = create("azalea_leaves", b -> b.require(Blocks.AZALEA).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.AZALEA_LEAVES));
    GeneratedRecipe AZALEA_LEAVES_PRESSURIZED = create("azalea_leaves_pressurized", b -> b.require(Blocks.AZALEA).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.AZALEA_LEAVES));
    GeneratedRecipe FLOWERING_AZALEA_LEAVES = create("flowering_azalea_leaves", b -> b.require(Blocks.FLOWERING_AZALEA).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.FLOWERING_AZALEA_LEAVES));
    GeneratedRecipe FLOWERING_AZALEA_LEAVES_PRESSURIZED = create("flowering_azalea_leaves_pressurized", b -> b.require(Blocks.FLOWERING_AZALEA).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.FLOWERING_AZALEA_LEAVES));
    GeneratedRecipe BROWN_MUSHROOM_BLOCK = create("brown_mushroom_block", b -> b.require(Blocks.BROWN_MUSHROOM).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.BROWN_MUSHROOM_BLOCK));
    GeneratedRecipe BROWN_MUSHROOM_BLOCK_PRESSURIZED = create("brown_mushroom_block_pressurized", b -> b.require(Blocks.BROWN_MUSHROOM).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.BROWN_MUSHROOM_BLOCK));
    GeneratedRecipe RED_MUSHROOM_BLOCK = create("red_mushroom_block", b -> b.require(Blocks.RED_MUSHROOM).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.RED_MUSHROOM_BLOCK));
    GeneratedRecipe RED_MUSHROOM_BLOCK_PRESSURIZED = create("red_mushroom_block_pressurized", b -> b.require(Blocks.RED_MUSHROOM).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.RED_MUSHROOM_BLOCK));
    GeneratedRecipe NETHER_WART_BLOCK = create("nether_wart_block", b -> b.require(Blocks.CRIMSON_FUNGUS).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.NETHER_WART_BLOCK));
    GeneratedRecipe NETHER_WART_BLOCK_PRESSURIZED = create("nether_wart_block_pressurized", b -> b.require(Blocks.CRIMSON_FUNGUS).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.NETHER_WART_BLOCK));
    GeneratedRecipe WARPED_WART_BLOCK = create("warped_wart_block", b -> b.require(Blocks.WARPED_FUNGUS).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.WARPED_WART_BLOCK));
    GeneratedRecipe WARPED_WART_BLOCK_PRESSURIZED = create("warped_wart_block_pressurized", b -> b.require(Blocks.WARPED_FUNGUS).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.WARPED_WART_BLOCK));
    GeneratedRecipe AZALEA = create("azalea", b -> b.require(Blocks.MOSS_BLOCK).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.AZALEA));
    GeneratedRecipe AZALEA_PRESSURIZED = create("azalea_pressurized", b -> b.require(Blocks.MOSS_BLOCK).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.AZALEA));
    GeneratedRecipe TALL_GRASS = create("tall_grass", b -> b.require(Blocks.SHORT_GRASS).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.TALL_GRASS));
    GeneratedRecipe TALL_GRASS_PRESSURIZED = create("tall_grass_pressurized", b -> b.require(Blocks.SHORT_GRASS).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.TALL_GRASS));
    GeneratedRecipe LARGE_FERN = create("large_fern", b -> b.require(Blocks.FERN).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.LARGE_FERN));
    GeneratedRecipe LARGE_FERN_PRESSURIZED = create("large_fern_pressurized", b -> b.require(Blocks.FERN).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.LARGE_FERN));
    GeneratedRecipe CRIMSON_ROOTS = create("crimson_roots", b -> b.require(Blocks.NETHER_WART).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.CRIMSON_ROOTS));
    GeneratedRecipe CRIMSON_ROOTS_PRESSURIZED = create("crimson_roots_pressurized", b -> b.require(Blocks.NETHER_WART).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.CRIMSON_ROOTS));
    GeneratedRecipe WEEPING_VINES = create("weeping_vines", b -> b.require(Blocks.CRIMSON_ROOTS).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.WEEPING_VINES));
    GeneratedRecipe WEEPING_VINES_PRESSURIZED = create("weeping_vines_pressurized", b -> b.require(Blocks.CRIMSON_ROOTS).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.WEEPING_VINES));
    GeneratedRecipe WARPED_ROOTS = create("warped_roots", b -> b.require(Blocks.NETHER_SPROUTS).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.WARPED_ROOTS));
    GeneratedRecipe WARPED_ROOTS_PRESSURIZED = create("warped_roots_pressurized", b -> b.require(Blocks.NETHER_SPROUTS).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.WARPED_ROOTS));
    GeneratedRecipe TWISTING_VINES = create("twisting_vines", b -> b.require(Blocks.WARPED_ROOTS).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.TWISTING_VINES));
    GeneratedRecipe TWISTING_VINES_PRESSURIZED = create("twisting_vines_pressurized", b -> b.require(Blocks.WARPED_ROOTS).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.TWISTING_VINES));
    GeneratedRecipe BIG_DRIPLEAF = create("big_dripleaf", b -> b.require(Blocks.SMALL_DRIPLEAF).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.BIG_DRIPLEAF));
    GeneratedRecipe BIG_DRIPLEAF_PRESSURIZED = create("big_dripleaf_pressurized", b -> b.require(Blocks.SMALL_DRIPLEAF).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.BIG_DRIPLEAF));
    GeneratedRecipe TUBE_CORAL = create("tube_coral", b -> b.require(Blocks.TUBE_CORAL_FAN).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.TUBE_CORAL));
    GeneratedRecipe TUBE_CORAL_PRESSURIZED = create("tube_coral_pressurized", b -> b.require(Blocks.TUBE_CORAL_FAN).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.TUBE_CORAL));
    GeneratedRecipe TUBE_CORAL_BLOCK = create("tube_coral_block", b -> b.require(Blocks.TUBE_CORAL).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.TUBE_CORAL_BLOCK));
    GeneratedRecipe TUBE_CORAL_BLOCK_PRESSURIZED = create("tube_coral_block_pressurized", b -> b.require(Blocks.TUBE_CORAL).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.TUBE_CORAL_BLOCK));
    GeneratedRecipe BRAIN_CORAL = create("brain_coral", b -> b.require(Blocks.BRAIN_CORAL_FAN).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.BRAIN_CORAL));
    GeneratedRecipe BRAIN_CORAL_PRESSURIZED = create("brain_coral_pressurized", b -> b.require(Blocks.BRAIN_CORAL_FAN).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.BRAIN_CORAL));
    GeneratedRecipe BRAIN_CORAL_BLOCK = create("brain_coral_block", b -> b.require(Blocks.BRAIN_CORAL).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.BRAIN_CORAL_BLOCK));
    GeneratedRecipe BRAIN_CORAL_BLOCK_PRESSURIZED = create("brain_coral_block_pressurized", b -> b.require(Blocks.BRAIN_CORAL).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.BRAIN_CORAL_BLOCK));
    GeneratedRecipe BUBBLE_CORAL = create("bubble_coral", b -> b.require(Blocks.BUBBLE_CORAL_FAN).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.BUBBLE_CORAL));
    GeneratedRecipe BUBBLE_CORAL_PRESSURIZED = create("bubble_coral_pressurized", b -> b.require(Blocks.BUBBLE_CORAL_FAN).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.BUBBLE_CORAL));
    GeneratedRecipe BUBBLE_CORAL_BLOCK = create("bubble_coral_block", b -> b.require(Blocks.BUBBLE_CORAL).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.BUBBLE_CORAL_BLOCK));
    GeneratedRecipe BUBBLE_CORAL_BLOCK_PRESSURIZED = create("bubble_coral_block_pressurized", b -> b.require(Blocks.BUBBLE_CORAL).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.BUBBLE_CORAL_BLOCK));
    GeneratedRecipe FIRE_CORAL = create("fire_coral", b -> b.require(Blocks.FIRE_CORAL_FAN).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.FIRE_CORAL));
    GeneratedRecipe FIRE_CORAL_PRESSURIZED = create("fire_coral_pressurized", b -> b.require(Blocks.FIRE_CORAL_FAN).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.FIRE_CORAL));
    GeneratedRecipe FIRE_CORAL_BLOCK = create("fire_coral_block", b -> b.require(Blocks.FIRE_CORAL).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.FIRE_CORAL_BLOCK));
    GeneratedRecipe FIRE_CORAL_BLOCK_PRESSURIZED = create("fire_coral_block_pressurized", b -> b.require(Blocks.FIRE_CORAL).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.FIRE_CORAL_BLOCK));
    GeneratedRecipe HORN_CORAL = create("horn_coral", b -> b.require(Blocks.HORN_CORAL_FAN).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.HORN_CORAL));
    GeneratedRecipe HORN_CORAL_PRESSURIZED = create("horn_coral_pressurized", b -> b.require(Blocks.HORN_CORAL_FAN).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.HORN_CORAL));
    GeneratedRecipe HORN_CORAL_BLOCK = create("horn_coral_block", b -> b.require(Blocks.HORN_CORAL).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.HORN_CORAL_BLOCK));
    GeneratedRecipe HORN_CORAL_BLOCK_PRESSURIZED = create("horn_coral_block_pressurized", b -> b.require(Blocks.HORN_CORAL).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.HORN_CORAL_BLOCK));

    GeneratedRecipe DEEPSLATE = create("deepslate", b -> b.require(Blocks.STONE).require(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 100).output(Blocks.DEEPSLATE));
    GeneratedRecipe DEEPSLATE_PRESSURIZED = create("deepslate_pressurized", b -> b.require(Blocks.STONE).require(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), 5).output(Blocks.DEEPSLATE));
    GeneratedRecipe TUFF = create("tuff", b -> b.require(Blocks.GRAVEL).require(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 100).output(Blocks.TUFF));
    GeneratedRecipe TUFF_PRESSURIZED = create("tuff_pressurized", b -> b.require(Blocks.GRAVEL).require(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), 5).output(Blocks.TUFF));
    GeneratedRecipe DEAD_BUSH = create("dead_bush", b -> b.require(Blocks.SHORT_GRASS).require(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 100).output(Blocks.DEAD_BUSH));
    GeneratedRecipe DEAD_BUSH_PRESSURIZED = create("dead_bush_pressurized", b -> b.require(Blocks.SHORT_GRASS).require(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), 5).output(Blocks.DEAD_BUSH));

    GeneratedRecipe CRIMSON_NYLIUM = create("crimson_nylium", b -> b.require(Blocks.WARPED_NYLIUM).require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 100).output(Blocks.CRIMSON_NYLIUM));
    GeneratedRecipe CRIMSON_NYLIUM_PRESSURIZED = create("crimson_nylium_pressurized", b -> b.require(Blocks.WARPED_NYLIUM).require(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 5).output(Blocks.CRIMSON_NYLIUM));
    GeneratedRecipe WARPED_NYLIUM = create("warped_nylium", b -> b.require(Blocks.CRIMSON_NYLIUM).require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 100).output(Blocks.WARPED_NYLIUM));
    GeneratedRecipe WARPED_NYLIUM_PRESSURIZED = create("warped_nylium_pressurized", b -> b.require(Blocks.CRIMSON_NYLIUM).require(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 5).output(Blocks.WARPED_NYLIUM));
    GeneratedRecipe BROWN_MUSHROOM_BLOCK_FROM_RED = create("brown_mushroom_block_from_red", b -> b.require(Blocks.BROWN_MUSHROOM_BLOCK).require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 100).output(Blocks.RED_MUSHROOM_BLOCK));
    GeneratedRecipe BROWN_MUSHROOM_BLOCK_FROM_RED_PRESSURIZED = create("brown_mushroom_block_from_red_pressurized", b -> b.require(Blocks.BROWN_MUSHROOM_BLOCK).require(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 5).output(Blocks.RED_MUSHROOM_BLOCK));
    GeneratedRecipe RED_MUSHROOM_BLOCK_FROM_BROWN = create("red_mushroom_block_from_brown", b -> b.require(Blocks.RED_MUSHROOM_BLOCK).require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 100).output(Blocks.BROWN_MUSHROOM_BLOCK));
    GeneratedRecipe RED_MUSHROOM_BLOCK_FROM_BROWN_PRESSURIZED = create("red_mushroom_block_from_brown_pressurized", b -> b.require(Blocks.RED_MUSHROOM_BLOCK).require(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 5).output(Blocks.BROWN_MUSHROOM_BLOCK));
    GeneratedRecipe PEARLESCENT_FROGLIGHT = create("pearlescent_froglight", b -> b.require(Blocks.OCHRE_FROGLIGHT).require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 100).output(Blocks.PEARLESCENT_FROGLIGHT));
    GeneratedRecipe PEARLESCENT_FROGLIGHT_PRESSURIZED = create("pearlescent_froglight_pressurized", b -> b.require(Blocks.OCHRE_FROGLIGHT).require(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 5).output(Blocks.PEARLESCENT_FROGLIGHT));
    GeneratedRecipe VERDANT_FROGLIGHT = create("verdant_froglight", b -> b.require(Blocks.PEARLESCENT_FROGLIGHT).require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 100).output(Blocks.VERDANT_FROGLIGHT));
    GeneratedRecipe VERDANT_FROGLIGHT_PRESSURIZED = create("verdant_froglight_pressurized", b -> b.require(Blocks.PEARLESCENT_FROGLIGHT).require(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 5).output(Blocks.VERDANT_FROGLIGHT));
    GeneratedRecipe OCHRE_FROGLIGHT = create("ochre_froglight", b -> b.require(Blocks.VERDANT_FROGLIGHT).require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 100).output(Blocks.OCHRE_FROGLIGHT));
    GeneratedRecipe OCHRE_FROGLIGHT_PRESSURIZED = create("ochre_froglight_pressurized", b -> b.require(Blocks.VERDANT_FROGLIGHT).require(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 5).output(Blocks.OCHRE_FROGLIGHT));

    GeneratedRecipe MYCELIUM = create("mycelium", b -> b.require(Blocks.GRASS_BLOCK).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.MYCELIUM));
    GeneratedRecipe ROOTED_DIRT = create("rooted_dirt", b -> b.require(Blocks.DIRT).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.ROOTED_DIRT));
    GeneratedRecipe MOSS_BLOCK = create("moss_block", b -> b.require(Blocks.STONE).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.MOSS_BLOCK));
    GeneratedRecipe CRIMSON_NYLIUM_FROM_NETHERRACK = create("crimson_nylium_from_netherrack", b -> b.require(Blocks.NETHERRACK).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.CRIMSON_NYLIUM));
    GeneratedRecipe SHROOMLIGHT_FROM_CRIMSON = create("shroomlight_from_crimson", b -> b.require(Blocks.CRIMSON_FUNGUS).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.SHROOMLIGHT));
    GeneratedRecipe SHROOMLIGHT_FROM_WARPED = create("shroomlight_from_warped", b -> b.require(Blocks.WARPED_FUNGUS).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.SHROOMLIGHT));
    GeneratedRecipe SPORE_BLOSSOM = create("spore_blossom", b -> b.require(Blocks.FLOWERING_AZALEA).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.SPORE_BLOSSOM));
    GeneratedRecipe CHORUS_FLOWER = create("chorus_flower", b -> b.require(Items.CHORUS_FRUIT).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.CHORUS_FLOWER));

    GeneratedRecipe MUD = create("mud", b -> b.require(Blocks.DIRT).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.MUD));
    GeneratedRecipe CLAY = create("clay", b -> b.require(Blocks.MUD).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.CLAY));
    GeneratedRecipe TUBE_CORAL_FAN_REVIVED = create("tube_coral_fan_revived", b -> b.require(Blocks.DEAD_TUBE_CORAL_FAN).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.TUBE_CORAL_FAN));
    GeneratedRecipe TUBE_CORAL_REVIVED = create("tube_coral_revived", b -> b.require(Blocks.DEAD_TUBE_CORAL).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.TUBE_CORAL));
    GeneratedRecipe TUBE_CORAL_BLOCK_REVIVED = create("tube_coral_block_revived", b -> b.require(Blocks.DEAD_TUBE_CORAL_BLOCK).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.TUBE_CORAL_BLOCK));
    GeneratedRecipe BRAIN_CORAL_FAN_REVIVED = create("brain_coral_fan_revived", b -> b.require(Blocks.DEAD_BRAIN_CORAL_FAN).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.BRAIN_CORAL_FAN));
    GeneratedRecipe BRAIN_CORAL_REVIVED = create("brain_coral_revived", b -> b.require(Blocks.DEAD_BRAIN_CORAL).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.BRAIN_CORAL));
    GeneratedRecipe BRAIN_CORAL_BLOCK_REVIVED = create("brain_coral_block_revived", b -> b.require(Blocks.DEAD_BRAIN_CORAL_BLOCK).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.BRAIN_CORAL_BLOCK));
    GeneratedRecipe BUBBLE_CORAL_FAN_REVIVED = create("bubble_coral_fan_revived", b -> b.require(Blocks.DEAD_BUBBLE_CORAL_FAN).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.BUBBLE_CORAL_FAN));
    GeneratedRecipe BUBBLE_CORAL_REVIVED = create("bubble_coral_revived", b -> b.require(Blocks.DEAD_BUBBLE_CORAL).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.BUBBLE_CORAL));
    GeneratedRecipe BUBBLE_CORAL_BLOCK_REVIVED = create("fire_coral_block_revived", b -> b.require(Blocks.DEAD_BUBBLE_CORAL_BLOCK).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.BUBBLE_CORAL_BLOCK));
    GeneratedRecipe FIRE_CORAL_FAN_REVIVED = create("fire_coral_fan_revived", b -> b.require(Blocks.DEAD_FIRE_CORAL_FAN).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.FIRE_CORAL_FAN));
    GeneratedRecipe FIRE_CORAL_REVIVED = create("fire_coral_revived", b -> b.require(Blocks.DEAD_FIRE_CORAL).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.FIRE_CORAL));
    GeneratedRecipe FIRE_CORAL_BLOCK_REVIVED = create("bubble_coral_block_revived", b -> b.require(Blocks.DEAD_FIRE_CORAL_BLOCK).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.FIRE_CORAL_BLOCK));
    GeneratedRecipe HORN_CORAL_FAN_REVIVED = create("horn_coral_fan_revived", b -> b.require(Blocks.DEAD_HORN_CORAL_FAN).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.HORN_CORAL_FAN));
    GeneratedRecipe HORN_CORAL_REVIVED = create("horn_coral_revived", b -> b.require(Blocks.DEAD_HORN_CORAL).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.HORN_CORAL));
    GeneratedRecipe HORN_CORAL_BLOCK_REVIVED = create("horn_coral_block_revived", b -> b.require(Blocks.DEAD_HORN_CORAL_BLOCK).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.HORN_CORAL_BLOCK));
    GeneratedRecipe WET_SPONGE = create("wet_sponge", b -> b.require(Blocks.SPONGE).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.WET_SPONGE));

    GeneratedRecipe SUNNY_FLARE = create("sunny_flare", b -> b.require(CCBItems.UNFILLED_WEATHER_FLARE).require(CCBGases.ULTRAWARM_AIR.get(), 500).output(CCBItems.SUNNY_FLARE));
    GeneratedRecipe SUNNY_FLARE_PRESSURIZED = create("sunny_flare_pressurized", b -> b.require(CCBItems.UNFILLED_WEATHER_FLARE).require(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 25).output(CCBItems.SUNNY_FLARE));
    GeneratedRecipe RAIN_FLARE = create("rain_flare", b -> b.require(CCBItems.UNFILLED_WEATHER_FLARE).require(CCBGases.MOIST_AIR.get(), 500).output(CCBItems.RAIN_FLARE));
    GeneratedRecipe THUNDERSTORM_FLARE = create("thunderstorm_flare", b -> b.require(CCBItems.UNFILLED_WEATHER_FLARE).require(CCBGases.ETHEREAL_AIR.get(), 500).output(CCBItems.THUNDERSTORM_FLARE));
    GeneratedRecipe THUNDERSTORM_FLARE_PRESSURIZED = create("thunderstorm_flare_pressurized", b -> b.require(CCBItems.UNFILLED_WEATHER_FLARE).require(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), 25).output(CCBItems.THUNDERSTORM_FLARE));

    public CCBGasInjectionRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
