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
    private final GeneratedRecipe NATURAL = create("natural", builder -> builder.require(CCBGases.NATURAL_AIR.get(), 1).output(Items.CLAY_BALL));
    private final GeneratedRecipe ULTRAWARM = create("ultrawarm", builder -> builder.require(CCBGases.ULTRAWARM_AIR.get(), 1).output(AllItems.CINDER_FLOUR));
    private final GeneratedRecipe ETHEREAL = create("ethereal", builder -> builder.require(CCBGases.ETHEREAL_AIR.get(), 1).output(CCBItems.CHORUS_FLOWER_POWDER));
    private final GeneratedRecipe STEAM = create("steam", builder -> builder.require(CCBGases.STEAM.get(), 1));

    private final GeneratedRecipe MOIST = create("moist", builder -> builder.require(CCBGases.MOIST_AIR.get(), 1).output(new FluidStack(Fluids.WATER, 1000)));
    private final GeneratedRecipe SPORE = create("spore", builder -> builder.require(CCBGases.SPORE_AIR.get(), 1).output(Items.MUSHROOM_STEM));
    private final GeneratedRecipe SCULK = create("sculk", builder -> builder.require(CCBGases.SCULK_AIR.get(), 1).output(Items.SCULK_VEIN));

    public CCBResidueGenerationRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CCBAPI.MOD_ID);
    }
}
