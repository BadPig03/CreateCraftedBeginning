package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationPlanner.BeltPlan;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberBeltOutputs {
    private final GasInjectionChamberBlockEntity chamber;
    private final GasInjectionChamberFilterState filter;

    @Internal
    public GasInjectionChamberBeltOutputs(GasInjectionChamberBlockEntity chamber, GasInjectionChamberFilterState filter) {
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

    @Internal
    public Optional<List<ItemStack>> createResults(BeltPlan plan) {
        Level level = chamber.getLevel();
        if (level == null) {
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
                    addResultStack(resultStacks, recipe.rollFirstResult(level));
                }
            }
            case FAN_PROCESSING -> {
                ResourceLocation fanProcessingTypeId = plan.fanProcessingTypeId();
                if (!isFanProcessingOperationStillValid(fanProcessingTypeId)) {
                    return Optional.empty();
                }

                Optional<FanProcessingType> processingType = GasInjectionChamberFilterItem.getFanProcessingType(fanProcessingTypeId);
                if (processingType.isEmpty()) {
                    return Optional.empty();
                }

                List<ItemStack> processedStacks = processingType.get().process(plan.input().copy(), level);
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

    private boolean isFanProcessingOperationStillValid(@Nullable ResourceLocation typeId) {
        return typeId != null && filter.getFanProcessingType().filter(typeId::equals).isPresent();
    }
}
