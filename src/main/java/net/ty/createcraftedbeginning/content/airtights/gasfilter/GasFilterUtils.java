package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.GasStackLinkedSet;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasFilterUtils {
    private static final Predicate<GasStack> ALLOW_ALL = gas -> !gas.isEmpty();
    private static final Predicate<GasStack> DENY_ALL = gas -> false;

    private GasFilterUtils() {
    }

    public static boolean isFilter(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IGasFilter;
    }

    public static ItemStack normalizeStack(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    public static Predicate<GasStack> compile(ItemStack filterStack) {
        if (filterStack.isEmpty()) {
            return ALLOW_ALL;
        }

        if (!(filterStack.getItem() instanceof IGasFilter filter)) {
            return DENY_ALL;
        }

        Predicate<GasStack> compiled = filter.compile(normalizeStack(filterStack));
        return gas -> !gas.isEmpty() && compiled.test(gas);
    }

    public static boolean matches(ItemStack filterStack, GasStack gasStack) {
        return !gasStack.isEmpty() && (filterStack.isEmpty() || filterStack.getItem() instanceof IGasFilter filter && filter.test(filterStack, gasStack));
    }

    public record GasFilterData(boolean blacklist, List<GasStack> gases) {
        public static final int MAX_ENTRIES = 18;
        public static final GasFilterData EMPTY = new GasFilterData(false, List.of());
        public static final Codec<GasFilterData> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.BOOL.fieldOf("blacklist").forGetter(GasFilterData::blacklist), GasStack.CODEC.listOf(0, MAX_ENTRIES).fieldOf("gases").forGetter(data -> data.gases)).apply(instance, GasFilterData::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, GasFilterData> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public GasFilterData decode(RegistryFriendlyByteBuf buffer) {
                boolean blacklist = buffer.readBoolean();
                int size = buffer.readVarInt();
                if (size < 0 || size > MAX_ENTRIES) {
                    throw new DecoderException("Invalid gas filter entry count: " + size);
                }

                List<GasStack> gases = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    gases.add(GasStack.STREAM_CODEC.decode(buffer));
                }
                return new GasFilterData(blacklist, gases);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, GasFilterData data) {
                int size = data.gases.size();
                if (size > MAX_ENTRIES) {
                    throw new EncoderException("Too many gas filter entries: " + size);
                }

                buffer.writeBoolean(data.blacklist);
                buffer.writeVarInt(size);
                data.gases.forEach(gas -> GasStack.STREAM_CODEC.encode(buffer, gas));
            }
        };

        public GasFilterData {
            gases = normalize(gases);
        }

        private static List<GasStack> normalize(List<GasStack> input) {
            if (input.isEmpty()) {
                return List.of();
            }

            Set<GasStack> seen = GasStackLinkedSet.createTypeAndComponentsSet();
            List<GasStack> normalized = new ArrayList<>(Math.min(input.size(), MAX_ENTRIES));
            for (GasStack candidate : input) {
                if (candidate == null || candidate.isEmpty()) {
                    continue;
                }

                GasStack gas = candidate.copyWithAmount(1);
                if (!seen.add(gas)) {
                    continue;
                }

                normalized.add(gas);
                if (normalized.size() == MAX_ENTRIES) {
                    break;
                }
            }
            return List.copyOf(normalized);
        }

        @Override
        public @Unmodifiable List<GasStack> gases() {
            return gases.stream().map(GasStack::copy).toList();
        }

        public boolean isDefault() {
            return !blacklist && gases.isEmpty();
        }

        public boolean test(GasStack gas) {
            if (gas.isEmpty()) {
                return false;
            }

            for (GasStack entry : gases) {
                if (!GasStack.isSameGasSameComponents(entry, gas)) {
                    continue;
                }

                return !blacklist;
            }
            return blacklist;
        }

        public Predicate<GasStack> compile() {
            Set<GasStack> configuredGases = GasStackLinkedSet.createTypeAndComponentsSet();
            configuredGases.addAll(gases);
            return gas -> !gas.isEmpty() && blacklist != configuredGases.contains(gas);
        }
    }
}
