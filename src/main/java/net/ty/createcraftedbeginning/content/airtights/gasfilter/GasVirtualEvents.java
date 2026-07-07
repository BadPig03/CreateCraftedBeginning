package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(Dist.CLIENT)
public class GasVirtualEvents {
    @SubscribeEvent
    public static void onRegisterItemColors(Item event) {
        event.register((stack, i) -> stack.getOrDefault(CCBDataComponents.GAS_VIRTUAL_ITEM_COLOR, 0xFFFFFF), CCBItems.GAS_VIRTUAL_ITEM.get());
    }

    @SubscribeEvent
    public static void onDropGasVirtualItems(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!GasVirtualUtils.isVirtualItem(stack)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClickOnGasVirtualItems(ItemStackedOnOtherEvent event) {
        ItemStack carried = event.getCarriedItem();
        boolean cancel = false;
        if (GasVirtualUtils.isVirtualItem(carried)) {
            carried.shrink(1);
            cancel = true;
        }

        ItemStack stackOn = event.getStackedOnItem();
        if (GasVirtualUtils.isVirtualItem(stackOn)) {
            stackOn.shrink(1);
            cancel = true;
        }
        event.setCanceled(cancel);
    }
}
