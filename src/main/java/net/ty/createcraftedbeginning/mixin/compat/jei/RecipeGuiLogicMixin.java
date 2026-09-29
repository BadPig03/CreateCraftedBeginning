package net.ty.createcraftedbeginning.mixin.compat.jei;

import com.llamalad7.mixinextras.sugar.Local;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.compat.jei.category.FractionationTowerCategory;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Pseudo
@Mixin(targets = "mezz.jei.gui.recipes.RecipeGuiLogic", remap = false)
public abstract class RecipeGuiLogicMixin {
    @Shadow
    private @Nullable IRecipeCategory<?> cachedRecipeCategory;

    @Shadow
    public abstract IRecipeCategory<?> getSelectedRecipeCategory();

    @Inject(method = "getVisibleRecipeLayoutsWithButtons", at = @At("HEAD"))
    private void ccb$getVisibleRecipeLayoutsWithButtons(CallbackInfoReturnable<List<?>> callback, @Local(argsOnly = true, ordinal = 0) int availableHeight, @Local(argsOnly = true, ordinal = 1) int minRecipePadding) {
        if (!(getSelectedRecipeCategory() instanceof FractionationTowerCategory category) || !category.updateAvailableHeight(availableHeight, minRecipePadding)) {
            return;
        }

        cachedRecipeCategory = null;
    }

    @ModifyArg(method = "getVisibleRecipeLayoutsWithButtons", at = @At(value = "INVOKE", target = "Lmezz/jei/gui/recipes/lookups/ILookupState;setRecipesPerPage(I)V"), index = 0)
    private int ccb$limitFractionationTowerRecipesPerPage(int recipesPerPage) {
        if (!(getSelectedRecipeCategory() instanceof FractionationTowerCategory)) {
            return recipesPerPage;
        }

        return Math.min(2, recipesPerPage);
    }
}
