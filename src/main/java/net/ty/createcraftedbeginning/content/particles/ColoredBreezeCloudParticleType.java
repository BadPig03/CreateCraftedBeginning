package net.ty.createcraftedbeginning.content.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.ty.createcraftedbeginning.content.particles.ColoredBreezeCloudParticleType.ColoredBreezeCloudParticleOptions;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ColoredBreezeCloudParticleType extends ParticleType<ColoredBreezeCloudParticleOptions> {
    public ColoredBreezeCloudParticleType(boolean overrideLimiter) {
        super(overrideLimiter);
    }

    @Override
    public MapCodec<ColoredBreezeCloudParticleOptions> codec() {
        return ColoredBreezeCloudParticleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ColoredBreezeCloudParticleOptions> streamCodec() {
        return ColoredBreezeCloudParticleOptions.STREAM_CODEC;
    }

    public record ColoredBreezeCloudParticleOptions(int color) implements ParticleOptions {
        public static final MapCodec<ColoredBreezeCloudParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(Codec.INT.fieldOf("color").forGetter(ColoredBreezeCloudParticleOptions::color)).apply(instance, ColoredBreezeCloudParticleOptions::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ColoredBreezeCloudParticleOptions> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, ColoredBreezeCloudParticleOptions::color, ColoredBreezeCloudParticleOptions::new);

        @Override
        public ParticleType<?> getType() {
            return CCBParticleTypes.COLORED_BREEZE_CLOUD.get();
        }
    }
}