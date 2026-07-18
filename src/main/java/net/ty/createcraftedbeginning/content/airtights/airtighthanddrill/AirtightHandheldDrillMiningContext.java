package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.HandheldDrillContainerProtectionButton;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.HandheldDrillFilterButton;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public record AirtightHandheldDrillMiningContext(Level level, BlockPos basePos, Set<BlockPos> totalPos, Set<BlockPos> protectedPos, Set<BlockPos> unbreakablePos, Set<BlockPos> liquidPos, Set<BlockPos> instantDestructionPos, Set<BlockPos> destructionPos, Set<BlockPos> breakSpeedPos) {
    public static AirtightHandheldDrillMiningContext of(ItemStack drill, BlockPos basePos, Level level) {
        return of(drill, basePos, level, level.getBlockState(basePos));
    }

    public static AirtightHandheldDrillMiningContext of(ItemStack drill, BlockPos basePos, Level level, BlockState baseState) {
        Set<BlockPos> totalPos = getTotalPos(drill, basePos, level, baseState);
        Set<BlockPos> protectedPos = new LinkedHashSet<>();
        Set<BlockPos> unbreakablePos = new LinkedHashSet<>();
        Set<BlockPos> liquidPos = new LinkedHashSet<>();
        Set<BlockPos> instantDestructionPos = new LinkedHashSet<>();
        Set<BlockPos> destructionPos = new LinkedHashSet<>();
        Set<BlockPos> breakSpeedPos = new LinkedHashSet<>();
        FilterItemStack filter = getFilter(drill);
        boolean protectContainers = HandheldDrillContainerProtectionButton.INSTANCE.canApply(drill);
        for (BlockPos pos : totalPos) {
            BlockState state = level.getBlockState(pos);
            boolean isProtected = isProtected(level, pos, state, filter, protectContainers);
            if (isProtected) {
                protectedPos.add(pos);
            }

            float destroySpeed = state.getDestroySpeed(level, pos);
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
            if (!isLiquid && !isInstantDestruction) {
                breakSpeedPos.add(pos);
            }
        }

        return new AirtightHandheldDrillMiningContext(level, basePos, immutableOrderedCopy(totalPos), immutableOrderedCopy(protectedPos), immutableOrderedCopy(unbreakablePos), immutableOrderedCopy(liquidPos), immutableOrderedCopy(instantDestructionPos), immutableOrderedCopy(destructionPos), immutableOrderedCopy(breakSpeedPos));
    }

    private static @Nullable FilterItemStack getFilter(ItemStack drill) {
        if (!HandheldDrillFilterButton.INSTANCE.canApply(drill)) {
            return null;
        }

        ItemStack filterStack = AirtightUpgradableMenu.getInventoryHandler(drill, 2).getStackInSlot(AirtightHandheldDrillMenu.FILTER_SLOT_INDEX);
        return filterStack.isEmpty() ? null : FilterItemStack.of(filterStack);
    }

    private static boolean isProtected(Level level, BlockPos pos, BlockState state, @Nullable FilterItemStack filter, boolean protectContainers) {
        return filter != null && filter.test(level, new ItemStack(state.getBlock().asItem())) || protectContainers && level.getCapability(ItemHandler.BLOCK, pos, null) != null;
    }

    private static Set<BlockPos> getTotalPos(ItemStack drill, BlockPos basePos, Level level, BlockState baseState) {
        return new LinkedHashSet<>(AirtightHandheldDrillUtils.getMiningTemplate(drill).getTemplate().getTargetPositions(drill, basePos, level, baseState));
    }

    private static @UnmodifiableView Set<BlockPos> immutableOrderedCopy(Set<BlockPos> positions) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(positions));
    }

    public Set<BlockPos> getDestructionPos(boolean destroyExtra) {
        return destroyExtra ? destructionPos : breakSpeedPos;
    }

    public boolean isValidBaseTarget() {
        return destructionPos.contains(basePos);
    }

    public boolean isEmpty() {
        return destructionPos.isEmpty();
    }
}
