package net.ty.createcraftedbeginning.compat.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedBreezeCooler;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class CoolingCategory extends CCBRecipeCategory<CoolingRecipe> {
    private static final int COLOR = 0x888888;
    private final AnimatedBreezeCooler cooler = new AnimatedBreezeCooler();

    public CoolingCategory(Info<CoolingRecipe> info) {
        super(info);
    }

    @Override
    public void draw(CoolingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        CCBGUITextures.JEI_SHADOW.render(graphics, 122, 37);
        CCBGUITextures.JEI_LONG_ARROW.render(graphics, 42, 30);
        CCBGUITextures.JEI_COOLING_BACKGROUND.render(graphics, 16, 8);
        if (getBackground() == null) {
            return;
        }

        if (!recipe.isFluidIngredients() && recipe.isCreativeIceCream()) {
            MutableComponent text = Component.translatable("jade.gas.infinity_mark");
            graphics.drawString(font, text, getBackground().getWidth() / 2 - font.width(text) / 2 - 12, 22, COLOR, false);
            cooler.draw(graphics, getBackground().getWidth() / 2 + 44, 18);
            return;
        }

        MutableComponent time = CCBLang.secondsWithGameTicks(recipe.getProcessingDuration(), 20).component();
        graphics.drawString(font, time, getBackground().getWidth() / 2 - font.width(time) / 2 - 12, 22, COLOR, false);
        cooler.draw(graphics, getBackground().getWidth() / 2 + 44, 18);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, @NotNull CoolingRecipe recipe, @NotNull IFocusGroup focuses) {
        if (recipe.isFluidIngredients()) {
            addFluidInputSlot(builder, recipe.getFluidIngredient());
        }
        else {
            addItemInputSlot(builder, recipe.getIngredient());
        }
    }

    private static void addItemInputSlot(@NotNull IRecipeLayoutBuilder builder, Ingredient ingredient) {
        builder.addSlot(RecipeIngredientRole.INPUT, 16, 27).setBackground(getRenderedSlot(), -1, -1).addIngredients(ingredient);
    }

    private static void addFluidInputSlot(@NotNull IRecipeLayoutBuilder builder, @NotNull SizedFluidIngredient fluidIngredient) {
        builder.addSlot(RecipeIngredientRole.INPUT, 16, 27).setFluidRenderer(1000, false, 16, 16).setBackground(getRenderedSlot(), -1, -1).addIngredients(NeoForgeTypes.FLUID_STACK, Arrays.asList(fluidIngredient.getFluids()));
    }
}
