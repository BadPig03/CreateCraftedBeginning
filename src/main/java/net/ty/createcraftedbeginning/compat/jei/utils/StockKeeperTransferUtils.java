package net.ty.createcraftedbeginning.compat.jei.utils;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.foundation.utility.CreateLang;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.client.stockkeeper.StockKeeperCraftingUtils;
import net.ty.createcraftedbeginning.compat.fluidlogistics.FluidLogisticsStockKeeperCompat;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class StockKeeperTransferUtils {
    private StockKeeperTransferUtils() {
    }

    public static @Nullable List<BigItemStack> collectRequirements(IRecipeSlotsView recipeSlots, InventorySummary summary, List<BigItemStack> existingOrders) {
        List<BigItemStack> requirements = new ArrayList<>();
        for (IRecipeSlotView slotView : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
            List<BigItemStack> candidates = getCandidates(slotView);
            if (candidates.isEmpty()) {
                continue;
            }

            BigItemStack chosen = chooseCandidate(candidates, summary, requirements, existingOrders);
            if (chosen == null) {
                return null;
            }

            StockKeeperCraftingUtils.mergeRequirement(requirements, chosen);
        }
        return requirements;
    }

    public static @Nullable OutputTarget getOutputTarget(IRecipeSlotsView recipeSlots, Player player, Recipe<?> recipe) {
        for (IRecipeSlotView slotView : recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT)) {
            Optional<ItemStack> itemOutput = slotView.getItemStacks().filter(stack -> !stack.isEmpty()).findFirst();
            if (itemOutput.isPresent()) {
                ItemStack stack = itemOutput.get();
                return new OutputTarget(stack.copyWithCount(1), Math.max(1, stack.getCount()), Math.max(1, stack.getMaxStackSize()));
            }

            Optional<FluidStack> fluidOutput = slotView.getIngredients(NeoForgeTypes.FLUID_STACK).filter(fluid -> fluid != null && !fluid.isEmpty()).findFirst();
            if (fluidOutput.isPresent() && FluidLogisticsStockKeeperCompat.isAvailable()) {
                FluidStack fluid = fluidOutput.get();
                ItemStack fluidKey = FluidLogisticsStockKeeperCompat.createFluidKey(fluid);
                if (fluidKey.isEmpty() || fluid.getAmount() <= 0) {
                    return null;
                }

                int outputAmount = fluid.getAmount();
                return new OutputTarget(fluidKey, outputAmount, FluidLogisticsStockKeeperCompat.getFluidPerPackage(outputAmount));
            }

            Optional<GasStack> gasOutput = slotView.getIngredients(CCBJEIPlugin.GAS_STACK).filter(gas -> gas != null && !gas.isEmpty()).findFirst();
            if (gasOutput.isPresent()) {
                GasStack gas = gasOutput.get();
                int outputAmount = GasRequestUtils.toLogisticsAmount(gas.getAmount());
                if (outputAmount <= 0) {
                    return null;
                }

                ItemStack virtualGas = GasVirtualUtils.createVirtualItem(gas.copyWithAmount(1));
                if (virtualGas.isEmpty()) {
                    return null;
                }

                return new OutputTarget(virtualGas.copyWithCount(1), outputAmount, outputAmount);
            }
        }

        ItemStack result = recipe.getResultItem(player.level().registryAccess());
        if (result.isEmpty()) {
            return null;
        }
        return new OutputTarget(result.copyWithCount(1), Math.max(1, result.getCount()), Math.max(1, result.getMaxStackSize()));
    }

    public static IRecipeTransferError throwError(String key) {
        return new IRecipeTransferError() {
            @Override
            public Type getType() {
                return Type.USER_FACING;
            }

            @Override
            public void getTooltip(ITooltipBuilder tooltip) {
                tooltip.add(CreateLang.translate(key).component());
            }
        };
    }

    public static boolean containsGasIngredient(IRecipeSlotsView recipeSlots, RecipeIngredientRole role) {
        return recipeSlots.getSlotViews(role).stream().anyMatch(slot -> slot.getIngredients(CCBJEIPlugin.GAS_STACK).anyMatch(gas -> gas != null && !gas.isEmpty()));
    }

    private static @Nullable BigItemStack chooseCandidate(List<BigItemStack> candidates, InventorySummary summary, List<BigItemStack> selectedRequirements, List<BigItemStack> existingOrders) {
        BigItemStack best = null;
        int bestBatches = 0;
        boolean bestAlreadyUsed = false;
        for (BigItemStack candidate : candidates) {
            int alreadySelected = StockKeeperCraftingUtils.getMatchingCount(selectedRequirements, candidate.stack);
            int alreadyOrdered = StockKeeperCraftingUtils.getMatchingCount(existingOrders, candidate.stack);
            int available = summary.getCountOf(candidate.stack) - alreadySelected - alreadyOrdered;
            if (available < candidate.count) {
                continue;
            }

            int batches = available / candidate.count;
            boolean alreadyUsed = alreadySelected > 0 || alreadyOrdered > 0;
            if (best == null) {
                best = candidate;
                bestBatches = batches;
                bestAlreadyUsed = alreadyUsed;
                continue;
            }

            if (alreadyUsed && !bestAlreadyUsed) {
                best = candidate;
                bestBatches = batches;
                bestAlreadyUsed = true;
                continue;
            }

            if (alreadyUsed != bestAlreadyUsed || batches <= bestBatches) {
                continue;
            }

            best = candidate;
            bestBatches = batches;
        }
        if (best == null) {
            return null;
        }
        return new BigItemStack(best.stack.copyWithCount(1), best.count);
    }

    private static List<BigItemStack> getCandidates(IRecipeSlotView slotView) {
        List<BigItemStack> candidates = new ArrayList<>();
        addItemCandidates(slotView, candidates);
        addFluidCandidates(slotView, candidates);
        addGasCandidates(slotView, candidates);
        return candidates;
    }

    private static void addItemCandidates(IRecipeSlotView slotView, List<BigItemStack> candidates) {
        slotView.getItemStacks().forEach(stack -> {
            if (!stack.isEmpty()) {
                candidates.add(new BigItemStack(stack.copyWithCount(1), Math.max(1, stack.getCount())));
            }
        });
    }

    private static void addFluidCandidates(IRecipeSlotView slotView, List<BigItemStack> candidates) {
        if (!FluidLogisticsStockKeeperCompat.isAvailable()) {
            return;
        }

        slotView.getIngredients(NeoForgeTypes.FLUID_STACK).forEach(fluid -> {
            if (fluid == null || fluid.isEmpty() || fluid.getAmount() <= 0) {
                return;
            }

            ItemStack fluidKey = FluidLogisticsStockKeeperCompat.createFluidKey(fluid);
            if (!fluidKey.isEmpty()) {
                candidates.add(new BigItemStack(fluidKey, fluid.getAmount()));
            }
        });
    }

    private static void addGasCandidates(IRecipeSlotView slotView, List<BigItemStack> candidates) {
        slotView.getIngredients(CCBJEIPlugin.GAS_STACK).forEach(gas -> {
            if (gas == null || gas.isEmpty()) {
                return;
            }

            int amount = GasRequestUtils.toLogisticsAmount(gas.getAmount());
            if (amount <= 0) {
                return;
            }

            ItemStack virtualGas = GasVirtualUtils.createVirtualItem(gas.copyWithAmount(1));
            if (!virtualGas.isEmpty()) {
                candidates.add(new BigItemStack(virtualGas.copyWithCount(1), amount));
            }
        });
    }

    public record OutputTarget(ItemStack displayStack, int outputPerCraft, int transferLimit) {}
}
