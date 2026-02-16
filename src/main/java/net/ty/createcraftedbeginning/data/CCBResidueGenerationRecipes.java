package net.ty.createcraftedbeginning.data;

import com.simibubi.create.AllItems;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.recipe.generators.ResidueGenerationGen;
import net.ty.createcraftedbeginning.registry.CCBItems;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBResidueGenerationRecipes extends ResidueGenerationGen {
    GeneratedRecipe NATURAL = create("natural", b -> b.require(CCBGases.NATURAL_AIR.get(), 1).output(Items.CLAY_BALL));
    GeneratedRecipe PRESSURIZED_NATURAL = create("pressurized_natural", b -> b.require(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 1).output(Items.CLAY_BALL));

    GeneratedRecipe ULTRAWARM = create("ultrawarm", b -> b.require(CCBGases.ULTRAWARM_AIR.get(), 1).output(AllItems.CINDER_FLOUR));
    GeneratedRecipe PRESSURIZED_ULTRAWARM = create("pressurized_ultrawarm", b -> b.require(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 1).output(Items.CLAY_BALL));

    GeneratedRecipe ETHEREAL = create("ethereal", b -> b.require(CCBGases.ETHEREAL_AIR.get(), 1).output(CCBItems.CHORUS_FLOWER_POWDER));
    GeneratedRecipe PRESSURIZED_ETHEREAL = create("pressurized_ethereal", b -> b.require(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), 1).output(CCBItems.CHORUS_FLOWER_POWDER));

    GeneratedRecipe MOIST = create("moist", b -> b.require(CCBGases.MOIST_AIR.get(), 1).output(new FluidStack(Fluids.WATER, 1000)));

    public CCBResidueGenerationRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
