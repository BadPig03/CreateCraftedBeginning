package net.ty.createcraftedbeginning.recipe.gas.ingredient;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class GasIngredient implements Predicate<GasStack> {
    public static final StreamCodec<RegistryFriendlyByteBuf, GasIngredient> STREAM_CODEC = new StreamCodec<>() {
        private static final StreamCodec<RegistryFriendlyByteBuf, GasIngredient> DISPATCH_CODEC = ByteBufCodecs.registry(GasIngredientTypes.REGISTRY_KEY).dispatch(GasIngredient::getType, GasIngredientType::streamCodec);
        private static final StreamCodec<RegistryFriendlyByteBuf, List<GasStack>> GAS_LIST_CODEC = GasStack.STREAM_CODEC.apply(ByteBufCodecs.collection(NonNullList::createWithCapacity));

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, GasIngredient ingredient) {
            if (ingredient.isSimple()) {
                GAS_LIST_CODEC.encode(buffer, Arrays.asList(ingredient.getStacks()));
                return;
            }

            buffer.writeVarInt(-1);
            DISPATCH_CODEC.encode(buffer, ingredient);
        }

        @Override
        public GasIngredient decode(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            if (size == -1) {
                return DISPATCH_CODEC.decode(buffer);
            }

            return CompoundGasIngredient.of(Stream.generate(() -> GasStack.STREAM_CODEC.decode(buffer)).limit(size).map(GasIngredient::single));
        }
    };

    public static final MapCodec<GasIngredient> SINGLE_OR_TAG_CODEC = MapCodec.recursive("GasIngredient.SINGLE_OR_TAG_CODEC", self -> singleOrTagCodec());
    public static final MapCodec<GasIngredient> MAP_CODEC_NONEMPTY = makeMapCodec();
    public static final Codec<GasIngredient> MAP_CODEC_CODEC = MAP_CODEC_NONEMPTY.codec();
    public static final Codec<List<GasIngredient>> LIST_CODEC = MAP_CODEC_CODEC.listOf();
    public static final Codec<List<GasIngredient>> LIST_CODEC_NON_EMPTY = LIST_CODEC.validate(list -> {
        if (list.isEmpty()) {
            return DataResult.error(() -> "Gas ingredient list must contain at least one ingredient.");
        }

        return DataResult.success(list);
    });
    public static final Codec<GasIngredient> CODEC = codec(true);
    public static final Codec<GasIngredient> CODEC_NON_EMPTY = codec(false);
    @Nullable
    private GasStack[] stacks;

    public static GasIngredient empty() {
        return EmptyGasIngredient.INSTANCE;
    }

    public static GasIngredient of() {
        return empty();
    }

    public static GasIngredient of(GasStack... gases) {
        return of(Arrays.stream(gases).map(GasStack::getGasType));
    }

    public static GasIngredient of(Gas... gases) {
        return of(Arrays.stream(gases));
    }

    @Contract("_ -> new")
    public static GasIngredient single(GasStack stack) {
        return single(stack.getGasType());
    }

    @Contract("_ -> new")
    public static GasIngredient single(Gas gasType) {
        return single(gasType.getHolder());
    }

    @Contract("_ -> new")
    public static GasIngredient single(Holder<Gas> gasHolder) {
        return new SingleGasIngredient(gasHolder);
    }

    @Contract("_ -> new")
    public static GasIngredient tag(TagKey<Gas> tag) {
        return new TagGasIngredient(tag);
    }

    private static MapCodec<GasIngredient> singleOrTagCodec() {
        return NeoForgeExtraCodecs.xor(SingleGasIngredient.CODEC, TagGasIngredient.CODEC).xmap(either -> either.map(value -> value, value -> value), ingredient -> switch (ingredient) {
            case SingleGasIngredient gas -> Either.left(gas);
            case TagGasIngredient tag -> Either.right(tag);
            default -> throw new IllegalStateException("Basic gas ingredient should be either a gas or a tag.");
        });
    }

    private static MapCodec<GasIngredient> makeMapCodec() {
        return NeoForgeExtraCodecs.<GasIngredientType<?>, GasIngredient, GasIngredient>dispatchMapOrElse(GasIngredientTypes.REGISTRY.byNameCodec(), GasIngredient::getType, GasIngredientType::codec, SINGLE_OR_TAG_CODEC).xmap(either -> either.map(value -> value, value -> value), ingredient -> {
            if (ingredient instanceof SingleGasIngredient || ingredient instanceof TagGasIngredient) {
                return Either.right(ingredient);
            }

            return Either.left(ingredient);
        }).validate(ingredient -> {
            if (ingredient.isEmpty()) {
                return DataResult.error(() -> "Cannot serialize empty gas ingredient using the map codec.");
            }

            return DataResult.success(ingredient);
        });
    }

    private static Codec<GasIngredient> codec(boolean allowEmpty) {
        Codec<List<GasIngredient>> listCodec = Codec.lazyInitialized(() -> allowEmpty ? LIST_CODEC : LIST_CODEC_NON_EMPTY);
        return Codec.either(listCodec, MAP_CODEC_CODEC).xmap(either -> either.map(CompoundGasIngredient::of, value -> value), ingredient -> {
            if (ingredient instanceof CompoundGasIngredient compound) {
                return Either.left(compound.children());
            }

            if (ingredient.isEmpty()) {
                return Either.left(List.of());
            }

            return Either.right(ingredient);
        });
    }

    private static GasIngredient of(Stream<Gas> gases) {
        return CompoundGasIngredient.of(gases.map(GasIngredient::single));
    }

    @Override
    public abstract boolean test(GasStack gasStack);

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object object);

    public abstract boolean isSimple();

    public abstract GasIngredientType<?> getType();

    public final GasStack[] getStacks() {
        if (stacks != null) {
            return stacks;
        }

        stacks = generateStacks().collect(Collectors.toCollection(GasStackLinkedSet::createTypeAndComponentsSet)).toArray(GasStack[]::new);
        return stacks;
    }

    public final boolean isEmpty() {
        return this == empty();
    }

    protected abstract Stream<GasStack> generateStacks();
}
