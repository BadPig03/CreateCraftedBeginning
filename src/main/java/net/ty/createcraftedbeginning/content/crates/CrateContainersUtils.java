package net.ty.createcraftedbeginning.content.crates;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateContents;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CrateContainersUtils {
    private CrateContainersUtils() {
    }

    public static void dropContents(Level level, Vec3 pos, @NotNull SturdyCrateContents contents) {
        ItemStack content = contents.content();
        int count = contents.count();
        if (content.isEmpty() || count <= 0) {
            return;
        }

        int limit = content.getMaxStackSize();
        while (count > 0) {
            int dropCount = Math.min(count, limit);
            Containers.dropItemStack(level, pos.x, pos.y, pos.z, content.copyWithCount(dropCount));
            count -= dropCount;
        }
    }

    public static void dropContents(Level level, double x, double y, double z, @NotNull CrateItemStackHandler handler) {
        ItemStack content = handler.getStackInSlot(0);
        int count = handler.getCountInSlot(0);
        if (content.isEmpty() || count <= 0) {
            return;
        }

        int limit = content.getMaxStackSize();
        while (count > 0) {
            int dropCount = Math.min(count, limit);
            Containers.dropItemStack(level, x, y, z, content.copyWithCount(dropCount));
            count -= dropCount;
        }
    }

    public static int calculateRedstoneSignal(@NotNull CrateItemStackHandler handler) {
        ItemStack content = handler.getStackInSlot(0);
        int count = handler.getCountInSlot(0);
        if (content.isEmpty() || count <= 0) {
            return 0;
        }

        int limit = handler.getSlotLimit(0);
        return Mth.floor((float) count / limit * 14) + 1;
    }

    public static boolean defaultUnpack(@NotNull Level level, BlockPos pos, List<ItemStack> items, boolean simulate) {
        if (!(level.getBlockEntity(pos) instanceof CratesBlockEntity crateBlockEntity)) {
            return false;
        }

        CrateItemStackHandler handler = crateBlockEntity.getHandler();
        if (handler == null) {
            return false;
        }

        int maxCount = handler.getSlotLimit(0);
        int space = maxCount - handler.getCountInSlot(0);
        int totalCount = items.stream().mapToInt(ItemStack::getCount).sum();
        if (totalCount > space) {
            return false;
        }

        items.forEach(stack -> handler.insertItem(0, stack, simulate));
        return true;
    }
}
