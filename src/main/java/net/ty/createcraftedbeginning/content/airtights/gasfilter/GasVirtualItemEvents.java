package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(Dist.CLIENT)
public class GasVirtualItemEvents {
    @SubscribeEvent
    public static void registerColors(@NotNull Item event) {
        event.register((stack, color) -> stack.getOrDefault(CCBDataComponents.GAS_VIRTUAL_ITEM_COLOR, 0xFFFFFFFF), CCBItems.GAS_VIRTUAL_ITEM.get());
    }

    @SubscribeEvent
    public static void onDropGasVirtualItems(@NotNull ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!stack.is(CCBItems.GAS_VIRTUAL_ITEM)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClickOnGasVirtualItems(@NotNull ItemStackedOnOtherEvent event) {
        ItemStack carried = event.getCarriedItem();
        ItemStack stackOn = event.getStackedOnItem();
        boolean cancel = false;
        if (carried.is(CCBItems.GAS_VIRTUAL_ITEM)) {
            carried.shrink(1);
            cancel = true;
        }
        if (stackOn.is(CCBItems.GAS_VIRTUAL_ITEM)) {
            stackOn.shrink(1);
            cancel = true;
        }
        event.setCanceled(cancel);
    }
}
