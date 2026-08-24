package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.HandheldDrillContainerProtectionButton;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.HandheldDrillFilterButton;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record AirtightHandheldDrillMiningContext(Level level, BlockPos basePos, Set<BlockPos> totalPos, Set<BlockPos> protectedPos, Set<BlockPos> unbreakablePos, Set<BlockPos> liquidPos, Set<BlockPos> instantDestructionPos, Set<BlockPos> destructionPos, Set<BlockPos> breakSpeedPos, float baseHardness, float totalBreakHardness) {
    static AirtightHandheldDrillMiningContext of(ItemStack drill, BlockPos basePos, Level level) {
        return of(drill, basePos, level, level.getBlockState(basePos));
    }

    static AirtightHandheldDrillMiningContext of(ItemStack drill, BlockPos basePos, Level level, BlockState baseState) {
        Set<BlockPos> totalPos = getTotalPos(drill, basePos, level, baseState);
        Set<BlockPos> protectedPos = new LinkedHashSet<>();
        Set<BlockPos> unbreakablePos = new LinkedHashSet<>();
        Set<BlockPos> liquidPos = new LinkedHashSet<>();
        Set<BlockPos> instantDestructionPos = new LinkedHashSet<>();
        Set<BlockPos> destructionPos = new LinkedHashSet<>();
        Set<BlockPos> breakSpeedPos = new LinkedHashSet<>();
        FilterItemStack filter = getFilter(drill);
        Map<Item, Boolean> filterMatches = filter == null ? null : new HashMap<>();
        boolean shouldBreakContainers = HandheldDrillContainerProtectionButton.INSTANCE.canApply(drill);
        float baseHardness = 0;
        float totalBreakHardness = 0;
        for (BlockPos targetPos : totalPos) {
            BlockState blockState = level.getBlockState(targetPos);
            if (blockState.isAir()) {
                continue;
            }

            boolean isProtectedTarget = isProtected(level, targetPos, blockState, filter, filterMatches, shouldBreakContainers);
            if (isProtectedTarget) {
                protectedPos.add(targetPos);
            }

            float blockHardness = blockState.getDestroySpeed(level, targetPos);
            if (targetPos.equals(basePos)) {
                baseHardness = blockHardness;
            }

            boolean isUnbreakable = blockHardness == -1;
            boolean isLiquid = blockState.getBlock() instanceof LiquidBlock;
            boolean isInstantDestruction = blockHardness == 0;
            if (isUnbreakable) {
                unbreakablePos.add(targetPos);
            }
            if (isLiquid) {
                liquidPos.add(targetPos);
            }
            if (isInstantDestruction) {
                instantDestructionPos.add(targetPos);
            }

            if (isProtectedTarget || isUnbreakable) {
                continue;
            }

            destructionPos.add(targetPos);
            if (isLiquid || isInstantDestruction) {
                continue;
            }

            breakSpeedPos.add(targetPos);
            totalBreakHardness += Math.max(0, blockHardness);
        }
        return new AirtightHandheldDrillMiningContext(level, basePos, immutableView(totalPos), immutableView(protectedPos), immutableView(unbreakablePos), immutableView(liquidPos), immutableView(instantDestructionPos), immutableView(destructionPos), immutableView(breakSpeedPos), baseHardness, totalBreakHardness);
    }

    private static @Nullable FilterItemStack getFilter(ItemStack drill) {
        if (!HandheldDrillFilterButton.INSTANCE.canApply(drill)) {
            return null;
        }

        ItemContainerContents upgradeInventory = drill.get(CCBDataComponents.AIRTIGHT_UPGRADABLE_INVENTORY);
        if (upgradeInventory == null || upgradeInventory.getSlots() <= AirtightHandheldDrillMenu.FILTER_SLOT_INDEX) {
            return null;
        }

        ItemStack filterStack = upgradeInventory.getStackInSlot(AirtightHandheldDrillMenu.FILTER_SLOT_INDEX);
        return filterStack.isEmpty() ? null : FilterItemStack.of(filterStack);
    }

    private static boolean isProtected(Level level, BlockPos blockPos, BlockState blockState, @Nullable FilterItemStack filter, @Nullable Map<Item, Boolean> filterMatches, boolean shouldBreakContainers) {
        boolean matchesFilter = false;
        if (filter != null && filterMatches != null) {
            Item blockItem = blockState.getBlock().asItem();
            matchesFilter = filterMatches.computeIfAbsent(blockItem, filterItem -> filter.test(level, new ItemStack(filterItem)));
        }
        return matchesFilter || !shouldBreakContainers && level.getCapability(ItemHandler.BLOCK, blockPos, null) != null;
    }

    private static Set<BlockPos> getTotalPos(ItemStack drill, BlockPos basePos, Level level, BlockState baseState) {
        return AirtightHandheldDrillUtils.getMiningTemplate(drill).getTemplate().getTargetPositions(drill, basePos, level, baseState);
    }

    private static @UnmodifiableView Set<BlockPos> immutableView(Set<BlockPos> positions) {
        return Collections.unmodifiableSet(positions);
    }

    boolean isValidBaseTarget() {
        return destructionPos.contains(basePos);
    }

    boolean isEmpty() {
        return destructionPos.isEmpty();
    }
}
