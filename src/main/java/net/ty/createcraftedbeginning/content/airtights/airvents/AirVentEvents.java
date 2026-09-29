package net.ty.createcraftedbeginning.content.airtights.airvents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class AirVentEvents {
    private AirVentEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onRightClickBlock(RightClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof BlockItem) || !AirVentBlock.isInsideAirVent(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onBlockPlaced(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player) || !AirVentBlock.isInsideAirVent(player)) {
            return;
        }

        event.setCanceled(true);
    }
}
