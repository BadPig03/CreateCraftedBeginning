package net.ty.createcraftedbeginning.compat.jei.category;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.compat.jei.CCBJEITextures;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedGasInjectionChamber;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

import static com.simibubi.create.compat.jei.category.CreateRecipeCategory.addFluidSlot;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionCategory extends CCBRecipeCategory<GasInjectionRecipe> {
    private final AnimatedGasInjectionChamber depotChamber = new AnimatedGasInjectionChamber(false);
    private final AnimatedGasInjectionChamber basinChamber = new AnimatedGasInjectionChamber(true);

    public GasInjectionCategory(Info<GasInjectionRecipe> info) {
        super(info);
    }

    @Override
    public void draw(GasInjectionRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        CCBJEITextures.JEI_SHADOW.render(graphics, 62, 57);
        CCBJEITextures.JEI_DOWN_ARROW.render(graphics, 126, 29);
        if (!recipe.canProcessOnBelt()) {
            basinChamber.draw(graphics, background.getWidth() / 2 - 13, 22);
            return;
        }

        depotChamber.draw(graphics, background.getWidth() / 2 - 13, 22);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GasInjectionRecipe recipe, IFocusGroup focuses) {
        GasRecipeRequirement gasRequirement = recipe.getGasRequirement();
        List<GasStack> gases = Arrays.stream(gasRequirement.getGases()).map(GasStack::copy).toList();
        builder.addSlot(RecipeIngredientRole.INPUT, 27, 32).setBackground(getRenderedSlot(), -1, -1).addIngredients(CCBJEIPlugin.GAS_STACK, gases).addRichTooltipCallback((view, tooltip) -> addGasRequirementTooltip(tooltip, gasRequirement));
        if (recipe.hasFluidInput()) {
            addFluidSlot(builder, 27, 51, recipe.getFluidIngredient());
        }
        else {
            builder.addSlot(RecipeIngredientRole.INPUT, 27, 51).setBackground(getRenderedSlot(), -1, -1).addIngredients(recipe.getIngredient());
        }

        if (recipe.hasFluidOutput()) {
            addFluidSlot(builder, 132, 51, recipe.getFluidResult());
            return;
        }

        ProcessingOutput output = recipe.getRollableResults().getFirst();
        builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 51).setBackground(getRenderedSlot(output), -1, -1).addItemStack(output.getStack()).addRichTooltipCallback(CreateRecipeCategory.addStochasticTooltip(output));
    }
}
