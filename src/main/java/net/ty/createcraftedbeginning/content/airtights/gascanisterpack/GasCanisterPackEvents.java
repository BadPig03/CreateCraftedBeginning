package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class GasCanisterPackEvents {
    @SubscribeEvent
    public static void onItemExpire(@NotNull ItemExpireEvent event) {
        ItemStack itemStack = event.getEntity().getItem();
        if (!itemStack.is(CCBItems.GAS_CANISTER_PACK)) {
            return;
        }

        CreateCraftedBeginning.GAS_CANISTER_PACK_CONTENTS_DATA_MANAGER.removeContents(GasCanisterPackUtils.getCanisterPackUUID(itemStack));
    }

    @SubscribeEvent
    public static void onPlayerDestroyItem(@NotNull PlayerDestroyItemEvent event) {
        ItemStack itemStack = event.getOriginal();
        if (!itemStack.is(CCBItems.GAS_CANISTER_PACK)) {
            return;
        }

        CreateCraftedBeginning.GAS_CANISTER_PACK_CONTENTS_DATA_MANAGER.removeContents(GasCanisterPackUtils.getCanisterPackUUID(itemStack));
    }
}
