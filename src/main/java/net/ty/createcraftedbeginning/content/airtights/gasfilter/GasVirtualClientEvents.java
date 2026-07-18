package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID, value = Dist.CLIENT)
public class GasVirtualClientEvents {
    @SubscribeEvent
    public static void onRegisterItemColors(Item event) {
        event.register((stack, tintIndex) -> stack.getOrDefault(CCBDataComponents.GAS_VIRTUAL_ITEM_COLOR, 0xFFFFFF), CCBItems.GAS_VIRTUAL_ITEM.get());
    }
}
