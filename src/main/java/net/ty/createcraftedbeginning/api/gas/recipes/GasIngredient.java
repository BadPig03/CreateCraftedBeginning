package net.ty.createcraftedbeginning.api.gas.recipes;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasTags;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public abstract sealed class GasIngredient implements Predicate<GasStack> {
    public static final Codec<GasIngredient> CODEC = Type.CODEC.dispatch(GasIngredient::getType, type -> type.codec);
    public static final StreamCodec<RegistryFriendlyByteBuf, GasIngredient> STREAM_CODEC = Type.STREAM_CODEC.dispatch(GasIngredient::getType, type -> type.streamCodec);

    public List<GasStack> matchingGasStacks;
    protected long amountRequired;

    @Contract(" -> new")
    public static @NotNull GasIngredient empty() {
        return new GasStackIngredient();
    }

    @Contract("_, _ -> new")
    public static @NotNull GasIngredient fromTag(TagKey<Gas> tag, int amount) {
        return new GasTagIngredient(tag, amount);
    }

    public static @NotNull GasIngredient fromGas(Gas gas, int amount) {
        return new GasStackIngredient(gas, amount);
    }

    public static @NotNull GasIngredient fromGasStack(@NotNull GasStack gasStack) {
        return new GasStackIngredient(gasStack.getGas(), gasStack.getAmount());
    }

    public static void write(@NotNull RegistryFriendlyByteBuf buffer, @NotNull GasIngredient ingredient) {
        buffer.writeBoolean(ingredient instanceof GasTagIngredient);
        buffer.writeVarLong(ingredient.amountRequired);
        ingredient.writeInternal(buffer);
    }

    public static @NotNull GasIngredient read(@NotNull RegistryFriendlyByteBuf buffer) {
        boolean isTagIngredient = buffer.readBoolean();
        GasIngredient ingredient = isTagIngredient ? new GasTagIngredient() : new GasStackIngredient();
        ingredient.amountRequired = buffer.readVarLong();
        ingredient.readInternal(buffer);
        return ingredient;
    }

    protected abstract void writeInternal(RegistryFriendlyByteBuf buffer);

    protected abstract void readInternal(RegistryFriendlyByteBuf buffer);

    public long getRequiredAmount() {
        return amountRequired;
    }

    protected abstract Type getType();

    public List<GasStack> getMatchingGasStacks() {
        return matchingGasStacks != null ? matchingGasStacks : (matchingGasStacks = determineMatchingGasStacks());
    }

    protected abstract List<GasStack> determineMatchingGasStacks();

    @Override
    public boolean test(GasStack gasStack) {
        if (gasStack == null) {
            throw new IllegalArgumentException("GasStack cannot be null");
        }

        return testInternal(gasStack);
    }

    protected abstract boolean testInternal(GasStack gasStack);

    protected enum Type implements StringRepresentable {
        GAS_STACK(GasStackIngredient.CODEC, GasStackIngredient.STREAM_CODEC),
        GAS_TAG(GasTagIngredient.CODEC, GasTagIngredient.STREAM_CODEC);

        public static final Codec<Type> CODEC = StringRepresentable.fromValues(Type::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, Type> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(Type.class);

        private final MapCodec<? extends GasIngredient> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, ? extends GasIngredient> streamCodec;

        Type(MapCodec<? extends GasIngredient> codec, StreamCodec<RegistryFriendlyByteBuf, ? extends GasIngredient> streamCodec) {
            this.codec = codec;
            this.streamCodec = streamCodec;
        }

        @Override
        public @NotNull String getSerializedName() {
            return CreateCraftedBeginning.MOD_ID + ':' + Lang.asId(name());
        }
    }

    public static final class GasStackIngredient extends GasIngredient {
        public static final StreamCodec<RegistryFriendlyByteBuf, GasStackIngredient> STREAM_CODEC = StreamCodec.composite(Gas.GAS_STREAM_CODEC, i -> i.gas, ByteBufCodecs.VAR_LONG, i -> i.amountRequired, GasStackIngredient::new);
        private static final Codec<Gas> GAS_NON_AIR_CODEC = CCBGasRegistries.GAS_REGISTRY.byNameCodec().validate(gas -> gas == Gas.EMPTY_GAS_HOLDER.value() ? DataResult.error(() -> "Gas must not be minecraft:empty") : DataResult.success(gas));
        public static final MapCodec<GasStackIngredient> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(GAS_NON_AIR_CODEC.fieldOf("gas").forGetter(gsi -> gsi.gas), Codec.LONG.fieldOf("amount").forGetter(gsi -> gsi.amountRequired)).apply(i, GasStackIngredient::new));
        private Gas gas;

        public GasStackIngredient() {
        }

        public GasStackIngredient(Gas gas, long amountRequired) {
            this.gas = gas;
            this.amountRequired = amountRequired;
        }

        @Override
        protected void writeInternal(RegistryFriendlyByteBuf buffer) {
            Gas.GAS_STREAM_CODEC.encode(buffer, gas);
        }

        @Override
        protected void readInternal(RegistryFriendlyByteBuf buffer) {
            gas = Gas.GAS_STREAM_CODEC.decode(buffer);
        }

        @Override
        protected Type getType() {
            return Type.GAS_STACK;
        }

        @Override
        protected @NotNull @Unmodifiable List<GasStack> determineMatchingGasStacks() {
            return ImmutableList.of(new GasStack(gas.getHolder(), amountRequired));
        }

        @Override
        protected boolean testInternal(@NotNull GasStack gasStack) {
            return gasStack.is(gas);
        }
    }

    public static final class GasTagIngredient extends GasIngredient {
        public static final MapCodec<GasTagIngredient> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(TagKey.codec(CCBRegistries.GAS_REGISTRY_KEY).fieldOf("gas_tag").forGetter(gti -> gti.tag), Codec.LONG.fieldOf("amount").forGetter(gti -> gti.amountRequired)).apply(i, GasTagIngredient::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, GasTagIngredient> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC, i -> i.tag.location(), ByteBufCodecs.VAR_LONG, i -> i.amountRequired, (tag, amount) -> new GasTagIngredient(TagKey.create(CCBRegistries.GAS_REGISTRY_KEY, tag), amount));

        private TagKey<Gas> tag;

        public GasTagIngredient() {
        }

        public GasTagIngredient(TagKey<Gas> tag, long amountRequired) {
            this.tag = tag;
            this.amountRequired = amountRequired;
        }

        @Override
        protected void writeInternal(RegistryFriendlyByteBuf buffer) {
            GasStack.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, getMatchingGasStacks());
        }

        @Override
        protected void readInternal(RegistryFriendlyByteBuf buffer) {
            matchingGasStacks = GasStack.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer);
        }

        @Override
        protected Type getType() {
            return Type.GAS_TAG;
        }

        @Override
        protected @NotNull List<GasStack> determineMatchingGasStacks() {
            List<GasStack> stacks = new ArrayList<>();
            for (Holder<Gas> holder : CCBGasRegistries.GAS_REGISTRY.getTagOrEmpty(tag)) {
                stacks.add(new GasStack(holder, amountRequired));
            }
            return stacks;
        }

        @Override
        protected boolean testInternal(GasStack gasStack) {
            if (tag != null) {
                return GasTags.isTag(gasStack, tag);
            }

            return getMatchingGasStacks().stream().anyMatch(accepted -> GasStack.isSameGas(accepted, gasStack));
        }
    }
}
