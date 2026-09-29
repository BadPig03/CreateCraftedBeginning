package net.ty.createcraftedbeginning.recipe.gas.ingredient;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SingleGasIngredient extends GasIngredient {
    public static final MapCodec<SingleGasIngredient> CODEC = GasStack.GAS_NON_EMPTY_CODEC.xmap(SingleGasIngredient::new, SingleGasIngredient::gas).fieldOf("gas");

    private final Holder<Gas> gas;

    public SingleGasIngredient(Holder<Gas> gas) {
        if (gas.value().isEmpty()) {
            throw new IllegalStateException("SingleGasIngredient requires a non-empty gas; use 'GasIngredient.empty()' for an empty ingredient.");
        }

        this.gas = gas;
    }

    @Override
    public boolean test(GasStack gasStack) {
        return gasStack.is(gas);
    }

    @Override
    public int hashCode() {
        return gas().value().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof SingleGasIngredient other && gas.equals(other.gas().value());
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public GasIngredientType<?> getType() {
        return GasIngredientTypes.SINGLE.get();
    }

    @Override
    protected Stream<GasStack> generateStacks() {
        return Stream.of(new GasStack(gas, GasUnits.GU_PER_KGU));
    }

    public Holder<Gas> gas() {
        return gas;
    }
}
