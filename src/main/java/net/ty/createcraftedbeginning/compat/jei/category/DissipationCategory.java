package net.ty.createcraftedbeginning.compat.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedBreezeChamberWithTank;
import net.ty.createcraftedbeginning.compat.jei.CCBJEITextures;
import net.ty.createcraftedbeginning.recipe.DissipationRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DissipationCategory extends CCBRecipeCategory<DissipationRecipe> {
    private final AnimatedBreezeChamberWithTank chamber = new AnimatedBreezeChamberWithTank(true);

    public DissipationCategory(Info<DissipationRecipe> info) {
        super(info);
    }

    @Override
    public void draw(DissipationRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        CCBJEITextures.JEI_SHADOW.render(graphics, 61, 41);
        CCBJEITextures.JEI_LONG_ARROW.render(graphics, 52, 54);
        chamber.draw(graphics, background.getWidth() / 2 - 17, 24);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DissipationRecipe recipe, IFocusGroup focuses) {
        SizedGasIngredient gasIngredient = recipe.getGasIngredient();
        List<GasStack> gasStacks = Arrays.stream(gasIngredient.getGases()).map(GasStack::copy).toList();
        GasStack outputGas = recipe.getGasResult();
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 51).setBackground(getRenderedSlot(), -1, -1).addIngredients(CCBJEIPlugin.GAS_STACK, gasStacks).addRichTooltipCallback((view, tooltip) -> tooltip.add(GasAmounts.precise(gasIngredient.amount()).style(ChatFormatting.GRAY).component()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 131, 50).setBackground(getRenderedSlot(), -1, -1).addIngredient(CCBJEIPlugin.GAS_STACK, outputGas.copy()).addRichTooltipCallback((view, tooltip) -> tooltip.add(GasAmounts.precise(outputGas.getAmount()).style(ChatFormatting.GRAY).component()));
    }
}
