package net.ty.createcraftedbeginning.compat.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedBreezeCooler;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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
    public void setRecipe(IRecipeLayoutBuilder builder, CoolingRecipe recipe, IFocusGroup focuses) {
        if (recipe.isFluidIngredients()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 16, 27).setFluidRenderer(1000, false, 16, 16).setBackground(getRenderedSlot(), -1, -1).addIngredients(NeoForgeTypes.FLUID_STACK, Arrays.asList(recipe.getFluidIngredient().getFluids()));
            return;
        }

        builder.addSlot(RecipeIngredientRole.INPUT, 16, 27).setBackground(getRenderedSlot(), -1, -1).addIngredients(recipe.getIngredient());
    }
}
