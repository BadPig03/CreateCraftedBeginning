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
import net.ty.createcraftedbeginning.api.gas.gases.SizedGasIngredient;
import net.ty.createcraftedbeginning.compat.jei.JEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedBreezeChamberWithTank;
import net.ty.createcraftedbeginning.recipe.EnergizationRecipe;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnergizationCategory extends CCBRecipeCategory<EnergizationRecipe> {
    private final AnimatedBreezeChamberWithTank chamber = new AnimatedBreezeChamberWithTank();

    public EnergizationCategory(Info<EnergizationRecipe> info) {
        super(info);
    }

    @Override
    public void draw(EnergizationRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        CCBGUITextures.JEI_SHADOW.render(graphics, 61, 41);
        CCBGUITextures.JEI_LONG_ARROW.render(graphics, 52, 54);
        if (getBackground() == null) {
            return;
        }

        chamber.draw(graphics, getBackground().getWidth() / 2 - 17, 24);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, @NotNull EnergizationRecipe recipe, @NotNull IFocusGroup focuses) {
        addGasInputSlot(builder, recipe.getIngredientsGas());
        addGasOutputSlot(builder, recipe.getResultingGas());
    }

    private static void addGasInputSlot(@NotNull IRecipeLayoutBuilder builder, @NotNull SizedGasIngredient gasIngredient) {
        List<GasStack> gasStackList = Arrays.asList(gasIngredient.getGases());
        List<GasStack> fullStacks = new ArrayList<>();
        for (GasStack stack : gasStackList) {
            fullStacks.add(stack.copyWithAmount(FluidType.BUCKET_VOLUME));
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 51).setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 16).setBackground(getRenderedSlot(), -1, -1).addIngredients(JEIPlugin.GAS_STACK, fullStacks).addRichTooltipCallback((recipeSlotView, tooltip) -> gasStackList.stream().map(stack -> Component.translatable("jei.tooltip.gas.amount", stack.getAmount()).withStyle(ChatFormatting.GRAY)).forEach(tooltip::add));
    }

    private static void addGasOutputSlot(@NotNull IRecipeLayoutBuilder builder, @NotNull GasStack outputGas) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, 131, 50).setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 16).setBackground(getRenderedSlot(), -1, -1).addIngredient(JEIPlugin.GAS_STACK, outputGas.copyWithAmount(FluidType.BUCKET_VOLUME)).addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(Component.translatable("jei.tooltip.gas.amount", outputGas.getAmount()).withStyle(ChatFormatting.GRAY)));
    }
}
