package net.ty.createcraftedbeginning.compat.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.compat.jei.JEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedGasInjectionChamber;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import org.jetbrains.annotations.NotNull;

public class GasInjectionCategory extends CCBRecipeCategory<GasInjectionRecipe> {
    private final AnimatedGasInjectionChamber chamber = new AnimatedGasInjectionChamber();

    public GasInjectionCategory(Info<GasInjectionRecipe> info) {
        super(info);
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

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull GasInjectionRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 51).setBackground(getRenderedSlot(), -1, -1).addIngredients(recipe.getIngredients().getFirst());
        GasStack gasStack = recipe.getRequiredGas().getFirstGas().copyWithAmount(FluidType.BUCKET_VOLUME);
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 32).setBackground(getRenderedSlot(), -1, -1).addIngredient(JEIPlugin.GAS_STACK, gasStack).setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 16).addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(Component.translatable("jei.tooltip.gas.amount", gasStack.getAmount()).withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 51).setBackground(getRenderedSlot(), -1, -1).addItemStack(getResultItem(recipe));
    }
}
