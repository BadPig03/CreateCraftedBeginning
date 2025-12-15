package net.ty.createcraftedbeginning.content.airtights.gascanister;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterQueryUtils;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterSupplierUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class GasCanisterEvents {
    @SubscribeEvent
    public static void onPlayerPostTick(@NotNull Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        GasStack content = GasCanisterSupplierUtils.getTotalGasStack(player);
        if (content.isEmpty()) {
            CatnipServices.NETWORK.sendToClient(serverPlayer, new GasCanisterOverlayPacket(GasStack.EMPTY, 0));
            return;
        }

        long capacity = GasCanisterQueryUtils.getTotalGasCapacity(player, content.getGas());
        CatnipServices.NETWORK.sendToClient(serverPlayer, new GasCanisterOverlayPacket(content, capacity));
    }
}
