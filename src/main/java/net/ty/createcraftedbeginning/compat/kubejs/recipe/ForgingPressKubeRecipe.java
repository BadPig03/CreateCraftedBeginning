package net.ty.createcraftedbeginning.compat.kubejs.recipe;

import com.mojang.datafixers.util.Either;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext.Impl;
import dev.latvian.mods.kubejs.recipe.component.ComponentValueMap;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeConstructor;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaType;
import dev.latvian.mods.kubejs.util.IntBounds;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Wrapper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ForgingPressKubeRecipe extends MachineKubeRecipe {
    private static final int PRESS_HEAD_SLOT = 1;
    private static final RecipeComponent<Either<Ingredient, SizedFluidIngredient>> INPUT = IngredientComponent.INGREDIENT.instance().or(CCBRecipeComponents.FLUID_INPUT.instance());
    private static final RecipeKey<List<Either<Ingredient, SizedFluidIngredient>>> INPUTS = IngredientComponent.OPTIONAL_INGREDIENT.instance().or(CCBRecipeComponents.FLUID_INPUT.instance()).asList().withBounds(IntBounds.OPTIONAL).inputKey("ingredients").noFunctions();
    private static final RecipeKey<List<GasRecipeRequirement>> GAS_INPUTS = CCBRecipeComponents.GAS_INPUT.instance().asList().withBounds(IntBounds.OPTIONAL).inputKey("gas_ingredients").optional(List.of()).noFunctions();
    private static final RecipeKey<List<Either<Either<Ingredient, SizedFluidIngredient>, GasRecipeRequirement>>> MIXED_INPUTS = INPUT.or(CCBRecipeComponents.GAS_INPUT.instance()).asList().inputKey("inputs");

    static RecipeSchema schema() {
        return new RecipeSchema(CCBRecipeSchemas.ITEM_OUTPUTS, INPUTS, GAS_INPUTS).factory(new KubeRecipeFactory(CCBAPI.asResource("forging_press"), ForgingPressKubeRecipe.class, ForgingPressKubeRecipe::new)).constructor(new RecipeConstructor(CCBRecipeSchemas.ITEM_OUTPUTS, MIXED_INPUTS) {
            @Override
            public void setValues(RecipeScriptContext context, RecipeSchemaType schemaType, ComponentValueMap values) {
                Object from = values.get(MIXED_INPUTS);
                if (!(from instanceof Iterable<?> mixedInputs)) {
                    throw new IllegalArgumentException("Forging press inputs must be an array containing a base item and at most one additional item, fluid or gas ingredient; got " + from + '.');
                }

                List<Object> ingredients = new ArrayList<>();
                for (Object input : mixedInputs) {
                    ingredients.add(Wrapper.unwrapped(input));
                }

                int inputCount = ingredients.size();
                if (inputCount < 1 || inputCount > 2) {
                    throw new IllegalArgumentException("Forging press inputs must contain one or two ingredients; got " + inputCount + '.');
                }

                Object first = ingredients.getFirst();
                if (first instanceof GasRecipeValue || first instanceof GasRecipeRequirement) {
                    throw new IllegalArgumentException("Forging press base must be a non-empty item ingredient; got a gas ingredient.");
                }

                Either<Ingredient, SizedFluidIngredient> base = INPUT.wrap(context, first);
                Optional<Ingredient> baseItem = base.left();
                if (baseItem.isEmpty() || baseItem.orElseThrow().isEmpty()) {
                    throw new IllegalArgumentException("Forging press base must be a non-empty item ingredient; got " + first + '.');
                }

                List<Either<Ingredient, SizedFluidIngredient>> inputs = new ArrayList<>();
                List<GasRecipeRequirement> gases = new ArrayList<>();
                inputs.add(base);
                if (inputCount == 2) {
                    Object second = ingredients.get(1);
                    if (second instanceof GasRecipeValue || second instanceof GasRecipeRequirement) {
                        gases.add(CCBRecipeComponents.GAS_INPUT.instance().wrap(context, second));
                    }
                    else {
                        Either<Ingredient, SizedFluidIngredient> addition = INPUT.wrap(context, second);
                        Optional<Ingredient> additionItem = addition.left();
                        if (additionItem.isPresent()) {
                            if (additionItem.orElseThrow().isEmpty()) {
                                throw new IllegalArgumentException("Forging press additional item ingredient must be non-empty; got an empty ingredient. Omit it to use only the base.");
                            }

                            inputs.add(Either.left(Ingredient.EMPTY));
                        }

                        inputs.add(addition);
                    }
                }

                KubeRecipe recipe = context.recipe();
                recipe.setValue(CCBRecipeSchemas.ITEM_OUTPUTS, values.getValue(context, CCBRecipeSchemas.ITEM_OUTPUTS));
                recipe.setValue(INPUTS, inputs);
                recipe.setValue(GAS_INPUTS, gases);
            }
        });
    }

    public ForgingPressKubeRecipe pressHead(Context context, Object from) {
        Ingredient ingredient = IngredientComponent.OPTIONAL_INGREDIENT.instance().wrap(new Impl(context, this), Wrapper.unwrapped(from));
        List<Either<Ingredient, SizedFluidIngredient>> inputs = new ArrayList<>(getValue(INPUTS));
        int itemSlot = 0;
        for (int index = 0; index < inputs.size(); index++) {
            if (inputs.get(index).left().isEmpty()) {
                continue;
            }

            if (itemSlot++ != PRESS_HEAD_SLOT) {
                continue;
            }

            inputs.set(index, Either.left(ingredient));
            setValue(INPUTS, inputs);
            return this;
        }

        while (itemSlot < PRESS_HEAD_SLOT) {
            inputs.add(Either.left(Ingredient.EMPTY));
            itemSlot++;
        }

        inputs.add(Either.left(ingredient));
        setValue(INPUTS, inputs);
        return this;
    }
}
