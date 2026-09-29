package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipeLookup;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipeLookup.RecipeMatch;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlanner;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasRecipePressureSpeed;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.CANISTER;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.FAN_PROCESSING;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType.ITEM_RECIPE;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberOperationPlanner {
    private final GasInjectionChamberBlockEntity chamber;
    private final GasInjectionChamberFilterState filter;

    @Internal
    public GasInjectionChamberOperationPlanner(GasInjectionChamberBlockEntity chamber, GasInjectionChamberFilterState filter) {
        this.chamber = chamber;
        this.filter = filter;
    }

    @Internal
    public Optional<BeltPlan> createRecipePlan(ItemStack itemStack, @Nullable GasInjectionRecipe recipe) {
        if (recipe == null) {
            return Optional.empty();
        }

        GasStack tankGas = chamber.getGasInTank();
        return Optional.ofNullable(createRecipePlan(itemStack, tankGas, recipe));
    }

    Optional<BeltPlan> createPlan(ItemStack itemStack) {
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

    boolean wasProcessedByInstalledFilter(TransportedItemStack transported) {
        return transported.processedBy != null && transported.processingTime == -1 && filter.getFanProcessingType().flatMap(GasInjectionChamberFilterItem::getFanProcessingType).filter(type -> type == transported.processedBy).isPresent();
    }

    private @Nullable BeltPlan createCanisterPlan(ItemStack itemStack, GasStack tankGas) {
        GasCanisterContainer canisterContents = itemStack.getCapability(CanisterCapabilities.ITEM);
        if (canisterContents == null) {
            return null;
        }

        long injectableAmount = GasInjectionChamberCanisterTransfer.getTransferableAmount(chamber.getGasTank(), canisterContents, tankGas, tankGas.getAmount());
        if (injectableAmount <= 0) {
            return null;
        }

        return new BeltPlan(CANISTER, itemStack.copyWithCount(1), tankGas.copy(), injectableAmount, -1, null, null, null);
    }

    private @Nullable BeltPlan createRecipePlan(ItemStack itemStack, GasStack tankGas) {
        Level level = chamber.getLevel();
        if (level == null) {
            return null;
        }

        Optional<RecipeMatch> recipeMatch = new GasInjectionRecipeLookup(level, chamber.getGasTank()).findRecipeMatch(itemStack);
        return recipeMatch.map(match -> createRecipePlan(itemStack, tankGas, match.recipe())).orElse(null);
    }

    private @Nullable BeltPlan createRecipePlan(ItemStack itemStack, GasStack tankGas, GasInjectionRecipe recipe) {
        Level level = chamber.getLevel();
        if (level == null || itemStack.isEmpty() || tankGas.isEmpty() || !recipe.canProcessOnBelt() || !recipe.matches(new SingleRecipeInput(itemStack), level)) {
            return null;
        }

        int desiredCount = Math.min(itemStack.getCount(), itemStack.getMaxStackSize());
        int batchSize = GasConsumptionPlanner.findMaximumMultiplier(recipe.getGasRequirement(), chamber.getGasTank(), desiredCount);
        if (batchSize <= 0) {
            return null;
        }

        return GasConsumptionPlanner.plan(recipe.getGasRequirement(), chamber.getGasTank(), batchSize).map(plan -> new BeltPlan(ITEM_RECIPE, itemStack.copyWithCount(batchSize), tankGas.copy(), 0, -1, plan, recipe, null)).orElse(null);
    }

    private @Nullable BeltPlan createFanProcessingPlan(ItemStack itemStack, GasStack tankGas) {
        Level level = chamber.getLevel();
        if (level == null || itemStack.isEmpty() || tankGas.isEmpty()) {
            return null;
        }

        Optional<ResourceLocation> fanProcessingTypeId = filter.getFanProcessingType();
        if (fanProcessingTypeId.isEmpty()) {
            return null;
        }

        Optional<FanProcessingType> processingType = GasInjectionChamberFilterItem.getFanProcessingType(fanProcessingTypeId.get());
        if (processingType.isEmpty() || !processingType.get().canProcess(itemStack, level)) {
            return null;
        }

        long sourcePressurePa = chamber.getGasTank().getPressurePa();
        int desiredCount = Math.min(itemStack.getCount(), itemStack.getMaxStackSize());
        int batchSize = GasInjectionFanCost.getMaxFanProcessingBatchSize(tankGas, sourcePressurePa, desiredCount, chamber.getGasTank().getMaxAmount());
        if (batchSize <= 0) {
            return null;
        }

        long gasCost = GasInjectionFanCost.getFanProcessingGasCost(tankGas, sourcePressurePa, batchSize);
        return new BeltPlan(FAN_PROCESSING, itemStack.copyWithCount(batchSize), tankGas.copy(), gasCost, sourcePressurePa, null, null, fanProcessingTypeId.get());
    }

    @Internal
    public record BeltPlan(OperationType type, ItemStack input, GasStack gas, long requiredGas, long sourcePressurePa, @Nullable GasConsumptionPlan recipeGasPlan, @Nullable GasInjectionRecipe recipe, @Nullable ResourceLocation fanProcessingTypeId) {
        int batchSize() {
            return input.getCount();
        }

        boolean hasRequiredGas() {
            if (recipeGasPlan != null) {
                return recipeGasPlan.canExecute();
            }

            return requiredGas <= 0 || gas.getAmount() >= requiredGas;
        }

        float pressureSpeedMultiplier() {
            if (type == ITEM_RECIPE && recipeGasPlan != null) {
                return GasRecipePressureSpeed.multiplier(recipeGasPlan);
            }

            return 1;
        }

        GasStack gasRequest() {
            if (requiredGas <= 0) {
                return GasStack.EMPTY;
            }

            return gas.copyWithAmount(requiredGas);
        }
    }
}
