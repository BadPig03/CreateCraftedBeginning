package net.ty.createcraftedbeginning.compat.kubejs.recipe;

import com.mojang.datafixers.util.Either;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext.Impl;
import dev.latvian.mods.kubejs.recipe.component.ComponentValueMap;
import dev.latvian.mods.kubejs.recipe.component.FluidStackComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeConstructor;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaType;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Wrapper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.recipe.ResidueGenerationRecipe;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ResidueKubeRecipe extends MachineKubeRecipe {
    private boolean requiresSelection;
    private boolean gasSelected;
    private int outputSelections;
    @Nullable
    private OutputKind outputKind;

    static RecipeConstructor mappingConstructor() {
        return new RecipeConstructor() {
            @Override
            public void setValues(RecipeScriptContext context, RecipeSchemaType schemaType, ComponentValueMap values) {
                super.setValues(context, schemaType, values);
                ((ResidueKubeRecipe) context.recipe()).requiresSelection = true;
            }
        };
    }

    @Override
    protected void validateComplete(ProcessingRecipe<?, ?> processing) {
        super.validateComplete(processing);
        if (!(processing instanceof ResidueGenerationRecipe recipe)) {
            throw new IllegalArgumentException("Residue mapping requires a residue generation recipe; got '" + type.idString + "'.");
        }

        if (!requiresSelection) {
            return;
        }

        if (!gasSelected || outputKind == null || outputSelections != 1) {
            throw new IllegalArgumentException("Residue mapping requires forGas and exactly one of itemResidue, fluidResidue or noResidue; got gasSelected=" + gasSelected + ", output=" + outputKind + ", selections=" + outputSelections + '.');
        }

        boolean hasItem = !recipe.getRollableResults().isEmpty();
        boolean hasFluid = !recipe.getFluidResults().isEmpty();
        boolean outputMatches = switch (outputKind) {
            case ITEM -> hasItem && !hasFluid;
            case FLUID -> !hasItem && hasFluid;
            case NONE -> !hasItem && !hasFluid;
        };
        if (outputMatches) {
            return;
        }

        throw new IllegalArgumentException("Residue mapping must keep its selected output kind; got selected=" + outputKind + ", item=" + hasItem + ", fluid=" + hasFluid + '.');
    }

    @SuppressWarnings("unused")
    public ResidueKubeRecipe forGas(String id) {
        GasRecipeRequirement gas = GasRecipeValue.of(id, 1).asIngredient();
        setValue(CCBRecipeSchemas.GAS_INPUTS, List.of(gas));
        requiresSelection = true;
        gasSelected = true;
        return this;
    }

    @SuppressWarnings("unused")
    public ResidueKubeRecipe itemResidue(Context context, Object product) {
        selectOutput(OutputKind.ITEM);
        ProcessingOutput output = CCBRecipeComponents.ITEM_OUTPUT.instance().wrap(new Impl(context, this), Wrapper.unwrapped(product));
        setValue(CCBRecipeSchemas.OPTIONAL_OUTPUTS, List.of(Either.right(new ProcessingOutput(output.getStack().copy(), output.getChance()))));
        return this;
    }

    @SuppressWarnings("unused")
    public ResidueKubeRecipe fluidResidue(Context context, Object product) {
        selectOutput(OutputKind.FLUID);
        product = Wrapper.unwrapped(product);
        FluidStack output;
        if (product instanceof CharSequence sequence) {
            Fluid fluid = BuiltInRegistries.FLUID.getOptional(ResourceLocation.parse(sequence.toString())).orElseThrow(() -> new IllegalArgumentException("Fluid residue ID must be registered; got '" + sequence + "'."));
            output = new FluidStack(fluid, 1);
        }
        else {
            output = FluidStackComponent.FLUID_STACK.instance().wrap(new Impl(context, this), product);
        }

        setValue(CCBRecipeSchemas.OPTIONAL_OUTPUTS, List.of(Either.left(output.copy())));
        return this;
    }

    @SuppressWarnings("unused")
    public ResidueKubeRecipe noResidue() {
        selectOutput(OutputKind.NONE);
        setValue(CCBRecipeSchemas.OPTIONAL_OUTPUTS, List.of());
        return this;
    }

    private void selectOutput(OutputKind selected) {
        requiresSelection = true;
        if (++outputSelections != 1) {
            throw new IllegalArgumentException("Residue mapping must select its output only once; got " + selected + " after " + outputKind + '.');
        }

        outputKind = selected;
    }

    private enum OutputKind {
        ITEM,
        FLUID,
        NONE
    }
}
