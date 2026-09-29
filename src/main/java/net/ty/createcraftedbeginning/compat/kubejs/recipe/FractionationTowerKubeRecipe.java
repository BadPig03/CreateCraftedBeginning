package net.ty.createcraftedbeginning.compat.kubejs.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext.Impl;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Wrapper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.recipe.FractionationTowerOutput;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class FractionationTowerKubeRecipe extends MachineKubeRecipe {
    public FractionationTowerKubeRecipe outputAt(Context cx, int layer, Object product) {
        product = Wrapper.unwrapped(product);
        FractionationTowerOutput output;
        switch (product) {
            case GasRecipeValue gas -> output = new FractionationTowerOutput(layer, ItemStack.EMPTY, FluidStack.EMPTY, gas.asStack());
            case GasStack gas -> output = new FractionationTowerOutput(layer, ItemStack.EMPTY, FluidStack.EMPTY, gas);
            case FluidStack fluid -> output = new FractionationTowerOutput(layer, ItemStack.EMPTY, fluid, GasStack.EMPTY);
            case null, default -> {
                if (product instanceof ItemRecipeOutput(ProcessingOutput itemOutput)) {
                    float chance = itemOutput.getChance();
                    if (chance != 1) {
                        throw new IllegalArgumentException("Fractionation output chance must be 1; got " + chance + " for layer " + layer + '.');
                    }

                    product = itemOutput.getStack();
                }

                ItemStack item = ItemStackComponent.ITEM_STACK.instance().wrap(new Impl(cx, this), product);
                output = new FractionationTowerOutput(layer, item, FluidStack.EMPTY, GasStack.EMPTY);
            }
        }

        if (!output.isValid()) {
            throw new IllegalArgumentException("Fractionation output must contain one non-empty product and use a layer in [1, " + (FractionationTowerOutput.MAX_LAYER_OFFSET + 1) + "); got layer=" + layer + ", product=" + product + '.');
        }

        List<FractionationTowerOutput> current = getValue(CCBRecipeSchemas.LAYER_OUTPUTS);
        List<FractionationTowerOutput> outputs = current == null ? new ArrayList<>() : new ArrayList<>(current);
        if (outputs.stream().anyMatch(existing -> existing.layer() == layer)) {
            throw new IllegalArgumentException("Fractionation output layer must be unique; got duplicate layer " + layer + '.');
        }

        outputs.add(output);
        setValue(CCBRecipeSchemas.LAYER_OUTPUTS, outputs);
        return this;
    }
}
