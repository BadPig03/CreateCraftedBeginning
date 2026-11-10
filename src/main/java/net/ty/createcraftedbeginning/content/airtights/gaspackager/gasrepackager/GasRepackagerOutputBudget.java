package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.Candidate;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasRepackagerOutputBudget {
    static final int MAX_OUTPUT_PACKAGES = 1024;

    private int remainingPackages = MAX_OUTPUT_PACKAGES;

    boolean reserveGas(long amount, long packingLimit) {
        if (amount <= 0 || packingLimit <= 0) {
            return false;
        }

        long packageCount = amount / packingLimit;
        if (amount % packingLimit != 0) {
            packageCount++;
        }
        if (packageCount > remainingPackages) {
            return false;
        }

        remainingPackages -= (int) packageCount;
        return true;
    }

    boolean reserveItems(List<Candidate> candidates, @Nullable PackageOrderWithCrafts context) {
        InventorySummary summary = new InventorySummary();
        for (Candidate candidate : candidates) {
            ItemStackHandler contents = PackageItem.getContents(candidate.box());
            for (int slot = 0; slot < contents.getSlots(); slot++) {
                summary.add(contents.getStackInSlot(slot));
            }
        }
        if (context != null) {
            for (CraftingEntry craft : context.orderedCrafts()) {
                Crafts:
                for (int craftIndex = 0; craftIndex < craft.count(); craftIndex++) {
                    for (BigItemStack required : craft.pattern().stacks()) {
                        if (required.stack.isEmpty()) {
                            continue;
                        }

                        if (summary.getCountOf(required.stack) <= 0) {
                            break Crafts;
                        }

                        summary.add(required.stack, -1);
                    }
                    if (remainingPackages <= 0) {
                        return false;
                    }

                    remainingPackages--;
                }
            }
        }

        List<BigItemStack> orderedStacks = new ArrayList<>();
        if (context != null && context.orderedCrafts().isEmpty()) {
            for (BigItemStack stack : context.stacks()) {
                orderedStacks.add(new BigItemStack(stack.stack, stack.count));
            }
        }
        List<BigItemStack> allItems = summary.getStacks();
        long outputSlots = 0;
        while (true) {
            allItems.removeIf(entry -> entry.count == 0);
            if (allItems.isEmpty()) {
                break;
            }

            BigItemStack targetedEntry = orderedStacks.isEmpty() ? null : orderedStacks.removeFirst();
            for (BigItemStack entry : allItems) {
                if (targetedEntry != null && !ItemStack.isSameItemSameComponents(entry.stack, targetedEntry.stack)) {
                    continue;
                }

                int targetAmount = targetedEntry == null ? entry.count : Math.min(targetedEntry.count, entry.count);
                if (targetAmount <= 0) {
                    continue;
                }

                int stackSize = entry.stack.getMaxStackSize();
                outputSlots += targetAmount / stackSize;
                if (targetAmount % stackSize != 0) {
                    outputSlots++;
                }
                if (outputSlots > (long) remainingPackages * PackageItem.SLOTS) {
                    return false;
                }

                entry.count -= targetAmount;
                if (targetedEntry != null) {
                    targetedEntry.count -= targetAmount;
                }
                break;
            }
        }
        int packageCount = (int) (outputSlots / PackageItem.SLOTS);
        if (outputSlots % PackageItem.SLOTS != 0) {
            packageCount++;
        }
        remainingPackages -= packageCount;
        return true;
    }
}
