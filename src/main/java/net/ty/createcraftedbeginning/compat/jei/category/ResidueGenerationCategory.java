package net.ty.createcraftedbeginning.compat.jei.category;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedAirtightEngine;
import net.ty.createcraftedbeginning.foundation.client.CCBGUITextures;
import net.ty.createcraftedbeginning.recipe.ResidueGenerationRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResidueGenerationCategory extends CCBRecipeCategory<ResidueGenerationRecipe> {
    private static final int NO_RESIDUE_TEXT_COLOR = 0x888888;
    protected final AnimatedAirtightEngine engine = new AnimatedAirtightEngine();

    public ResidueGenerationCategory(Info<ResidueGenerationRecipe> info) {
        super(info);
    }

    @Override
    protected void draw(ResidueGenerationRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        CCBGUITextures.JEI_SHADOW.render(graphics, 61, 54);
        CCBGUITextures.JEI_LONG_ARROW.render(graphics, 52, 77);
        engine.draw(graphics, background.getWidth() / 2 - 13, 39);
        if (recipe.hasResidueOutput()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        Component text = Component.translatable("createcraftedbeginning.recipe.residue_generation.no_residue");
        graphics.drawString(font, text, 141 - font.width(text) / 2, 94, NO_RESIDUE_TEXT_COLOR, false);
    }

    @Override
    protected void setRecipe(IRecipeLayoutBuilder builder, ResidueGenerationRecipe recipe, IFocusGroup focuses) {
        List<GasStack> gases = Arrays.stream(recipe.getIngredientsGas().getGases()).map(GasStack::copy).toList();
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 74).setBackground(getRenderedSlot(), -1, -1).addIngredients(CCBJEIPlugin.GAS_STACK, gases);
        IRecipeSlotBuilder outputSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 74).setBackground(getRenderedSlot(), -1, -1);
        if (!recipe.hasResidueOutput()) {
            return;
        }
        if (recipe.getFluidResults().isEmpty()) {
            outputSlot.addItemStack(getResultItem(recipe));
            return;
        }

        outputSlot.addIngredient(NeoForgeTypes.FLUID_STACK, recipe.getFluidResults().getFirst()).setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 16);
    }
}
