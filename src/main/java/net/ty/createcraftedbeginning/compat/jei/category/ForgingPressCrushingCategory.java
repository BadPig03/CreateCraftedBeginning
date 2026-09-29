package net.ty.createcraftedbeginning.compat.jei.category;

import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.ty.createcraftedbeginning.compat.jei.CCBJEITextures;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedAirtightForgingPress;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static com.simibubi.create.compat.jei.category.CreateRecipeCategory.addStochasticTooltip;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ForgingPressCrushingCategory extends CCBRecipeCategory<CrushingRecipe> {
    private final AnimatedAirtightForgingPress forgingPress = new AnimatedAirtightForgingPress();

    public ForgingPressCrushingCategory(Info<CrushingRecipe> info) {
        super(info);
    }

    @Override
    protected void draw(CrushingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        CCBJEITextures.JEI_PRESS_HEAD_TOOL.render(graphics, 24, 43);
        CCBJEITextures.JEI_SHADOW.render(graphics, 66, 66);
        CCBJEITextures.JEI_LONG_ARROW.render(graphics, background.getWidth() / 2 - 35, 86);
        forgingPress.draw(graphics, background.getWidth() / 2 - 8, 58);
    }

    @Override
    protected void setRecipe(IRecipeLayoutBuilder builder, CrushingRecipe recipe, IFocusGroup focuses) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) {
            return;
        }

        builder.addSlot(RecipeIngredientRole.INPUT, 18, 82).setBackground(getRenderedSlot(), -1, -1).addItemStacks(List.of(ingredients.getFirst().getItems()));
        builder.addSlot(RecipeIngredientRole.CATALYST, 42, 45).setBackground(getRenderedSlot(), -1, -1).addItemStack(new ItemStack(Items.HEAVY_CORE)).addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("createcraftedbeginning.recipe.forging_press_auto_crushing.press_head")));
        List<ProcessingOutput> results = recipe.getRollableResults();
        for (int resultIndex = 0; resultIndex < results.size(); resultIndex++) {
            ProcessingOutput result = results.get(resultIndex);
            int slotX = 132 + resultIndex % 2 * 19;
            int slotY = 82 - resultIndex / 2 * 19;
            builder.addSlot(RecipeIngredientRole.OUTPUT, slotX, slotY).setBackground(getRenderedSlot(result), -1, -1).addItemStack(result.getStack()).addRichTooltipCallback(addStochasticTooltip(result));
        }
    }
}
