package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
final class AmethystSoundSuppressorClientEvents {
    private AmethystSoundSuppressorClientEvents() {
    }

    @SubscribeEvent
    private static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null || !AmethystSoundSuppressorSoundTracker.handlePlayback(sound)) {
            return;
        }

        event.setSound(null);
    }

    @SubscribeEvent
    private static void onClientTick(Post event) {
        AmethystSoundSuppressorSoundTracker.tick();
    }

    @SubscribeEvent
    private static void onClientLoggingOut(LoggingOut event) {
        AmethystSoundSuppressorSoundTracker.clear();
        ClientAmethystSoundSuppressorCache.INSTANCE.clear();
    }
}
