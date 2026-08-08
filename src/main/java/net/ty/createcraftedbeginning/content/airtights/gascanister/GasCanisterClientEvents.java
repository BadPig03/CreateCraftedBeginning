package net.ty.createcraftedbeginning.content.airtights.gascanister;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
public final class GasCanisterClientEvents {
    private GasCanisterClientEvents() {
    }

    @SubscribeEvent
    public static void onClientLoggingOut(LoggingOut event) {
        CanisterContainerClients.clearDisplayedGasState();
    }
}
