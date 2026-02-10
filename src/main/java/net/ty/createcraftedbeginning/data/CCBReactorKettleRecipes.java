package net.ty.createcraftedbeginning.data;

import com.simibubi.create.AllItems;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.Tags.Fluids;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.reactorkettle.TemperatureCondition;
import net.ty.createcraftedbeginning.recipe.generators.ReactorKettleRecipeGen;
import net.ty.createcraftedbeginning.registry.CCBItems;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBReactorKettleRecipes extends ReactorKettleRecipeGen {
    private static final TagKey<Item> ZINC_NUGGETS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "nuggets/zinc"));
    private static final TagKey<Item> WHEAT_FLOUR = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "flours/wheat"));

    GeneratedRecipe ANDESITE_ALLOY = create("andesite_alloy", b -> b.require(Tags.Items.COBBLESTONES).require(Tags.Items.GEMS_QUARTZ).require(Tags.Items.NUGGETS_IRON).duration(0).output(AllItems.ANDESITE_ALLOY));
    GeneratedRecipe ANDESITE_ALLOY_FROM_ZINC = create("andesite_alloy_from_zinc", b -> b.require(Tags.Items.COBBLESTONES).require(Tags.Items.GEMS_QUARTZ).require(ZINC_NUGGETS).duration(0).output(AllItems.ANDESITE_ALLOY));
    GeneratedRecipe GUNPOWDER_FROM_COAL = create("gunpowder_from_coal", b -> b.require(Items.COAL).require(Items.FLINT).temperatureCondition(TemperatureCondition.HEATED).duration(0).output(Items.GUNPOWDER, 2));
    GeneratedRecipe GUNPOWDER_FROM_CHARCOAL = create("gunpowder_from_charcoal", b -> b.require(Items.CHARCOAL).require(Items.FLINT).temperatureCondition(TemperatureCondition.HEATED).duration(0).output(Items.GUNPOWDER, 3));
    GeneratedRecipe ICE_CHILLED = create("ice_chilled", b -> b.require(Fluids.WATER, 1000).temperatureCondition(TemperatureCondition.CHILLED).duration(0).output(Blocks.ICE));
    GeneratedRecipe ICE_SUPERCHILLED = create("ice_superchilled", b -> b.require(Fluids.WATER, 1000).temperatureCondition(TemperatureCondition.SUPERCHILLED).duration(0).output(Blocks.ICE));

    GeneratedRecipe NATURAL_AIR = create("natural_air", b -> b.require(CCBItems.BREEZE_CORE).require(Tags.Items.STONES).duration(200).output(CCBGases.NATURAL_AIR.get(), 10).output(CCBItems.BREEZE_CORE).output(0.25f, Items.GRAVEL));
    GeneratedRecipe ULTRAWARM_AIR = create("ultrawarm_air", b -> b.require(CCBItems.BREEZE_CORE).require(Tags.Items.NETHERRACKS).duration(200).output(CCBGases.ULTRAWARM_AIR.get(), 10).output(CCBItems.BREEZE_CORE).output(0.25f, Items.GRAVEL).temperatureCondition(TemperatureCondition.SUPERHEATED));
    GeneratedRecipe ETHEREAL_AIR = create("ethereal_air", b -> b.require(CCBItems.BREEZE_CORE).require(Tags.Items.END_STONES).duration(200).output(CCBGases.ETHEREAL_AIR.get(), 10).output(CCBItems.BREEZE_CORE).output(0.25f, Items.GRAVEL).temperatureCondition(TemperatureCondition.SUPERCHILLED));

    GeneratedRecipe NETHER_WART = create("nether_wart", b -> b.require(Tags.Items.SEEDS).require(AllItems.CINDER_FLOUR).require(CCBGases.ULTRAWARM_AIR.get(), 500).temperatureCondition(TemperatureCondition.HEATED).averageProcessingDuration().output(Items.NETHER_WART));
    GeneratedRecipe NETHER_WART_PRESSURIZED = create("nether_wart_pressurized", b -> b.require(Tags.Items.SEEDS).require(AllItems.CINDER_FLOUR).require(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 25).temperatureCondition(TemperatureCondition.HEATED).averageProcessingDuration().output(Items.NETHER_WART));
    GeneratedRecipe CINDER_FLOUR = create("cinder_flour", b -> b.require(AllItems.CINDER_FLOUR).require(WHEAT_FLOUR).require(CCBGases.ULTRAWARM_AIR.get(), 500).temperatureCondition(TemperatureCondition.HEATED).averageProcessingDuration().output(AllItems.CINDER_FLOUR, 2));
    GeneratedRecipe CINDER_FLOUR_PRESSURIZED = create("cinder_flour_pressurized", b -> b.require(AllItems.CINDER_FLOUR).require(WHEAT_FLOUR).require(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 25).temperatureCondition(TemperatureCondition.HEATED).averageProcessingDuration().output(AllItems.CINDER_FLOUR, 2));
    GeneratedRecipe BLAZE_POWDER = create("blaze_powder", b -> b.require(Tags.Items.GUNPOWDERS).require(CCBGases.ULTRAWARM_AIR.get(), 500).temperatureCondition(TemperatureCondition.SUPERHEATED).averageProcessingDuration().output(Items.BLAZE_POWDER));
    GeneratedRecipe BLAZE_POWDER_PRESSURIZED = create("blaze_powder_pressurized", b -> b.require(Tags.Items.GUNPOWDERS).require(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 25).temperatureCondition(TemperatureCondition.SUPERHEATED).averageProcessingDuration().output(Items.BLAZE_POWDER));
    GeneratedRecipe OBSIDIAN = create("obsidian", b -> b.require(Tags.Items.COBBLESTONES).require(CCBGases.MOIST_AIR.get(), 250).require(CCBGases.ULTRAWARM_AIR.get(), 250).duration(200).output(Blocks.OBSIDIAN));

    public CCBReactorKettleRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
