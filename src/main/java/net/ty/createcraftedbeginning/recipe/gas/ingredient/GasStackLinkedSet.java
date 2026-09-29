package net.ty.createcraftedbeginning.recipe.gas.ingredient;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasStackLinkedSet {
    public static final Strategy<? super GasStack> TYPE = new Strategy<>() {
        @Override
        public int hashCode(@Nullable GasStack stack) {
            if (stack == null || stack.isEmpty()) {
                return 0;
            }

            return stack.getGasHolder().hashCode();
        }

        @Override
        public boolean equals(@Nullable GasStack first, @Nullable GasStack second) {
            if (first == second) {
                return true;
            }

            if (first == null || second == null) {
                return false;
            }

            if (first.isEmpty() || second.isEmpty()) {
                return first.isEmpty() && second.isEmpty();
            }

            return GasStack.isSameGas(first, second);
        }
    };

    public static final Strategy<? super GasStack> TYPE_AND_COMPONENTS = new Strategy<>() {
        @Override
        public int hashCode(@Nullable GasStack stack) {
            if (stack == null || stack.isEmpty()) {
                return 0;
            }

            return 31 * stack.getGasHolder().hashCode() + stack.getComponents().hashCode();
        }

        @Override
        public boolean equals(@Nullable GasStack first, @Nullable GasStack second) {
            if (first == second) {
                return true;
            }

            if (first == null || second == null) {
                return false;
            }

            if (first.isEmpty() || second.isEmpty()) {
                return first.isEmpty() && second.isEmpty();
            }

            return GasStack.isSameGasSameComponents(first, second);
        }
    };

    @Contract(value = " -> new", pure = true)
    public static Set<GasStack> createTypeSet() {
        return new ObjectLinkedOpenCustomHashSet<>(TYPE);
    }

    @Contract(value = " -> new", pure = true)
    public static Set<GasStack> createTypeAndComponentsSet() {
        return new ObjectLinkedOpenCustomHashSet<>(TYPE_AND_COMPONENTS);
    }
}
