package net.ty.createcraftedbeginning.api.gas.gases.ingredients;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EmptyGasIngredient extends GasIngredient {
    public static final EmptyGasIngredient INSTANCE = new EmptyGasIngredient();
    public static final MapCodec<EmptyGasIngredient> CODEC = MapCodec.unit(INSTANCE);

    private EmptyGasIngredient() {
    }

    @Override
    protected Stream<GasStack> generateStacks() {
        return Stream.empty();
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public GasIngredientType<?> getType() {
        return CCBGasRegistries.EMPTY_GAS_INGREDIENT_TYPE.get();
    }

    @Override
    public boolean test(GasStack gasStack) {
        return gasStack.isEmpty();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
