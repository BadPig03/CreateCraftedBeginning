package net.ty.createcraftedbeginning.platform.access;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.createmod.catnip.data.Couple;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface StockKeeperRequestScreenAccess {
    StockTickerBlockEntity getBlockEntity();

    Couple<Integer> ccb$getHoveredSlot(int x, int y);

    @Nullable BigItemStack ccb$getOrderForItem(ItemStack stack);
}
