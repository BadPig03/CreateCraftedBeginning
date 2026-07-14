package net.ty.createcraftedbeginning.mixin.client.create;

import com.simibubi.create.compat.jei.StockKeeperTransferHandler;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferError.Type;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.ty.createcraftedbeginning.compat.jei.category.stockkeeper.GasCraftableBigItemStack;
import net.ty.createcraftedbeginning.compat.jei.utils.StockKeeperTransferUtils.OutputTarget;
import net.ty.createcraftedbeginning.compat.jei.utils.StockKeeperTransferUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@SuppressWarnings("MethodMayBeStatic")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = StockKeeperTransferHandler.class, remap = false)
public abstract class StockKeeperTransferHandlerMixin {
    @Inject(method = "transferRecipeOnClient", at = @At("HEAD"), cancellable = true)
    private void ccb$transferRecipeOnClient(StockKeeperRequestMenu container, RecipeHolder<Recipe<?>> recipeHolder, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer, CallbackInfoReturnable<@Nullable IRecipeTransferError> cir) {
        if (!(StockKeeperTransferUtils.containsGasIngredient(recipeSlots, RecipeIngredientRole.INPUT) || StockKeeperTransferUtils.containsGasIngredient(recipeSlots, RecipeIngredientRole.OUTPUT))) {
            return;
        }

        if (!(container.screenReference instanceof StockKeeperRequestScreen screen)) {
            cir.setReturnValue(() -> Type.INTERNAL);
            return;
        }

        InventorySummary summary = screen.getMenu().contentHolder.getLastClientsideStockSnapshotAsSummary();
        if (summary == null) {
            cir.setReturnValue(() -> Type.INTERNAL);
            return;
        }

        List<BigItemStack> requirements = StockKeeperTransferUtils.collectRequirements(recipeSlots, summary, screen.itemsToOrder);
        if (requirements == null || requirements.isEmpty()) {
            cir.setReturnValue(StockKeeperTransferUtils.throwError("gui.stock_keeper.not_in_stock"));
            return;
        }

        Recipe<?> recipe = recipeHolder.value();
        OutputTarget outputTarget = StockKeeperTransferUtils.getOutputTarget(recipeSlots, player, recipe);
        if (outputTarget == null) {
            cir.setReturnValue(() -> Type.INTERNAL);
            return;
        }

        GasCraftableBigItemStack existing = screen.recipesToOrder.stream().filter(entry -> entry instanceof GasCraftableBigItemStack gasRecipe && gasRecipe.matches(recipe, outputTarget.displayStack())).map(entry -> (GasCraftableBigItemStack) entry).findFirst().orElse(null);
        boolean isNewEntry = existing == null;
        if (isNewEntry && screen.recipesToOrder.size() >= 9) {
            cir.setReturnValue(StockKeeperTransferUtils.throwError("gui.stock_keeper.slots_full"));
            return;
        }

        GasCraftableBigItemStack entry = existing;
        if (entry == null) {
            entry = new GasCraftableBigItemStack(outputTarget.displayStack(), recipe, outputTarget.outputPerCraft(), outputTarget.transferLimit(), requirements);
        }
        if (!StockKeeperTransferUtils.canFitNewOrderTypes(screen.itemsToOrder, entry.getRequirements())) {
            cir.setReturnValue(StockKeeperTransferUtils.throwError("gui.stock_keeper.slots_full"));
            return;
        }

        int maxSets = StockKeeperTransferUtils.getMaxAdditionalSets(summary, screen.itemsToOrder, entry.getRequirements());
        int requestedSets = maxTransfer ? maxSets : 1;
        if (requestedSets <= 0) {
            cir.setReturnValue(StockKeeperTransferUtils.throwError("gui.stock_keeper.not_in_stock"));
            return;
        }

        if (!doTransfer) {
            cir.setReturnValue(null);
            return;
        }

        if (isNewEntry) {
            screen.recipesToOrder.add(entry);
        }
        if (StockKeeperTransferUtils.requestCraftable(screen, entry, entry.getOutputPerCraft() * requestedSets)) {
            cir.setReturnValue(null);
            return;
        }

        if (isNewEntry) {
            screen.recipesToOrder.remove(entry);
        }
        cir.setReturnValue(StockKeeperTransferUtils.throwError("gui.stock_keeper.not_in_stock"));
    }
}
