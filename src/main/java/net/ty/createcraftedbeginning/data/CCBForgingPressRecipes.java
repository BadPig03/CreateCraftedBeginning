package net.ty.createcraftedbeginning.data;

import com.simibubi.create.AllItems;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.recipe.generators.ForgingPressRecipeGen;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBForgingPressRecipes extends ForgingPressRecipeGen {
    GeneratedRecipe OBSIDIAN_CHUNK = create("obsidian_chunk", b -> b.require(Tags.Items.OBSIDIANS_NORMAL).output(CCBItems.OBSIDIAN_CHUNK));
    GeneratedRecipe CRYING_OBSIDIAN_CHUNK = create("crying_obsidian_chunk", b -> b.require(Tags.Items.OBSIDIANS_CRYING).output(CCBItems.CRYING_OBSIDIAN_CHUNK));

    GeneratedRecipe PAPER = create("paper", b -> b.require(Tags.Items.CROPS_SUGAR_CANE).output(Items.PAPER));
    GeneratedRecipe DIRT_PATH_FROM_DIRT = create("dirt_path_from_dirt", b -> b.require(Ingredient.of(Items.DIRT, Items.COARSE_DIRT, Items.ROOTED_DIRT, Items.MYCELIUM, Items.PODZOL)).output(Items.DIRT_PATH));
    GeneratedRecipe DIRT_PATH_FROM_GRASS = create("dirt_path_from_grass", b -> b.require(Items.GRASS_BLOCK).output(Items.DIRT_PATH));
    GeneratedRecipe CARDBOARD = create("cardboard", b -> b.require(AllItems.PULP).output(AllItems.CARDBOARD));

    GeneratedRecipe IRON_SHEET = create("iron_sheet", b -> b.require(Tags.Items.INGOTS_IRON).output(AllItems.IRON_SHEET));
    GeneratedRecipe COPPER_SHEET = create("copper_sheet", b -> b.require(Tags.Items.INGOTS_COPPER).output(AllItems.COPPER_SHEET));
    GeneratedRecipe GOLDEN_SHEET = create("golden_sheet", b -> b.require(Tags.Items.INGOTS_GOLD).output(AllItems.GOLDEN_SHEET));
    GeneratedRecipe END_ALLOY_SHEET = create("end_alloy_sheet", b -> b.require(CCBItems.END_ALLOY).output(CCBItems.END_ALLOY_SHEET));
    GeneratedRecipe BRASS_SHEET = create("brass_sheet", b -> b.require(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/brass"))).output(AllItems.BRASS_SHEET));

    GeneratedRecipe DIAMOND_FROM_CHARCOAL = create("diamond_from_charcoal", b -> b.require(Items.CHARCOAL).require(Items.HEAVY_CORE).require(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), 250).output(0.2f, Items.DIAMOND));

    public CCBForgingPressRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
