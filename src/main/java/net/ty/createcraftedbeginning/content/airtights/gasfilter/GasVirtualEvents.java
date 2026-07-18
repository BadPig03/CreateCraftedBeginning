package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class GasVirtualEvents {
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
        boolean shouldCancel = false;
        if (GasVirtualUtils.isVirtualItem(carried)) {
            carried.shrink(1);
            shouldCancel = true;
        }

        ItemStack stackedOn = event.getStackedOnItem();
        if (GasVirtualUtils.isVirtualItem(stackedOn)) {
            stackedOn.shrink(1);
            shouldCancel = true;
        }
        event.setCanceled(shouldCancel);
    }
}
