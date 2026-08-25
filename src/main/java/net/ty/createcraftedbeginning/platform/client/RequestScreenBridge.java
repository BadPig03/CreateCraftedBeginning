package net.ty.createcraftedbeginning.platform.client;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.platform.access.client.RedstoneRequesterScreenAccess;
import net.ty.createcraftedbeginning.platform.access.client.StockKeeperRequestScreenAccess;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class RequestScreenBridge {
    private RequestScreenBridge() {
    }

    public static boolean supportsRequesterAmounts(AbstractContainerScreen<?> screen) {
        return screen instanceof RedstoneRequesterScreenAccess;
    }

    public static boolean hasRequesterAmountSlot(AbstractContainerScreen<?> screen, int slotIndex) {
        if (!(screen instanceof RedstoneRequesterScreenAccess access)) {
            return false;
        }

        List<Integer> amounts = access.ccb$getAmounts();
        return slotIndex >= 0 && slotIndex < amounts.size();
    }

    public static boolean setRequesterAmount(AbstractContainerScreen<?> screen, int slotIndex, int amount) {
        if (!(screen instanceof RedstoneRequesterScreenAccess access)) {
            return false;
        }

        List<Integer> amounts = access.ccb$getAmounts();
        if (slotIndex < 0 || slotIndex >= amounts.size()) {
            return false;
        }

        amounts.set(slotIndex, amount);
        return true;
    }

    public static int getAvailableAmount(StockKeeperRequestScreen screen, ItemStack stack) {
        if (!(screen instanceof StockKeeperRequestScreenAccess access)) {
            return 0;
        }

        return access.ccb$getBlockEntity().getLastClientsideStockSnapshotAsSummary().getCountOf(stack);
    }

    public static @Nullable BigItemStack getRequestedOrder(StockKeeperRequestScreen screen, ItemStack stack) {
        return screen instanceof StockKeeperRequestScreenAccess access ? access.ccb$getOrderForItem(stack) : null;
    }
}
