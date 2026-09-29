package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class AirtightUpgradeMaterialsEvents {
    private AirtightUpgradeMaterialsEvents() {
    }

    @SubscribeEvent
    private static void onDatapackSync(OnDatapackSyncEvent event) {
        AirtightUpgradeMaterialsSyncPacket packet = new AirtightUpgradeMaterialsSyncPacket(AirtightUpgradeMaterials.createServerSnapshot());
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            CatnipServices.NETWORK.sendToClient(player, packet);
            return;
        }

        for (ServerPlayer onlinePlayer : event.getPlayerList().getPlayers()) {
            CatnipServices.NETWORK.sendToClient(onlinePlayer, packet);
        }
    }
}
