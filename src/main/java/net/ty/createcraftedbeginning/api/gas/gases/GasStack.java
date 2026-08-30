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
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasStack implements MutableDataComponentHolder {
    public static final GasStack EMPTY = new GasStack(null);
    public static final Codec<Holder<Gas>> GAS_NON_EMPTY_CODEC = GasRegistries.GAS_REGISTRY.holderByNameCodec().validate(holder -> holder.value().isEmpty() ? DataResult.error(() -> "Gas must not be empty") : DataResult.success(holder));
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
                return;
            }

            buffer.writeVarLong(stack.getAmount());
            Gas.HOLDER_STREAM_CODEC.encode(buffer, stack.getGasHolder());
            DataComponentPatch.STREAM_CODEC.encode(buffer, stack.components.asPatch());
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
    private static final Codec<Long> POSITIVE_AMOUNT_CODEC = Codec.LONG.validate(amount -> amount > 0 ? DataResult.success(amount) : DataResult.error(() -> "Gas amount must be positive"));
    public static final Codec<GasStack> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(Gas.HOLDER_CODEC.validate(gas -> gas.value().isEmpty() ? DataResult.error(() -> "Gas must not be empty") : DataResult.success(gas)).fieldOf("id").forGetter(GasStack::getGasHolder), POSITIVE_AMOUNT_CODEC.fieldOf("amount").forGetter(GasStack::getAmount), DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(stack -> stack.components.asPatch())).apply(instance, GasStack::new)));
    public static final Codec<GasStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC).xmap(optional -> optional.orElse(EMPTY), stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack));
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

    public static GasStack parseOptional(Provider lookupProvider, CompoundTag compoundTag) {
        if (CCBNbtUtils.isEmpty(compoundTag)) {
            return EMPTY;
        }
        return parse(lookupProvider, compoundTag).orElse(EMPTY);
    }

    public static Optional<GasStack> parse(Provider lookupProvider, Tag tag) {
        return CODEC.parse(lookupProvider.createSerializationContext(NbtOps.INSTANCE), tag).resultOrPartial(error -> CCBAPI.LOGGER.error("Tried to parse invalid gas holder: '{}'", error));
    }

    public static boolean matches(GasStack first, GasStack second) {
        return first == second || first.getAmount() == second.getAmount() && isSameGasSameComponents(first, second);
    }

    public static boolean isSameGasSameComponents(GasStack first, GasStack second) {
        return first.is(second.getGasType()) && (first.isEmpty() && second.isEmpty() || Objects.equals(first.components, second.components));
    }

    @SuppressWarnings("unused")
    public static boolean isSameGas(GasStack first, GasStack second) {
        return first.is(second.getGasHolder());
    }

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
        if (isEmpty()) {
            return Gas.EMPTY_GAS_HOLDER.value();
        }
        return getGasHolder().value();
    }

    @Override
    public PatchedDataComponentMap getComponents() {
        if (isEmpty()) {
            return new PatchedDataComponentMap(DataComponentMap.EMPTY);
        }
        return components;
    }

    public DataComponentPatch getComponentsPatch() {
        if (isEmpty()) {
            return DataComponentPatch.EMPTY;
        }
        return components.asPatch();
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
        if (isEmpty()) {
            return EMPTY;
        }
        return new GasStack(getGasHolder(), getAmount(), components.copy());
    }

    public long getAmount() {
        if (isEmpty()) {
            return 0;
        }
        return amount;
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

        setAmount(CCBMathUtils.saturatedSubtract(this.amount, amount));
    }

    public void grow(long amount) {
        if (isEmpty()) {
            return;
        }

        setAmount(CCBMathUtils.saturatedAdd(this.amount, amount));
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
        if (isEmpty()) {
            return new CompoundTag();
        }
        return save(provider, new CompoundTag());
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

    @Override
    public String toString() {
        return getAmount() + " " + getGasType();
    }
}