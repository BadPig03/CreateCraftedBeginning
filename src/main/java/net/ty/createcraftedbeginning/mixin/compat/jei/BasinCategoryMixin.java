package net.ty.createcraftedbeginning.mixin.compat.jei;

import com.simibubi.create.compat.jei.category.BasinCategory;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.createmod.catnip.theme.Color;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.compat.jei.category.animations.AnimatedBreezeCooler;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.recipe.ChilledBasinProcessing;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = BasinCategory.class, remap = false)
public abstract class BasinCategoryMixin {
    @Unique
    private static final AnimatedBreezeCooler CCB$BREEZE_COOLER = new AnimatedBreezeCooler();

    @SuppressWarnings("MethodMayBeStatic")
    @Inject(method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lcom/simibubi/create/content/processing/basin/BasinRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V", at = @At("TAIL"))
    private void ccb$setRecipe(IRecipeLayoutBuilder builder, BasinRecipe recipe, IFocusGroup focuses, CallbackInfo callback) {
        if (!ChilledBasinProcessing.isChilledRecipe(recipe)) {
            return;
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, 134, 81).addItemStack(new ItemStack(CCBBlocks.BREEZE_COOLER_BLOCK));
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "draw(Lcom/simibubi/create/content/processing/basin/BasinRecipe;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/client/gui/GuiGraphics;DD)V", at = @At("HEAD"), cancellable = true)
    private void ccb$draw(BasinRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY, CallbackInfo callback) {
        if (!ChilledBasinProcessing.isChilledRecipe(recipe)) {
            return;
        }

        int outputRows = (1 + recipe.getFluidResults().size() + recipe.getRollableResults().size()) / 2;
        if (outputRows <= 2) {
            AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, 51 - 19 * outputRows);
        }

        TemperatureCondition condition = TemperatureCondition.CHILLED;
        int color = condition.getColor();
        AllGuiTextures.JEI_HEAT_BAR.render(graphics, 4, 80, new Color(color));
        AllGuiTextures.JEI_LIGHT.render(graphics, 81, 88);
        graphics.drawString(Minecraft.getInstance().font, CCBLang.translateDirect(condition.getTranslationKey()), 9, 86, color, false);

        int centerX = ((BasinCategory) (Object) this).getBackground().getWidth() / 2 + 3;
        CCB$BREEZE_COOLER.drawForBasin(graphics, centerX, 55);
        callback.cancel();
    }
}
