package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.mojang.serialization.Codec;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BalloonGasContents {
    public static final BalloonGasContents EMPTY = new BalloonGasContents(List.of(), 0);
    public static final int MAX_GAS_TYPES = 64;
    public static final Codec<BalloonGasContents> CODEC = GasStack.CODEC.listOf(0, MAX_GAS_TYPES).xmap(BalloonGasContents::new, BalloonGasContents::copyGasStacks);

    public static final StreamCodec<RegistryFriendlyByteBuf, BalloonGasContents> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BalloonGasContents decode(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            if (size < 0 || size > MAX_GAS_TYPES) {
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
            int size = contents.gases.size();
            if (size > MAX_GAS_TYPES) {
                throw new EncoderException("Too many gas types in balloon: " + size);
            }

            buffer.writeVarInt(size);
            contents.gases.forEach(gas -> GasStack.STREAM_CODEC.encode(buffer, gas.toStack()));
        }
    };

    private final List<GasEntry> gases;
    private final long totalAmount;

    public BalloonGasContents(List<GasStack> gases) {
        this(normalize(gases));
    }

    private BalloonGasContents(List<GasEntry> gases, long totalAmount) {
        this.gases = gases;
        this.totalAmount = totalAmount;
    }

    private BalloonGasContents(NormalizedContents normalized) {
        this(normalized.gases(), normalized.totalAmount());
    }

    public static BalloonGasContents parseOptional(Provider provider, Tag tag) {
        if (tag instanceof CompoundTag compoundTag && compoundTag.isEmpty()) {
            return EMPTY;
        }
        return CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag).resultOrPartial(error -> CCBAPI.LOGGER.error("Tried to parse invalid balloon gas contents: '{}'", error)).orElse(EMPTY);
    }

    private static NormalizedContents normalize(List<GasStack> input) {
        if (input.isEmpty()) {
            return new NormalizedContents(List.of(), 0);
        }

        List<GasStack> merged = new ArrayList<>();
        for (GasStack gas : input) {
            if (gas == null || gas.isEmpty()) {
                continue;
            }

            GasStack copy = gas.copy();
            int index = findMatching(merged, copy);
            if (index < 0) {
                merged.add(copy);
                continue;
            }

            GasStack existing = merged.get(index);
            merged.set(index, existing.copyWithAmount(saturatedAdd(existing.getAmount(), copy.getAmount())));
        }

        if (merged.isEmpty()) {
            return new NormalizedContents(List.of(), 0);
        }

        List<GasEntry> entries = new ArrayList<>(merged.size());
        long total = 0;
        for (GasStack gas : merged) {
            GasEntry entry = GasEntry.from(gas);
            entries.add(entry);
            total = saturatedAdd(total, entry.getAmount());
        }
        return new NormalizedContents(List.copyOf(entries), total);
    }

    private static int findMatching(List<GasStack> gases, GasStack target) {
        for (int i = 0; i < gases.size(); i++) {
            if (GasStack.isSameGasSameComponents(gases.get(i), target)) {
                return i;
            }
        }
        return -1;
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

    public @Unmodifiable List<GasEntry> gases() {
        return gases;
    }

    public @Unmodifiable List<GasStack> copyGasStacks() {
        if (gases.isEmpty()) {
            return List.of();
        }
        return gases.stream().map(GasEntry::toStack).toList();
    }

    public boolean isEmpty() {
        return gases.isEmpty();
    }

    public long totalAmount() {
        return totalAmount;
    }

    public int gasTypeCount() {
        return gases.size();
    }

    public BalloonGasContents limitedTo(long maxAmount, int maxGasTypes) {
        if (isEmpty() || maxAmount <= 0 || maxGasTypes <= 0) {
            return EMPTY;
        }

        List<GasEntry> limited = new ArrayList<>(Math.min(gases.size(), maxGasTypes));
        long remaining = maxAmount;
        long total = 0;
        for (GasEntry gas : gases) {
            if (remaining <= 0 || limited.size() >= maxGasTypes) {
                break;
            }

            long amount = Math.min(remaining, gas.getAmount());
            if (amount <= 0) {
                continue;
            }

            limited.add(gas.withAmount(amount));
            total += amount;
            remaining -= amount;
        }
        return limited.isEmpty() ? EMPTY : new BalloonGasContents(List.copyOf(limited), total);
    }

    public BalloonGasContents normalized() {
        return this;
    }

    public BalloonGasContents copy() {
        return this;
    }

    public Tag save(Provider provider) {
        return CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow();
    }

    public Tag saveOptional(Provider provider) {
        return isEmpty() ? new CompoundTag() : save(provider);
    }

    @Override
    public int hashCode() {
        return gases.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof BalloonGasContents other && gases.equals(other.gases);
    }

    @Override
    public String toString() {
        return "BalloonGasContents[gases=" + gases + ']';
    }

    public record GasEntry(Holder<Gas> gasHolder, long amount, DataComponentPatch components) {
        public GasEntry {
            Objects.requireNonNull(gasHolder, "gasHolder");
            Objects.requireNonNull(components, "components");
            if (gasHolder.value().isEmpty() || amount <= 0) {
                throw new IllegalArgumentException("Balloon gas entries must be non-empty");
            }
        }

        private static GasEntry from(GasStack gas) {
            return new GasEntry(gas.getGasHolder(), gas.getAmount(), gas.getComponentsPatch());
        }

        public Gas getGasType() {
            return gasHolder.value();
        }

        public Holder<Gas> getGasHolder() {
            return gasHolder;
        }

        public long getAmount() {
            return amount;
        }

        private GasEntry withAmount(long amount) {
            return amount == this.amount ? this : new GasEntry(gasHolder, amount, components);
        }

        public GasStack toStack() {
            return new GasStack(gasHolder, amount, components);
        }

        public GasStack toStack(long amount) {
            return amount <= 0 ? GasStack.EMPTY : new GasStack(gasHolder, amount, components);
        }
    }

    private record NormalizedContents(List<GasEntry> gases, long totalAmount) {}
}
