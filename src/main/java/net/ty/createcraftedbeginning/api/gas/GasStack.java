package net.ty.createcraftedbeginning.api.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.IWithData;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.interfaces.IHasTextComponent;
import net.ty.createcraftedbeginning.api.gas.interfaces.IHasTranslationKey;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.data.CCBSerializerHelper;
import net.ty.createcraftedbeginning.data.CCBGasRegistry;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public final class GasStack implements IHasTextComponent, IHasTranslationKey, IWithData<Gas> {
    public static final GasStack EMPTY = new GasStack(null);
    public static final Codec<Holder<Gas>> GAS_NON_EMPTY_HOLDER_CODEC = Gas.HOLDER_CODEC.validate(gas -> gas.is(CCBGasRegistry.EMPTY_GAS_KEY) ? DataResult.error(() -> "Gas must not be empty") : DataResult.success(gas));
    public static final MapCodec<GasStack> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(GAS_NON_EMPTY_HOLDER_CODEC.fieldOf("id").forGetter(GasStack::getGasHolder), CCBSerializerHelper.NON_NEGATIVE_LONG_CODEC.fieldOf("amount").forGetter(GasStack::getAmount)).apply(instance, GasStack::new));
    public static final Codec<GasStack> CODEC = MAP_CODEC.codec();
    public static final Codec<GasStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC).xmap(optional -> optional.orElse(EMPTY), stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack));
    public static final StreamCodec<RegistryFriendlyByteBuf, GasStack> OPTIONAL_STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull GasStack decode(@NotNull RegistryFriendlyByteBuf buffer) {
            long amount = buffer.readVarLong();
            if (amount <= 0) {
                return EMPTY;
            }
            return new GasStack(Gas.HOLDER_STREAM_CODEC.decode(buffer), amount);
        }

        @Override
        public void encode(@NotNull RegistryFriendlyByteBuf buffer, @NotNull GasStack stack) {
            buffer.writeVarLong(stack.getAmount());
            if (!stack.isEmpty()) {
                Gas.HOLDER_STREAM_CODEC.encode(buffer, stack.getGasHolder());
            }
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, GasStack> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull GasStack decode(@NotNull RegistryFriendlyByteBuf buffer) {
            GasStack stack = OPTIONAL_STREAM_CODEC.decode(buffer);
            if (stack.isEmpty()) {
                throw new DecoderException("Empty GasStack not allowed");
            }
            return stack;
        }

        @Override
        public void encode(@NotNull RegistryFriendlyByteBuf buffer, @NotNull GasStack stack) {
            if (stack.isEmpty()) {
                throw new EncoderException("Empty GasStack not allowed");
            }
            OPTIONAL_STREAM_CODEC.encode(buffer, stack);
        }
    };
    private static final Consumer<String> ON_STACK_LOAD_ERROR = error -> CreateCraftedBeginning.LOGGER.error("Tried to load invalid gas: '{}'", error);
    public static final Codec<GasStack> LENIENT_OPTIONAL_CODEC = OPTIONAL_CODEC.promotePartial(ON_STACK_LOAD_ERROR).orElse(EMPTY);
    @Nullable
    private final Holder<Gas> gas;
    private long amount;

    public GasStack(@Nullable Holder<Gas> gas, long amount) {
        Objects.requireNonNull(gas, "Cannot create a GasStack from a null gas holder");
        if (gas.kind() == Holder.Kind.DIRECT) {
            if (!gas.isBound()) {
                throw new IllegalArgumentException("Cannot create a GasStack from an unbound direct holder");
            }
            gas = CCBGasRegistry.GAS_REGISTRY.wrapAsHolder(gas.value());
            if (gas.kind() == Holder.Kind.DIRECT) {
                throw new IllegalArgumentException("Cannot create a GasStack from a direct holder for a gas that is not yet registered");
            }
        }
        this.gas = gas;
        this.amount = amount;
    }

    private GasStack(@Nullable Void unused) {
        this.gas = null;
    }

    public static Codec<GasStack> fixedAmountCodec(long amount) {
        return RecordCodecBuilder.create(instance -> instance.group(GAS_NON_EMPTY_HOLDER_CODEC.fieldOf("id").forGetter(GasStack::getGasHolder)).apply(instance, holder -> new GasStack(holder, amount)));
    }

    public static boolean isSameGas(@NotNull GasStack first, @NotNull GasStack second) {
        return first.is(second.getGasHolder());
    }

    public static boolean isSameGas(@NotNull GasStack stack, @NotNull Gas gas) {
        return stack.is(gas.getHolder());
    }

    public static boolean isSameGas(Gas first, Gas second) {
        return first == second;
    }

    public static Optional<GasStack> parse(HolderLookup.@NotNull Provider lookupProvider, Tag tag) {
        return CODEC.parse(lookupProvider.createSerializationContext(NbtOps.INSTANCE), tag).resultOrPartial(ON_STACK_LOAD_ERROR);
    }

    public static GasStack parseOptional(HolderLookup.Provider lookupProvider, @NotNull CompoundTag tag) {
        return tag.isEmpty() ? EMPTY : parse(lookupProvider, tag).orElse(EMPTY);
    }

    public GasStack copy() {
        if (isEmpty()) {
            return EMPTY;
        }
        return new GasStack(getGasHolder(), getAmount());
    }

    public GasStack copyWithAmount(long amount) {
        if (isEmpty() || amount == 0) {
            return EMPTY;
        }
        GasStack copy = copy();
        copy.setAmount(amount);
        return copy;
    }

    public GasStack split(long amount) {
        long i = Math.min(amount, getAmount());
        GasStack stack = copyWithAmount(i);
        this.shrink(i);
        return stack;
    }

    public GasStack copyAndClear() {
        if (isEmpty()) {
            return EMPTY;
        }
        GasStack stack = copy();
        this.setAmount(0);
        return stack;
    }

    public @NotNull Gas getGas() {
        return getGasHolder().value();
    }

    public Holder<Gas> getGasHolder() {
        return isEmpty() ? Gas.EMPTY_GAS_HOLDER : gas;
    }

    public boolean is(TagKey<Gas> tag) {
        return getGasHolder().is(tag);
    }

    public boolean is(Gas gas) {
        return getGas() == gas;
    }

    public boolean is(@NotNull Predicate<Holder<Gas>> predicate) {
        return predicate.test(getGasHolder());
    }

    public boolean is(@NotNull Holder<Gas> holder) {
        return is(holder.value());
    }

    public boolean is(@NotNull HolderSet<Gas> holderSet) {
        return holderSet.contains(getGasHolder());
    }

    public @NotNull Stream<TagKey<Gas>> getTags() {
        return getGasHolder().tags();
    }

    public Tag save(HolderLookup.Provider lookupProvider, Tag prefix) {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot encode empty GasStack");
        }
        return CODEC.encode(this, lookupProvider.createSerializationContext(NbtOps.INSTANCE), prefix).getOrThrow();
    }

    public Tag save(HolderLookup.Provider lookupProvider) {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot encode empty GasStack");
        }
        return CODEC.encodeStart(lookupProvider.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow();
    }

    public Tag saveOptional(HolderLookup.Provider lookupProvider) {
        return isEmpty() ? new CompoundTag() : save(lookupProvider);
    }

    public int getGasTint() {
        return getGas().getTint();
    }

    public boolean isEmpty() {
        return gas == null || gas.is(CCBGasRegistry.EMPTY_GAS_KEY) || this.amount <= 0;
    }

    public long getAmount() {
        return isEmpty() ? 0 : amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public void limitAmount(long amount) {
        if (isEmpty() || getAmount() <= amount) {
            return;
        }
        setAmount(amount);
    }

    public void grow(long amount) {
        setAmount(this.amount + amount);
    }

    public void shrink(long amount) {
        setAmount(this.amount - amount);
    }

    public void appendHoverText(Item.TooltipContext context, List<Component> tooltips, TooltipFlag tooltipFlag) {
        Holder<Gas> gasHolder = getGasHolder();
        if (gasHolder.is(CCBGasRegistry.EMPTY_GAS_KEY)) {
            return;
        }
        if (tooltipFlag.isAdvanced()) {
            tooltips.add(CCBLang.builder().text(ChatFormatting.DARK_GRAY, getGasHolder().getRegisteredName()).component());
        }
    }

    @Contract(" -> new")
    public @NotNull Component getHoverName() {
        return Component.translatable(getGas().getTranslationKey());
    }

    @Nullable
    @Override
    public <T> T getData(@NotNull DataMapType<Gas, T> type) {
        return getGasHolder().getData(type);
    }

    @Override
    public int hashCode() {
        if (isEmpty() || gas == null) {
            return 0;
        }
        int hash = gas.hashCode();
        return 31 * hash + Long.hashCode(amount);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GasStack other = (GasStack) obj;
        return getAmount() == other.getAmount() && is(other.getGasHolder());
    }

    @Override
    public String toString() {
        return getAmount() + " " + getGasHolder().getRegisteredName();
    }

    @Override
    public Component getTextComponent() {
        return getGas().getTextComponent();
    }

    @Override
    public String getTranslationKey() {
        return getGas().getTranslationKey();
    }
}
