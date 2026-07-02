package net.ty.createcraftedbeginning.api.gas.gases;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.common.util.DataComponentUtil;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Represents a stack of gas with amount and data components, similar to ItemStack but for gases.
 * Provides functionality for gas storage, manipulation, serialization, and comparison.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public final class GasStack implements MutableDataComponentHolder {
    public static final GasStack EMPTY = new GasStack(null);
    public static final Codec<GasStack> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(Gas.HOLDER_CODEC.validate(gas -> gas.value().isEmpty() ? DataResult.error(() -> "Gas must not be empty") : DataResult.success(gas)).fieldOf("id").forGetter(GasStack::getGasHolder), Codec.LONG.fieldOf("amount").forGetter(GasStack::getAmount), DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(stack -> stack.components.asPatch())).apply(instance, GasStack::new)));
    public static final Codec<GasStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC).xmap(optional -> optional.orElse(EMPTY), stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack));
    public static final Codec<Holder<Gas>> GAS_NON_EMPTY_CODEC = CCBGasRegistries.GAS_REGISTRY.holderByNameCodec().validate(holder -> holder.value().isEmpty() ? DataResult.error(() -> "Gas must not be minecraft:empty") : DataResult.success(holder));
    public static final StreamCodec<RegistryFriendlyByteBuf, GasStack> OPTIONAL_STREAM_CODEC = new StreamCodec<>() {
        @Override
        public GasStack decode(RegistryFriendlyByteBuf buffer) {
            long amount = buffer.readVarLong();
            if (amount <= 0) {
                return EMPTY;
            }

            Holder<Gas> holder = Gas.HOLDER_STREAM_CODEC.decode(buffer);
            DataComponentPatch patch = DataComponentPatch.STREAM_CODEC.decode(buffer);
            return new GasStack(holder, amount, patch);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, GasStack stack) {
            if (stack.isEmpty()) {
                buffer.writeVarLong(0);
            }
            else {
                buffer.writeVarLong(stack.getAmount());
                Gas.HOLDER_STREAM_CODEC.encode(buffer, stack.getGasHolder());
                DataComponentPatch.STREAM_CODEC.encode(buffer, stack.components.asPatch());
            }
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, GasStack> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public GasStack decode(RegistryFriendlyByteBuf buffer) {
            GasStack stack = OPTIONAL_STREAM_CODEC.decode(buffer);
            if (stack.isEmpty()) {
                throw new DecoderException("Empty GasStack not allowed");
            }

            return stack;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, GasStack stack) {
            if (stack.isEmpty()) {
                throw new EncoderException("Empty GasStack not allowed");
            }

            OPTIONAL_STREAM_CODEC.encode(buffer, stack);
        }
    };

    @Nullable
    private final Holder<Gas> gasHolder;
    private final PatchedDataComponentMap components;
    private long amount;

    public GasStack(Holder<Gas> gasHolder, long amount, DataComponentPatch patch) {
        this(gasHolder.value(), amount, PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, patch));
    }

    public GasStack(Gas gasType, long amount, DataComponentPatch patch) {
        this(gasType, amount, PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, patch));
    }

    public GasStack(Holder<Gas> gasHolder, long amount) {
        this(gasHolder.value(), amount, new PatchedDataComponentMap(DataComponentMap.EMPTY));
    }

    public GasStack(Gas gasType, long amount) {
        this(gasType, amount, new PatchedDataComponentMap(DataComponentMap.EMPTY));
    }

    private GasStack(Gas gasType, long amount, PatchedDataComponentMap components) {
        if (gasType.isEmpty() || amount <= 0) {
            gasHolder = null;
            this.amount = 0;
            this.components = new PatchedDataComponentMap(DataComponentMap.EMPTY);
            return;
        }

        gasHolder = gasType.getHolder();
        this.amount = amount;
        this.components = components;
    }

    private GasStack(Holder<Gas> gasHolder, long amount, PatchedDataComponentMap components) {
        if (gasHolder.value().isEmpty() || amount <= 0) {
            this.gasHolder = null;
            this.amount = 0;
            this.components = new PatchedDataComponentMap(DataComponentMap.EMPTY);
            return;
        }

        this.gasHolder = gasHolder;
        this.amount = amount;
        this.components = components;
    }

    private GasStack(@Nullable Void ignored) {
        gasHolder = null;
        components = new PatchedDataComponentMap(DataComponentMap.EMPTY);
    }

    /**
     * Parses a GasStack from the compound tag, returning {@link #EMPTY} if parsing fails or tag is empty.
     *
     * @param lookupProvider the holder lookup provider for deserialization context
     * @param compoundTag    the compound tag to parse from
     * @return the parsed GasStack, or {@link #EMPTY} if parsing fails or tag is empty
     */
    public static GasStack parseOptional(Provider lookupProvider, CompoundTag compoundTag) {
        return compoundTag.isEmpty() ? EMPTY : parse(lookupProvider, compoundTag).orElse(EMPTY);
    }

    /**
     * Parses a GasStack from a Tag, returning Optional.empty() if parsing fails.
     *
     * @param lookupProvider the holder lookup provider for deserialization context
     * @param tag            the tag to parse from
     * @return an Optional containing the parsed GasStack, or empty if parsing fails
     */
    public static Optional<GasStack> parse(Provider lookupProvider, Tag tag) {
        return CODEC.parse(lookupProvider.createSerializationContext(NbtOps.INSTANCE), tag).resultOrPartial(error -> CreateCraftedBeginning.LOGGER.error("Tried to parse invalid gas holder: '{}'", error));
    }

    /**
     * Checks if two GasStacks match exactly in amount, gas type, and components.
     *
     * @param first  the first GasStack to compare (cannot be null)
     * @param second the second GasStack to compare (cannot be null)
     * @return true if both stacks have same amount, gas type, and components
     */
    public static boolean matches(GasStack first, GasStack second) {
        return first == second || first.getAmount() == second.getAmount() && isSameGasSameComponents(first, second);
    }

    /**
     * Checks if two GasStacks have the same gas type and components. Ignores amount.
     *
     * @param first  the first GasStack to compare (cannot be null)
     * @param second the second GasStack to compare (cannot be null)
     * @return true if both stacks have same gas type and components
     */
    public static boolean isSameGasSameComponents(GasStack first, GasStack second) {
        return first.is(second.getGasType()) && (first.isEmpty() && second.isEmpty() || Objects.equals(first.components, second.components));
    }

    /**
     * Checks if two GasStacks contain the same gas type.
     *
     * @param first  the first GasStack to compare
     * @param second the second GasStack to compare
     * @return true if both stacks contain the same gas type
     */
    public static boolean isSameGas(GasStack first, GasStack second) {
        return first.is(second.getGasHolder());
    }

    /**
     * Generates a hash code based on gas type and components. Ignores amount.
     *
     * @param stack the GasStack to hash (can be null)
     * @return a hash code combining gas type and components, or 0 if stack is null
     */
    public static int hashGasAndComponents(@Nullable GasStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        int result = stack.getGasHolder().hashCode();
        result = 31 * result + stack.components.hashCode();
        return result;
    }

    public Holder<Gas> getGasHolder() {
        if (isEmpty() || gasHolder == null) {
            return Gas.EMPTY_GAS_HOLDER;
        }
        return gasHolder;
    }

    public boolean isEmpty() {
        return this == EMPTY || gasHolder == null || gasHolder.value().isEmpty() || amount <= 0;
    }

    public boolean is(TagKey<Gas> tag) {
        return getGasHolder().is(tag);
    }

    public boolean is(Predicate<Holder<Gas>> predicate) {
        return predicate.test(getGasHolder());
    }

    public boolean is(HolderSet<Gas> holderSet) {
        return holderSet.contains(getGasHolder());
    }

    public boolean is(Holder<Gas> holder) {
        return is(holder.value());
    }

    public boolean is(Gas gasType) {
        return getGasType() == gasType;
    }

    public Gas getGasType() {
        return isEmpty() ? Gas.EMPTY_GAS_HOLDER.value() : getGasHolder().value();
    }

    @Override
    public PatchedDataComponentMap getComponents() {
        return isEmpty() ? new PatchedDataComponentMap(DataComponentMap.EMPTY) : components;
    }

    public DataComponentPatch getComponentsPatch() {
        return isEmpty() ? DataComponentPatch.EMPTY : components.asPatch();
    }

    public boolean isComponentsPatchEmpty() {
        return isEmpty() || components.isPatchEmpty();
    }

    public GasStack copyWithAmount(long amount) {
        if (isEmpty() || amount <= 0) {
            return EMPTY;
        }

        GasStack copy = copy();
        copy.amount = amount;
        return copy;
    }

    public GasStack copy() {
        return isEmpty() ? EMPTY : new GasStack(getGasHolder(), getAmount(), components.copy());
    }

    public long getAmount() {
        return isEmpty() ? 0 : amount;
    }

    public void setAmount(long amount) {
        if (this == EMPTY || gasHolder == null || gasHolder.value().isEmpty()) {
            return;
        }

        this.amount = Math.max(0, amount);
    }

    public void shrink(long amount) {
        if (isEmpty()) {
            return;
        }

        grow(-amount);
    }

    public void grow(long amount) {
        if (isEmpty()) {
            return;
        }

        setAmount(this.amount + amount);
    }

    public Stream<TagKey<Gas>> getTags() {
        return getGasHolder().tags();
    }

    public Tag save(Provider provider) {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot encode empty GasStack");
        }

        return DataComponentUtil.wrapEncodingExceptions(this, CODEC, provider);
    }

    public Tag saveOptional(Provider provider) {
        return isEmpty() ? new CompoundTag() : save(provider, new CompoundTag());
    }

    public Tag save(Provider provider, Tag tag) {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot encode empty GasStack");
        }

        return DataComponentUtil.wrapEncodingExceptions(this, CODEC, provider, tag);
    }

    @Nullable
    @Override
    public <T> T set(DataComponentType<? super T> type, @Nullable T component) {
        if (isEmpty()) {
            return null;
        }

        return components.set(type, component);
    }

    @Nullable
    @Override
    public <T> T remove(DataComponentType<? extends T> type) {
        if (isEmpty()) {
            return null;
        }

        return components.remove(type);
    }

    @Override
    public void applyComponents(DataComponentPatch patch) {
        if (isEmpty()) {
            return;
        }

        components.applyPatch(patch);
    }

    @Override
    public void applyComponents(DataComponentMap componentMap) {
        if (isEmpty()) {
            return;
        }

        components.setAll(componentMap);
    }

    @Contract(" -> new")
    public Component getHoverName() {
        return Component.translatable(getGasType().getTranslationKey());
    }

    public String getTranslationKey() {
        return getGasType().getTranslationKey();
    }

    public int getHint() {
        return getGasType().getTint();
    }

    @Override
    public int hashCode() {
        if (isEmpty()) {
            return 0;
        }

        int result = getGasHolder().hashCode();
        result = 31 * result + Long.hashCode(getAmount());
        result = 31 * result + components.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof GasStack other && (isEmpty() && other.isEmpty() || !isEmpty() && !other.isEmpty() && getAmount() == other.getAmount() && is(other.getGasHolder()) && Objects.equals(components, other.components));
    }

    @Override
    public String toString() {
        return getAmount() + " " + getGasType();
    }
}