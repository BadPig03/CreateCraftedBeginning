package net.ty.createcraftedbeginning.compat.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.crafting.Ingredient;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedBreezeChamber;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe;
import net.ty.createcraftedbeginning.registry.CCBGUITextures;
import org.jetbrains.annotations.NotNull;

public class WindChargingCategory extends CCBRecipeCategory<WindChargingRecipe> {
    private static final int COLOR_NORMAL = 0x888888;
    private static final int COLOR_BAD = 0xAB2222;
    private final AnimatedBreezeChamber chamber = new AnimatedBreezeChamber();

    public WindChargingCategory(Info<WindChargingRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, @NotNull WindChargingRecipe recipe, @NotNull IFocusGroup focuses) {
        addItemInputSlot(builder, recipe.getIngredientsItem());
    }

    @Override
    public void draw(WindChargingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        CCBGUITextures.JEI_SHADOW.render(graphics, 122, 37);
        CCBGUITextures.JEI_LONG_ARROW.render(graphics, 42, 30);
        CCBGUITextures.JEI_WIND_CHARGING_BACKGROUND.render(graphics, 16, 8);

        if (getBackground() == null) {
            return;
        }

        if (recipe.isCreativeIceCream()) {
            MutableComponent text = CCBLang.translateDirect("gui.goggles.infinity_mark");
            graphics.drawString(font, text, getBackground().getWidth() / 2 - font.width(text) / 2 - 12, 22, COLOR_NORMAL, false);
            chamber.draw(graphics, getBackground().getWidth() / 2 + 44, 18);
            return;
        }

        if (recipe.isMilkyItem()) {
            MutableComponent text = CCBLang.translateDirect("gui.goggles.clear_negative_effects");
            graphics.drawString(font, text, getBackground().getWidth() / 2 - font.width(text) / 2 - 15, 22, COLOR_NORMAL, false);
            chamber.draw(graphics, getBackground().getWidth() / 2 + 44, 18);
            return;
        }

        boolean isBadFood = recipe.isBadFood();
        MutableComponent negative = CCBLang.text("-").component();
        MutableComponent time = CCBLang.secondsWithGameTicks(recipe.getResultTime(), 20).component();
        MutableComponent realTime = (isBadFood ? negative : Component.empty()).append(time);
        graphics.drawString(font, realTime, getBackground().getWidth() / 2 - font.width(realTime) / 2 - 12, 22, isBadFood ? COLOR_BAD : COLOR_NORMAL, false);
        chamber.draw(graphics, getBackground().getWidth() / 2 + 44, 18);
    }

    private void addItemInputSlot(@NotNull IRecipeLayoutBuilder builder, Ingredient ingredient) {
        builder.addSlot(RecipeIngredientRole.INPUT, 16, 27).setBackground(getRenderedSlot(), -1, -1).addIngredients(ingredient);
    }
}
