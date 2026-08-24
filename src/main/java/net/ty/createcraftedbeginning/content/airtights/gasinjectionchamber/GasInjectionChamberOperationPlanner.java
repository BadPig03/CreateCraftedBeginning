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
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe.RecipeMatch;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.CANISTER;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.FAN_PROCESSING;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.ITEM_RECIPE;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberOperationPlanner {
    private final GasInjectionChamberBlockEntity chamber;
    private final GasInjectionChamberFilterState filter;

    public GasInjectionChamberOperationPlanner(GasInjectionChamberBlockEntity chamber, GasInjectionChamberFilterState filter) {
        this.chamber = chamber;
        this.filter = filter;
    }

    private static void addResultStack(List<ItemStack> resultStacks, ItemStack stackToAdd) {
        if (stackToAdd.isEmpty()) {
            return;
        }

        ItemStack remainingStack = stackToAdd.copy();
        for (ItemStack existingStack : resultStacks) {
            if (!ItemStack.isSameItemSameComponents(existingStack, remainingStack)) {
                continue;
            }

            int availableSpace = existingStack.getMaxStackSize() - existingStack.getCount();
            if (availableSpace <= 0) {
                continue;
            }

            int movedCount = Math.min(availableSpace, remainingStack.getCount());
            existingStack.grow(movedCount);
            remainingStack.shrink(movedCount);
            if (remainingStack.isEmpty()) {
                return;
            }
        }

        while (!remainingStack.isEmpty()) {
            int splitCount = Math.min(remainingStack.getCount(), remainingStack.getMaxStackSize());
            resultStacks.add(remainingStack.split(splitCount));
        }
    }

    public Optional<BeltPlan> createPlan(ItemStack itemStack) {
        if (chamber.getLevel() == null || itemStack.isEmpty()) {
            return Optional.empty();
        }

        GasStack tankGas = chamber.getGasInTank();
        if (tankGas.isEmpty()) {
            return Optional.empty();
        }

        BeltPlan canisterPlan = createCanisterPlan(itemStack, tankGas);
        if (canisterPlan != null) {
            return Optional.of(canisterPlan);
        }

        BeltPlan recipePlan = createRecipePlan(itemStack, tankGas);
        if (recipePlan != null) {
            return Optional.of(recipePlan);
        }

        return Optional.ofNullable(createFanProcessingPlan(itemStack, tankGas));
    }

    public Optional<List<ItemStack>> createResults(BeltPlan plan) {
        if (chamber.getLevel() == null) {
            return Optional.empty();
        }

        List<ItemStack> resultStacks = new ArrayList<>();
        switch (plan.type()) {
            case ITEM_RECIPE -> {
                GasInjectionRecipe recipe = plan.recipe();
                if (recipe == null) {
                    return Optional.empty();
                }

                for (int resultIndex = 0; resultIndex < plan.batchSize(); resultIndex++) {
                    addResultStack(resultStacks, recipe.rollFirstResult(chamber.getLevel()));
                }
            }
            case FAN_PROCESSING -> {
                ResourceLocation fanProcessingTypeId = plan.fanProcessingTypeId();
                if (!isFanProcessingOperationStillValid(fanProcessingTypeId)) {
                    return Optional.empty();
                }

                Optional<FanProcessingType> processingType = GasInjectionChamberUtils.getFanProcessingType(fanProcessingTypeId);
                if (processingType.isEmpty()) {
                    return Optional.empty();
                }

                List<ItemStack> processedStacks = processingType.get().process(plan.input().copy(), chamber.getLevel());
                if (processedStacks == null) {
                    return Optional.empty();
                }
                processedStacks.forEach(resultStack -> addResultStack(resultStacks, resultStack));
            }
            case CANISTER, BASIN_RECIPE, NONE -> {
                return Optional.of(resultStacks);
            }
        }
        return Optional.of(resultStacks);
    }

    public boolean wasProcessedByInstalledFilter(TransportedItemStack transported) {
        return transported.processedBy != null && transported.processingTime == -1 && filter.getFanProcessingType().flatMap(GasInjectionChamberUtils::getFanProcessingType).filter(type -> type == transported.processedBy).isPresent();
    }

    public boolean isFanProcessingOperationStillValid(@Nullable ResourceLocation typeId) {
        return typeId != null && filter.getFanProcessingType().filter(typeId::equals).isPresent();
    }

    private @Nullable BeltPlan createCanisterPlan(ItemStack itemStack, GasStack tankGas) {
        IGasCanisterContainer canisterContents = itemStack.getCapability(GasHandler.ITEM);
        if (canisterContents == null) {
            return null;
        }

        long injectableAmount = GasCanisterUtils.getInjectableAmount(canisterContents, tankGas, chamber.getGasTank().getCapacity());
        if (injectableAmount <= 0) {
            return null;
        }
        return new BeltPlan(CANISTER, itemStack.copyWithCount(1), tankGas.copy(), injectableAmount, null, null);
    }

    private @Nullable BeltPlan createRecipePlan(ItemStack itemStack, GasStack tankGas) {
        if (chamber.getLevel() == null) {
            return null;
        }

        Optional<RecipeMatch> recipeMatch = GasInjectionRecipe.findRecipeMatch(chamber.getLevel(), itemStack, tankGas);
        if (recipeMatch.isEmpty()) {
            return null;
        }

        GasInjectionRecipe recipe = recipeMatch.get().recipe();
        long gasPerItem = recipe.getGasIngredient().amount();
        int batchSize = getRecipeBatchSize(itemStack, gasPerItem);
        if (batchSize <= 0) {
            return null;
        }
        return new BeltPlan(ITEM_RECIPE, itemStack.copyWithCount(batchSize), tankGas.copy(), gasPerItem * batchSize, recipe, null);
    }

    private @Nullable BeltPlan createFanProcessingPlan(ItemStack itemStack, GasStack tankGas) {
        if (chamber.getLevel() == null || itemStack.isEmpty() || tankGas.isEmpty()) {
            return null;
        }

        Optional<ResourceLocation> fanProcessingTypeId = filter.getFanProcessingType();
        if (fanProcessingTypeId.isEmpty()) {
            return null;
        }

        Optional<FanProcessingType> processingType = GasInjectionChamberUtils.getFanProcessingType(fanProcessingTypeId.get());
        if (processingType.isEmpty() || !processingType.get().canProcess(itemStack, chamber.getLevel())) {
            return null;
        }

        int desiredCount = Math.min(itemStack.getCount(), itemStack.getMaxStackSize());
        int batchSize = GasInjectionChamberUtils.getMaxFanProcessingBatchSize(tankGas, desiredCount, chamber.getGasTank().getCapacity());
        if (batchSize <= 0) {
            return null;
        }

        long gasCost = GasInjectionChamberUtils.getFanProcessingGasCost(tankGas, batchSize);
        return new BeltPlan(FAN_PROCESSING, itemStack.copyWithCount(batchSize), tankGas.copy(), gasCost, null, fanProcessingTypeId.get());
    }

    private int getRecipeBatchSize(ItemStack inputStack, long gasPerItem) {
        if (gasPerItem <= 0) {
            return 0;
        }

        int desiredCount = Math.min(inputStack.getCount(), inputStack.getMaxStackSize());
        return Math.clamp(chamber.getGasTank().getCapacity() / gasPerItem, 0, desiredCount);
    }

    public record BeltPlan(OperationType type, ItemStack input, GasStack gas, long requiredGas, @Nullable GasInjectionRecipe recipe, @Nullable ResourceLocation fanProcessingTypeId) {
        public int batchSize() {
            return input.getCount();
        }

        public boolean hasRequiredGas() {
            return requiredGas <= 0 || gas.getAmount() >= requiredGas;
        }

        public GasStack gasRequest() {
            return requiredGas <= 0 ? GasStack.EMPTY : gas.copyWithAmount(requiredGas);
        }
    }
}
