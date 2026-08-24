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

        if (!(filterStack.getItem() instanceof IGasFilter gasFilter)) {
            return DENY_ALL;
        }

        Predicate<GasStack> compiledFilter = gasFilter.compile(normalizeStack(filterStack));
        return gasStack -> !gasStack.isEmpty() && compiledFilter.test(gasStack);
    }

    public static boolean matches(ItemStack filterStack, GasStack gasStack) {
        return !gasStack.isEmpty() && (filterStack.isEmpty() || filterStack.getItem() instanceof IGasFilter filter && filter.test(filterStack, gasStack));
    }

    public record GasFilterData(boolean blacklist, boolean respectData, List<GasStack> gases) {
        public static final int MAX_ENTRIES = 18;
        public static final GasFilterData EMPTY = new GasFilterData(false, false, List.of());
        public static final Codec<GasFilterData> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.BOOL.fieldOf("blacklist").forGetter(GasFilterData::blacklist), Codec.BOOL.optionalFieldOf("respect_data", true).forGetter(GasFilterData::respectData), GasStack.CODEC.listOf(0, MAX_ENTRIES).fieldOf("gases").forGetter(filterData -> filterData.gases)).apply(instance, GasFilterData::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, GasFilterData> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public GasFilterData decode(RegistryFriendlyByteBuf buffer) {
                boolean blacklist = buffer.readBoolean();
                boolean respectData = buffer.readBoolean();
                int entryCount = buffer.readVarInt();
                if (entryCount < 0 || entryCount > MAX_ENTRIES) {
                    throw new DecoderException("Invalid gas filter entry count: " + entryCount);
                }

                List<GasStack> configuredGases = new ArrayList<>(entryCount);
                for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
                    configuredGases.add(GasStack.STREAM_CODEC.decode(buffer));
                }
                return new GasFilterData(blacklist, respectData, configuredGases);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, GasFilterData filterData) {
                int entryCount = filterData.gases.size();
                if (entryCount > MAX_ENTRIES) {
                    throw new EncoderException("Too many gas filter entries: " + entryCount);
                }

                buffer.writeBoolean(filterData.blacklist);
                buffer.writeBoolean(filterData.respectData);
                buffer.writeVarInt(entryCount);
                filterData.gases.forEach(gas -> GasStack.STREAM_CODEC.encode(buffer, gas));
            }
        };

        public GasFilterData {
            gases = normalize(gases, respectData);
        }

        private static List<GasStack> normalize(List<GasStack> inputGases, boolean respectData) {
            if (inputGases.isEmpty()) {
                return List.of();
            }

            Set<GasStack> seenGases = respectData ? GasStackLinkedSet.createTypeAndComponentsSet() : GasStackLinkedSet.createTypeSet();
            List<GasStack> normalizedGases = new ArrayList<>(Math.min(inputGases.size(), MAX_ENTRIES));
            for (GasStack candidateGas : inputGases) {
                if (candidateGas == null || candidateGas.isEmpty()) {
                    continue;
                }

                GasStack normalizedGas = candidateGas.copyWithAmount(1);
                if (!seenGases.add(normalizedGas)) {
                    continue;
                }

                normalizedGases.add(normalizedGas);
                if (normalizedGases.size() != MAX_ENTRIES) {
                    continue;
                }

                break;
            }
            return List.copyOf(normalizedGases);
        }

        @Override
        public @Unmodifiable List<GasStack> gases() {
            return gases.stream().map(GasStack::copy).toList();
        }

        public boolean isDefault() {
            return !blacklist && !respectData && gases.isEmpty();
        }

        public boolean test(GasStack gasStack) {
            if (gasStack.isEmpty()) {
                return false;
            }

            for (GasStack configuredGas : gases) {
                boolean matches = respectData ? GasStack.isSameGasSameComponents(configuredGas, gasStack) : GasStack.isSameGas(configuredGas, gasStack);
                if (!matches) {
                    continue;
                }

                return !blacklist;
            }
            return blacklist;
        }

        public Predicate<GasStack> compile() {
            Set<GasStack> configuredGases = respectData ? GasStackLinkedSet.createTypeAndComponentsSet() : GasStackLinkedSet.createTypeSet();
            configuredGases.addAll(gases);
            return gasStack -> !gasStack.isEmpty() && blacklist != configuredGases.contains(gasStack);
        }
    }
}
