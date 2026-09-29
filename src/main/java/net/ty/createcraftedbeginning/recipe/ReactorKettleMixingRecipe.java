package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureMatching;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ReactorKettleMixingRecipe extends ReactorKettleRecipe {
    private final BasinRecipe source;
    private final boolean brewing;

    private ReactorKettleMixingRecipe(ReactorKettleRecipeParams params, BasinRecipe source, boolean brewing) {
        super(params);
        this.source = source;
        this.brewing = brewing;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(RecipeInput input) {
        return source.getRemainingItems(input);
    }

    @Override
    public List<ItemStack> rollResults(RandomSource random) {
        return source.rollResults(random);
    }

    public boolean isBrewing() {
        return brewing;
    }

    public static boolean isSupported(RecipeHolder<?> holder) {
        Recipe<?> recipe = holder.value();
        Class<?> recipeClass = recipe.getClass();
        if (recipeClass != MixingRecipe.class && recipeClass != ChilledMixingRecipe.class) {
            return false;
        }

        return recipe.getType() == AllRecipeTypes.MIXING.getType() && !AllRecipeTypes.shouldIgnoreInAutomation(holder);
    }

    public static RecipeHolder<ReactorKettleRecipe> convert(RecipeHolder<?> holder) {
        return convert(holder, false);
    }

    public static RecipeHolder<ReactorKettleRecipe> convertBrewing(RecipeHolder<MixingRecipe> holder) {
        return convert(holder, true);
    }

    private static RecipeHolder<ReactorKettleRecipe> convert(RecipeHolder<?> holder, boolean brewing) {
        if (!isSupported(holder)) {
            throw new IllegalArgumentException("Recipe '" + holder.id() + "' must be an eligible basin mixing recipe.");
        }

        BasinRecipe source = (BasinRecipe) holder.value();
        TemperatureCondition condition = switch (source.getRequiredHeat()) {
            case NONE -> TemperatureCondition.NONE;
            case HEATED -> TemperatureCondition.HEATED;
            case SUPERHEATED -> TemperatureCondition.SUPERHEATED;
        };
        if (source instanceof ChilledMixingRecipe) {
            condition = TemperatureCondition.CHILLED;
        }

        TemperatureMatching matching = condition.supportsCompatibleMatching() ? TemperatureMatching.COMPATIBLE : TemperatureMatching.EXACT;
        ReactorKettleRecipe converted = new Builder<>(params -> new ReactorKettleMixingRecipe(params, source, brewing), holder.id()).withItemIngredients(source.getIngredients().toArray(Ingredient[]::new)).withItemOutputs(source.getRollableResults().stream().map(output -> new ProcessingOutput(output.getStack().copy(), output.getChance())).toArray(ProcessingOutput[]::new)).withFluidIngredients(source.getFluidIngredients().toArray(SizedFluidIngredient[]::new)).withFluidOutputs(source.getFluidResults().stream().map(FluidStack::copy).toArray(FluidStack[]::new)).temperatureCondition(condition).temperatureMatching(matching).duration(0).build();
        return new RecipeHolder<>(holder.id(), converted);
    }
}
