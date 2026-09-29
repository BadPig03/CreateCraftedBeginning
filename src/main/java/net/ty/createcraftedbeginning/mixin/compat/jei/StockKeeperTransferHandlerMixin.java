package net.ty.createcraftedbeginning.mixin.compat.jei;

import com.simibubi.create.compat.jei.StockKeeperTransferHandler;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.ty.createcraftedbeginning.compat.jei.stockkeeper.StockKeeperTransfers;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@SuppressWarnings("MethodMayBeStatic")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = StockKeeperTransferHandler.class, remap = false)
public abstract class StockKeeperTransferHandlerMixin {
    @Inject(method = "transferRecipeOnClient", at = @At("HEAD"), cancellable = true, order = 900)
    private void ccb$transferRecipeOnClient(StockKeeperRequestMenu container, RecipeHolder<Recipe<?>> recipeHolder, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer, CallbackInfoReturnable<@Nullable IRecipeTransferError> callback) {
        if (!(StockKeeperTransfers.containsGasIngredient(recipeSlots, RecipeIngredientRole.INPUT) || StockKeeperTransfers.containsGasIngredient(recipeSlots, RecipeIngredientRole.OUTPUT))) {
            return;
        }

        callback.setReturnValue(StockKeeperTransfers.transfer(container, recipeHolder.value(), recipeSlots, player, maxTransfer, doTransfer));
    }
}
