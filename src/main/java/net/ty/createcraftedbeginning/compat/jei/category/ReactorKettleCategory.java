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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.reactorkettle.TemperatureCondition;
import net.ty.createcraftedbeginning.compat.jei.JEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedAirtightReactorKettle;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.simibubi.create.compat.jei.category.CreateRecipeCategory.addFluidSlot;
import static com.simibubi.create.compat.jei.category.CreateRecipeCategory.addStochasticTooltip;

public class ReactorKettleCategory extends CCBRecipeCategory<ReactorKettleRecipe> {
    private final AnimatedAirtightReactorKettle reactorKettleOpened = new AnimatedAirtightReactorKettle(false);
    private final AnimatedAirtightReactorKettle reactorKettleClosed = new AnimatedAirtightReactorKettle(true);

    public ReactorKettleCategory(Info<ReactorKettleRecipe> info) {
        super(info);
    }

    private static int getInputX(int i, int xOffset) {
        return 14 + xOffset + i % 3 * 19;
    }

    private static int getInputY(int i) {
        return 59 - i / 3 * 19;
    }

    private static int getOutputX(int i, int size) {
        return 142 - (size % 2 != 0 && i == size - 1 ? 0 : i % 2 == 0 ? 10 : -9);
    }

    private static int getOutputY(int i) {
        return -19 * (i / 2) + 59;
    }

    @Override
    protected void draw(@NotNull ReactorKettleRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        if (getBackground() == null) {
            return;
        }

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
        }
        else {
            reactorKettleClosed.draw(graphics, getBackground().getWidth() / 2 + 6, 58);
        }
    }

    @Override
    protected void setRecipe(IRecipeLayoutBuilder builder, @NotNull ReactorKettleRecipe recipe, IFocusGroup focuses) {
        List<Pair<Ingredient, Integer>> condensedIngredients = ReactorKettleRecipe.getCondensedIngredients(recipe.getIngredients());
        int size = condensedIngredients.size() + recipe.getFluidIngredients().size() + recipe.getGasIngredients().size();
        int xOffset = size < 3 ? (3 - size) * 19 / 2 : 0;
        int i = 0;
        for (Pair<Ingredient, Integer> pair : condensedIngredients) {
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack itemStack : pair.getFirst().getItems()) {
                stacks.add(itemStack.copyWithCount(pair.getSecond()));
            }
            int x = getInputX(i, xOffset);
            int y = getInputY(i);
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).setBackground(getRenderedSlot(), -1, -1).addItemStacks(stacks);
            i++;
        }
        for (SizedFluidIngredient fluidIngredient : recipe.getFluidIngredients()) {
            int x = getInputX(i, xOffset);
            int y = getInputY(i);
            addFluidSlot(builder, x, y, fluidIngredient);
            i++;
        }
        for (SizedGasIngredient gasIngredient : recipe.getGasIngredients()) {
            int x = getInputX(i, xOffset);
            int y = getInputY(i);
            GasStack gasStack = gasIngredient.getFirstGas();
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 16).setBackground(getRenderedSlot(), -1, -1).addIngredient(JEIPlugin.GAS_STACK, gasStack.copyWithAmount(FluidType.BUCKET_VOLUME)).addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(Component.translatable("jei.tooltip.gas.amount", gasStack.getAmount()).withStyle(ChatFormatting.GRAY)));
            i++;
        }

        size = recipe.getRollableResults().size() + recipe.getFluidResults().size() + recipe.getGasResults().size();
        i = 0;
        for (ProcessingOutput result : recipe.getRollableResults()) {
            int x = getOutputX(i, size);
            int y = getOutputY(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).setBackground(getRenderedSlot(result), -1, -1).addItemStack(result.getStack()).addRichTooltipCallback(addStochasticTooltip(result));
            i++;
        }
        for (FluidStack fluidResult : recipe.getFluidResults()) {
            int x = getOutputX(i, size);
            int y = getOutputY(i);
            addFluidSlot(builder, x, y, fluidResult);
            i++;
        }
        for (GasStack gasResult : recipe.getGasResults()) {
            int x = getOutputX(i, size);
            int y = getOutputY(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 16).setBackground(getRenderedSlot(), -1, -1).addIngredient(JEIPlugin.GAS_STACK, gasResult.copyWithAmount(FluidType.BUCKET_VOLUME)).addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(Component.translatable("jei.tooltip.gas.amount", gasResult.getAmount()).withStyle(ChatFormatting.GRAY)));
            i++;
        }

        TemperatureCondition condition = recipe.getTemperatureCondition();
        if (condition == TemperatureCondition.NONE) {
            return;
        }

        switch (condition) {
            case CHILLED -> builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81).addItemStack(CCBBlocks.BREEZE_COOLER_BLOCK.asStack());
            case SUPERCHILLED -> builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81).addItemStack(new ItemStack(CCBBlocks.BREEZE_COOLER_BLOCK.asItem(), 3));
            case HEATED -> builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81).addItemStack(AllBlocks.BLAZE_BURNER.asStack());
            case SUPERHEATED -> {
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81).addItemStack(AllBlocks.BLAZE_BURNER.asStack());
                builder.addSlot(RecipeIngredientRole.CATALYST, 153, 81).addItemStack(AllItems.BLAZE_CAKE.asStack());
            }
        }
    }
}
