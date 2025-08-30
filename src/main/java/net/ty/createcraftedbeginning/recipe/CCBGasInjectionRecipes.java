package net.ty.createcraftedbeginning.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBFluids;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBGasInjectionRecipes extends GasInjectionRecipeGen {
    GeneratedRecipe

    WIND_CHARGE = create("wind_charge", b -> b.require(Items.BLAZE_POWDER).require(CCBFluids.MEDIUM_PRESSURE_COMPRESSED_AIR.get(), 250).output(Items.WIND_CHARGE, 2)),

    WIND_CHARGE_FROM_HIGH_PRESSURE = create("wind_charge_from_high_pressure", b -> b.require(Items.BLAZE_POWDER).require(CCBFluids.HIGH_PRESSURE_COMPRESSED_AIR.get(), 15).output(Items.WIND_CHARGE, 2)),

    BREEZE_ROD = create("breeze_rod", b -> b.require(Items.BLAZE_ROD).require(CCBFluids.MEDIUM_PRESSURE_COMPRESSED_AIR.get(), 500).output(Items.BREEZE_ROD)),

    BREEZE_ROD_FROM_HIGH_PRESSURE = create("breeze_rod_from_high_pressure", b -> b.require(Items.BLAZE_ROD).require(CCBFluids.HIGH_PRESSURE_COMPRESSED_AIR.get(), 30).output(Items.BREEZE_ROD));

    public CCBGasInjectionRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
