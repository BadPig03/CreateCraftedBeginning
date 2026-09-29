package net.ty.createcraftedbeginning.compat.kubejs.recipe;

import com.mojang.datafixers.util.Either;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.FluidStackComponent;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import dev.latvian.mods.kubejs.util.IntBounds;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import net.ty.createcraftedbeginning.recipe.FractionationTowerOutput;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureMatching;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBRecipeSchemas {
    private static final int DEFAULT_PROCESSING_TIME = 200;
    private static final RecipeKey<List<GasRecipeRequirement>> GAS_INPUTS = CCBRecipeComponents.GAS_INPUT.instance().asList().withBounds(IntBounds.OPTIONAL).inputKey("gas_ingredients").optional(List.of()).functionNames("gasInputs");
    static final RecipeKey<List<FractionationTowerOutput>> LAYER_OUTPUTS = CCBRecipeComponents.LAYER_OUTPUT.instance().asList().withBounds(IntBounds.OPTIONAL).outputKey("layer_outputs").optional(List.of()).alwaysWrite().functionNames("layerOutputs");
    private static final RecipeKey<List<Ingredient>> ITEM_INPUTS = IngredientComponent.INGREDIENT.instance().asList().inputKey("ingredients");
    private static final RecipeKey<List<ProcessingOutput>> ITEM_OUTPUTS = CCBRecipeComponents.ITEM_OUTPUT.instance().asList().outputKey("results");
    private static final RecipeKey<List<Either<SizedFluidIngredient, Ingredient>>> INPUTS = CCBRecipeComponents.FLUID_INPUT.instance().or(IngredientComponent.INGREDIENT.instance()).asList().withBounds(IntBounds.OPTIONAL).inputKey("ingredients");
    private static final RecipeKey<List<Either<SizedFluidIngredient, Ingredient>>> FORGING_INPUTS = CCBRecipeComponents.FLUID_INPUT.instance().or(IngredientComponent.OPTIONAL_INGREDIENT.instance()).asList().withBounds(IntBounds.OPTIONAL).inputKey("ingredients");
    private static final RecipeKey<List<Either<FluidStack, ProcessingOutput>>> OUTPUTS = FluidStackComponent.FLUID_STACK.instance().or(CCBRecipeComponents.ITEM_OUTPUT.instance()).asList().outputKey("results");
    private static final RecipeKey<List<ProcessingOutput>> EMPTY_OUTPUTS = CCBRecipeComponents.ITEM_OUTPUT.instance().asList().withBounds(IntBounds.OPTIONAL).outputKey("results").optional(List.of()).alwaysWrite().noFunctions();
    private static final RecipeKey<Integer> DURATION = NumberComponent.intRange(1, Integer.MAX_VALUE).otherKey("processing_time").optional(DEFAULT_PROCESSING_TIME).alwaysWrite().functionNames("processingTime");
    private static final RecipeKey<TemperatureCondition> TEMPERATURE = CCBRecipeComponents.TEMPERATURE.otherKey("temperature").optional(TemperatureCondition.NONE);
    private static final RecipeKey<TemperatureMatching> TEMPERATURE_MATCHING = CCBRecipeComponents.TEMPERATURE_MATCHING.otherKey("temperature_matching").optional(TemperatureMatching.EXACT);
    private static final RecipeKey<List<Ingredient>> EMPTY_INPUTS = IngredientComponent.INGREDIENT.instance().asList().withBounds(IntBounds.OPTIONAL).inputKey("ingredients").optional(List.of()).alwaysWrite().noFunctions();
    private static final RecipeKey<List<ProcessingOutput>> OPTIONAL_ITEM_OUTPUTS = CCBRecipeComponents.ITEM_OUTPUT.instance().asList().withBounds(IntBounds.OPTIONAL).outputKey("results").optional(List.of()).alwaysWrite();
    private static final RecipeKey<List<Either<FluidStack, ProcessingOutput>>> OPTIONAL_OUTPUTS = FluidStackComponent.FLUID_STACK.instance().or(CCBRecipeComponents.ITEM_OUTPUT.instance()).asList().withBounds(IntBounds.OPTIONAL).outputKey("results").optional(List.of()).alwaysWrite();
    private static final RecipeKey<List<GasStack>> GAS_OUTPUTS = CCBRecipeComponents.GAS_OUTPUT.instance().asList().withBounds(IntBounds.OPTIONAL).outputKey("gas_results").optional(List.of()).functionNames("gasOutputs");
    private static final RecipeKey<Integer> COOLING_DURATION = NumberComponent.intRange(1, Integer.MAX_VALUE).otherKey("processing_time").functionNames("processingTime");
    private static final RecipeKey<Integer> OPTIONAL_DURATION = NumberComponent.intRange(0, Integer.MAX_VALUE).otherKey("processing_time").optional(0).functionNames("processingTime");
    private static final RecipeKey<Integer> WIND_CHARGE_DURATION = NumberComponent.intRange(Integer.MIN_VALUE, Integer.MAX_VALUE).otherKey("processing_time").optional(0).functionNames("processingTime");
    private static final RecipeKey<String> WIND_CHARGE_ACTION = StringComponent.STRING.instance().otherKey("action").optional("charge").alwaysWrite();
    private static final RecipeKey<Integer> PRIORITY = NumberComponent.intRange(Integer.MIN_VALUE, Integer.MAX_VALUE).otherKey("priority").optional(0);
    private static final KubeRecipeFactory MACHINE = new KubeRecipeFactory(CCBAPI.asResource("machine"), MachineKubeRecipe.class, MachineKubeRecipe::new);
    private static final KubeRecipeFactory FRACTIONATION = new KubeRecipeFactory(CCBAPI.asResource("fractionation"), FractionationTowerKubeRecipe.class, FractionationTowerKubeRecipe::new);

    private CCBRecipeSchemas() {
    }

    public static void register(RecipeSchemaRegistry registry) {
        registry.register(CCBRecipeTypes.CHILLING.getId(), new RecipeSchema(ITEM_OUTPUTS, ITEM_INPUTS).factory(MACHINE).constructor(ITEM_OUTPUTS, ITEM_INPUTS));
        registry.register(CCBRecipeTypes.GAS_INJECTION.getId(), new RecipeSchema(OUTPUTS, INPUTS, GAS_INPUTS).factory(MACHINE).constructor(OUTPUTS, INPUTS).constructor(OUTPUTS, INPUTS, GAS_INPUTS));
        registry.register(CCBRecipeTypes.FORGING_PRESS.getId(), new RecipeSchema(ITEM_OUTPUTS, FORGING_INPUTS, GAS_INPUTS).factory(MACHINE).constructor(ITEM_OUTPUTS, FORGING_INPUTS).constructor(ITEM_OUTPUTS, FORGING_INPUTS, GAS_INPUTS));
        registry.register(CCBRecipeTypes.CHILLED_COMPACTING.getId(), new RecipeSchema(OUTPUTS, INPUTS).factory(MACHINE).constructor(OUTPUTS, INPUTS));
        registry.register(CCBRecipeTypes.CHILLED_MIXING.getId(), new RecipeSchema(OUTPUTS, INPUTS, OPTIONAL_DURATION).factory(MACHINE).constructor(OUTPUTS, INPUTS));
        registry.register(CCBRecipeTypes.COOLING.getId(), new RecipeSchema(INPUTS, COOLING_DURATION, EMPTY_OUTPUTS).factory(MACHINE).constructor(INPUTS, COOLING_DURATION));
        registry.register(CCBRecipeTypes.DISSIPATION.getId(), new RecipeSchema(GAS_OUTPUTS, GAS_INPUTS, EMPTY_INPUTS, EMPTY_OUTPUTS).factory(MACHINE).constructor(GAS_OUTPUTS, GAS_INPUTS));
        registry.register(CCBRecipeTypes.ENERGIZATION.getId(), new RecipeSchema(GAS_OUTPUTS, GAS_INPUTS, EMPTY_INPUTS, EMPTY_OUTPUTS).factory(MACHINE).constructor(GAS_OUTPUTS, GAS_INPUTS));
        registry.register(CCBRecipeTypes.RESIDUE_GENERATION.getId(), new RecipeSchema(OPTIONAL_OUTPUTS, GAS_INPUTS, EMPTY_INPUTS).factory(MACHINE).constructor(OPTIONAL_OUTPUTS, GAS_INPUTS));
        registry.register(CCBRecipeTypes.REACTOR_KETTLE.getId(), new RecipeSchema(OPTIONAL_OUTPUTS, INPUTS, GAS_INPUTS, GAS_OUTPUTS, OPTIONAL_DURATION, TEMPERATURE, TEMPERATURE_MATCHING).factory(MACHINE).constructor(OPTIONAL_OUTPUTS, INPUTS));
        registry.register(CCBRecipeTypes.WIND_CHARGING.getId(), new RecipeSchema(OPTIONAL_ITEM_OUTPUTS, ITEM_INPUTS, WIND_CHARGE_DURATION, WIND_CHARGE_ACTION, PRIORITY).factory(MACHINE).constructor(OPTIONAL_ITEM_OUTPUTS, ITEM_INPUTS));
        registry.register(CCBRecipeTypes.FRACTIONATION_TOWER.getId(), new RecipeSchema(INPUTS, EMPTY_OUTPUTS, GAS_INPUTS, LAYER_OUTPUTS, DURATION, TEMPERATURE, TEMPERATURE_MATCHING).factory(FRACTIONATION).constructor(INPUTS));
    }
}
