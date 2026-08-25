package net.ty.createcraftedbeginning.mixin.client.accessor;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.platform.access.client.StockKeeperRequestScreenAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(StockKeeperRequestScreen.class)
public interface StockKeeperRequestScreenAccessor extends StockKeeperRequestScreenAccess {
    @Override
    @Accessor("blockEntity")
    StockTickerBlockEntity ccb$getBlockEntity();

    @Override
    @Invoker("getHoveredSlot")
    Couple<Integer> ccb$getHoveredSlot(int x, int y);

    @Override
    @Invoker("getOrderForItem")
    @Nullable BigItemStack ccb$getOrderForItem(ItemStack stack);
}
