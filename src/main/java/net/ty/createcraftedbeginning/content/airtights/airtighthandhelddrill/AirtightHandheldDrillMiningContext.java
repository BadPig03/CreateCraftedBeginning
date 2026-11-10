package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.templates.AirtightHandheldDrillMiningTemplates;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.HandheldDrillContainerProtectionButton;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.HandheldDrillFilterButton;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.LiquidReplacementUpgrade;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.ApiStatus.Internal;
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
@Internal
public record AirtightHandheldDrillMiningContext(Level level, BlockPos basePos, Set<BlockPos> totalPos, Set<BlockPos> protectedPos, Set<BlockPos> unbreakablePos, Set<BlockPos> liquidPos, Set<BlockPos> instantDestructionPos, Set<BlockPos> destructionPos, Set<BlockPos> breakSpeedPos, float baseHardness, float totalBreakHardness) {
    public static AirtightHandheldDrillMiningContext of(ItemStack drill, BlockPos basePos, Level level, Player player) {
        return of(drill, basePos, level, level.getBlockState(basePos), player);
    }

    static AirtightHandheldDrillMiningContext of(ItemStack drill, BlockPos basePos, Level level, BlockState baseState, Player player) {
        Direction miningDirection = AirtightHandheldDrillSettings.getMiningDirection(drill).resolve(player.getLookAngle());
        AirtightHandheldDrillMiningTemplates miningTemplate = AirtightHandheldDrillSettings.getMiningTemplate(drill);
        FilterItemStack filter = getFilter(drill);
        Map<BlockState, Boolean> filterMatches = new HashMap<>();
        boolean shouldBreakContainers = HandheldDrillContainerProtectionButton.INSTANCE.canApply(drill);
        boolean liquidReplacement = LiquidReplacementUpgrade.INSTANCE.canApply(drill);
        boolean singleBlockMining = AirtightHandheldDrillControls.isSingleBlockMining(player, drill);
        Set<BlockPos> totalPos;
        if (singleBlockMining) {
            totalPos = Set.of(basePos.immutable());
        }
        else {
            totalPos = miningTemplate.getTemplate().getTargetPositions(drill, basePos, level, baseState, miningDirection, (pos, state) -> !isProtected(level, pos, state, filter, filterMatches, shouldBreakContainers) && state.getDestroySpeed(level, pos) >= 0 && (!(state.getBlock() instanceof LiquidBlock) || liquidReplacement));
        }
        Set<BlockPos> protectedPos = new LinkedHashSet<>();
        Set<BlockPos> unbreakablePos = new LinkedHashSet<>();
        Set<BlockPos> liquidPos = new LinkedHashSet<>();
        Set<BlockPos> instantDestructionPos = new LinkedHashSet<>();
        Set<BlockPos> destructionPos = new LinkedHashSet<>();
        Set<BlockPos> breakSpeedPos = new LinkedHashSet<>();
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
            boolean isLiquid = blockState.getBlock() instanceof LiquidBlock;
            boolean isUnbreakable = blockHardness == -1;
            if (isLiquid && !isUnbreakable) {
                blockHardness /= LiquidReplacementUpgrade.HARDNESS_DIVISOR;
            }
            if (targetPos.equals(basePos)) {
                baseHardness = blockHardness;
            }

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

            if (isProtectedTarget || isUnbreakable || isLiquid && !liquidReplacement) {
                continue;
            }

            destructionPos.add(targetPos);
            if (isInstantDestruction) {
                continue;
            }

            breakSpeedPos.add(targetPos);
            totalBreakHardness += Math.max(0, blockHardness);
        }
        AirtightHandheldDrillMiningContext context = new AirtightHandheldDrillMiningContext(level, basePos, immutableView(totalPos), immutableView(protectedPos), immutableView(unbreakablePos), immutableView(liquidPos), immutableView(instantDestructionPos), immutableView(destructionPos), immutableView(breakSpeedPos), baseHardness, totalBreakHardness);
        if (singleBlockMining || miningTemplate != AirtightHandheldDrillMiningTemplates.CHAIN_MINING || !context.isValidBaseTarget()) {
            return context;
        }

