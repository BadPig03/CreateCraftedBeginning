package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.content.logistics.BigItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasOrderChanges {
    private GasOrderChanges() {
    }

    public static Change apply(List<BigItemStack> orders, @Nullable BigItemStack order, ItemStack item, int available, boolean remove, int transfer, boolean control) {
        if (transfer <= 0) {
            return Change.NONE;
        }

        if (order == null && (remove || orders.size() >= 9 || available <= 0)) {
            return Change.NONE;
        }

        if (order == null) {
            order = new BigItemStack(item.copyWithCount(1), 0);
            orders.add(order);
        }

        int current = order.count;
        if (!control && !remove && current == 1 && transfer > 1) {
            transfer--;
        }
        int next = remove ? current - transfer : current + Mth.clamp(available - current, 0, transfer);
        if (next <= 0) {
            orders.remove(order);
            return Change.REMOVED;
        }

        order.count = next;
        if (current == 0) {
            return Change.ADDED;
        }

        return Change.UPDATED;
    }

    public enum Change {
        NONE,
        ADDED,
        UPDATED,
        REMOVED
    }
}
