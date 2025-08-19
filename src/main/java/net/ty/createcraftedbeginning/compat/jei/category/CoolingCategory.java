package net.ty.createcraftedbeginning.compat.jei.category;

import com.simibubi.create.foundation.fluid.FluidIngredient;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.crafting.Ingredient;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedBreezeChamber;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBGUITextures;
import org.jetbrains.annotations.NotNull;

public class CoolingCategory extends CCBRecipeCategory<CoolingRecipe> {
    private final AnimatedBreezeChamber chamber = new AnimatedBreezeChamber().withFrost(false);

    public CoolingCategory(Info<CoolingRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CoolingRecipe recipe, @NotNull IFocusGroup focuses) {
        if (recipe.isIngredientsFluid()) {
            addFluidInputSlot(builder, recipe.getIngredientsFluid());
        }
        else {
            addItemInputSlot(builder, recipe.getIngredientsItem());
        }
    }

    @Override
    public void draw(CoolingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        CCBGUITextures.JEI_SHADOW.render(graphics, 122, 37);
        CCBGUITextures.JEI_LONG_ARROW.render(graphics, 42, 30);
        CCBGUITextures.JEI_COOLING.render(graphics, 16, 8);
		CCBGUITextures.JEI_COOLING_BACKGROUND.render(graphics, 16, 8);

        if (getBackground() == null) {
            return;
        }

        MutableComponent time = CCBLang.secondsWithGameTicks(recipe.getResultTime(), 20).component();
        graphics.drawString(font, time, getBackground().getWidth() / 2 - font.width(time) / 2 - 12, 22, 0x888888, false);

        chamber.draw(graphics, getBackground().getWidth() / 2 + 44, 18);
    }

    private void addItemInputSlot(IRecipeLayoutBuilder builder, Ingredient ingredient) {
        builder.addSlot(RecipeIngredientRole.INPUT, 16, 27).setBackground(getRenderedSlot(), -1, -1).addIngredients(ingredient);
    }

    private void addFluidInputSlot(IRecipeLayoutBuilder builder, FluidIngredient fluidIngredient) {
        builder.addSlot(RecipeIngredientRole.INPUT, 16, 27).setFluidRenderer(1000, false, 16, 16).setBackground(getRenderedSlot(), -1, -1).addIngredients(NeoForgeTypes.FLUID_STACK, fluidIngredient.getMatchingFluidStacks());
    }
}
