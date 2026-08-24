package net.ty.createcraftedbeginning.datagen.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.Tags.Fluids;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.datagen.recipe.generator.GasInjectionRecipeGen;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBGasInjectionRecipes extends GasInjectionRecipeGen {
    private final GeneratedRecipe WIND_CHARGE = create("wind_charge", builder -> builder.require(Items.BLAZE_POWDER).require(CCBGases.NATURAL_AIR.get(), 500).output(Items.WIND_CHARGE, 2));
    private final GeneratedRecipe WIND_CHARGE_PRESSURIZED = create("wind_charge_pressurized", builder -> builder.require(Items.BLAZE_POWDER).require(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 25).output(Items.WIND_CHARGE, 2));
    private final GeneratedRecipe BREEZE_ROD = create("breeze_rod", builder -> builder.require(Tags.Items.RODS_BLAZE).require(CCBGases.NATURAL_AIR.get(), 500).output(Items.BREEZE_ROD));
    private final GeneratedRecipe BREEZE_ROD_PRESSURIZED = create("breeze_rod_pressurized", builder -> builder.require(Tags.Items.RODS_BLAZE).require(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 25).output(Items.BREEZE_ROD));
    private final GeneratedRecipe MUSHROOM_STEM_FROM_BROWN = create("mushroom_stem_from_brown", builder -> builder.require(Blocks.BROWN_MUSHROOM_BLOCK).require(CCBGases.NATURAL_AIR.get(), 500).output(Blocks.MUSHROOM_STEM));
    private final GeneratedRecipe MUSHROOM_STEM_FROM_BROWN_PRESSURIZED = create("mushroom_stem_from_brown_pressurized", builder -> builder.require(Blocks.BROWN_MUSHROOM_BLOCK).require(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 25).output(Blocks.MUSHROOM_STEM));
    private final GeneratedRecipe MUSHROOM_STEM_FROM_RED = create("mushroom_stem_from_red", builder -> builder.require(Blocks.RED_MUSHROOM_BLOCK).require(CCBGases.NATURAL_AIR.get(), 500).output(Blocks.MUSHROOM_STEM));
    private final GeneratedRecipe MUSHROOM_STEM_FROM_RED_PRESSURIZED = create("mushroom_stem_from_red_pressurized", builder -> builder.require(Blocks.RED_MUSHROOM_BLOCK).require(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 25).output(Blocks.MUSHROOM_STEM));

    private final GeneratedRecipe GRASS_BLOCK = create("grass_block", builder -> builder.require(Blocks.DIRT).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.GRASS_BLOCK));
    private final GeneratedRecipe GRASS_BLOCK_PRESSURIZED = create("grass_block_pressurized", builder -> builder.require(Blocks.DIRT).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.GRASS_BLOCK));
    private final GeneratedRecipe PODZOL = create("podzol", builder -> builder.require(Blocks.GRASS_BLOCK).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.PODZOL));
    private final GeneratedRecipe PODZOL_PRESSURIZED = create("podzol_pressurized", builder -> builder.require(Blocks.GRASS_BLOCK).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.PODZOL));
    private final GeneratedRecipe CALCITE = create("calcite", builder -> builder.require(Blocks.DRIPSTONE_BLOCK).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.CALCITE));
    private final GeneratedRecipe CALCITE_PRESSURIZED = create("calcite_pressurized", builder -> builder.require(Blocks.DRIPSTONE_BLOCK).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.CALCITE));
    private final GeneratedRecipe OAK_LEAVES = create("oak_leaves", builder -> builder.require(Blocks.OAK_SAPLING).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.OAK_LEAVES));
    private final GeneratedRecipe OAK_LEAVES_PRESSURIZED = create("oak_leaves_pressurized", builder -> builder.require(Blocks.OAK_SAPLING).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.OAK_LEAVES));
    private final GeneratedRecipe SPRUCE_LEAVES = create("spruce_leaves", builder -> builder.require(Blocks.SPRUCE_SAPLING).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.SPRUCE_LEAVES));
    private final GeneratedRecipe SPRUCE_LEAVES_PRESSURIZED = create("spruce_leaves_pressurized", builder -> builder.require(Blocks.SPRUCE_SAPLING).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.SPRUCE_LEAVES));
    private final GeneratedRecipe BIRCH_LEAVES = create("birch_leaves", builder -> builder.require(Blocks.BIRCH_SAPLING).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.BIRCH_LEAVES));
    private final GeneratedRecipe BIRCH_LEAVES_PRESSURIZED = create("birch_leaves_pressurized", builder -> builder.require(Blocks.BIRCH_SAPLING).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.BIRCH_LEAVES));
    private final GeneratedRecipe JUNGLE_LEAVES = create("jungle_leaves", builder -> builder.require(Blocks.JUNGLE_SAPLING).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.JUNGLE_LEAVES));
    private final GeneratedRecipe JUNGLE_LEAVES_PRESSURIZED = create("jungle_leaves_pressurized", builder -> builder.require(Blocks.JUNGLE_SAPLING).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.JUNGLE_LEAVES));
    private final GeneratedRecipe ACACIA_LEAVES = create("acacia_leaves", builder -> builder.require(Blocks.ACACIA_SAPLING).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.ACACIA_LEAVES));
    private final GeneratedRecipe ACACIA_LEAVES_PRESSURIZED = create("acacia_leaves_pressurized", builder -> builder.require(Blocks.ACACIA_SAPLING).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.ACACIA_LEAVES));
    private final GeneratedRecipe DARK_OAK_LEAVES = create("dark_oak_leaves", builder -> builder.require(Blocks.DARK_OAK_SAPLING).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.DARK_OAK_LEAVES));
    private final GeneratedRecipe DARK_OAK_LEAVES_PRESSURIZED = create("dark_oak_leaves_pressurized", builder -> builder.require(Blocks.DARK_OAK_SAPLING).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.DARK_OAK_LEAVES));
    private final GeneratedRecipe CHERRY_LEAVES = create("cherry_leaves", builder -> builder.require(Blocks.CHERRY_LEAVES).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.CHERRY_LEAVES));
    private final GeneratedRecipe CHERRY_LEAVES_PRESSURIZED = create("cherry_leaves_pressurized", builder -> builder.require(Blocks.CHERRY_LEAVES).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.CHERRY_LEAVES));
    private final GeneratedRecipe MANGROVE_LEAVES = create("mangrove_leaves", builder -> builder.require(Blocks.MANGROVE_PROPAGULE).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.MANGROVE_LEAVES));
    private final GeneratedRecipe MANGROVE_LEAVES_PRESSURIZED = create("mangrove_leaves_pressurized", builder -> builder.require(Blocks.MANGROVE_PROPAGULE).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.MANGROVE_LEAVES));
    private final GeneratedRecipe AZALEA_LEAVES = create("azalea_leaves", builder -> builder.require(Blocks.AZALEA).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.AZALEA_LEAVES));
    private final GeneratedRecipe AZALEA_LEAVES_PRESSURIZED = create("azalea_leaves_pressurized", builder -> builder.require(Blocks.AZALEA).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.AZALEA_LEAVES));
    private final GeneratedRecipe FLOWERING_AZALEA_LEAVES = create("flowering_azalea_leaves", builder -> builder.require(Blocks.FLOWERING_AZALEA).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.FLOWERING_AZALEA_LEAVES));
    private final GeneratedRecipe FLOWERING_AZALEA_LEAVES_PRESSURIZED = create("flowering_azalea_leaves_pressurized", builder -> builder.require(Blocks.FLOWERING_AZALEA).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.FLOWERING_AZALEA_LEAVES));
    private final GeneratedRecipe BROWN_MUSHROOM_BLOCK = create("brown_mushroom_block", builder -> builder.require(Blocks.BROWN_MUSHROOM).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.BROWN_MUSHROOM_BLOCK));
    private final GeneratedRecipe BROWN_MUSHROOM_BLOCK_PRESSURIZED = create("brown_mushroom_block_pressurized", builder -> builder.require(Blocks.BROWN_MUSHROOM).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.BROWN_MUSHROOM_BLOCK));
    private final GeneratedRecipe RED_MUSHROOM_BLOCK = create("red_mushroom_block", builder -> builder.require(Blocks.RED_MUSHROOM).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.RED_MUSHROOM_BLOCK));
    private final GeneratedRecipe RED_MUSHROOM_BLOCK_PRESSURIZED = create("red_mushroom_block_pressurized", builder -> builder.require(Blocks.RED_MUSHROOM).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.RED_MUSHROOM_BLOCK));
    private final GeneratedRecipe NETHER_WART_BLOCK = create("nether_wart_block", builder -> builder.require(Blocks.CRIMSON_FUNGUS).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.NETHER_WART_BLOCK));
    private final GeneratedRecipe NETHER_WART_BLOCK_PRESSURIZED = create("nether_wart_block_pressurized", builder -> builder.require(Blocks.CRIMSON_FUNGUS).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.NETHER_WART_BLOCK));
    private final GeneratedRecipe WARPED_WART_BLOCK = create("warped_wart_block", builder -> builder.require(Blocks.WARPED_FUNGUS).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 20).output(Blocks.WARPED_WART_BLOCK));
    private final GeneratedRecipe WARPED_WART_BLOCK_PRESSURIZED = create("warped_wart_block_pressurized", builder -> builder.require(Blocks.WARPED_FUNGUS).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 1).output(Blocks.WARPED_WART_BLOCK));
    private final GeneratedRecipe AZALEA = create("azalea", builder -> builder.require(Blocks.MOSS_BLOCK).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.AZALEA));
    private final GeneratedRecipe AZALEA_PRESSURIZED = create("azalea_pressurized", builder -> builder.require(Blocks.MOSS_BLOCK).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.AZALEA));
    private final GeneratedRecipe TALL_GRASS = create("tall_grass", builder -> builder.require(Blocks.SHORT_GRASS).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.TALL_GRASS));
    private final GeneratedRecipe TALL_GRASS_PRESSURIZED = create("tall_grass_pressurized", builder -> builder.require(Blocks.SHORT_GRASS).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.TALL_GRASS));
    private final GeneratedRecipe LARGE_FERN = create("large_fern", builder -> builder.require(Blocks.FERN).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.LARGE_FERN));
    private final GeneratedRecipe LARGE_FERN_PRESSURIZED = create("large_fern_pressurized", builder -> builder.require(Blocks.FERN).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.LARGE_FERN));
    private final GeneratedRecipe CRIMSON_ROOTS = create("crimson_roots", builder -> builder.require(Blocks.NETHER_WART).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.CRIMSON_ROOTS));
    private final GeneratedRecipe CRIMSON_ROOTS_PRESSURIZED = create("crimson_roots_pressurized", builder -> builder.require(Blocks.NETHER_WART).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.CRIMSON_ROOTS));
    private final GeneratedRecipe WEEPING_VINES = create("weeping_vines", builder -> builder.require(Blocks.CRIMSON_ROOTS).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.WEEPING_VINES));
    private final GeneratedRecipe WEEPING_VINES_PRESSURIZED = create("weeping_vines_pressurized", builder -> builder.require(Blocks.CRIMSON_ROOTS).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.WEEPING_VINES));
    private final GeneratedRecipe WARPED_ROOTS = create("warped_roots", builder -> builder.require(Blocks.NETHER_SPROUTS).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.WARPED_ROOTS));
    private final GeneratedRecipe WARPED_ROOTS_PRESSURIZED = create("warped_roots_pressurized", builder -> builder.require(Blocks.NETHER_SPROUTS).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.WARPED_ROOTS));
    private final GeneratedRecipe TWISTING_VINES = create("twisting_vines", builder -> builder.require(Blocks.WARPED_ROOTS).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.TWISTING_VINES));
    private final GeneratedRecipe TWISTING_VINES_PRESSURIZED = create("twisting_vines_pressurized", builder -> builder.require(Blocks.WARPED_ROOTS).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.TWISTING_VINES));
    private final GeneratedRecipe BIG_DRIPLEAF = create("big_dripleaf", builder -> builder.require(Blocks.SMALL_DRIPLEAF).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.BIG_DRIPLEAF));
    private final GeneratedRecipe BIG_DRIPLEAF_PRESSURIZED = create("big_dripleaf_pressurized", builder -> builder.require(Blocks.SMALL_DRIPLEAF).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.BIG_DRIPLEAF));
    private final GeneratedRecipe TUBE_CORAL = create("tube_coral", builder -> builder.require(Blocks.TUBE_CORAL_FAN).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.TUBE_CORAL));
    private final GeneratedRecipe TUBE_CORAL_PRESSURIZED = create("tube_coral_pressurized", builder -> builder.require(Blocks.TUBE_CORAL_FAN).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.TUBE_CORAL));
    private final GeneratedRecipe TUBE_CORAL_BLOCK = create("tube_coral_block", builder -> builder.require(Blocks.TUBE_CORAL).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.TUBE_CORAL_BLOCK));
    private final GeneratedRecipe TUBE_CORAL_BLOCK_PRESSURIZED = create("tube_coral_block_pressurized", builder -> builder.require(Blocks.TUBE_CORAL).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.TUBE_CORAL_BLOCK));
    private final GeneratedRecipe BRAIN_CORAL = create("brain_coral", builder -> builder.require(Blocks.BRAIN_CORAL_FAN).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.BRAIN_CORAL));
    private final GeneratedRecipe BRAIN_CORAL_PRESSURIZED = create("brain_coral_pressurized", builder -> builder.require(Blocks.BRAIN_CORAL_FAN).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.BRAIN_CORAL));
    private final GeneratedRecipe BRAIN_CORAL_BLOCK = create("brain_coral_block", builder -> builder.require(Blocks.BRAIN_CORAL).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.BRAIN_CORAL_BLOCK));
    private final GeneratedRecipe BRAIN_CORAL_BLOCK_PRESSURIZED = create("brain_coral_block_pressurized", builder -> builder.require(Blocks.BRAIN_CORAL).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.BRAIN_CORAL_BLOCK));
    private final GeneratedRecipe BUBBLE_CORAL = create("bubble_coral", builder -> builder.require(Blocks.BUBBLE_CORAL_FAN).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.BUBBLE_CORAL));
    private final GeneratedRecipe BUBBLE_CORAL_PRESSURIZED = create("bubble_coral_pressurized", builder -> builder.require(Blocks.BUBBLE_CORAL_FAN).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.BUBBLE_CORAL));
    private final GeneratedRecipe BUBBLE_CORAL_BLOCK = create("bubble_coral_block", builder -> builder.require(Blocks.BUBBLE_CORAL).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.BUBBLE_CORAL_BLOCK));
    private final GeneratedRecipe BUBBLE_CORAL_BLOCK_PRESSURIZED = create("bubble_coral_block_pressurized", builder -> builder.require(Blocks.BUBBLE_CORAL).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.BUBBLE_CORAL_BLOCK));
    private final GeneratedRecipe FIRE_CORAL = create("fire_coral", builder -> builder.require(Blocks.FIRE_CORAL_FAN).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.FIRE_CORAL));
    private final GeneratedRecipe FIRE_CORAL_PRESSURIZED = create("fire_coral_pressurized", builder -> builder.require(Blocks.FIRE_CORAL_FAN).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.FIRE_CORAL));
    private final GeneratedRecipe FIRE_CORAL_BLOCK = create("fire_coral_block", builder -> builder.require(Blocks.FIRE_CORAL).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.FIRE_CORAL_BLOCK));
    private final GeneratedRecipe FIRE_CORAL_BLOCK_PRESSURIZED = create("fire_coral_block_pressurized", builder -> builder.require(Blocks.FIRE_CORAL).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.FIRE_CORAL_BLOCK));
    private final GeneratedRecipe HORN_CORAL = create("horn_coral", builder -> builder.require(Blocks.HORN_CORAL_FAN).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.HORN_CORAL));
    private final GeneratedRecipe HORN_CORAL_PRESSURIZED = create("horn_coral_pressurized", builder -> builder.require(Blocks.HORN_CORAL_FAN).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.HORN_CORAL));
    private final GeneratedRecipe HORN_CORAL_BLOCK = create("horn_coral_block", builder -> builder.require(Blocks.HORN_CORAL).require(CCBGases.ENERGIZED_NATURAL_AIR.get(), 100).output(Blocks.HORN_CORAL_BLOCK));
    private final GeneratedRecipe HORN_CORAL_BLOCK_PRESSURIZED = create("horn_coral_block_pressurized", builder -> builder.require(Blocks.HORN_CORAL).require(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), 5).output(Blocks.HORN_CORAL_BLOCK));

    private final GeneratedRecipe DEEPSLATE = create("deepslate", builder -> builder.require(Blocks.STONE).require(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 100).output(Blocks.DEEPSLATE));
    private final GeneratedRecipe DEEPSLATE_PRESSURIZED = create("deepslate_pressurized", builder -> builder.require(Blocks.STONE).require(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), 5).output(Blocks.DEEPSLATE));
    private final GeneratedRecipe TUFF = create("tuff", builder -> builder.require(Blocks.GRAVEL).require(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 100).output(Blocks.TUFF));
    private final GeneratedRecipe TUFF_PRESSURIZED = create("tuff_pressurized", builder -> builder.require(Blocks.GRAVEL).require(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), 5).output(Blocks.TUFF));
    private final GeneratedRecipe DEAD_BUSH = create("dead_bush", builder -> builder.require(Blocks.SHORT_GRASS).require(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 100).output(Blocks.DEAD_BUSH));
    private final GeneratedRecipe DEAD_BUSH_PRESSURIZED = create("dead_bush_pressurized", builder -> builder.require(Blocks.SHORT_GRASS).require(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), 5).output(Blocks.DEAD_BUSH));

    private final GeneratedRecipe CRIMSON_NYLIUM = create("crimson_nylium", builder -> builder.require(Blocks.WARPED_NYLIUM).require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 100).output(Blocks.CRIMSON_NYLIUM));
    private final GeneratedRecipe CRIMSON_NYLIUM_PRESSURIZED = create("crimson_nylium_pressurized", builder -> builder.require(Blocks.WARPED_NYLIUM).require(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 5).output(Blocks.CRIMSON_NYLIUM));
    private final GeneratedRecipe WARPED_NYLIUM = create("warped_nylium", builder -> builder.require(Blocks.CRIMSON_NYLIUM).require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 100).output(Blocks.WARPED_NYLIUM));
    private final GeneratedRecipe WARPED_NYLIUM_PRESSURIZED = create("warped_nylium_pressurized", builder -> builder.require(Blocks.CRIMSON_NYLIUM).require(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 5).output(Blocks.WARPED_NYLIUM));
    private final GeneratedRecipe RED_MUSHROOM_BLOCK_FROM_BROWN = create("red_mushroom_block_from_brown", builder -> builder.require(Blocks.BROWN_MUSHROOM_BLOCK).require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 100).output(Blocks.RED_MUSHROOM_BLOCK));
    private final GeneratedRecipe RED_MUSHROOM_BLOCK_FROM_BROWN_PRESSURIZED = create("red_mushroom_block_from_brown_pressurized", builder -> builder.require(Blocks.BROWN_MUSHROOM_BLOCK).require(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 5).output(Blocks.RED_MUSHROOM_BLOCK));
    private final GeneratedRecipe BROWN_MUSHROOM_BLOCK_FROM_RED = create("brown_mushroom_block_from_red", builder -> builder.require(Blocks.RED_MUSHROOM_BLOCK).require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 100).output(Blocks.BROWN_MUSHROOM_BLOCK));
    private final GeneratedRecipe BROWN_MUSHROOM_BLOCK_FROM_RED_PRESSURIZED = create("brown_mushroom_block_from_red_pressurized", builder -> builder.require(Blocks.RED_MUSHROOM_BLOCK).require(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 5).output(Blocks.BROWN_MUSHROOM_BLOCK));
    private final GeneratedRecipe PEARLESCENT_FROGLIGHT = create("pearlescent_froglight", builder -> builder.require(Blocks.OCHRE_FROGLIGHT).require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 100).output(Blocks.PEARLESCENT_FROGLIGHT));
    private final GeneratedRecipe PEARLESCENT_FROGLIGHT_PRESSURIZED = create("pearlescent_froglight_pressurized", builder -> builder.require(Blocks.OCHRE_FROGLIGHT).require(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 5).output(Blocks.PEARLESCENT_FROGLIGHT));
    private final GeneratedRecipe VERDANT_FROGLIGHT = create("verdant_froglight", builder -> builder.require(Blocks.PEARLESCENT_FROGLIGHT).require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 100).output(Blocks.VERDANT_FROGLIGHT));
    private final GeneratedRecipe VERDANT_FROGLIGHT_PRESSURIZED = create("verdant_froglight_pressurized", builder -> builder.require(Blocks.PEARLESCENT_FROGLIGHT).require(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 5).output(Blocks.VERDANT_FROGLIGHT));
    private final GeneratedRecipe OCHRE_FROGLIGHT = create("ochre_froglight", builder -> builder.require(Blocks.VERDANT_FROGLIGHT).require(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), 100).output(Blocks.OCHRE_FROGLIGHT));
    private final GeneratedRecipe OCHRE_FROGLIGHT_PRESSURIZED = create("ochre_froglight_pressurized", builder -> builder.require(Blocks.VERDANT_FROGLIGHT).require(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), 5).output(Blocks.OCHRE_FROGLIGHT));

    private final GeneratedRecipe MYCELIUM = create("mycelium", builder -> builder.require(Blocks.GRASS_BLOCK).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.MYCELIUM));
    private final GeneratedRecipe ROOTED_DIRT = create("rooted_dirt", builder -> builder.require(Blocks.DIRT).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.ROOTED_DIRT));
    private final GeneratedRecipe MOSS_BLOCK = create("moss_block", builder -> builder.require(Blocks.STONE).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.MOSS_BLOCK));
    private final GeneratedRecipe CRIMSON_NYLIUM_FROM_NETHERRACK = create("crimson_nylium_from_netherrack", builder -> builder.require(Blocks.NETHERRACK).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.CRIMSON_NYLIUM));
    private final GeneratedRecipe SHROOMLIGHT_FROM_CRIMSON = create("shroomlight_from_crimson", builder -> builder.require(Blocks.CRIMSON_FUNGUS).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.SHROOMLIGHT));
    private final GeneratedRecipe SHROOMLIGHT_FROM_WARPED = create("shroomlight_from_warped", builder -> builder.require(Blocks.WARPED_FUNGUS).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.SHROOMLIGHT));
    private final GeneratedRecipe SPORE_BLOSSOM = create("spore_blossom", builder -> builder.require(Blocks.FLOWERING_AZALEA).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.SPORE_BLOSSOM));
    private final GeneratedRecipe CHORUS_FLOWER = create("chorus_flower", builder -> builder.require(Items.CHORUS_FRUIT).require(CCBGases.SPORE_AIR.get(), 500).output(Blocks.CHORUS_FLOWER));

    private final GeneratedRecipe MUD = create("mud", b -> b.require(Blocks.DIRT).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.MUD));
    private final GeneratedRecipe CLAY = create("clay", b -> b.require(Blocks.MUD).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.CLAY));
    private final GeneratedRecipe TUBE_CORAL_FAN_REVIVED = create("tube_coral_fan_revived", builder -> builder.require(Blocks.DEAD_TUBE_CORAL_FAN).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.TUBE_CORAL_FAN));
    private final GeneratedRecipe TUBE_CORAL_REVIVED = create("tube_coral_revived", builder -> builder.require(Blocks.DEAD_TUBE_CORAL).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.TUBE_CORAL));
    private final GeneratedRecipe TUBE_CORAL_BLOCK_REVIVED = create("tube_coral_block_revived", builder -> builder.require(Blocks.DEAD_TUBE_CORAL_BLOCK).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.TUBE_CORAL_BLOCK));
    private final GeneratedRecipe BRAIN_CORAL_FAN_REVIVED = create("brain_coral_fan_revived", builder -> builder.require(Blocks.DEAD_BRAIN_CORAL_FAN).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.BRAIN_CORAL_FAN));
    private final GeneratedRecipe BRAIN_CORAL_REVIVED = create("brain_coral_revived", builder -> builder.require(Blocks.DEAD_BRAIN_CORAL).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.BRAIN_CORAL));
    private final GeneratedRecipe BRAIN_CORAL_BLOCK_REVIVED = create("brain_coral_block_revived", builder -> builder.require(Blocks.DEAD_BRAIN_CORAL_BLOCK).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.BRAIN_CORAL_BLOCK));
    private final GeneratedRecipe BUBBLE_CORAL_FAN_REVIVED = create("bubble_coral_fan_revived", builder -> builder.require(Blocks.DEAD_BUBBLE_CORAL_FAN).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.BUBBLE_CORAL_FAN));
    private final GeneratedRecipe BUBBLE_CORAL_REVIVED = create("bubble_coral_revived", builder -> builder.require(Blocks.DEAD_BUBBLE_CORAL).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.BUBBLE_CORAL));
    private final GeneratedRecipe BUBBLE_CORAL_BLOCK_REVIVED = create("bubble_coral_block_revived", builder -> builder.require(Blocks.DEAD_BUBBLE_CORAL_BLOCK).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.BUBBLE_CORAL_BLOCK));
    private final GeneratedRecipe FIRE_CORAL_FAN_REVIVED = create("fire_coral_fan_revived", builder -> builder.require(Blocks.DEAD_FIRE_CORAL_FAN).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.FIRE_CORAL_FAN));
    private final GeneratedRecipe FIRE_CORAL_REVIVED = create("fire_coral_revived", builder -> builder.require(Blocks.DEAD_FIRE_CORAL).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.FIRE_CORAL));
    private final GeneratedRecipe FIRE_CORAL_BLOCK_REVIVED = create("fire_coral_block_revived", builder -> builder.require(Blocks.DEAD_FIRE_CORAL_BLOCK).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.FIRE_CORAL_BLOCK));
    private final GeneratedRecipe HORN_CORAL_FAN_REVIVED = create("horn_coral_fan_revived", builder -> builder.require(Blocks.DEAD_HORN_CORAL_FAN).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.HORN_CORAL_FAN));
    private final GeneratedRecipe HORN_CORAL_REVIVED = create("horn_coral_revived", builder -> builder.require(Blocks.DEAD_HORN_CORAL).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.HORN_CORAL));
    private final GeneratedRecipe HORN_CORAL_BLOCK_REVIVED = create("horn_coral_block_revived", builder -> builder.require(Blocks.DEAD_HORN_CORAL_BLOCK).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.HORN_CORAL_BLOCK));
    private final GeneratedRecipe WET_SPONGE = create("wet_sponge", builder -> builder.require(Blocks.SPONGE).require(CCBGases.MOIST_AIR.get(), 500).output(Blocks.WET_SPONGE));

    private final GeneratedRecipe SUNNY_FLARE = create("sunny_flare", builder -> builder.require(CCBItems.UNFILLED_WEATHER_FLARE).require(CCBGases.ULTRAWARM_AIR.get(), 500).output(CCBItems.SUNNY_FLARE));
    private final GeneratedRecipe SUNNY_FLARE_PRESSURIZED = create("sunny_flare_pressurized", builder -> builder.require(CCBItems.UNFILLED_WEATHER_FLARE).require(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 25).output(CCBItems.SUNNY_FLARE));
    private final GeneratedRecipe RAIN_FLARE = create("rain_flare", builder -> builder.require(CCBItems.UNFILLED_WEATHER_FLARE).require(CCBGases.MOIST_AIR.get(), 500).output(CCBItems.RAIN_FLARE));
    private final GeneratedRecipe THUNDERSTORM_FLARE = create("thunderstorm_flare", builder -> builder.require(CCBItems.UNFILLED_WEATHER_FLARE).require(CCBGases.ETHEREAL_AIR.get(), 500).output(CCBItems.THUNDERSTORM_FLARE));
    private final GeneratedRecipe THUNDERSTORM_FLARE_PRESSURIZED = create("thunderstorm_flare_pressurized", builder -> builder.require(CCBItems.UNFILLED_WEATHER_FLARE).require(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), 25).output(CCBItems.THUNDERSTORM_FLARE));

    private final GeneratedRecipe LAVA_TO_BRIMSTONE = create("lava_to_brimstone", builder -> builder.require(Fluids.LAVA, 1000).require(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 500).output(CCBFluids.BRIMSTONE.get(), 100));
    private final GeneratedRecipe LAVA_TO_BRIMSTONE_PRESSURIZED = create("lava_to_brimstone_pressurized", builder -> builder.require(Fluids.LAVA, 1000).require(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), 25).output(CCBFluids.BRIMSTONE.get(), 100));

    public CCBGasInjectionRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CCBAPI.MOD_ID);
    }
}
