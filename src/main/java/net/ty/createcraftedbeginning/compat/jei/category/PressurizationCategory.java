package net.ty.createcraftedbeginning.compat.jei.category;

import com.simibubi.create.foundation.fluid.FluidIngredient;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedAirCompressor;
import net.ty.createcraftedbeginning.recipe.PressurizationRecipe;
import net.ty.createcraftedbeginning.registry.CCBGUITextures;
import org.jetbrains.annotations.NotNull;

public class PressurizationCategory extends CCBRecipeCategory<PressurizationRecipe> {
    private final AnimatedAirCompressor compressor = new AnimatedAirCompressor();

    public PressurizationCategory(Info<PressurizationRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PressurizationRecipe recipe, @NotNull IFocusGroup focuses) {
        addFluidInputSlot(builder, recipe.getIngredientsFluid());

        addFluidOutputSlot(builder, recipe.getResultingFluid());
    }

    @Override
    public void draw(PressurizationRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        CCBGUITextures.JEI_SHADOW.render(graphics, 61, 41);
		CCBGUITextures.JEI_LONG_ARROW.render(graphics, 52, 54);

        if (getBackground() == null) {
            return;
        }

        compressor.draw(graphics, getBackground().getWidth() / 2 - 17, 24);
    }

    private void addFluidInputSlot(IRecipeLayoutBuilder builder, FluidIngredient fluidIngredient) {
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 51).setFluidRenderer(1000, false, 16, 16).setBackground(getRenderedSlot(), -1, -1).addIngredients(NeoForgeTypes.FLUID_STACK, fluidIngredient.getMatchingFluidStacks());
    }

    private void addFluidOutputSlot(IRecipeLayoutBuilder builder, FluidStack outputFluid) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, 131, 50).setFluidRenderer(1000, false, 16, 16).setBackground(getRenderedSlot(), -1, -1).addIngredient(NeoForgeTypes.FLUID_STACK, outputFluid);
    }
}
