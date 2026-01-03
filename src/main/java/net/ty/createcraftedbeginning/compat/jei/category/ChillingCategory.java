package net.ty.createcraftedbeginning.compat.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.recipe.ChillingRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class ChillingCategory extends CCBRecipeCategory<ChillingRecipe> {
    private static final int SCALE = 24;

    public ChillingCategory(@NotNull Info<ChillingRecipe> info) {
        super(info);
    }

    public static @NotNull Supplier<ItemStack> getCatalystStack() {
        ItemStack stack = AllBlocks.ENCASED_FAN.asStack();
        stack.set(DataComponents.CUSTOM_NAME, CCBLang.translateDirect("recipe.fan_chilling.fan").withStyle(style -> style.withItalic(false)));
        return () -> stack;
    }

    @Override
    protected void draw(@NotNull ChillingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        int size = recipe.getRollableResultsAsItemStacks().size();
        int xOffsetAmount = 1 - Math.min(3, size);
        AllGuiTextures.JEI_SHADOW.render(graphics, 46, 29);
        AllGuiTextures.JEI_SHADOW.render(graphics, 65, 39);
        AllGuiTextures.JEI_LONG_ARROW.render(graphics, 7 * xOffsetAmount + 54, 51);
        PoseStack matrixStack = graphics.pose();

        matrixStack.pushPose();
        matrixStack.translate(56, 33, 0);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-12.5f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));
        AnimatedKinetics.defaultBlockElement(AllPartialModels.ENCASED_FAN_INNER).rotateBlock(180, 0, AnimatedKinetics.getCurrentAngle() * 16).scale(SCALE).render(graphics);
        AnimatedKinetics.defaultBlockElement(AllBlocks.ENCASED_FAN.getDefaultState()).rotateBlock(0, 180, 0).atLocal(0, 0, 0).scale(SCALE).render(graphics);
        AnimatedKinetics.defaultBlockElement(CCBBlocks.BREEZE_COOLER_BLOCK.getDefaultState()).atLocal(0, 0, 2).scale(SCALE).render(graphics);
        AnimatedKinetics.defaultBlockElement(CCBPartialModels.BREEZE_COOLER_WIND).rotateBlock(0, AnimatedKinetics.getCurrentAngle() * 4, 0).atLocal(0, 0, 2).scale(SCALE).render(graphics);
        AnimatedKinetics.defaultBlockElement(CCBPartialModels.BREEZE_CHILLED).rotateBlock(0, 180, 0).atLocal(0, 0, 2).scale(SCALE).render(graphics);

        matrixStack.popPose();
    }

    @Override
    protected void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull ChillingRecipe recipe, IFocusGroup focuses) {
        List<ProcessingOutput> results = recipe.getRollableResults();
        int xOffsetAmount = 1 - Math.min(3, results.size());
        builder.addSlot(RecipeIngredientRole.INPUT, 5 * xOffsetAmount + 21, 48).setBackground(getRenderedSlot(), -1, -1).addIngredients(recipe.getIngredients().getFirst());
        boolean excessive = results.size() > 9;
        int i = 0;
        for (ProcessingOutput output : results) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 141 + i % 3 * 19 + 9 * xOffsetAmount, 48 + i / 3 * -19 + (excessive ? 8 : 0)).setBackground(getRenderedSlot(output), -1, -1).addItemStack(output.getStack()).addRichTooltipCallback(CreateRecipeCategory.addStochasticTooltip(output));
            i++;
        }
    }
}
