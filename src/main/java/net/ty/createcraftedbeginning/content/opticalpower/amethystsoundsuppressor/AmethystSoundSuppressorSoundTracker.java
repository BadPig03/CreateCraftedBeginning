package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystSoundSuppressorSoundTracker {
    private static final int INACTIVE_SOUND_GRACE_TICKS = 3;
    private static final Map<SoundInstance, Integer> TRACKED_LOOPING_SOUNDS = new IdentityHashMap<>();

    private AmethystSoundSuppressorSoundTracker() {
    }

    static boolean handlePlayback(SoundInstance sound) {
        ResourceLocation soundId = sound.getLocation();
        SoundSource soundSource = sound.getSource();
        boolean relative = sound.isRelative();
        if (!AmethystSoundSuppressorSounds.canSuppress(soundId, soundSource, relative)) {
            return false;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }

        if (shouldMute(level, sound)) {
            TRACKED_LOOPING_SOUNDS.remove(sound);
            return true;
        }

        if (!sound.isLooping()) {
            return false;
        }

        TRACKED_LOOPING_SOUNDS.put(sound, 0);
        return false;
    }

    static void tick() {
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
        boolean hasCoverage = ClientAmethystSoundSuppressorCache.INSTANCE.hasCoverage(dimension);
        boolean hasCheckedListener = false;
        boolean isListenerMuted = false;
        SoundManager soundManager = minecraft.getSoundManager();
        Iterator<Entry<SoundInstance, Integer>> soundIterator = TRACKED_LOOPING_SOUNDS.entrySet().iterator();
        while (soundIterator.hasNext()) {
            Entry<SoundInstance, Integer> soundEntry = soundIterator.next();
            SoundInstance sound = soundEntry.getKey();
            if (!AmethystSoundSuppressorSounds.canSuppress(sound.getLocation(), sound.getSource(), sound.isRelative())) {
                soundIterator.remove();
                continue;
            }

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

            if (!isListenerMuted && !isWithinRange(level, new Vec3(sound.getX(), sound.getY(), sound.getZ()), dimension)) {
                continue;
            }

            soundManager.stop(sound);
            soundIterator.remove();
        }
    }

    static void clear() {
        TRACKED_LOOPING_SOUNDS.clear();
    }

    private static boolean shouldMute(ClientLevel level, SoundInstance sound) {
        ResourceLocation dimension = level.dimension().location();
        if (!ClientAmethystSoundSuppressorCache.INSTANCE.hasCoverage(dimension)) {
            return false;
        }

        if (isWithinRange(level, new Vec3(sound.getX(), sound.getY(), sound.getZ()), dimension)) {
            return true;
        }

        Vec3 listenerPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return isWithinRange(level, listenerPosition, dimension);
    }

    private static boolean isWithinRange(ClientLevel level, Vec3 worldPosition, ResourceLocation dimension) {
        return ClientAmethystSoundSuppressorCache.INSTANCE.checkWithinRange(SubLevelBridge.resolve(level, worldPosition).blockPos(), dimension);
    }
}
