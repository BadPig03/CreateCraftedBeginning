package net.ty.createcraftedbeginning.datagen.recipe;

import com.simibubi.create.AllItems;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.recipes.TemperatureCondition;
import net.ty.createcraftedbeginning.api.gas.recipes.TemperatureMatching;
import net.ty.createcraftedbeginning.datagen.recipe.generator.ReactorKettleRecipeGen;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBReactorKettleRecipes extends ReactorKettleRecipeGen {
    private static final TagKey<Item> ZINC_NUGGETS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "nuggets/zinc"));
    private static final TagKey<Item> WHEAT_FLOUR = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "flours/wheat"));

    private final GeneratedRecipe ANDESITE_ALLOY = create("andesite_alloy", builder -> builder.require(Tags.Items.COBBLESTONES).require(Tags.Items.GEMS_QUARTZ).require(Tags.Items.NUGGETS_IRON).temperatureCondition(TemperatureCondition.NONE).temperatureMatching(TemperatureMatching.COMPATIBLE).duration(0).output(AllItems.ANDESITE_ALLOY));
    private final GeneratedRecipe ANDESITE_ALLOY_FROM_ZINC = create("andesite_alloy_from_zinc", builder -> builder.require(Tags.Items.COBBLESTONES).require(Tags.Items.GEMS_QUARTZ).require(ZINC_NUGGETS).temperatureCondition(TemperatureCondition.NONE).temperatureMatching(TemperatureMatching.COMPATIBLE).duration(0).output(AllItems.ANDESITE_ALLOY));
    private final GeneratedRecipe GUNPOWDER_FROM_COAL = create("gunpowder_from_coal", builder -> builder.require(Items.COAL).require(Items.FLINT).temperatureCondition(TemperatureCondition.HEATED).temperatureMatching(TemperatureMatching.COMPATIBLE).duration(0).output(Items.GUNPOWDER, 2));
    private final GeneratedRecipe GUNPOWDER_FROM_CHARCOAL = create("gunpowder_from_charcoal", builder -> builder.require(Items.CHARCOAL).require(Items.FLINT).temperatureCondition(TemperatureCondition.HEATED).temperatureMatching(TemperatureMatching.COMPATIBLE).duration(0).output(Items.GUNPOWDER, 3));
    private final GeneratedRecipe ICE_CHILLED = create("ice_chilled", builder -> builder.require(Fluids.WATER, 1000).temperatureCondition(TemperatureCondition.CHILLED).duration(0).output(Blocks.ICE));
    private final GeneratedRecipe ICE_SUPERCHILLED = create("ice_superchilled", builder -> builder.require(Fluids.WATER, 1000).temperatureCondition(TemperatureCondition.SUPERCHILLED).duration(0).output(0.75f, Blocks.PACKED_ICE).output(0.25f, Blocks.BLUE_ICE));
    private final GeneratedRecipe OBSIDIAN = create("obsidian", builder -> builder.require(Tags.Items.COBBLESTONES).require(CCBGases.MOIST_AIR.get(), 250).require(CCBGases.ULTRAWARM_AIR.get(), 250).averageProcessingDuration().output(Blocks.OBSIDIAN));

    private final GeneratedRecipe NATURAL_AIR = create("natural_air", builder -> builder.require(CCBItems.BREEZE_CORE).require(Tags.Items.STONES).duration(200).output(CCBGases.NATURAL_AIR.get(), 10).output(CCBItems.BREEZE_CORE).output(0.25f, Items.GRAVEL));
    private final GeneratedRecipe ULTRAWARM_AIR = create("ultrawarm_air", builder -> builder.require(CCBItems.BREEZE_CORE).require(Tags.Items.NETHERRACKS).temperatureCondition(TemperatureCondition.SUPERHEATED).duration(200).output(CCBGases.ULTRAWARM_AIR.get(), 10).output(CCBItems.BREEZE_CORE).output(0.25f, Items.GRAVEL));
    private final GeneratedRecipe ETHEREAL_AIR = create("ethereal_air", builder -> builder.require(CCBItems.BREEZE_CORE).require(Tags.Items.END_STONES).temperatureCondition(TemperatureCondition.SUPERCHILLED).duration(200).output(CCBGases.ETHEREAL_AIR.get(), 10).output(CCBItems.BREEZE_CORE).output(0.25f, Items.GRAVEL));

    private final GeneratedRecipe NETHER_WART = create("nether_wart", builder -> builder.require(Tags.Items.SEEDS).require(AllItems.CINDER_FLOUR).require(CCBGases.ULTRAWARM_AIR.get(), 500).temperatureCondition(TemperatureCondition.HEATED).temperatureMatching(TemperatureMatching.COMPATIBLE).averageProcessingDuration().output(Items.NETHER_WART));
    private final GeneratedRecipe NETHER_WART_PRESSURIZED = create("nether_wart_pressurized", builder -> builder.require(Tags.Items.SEEDS).require(AllItems.CINDER_FLOUR).require(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 25).temperatureCondition(TemperatureCondition.HEATED).temperatureMatching(TemperatureMatching.COMPATIBLE).averageProcessingDuration().output(Items.NETHER_WART));
    private final GeneratedRecipe CINDER_FLOUR = create("cinder_flour", builder -> builder.require(AllItems.CINDER_FLOUR).require(WHEAT_FLOUR).require(CCBGases.ULTRAWARM_AIR.get(), 500).temperatureCondition(TemperatureCondition.HEATED).temperatureMatching(TemperatureMatching.COMPATIBLE).averageProcessingDuration().output(AllItems.CINDER_FLOUR, 2));
    private final GeneratedRecipe CINDER_FLOUR_PRESSURIZED = create("cinder_flour_pressurized", builder -> builder.require(AllItems.CINDER_FLOUR).require(WHEAT_FLOUR).require(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 25).temperatureCondition(TemperatureCondition.HEATED).temperatureMatching(TemperatureMatching.COMPATIBLE).averageProcessingDuration().output(AllItems.CINDER_FLOUR, 2));
    private final GeneratedRecipe BLAZE_POWDER = create("blaze_powder", builder -> builder.require(Tags.Items.GUNPOWDERS).require(CCBGases.ULTRAWARM_AIR.get(), 500).temperatureCondition(TemperatureCondition.SUPERHEATED).averageProcessingDuration().output(Items.BLAZE_POWDER));
    private final GeneratedRecipe BLAZE_POWDER_PRESSURIZED = create("blaze_powder_pressurized", builder -> builder.require(Tags.Items.GUNPOWDERS).require(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 25).temperatureCondition(TemperatureCondition.SUPERHEATED).averageProcessingDuration().output(Items.BLAZE_POWDER));

    public CCBReactorKettleRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CCBAPI.MOD_ID);
    }
}
