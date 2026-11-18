package net.ty.createcraftedbeginning.compat.kubejs.recipe;

import com.mojang.datafixers.util.Either;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.ComponentValueMap;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeConstructor;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaType;
import dev.latvian.mods.kubejs.util.IntBounds;
import dev.latvian.mods.rhino.Wrapper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionKubeRecipe extends MachineKubeRecipe {
    private static final RecipeComponent<Either<Ingredient, SizedFluidIngredient>> INPUT = IngredientComponent.INGREDIENT.instance().or(CCBRecipeComponents.FLUID_INPUT.instance());
    private static final RecipeKey<List<Either<Ingredient, SizedFluidIngredient>>> INPUTS = INPUT.asList().withBounds(IntBounds.OPTIONAL).inputKey("ingredients").noFunctions();
    private static final RecipeKey<List<GasRecipeRequirement>> GAS_INPUTS = CCBRecipeComponents.GAS_INPUT.instance().asList().withBounds(IntBounds.OPTIONAL).inputKey("gas_ingredients").optional(List.of()).noFunctions();
    private static final RecipeKey<List<Either<Either<Ingredient, SizedFluidIngredient>, GasRecipeRequirement>>> MIXED_INPUTS = INPUT.or(CCBRecipeComponents.GAS_INPUT.instance()).asList().inputKey("inputs");

    static RecipeSchema schema() {
        return new RecipeSchema(CCBRecipeSchemas.OUTPUTS, INPUTS, GAS_INPUTS).factory(new KubeRecipeFactory(CCBAPI.asResource("gas_injection"), GasInjectionKubeRecipe.class, GasInjectionKubeRecipe::new)).constructor(new RecipeConstructor(CCBRecipeSchemas.OUTPUTS, MIXED_INPUTS) {
            @Override
            public void setValues(RecipeScriptContext context, RecipeSchemaType schemaType, ComponentValueMap values) {
                Object from = values.get(MIXED_INPUTS);
                if (!(from instanceof Iterable<?> mixedInputs)) {
                    throw new IllegalArgumentException("Gas injection inputs must be an array containing one item or fluid ingredient and one CCBGas value; got " + from + '.');
                }

                List<Either<Ingredient, SizedFluidIngredient>> inputs = new ArrayList<>();
                List<GasRecipeRequirement> gases = new ArrayList<>();
                for (Object input : mixedInputs) {
                    input = Wrapper.unwrapped(input);
                    if (input instanceof GasRecipeValue || input instanceof GasRecipeRequirement) {
                        gases.add(CCBRecipeComponents.GAS_INPUT.instance().wrap(context, input));
                        continue;
                    }

                    inputs.add(INPUT.wrap(context, input));
                }

                if (inputs.size() != 1 || gases.size() != 1) {
                    throw new IllegalArgumentException("Gas injection inputs must contain one item or fluid ingredient and one gas ingredient; got ordinaryInputs=" + inputs.size() + ", gasInputs=" + gases.size() + '.');
                }

                KubeRecipe recipe = context.recipe();
                recipe.setValue(CCBRecipeSchemas.OUTPUTS, values.getValue(context, CCBRecipeSchemas.OUTPUTS));
                recipe.setValue(INPUTS, inputs);
                recipe.setValue(GAS_INPUTS, gases);
            }
        });
    }
}