        return AirtightHandheldDrillFuel.limitToAffordableTargets(player, drill, context);
    }

    static @Nullable FilterItemStack getFilter(ItemStack drill) {
        if (!HandheldDrillFilterButton.INSTANCE.canApply(drill)) {
            return null;
        }

        ItemContainerContents upgradeInventory = drill.get(CCBDataComponents.AIRTIGHT_UPGRADABLE_INVENTORY);
        if (upgradeInventory == null || upgradeInventory.getSlots() <= AirtightHandheldDrillMenu.FILTER_SLOT_INDEX) {
            return null;
        }

        ItemStack filterStack = upgradeInventory.getStackInSlot(AirtightHandheldDrillMenu.FILTER_SLOT_INDEX);
        if (filterStack.isEmpty()) {
            return null;
        }

        return FilterItemStack.of(filterStack.copy());
    }

    static boolean isProtected(Level level, BlockPos blockPos, BlockState blockState, @Nullable FilterItemStack filter, @Nullable Map<BlockState, Boolean> filterMatches, boolean shouldBreakContainers) {
        boolean matchesFilter = filter != null && filterMatches != null && filterMatches.computeIfAbsent(blockState, state -> {
            Block block = state.getBlock();
            if (!(block instanceof LiquidBlock) && filter.test(level, new ItemStack(block.asItem()))) {
                return true;
            }

            FluidState fluidState = state.getFluidState();
            if (fluidState.isEmpty()) {
                return false;
            }

            Fluid fluid = fluidState.getType();
            if (fluid instanceof FlowingFluid flowingFluid) {
                fluid = flowingFluid.getSource();
            }

            return filter.test(level, new FluidStack(fluid, FluidType.BUCKET_VOLUME));
        });
        return matchesFilter || !shouldBreakContainers && level.getCapability(ItemHandler.BLOCK, blockPos, null) != null;
    }

    private static @UnmodifiableView Set<BlockPos> immutableView(Set<BlockPos> positions) {
        return Collections.unmodifiableSet(positions);
    }

    AirtightHandheldDrillMiningContext withTargetLimit(int blockLimit) {
        Set<BlockPos> limitedPositions = new LinkedHashSet<>();
        Set<BlockPos> limitedLiquids = new LinkedHashSet<>();
        Set<BlockPos> limitedInstantDestruction = new LinkedHashSet<>();
        Set<BlockPos> limitedBreakSpeed = new LinkedHashSet<>();
        float limitedHardness = 0;
        for (BlockPos pos : destructionPos) {
            if (limitedPositions.size() >= blockLimit) {
                break;
            }

            limitedPositions.add(pos);
            if (liquidPos.contains(pos)) {
                limitedLiquids.add(pos);
            }
            if (instantDestructionPos.contains(pos)) {
                limitedInstantDestruction.add(pos);
            }
            if (!breakSpeedPos.contains(pos)) {
                continue;
            }

            limitedBreakSpeed.add(pos);
            float hardness = level.getBlockState(pos).getDestroySpeed(level, pos);
            if (liquidPos.contains(pos)) {
                hardness /= LiquidReplacementUpgrade.HARDNESS_DIVISOR;
            }
            limitedHardness += Math.max(0, hardness);
        }
        Set<BlockPos> selectedPositions = immutableView(limitedPositions);
        return new AirtightHandheldDrillMiningContext(level, basePos, selectedPositions, Set.of(), Set.of(), immutableView(limitedLiquids), immutableView(limitedInstantDestruction), selectedPositions, immutableView(limitedBreakSpeed), baseHardness, limitedHardness);
    }

    boolean isValidBaseTarget() {
        return destructionPos.contains(basePos);
    }

    boolean isEmpty() {
        return destructionPos.isEmpty();
    }
}
