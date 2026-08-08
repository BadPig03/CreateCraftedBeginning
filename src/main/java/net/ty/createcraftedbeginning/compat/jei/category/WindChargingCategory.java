package net.ty.createcraftedbeginning.compat.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.createmod.catnip.data.TriState;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.crafting.Ingredient;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedBreezeChamber;
import net.ty.createcraftedbeginning.foundation.client.CCBGUITextures;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe.WindChargingAction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WindChargingCategory extends CCBRecipeCategory<WindChargingRecipe> {
    private static final int COLOR_NORMAL = 0x888888;
    private static final int COLOR_BAD = 0xAB2222;
    private final AnimatedBreezeChamber galeChamber = new AnimatedBreezeChamber(TriState.FALSE);
    private final AnimatedBreezeChamber illChamber = new AnimatedBreezeChamber(TriState.TRUE);
    private final AnimatedBreezeChamber calmChamber = new AnimatedBreezeChamber(TriState.DEFAULT);

    public WindChargingCategory(Info<WindChargingRecipe> info) {
        super(info);
    }

    private static void addItemInputSlot(IRecipeLayoutBuilder builder, Ingredient ingredient) {
        builder.addSlot(RecipeIngredientRole.INPUT, 16, 27).setBackground(getRenderedSlot(), -1, -1).addIngredients(ingredient);
    }

    @Override
    public void draw(WindChargingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        int chamberX = getBackground().getWidth() / 2 + 44;
        CCBGUITextures.JEI_SHADOW.render(graphics, 122, 37);
        CCBGUITextures.JEI_LONG_ARROW.render(graphics, 42, 30);
        CCBGUITextures.JEI_WIND_CHARGING_BACKGROUND.render(graphics, 16, 8);
        if (recipe.getAction() == WindChargingAction.CYCLE_CREATIVE) {
            MutableComponent text = Component.translatable("jade.gas.infinity_mark");
            graphics.drawString(font, text, getBackground().getWidth() / 2 - font.width(text) / 2 - 12, 22, COLOR_NORMAL, false);
            galeChamber.draw(graphics, chamberX, 18);
            return;
        }

        if (recipe.getAction() == WindChargingAction.CLEAR_ILL) {
            MutableComponent text = CCBLang.translateDirect("gui.clear_ill_state");
            graphics.drawString(font, text, getBackground().getWidth() / 2 - font.width(text) / 2 - 15, 22, COLOR_NORMAL, false);
            calmChamber.draw(graphics, chamberX, 18);
            return;
        }

        boolean isBadFood = recipe.isBadFood();
        MutableComponent time = isBadFood ? CCBLang.text("-").component() : Component.empty();
        time.append(CCBLang.secondsWithGameTicks(Math.abs(recipe.getProcessingDuration()), 20).component());
        int textX = getBackground().getWidth() / 2 - font.width(time) / 2 - 12;
        int color = isBadFood ? COLOR_BAD : COLOR_NORMAL;
        graphics.drawString(font, time, textX, 22, color, false);
        if (isBadFood) {
            illChamber.draw(graphics, chamberX, 18);
        }
        else {
            galeChamber.draw(graphics, chamberX, 18);
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, WindChargingRecipe recipe, IFocusGroup focuses) {
        addItemInputSlot(builder, recipe.getIngredient());
    }
}
