package net.ty.createcraftedbeginning.compat.kubejs.recipe;

import com.google.gson.JsonObject;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext.Impl;
import dev.latvian.mods.kubejs.recipe.RecipeTypeFunction;
import dev.latvian.mods.kubejs.recipe.component.ComponentValueMap;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeConstructor;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaType;
import dev.latvian.mods.kubejs.script.SourceLine;
import dev.latvian.mods.kubejs.util.IntBounds;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Wrapper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe.WindChargingAction;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class WindChargingKubeRecipe extends MachineKubeRecipe {
    private static final long MAX_SUBTRACTED_WIND = -(long) Integer.MIN_VALUE;
    private static final RecipeKey<Ingredient> INPUT = IngredientComponent.INGREDIENT.instance().inputKey("input");
    private static final RecipeKey<List<ProcessingOutput>> RETURN_ITEMS = CCBRecipeComponents.ITEM_OUTPUT.instance().asList().withBounds(IntBounds.OPTIONAL).outputKey("results").optional(List.of()).alwaysWrite();
    private static final RecipeKey<Integer> DURATION = NumberComponent.intRange(Integer.MIN_VALUE, Integer.MAX_VALUE).otherKey("processing_time").optional(0).noFunctions();
    private static final RecipeKey<String> ACTION = StringComponent.STRING.instance().otherKey("action").optional(WindChargingAction.CHARGE.getSerializedName()).alwaysWrite().noFunctions();
    private static final RecipeKey<Integer> PRIORITY = NumberComponent.intRange(Integer.MIN_VALUE, Integer.MAX_VALUE).otherKey("priority").optional(0).noFunctions();

    private boolean requiresNamedEffect;
    private boolean effectSelected;

    static RecipeSchema schema() {
        return new RecipeSchema(RETURN_ITEMS, CCBRecipeSchemas.ITEM_INPUTS, DURATION, ACTION, PRIORITY).factory(new KubeRecipeFactory(CCBAPI.asResource("wind_charging"), WindChargingKubeRecipe.class, WindChargingKubeRecipe::new)).constructor(RETURN_ITEMS, CCBRecipeSchemas.ITEM_INPUTS).constructor(new RecipeConstructor(INPUT) {
            @Override
            public KubeRecipe create(Context cx, SourceLine sourceLine, RecipeTypeFunction type, RecipeSchemaType schemaType, ComponentValueMap values) {
                Object input = values.get(INPUT);
                if (input instanceof Map<?, ?> || input instanceof JsonObject) {
                    JsonObject json = JsonUtils.objectOf(cx, input);
                    if (json.has(CCBRecipeSchemas.ITEM_INPUTS.name)) {
                        return schemaType.schema.deserialize(sourceLine, type, null, json);
                    }
                }

                return super.create(cx, sourceLine, type, schemaType, values);
            }

            @Override
            public void setValues(RecipeScriptContext context, RecipeSchemaType schemaType, ComponentValueMap values) {
                Ingredient input = values.getValue(context, INPUT);
                WindChargingKubeRecipe recipe = (WindChargingKubeRecipe) context.recipe();
                recipe.setValue(CCBRecipeSchemas.ITEM_INPUTS, List.of(input));
                recipe.requiresNamedEffect = true;
            }
        });
    }

    @Override
    protected void validateScriptSettings() {
        if (requiresNamedEffect && !effectSelected) {
            throw new IllegalArgumentException("Wind charging recipe must select one effect with addWind, subtractWind, clearIll or cycleCreative.");
        }
    }

    public WindChargingKubeRecipe addWind(@Nullable Object ticks) {
        int duration = (int) requireInteger(ticks, "added wind in ticks", 1, Integer.MAX_VALUE);
        return selectEffect(WindChargingAction.CHARGE, duration);
    }

    public WindChargingKubeRecipe subtractWind(@Nullable Object ticks) {
        int duration = (int) -requireInteger(ticks, "subtracted wind in ticks", 1, MAX_SUBTRACTED_WIND);
        return selectEffect(WindChargingAction.CHARGE, duration);
    }

    public WindChargingKubeRecipe clearIll() {
        return selectEffect(WindChargingAction.CLEAR_ILL, 0);
    }

    public WindChargingKubeRecipe cycleCreative() {
        return selectEffect(WindChargingAction.CYCLE_CREATIVE, 0);
    }

    public WindChargingKubeRecipe returnItem(Context cx, @Nullable Object from) {
        Object value = Wrapper.unwrapped(from);
        try {
            ProcessingOutput output = CCBRecipeComponents.ITEM_OUTPUT.instance().wrap(new Impl(cx, this), value);
            ItemStack stack = output.getStack();
            float chance = output.getChance();
            if (stack.isEmpty() || chance != 1) {
                throw new IllegalArgumentException("Wind charging return item must be non-empty with chance 1; got stack=" + stack + ", chance=" + chance + '.');
            }

            setValue(RETURN_ITEMS, List.of(new ProcessingOutput(stack.copy(), 1)));
            return this;
        }
        catch (RuntimeException exception) {
            if (newRecipe) {
                remove();
            }

            throw exception;
        }
    }

    public WindChargingKubeRecipe priority(Context cx, @Nullable Object from) {
        if (!requiresNamedEffect) {
            setValue(PRIORITY, PRIORITY.component.wrap(new Impl(cx, this), Wrapper.unwrapped(from)));
            return this;
        }

        setValue(PRIORITY, (int) requireInteger(from, "priority", Integer.MIN_VALUE, Integer.MAX_VALUE));
        return this;
    }

    public WindChargingKubeRecipe action(Context cx, @Nullable Object from) {
        return setLegacyEffectField(cx, ACTION, from);
    }

    public WindChargingKubeRecipe processingTime(Context cx, @Nullable Object from) {
        return setLegacyEffectField(cx, DURATION, from);
    }

    private WindChargingKubeRecipe selectEffect(WindChargingAction action, int duration) {
        if (requiresNamedEffect && effectSelected) {
            throw invalidRecipe("Wind charging recipe must select only one effect; got action '" + action.getSerializedName() + "' after an effect was already selected.");
        }

        setValue(ACTION, action.getSerializedName());
        setValue(DURATION, duration);
        effectSelected = true;
        return this;
    }

    private <T> WindChargingKubeRecipe setLegacyEffectField(Context cx, RecipeKey<T> key, @Nullable Object from) {
        if (requiresNamedEffect) {
            throw invalidRecipe("Wind charging recipes with one input argument must use addWind, subtractWind, clearIll or cycleCreative; got field '" + key.name + "'.");
        }

        setValue(key, key.component.wrap(new Impl(cx, this), Wrapper.unwrapped(from)));
        return this;
    }

    private long requireInteger(@Nullable Object from, String parameter, long min, long max) {
        Object value = Wrapper.unwrapped(from);
        if (value instanceof Number number) {
            double integer = number.doubleValue();
            if (integer >= min && integer <= max && integer % 1 == 0) {
                return (long) integer;
            }
        }

        Object argument = value instanceof CharSequence ? "'" + value + '\'' : value;
        throw invalidRecipe("Wind charging " + parameter + " must be an integer in [" + min + ", " + max + "]; got " + argument + '.');
    }

    private IllegalArgumentException invalidRecipe(String message) {
        if (newRecipe) {
            remove();
        }

        return new IllegalArgumentException(message);
    }
}
