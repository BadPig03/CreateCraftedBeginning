package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;
import net.neoforged.neoforge.client.event.InputEvent.Key;
import net.neoforged.neoforge.client.event.ScreenEvent.Opening;
import net.ty.createcraftedbeginning.api.CCBAPI;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
final class AirtightHandheldDrillControlsClient {
    private static @Nullable LocalPlayer trackedPlayer;

    private AirtightHandheldDrillControlsClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private static void onClientTick(Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        updateSingleBlockMode(AirtightHandheldDrillKeys.canUseShortcuts(minecraft) && AirtightHandheldDrillKeys.getSingleBlockKey().isDown());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private static void onKeyInput(Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        updateSingleBlockMode(AirtightHandheldDrillKeys.canUseShortcuts(minecraft) && AirtightHandheldDrillKeys.getSingleBlockKey().isDown());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onScreenOpening(Opening event) {
        if (event.getNewScreen() == null) {
            return;
        }

        updateSingleBlockMode(false);
    }

    private static void updateSingleBlockMode(boolean pressed) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        LocalPlayer previousPlayer = trackedPlayer;
        if (previousPlayer != null && previousPlayer != player) {
            AirtightHandheldDrillControls.setSingleBlockMode(previousPlayer, false);
        }
        trackedPlayer = player;
        if (player == null || !AirtightHandheldDrillControls.setSingleBlockMode(player, pressed)) {
            return;
        }

        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) {
            int hotbarSlot = player.getInventory().selected;
            connection.send(new ServerboundSetCarriedItemPacket(hotbarSlot));
            CatnipServices.NETWORK.sendToServer(new AirtightHandheldDrillSingleBlockPacket(hotbarSlot, pressed));
        }
        if (minecraft.gameMode == null) {
            return;
        }

        minecraft.gameMode.stopDestroyBlock();
    }
}
