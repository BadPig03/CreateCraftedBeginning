package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.platform.CCBSubLevelBridge;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
final class EndSculkSilencerClientEvents {
    private static final int INACTIVE_SOUND_GRACE_TICKS = 3;
    private static final Map<SoundInstance, Integer> TRACKED_LOOPING_SOUNDS = new IdentityHashMap<>();

    private EndSculkSilencerClientEvents() {
    }

    @SubscribeEvent
    private static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        if (shouldMute(level, sound)) {
            TRACKED_LOOPING_SOUNDS.remove(sound);
            event.setSound(null);
            return;
        }

        if (!sound.isLooping() || sound.isRelative()) {
            return;
        }

        TRACKED_LOOPING_SOUNDS.put(sound, 0);
    }

    @SubscribeEvent
    private static void onClientTick(Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            TRACKED_LOOPING_SOUNDS.clear();
            return;
        }

        if (TRACKED_LOOPING_SOUNDS.isEmpty()) {
            return;
        }

        ResourceLocation dimension = level.dimension().location();
        boolean hasCoverage = ClientEndSculkSilencerCache.INSTANCE.hasCoverage(dimension);
        boolean hasCheckedListener = false;
        boolean isListenerMuted = false;
        SoundManager soundManager = minecraft.getSoundManager();
        Iterator<Entry<SoundInstance, Integer>> soundIterator = TRACKED_LOOPING_SOUNDS.entrySet().iterator();
        while (soundIterator.hasNext()) {
            Entry<SoundInstance, Integer> soundEntry = soundIterator.next();
            SoundInstance sound = soundEntry.getKey();
            if (!soundManager.isActive(sound)) {
                int inactiveTicks = soundEntry.getValue() + 1;
                if (inactiveTicks > INACTIVE_SOUND_GRACE_TICKS) {
                    soundIterator.remove();
                }
                else {
                    soundEntry.setValue(inactiveTicks);
                }
                continue;
            }

            soundEntry.setValue(0);
            if (!hasCoverage) {
                continue;
            }

            if (!hasCheckedListener) {
                isListenerMuted = isWithinRange(level, minecraft.gameRenderer.getMainCamera().getPosition(), dimension);
                hasCheckedListener = true;
            }

            if (!isListenerMuted && !isSoundWithinRange(level, sound, dimension)) {
                continue;
            }

            soundManager.stop(sound);
            soundIterator.remove();
        }
    }

    @SubscribeEvent
    private static void onClientLoggingOut(LoggingOut event) {
        TRACKED_LOOPING_SOUNDS.clear();
        ClientEndSculkSilencerCache.INSTANCE.clear();
    }

    private static boolean shouldMute(ClientLevel level, SoundInstance sound) {
        if (sound.isRelative()) {
            return false;
        }

        ResourceLocation dimension = level.dimension().location();
        if (!ClientEndSculkSilencerCache.INSTANCE.hasCoverage(dimension)) {
            return false;
        }

        Vec3 listenerPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return isWithinRange(level, listenerPosition, dimension) || isSoundWithinRange(level, sound, dimension);
    }

    private static boolean isSoundWithinRange(ClientLevel level, SoundInstance sound, ResourceLocation dimension) {
        return isWithinRange(level, new Vec3(sound.getX(), sound.getY(), sound.getZ()), dimension);
    }

    private static boolean isWithinRange(ClientLevel level, Vec3 worldPosition, ResourceLocation dimension) {
        return ClientEndSculkSilencerCache.INSTANCE.checkWithinRange(CCBSubLevelBridge.resolve(level, worldPosition).blockPos(), dimension);
    }
}
