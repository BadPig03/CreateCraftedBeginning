package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystSoundSuppressorSounds {
    private static final TagKey<SoundEvent> SOUND_SUPPRESSION_EXEMPT = TagKey.create(Registries.SOUND_EVENT, ResourceLocation.fromNamespaceAndPath(CCBAPI.MOD_ID, "sound_suppression_exempt"));

    private AmethystSoundSuppressorSounds() {
    }

    static boolean canSuppress(ResourceLocation soundId, SoundSource source, boolean relative) {
        if (relative || source == SoundSource.MUSIC) {
            return false;
        }

        Optional<Reference<SoundEvent>> sound = BuiltInRegistries.SOUND_EVENT.getHolder(ResourceKey.create(Registries.SOUND_EVENT, soundId));
        return sound.isEmpty() || !sound.get().is(SOUND_SUPPRESSION_EXEMPT);
    }

    static boolean shouldMute(ResourceLocation soundId, SoundSource source, boolean relative, ResourceLocation dimension, BlockPos sourcePos, BlockPos listenerPos) {
        return canSuppress(soundId, source, relative) && shouldMute(dimension, sourcePos, listenerPos);
    }

    static boolean shouldMute(ResourceLocation dimension, BlockPos sourcePos, BlockPos listenerPos) {
        ClientAmethystSoundSuppressorCache cache = ClientAmethystSoundSuppressorCache.INSTANCE;
        return cache.checkWithinRange(sourcePos, dimension) || cache.checkWithinRange(listenerPos, dimension);
    }
}
