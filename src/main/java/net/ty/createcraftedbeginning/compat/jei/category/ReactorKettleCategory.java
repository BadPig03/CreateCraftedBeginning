package net.ty.createcraftedbeginning.compat.jei.category;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.client.CCBGUITextures;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedAirtightReactorKettle;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.TemperatureCondition;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.simibubi.create.compat.jei.category.CreateRecipeCategory.addFluidSlot;
import static com.simibubi.create.compat.jei.category.CreateRecipeCategory.addStochasticTooltip;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReactorKettleCategory extends CCBRecipeCategory<ReactorKettleRecipe> {
    private final AnimatedAirtightReactorKettle reactorKettleOpened = new AnimatedAirtightReactorKettle(false);
    private final AnimatedAirtightReactorKettle reactorKettleClosed = new AnimatedAirtightReactorKettle(true);

    public ReactorKettleCategory(Info<ReactorKettleRecipe> info) {
        super(info);
    }

    private static int getInputX(int index, int xOffset) {
        return 14 + xOffset + index % 3 * 19;
    }

    private static int getInputY(int index) {
        return 59 - index / 3 * 19;
    }

    private static int getOutputX(int index, int size) {
        if (size % 2 != 0 && index == size - 1) {
            return 142;
        }
        return index % 2 == 0 ? 132 : 151;
    }

    private static int getOutputY(int index) {
        return -19 * (index / 2) + 59;
    }

    @Override
    protected void draw(ReactorKettleRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        int size = recipe.getFluidResults().size() + recipe.getGasResults().size() + recipe.getRollableResults().size();
        if (size <= 3) {
            CCBGUITextures.JEI_DOWN_ARROW.render(graphics, 136, -19 * ((1 + size) / 2 - 1) + 32);
        }
        CCBGUITextures.JEI_SHADOW.render(graphics, 76, 66);

        TemperatureCondition condition = recipe.getTemperatureCondition();
        int color = condition.getColor();
        CCBGUITextures.JEI_HEAT_BAR.render(graphics, 4, 80, new Color(color));
        graphics.drawString(Minecraft.getInstance().font, CCBLang.translateDirect(condition.getTranslationKey()), 9, 86, color, false);

        if (recipe.getGasIngredients().isEmpty() && recipe.getGasResults().isEmpty()) {
            reactorKettleOpened.draw(graphics, getBackground().getWidth() / 2 + 6, 58);
            return;
        }

        reactorKettleClosed.draw(graphics, getBackground().getWidth() / 2 + 6, 58);
    }

    @Override
    protected void setRecipe(IRecipeLayoutBuilder builder, ReactorKettleRecipe recipe, IFocusGroup focuses) {
        List<Pair<Ingredient, Integer>> condensedIngredients = ReactorKettleRecipe.getCondensedIngredients(recipe.getIngredients());
        NonNullList<SizedFluidIngredient> fluidIngredients = recipe.getFluidIngredients();
        NonNullList<SizedGasIngredient> gasIngredients = recipe.getGasIngredients();
        List<ProcessingOutput> results = recipe.getRollableResults();
        NonNullList<FluidStack> fluidResults = recipe.getFluidResults();
        NonNullList<GasStack> gasResults = recipe.getGasResults();
        int inputCount = condensedIngredients.size() + fluidIngredients.size() + gasIngredients.size();
        int xOffset = inputCount < 3 ? (3 - inputCount) * 19 / 2 : 0;
        int inputIndex = 0;
        for (Pair<Ingredient, Integer> pair : condensedIngredients) {
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack stack : pair.getFirst().getItems()) {
                stacks.add(stack.copyWithCount(pair.getSecond()));
            }
            int x = getInputX(inputIndex, xOffset);
            int y = getInputY(inputIndex);
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).setBackground(getRenderedSlot(), -1, -1).addItemStacks(stacks);
            inputIndex++;
        }
        for (SizedFluidIngredient fluidIngredient : fluidIngredients) {
            int x = getInputX(inputIndex, xOffset);
            int y = getInputY(inputIndex);
            addFluidSlot(builder, x, y, fluidIngredient);
            inputIndex++;
        }
        for (SizedGasIngredient gasIngredient : gasIngredients) {
            int x = getInputX(inputIndex, xOffset);
            int y = getInputY(inputIndex);
            List<GasStack> gases = Arrays.stream(gasIngredient.getGases()).map(GasStack::copy).toList();
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).setBackground(getRenderedSlot(), -1, -1).addIngredients(CCBJEIPlugin.GAS_STACK, gases).addRichTooltipCallback((view, tooltip) -> tooltip.add(GasAmountUtils.precise(gasIngredient.amount()).style(ChatFormatting.GRAY).component()));
            inputIndex++;
        }

        int outputCount = results.size() + fluidResults.size() + gasResults.size();
        int outputIndex = 0;
        for (ProcessingOutput result : results) {
            int x = getOutputX(outputIndex, outputCount);
            int y = getOutputY(outputIndex);
            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).setBackground(getRenderedSlot(result), -1, -1).addItemStack(result.getStack()).addRichTooltipCallback(addStochasticTooltip(result));
            outputIndex++;
        }
        for (FluidStack fluidResult : fluidResults) {
            int x = getOutputX(outputIndex, outputCount);
            int y = getOutputY(outputIndex);
            addFluidSlot(builder, x, y, fluidResult);
            outputIndex++;
        }
        for (GasStack gasResult : gasResults) {
            int x = getOutputX(outputIndex, outputCount);
            int y = getOutputY(outputIndex);
            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).setBackground(getRenderedSlot(), -1, -1).addIngredient(CCBJEIPlugin.GAS_STACK, gasResult.copy()).addRichTooltipCallback((view, tooltip) -> tooltip.add(GasAmountUtils.precise(gasResult.getAmount()).style(ChatFormatting.GRAY).component()));
            outputIndex++;
        }

        TemperatureCondition condition = recipe.getTemperatureCondition();
        if (condition == TemperatureCondition.NONE) {
            return;
        }

        switch (condition) {
            case CHILLED -> builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81).addItemStack(new ItemStack(CCBBlocks.BREEZE_COOLER_BLOCK));
            case SUPERCHILLED -> builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81).addItemStack(new ItemStack(CCBBlocks.BREEZE_COOLER_BLOCK, 3));
            case HEATED -> builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81).addItemStack(new ItemStack(AllBlocks.BLAZE_BURNER));
            case SUPERHEATED -> {
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81).addItemStack(new ItemStack(AllBlocks.BLAZE_BURNER));
                builder.addSlot(RecipeIngredientRole.CATALYST, 153, 81).addItemStack(new ItemStack(AllItems.BLAZE_CAKE.asItem()));
            }
        }
    }
}
