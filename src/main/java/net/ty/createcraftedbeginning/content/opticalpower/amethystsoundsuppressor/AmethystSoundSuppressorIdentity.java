package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public record AmethystSoundSuppressorIdentity(BlockPos blockPos, @Nullable UUID contraptionId) implements Comparable<AmethystSoundSuppressorIdentity> {
    static final StreamCodec<RegistryFriendlyByteBuf, AmethystSoundSuppressorIdentity> STREAM_CODEC = StreamCodec.of(AmethystSoundSuppressorIdentity::encode, AmethystSoundSuppressorIdentity::decode);

    public AmethystSoundSuppressorIdentity {
        blockPos = blockPos.immutable();
    }

    private static void encode(RegistryFriendlyByteBuf buffer, AmethystSoundSuppressorIdentity identity) {
        buffer.writeBlockPos(identity.blockPos);
        UUID contraptionId = identity.contraptionId;
        buffer.writeBoolean(contraptionId != null);
        if (contraptionId == null) {
            return;
        }

        buffer.writeUUID(contraptionId);
    }

    private static AmethystSoundSuppressorIdentity decode(RegistryFriendlyByteBuf buffer) {
        BlockPos blockPos = buffer.readBlockPos();
        if (!buffer.readBoolean()) {
            return new AmethystSoundSuppressorIdentity(blockPos, null);
        }

        return new AmethystSoundSuppressorIdentity(blockPos, buffer.readUUID());
    }

    @Override
    public int compareTo(AmethystSoundSuppressorIdentity other) {
        UUID otherId = other.contraptionId;
        if (contraptionId == null) {
            if (otherId != null) {
                return -1;
            }

            return blockPos.compareTo(other.blockPos);
        }

        if (otherId == null) {
            return 1;
        }

        int order = contraptionId.compareTo(otherId);
        if (order != 0) {
            return order;
        }

        return blockPos.compareTo(other.blockPos);
    }
}
