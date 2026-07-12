package net.ty.createcraftedbeginning.compat.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedAirCompressor;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import net.ty.createcraftedbeginning.recipe.PressurizationRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizationCategory extends CCBRecipeCategory<PressurizationRecipe> {
    private static final int MAX_CAPACITY = FluidType.BUCKET_VOLUME;
    private final AnimatedAirCompressor compressor = new AnimatedAirCompressor();

    public PressurizationCategory(Info<PressurizationRecipe> info) {
        super(info);
    }

    @Override
    public void draw(PressurizationRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        CCBGUITextures.JEI_SHADOW.render(graphics, 61, 41);
        CCBGUITextures.JEI_LONG_ARROW.render(graphics, 52, 54);
        compressor.draw(graphics, getBackground().getWidth() / 2 - 17, 24);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PressurizationRecipe recipe, IFocusGroup focuses) {
        SizedGasIngredient gasIngredient = recipe.getGasIngredient();
        List<GasStack> stacks = Arrays.stream(gasIngredient.getGases()).map(GasStack::copy).toList();
        GasStack outputGas = recipe.getGasResult();
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 51).setBackground(getRenderedSlot(), -1, -1).addIngredients(CCBJEIPlugin.GAS_STACK, stacks).addRichTooltipCallback((v, t) -> Arrays.stream(gasIngredient.getGases()).map(stack -> Component.translatable("jei.tooltip.gas.amount", stack.getAmount()).withStyle(ChatFormatting.GRAY)).forEach(t::add));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 131, 50).setBackground(getRenderedSlot(), -1, -1).addIngredient(CCBJEIPlugin.GAS_STACK, outputGas.copy()).addRichTooltipCallback((v, t) -> t.add(Component.translatable("jei.tooltip.gas.amount", outputGas.getAmount()).withStyle(ChatFormatting.GRAY)));
    }
}
