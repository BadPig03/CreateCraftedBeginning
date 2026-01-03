package net.ty.createcraftedbeginning.api.gas.gases;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

public final class SizedGasIngredient {
    private static final Codec<SizedGasIngredient> GAS_STACK = RecordCodecBuilder.create(i -> i.group(validatedType("gas_stack"), GasStack.GAS_NON_EMPTY_CODEC.fieldOf("gas").forGetter(s -> null), DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(s -> null), Codec.LONG.fieldOf("amount").forGetter(s -> null)).apply(i, (type, gas, components, amount) -> new SizedGasIngredient(DataComponentGasIngredient.of(false, components.split().added(), gas), amount)));
    private static final Codec<SizedGasIngredient> GAS_TAG = RecordCodecBuilder.create(i -> i.group(validatedType("gas_tag"), TagKey.codec(CCBRegistries.GAS_REGISTRY_KEY).fieldOf("gas_tag").forGetter(s -> null), Codec.LONG.fieldOf("amount").forGetter(s -> null)).apply(i, (type, tag, amount) -> new SizedGasIngredient(TagGasIngredient.tag(tag), amount)));

    public static final Codec<SizedGasIngredient> FLAT_CODEC = RecordCodecBuilder.create(instance -> instance.group(GasIngredient.MAP_CODEC_NONEMPTY.forGetter(SizedGasIngredient::ingredient), NeoForgeExtraCodecs.optionalFieldAlwaysWrite(Codec.LONG, "amount", (long) FluidType.BUCKET_VOLUME).forGetter(SizedGasIngredient::amount)).apply(instance, SizedGasIngredient::new));
    public static final Codec<SizedGasIngredient> NESTED_CODEC = RecordCodecBuilder.create(instance -> instance.group(GasIngredient.CODEC_NON_EMPTY.fieldOf("ingredient").forGetter(SizedGasIngredient::ingredient), NeoForgeExtraCodecs.optionalFieldAlwaysWrite(Codec.LONG, "amount", (long) FluidType.BUCKET_VOLUME).forGetter(SizedGasIngredient::amount)).apply(instance, SizedGasIngredient::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SizedGasIngredient> STREAM_CODEC = StreamCodec.composite(GasIngredient.STREAM_CODEC, SizedGasIngredient::ingredient, ByteBufCodecs.VAR_LONG, SizedGasIngredient::amount, SizedGasIngredient::new);
    public static final Codec<SizedGasIngredient> FLAT_SIZED_GAS_INGREDIENT_WITH_TYPE = RecordCodecBuilder.create(instance -> instance.group(CCBGasRegistries.GAS_INGREDIENT_TYPES_REGISTRY.byNameCodec().fieldOf("type").forGetter(i -> i.ingredient().getType()), GasIngredient.MAP_CODEC_NONEMPTY.forGetter(SizedGasIngredient::ingredient), NeoForgeExtraCodecs.optionalFieldAlwaysWrite(Codec.LONG, "amount", (long) FluidType.BUCKET_VOLUME).forGetter(SizedGasIngredient::amount)).apply(instance, (type, ingredient, amount) -> new SizedGasIngredient(ingredient, amount)));
    public static final Codec<SizedGasIngredient> SIZED_GAS_INGREDIENT = Codec.withAlternative(FLAT_SIZED_GAS_INGREDIENT_WITH_TYPE, Codec.withAlternative(GAS_STACK, GAS_TAG));

    private final GasIngredient ingredient;
    private final long amount;
    @Nullable
    private GasStack[] cachedStacks;
    public SizedGasIngredient(GasIngredient ingredient, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }

        this.ingredient = ingredient;
        this.amount = amount;
    }

    @Contract("_ -> new")
    private static <T> @NotNull RecordCodecBuilder<T, String> validatedType(String requiredType) {
        return Codec.STRING.validate(s -> s.equals(requiredType) ? DataResult.success(s) : DataResult.error(() -> "Invalid Type: " + s)).fieldOf("type").forGetter(s -> requiredType);
    }

    @Contract("_, _ -> new")
    public static @NotNull SizedGasIngredient of(Gas gas, long amount) {
        return new SizedGasIngredient(GasIngredient.of(gas), amount);
    }

    @Contract("_ -> new")
    public static @NotNull SizedGasIngredient of(GasStack stack) {
        return new SizedGasIngredient(GasIngredient.single(stack), stack.getAmount());
    }

    @Contract("_, _ -> new")
    public static @NotNull SizedGasIngredient of(TagKey<Gas> tag, long amount) {
        return new SizedGasIngredient(GasIngredient.tag(tag), amount);
    }

    public GasIngredient ingredient() {
        return ingredient;
    }

    public long amount() {
        return amount;
    }

    public boolean test(GasStack stack) {
        return ingredient.test(stack) && stack.getAmount() >= amount;
    }

    public GasStack[] getGases() {
        if (cachedStacks == null) {
            cachedStacks = Stream.of(ingredient.getStacks()).map(s -> s.copyWithAmount(amount)).toArray(GasStack[]::new);
        }
        return cachedStacks;
    }

    public GasStack getFirstGas() {
        return getGases()[0];
    }

    @Override
    public int hashCode() {
        return Objects.hash(ingredient, amount);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof SizedGasIngredient other && other.amount() == amount && other.ingredient() == ingredient;
    }

    @Override
    public String toString() {
        return amount + "x " + ingredient;
    }
}
