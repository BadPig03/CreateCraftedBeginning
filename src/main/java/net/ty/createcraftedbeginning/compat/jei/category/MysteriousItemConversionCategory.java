package net.ty.createcraftedbeginning.compat.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.ty.createcraftedbeginning.recipe.ConversionRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBGUITextures;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MysteriousItemConversionCategory extends CCBRecipeCategory<ConversionRecipe> {
    public static final List<RecipeHolder<ConversionRecipe>> RECIPES = new ArrayList<>();

    static {
        RECIPES.add(ConversionRecipe.create(CCBBlocks.EMPTY_BREEZE_CHAMBER_BLOCK.asStack(), CCBBlocks.BREEZE_CHAMBER_BLOCK.asStack(), "breeze_chamber"));
    }

    public MysteriousItemConversionCategory(Info<ConversionRecipe> info) {
        super(info);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ConversionRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 17).setBackground(getRenderedSlot(), -1, -1).addIngredients(recipe.getIngredients().getFirst());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 17).setBackground(getRenderedSlot(), -1, -1).addItemStack(recipe.getRollableResults().getFirst().getStack());
    }

    @Override
    public void draw(@NotNull ConversionRecipe recipe, @NotNull IRecipeSlotsView iRecipeSlotsView, @NotNull GuiGraphics graphics, double mouseX, double mouseY) {
        CCBGUITextures.JEI_LONG_ARROW.render(graphics, 52, 20);
        CCBGUITextures.JEI_QUESTION_MARK.render(graphics, 77, 5);
    }
}
