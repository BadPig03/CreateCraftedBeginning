package net.ty.createcraftedbeginning.compat.kubejs.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipesKubeEvent;
import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.kubejs.util.ErrorStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Recipe;
import net.ty.createcraftedbeginning.recipe.ChillingRecipe;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.recipe.DissipationRecipe;
import net.ty.createcraftedbeginning.recipe.EnergizationRecipe;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MachineKubeRecipe extends KubeRecipe {
    @Override
    public KubeRecipe serializeChanges() {
        if (!newRecipe && !hasChanged()) {
            return this;
        }

        try {
            afterLoaded(new ErrorStack());
            super.serializeChanges();
            validateComplete();
        }
        catch (RuntimeException exception) {
            ConsoleJS.SERVER.error("Failed to validate CCB machine recipe '%s'; skipping recipe: %s".formatted(getOrCreateId(), exception.getMessage()), sourceLine, exception, RecipesKubeEvent.CREATE_RECIPE_SKIP_ERROR);
            json = new JsonObject();
            json.addProperty("type", type.idString);
            JsonObject condition = new JsonObject();
            condition.addProperty("type", "neoforge:false");
            JsonArray conditions = new JsonArray();
            conditions.add(condition);
            json.add("neoforge:conditions", conditions);
        }

        return this;
    }

    private void validateComplete() {
        Recipe<?> recipe = type.schemaType.getSerializer().codec().codec().parse(type.event.ops.json(), json).getOrThrow(error -> new IllegalStateException("Failed to decode recipe for serializer '" + type.idString + "': " + error));
        if (!(recipe instanceof ProcessingRecipe<?, ?> processing)) {
            throw new IllegalArgumentException("Recipe must be a CCB processing recipe; got serializer '" + type.idString + "'.");
        }

        List<String> errors = processing.validate();
        int itemInputCount = processing.getIngredients().size();
        int itemOutputCount = processing.getRollableResults().size();
        if (processing instanceof ChillingRecipe && (itemInputCount != 1 || itemOutputCount == 0)) {
            errors.add("Chilling recipe must contain one item ingredient and at least one item output; got inputs=" + itemInputCount + ", outputs=" + itemOutputCount + '.');
        }

        if (processing instanceof CoolingRecipe) {
            int fluidInputCount = processing.getFluidIngredients().size();
            if (itemInputCount + fluidInputCount != 1) {
                errors.add("Cooling recipe must contain exactly one item or fluid ingredient; got items=" + itemInputCount + ", fluids=" + fluidInputCount + '.');
            }
        }

        if (processing instanceof WindChargingRecipe && itemInputCount != 1) {
            errors.add("Wind charging recipe must contain exactly one item ingredient; got " + itemInputCount + '.');
        }

        if (processing instanceof DissipationRecipe || processing instanceof EnergizationRecipe) {
            GasProcessingRecipe<?, ?> conversion = (GasProcessingRecipe<?, ?>) processing;
            int gasInputCount = conversion.getGasRequirements().size();
            int gasOutputCount = conversion.getGasResults().size();
            if (gasInputCount != 1 || gasOutputCount != 1) {
                errors.add("Gas conversion recipe must contain one gas ingredient and one gas output; got inputs=" + gasInputCount + ", outputs=" + gasOutputCount + '.');
            }
        }

        if (errors.isEmpty()) {
            return;
        }

        throw new IllegalArgumentException(String.join(" ", errors));
    }
}
