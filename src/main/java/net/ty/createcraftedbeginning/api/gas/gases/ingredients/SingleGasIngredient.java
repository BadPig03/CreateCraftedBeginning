package net.ty.createcraftedbeginning.api.gas.gases.ingredients;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasRegistries;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SingleGasIngredient extends GasIngredient {
    public static final MapCodec<SingleGasIngredient> CODEC = GasStack.GAS_NON_EMPTY_CODEC.xmap(SingleGasIngredient::new, SingleGasIngredient::gas).fieldOf("gas");

    private final Holder<Gas> gas;

    /**
     * Creates a new {@code SingleGasIngredient} instance.
     *
     * @param gas the gas to inspect or process
     */
    public SingleGasIngredient(Holder<Gas> gas) {
        if (gas.value().isEmpty()) {
            throw new IllegalStateException("SingleGasIngredient must not be constructed with minecraft:empty, use GasIngredient.empty() instead!");
        }

        this.gas = gas;
    }

    @Override
    protected Stream<GasStack> generateStacks() {
        return Stream.of(new GasStack(gas, FluidType.BUCKET_VOLUME));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSimple() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasIngredientType<?> getType() {
        return GasRegistries.SINGLE_GAS_INGREDIENT_TYPE.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean test(GasStack gasStack) {
        return gasStack.is(gas);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return gas().value().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof SingleGasIngredient other && gas.equals(other.gas().value());
    }

    /**
     * Sets the gas stack used by this builder.
     *
     * @return the resulting holder
     */
    public Holder<Gas> gas() {
        return gas;
    }
}
