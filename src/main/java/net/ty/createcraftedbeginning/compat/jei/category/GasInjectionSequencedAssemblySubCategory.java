package net.ty.createcraftedbeginning.compat.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedGasInjectionChamber;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionSequencedAssemblySubCategory extends SequencedAssemblySubCategory {
    private final AnimatedGasInjectionChamber chamber;

    public GasInjectionSequencedAssemblySubCategory() {
        super(25);
        chamber = new AnimatedGasInjectionChamber(false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SequencedRecipe<?> recipe, IFocusGroup focuses, int slotX) {
        GasInjectionRecipe injectionRecipe = (GasInjectionRecipe) recipe.getRecipe();
        GasRecipeRequirement gasRequirement = injectionRecipe.getGasRequirement();
        List<GasStack> gasStacks = Arrays.stream(gasRequirement.getGases()).map(GasStack::copy).toList();
        builder.addSlot(RecipeIngredientRole.INPUT, slotX + 4, 15).setBackground(CCBRecipeCategory.getRenderedSlot(), -1, -1).addIngredients(CCBJEIPlugin.GAS_STACK, gasStacks).addRichTooltipCallback((view, tooltip) -> CCBRecipeCategory.addGasRequirementTooltip(tooltip, gasRequirement));
    }

    @Override
    public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int stepIndex) {
        PoseStack poseStack = graphics.pose();

        chamber.offset = stepIndex;
        poseStack.pushPose();
        poseStack.translate(-7, 50, 0);
        poseStack.scale(0.75F, 0.75F, 0.75F);
        chamber.draw(graphics, getWidth() / 2, 0);

        poseStack.popPose();
    }
}
