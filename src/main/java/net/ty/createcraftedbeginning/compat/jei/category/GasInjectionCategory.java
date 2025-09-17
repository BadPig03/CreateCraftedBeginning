package net.ty.createcraftedbeginning.compat.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedGasInjectionChamber;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.registry.CCBGUITextures;
import org.jetbrains.annotations.NotNull;

public class GasInjectionCategory extends CCBRecipeCategory<GasInjectionRecipe> {
    private final AnimatedGasInjectionChamber chamber = new AnimatedGasInjectionChamber();

    public GasInjectionCategory(Info<GasInjectionRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull GasInjectionRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 51).setBackground(getRenderedSlot(), -1, -1).addIngredients(recipe.getIngredients().getFirst());

        FluidStack fluidStack = recipe.getRequiredFluid().getMatchingFluidStacks().getFirst();
        builder.addSlot(RecipeIngredientRole.OUTPUT, 27, 32).setBackground(getRenderedSlot(), -1, -1).addIngredient(NeoForgeTypes.FLUID_STACK, fluidStack).setFluidRenderer(fluidStack.getAmount(), false, 16, 16);

        builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 51).setBackground(getRenderedSlot(), -1, -1).addItemStack(getResultItem(recipe));
    }

    @Override
    public void draw(GasInjectionRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        CCBGUITextures.JEI_SHADOW.render(graphics, 62, 57);
        CCBGUITextures.JEI_DOWN_ARROW.render(graphics, 126, 29);

        if (getBackground() == null) {
            return;
        }

        chamber.draw(graphics, getBackground().getWidth() / 2 - 13, 22);
    }
}
