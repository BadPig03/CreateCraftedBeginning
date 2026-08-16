package net.ty.createcraftedbeginning.api.gas.gases.ingredients;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasRegistries;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SizedGasIngredient {
    public static final StreamCodec<RegistryFriendlyByteBuf, SizedGasIngredient> STREAM_CODEC = StreamCodec.composite(GasIngredient.STREAM_CODEC, SizedGasIngredient::ingredient, ByteBufCodecs.VAR_LONG, SizedGasIngredient::amount, SizedGasIngredient::new);
    public static final Codec<SizedGasIngredient> TYPED_CODEC = RecordCodecBuilder.create(instance -> instance.group(GasRegistries.GAS_INGREDIENT_TYPES_REGISTRY.byNameCodec().fieldOf("type").forGetter(value -> value.ingredient().getType()), GasIngredient.MAP_CODEC_NONEMPTY.forGetter(SizedGasIngredient::ingredient), NeoForgeExtraCodecs.optionalFieldAlwaysWrite(Codec.LONG, "amount", (long) FluidType.BUCKET_VOLUME).forGetter(SizedGasIngredient::amount)).apply(instance, (type, ingredient, amount) -> new SizedGasIngredient(ingredient, amount)));

    private static final Codec<SizedGasIngredient> GAS_STACK_CODEC = RecordCodecBuilder.create(instance -> instance.group(validatedType("gas_stack"), GasStack.GAS_NON_EMPTY_CODEC.fieldOf("gas").forGetter(value -> null), DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(value -> null), Codec.LONG.fieldOf("amount").forGetter(value -> null)).apply(instance, (type, gas, components, amount) -> new SizedGasIngredient(DataComponentGasIngredient.of(false, components.split().added(), gas), amount)));
    private static final Codec<SizedGasIngredient> GAS_TAG_CODEC = RecordCodecBuilder.create(instance -> instance.group(validatedType("gas_tag"), TagKey.codec(GasRegistries.GAS_REGISTRY_KEY).fieldOf("gas_tag").forGetter(value -> null), Codec.LONG.fieldOf("amount").forGetter(value -> null)).apply(instance, (type, tag, amount) -> new SizedGasIngredient(TagGasIngredient.tag(tag), amount)));

    public static final Codec<SizedGasIngredient> CODEC = Codec.withAlternative(TYPED_CODEC, Codec.withAlternative(GAS_STACK_CODEC, GAS_TAG_CODEC));

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

    private static <T> @NotNull RecordCodecBuilder<T, String> validatedType(String requiredType) {
        return Codec.STRING.validate(type -> type.equals(requiredType) ? DataResult.success(type) : DataResult.error(() -> "Invalid Type: " + type)).fieldOf("type").forGetter(value -> requiredType);
    }

    @Contract("_, _ -> new")
    public static SizedGasIngredient of(Gas gasType, long amount) {
        return new SizedGasIngredient(GasIngredient.of(gasType), amount);
    }

    @Contract("_ -> new")
    public static SizedGasIngredient of(GasStack stack) {
        return new SizedGasIngredient(GasIngredient.single(stack), stack.getAmount());
    }

    @Contract("_, _ -> new")
    public static SizedGasIngredient of(TagKey<Gas> tag, long amount) {
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

    public boolean test(Gas gasType) {
        return ingredient.test(new GasStack(gasType, amount));
    }

    public GasStack[] getGases() {
        if (cachedStacks != null) {
            return cachedStacks;
        }

        cachedStacks = Stream.of(ingredient.getStacks()).map(stack -> stack.copyWithAmount(amount)).toArray(GasStack[]::new);
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
        return this == obj || obj instanceof SizedGasIngredient other && other.amount() == amount && ingredient.equals(other.ingredient());
    }

    @Override
    public String toString() {
        return amount + "x " + ingredient;
    }
}
