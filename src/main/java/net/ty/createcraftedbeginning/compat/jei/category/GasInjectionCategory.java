package net.ty.createcraftedbeginning.compat.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.client.CCBGUITextures;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedGasInjectionChamber;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

import static com.simibubi.create.compat.jei.category.CreateRecipeCategory.addFluidSlot;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionCategory extends CCBRecipeCategory<GasInjectionRecipe> {
    private final AnimatedGasInjectionChamber depotChamber = new AnimatedGasInjectionChamber(false);
    private final AnimatedGasInjectionChamber basinChamber = new AnimatedGasInjectionChamber(true);

    public GasInjectionCategory(Info<GasInjectionRecipe> info) {
        super(info);
    }

    @Override
    public void draw(GasInjectionRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        CCBGUITextures.JEI_SHADOW.render(graphics, 62, 57);
        CCBGUITextures.JEI_DOWN_ARROW.render(graphics, 126, 29);

        if (recipe.isFluidInjection()) {
            basinChamber.draw(graphics, getBackground().getWidth() / 2 - 13, 22);
            return;
        }

        depotChamber.draw(graphics, getBackground().getWidth() / 2 - 13, 22);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GasInjectionRecipe recipe, IFocusGroup focuses) {
        SizedGasIngredient gasIngredient = recipe.getGasIngredient();
        List<GasStack> gases = Arrays.stream(gasIngredient.getGases()).map(GasStack::copy).toList();
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 32).setBackground(getRenderedSlot(), -1, -1).addIngredients(CCBJEIPlugin.GAS_STACK, gases).addRichTooltipCallback((view, tooltip) -> tooltip.add(GasAmountUtils.precise(gasIngredient.amount()).style(ChatFormatting.GRAY).component()));

        if (recipe.isFluidInjection()) {
            addFluidSlot(builder, 27, 51, recipe.getFluidIngredient());
            addFluidSlot(builder, 132, 51, recipe.getFluidResult());
            return;
        }

        builder.addSlot(RecipeIngredientRole.INPUT, 27, 51).setBackground(getRenderedSlot(), -1, -1).addIngredients(recipe.getIngredient());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 51).setBackground(getRenderedSlot(), -1, -1).addItemStack(getResultItem(recipe));
    }
}
