package net.ty.createcraftedbeginning.compat.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.compat.jei.CCBJEI;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedAirtightEngine;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import net.ty.createcraftedbeginning.recipe.ResidueGenerationRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResidueGenerationCategory extends CCBRecipeCategory<ResidueGenerationRecipe> {
    private final AnimatedAirtightEngine engine = new AnimatedAirtightEngine();

    public ResidueGenerationCategory(Info<ResidueGenerationRecipe> info) {
        super(info);
    }

    @Override
    protected void draw(ResidueGenerationRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        CCBGUITextures.JEI_SHADOW.render(graphics, 61, 54);
        CCBGUITextures.JEI_LONG_ARROW.render(graphics, 52, 77);
        engine.draw(graphics, getBackground().getWidth() / 2 - 13, 39);
    }

    @Override
    protected void setRecipe(IRecipeLayoutBuilder builder, ResidueGenerationRecipe recipe, IFocusGroup focuses) {
        addGasInputSlot(builder, recipe.getIngredientsGas());
        addOutputSlot(builder, recipe);
    }

    private static void addGasInputSlot(IRecipeLayoutBuilder builder, SizedGasIngredient gasIngredient) {
        GasStack[] gasStackList = gasIngredient.getGases();
        List<GasStack> fullStacks = new ArrayList<>();
        for (GasStack stack : gasStackList) {
            fullStacks.add(stack.copyWithAmount(FluidType.BUCKET_VOLUME));
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 74).setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 16).setBackground(getRenderedSlot(), -1, -1).addIngredients(CCBJEI.GAS_STACK, fullStacks);
    }

    private static void addOutputSlot(IRecipeLayoutBuilder builder, ResidueGenerationRecipe recipe) {
        if (recipe.getFluidResults().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 74).setBackground(getRenderedSlot(), -1, -1).addItemStack(getResultItem(recipe));
        }
        else {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 74).setBackground(getRenderedSlot(), -1, -1).addIngredient(NeoForgeTypes.FLUID_STACK, recipe.getFluidResults().getFirst()).setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 16);
        }
    }
}
