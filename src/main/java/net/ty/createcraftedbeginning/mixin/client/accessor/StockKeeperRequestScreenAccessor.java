package net.ty.createcraftedbeginning.mixin.client.accessor;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.createmod.catnip.data.Couple;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StockKeeperRequestScreen.class)
public interface StockKeeperRequestScreenAccessor {
    @Accessor("blockEntity")
    StockTickerBlockEntity getBlockEntity();

    @Invoker("getHoveredSlot")
    Couple<Integer> ccb$getHoveredSlot(int x, int y);

    @Invoker("getOrderForItem")
    @Nullable
    BigItemStack ccb$getOrderForItem(ItemStack stack);
}
