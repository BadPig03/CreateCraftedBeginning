package net.ty.createcraftedbeginning.data;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.reactorkettle.TemperatureCondition;
import net.ty.createcraftedbeginning.recipe.generators.ReactorKettleRecipeGen;
import net.ty.createcraftedbeginning.registry.CCBItems;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBReactorKettleRecipes extends ReactorKettleRecipeGen {
    /*GeneratedRecipe ANDESITE_ALLOY = create("andesite_alloy", b -> b.require(Items.COBBLESTONE).require(Tags.Items.GEMS_QUARTZ).require(Tags.Items.NUGGETS_IRON).output(AllItems.ANDESITE_ALLOY));
    GeneratedRecipe ANDESITE_ALLOY_FROM_ZINC = create("andesite_alloy_from_zinc", b -> b.require(Items.COBBLESTONE).require(Tags.Items.GEMS_QUARTZ).require(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "nuggets/zinc"))).output(AllItems.ANDESITE_ALLOY));
    GeneratedRecipe BRASS_INGOT = create("brass_ingot", b -> b.require(Tags.Items.INGOTS_COPPER).require(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/zinc"))).temperatureCondition(TemperatureCondition.HEATED).output(AllItems.BRASS_INGOT.asItem(), 2));
    GeneratedRecipe BRASS_BLOCK = create("brass_block", b -> b.require(Tags.Items.STORAGE_BLOCKS_COPPER).require(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/zinc"))).temperatureCondition(TemperatureCondition.HEATED).output(AllBlocks.BRASS_BLOCK.asItem(), 2));
    GeneratedRecipe CHOCOLATE = create("chocolate", b -> b.require(Items.SUGAR).require(Items.COCOA_BEANS).require(Tags.Fluids.MILK, 500).temperatureCondition(TemperatureCondition.HEATED).output(AllFluids.CHOCOLATE.get(), 500));
    GeneratedRecipe CHOCOLATE_MELTING = create("chocolate_melting", b -> b.require(AllItems.BAR_OF_CHOCOLATE).temperatureCondition(TemperatureCondition.HEATED).output(AllFluids.CHOCOLATE.get(), 250));
    GeneratedRecipe PULP = create("pulp", b -> b.require(AllItemTags.PULPIFIABLE.tag).require(AllItemTags.PULPIFIABLE.tag).require(Tags.Fluids.WATER, 250).output(AllItems.PULP));
    GeneratedRecipe DOUGH_BY_MIXING = create("dough_by_mixing", b -> b.require(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "flours/wheat"))).require(Tags.Fluids.WATER, 500).output(AllItems.DOUGH));
    GeneratedRecipe LAVA_FROM_COBBLESTONE = create("lava_from_cobblestone", b -> b.require(Tags.Items.COBBLESTONES).temperatureCondition(TemperatureCondition.SUPERHEATED).output(Fluids.LAVA, 100));
    GeneratedRecipe HONEY = create("honey", b -> b.require(Blocks.HONEY_BLOCK).temperatureCondition(TemperatureCondition.HEATED).output(AllFluids.HONEY.get(), 1000));
    GeneratedRecipe MUD_BY_MIXING = create("mud_by_mixing", b -> b.require(new BlockTagIngredient(BlockTags.CONVERTABLE_TO_MUD)).require(Tags.Fluids.WATER, 125).output(Blocks.MUD));
    GeneratedRecipe TEA = create("tea", b -> b.require(Tags.Fluids.WATER, 500).require(Tags.Fluids.MILK, 500).require(ItemTags.LEAVES).temperatureCondition(TemperatureCondition.HEATED).output(AllFluids.TEA.get(), 1000));
    */

    /*GeneratedRecipe SLUSH = create("slush", b -> b.require(Tags.Fluids.WATER, 1000).require(Blocks.SNOW_BLOCK).output(CCBFluids.SLUSH.get(), 1500));
    GeneratedRecipe AMETHYST_SUSPENSION = create("amethyst_suspension", b -> b.require(Tags.Fluids.WATER, 250).require(CCBItems.POWDERED_AMETHYST).output(CCBFluids.AMETHYST_SUSPENSION.get(), 250));
    GeneratedRecipe CINDER_ALLOY = create("cinder_alloy", b -> b.require(AllItems.CINDER_FLOUR).require(Tags.Items.INGOTS_IRON).require(Tags.Items.INGOTS_GOLD).temperatureCondition(TemperatureCondition.SUPERHEATED).output(CCBItems.CINDER_ALLOY, 2));
    GeneratedRecipe END_ALLOY = create("end_alloy", b -> b.require(CCBItemTags.CHORUS_FLOWER.tag).require(Tags.Items.ENDER_PEARLS).require(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/zinc"))).temperatureCondition(TemperatureCondition.SUPERCHILLED).output(CCBItems.END_ALLOY, 2));
    GeneratedRecipe OBSIDIAN_FROM_WATER_AND_LAVA = create("obsidian_from_water_and_lava", b -> b.require(Tags.Fluids.WATER, 250).require(Tags.Fluids.LAVA, 750).duration(200).output(Blocks.OBSIDIAN));
    GeneratedRecipe ICE_CHILLED = create("ice_chilled", b -> b.require(Tags.Fluids.WATER, 1000).temperatureCondition(TemperatureCondition.CHILLED).duration(100).output(Blocks.ICE));
    GeneratedRecipe ICE_SUPERCHILLED = create("ice_superchilled", b -> b.require(Tags.Fluids.WATER, 1000).temperatureCondition(TemperatureCondition.SUPERCHILLED).duration(50).output(Blocks.ICE));

    GeneratedRecipe CINDER_FLOUR_ULTRAWARM = create("cinder_flour_ultrawarm", b -> b.require(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "flours/wheat"))).require(CCBGases.ULTRAWARM_AIR.get(), 200).temperatureCondition(TemperatureCondition.HEATED).output(AllItems.CINDER_FLOUR));
    GeneratedRecipe CINDER_FLOUR_PRESSURIZED_ULTRAWARM = create("cinder_flour_pressurized_ultrawarm", b -> b.require(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "flours/wheat"))).require(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 10).temperatureCondition(TemperatureCondition.HEATED).output(AllItems.CINDER_FLOUR));
    GeneratedRecipe BLAZE_POWDER_ULTRAWARM = create("blaze_powder_ultrawarm", b -> b.require(Tags.Items.GUNPOWDERS).require(CCBGases.ULTRAWARM_AIR.get(), 200).temperatureCondition(TemperatureCondition.SUPERHEATED).output(Items.BLAZE_POWDER));
    GeneratedRecipe BLAZE_POWDER_PRESSURIZED_ULTRAWARM = create("blaze_powder_pressurized_ultrawarm", b -> b.require(Tags.Items.GUNPOWDERS).require(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 10).temperatureCondition(TemperatureCondition.SUPERHEATED).output(Items.BLAZE_POWDER));
    GeneratedRecipe NETHER_WART_ULTRAWARM = create("nether_wart_ultrawarm", b -> b.require(Tags.Items.SEEDS).require(CCBGases.ULTRAWARM_AIR.get(), 200).temperatureCondition(TemperatureCondition.HEATED).output(Items.NETHER_WART));
    GeneratedRecipe NETHER_WART_PRESSURIZED_ULTRAWARM = create("nether_wart_pressurized_ultrawarm", b -> b.require(Tags.Items.SEEDS).require(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 10).temperatureCondition(TemperatureCondition.HEATED).output(Items.NETHER_WART));
    GeneratedRecipe GUNPOWDER_COAL = create("gunpowder_coal", b -> b.require(Items.COAL).require(Items.FLINT).temperatureCondition(TemperatureCondition.HEATED).output(Items.GUNPOWDER, 2));
    GeneratedRecipe GUNPOWDER_CHARCOAL = create("gunpowder_charcoal", b -> b.require(Items.CHARCOAL).require(Items.FLINT).temperatureCondition(TemperatureCondition.HEATED).output(Items.GUNPOWDER, 3));
    */

    GeneratedRecipe NATURAL_AIR = create("natural_air", b -> b.require(CCBItems.BREEZE_CORE).require(Tags.Items.STONES).output(CCBGases.NATURAL_AIR.get(), 10).output(CCBItems.BREEZE_CORE).output(0.25f, Items.GRAVEL));
    GeneratedRecipe ULTRAWARM_AIR = create("ultrawarm_air", b -> b.require(CCBItems.BREEZE_CORE).require(Tags.Items.NETHERRACKS).output(CCBGases.ULTRAWARM_AIR.get(), 10).output(CCBItems.BREEZE_CORE).output(0.25f, Items.GRAVEL).temperatureCondition(TemperatureCondition.SUPERHEATED));
    GeneratedRecipe ETHEREAL_AIR = create("ethereal_air", b -> b.require(CCBItems.BREEZE_CORE).require(Tags.Items.END_STONES).output(CCBGases.ETHEREAL_AIR.get(), 10).output(CCBItems.BREEZE_CORE).output(0.25f, Items.GRAVEL).temperatureCondition(TemperatureCondition.SUPERCHILLED));

    public CCBReactorKettleRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
