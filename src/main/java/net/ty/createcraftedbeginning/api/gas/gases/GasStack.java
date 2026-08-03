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
 * Represents a gas type, an amount, and an associated data-component patch.
 * This value object provides serialization, comparison, mutation, and display helpers for gas transfers.
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
        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, GasStack stack) {
            if (stack.isEmpty()) {
                buffer.writeVarLong(0);
                return;
            }

            buffer.writeVarLong(stack.getAmount());
            Gas.HOLDER_STREAM_CODEC.encode(buffer, stack.getGasHolder());
            DataComponentPatch.STREAM_CODEC.encode(buffer, stack.components.asPatch());
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, GasStack> STREAM_CODEC = new StreamCodec<>() {
        /**
         * {@inheritDoc}
         */
        @Override
        public GasStack decode(RegistryFriendlyByteBuf buffer) {
            GasStack stack = OPTIONAL_STREAM_CODEC.decode(buffer);
            if (stack.isEmpty()) {
                throw new DecoderException("Empty GasStack not allowed");
            }

            return stack;
        }

        /**
         * {@inheritDoc}
         */
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

    /**
     * Creates a new {@code GasStack} instance.
     *
     * @param gasHolder the gas holder to use
     * @param amount    the amount to use
     * @param patch     the patch to use
     */
    public GasStack(Holder<Gas> gasHolder, long amount, DataComponentPatch patch) {
        this(gasHolder.value(), amount, PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, patch));
    }

    /**
     * Creates a new {@code GasStack} instance.
     *
     * @param gasType the gas type to inspect or process
     * @param amount  the amount to use
     * @param patch   the patch to use
     */
    public GasStack(Gas gasType, long amount, DataComponentPatch patch) {
        this(gasType, amount, PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, patch));
    }

    /**
     * Creates a new {@code GasStack} instance.
     *
     * @param gasHolder the gas holder to use
     * @param amount    the amount to use
     */
    public GasStack(Holder<Gas> gasHolder, long amount) {
        this(gasHolder.value(), amount, new PatchedDataComponentMap(DataComponentMap.EMPTY));
    }

    /**
     * Creates a new {@code GasStack} instance.
     *
     * @param gasType the gas type to inspect or process
     * @param amount  the amount to use
     */
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
     * Parses the optional from the supplied input.
     *
     * @param lookupProvider the lookup provider to use
     * @param compoundTag    the NBT compound to read from or write to
     * @return the created value
     */
    public static GasStack parseOptional(Provider lookupProvider, CompoundTag compoundTag) {
        return compoundTag.isEmpty() ? EMPTY : parse(lookupProvider, compoundTag).orElse(EMPTY);
    }

    /**
     * Parses the value from the supplied input.
     *
     * @param lookupProvider the lookup provider to use
     * @param tag            the tag to inspect or process
     * @return an optional containing the parsed value, or an empty optional when parsing fails
     */
    public static Optional<GasStack> parse(Provider lookupProvider, Tag tag) {
        return CODEC.parse(lookupProvider.createSerializationContext(NbtOps.INSTANCE), tag).resultOrPartial(error -> CreateCraftedBeginning.LOGGER.error("Tried to parse invalid gas holder: '{}'", error));
    }

    /**
     * Checks whether the supplied value matches this condition.
     *
     * @param first  the first value to compare or combine
     * @param second the second value to compare or combine
     * @return {@code true} if the supplied value matches this condition; otherwise {@code false}
     */
    public static boolean matches(GasStack first, GasStack second) {
        return first == second || first.getAmount() == second.getAmount() && isSameGasSameComponents(first, second);
    }

    /**
     * Checks whether this value is same gas same components.
     *
     * @param first  the first value to compare or combine
     * @param second the second value to compare or combine
     * @return {@code true} if this value is same gas same components; otherwise {@code false}
     */
    public static boolean isSameGasSameComponents(GasStack first, GasStack second) {
        return first.is(second.getGasType()) && (first.isEmpty() && second.isEmpty() || Objects.equals(first.components, second.components));
    }

    /**
     * Checks whether this value is same gas.
     *
     * @param first  the first value to compare or combine
     * @param second the second value to compare or combine
     * @return {@code true} if this value is same gas; otherwise {@code false}
     */
    public static boolean isSameGas(GasStack first, GasStack second) {
        return first.is(second.getGasHolder());
    }

    /**
     * Checks whether this value has h gas and components.
     *
     * @param stack the stack to inspect or process
     * @return {@code true} if this value has h gas and components; otherwise {@code false}
     */
    public static int hashGasAndComponents(@Nullable GasStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        int result = stack.getGasHolder().hashCode();
        result = 31 * result + stack.components.hashCode();
        return result;
    }

    /**
     * Returns the gas holder.
     *
     * @return the gas holder
     */
    public Holder<Gas> getGasHolder() {
        return isEmpty() || gasHolder == null ? Gas.EMPTY_GAS_HOLDER : gasHolder;
    }

    /**
     * Checks whether this value is empty.
     *
     * @return {@code true} if this value is empty; otherwise {@code false}
     */
    public boolean isEmpty() {
        return this == EMPTY || gasHolder == null || gasHolder.value().isEmpty() || amount <= 0;
    }

    /**
     * Checks whether this gas belongs to the supplied tag.
     *
     * @param tag the tag to inspect or process
     * @return {@code true} if this gas belongs to the supplied tag; otherwise {@code false}
     */
    public boolean is(TagKey<Gas> tag) {
        return getGasHolder().is(tag);
    }

    /**
     * Checks whether this gas belongs to the supplied tag.
     *
     * @param predicate the predicate used to select matching values
     * @return {@code true} if this gas belongs to the supplied tag; otherwise {@code false}
     */
    public boolean is(Predicate<Holder<Gas>> predicate) {
        return predicate.test(getGasHolder());
    }

    /**
     * Checks whether this gas belongs to the supplied tag.
     *
     * @param holderSet the holder set to inspect or process
     * @return {@code true} if this gas belongs to the supplied tag; otherwise {@code false}
     */
    public boolean is(HolderSet<Gas> holderSet) {
        return holderSet.contains(getGasHolder());
    }

    /**
     * Checks whether this gas belongs to the supplied tag.
     *
     * @param holder the gas holder to inspect or process
     * @return {@code true} if this gas belongs to the supplied tag; otherwise {@code false}
     */
    public boolean is(Holder<Gas> holder) {
        return is(holder.value());
    }

    /**
     * Checks whether this gas belongs to the supplied tag.
     *
     * @param gasType the gas type to inspect or process
     * @return {@code true} if this gas belongs to the supplied tag; otherwise {@code false}
     */
    public boolean is(Gas gasType) {
        return getGasType() == gasType;
    }

    /**
     * Returns the gas type.
     *
     * @return the gas type
     */
    public Gas getGasType() {
        return isEmpty() ? Gas.EMPTY_GAS_HOLDER.value() : getGasHolder().value();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PatchedDataComponentMap getComponents() {
        return isEmpty() ? new PatchedDataComponentMap(DataComponentMap.EMPTY) : components;
    }

    /**
     * Returns the components patch.
     *
     * @return the components patch
     */
    public DataComponentPatch getComponentsPatch() {
        return isEmpty() ? DataComponentPatch.EMPTY : components.asPatch();
    }

    /**
     * Checks whether this value is components patch empty.
     *
     * @return {@code true} if this value is components patch empty; otherwise {@code false}
     */
    public boolean isComponentsPatchEmpty() {
        return isEmpty() || components.isPatchEmpty();
    }

    /**
     * Creates a copy of this stack with the supplied amount.
     *
     * @param amount the amount to use
     * @return the created value
     */
    public GasStack copyWithAmount(long amount) {
        if (isEmpty() || amount <= 0) {
            return EMPTY;
        }

        GasStack copy = copy();
        copy.amount = amount;
        return copy;
    }

    /**
     * Creates an independent copy of this instance.
     *
     * @return the created value
     */
    public GasStack copy() {
        return isEmpty() ? EMPTY : new GasStack(getGasHolder(), getAmount(), components.copy());
    }

    /**
     * Returns the amount.
     *
     * @return the amount
     */
    public long getAmount() {
        return isEmpty() ? 0 : amount;
    }

    /**
     * Sets the amount.
     *
     * @param amount the amount to use
     */
    public void setAmount(long amount) {
        if (this == EMPTY || gasHolder == null || gasHolder.value().isEmpty()) {
            return;
        }

        this.amount = Math.max(0, amount);
    }

    /**
     * Reduces this stack by the supplied amount.
     *
     * @param amount the amount to use
     */
    public void shrink(long amount) {
        if (isEmpty()) {
            return;
        }

        grow(-amount);
    }

    /**
     * Increases this stack by the supplied amount.
     *
     * @param amount the amount to use
     */
    public void grow(long amount) {
        if (isEmpty()) {
            return;
        }

        setAmount(this.amount + amount);
    }

    /**
     * Returns the tags.
     *
     * @return the tags
     */
    public Stream<TagKey<Gas>> getTags() {
        return getGasHolder().tags();
    }

    /**
     * Serializes this object's state.
     *
     * @param provider the provider used to resolve the requested value
     * @return the resulting tag
     */
    public Tag save(Provider provider) {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot encode empty GasStack");
        }

        return DataComponentUtil.wrapEncodingExceptions(this, CODEC, provider);
    }

    /**
     * Serializes the optional.
     *
     * @param provider the provider used to resolve the requested value
     * @return the resulting tag
     */
    public Tag saveOptional(Provider provider) {
        return isEmpty() ? new CompoundTag() : save(provider, new CompoundTag());
    }

    /**
     * Serializes this object's state.
     *
     * @param provider the provider used to resolve the requested value
     * @param tag      the tag to inspect or process
     * @return the resulting tag
     */
    public Tag save(Provider provider, Tag tag) {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot encode empty GasStack");
        }

        return DataComponentUtil.wrapEncodingExceptions(this, CODEC, provider, tag);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public <T> T set(DataComponentType<? super T> type, @Nullable T component) {
        if (isEmpty()) {
            return null;
        }
        return components.set(type, component);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public <T> T remove(DataComponentType<? extends T> type) {
        if (isEmpty()) {
            return null;
        }
        return components.remove(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyComponents(DataComponentPatch patch) {
        if (isEmpty()) {
            return;
        }

        components.applyPatch(patch);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyComponents(DataComponentMap componentMap) {
        if (isEmpty()) {
            return;
        }

        components.setAll(componentMap);
    }

    /**
     * Returns the hover name.
     *
     * @return the hover name
     */
    @Contract(" -> new")
    public Component getHoverName() {
        return Component.translatable(getGasType().getTranslationKey());
    }

    /**
     * Returns the translation key.
     *
     * @return the translation key
     */
    public String getTranslationKey() {
        return getGasType().getTranslationKey();
    }

    /**
     * Returns the hint.
     *
     * @return the hint
     */
    public int getHint() {
        return getGasType().getTint();
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof GasStack other)) {
            return false;
        }

        if (isEmpty()) {
            return other.isEmpty();
        }
        return !other.isEmpty() && getAmount() == other.getAmount() && is(other.getGasHolder()) && Objects.equals(components, other.components);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getAmount() + " " + getGasType();
    }
}