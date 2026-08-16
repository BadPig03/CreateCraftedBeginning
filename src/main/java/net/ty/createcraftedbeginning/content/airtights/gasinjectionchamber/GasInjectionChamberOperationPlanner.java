package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe.RecipeMatch;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.CANISTER;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.FAN_PROCESSING;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.ITEM_RECIPE;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberOperationPlanner {
    private final GasInjectionChamberBlockEntity chamber;
    private final GasInjectionChamberOperationState operation;
    private final GasInjectionChamberFilterState filter;

    public GasInjectionChamberOperationPlanner(GasInjectionChamberBlockEntity chamber, GasInjectionChamberOperationState operation, GasInjectionChamberFilterState filter) {
        this.chamber = chamber;
        this.operation = operation;
        this.filter = filter;
    }

    private static void addResultStack(List<ItemStack> results, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemStack remaining = stack.copy();
        for (ItemStack existing : results) {
            if (!ItemStack.isSameItemSameComponents(existing, remaining)) {
                continue;
            }

            int space = existing.getMaxStackSize() - existing.getCount();
            if (space <= 0) {
                continue;
            }

            int moved = Math.min(space, remaining.getCount());
            existing.grow(moved);
            remaining.shrink(moved);
            if (remaining.isEmpty()) {
                return;
            }
        }

        while (!remaining.isEmpty()) {
            int count = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            results.add(remaining.split(count));
        }
    }

    public boolean prepareOperation(ItemStack itemStack) {
        if (chamber.getLevel() == null) {
            return false;
        }

        GasStack tankGas = chamber.getGasInTank();
        return !tankGas.isEmpty() && (prepareCanisterOperation(itemStack, tankGas) || prepareRecipeOperation(itemStack, tankGas) || prepareFanProcessingOperation(itemStack, tankGas));
    }

    public boolean prepareOperationResultsIfNeeded(ItemStack itemStack) {
        return operation.resultPrepared || switch (operation.type) {
            case ITEM_RECIPE -> prepareRecipeResults(itemStack);
            case FAN_PROCESSING -> prepareFanProcessingResults();
            case BASIN_RECIPE, CANISTER, NONE -> true;
        };
    }

    public boolean wasProcessedByInstalledFilter(TransportedItemStack transported) {
        return transported.processedBy != null && transported.processingTime == -1 && filter.getFanProcessingType().flatMap(GasInjectionChamberUtils::getFanProcessingType).filter(type -> type == transported.processedBy).isPresent();
    }

    public boolean isFanProcessingOperationStillValid(@Nullable ResourceLocation typeId) {
        return typeId != null && filter.getFanProcessingType().filter(typeId::equals).isPresent();
    }

    private boolean prepareCanisterOperation(ItemStack itemStack, GasStack tankGas) {
        IGasCanisterContainer canister = itemStack.getCapability(GasHandler.ITEM);
        if (canister == null) {
            return false;
        }

        long amount = GasCanisterUtils.getInjectableAmount(canister, tankGas, chamber.getGasTank().getCapacity());
        if (amount <= 0) {
            return false;
        }

        operation.setOperation(CANISTER, itemStack, 1, tankGas, amount, null, null);
        chamber.setChanged();
        return true;
    }

    private boolean prepareRecipeOperation(ItemStack itemStack, GasStack tankGas) {
        if (chamber.getLevel() == null) {
            return false;
        }

        Optional<RecipeMatch> recipeMatch = GasInjectionRecipe.findRecipeMatch(chamber.getLevel(), itemStack, tankGas);
        if (recipeMatch.isEmpty()) {
            return false;
        }

        RecipeMatch match = recipeMatch.get();
        long gasPerItem = match.recipe().getGasIngredient().amount();
        int batchSize = getRecipeBatchSize(itemStack, gasPerItem);
        if (batchSize <= 0) {
            return false;
        }

        operation.setOperation(ITEM_RECIPE, itemStack, batchSize, tankGas, gasPerItem * batchSize, match.sequencedAssembly() ? null : match.recipe(), null);
        chamber.setChanged();
        return true;
    }

    private boolean prepareFanProcessingOperation(ItemStack itemStack, GasStack tankGas) {
        if (chamber.getLevel() == null || itemStack.isEmpty() || tankGas.isEmpty()) {
            return false;
        }

        Optional<ResourceLocation> typeId = filter.getFanProcessingType();
        if (typeId.isEmpty()) {
            return false;
        }

        Optional<FanProcessingType> processingType = GasInjectionChamberUtils.getFanProcessingType(typeId.get());
        if (processingType.isEmpty() || !processingType.get().canProcess(itemStack, chamber.getLevel())) {
            return false;
        }

        int desiredCount = Math.min(itemStack.getCount(), itemStack.getMaxStackSize());
        int batchSize = GasInjectionChamberUtils.getMaxFanProcessingBatchSize(tankGas, desiredCount);
        if (batchSize <= 0) {
            return false;
        }

        long gasCost = GasInjectionChamberUtils.getFanProcessingGasCost(tankGas, batchSize);
        long gasAmount = gasCost == 0 ? 1 : gasCost;
        operation.setOperation(FAN_PROCESSING, itemStack, batchSize, tankGas, gasAmount, null, typeId.get());
        chamber.setChanged();
        return true;
    }

    private int getRecipeBatchSize(ItemStack input, long gasPerItem) {
        if (gasPerItem <= 0) {
            return 0;
        }

        int desiredCount = Math.min(input.getCount(), input.getMaxStackSize());
        return Math.clamp(chamber.getGasTank().getCapacity() / gasPerItem, 0, desiredCount);
    }

    private boolean prepareRecipeResults(ItemStack itemStack) {
        if (chamber.getLevel() == null) {
            return false;
        }

        int inputCount = operation.input.getCount();
        GasInjectionRecipe recipe = operation.recipe;
        if (recipe == null) {
            Optional<RecipeMatch> recipeMatch = GasInjectionRecipe.findRecipeMatch(chamber.getLevel(), itemStack, operation.gas);
            if (recipeMatch.isEmpty()) {
                return false;
            }

            GasInjectionRecipe matchedRecipe = recipeMatch.get().recipe();
            long expectedGas = matchedRecipe.getGasIngredient().amount() * inputCount;
            if (expectedGas != operation.gas.getAmount()) {
                return false;
            }

            recipe = matchedRecipe;
        }

        for (int i = 0; i < inputCount; i++) {
            addResultStack(operation.results, recipe.rollFirstResult(chamber.getLevel()));
        }
        operation.resultPrepared = true;
        operation.recipe = null;
        chamber.setChanged();
        return true;
    }

    private boolean prepareFanProcessingResults() {
        ResourceLocation typeId = operation.fanProcessingTypeId;
        if (chamber.getLevel() == null || !isFanProcessingOperationStillValid(typeId)) {
            return false;
        }

        Optional<FanProcessingType> processingType = GasInjectionChamberUtils.getFanProcessingType(typeId);
        if (processingType.isEmpty()) {
            return false;
        }

        List<ItemStack> results = processingType.get().process(operation.input.copy(), chamber.getLevel());
        if (results == null) {
            return false;
        }

        for (ItemStack result : results) {
            addResultStack(operation.results, result);
        }
        operation.resultPrepared = true;
        chamber.setChanged();
        return true;
    }
}
