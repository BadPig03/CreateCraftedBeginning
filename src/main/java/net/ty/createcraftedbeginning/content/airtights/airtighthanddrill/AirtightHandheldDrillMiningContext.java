package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.HandheldDrillContainerProtectionButton;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.HandheldDrillFilterButton;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightHandheldDrillMiningContext(Level level, BlockPos basePos, Set<BlockPos> totalPos, Set<BlockPos> protectedPos, Set<BlockPos> unbreakablePos, Set<BlockPos> liquidPos, Set<BlockPos> instantDestructionPos, Set<BlockPos> destructionPos, Set<BlockPos> breakSpeedPos) {
    public static AirtightHandheldDrillMiningContext of(ItemStack drill, BlockPos basePos, Level level) {
        Set<BlockPos> totalPos = getTotalPos(drill, basePos);
        Set<BlockPos> protectedPos = getProtectedPos(level, drill, totalPos);
        Set<BlockPos> unbreakablePos = getUnbreakablePos(level, totalPos);
        Set<BlockPos> liquidPos = getLiquidPos(level, totalPos);
        Set<BlockPos> instantDestructionPos = getInstantDestructionPos(level, totalPos);
        Set<BlockPos> destructionPos = totalPos.stream().filter(pos -> !protectedPos.contains(pos)).filter(pos -> !unbreakablePos.contains(pos)).collect(Collectors.toCollection(HashSet::new));
        Set<BlockPos> breakSpeedPos = destructionPos.stream().filter(pos -> !liquidPos.contains(pos)).filter(pos -> !instantDestructionPos.contains(pos)).collect(Collectors.toCollection(HashSet::new));
        return new AirtightHandheldDrillMiningContext(level, basePos, Set.copyOf(totalPos), Set.copyOf(protectedPos), Set.copyOf(unbreakablePos), Set.copyOf(liquidPos), Set.copyOf(instantDestructionPos), Set.copyOf(destructionPos), Set.copyOf(breakSpeedPos));
    }

    private static Set<BlockPos> getLiquidPos(Level level, Set<BlockPos> totalPos) {
        return totalPos.stream().filter(pos -> level.getBlockState(pos).getBlock() instanceof LiquidBlock).collect(Collectors.toSet());
    }

    private static Set<BlockPos> getTotalPos(ItemStack drill, BlockPos base) {
        return AirtightHandheldDrillUtils.getMiningTemplate(drill).getTemplate().getFinalOffset(drill).stream().map(base::offset).collect(Collectors.toSet());
    }

    private static Set<BlockPos> getInstantDestructionPos(Level level, Set<BlockPos> totalPos) {
        return totalPos.stream().filter(pos -> level.getBlockState(pos).getDestroySpeed(level, pos) == 0).collect(Collectors.toSet());
    }

    private static Set<BlockPos> getUnbreakablePos(Level level, Set<BlockPos> totalPos) {
        return totalPos.stream().filter(pos -> level.getBlockState(pos).getDestroySpeed(level, pos) == -1).collect(Collectors.toSet());
    }

    private static Set<BlockPos> getProtectedPos(Level level, ItemStack drill, Set<BlockPos> totalPos) {
        return totalPos.stream().filter(pos -> {
            boolean result = false;
            ItemStack testItem = new ItemStack(level.getBlockState(pos).getBlock().asItem());
            if (HandheldDrillFilterButton.INSTANCE.canApply(drill)) {
                ItemStack filterStack = AirtightUpgradableMenu.getInventoryHandler(drill, 2).getStackInSlot(AirtightHandheldDrillMenu.FILTER_SLOT_INDEX);
                result = !filterStack.isEmpty() && FilterItemStack.of(filterStack).test(level, testItem);
            }

            return result || HandheldDrillContainerProtectionButton.INSTANCE.canApply(drill) && level.getCapability(ItemHandler.BLOCK, pos, null) != null;
        }).collect(Collectors.toSet());
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
