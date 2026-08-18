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
        boolean breakContainers = HandheldDrillContainerProtectionButton.INSTANCE.canApply(drill);
        float baseHardness = 0;
        float totalBreakHardness = 0;
        for (BlockPos pos : totalPos) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            boolean isProtected = isProtected(level, pos, state, filter, filterMatches, breakContainers);
            if (isProtected) {
                protectedPos.add(pos);
            }

            float destroySpeed = state.getDestroySpeed(level, pos);
            if (pos.equals(basePos)) {
                baseHardness = destroySpeed;
            }

            boolean isUnbreakable = destroySpeed == -1;
            boolean isLiquid = state.getBlock() instanceof LiquidBlock;
            boolean isInstantDestruction = destroySpeed == 0;
            if (isUnbreakable) {
                unbreakablePos.add(pos);
            }
            if (isLiquid) {
                liquidPos.add(pos);
            }
            if (isInstantDestruction) {
                instantDestructionPos.add(pos);
            }

            if (isProtected || isUnbreakable) {
                continue;
            }

            destructionPos.add(pos);
            if (isLiquid || isInstantDestruction) {
                continue;
            }

            breakSpeedPos.add(pos);
            totalBreakHardness += Math.max(0, destroySpeed);
        }
        return new AirtightHandheldDrillMiningContext(level, basePos, immutableView(totalPos), immutableView(protectedPos), immutableView(unbreakablePos), immutableView(liquidPos), immutableView(instantDestructionPos), immutableView(destructionPos), immutableView(breakSpeedPos), baseHardness, totalBreakHardness);
    }

    private static @Nullable FilterItemStack getFilter(ItemStack drill) {
        if (!HandheldDrillFilterButton.INSTANCE.canApply(drill)) {
            return null;
        }

        ItemContainerContents contents = drill.get(CCBDataComponents.AIRTIGHT_UPGRADABLE_INVENTORY);
        if (contents == null || contents.getSlots() <= AirtightHandheldDrillMenu.FILTER_SLOT_INDEX) {
            return null;
        }

        ItemStack filterStack = contents.getStackInSlot(AirtightHandheldDrillMenu.FILTER_SLOT_INDEX);
        return filterStack.isEmpty() ? null : FilterItemStack.of(filterStack);
    }

    private static boolean isProtected(Level level, BlockPos pos, BlockState state, @Nullable FilterItemStack filter, @Nullable Map<Item, Boolean> filterMatches, boolean breakContainers) {
        boolean matchesFilter = false;
        if (filter != null && filterMatches != null) {
            Item item = state.getBlock().asItem();
            matchesFilter = filterMatches.computeIfAbsent(item, key -> filter.test(level, new ItemStack(key)));
        }
        return matchesFilter || !breakContainers && level.getCapability(ItemHandler.BLOCK, pos, null) != null;
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
