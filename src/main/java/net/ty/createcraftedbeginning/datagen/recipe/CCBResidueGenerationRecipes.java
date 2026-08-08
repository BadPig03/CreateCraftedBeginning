package net.ty.createcraftedbeginning.datagen.recipe;

import com.simibubi.create.AllItems;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.datagen.recipe.generator.ResidueGenerationGen;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBResidueGenerationRecipes extends ResidueGenerationGen {
    GeneratedRecipe NATURAL = create("natural", b -> b.require(CCBGases.NATURAL_AIR.get(), 1).output(Items.CLAY_BALL));
    GeneratedRecipe PRESSURIZED_NATURAL = create("pressurized_natural", builder -> builder.require(CCBGases.PRESSURIZED_NATURAL_AIR.get(), 1).output(Items.CLAY_BALL));

    GeneratedRecipe ULTRAWARM = create("ultrawarm", b -> b.require(CCBGases.ULTRAWARM_AIR.get(), 1).output(AllItems.CINDER_FLOUR));
    GeneratedRecipe PRESSURIZED_ULTRAWARM = create("pressurized_ultrawarm", builder -> builder.require(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), 1).output(AllItems.CINDER_FLOUR));

    GeneratedRecipe ETHEREAL = create("ethereal", b -> b.require(CCBGases.ETHEREAL_AIR.get(), 1).output(CCBItems.CHORUS_FLOWER_POWDER));
    GeneratedRecipe PRESSURIZED_ETHEREAL = create("pressurized_ethereal", builder -> builder.require(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), 1).output(CCBItems.CHORUS_FLOWER_POWDER));

    GeneratedRecipe MOIST = create("moist", b -> b.require(CCBGases.MOIST_AIR.get(), 1).output(new FluidStack(Fluids.WATER, 1000)));
    GeneratedRecipe SPORE = create("spore", b -> b.require(CCBGases.SPORE_AIR.get(), 1).output(Items.MUSHROOM_STEM));
    GeneratedRecipe SCULK = create("sculk", b -> b.require(CCBGases.SCULK_AIR.get(), 1).output(Items.SCULK_VEIN));

    public CCBResidueGenerationRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CCBAPI.MOD_ID);
    }
}
