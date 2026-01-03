package net.ty.createcraftedbeginning.api.gas.gases;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class SingleGasIngredient extends GasIngredient {
    public static final MapCodec<SingleGasIngredient> CODEC = GasStack.GAS_NON_EMPTY_CODEC.xmap(SingleGasIngredient::new, SingleGasIngredient::gas).fieldOf("gas");

    private final Holder<Gas> gas;

    public SingleGasIngredient(@NotNull Holder<Gas> gas) {
        if (gas.value().isEmpty()) {
            throw new IllegalStateException("SingleGasIngredient must not be constructed with minecraft:empty, use GasIngredient.empty() instead!");
        }

        this.gas = gas;
    }

    @Override
    public boolean test(@NotNull GasStack gasStack) {
        return gasStack.is(gas);
    }

    @Override
    protected Stream<GasStack> generateStacks() {
        return Stream.of(new GasStack(gas, FluidType.BUCKET_VOLUME));
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public GasIngredientType<?> getType() {
        return CCBGasRegistries.SINGLE_GAS_INGREDIENT_TYPE.get();
    }

    @Override
    public int hashCode() {
        return gas().value().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof SingleGasIngredient other && other.gas().value() == gas;
    }

    public Holder<Gas> gas() {
        return gas;
    }
}
