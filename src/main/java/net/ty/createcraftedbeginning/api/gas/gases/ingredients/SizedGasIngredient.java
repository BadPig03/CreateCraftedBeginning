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

    /**
     * Creates a new {@code SizedGasIngredient} instance.
     *
     * @param ingredient the ingredient to add or inspect
     * @param amount     the amount to use
     */
    public SizedGasIngredient(GasIngredient ingredient, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }

        this.ingredient = ingredient;
        this.amount = amount;
    }

    @Contract("_ -> new")
    private static <T> @NotNull RecordCodecBuilder<T, String> validatedType(String requiredType) {
        return Codec.STRING.validate(type -> type.equals(requiredType) ? DataResult.success(type) : DataResult.error(() -> "Invalid Type: " + type)).fieldOf("type").forGetter(value -> requiredType);
    }

    /**
     * Creates a sized gas ingredient from the supplied values.
     *
     * @param gasType the gas type to inspect or process
     * @param amount  the amount to use
     * @return the created value
     */
    @Contract("_, _ -> new")
    public static SizedGasIngredient of(Gas gasType, long amount) {
        return new SizedGasIngredient(GasIngredient.of(gasType), amount);
    }

    /**
     * Creates a sized gas ingredient from the supplied value.
     *
     * @param stack the stack to inspect or process
     * @return the created value
     */
    @Contract("_ -> new")
    public static SizedGasIngredient of(GasStack stack) {
        return new SizedGasIngredient(GasIngredient.single(stack), stack.getAmount());
    }

    /**
     * Creates a sized gas ingredient from the supplied values.
     *
     * @param tag    the tag to inspect or process
     * @param amount the amount to use
     * @return the created value
     */
    @Contract("_, _ -> new")
    public static SizedGasIngredient of(TagKey<Gas> tag, long amount) {
        return new SizedGasIngredient(GasIngredient.tag(tag), amount);
    }

    /**
     * Adds the supplied ingredient to this builder.
     *
     * @return the resulting gas ingredient
     */
    public GasIngredient ingredient() {
        return ingredient;
    }

    /**
     * Sets the amount used by this builder.
     *
     * @return the amount value
     */
    public long amount() {
        return amount;
    }

    /**
     * Checks whether the supplied value matches this condition.
     *
     * @param stack the stack to inspect or process
     * @return {@code true} if the supplied value matches this condition; otherwise {@code false}
     */
    public boolean test(GasStack stack) {
        return ingredient.test(stack) && stack.getAmount() >= amount;
    }

    /**
     * Checks whether the supplied value matches this condition.
     *
     * @param gasType the gas type to inspect or process
     * @return {@code true} if the supplied value matches this condition; otherwise {@code false}
     */
    public boolean test(Gas gasType) {
        return ingredient.test(new GasStack(gasType, amount));
    }

    /**
     * Returns the gases.
     *
     * @return the gases
     */
    public GasStack[] getGases() {
        if (cachedStacks != null) {
            return cachedStacks;
        }

        cachedStacks = Stream.of(ingredient.getStacks()).map(stack -> stack.copyWithAmount(amount)).toArray(GasStack[]::new);
        return cachedStacks;
    }

    /**
     * Returns the first available gas.
     *
     * @return the first available gas
     */
    public GasStack getFirstGas() {
        return getGases()[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(ingredient, amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof SizedGasIngredient other && other.amount() == amount && ingredient.equals(other.ingredient());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return amount + "x " + ingredient;
    }
}
