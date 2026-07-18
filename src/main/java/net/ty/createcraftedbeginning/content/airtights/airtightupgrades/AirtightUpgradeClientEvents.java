package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID, value = Dist.CLIENT)
public final class AirtightUpgradeClientEvents {
    private AirtightUpgradeClientEvents() {
    }

    @SubscribeEvent
    public static void onClientLoggingOut(LoggingOut event) {
        GlobalAirtightUpgradesConsumptionManager.clearClientTracking();
    }
}
