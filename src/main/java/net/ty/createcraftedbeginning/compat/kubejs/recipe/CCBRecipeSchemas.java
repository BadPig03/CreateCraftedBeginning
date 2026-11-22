package net.ty.createcraftedbeginning.compat.kubejs.recipe;

import com.mojang.datafixers.util.Either;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.FluidStackComponent;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
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
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureMatching;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBRecipeSchemas {
    static final RecipeKey<List<GasRecipeRequirement>> GAS_INPUTS = CCBRecipeComponents.GAS_INPUT.instance().asList().withBounds(IntBounds.OPTIONAL).inputKey("gas_ingredients").optional(List.of()).functionNames("gasInputs");
    static final RecipeKey<List<Ingredient>> ITEM_INPUTS = IngredientComponent.INGREDIENT.instance().asList().inputKey("ingredients");
    static final RecipeKey<List<ProcessingOutput>> ITEM_OUTPUTS = CCBRecipeComponents.ITEM_OUTPUT.instance().asList().outputKey("results");
    static final RecipeComponent<Either<Ingredient, SizedFluidIngredient>> ITEM_OR_FLUID_INPUT = IngredientComponent.INGREDIENT.instance().or(CCBRecipeComponents.FLUID_INPUT.instance());
    static final RecipeKey<List<Either<Ingredient, SizedFluidIngredient>>> INPUTS = ITEM_OR_FLUID_INPUT.asList().withBounds(IntBounds.OPTIONAL).inputKey("ingredients");
    static final RecipeKey<List<Either<FluidStack, ProcessingOutput>>> OUTPUTS = FluidStackComponent.FLUID_STACK.instance().or(CCBRecipeComponents.ITEM_OUTPUT.instance()).asList().outputKey("results");
    static final RecipeKey<List<ProcessingOutput>> EMPTY_OUTPUTS = CCBRecipeComponents.ITEM_OUTPUT.instance().asList().withBounds(IntBounds.OPTIONAL).outputKey("results").optional(List.of()).alwaysWrite().noFunctions();
    static final RecipeKey<TemperatureCondition> TEMPERATURE = CCBRecipeComponents.TEMPERATURE.otherKey("temperature").optional(TemperatureCondition.NONE);
    static final RecipeKey<TemperatureMatching> TEMPERATURE_MATCHING = CCBRecipeComponents.TEMPERATURE_MATCHING.otherKey("temperature_matching").optional(TemperatureMatching.EXACT);
    private static final RecipeKey<List<Ingredient>> EMPTY_INPUTS = IngredientComponent.INGREDIENT.instance().asList().withBounds(IntBounds.OPTIONAL).inputKey("ingredients").optional(List.of()).alwaysWrite().noFunctions();
    static final RecipeKey<List<Either<FluidStack, ProcessingOutput>>> OPTIONAL_OUTPUTS = FluidStackComponent.FLUID_STACK.instance().or(CCBRecipeComponents.ITEM_OUTPUT.instance()).asList().withBounds(IntBounds.OPTIONAL).outputKey("results").optional(List.of()).alwaysWrite();
    private static final RecipeKey<List<GasStack>> GAS_OUTPUTS = CCBRecipeComponents.GAS_OUTPUT.instance().asList().withBounds(IntBounds.OPTIONAL).outputKey("gas_results").optional(List.of()).functionNames("gasOutputs");
    private static final RecipeKey<Integer> COOLING_DURATION = NumberComponent.intRange(1, Integer.MAX_VALUE).otherKey("processing_time").functionNames("processingTime");
    private static final RecipeKey<Integer> OPTIONAL_DURATION = NumberComponent.intRange(0, Integer.MAX_VALUE).otherKey("processing_time").optional(0).functionNames("processingTime");
    private static final KubeRecipeFactory MACHINE = new KubeRecipeFactory(CCBAPI.asResource("machine"), MachineKubeRecipe.class, MachineKubeRecipe::new);
    private static final KubeRecipeFactory RESIDUE = new KubeRecipeFactory(CCBAPI.asResource("residue"), ResidueKubeRecipe.class, ResidueKubeRecipe::new);

    private CCBRecipeSchemas() {
    }

    public static void register(RecipeSchemaRegistry registry) {
        registry.register(CCBRecipeTypes.CHILLING.getId(), new RecipeSchema(ITEM_OUTPUTS, ITEM_INPUTS).factory(MACHINE).constructor(ITEM_OUTPUTS, ITEM_INPUTS));
        registry.register(CCBRecipeTypes.GAS_INJECTION.getId(), GasInjectionKubeRecipe.schema());
        registry.register(CCBRecipeTypes.FORGING_PRESS.getId(), ForgingPressKubeRecipe.schema());
        registry.register(CCBRecipeTypes.CHILLED_COMPACTING.getId(), new RecipeSchema(OUTPUTS, INPUTS).factory(MACHINE).constructor(OUTPUTS, INPUTS));
        registry.register(CCBRecipeTypes.CHILLED_MIXING.getId(), new RecipeSchema(OUTPUTS, INPUTS, OPTIONAL_DURATION).factory(MACHINE).constructor(OUTPUTS, INPUTS));
        registry.register(CCBRecipeTypes.COOLING.getId(), new RecipeSchema(INPUTS, COOLING_DURATION, EMPTY_OUTPUTS).factory(MACHINE).constructor(INPUTS, COOLING_DURATION));
        registry.register(CCBRecipeTypes.DISSIPATION.getId(), new RecipeSchema(GAS_OUTPUTS, GAS_INPUTS, EMPTY_INPUTS, EMPTY_OUTPUTS).factory(MACHINE).constructor(GAS_OUTPUTS, GAS_INPUTS));
        registry.register(CCBRecipeTypes.ENERGIZATION.getId(), new RecipeSchema(GAS_OUTPUTS, GAS_INPUTS, EMPTY_INPUTS, EMPTY_OUTPUTS).factory(MACHINE).constructor(GAS_OUTPUTS, GAS_INPUTS));
        registry.register(CCBRecipeTypes.RESIDUE_GENERATION.getId(), new RecipeSchema(OPTIONAL_OUTPUTS, GAS_INPUTS, EMPTY_INPUTS).factory(RESIDUE).constructor(ResidueKubeRecipe.mappingConstructor()));
        registry.register(CCBRecipeTypes.REACTOR_KETTLE.getId(), ReactorKettleKubeRecipe.schema());
        registry.register(CCBRecipeTypes.WIND_CHARGING.getId(), WindChargingKubeRecipe.schema());
        registry.register(CCBRecipeTypes.FRACTIONATION_TOWER.getId(), FractionationTowerKubeRecipe.schema());
    }
}
