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
import net.ty.createcraftedbeginning.api.gas.recipes.GasIngredient;
import net.ty.createcraftedbeginning.compat.jei.JEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedAirCompressor;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import net.ty.createcraftedbeginning.recipe.PressurizationRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PressurizationCategory extends CCBRecipeCategory<PressurizationRecipe> {
    private static final int MAX_CAPACITY = FluidType.BUCKET_VOLUME;
    private final AnimatedAirCompressor compressor = new AnimatedAirCompressor();

    public PressurizationCategory(Info<PressurizationRecipe> info) {
        super(info);
    }

    private static void addGasInputSlot(@NotNull IRecipeLayoutBuilder builder, @NotNull GasIngredient gasIngredient) {
        List<GasStack> fullStacks = new ArrayList<>();
        for (GasStack stack : gasIngredient.getMatchingGasStacks()) {
            fullStacks.add(stack.copyWithAmount(MAX_CAPACITY));
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 51).setFluidRenderer(MAX_CAPACITY, false, 16, 16).setBackground(getRenderedSlot(), -1, -1).addIngredients(JEIPlugin.GAS_STACK, fullStacks).addRichTooltipCallback((recipeSlotView, tooltip) -> gasIngredient.getMatchingGasStacks().stream().map(stack -> Component.translatable("jei.tooltip.gas.amount", stack.getAmount()).withStyle(ChatFormatting.GRAY)).forEach(tooltip::add));
    }

    private static void addGasOutputSlot(@NotNull IRecipeLayoutBuilder builder, @NotNull GasStack outputGas) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, 131, 50).setFluidRenderer(MAX_CAPACITY, false, 16, 16).setBackground(getRenderedSlot(), -1, -1).addIngredient(JEIPlugin.GAS_STACK, outputGas.copyWithAmount(MAX_CAPACITY)).addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(Component.translatable("jei.tooltip.gas.amount", outputGas.getAmount()).withStyle(ChatFormatting.GRAY)));
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

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, @NotNull PressurizationRecipe recipe, @NotNull IFocusGroup focuses) {
        addGasInputSlot(builder, recipe.getIngredientsGas());
        addGasOutputSlot(builder, recipe.getResultingGas());
    }
}
