package net.ty.createcraftedbeginning.recipe.trie;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class AbstractIngredient {
    final Set<AbstractVariant> variants;
    final int hashCode;

    public AbstractIngredient(Set<AbstractVariant> variants) {
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

    public static class Universal extends AbstractIngredient {
        public static final Universal INSTANCE = new Universal();
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
