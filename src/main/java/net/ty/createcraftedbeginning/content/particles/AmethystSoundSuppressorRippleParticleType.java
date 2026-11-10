package net.ty.createcraftedbeginning.content.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.ty.createcraftedbeginning.content.particles.AmethystSoundSuppressorRippleParticleType.RippleOptions;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystSoundSuppressorRippleParticleType extends ParticleType<RippleOptions> {
    public AmethystSoundSuppressorRippleParticleType(boolean overrideLimiter) {
        super(overrideLimiter);
    }

    @Override
    public MapCodec<RippleOptions> codec() {
        return RippleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, RippleOptions> streamCodec() {
        return RippleOptions.STREAM_CODEC;
    }

    public record RippleOptions(int delay) implements ParticleOptions {
        private static final MapCodec<RippleOptions> CODEC = Codec.INT.fieldOf("delay").xmap(RippleOptions::new, RippleOptions::delay);
        private static final StreamCodec<RegistryFriendlyByteBuf, RippleOptions> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, RippleOptions::delay, RippleOptions::new);

        @Override
        public ParticleType<?> getType() {
            return CCBParticleTypes.AMETHYST_SOUND_SUPPRESSOR_RIPPLE.get();
        }
    }
}
