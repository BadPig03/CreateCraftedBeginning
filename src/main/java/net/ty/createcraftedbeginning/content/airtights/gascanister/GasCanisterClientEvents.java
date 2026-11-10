package net.ty.createcraftedbeginning.content.airtights.gascanister;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
final class GasCanisterClientEvents {
    private GasCanisterClientEvents() {
    }

    @SubscribeEvent
    private static void onClientPostTick(Post event) {
        GasCanisterVentRenderer.tick();
    }

    @SubscribeEvent
    private static void onClientLoggingOut(LoggingOut event) {
        CanisterContainerClients.clearDisplayedGasState();
        LocalPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }

        CanisterContainerSuppliers.invalidateCache(player);
    }
}
