package net.ty.createcraftedbeginning.compat.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedGasInjectionChamber;

import static net.ty.createcraftedbeginning.compat.jei.category.CCBRecipeCategory.getRenderedSlot;

public abstract class CCBSequencedAssemblySubCategory extends SequencedAssemblySubCategory {
    public CCBSequencedAssemblySubCategory(int width) {
        super(width);
    }

    public static class AssemblyInjecting extends CCBSequencedAssemblySubCategory {
        AnimatedGasInjectionChamber chamber;

        public AssemblyInjecting() {
            super(25);
            chamber = new AnimatedGasInjectionChamber();
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, SequencedRecipe<?> recipe, IFocusGroup focuses, int x) {
            FluidIngredient fluidIngredient = recipe.getRecipe().getFluidIngredients().getFirst();

            int amount = fluidIngredient.getRequiredAmount();
            builder.addSlot(RecipeIngredientRole.OUTPUT, x + 4, 15).setBackground(getRenderedSlot(), -1, -1).addIngredients(NeoForgeTypes.FLUID_STACK, fluidIngredient.getMatchingFluidStacks()).setFluidRenderer(amount, false, 16, 16);
        }

        @Override
        public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int index) {
            PoseStack ms = graphics.pose();
            chamber.offset = index;
            ms.pushPose();
            ms.translate(-7, 50, 0);
            ms.scale(.75f, .75f, .75f);
            chamber.draw(graphics, getWidth() / 2, 0);
            ms.popPose();
        }
    }
}
