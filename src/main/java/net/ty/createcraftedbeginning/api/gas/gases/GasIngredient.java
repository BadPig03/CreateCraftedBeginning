package net.ty.createcraftedbeginning.api.gas.gases;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class GasIngredient implements Predicate<GasStack> {
    public static final StreamCodec<RegistryFriendlyByteBuf, GasIngredient> STREAM_CODEC = new StreamCodec<>() {
        private static final StreamCodec<RegistryFriendlyByteBuf, GasIngredient> DISPATCH_CODEC = ByteBufCodecs.registry(CCBRegistries.GAS_INGREDIENT_TYPES).dispatch(GasIngredient::getType, GasIngredientType::streamCodec);

        private static final StreamCodec<RegistryFriendlyByteBuf, List<GasStack>> GAS_LIST_CODEC = GasStack.STREAM_CODEC.apply(ByteBufCodecs.collection(NonNullList::createWithCapacity));

        @Override
        public void encode(@NotNull RegistryFriendlyByteBuf buf, @NotNull GasIngredient ingredient) {
            if (ingredient.isSimple()) {
                GAS_LIST_CODEC.encode(buf, Arrays.asList(ingredient.getStacks()));
            }
            else {
                buf.writeVarInt(-1);
                DISPATCH_CODEC.encode(buf, ingredient);
            }
        }

        @Override
        public @NotNull GasIngredient decode(@NotNull RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            if (size == -1) {
                return DISPATCH_CODEC.decode(buf);
            }

            return CompoundGasIngredient.of(Stream.generate(() -> GasStack.STREAM_CODEC.decode(buf)).limit(size).map(GasIngredient::single));
        }
    };

    public static final MapCodec<GasIngredient> SINGLE_OR_TAG_CODEC = MapCodec.recursive("GasIngredient.SINGLE_OR_TAG_CODEC", self -> singleOrTagCodec());
    public static final MapCodec<GasIngredient> MAP_CODEC_NONEMPTY = makeMapCodec();
    public static final Codec<GasIngredient> MAP_CODEC_CODEC = MAP_CODEC_NONEMPTY.codec();
    public static final Codec<GasIngredient> CODEC = codec(true);
    public static final Codec<GasIngredient> CODEC_NON_EMPTY = codec(false);

    public static final Codec<List<GasIngredient>> LIST_CODEC = MAP_CODEC_CODEC.listOf();
    public static final Codec<List<GasIngredient>> LIST_CODEC_NON_EMPTY = LIST_CODEC.validate(list -> {
        if (list.isEmpty()) {
            return DataResult.error(() -> "Gas ingredient cannot be empty, at least one item must be defined");
        }
        return DataResult.success(list);
    });

    private static MapCodec<GasIngredient> singleOrTagCodec() {
        return NeoForgeExtraCodecs.xor(SingleGasIngredient.CODEC, TagGasIngredient.CODEC).xmap(either -> either.map(id -> id, id -> id), ingredient -> {
            if (ingredient instanceof SingleGasIngredient gas) {
                return Either.left(gas);
            }
            else if (ingredient instanceof TagGasIngredient tag) {
                return Either.right(tag);
            }
            throw new IllegalStateException("Basic gas ingredient should be either a gas or a tag!");
        });
    }

    private static MapCodec<GasIngredient> makeMapCodec() {
        return NeoForgeExtraCodecs.<GasIngredientType<?>, GasIngredient, GasIngredient>dispatchMapOrElse(CCBGasRegistries.GAS_INGREDIENT_TYPES_REGISTRY.byNameCodec(), GasIngredient::getType, GasIngredientType::codec, SINGLE_OR_TAG_CODEC).xmap(either -> either.map(id -> id, id -> id), ingredient -> {
            if (ingredient instanceof SingleGasIngredient || ingredient instanceof TagGasIngredient) {
                return Either.right(ingredient);
            }

            return Either.left(ingredient);
        }).validate(ingredient -> {
            if (ingredient.isEmpty()) {
                return DataResult.error(() -> "Cannot serialize empty gas ingredient using the map codec");
            }
            return DataResult.success(ingredient);
        });
    }

    private static Codec<GasIngredient> codec(boolean allowEmpty) {
        var listCodec = Codec.lazyInitialized(() -> allowEmpty ? LIST_CODEC : LIST_CODEC_NON_EMPTY);
        return Codec.either(listCodec, MAP_CODEC_CODEC).xmap(either -> either.map(CompoundGasIngredient::of, i -> i), ingredient -> {
            if (ingredient instanceof CompoundGasIngredient compound) {
                return Either.left(compound.children());
            }
            else if (ingredient.isEmpty()) {
                return Either.left(List.of());
            }

            return Either.right(ingredient);
        });
    }

    public static GasIngredient empty() {
        return EmptyGasIngredient.INSTANCE;
    }

    public static GasIngredient of() {
        return empty();
    }

    public static @NotNull GasIngredient of(GasStack... gases) {
        return of(Arrays.stream(gases).map(GasStack::getGasType));
    }

    public static @NotNull GasIngredient of(Gas... gases) {
        return of(Arrays.stream(gases));
    }

    private static @NotNull GasIngredient of(@NotNull Stream<Gas> gases) {
        return CompoundGasIngredient.of(gases.map(GasIngredient::single));
    }

    @Contract("_ -> new")
    public static @NotNull GasIngredient single(@NotNull GasStack stack) {
        return single(stack.getGasType());
    }

    @Contract("_ -> new")
    public static @NotNull GasIngredient single(@NotNull Gas gasType) {
        return single(gasType.getHolder());
    }

    @Contract("_ -> new")
    public static @NotNull GasIngredient single(Holder<Gas> gasHolder) {
        return new SingleGasIngredient(gasHolder);
    }

    @Contract("_ -> new")
    public static @NotNull GasIngredient tag(TagKey<Gas> tag) {
        return new TagGasIngredient(tag);
    }

    @Nullable
    private GasStack[] stacks;

    protected abstract Stream<GasStack> generateStacks();

    public abstract boolean isSimple();

    public abstract GasIngredientType<?> getType();

    public final GasStack[] getStacks() {
        if (stacks == null) {
            stacks = generateStacks().collect(Collectors.toCollection(GasStackLinkedSet::createTypeAndComponentsSet)).toArray(GasStack[]::new);
        }
        return stacks;
    }

    public final boolean isEmpty() {
        return this == empty();
    }

    public final boolean hasNoGases() {
        return getStacks().length == 0;
    }

    @Override
    public abstract boolean test(GasStack gasStack);

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);
}
