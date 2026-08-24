package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public class GasVirtualEvents {
    @SubscribeEvent
    public static void onDropGasVirtualItems(ItemTossEvent event) {
        if (!GasVirtualUtils.isVirtualItem(event.getEntity().getItem())) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClickOnGasVirtualItems(ItemStackedOnOtherEvent event) {
        ItemStack carriedStack = event.getCarriedItem();
        boolean shouldCancel = false;
        if (GasVirtualUtils.isVirtualItem(carriedStack)) {
            carriedStack.shrink(1);
            shouldCancel = true;
        }

        ItemStack stackedOnStack = event.getStackedOnItem();
        if (GasVirtualUtils.isVirtualItem(stackedOnStack)) {
            stackedOnStack.shrink(1);
            shouldCancel = true;
        }
        event.setCanceled(shouldCancel);
    }
}
