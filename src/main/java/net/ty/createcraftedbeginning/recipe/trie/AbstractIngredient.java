package net.ty.createcraftedbeginning.recipe.trie;

import com.google.common.collect.ImmutableSet;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class AbstractIngredient {
    final Set<AbstractVariant> variants;
    private final int hashCode;

    AbstractIngredient(Set<AbstractVariant> variants) {
        this.variants = ImmutableSet.copyOf(variants);
        hashCode = variants.hashCode();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AbstractIngredient other && (this == other || hashCode == other.hashCode && variants.equals(other.variants));
    }

    static class Universal extends AbstractIngredient {
        static final Universal INSTANCE = new Universal();
        private static final int hashCode = Universal.class.hashCode();

        private Universal() {
            super(Set.of());
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Universal;
        }
    }
}
