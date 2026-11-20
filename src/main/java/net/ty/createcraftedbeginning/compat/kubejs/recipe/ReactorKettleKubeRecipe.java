package net.ty.createcraftedbeginning.compat.kubejs.recipe;

import com.mojang.datafixers.util.Either;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.ComponentValueMap;
import dev.latvian.mods.kubejs.recipe.component.FluidStackComponent;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeConstructor;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaType;
import dev.latvian.mods.kubejs.util.IntBounds;
import dev.latvian.mods.rhino.Wrapper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ReactorKettleKubeRecipe extends MachineKubeRecipe {
    private static final RecipeComponent<Either<Ingredient, SizedFluidIngredient>> INPUT = IngredientComponent.INGREDIENT.instance().or(CCBRecipeComponents.FLUID_INPUT.instance());
    private static final RecipeComponent<Either<FluidStack, ProcessingOutput>> OUTPUT = FluidStackComponent.FLUID_STACK.instance().or(CCBRecipeComponents.ITEM_OUTPUT.instance());
    private static final RecipeKey<List<Either<Ingredient, SizedFluidIngredient>>> INPUTS = INPUT.asList().withBounds(IntBounds.OPTIONAL).inputKey("ingredients").noFunctions();
    private static final RecipeKey<List<Either<FluidStack, ProcessingOutput>>> OUTPUTS = OUTPUT.asList().withBounds(IntBounds.OPTIONAL).outputKey("results").optional(List.of()).alwaysWrite().noFunctions();
    private static final RecipeKey<List<GasRecipeRequirement>> GAS_INPUTS = CCBRecipeComponents.GAS_INPUT.instance().asList().withBounds(IntBounds.OPTIONAL).inputKey("gas_ingredients").optional(List.of()).noFunctions();
    private static final RecipeKey<List<GasStack>> GAS_OUTPUTS = CCBRecipeComponents.GAS_OUTPUT.instance().asList().withBounds(IntBounds.OPTIONAL).outputKey("gas_results").optional(List.of()).noFunctions();
    private static final RecipeKey<Integer> DURATION = NumberComponent.intRange(0, Integer.MAX_VALUE).otherKey("processing_time").optional(0).noFunctions();
    private static final RecipeKey<List<Either<Either<Ingredient, SizedFluidIngredient>, GasRecipeRequirement>>> MIXED_INPUTS = INPUT.or(CCBRecipeComponents.GAS_INPUT.instance()).asList().inputKey("inputs");
    private static final RecipeKey<List<Either<Either<FluidStack, ProcessingOutput>, GasStack>>> MIXED_OUTPUTS = OUTPUT.or(CCBRecipeComponents.GAS_OUTPUT.instance()).asList().outputKey("outputs");

    static RecipeSchema schema() {
        return new RecipeSchema(OUTPUTS, INPUTS, GAS_INPUTS, GAS_OUTPUTS, DURATION, CCBRecipeSchemas.TEMPERATURE, CCBRecipeSchemas.TEMPERATURE_MATCHING).factory(new KubeRecipeFactory(CCBAPI.asResource("reactor_kettle"), ReactorKettleKubeRecipe.class, ReactorKettleKubeRecipe::new)).constructor(new RecipeConstructor(MIXED_OUTPUTS, MIXED_INPUTS) {
            @Override
            public void setValues(RecipeScriptContext context, RecipeSchemaType schemaType, ComponentValueMap values) {
                Iterable<?> mixedOutputs = requireArray(values.get(MIXED_OUTPUTS), "outputs");
                Iterable<?> mixedInputs = requireArray(values.get(MIXED_INPUTS), "inputs");

                List<Either<FluidStack, ProcessingOutput>> outputs = new ArrayList<>();
                List<GasStack> gasOutputs = new ArrayList<>();
                for (Object output : mixedOutputs) {
                    output = Wrapper.unwrapped(output);
                    if (output instanceof GasRecipeValue || output instanceof GasStack) {
                        gasOutputs.add(CCBRecipeComponents.GAS_OUTPUT.instance().wrap(context, output));
                        continue;
                    }

                    outputs.add(OUTPUT.wrap(context, output));
                }

                List<Either<Ingredient, SizedFluidIngredient>> inputs = new ArrayList<>();
                List<GasRecipeRequirement> gasInputs = new ArrayList<>();
                for (Object input : mixedInputs) {
                    input = Wrapper.unwrapped(input);
                    if (input instanceof GasRecipeValue || input instanceof GasRecipeRequirement) {
                        gasInputs.add(CCBRecipeComponents.GAS_INPUT.instance().wrap(context, input));
                        continue;
                    }

                    inputs.add(INPUT.wrap(context, input));
                }

                KubeRecipe recipe = context.recipe();
                recipe.setValue(OUTPUTS, outputs);
                recipe.setValue(INPUTS, inputs);
                recipe.setValue(GAS_INPUTS, gasInputs);
                recipe.setValue(GAS_OUTPUTS, gasOutputs);
            }
        });
    }

    private static Iterable<?> requireArray(@Nullable Object from, String side) {
        if (!(from instanceof Iterable<?> entries) || !entries.iterator().hasNext()) {
            throw new IllegalArgumentException("Reactor kettle " + side + " must be a non-empty array of item, fluid or gas entries; got " + from + '.');
        }

        return entries;
    }

    public ReactorKettleKubeRecipe processingTime() {
        return processingTime(null);
    }

    public ReactorKettleKubeRecipe processingTime(@Nullable Object from) {
        Object value = Wrapper.unwrapped(from);
        double ticks = value instanceof Number number ? number.doubleValue() : Double.NaN;
        if (ticks < 1 || ticks > Integer.MAX_VALUE || ticks % 1 != 0) {
            if (newRecipe) {
                remove();
            }

            Object argument = value instanceof CharSequence ? "'" + value + '\'' : value;
            throw new IllegalArgumentException("Reactor kettle processing time must be a positive integer in [1, " + Integer.MAX_VALUE + "] ticks; got " + argument + '.');
        }

        setValue(DURATION, (int) ticks);
        return this;
    }
}
