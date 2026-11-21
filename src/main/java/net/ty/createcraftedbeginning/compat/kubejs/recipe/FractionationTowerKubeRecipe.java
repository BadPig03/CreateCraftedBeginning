package net.ty.createcraftedbeginning.compat.kubejs.recipe;

import com.mojang.datafixers.util.Either;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext.Impl;
import dev.latvian.mods.kubejs.recipe.component.ComponentValueMap;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeConstructor;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaType;
import dev.latvian.mods.kubejs.util.IntBounds;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Wrapper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.recipe.FractionationTowerOutput;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class FractionationTowerKubeRecipe extends MachineKubeRecipe {
    private static final int DEFAULT_PROCESSING_TIME = 200;
    private static final RecipeKey<List<Either<Either<Ingredient, SizedFluidIngredient>, GasRecipeRequirement>>> MIXED_INPUTS = CCBRecipeSchemas.ITEM_OR_FLUID_INPUT.or(CCBRecipeComponents.GAS_INPUT.instance()).asList().inputKey("inputs");
    private static final RecipeKey<List<FractionationTowerOutput>> LAYER_OUTPUTS = CCBRecipeComponents.LAYER_OUTPUT.instance().asList().withBounds(IntBounds.OPTIONAL).outputKey("layer_outputs").optional(List.of()).alwaysWrite().functionNames("layerOutputs");
    private static final RecipeKey<Integer> DURATION = NumberComponent.intRange(1, Integer.MAX_VALUE).otherKey("processing_time").optional(DEFAULT_PROCESSING_TIME).alwaysWrite().noFunctions();

    static RecipeSchema schema() {
        return new RecipeSchema(CCBRecipeSchemas.INPUTS, CCBRecipeSchemas.EMPTY_OUTPUTS, CCBRecipeSchemas.GAS_INPUTS, LAYER_OUTPUTS, DURATION, CCBRecipeSchemas.TEMPERATURE, CCBRecipeSchemas.TEMPERATURE_MATCHING).factory(new KubeRecipeFactory(CCBAPI.asResource("fractionation"), FractionationTowerKubeRecipe.class, FractionationTowerKubeRecipe::new)).constructor(new RecipeConstructor(MIXED_INPUTS) {
            @Override
            public void setValues(RecipeScriptContext context, RecipeSchemaType schemaType, ComponentValueMap values) {
                Object from = values.get(MIXED_INPUTS);
                if (from == null) {
                    throw new IllegalArgumentException("Fractionation tower inputs must be an array or a single item or fluid ingredient; got null.");
                }

                KubeRecipe recipe = context.recipe();
                if (!(from instanceof Iterable<?> mixedInputs)) {
                    recipe.setValue(CCBRecipeSchemas.INPUTS, CCBRecipeSchemas.INPUTS.component.wrap(context, from));
                    return;
                }

                List<Either<Ingredient, SizedFluidIngredient>> inputs = new ArrayList<>();
                List<GasRecipeRequirement> gasInputs = new ArrayList<>();
                for (Object input : mixedInputs) {
                    input = Wrapper.unwrapped(input);
                    if (input instanceof GasRecipeValue || input instanceof GasRecipeRequirement) {
                        gasInputs.add(CCBRecipeComponents.GAS_INPUT.instance().wrap(context, input));
                        continue;
                    }

                    inputs.add(CCBRecipeSchemas.ITEM_OR_FLUID_INPUT.wrap(context, input));
                }

                recipe.setValue(CCBRecipeSchemas.INPUTS, inputs);
                recipe.setValue(CCBRecipeSchemas.GAS_INPUTS, gasInputs);
            }
        });
    }

    private static int requirePositiveInteger(@Nullable Object from, String parameter, int max) {
        Object value = Wrapper.unwrapped(from);
        if (value instanceof Number number) {
            double integer = number.doubleValue();
            if (integer >= 1 && integer <= max && integer % 1 == 0) {
                return (int) integer;
            }
        }

        Object argument = value instanceof CharSequence ? "'" + value + '\'' : value;
        throw new IllegalArgumentException(parameter + " must be an integer in [1, " + max + "]; got " + argument + '.');
    }

    public FractionationTowerKubeRecipe outputAt(Context cx, @Nullable Object offset, @Nullable Object product) {
        try {
            int layer = requirePositiveInteger(offset, "Fractionation tower layer offset", FractionationTowerOutput.MAX_LAYER_OFFSET);
            product = Wrapper.unwrapped(product);
            FractionationTowerOutput output = switch (product) {
                case GasRecipeValue gas -> new FractionationTowerOutput(layer, ItemStack.EMPTY, FluidStack.EMPTY, gas.asStack());
                case GasStack gas -> new FractionationTowerOutput(layer, ItemStack.EMPTY, FluidStack.EMPTY, gas);
                case FluidStack fluid -> new FractionationTowerOutput(layer, ItemStack.EMPTY, fluid, GasStack.EMPTY);
                case null, default -> {
                    if (product instanceof ItemRecipeOutput(ProcessingOutput itemOutput)) {
                        float chance = itemOutput.getChance();
                        if (chance != 1) {
                            throw new IllegalArgumentException("Fractionation tower item output chance must be 1; got " + chance + " for layer offset " + layer + '.');
                        }

                        product = itemOutput.getStack();
                    }

                    ItemStack item = ItemStackComponent.ITEM_STACK.instance().wrap(new Impl(cx, this), product);
                    yield new FractionationTowerOutput(layer, item, FluidStack.EMPTY, GasStack.EMPTY);
                }
            };
            if (!output.isValid()) {
                throw new IllegalArgumentException("Fractionation tower output at layer offset " + layer + " must contain one non-empty product; got " + product + '.');
            }

            List<FractionationTowerOutput> current = getValue(LAYER_OUTPUTS);
            List<FractionationTowerOutput> outputs = current == null ? new ArrayList<>() : new ArrayList<>(current);
            for (FractionationTowerOutput existing : outputs) {
                if (existing.layer() == layer) {
                    throw new IllegalArgumentException("Fractionation tower output offsets must be unique; got duplicate offset " + layer + '.');
                }
            }

            outputs.add(output);
            setValue(LAYER_OUTPUTS, outputs);
            return this;
        }
        catch (RuntimeException exception) {
            if (newRecipe) {
                remove();
            }

            throw exception;
        }
    }

    public FractionationTowerKubeRecipe processingTime() {
        return processingTime(null);
    }

    public FractionationTowerKubeRecipe processingTime(@Nullable Object ticks) {
        try {
            setValue(DURATION, requirePositiveInteger(ticks, "Fractionation tower processing time in ticks", Integer.MAX_VALUE));
            return this;
        }
        catch (RuntimeException exception) {
            if (newRecipe) {
                remove();
            }

            throw exception;
        }
    }
}
