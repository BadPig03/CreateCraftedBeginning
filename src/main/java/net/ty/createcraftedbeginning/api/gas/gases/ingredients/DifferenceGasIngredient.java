package net.ty.createcraftedbeginning.api.gas.gases.ingredients;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasRegistries;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DifferenceGasIngredient extends GasIngredient {
    public static final MapCodec<DifferenceGasIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(CODEC_NON_EMPTY.fieldOf("base").forGetter(DifferenceGasIngredient::base), CODEC_NON_EMPTY.fieldOf("subtracted").forGetter(DifferenceGasIngredient::subtracted)).apply(instance, DifferenceGasIngredient::new));
    private final GasIngredient base;
    private final GasIngredient subtracted;

    /**
     * Creates a new {@code DifferenceGasIngredient} instance.
     *
     * @param base       the base to use
     * @param subtracted the subtracted to use
     */
    public DifferenceGasIngredient(GasIngredient base, GasIngredient subtracted) {
        this.base = base;
        this.subtracted = subtracted;
    }

    /**
     * Creates a gas ingredient from the supplied values.
     *
     * @param base       the base to use
     * @param subtracted the subtracted to use
     * @return the created value
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static GasIngredient of(GasIngredient base, GasIngredient subtracted) {
        return new DifferenceGasIngredient(base, subtracted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<GasStack> generateStacks() {
        return base.generateStacks().filter(subtracted.negate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSimple() {
        return base.isSimple() && subtracted.isSimple();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasIngredientType<?> getType() {
        return GasRegistries.DIFFERENCE_GAS_INGREDIENT_TYPE.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean test(GasStack stack) {
        return base.test(stack) && !subtracted.test(stack);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(base, subtracted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof DifferenceGasIngredient other && base.equals(other.base()) && subtracted.equals(other.subtracted());
    }

    /**
     * Sets the base ingredient used by this compound ingredient.
     *
     * @return the resulting gas ingredient
     */
    public GasIngredient base() {
        return base;
    }

    /**
     * Creates a copy with the supplied amount removed.
     *
     * @return the resulting gas ingredient
     */
    public GasIngredient subtracted() {
        return subtracted;
    }
}
