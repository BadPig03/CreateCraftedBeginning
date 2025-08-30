package net.ty.createcraftedbeginning.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBFluids;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBCoolingRecipes extends CoolingRecipeGen {
    GeneratedRecipe

    SNOWBALL = create("snowball", b -> b.require(Items.SNOWBALL).output(CCBFluids.COOLING_TIME.get(), 5)),

    SNOW = create("snow", b -> b.require(Blocks.SNOW).output(CCBFluids.COOLING_TIME.get(), 10)),

    SNOW_BLOCK = create("snow_block", b -> b.require(Blocks.SNOW_BLOCK).output(CCBFluids.COOLING_TIME.get(), 20)),

    ICE = create("ice", b -> b.require(Blocks.ICE).output(CCBFluids.COOLING_TIME.get(), 180)),

    PACKED_ICE = create("packed_ice", b -> b.require(Blocks.PACKED_ICE).output(CCBFluids.COOLING_TIME.get(), 1620)),

    BLUE_ICE = create("blue_ice", b -> b.require(Blocks.BLUE_ICE).output(CCBFluids.COOLING_TIME.get(), 14580)),

    WATER = create("water", b -> b.require(Fluids.WATER, 500).output(CCBFluids.COOLING_TIME.get(), 8)),

    WATER_BUCKET = create("water_bucket", b -> b.require(Items.WATER_BUCKET).output(CCBFluids.COOLING_TIME.get(), 16)),

    MILK = create("milk", b -> b.require(NeoForgeMod.MILK.get(), 500).output(CCBFluids.COOLING_TIME.get(), 10)),

    MILK_BUCKET = create("milk_bucket", b -> b.require(Items.MILK_BUCKET).output(CCBFluids.COOLING_TIME.get(), 20));

    public CCBCoolingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
