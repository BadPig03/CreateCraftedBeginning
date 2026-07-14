package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.mojang.serialization.Codec;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record BalloonGasContents(List<GasStack> gases) {
    public static final BalloonGasContents EMPTY = new BalloonGasContents(List.of());
    public static final Codec<BalloonGasContents> CODEC = GasStack.CODEC.listOf().xmap(BalloonGasContents::new, BalloonGasContents::gases);

    private static final int MAX_NETWORK_GAS_TYPES = 1024;

    public static final StreamCodec<RegistryFriendlyByteBuf, BalloonGasContents> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BalloonGasContents decode(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            if (size < 0 || size > MAX_NETWORK_GAS_TYPES) {
                throw new DecoderException("Invalid balloon gas type count: " + size);
            }

            List<GasStack> gases = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                gases.add(GasStack.STREAM_CODEC.decode(buffer));
            }
            return new BalloonGasContents(gases);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, BalloonGasContents contents) {
            BalloonGasContents normalized = contents.normalized();
            int size = normalized.gases.size();
            if (size > MAX_NETWORK_GAS_TYPES) {
                throw new EncoderException("Too many gas types in balloon: " + size);
            }

            buffer.writeVarInt(size);
            normalized.gases.forEach(gas -> GasStack.STREAM_CODEC.encode(buffer, gas));
        }
    };

    public BalloonGasContents {
        gases = normalize(gases);
    }

    public static BalloonGasContents parseOptional(Provider provider, Tag tag) {
        if (tag instanceof CompoundTag compoundTag && compoundTag.isEmpty()) {
            return EMPTY;
        }

        Optional<BalloonGasContents> parsed = CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag).resultOrPartial(error -> CreateCraftedBeginning.LOGGER.error("Tried to parse invalid balloon gas contents: '{}'", error));
        return parsed.orElse(EMPTY).normalized();
    }

    private static List<GasStack> normalize(List<GasStack> input) {
        if (input.isEmpty()) {
            return List.of();
        }

        List<GasStack> normalized = new ArrayList<>();
        for (GasStack candidate : input) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }

            GasStack copy = candidate.copy();
            int matchingIndex = findMatching(normalized, copy);
            if (matchingIndex < 0) {
                normalized.add(copy);
                continue;
            }

            GasStack existing = normalized.get(matchingIndex);
            normalized.set(matchingIndex, existing.copyWithAmount(saturatedAdd(existing.getAmount(), copy.getAmount())));
        }
        return List.copyOf(normalized);
    }

    private static int findMatching(List<GasStack> gases, GasStack target) {
        return IntStream.range(0, gases.size()).filter(i -> GasStack.isSameGasSameComponents(gases.get(i), target)).findFirst().orElse(-1);
    }

    private static long saturatedAdd(long current, long addition) {
        if (current <= 0) {
            return Math.max(0, addition);
        }
        if (addition <= 0) {
            return current;
        }
        return current > Long.MAX_VALUE - addition ? Long.MAX_VALUE : current + addition;
    }

    @Override
    public @Unmodifiable List<GasStack> gases() {
        return gases.stream().map(GasStack::copy).toList();
    }

    public boolean isEmpty() {
        return gases.isEmpty();
    }

    public long totalAmount() {
        long total = 0;
        for (GasStack gas : gases) {
            total = saturatedAdd(total, gas.getAmount());
        }
        return total;
    }

    public BalloonGasContents normalized() {
        return this;
    }

    public BalloonGasContents copy() {
        return isEmpty() ? EMPTY : new BalloonGasContents(gases);
    }

    public Tag save(Provider provider) {
        return CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), normalized()).getOrThrow();
    }

    public Tag saveOptional(Provider provider) {
        return isEmpty() ? new CompoundTag() : save(provider);
    }
}
