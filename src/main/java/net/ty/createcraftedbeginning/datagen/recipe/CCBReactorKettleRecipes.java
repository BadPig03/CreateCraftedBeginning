package net.ty.createcraftedbeginning.datagen.recipe;

import com.simibubi.create.AllItems;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.Tags.Fluids;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.datagen.recipe.generator.ReactorKettleRecipeGen;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureMatching;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBTags;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBReactorKettleRecipes extends ReactorKettleRecipeGen {
    private final GeneratedRecipe ANDESITE_ALLOY = create("andesite_alloy", builder -> builder.require(Tags.Items.COBBLESTONES).require(Tags.Items.GEMS_QUARTZ).require(Tags.Items.NUGGETS_IRON).temperatureCondition(TemperatureCondition.NONE).temperatureMatching(TemperatureMatching.COMPATIBLE).duration(0).output(AllItems.ANDESITE_ALLOY));
    private final GeneratedRecipe ANDESITE_ALLOY_FROM_ZINC = create("andesite_alloy_from_zinc", builder -> builder.require(Tags.Items.COBBLESTONES).require(Tags.Items.GEMS_QUARTZ).require(CCBTags.commonItemTag("nuggets/zinc")).temperatureCondition(TemperatureCondition.NONE).temperatureMatching(TemperatureMatching.COMPATIBLE).duration(0).output(AllItems.ANDESITE_ALLOY));
    private final GeneratedRecipe GUNPOWDER_FROM_COAL = create("gunpowder_from_coal", builder -> builder.require(Items.COAL).require(Items.FLINT).temperatureCondition(TemperatureCondition.HEATED).temperatureMatching(TemperatureMatching.COMPATIBLE).duration(0).output(Items.GUNPOWDER, 2));
    private final GeneratedRecipe GUNPOWDER_FROM_CHARCOAL = create("gunpowder_from_charcoal", builder -> builder.require(Items.CHARCOAL).require(Items.FLINT).temperatureCondition(TemperatureCondition.HEATED).temperatureMatching(TemperatureMatching.COMPATIBLE).duration(0).output(Items.GUNPOWDER, 3));
    private final GeneratedRecipe ICE_SUPERCHILLED = create("ice_superchilled", builder -> builder.require(Fluids.WATER, 1000).temperatureCondition(TemperatureCondition.SUPERCHILLED).duration(0).output(0.75F, Blocks.PACKED_ICE).output(0.25F, Blocks.BLUE_ICE));
    private final GeneratedRecipe OBSIDIAN = create("obsidian", builder -> builder.require(Fluids.LAVA, 1000).temperatureCondition(TemperatureCondition.SUPERCHILLED).duration(0).output(Blocks.OBSIDIAN));

    private final GeneratedRecipe NATURAL_AIR = create("natural_air", builder -> builder.require(CCBItems.BREEZE_CORE).require(Tags.Items.STONES).duration(50).output(CCBGases.NATURAL_AIR.get(), 1000).output(CCBItems.BREEZE_CORE).output(0.25F, Items.GRAVEL));
    private final GeneratedRecipe ULTRAWARM_AIR = create("ultrawarm_air", builder -> builder.require(CCBItems.BREEZE_CORE).require(Tags.Items.NETHERRACKS).temperatureCondition(TemperatureCondition.SUPERHEATED).duration(50).output(CCBGases.ULTRAWARM_AIR.get(), 1000).output(CCBItems.BREEZE_CORE).output(0.25F, Items.GRAVEL));
    private final GeneratedRecipe ETHEREAL_AIR = create("ethereal_air", builder -> builder.require(CCBItems.BREEZE_CORE).require(Tags.Items.END_STONES).temperatureCondition(TemperatureCondition.SUPERCHILLED).duration(50).output(CCBGases.ETHEREAL_AIR.get(), 1000).output(CCBItems.BREEZE_CORE).output(0.25F, Items.GRAVEL));

    private final GeneratedRecipe NETHER_WART = create("nether_wart", builder -> builder.require(Tags.Items.SEEDS).require(AllItems.CINDER_FLOUR).require(CCBGases.ULTRAWARM_AIR.get(), 5000).temperatureCondition(TemperatureCondition.HEATED).temperatureMatching(TemperatureMatching.COMPATIBLE).duration(0).output(Items.NETHER_WART));
    private final GeneratedRecipe CINDER_FLOUR = create("cinder_flour", builder -> builder.require(AllItems.CINDER_FLOUR).require(CCBTags.commonItemTag("flours/wheat")).require(CCBGases.ULTRAWARM_AIR.get(), 5000).temperatureCondition(TemperatureCondition.HEATED).temperatureMatching(TemperatureMatching.COMPATIBLE).duration(0).output(AllItems.CINDER_FLOUR, 2));
    private final GeneratedRecipe BLAZE_POWDER = create("blaze_powder", builder -> builder.require(Tags.Items.GUNPOWDERS).require(CCBGases.ULTRAWARM_AIR.get(), 5000).temperatureCondition(TemperatureCondition.SUPERHEATED).duration(0).output(Items.BLAZE_POWDER));

    public CCBReactorKettleRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CCBAPI.MOD_ID);
    }
}
