package net.ty.createcraftedbeginning.compat.kubejs.recipe;

import com.mojang.serialization.Codec;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.codec.CreateCodecs;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentTypeRegistry;
import dev.latvian.mods.kubejs.recipe.component.SimpleRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.SizedFluidIngredientComponent;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.recipe.FractionationTowerOutput;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureMatching;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBRecipeComponents {
    static final RecipeComponentType<ProcessingOutput> ITEM_OUTPUT = RecipeComponentType.unit(CCBAPI.asResource("processing_output"), ProcessingOutputComponent::new);
    static final RecipeComponentType<SizedFluidIngredient> FLUID_INPUT = RecipeComponentType.unit(CCBAPI.asResource("fluid_ingredient"), type -> new SizedFluidIngredientComponent(type, CreateCodecs.FLAT_SIZED_FLUID_INGREDIENT_WITH_TYPE, false));
    static final RecipeComponentType<GasRecipeRequirement> GAS_INPUT = RecipeComponentType.unit(CCBAPI.asResource("gas_ingredient"), type -> new SimpleRecipeComponent<>(type, GasRecipeRequirement.CODEC, TypeInfo.of(GasRecipeValue.class)) {
        @Override
        public GasRecipeRequirement wrap(RecipeScriptContext cx, Object from) {
            if (from instanceof GasRecipeValue gas) {
                return gas.asIngredient();
            }

            if (from instanceof GasRecipeRequirement requirement) {
                return requirement;
            }

            return codec().parse(cx.ops().json(), JsonUtils.of(cx.cx(), from)).getOrThrow(error -> new IllegalStateException("Failed to decode gas recipe ingredient: " + error));
        }
    });
    static final RecipeComponentType<GasStack> GAS_OUTPUT = RecipeComponentType.unit(CCBAPI.asResource("gas_output"), type -> new SimpleRecipeComponent<>(type, GasStack.CODEC, TypeInfo.of(GasRecipeValue.class)) {
        @Override
        public GasStack wrap(RecipeScriptContext cx, Object from) {
            if (from instanceof GasRecipeValue gas) {
                return gas.asStack();
            }

            if (from instanceof GasStack gas) {
                return gas.copy();
            }

            return codec().parse(cx.ops().json(), JsonUtils.of(cx.cx(), from)).getOrThrow(error -> new IllegalStateException("Failed to decode gas recipe output: " + error));
        }
    });
    static final RecipeComponentType<FractionationTowerOutput> LAYER_OUTPUT = createJsonComponentType("layer_output", FractionationTowerOutput.CODEC, FractionationTowerOutput.class);
    static final RecipeComponentType<TemperatureCondition> TEMPERATURE = createJsonComponentType("temperature", TemperatureCondition.CODEC, TemperatureCondition.class);
    static final RecipeComponentType<TemperatureMatching> TEMPERATURE_MATCHING = createJsonComponentType("temperature_matching", TemperatureMatching.CODEC, TemperatureMatching.class);

    private CCBRecipeComponents() {
    }

    public static void register(RecipeComponentTypeRegistry registry) {
        registry.register(ITEM_OUTPUT);
        registry.register(FLUID_INPUT);
        registry.register(GAS_INPUT);
        registry.register(GAS_OUTPUT);
        registry.register(LAYER_OUTPUT);
        registry.register(TEMPERATURE);
        registry.register(TEMPERATURE_MATCHING);
    }

    private static <T> RecipeComponentType<T> createJsonComponentType(String id, Codec<T> codec, Class<T> valueClass) {
        return RecipeComponentType.unit(CCBAPI.asResource(id), type -> new SimpleRecipeComponent<>(type, codec, TypeInfo.of(valueClass)) {
            @Override
            public T wrap(RecipeScriptContext cx, Object from) {
                if (valueClass.isInstance(from)) {
                    return valueClass.cast(from);
                }

                return codec.parse(cx.ops().json(), JsonUtils.of(cx.cx(), from)).getOrThrow(error -> new IllegalStateException("Failed to decode recipe component '" + id + "': " + error));
            }
        });
    }
}
