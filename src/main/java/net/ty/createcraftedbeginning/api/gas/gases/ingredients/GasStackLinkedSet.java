package net.ty.createcraftedbeginning.api.gas.gases.ingredients;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasStackLinkedSet {
    public static final Strategy<? super GasStack> TYPE_AND_COMPONENTS = new Strategy<>() {
        @Override
        public int hashCode(@Nullable GasStack stack) {
            return GasStack.hashGasAndComponents(stack);
        }

        @Override
        public boolean equals(@Nullable GasStack first, @Nullable GasStack second) {
            return first == second || first != null && second != null && first.isEmpty() == second.isEmpty() && GasStack.isSameGasSameComponents(first, second);
        }
    };

    @Contract(value = " -> new", pure = true)
    public static Set<GasStack> createTypeAndComponentsSet() {
        return new ObjectLinkedOpenCustomHashSet<>(TYPE_AND_COMPONENTS);
    }
}
