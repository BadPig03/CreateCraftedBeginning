package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID, value = Dist.CLIENT)
public final class EndSculkSilencerClientEvents {
    private EndSculkSilencerClientEvents() {
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        BlockPos soundPos = BlockPos.containing(sound.getX(), sound.getY(), sound.getZ());
        String dimension = level.dimension().location().toString();
        if (!ClientEndSculkSilencerCache.INSTANCE.checkWithinRange(soundPos, dimension)) {
            return;
        }

        event.setSound(null);
    }

    @SubscribeEvent
    public static void onClientLoggingOut(LoggingOut event) {
        ClientEndSculkSilencerCache.INSTANCE.clear();
    }
}
