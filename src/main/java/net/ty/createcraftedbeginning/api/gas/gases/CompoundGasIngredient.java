package net.ty.createcraftedbeginning.api.gas.gases;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class CompoundGasIngredient extends GasIngredient {
    public static final MapCodec<CompoundGasIngredient> CODEC = NeoForgeExtraCodecs.aliasedFieldOf(LIST_CODEC_NON_EMPTY, "children", "ingredients").xmap(CompoundGasIngredient::new, CompoundGasIngredient::children);

    private final List<GasIngredient> children;

    public CompoundGasIngredient(@NotNull List<? extends GasIngredient> children) {
        if (children.isEmpty()) {
            throw new IllegalArgumentException("Compound gas ingredient must have at least one child");
        }
        this.children = List.copyOf(children);
    }

    public static GasIngredient of(GasIngredient @NotNull ... children) {
        if (children.length == 0) {
            return empty();
        }
        else if (children.length == 1) {
            return children[0];
        }
        else {
            return new CompoundGasIngredient(List.of(children));
        }
    }

    public static GasIngredient of(@NotNull List<GasIngredient> children) {
        if (children.isEmpty()) {
            return empty();
        }
        else if (children.size() == 1) {
            return children.getFirst();
        }
        else {
            return new CompoundGasIngredient(children);
        }
    }

    public static GasIngredient of(@NotNull Stream<GasIngredient> stream) {
        return of(stream.toList());
    }

    @Override
    public Stream<GasStack> generateStacks() {
        return children.stream().flatMap(GasIngredient::generateStacks);
    }

    @Override
    public boolean isSimple() {
        return children.stream().allMatch(GasIngredient::isSimple);
    }

    @Override
    public @NotNull GasIngredientType<?> getType() {
        return CCBGasRegistries.COMPOUND_GAS_INGREDIENT_TYPE.get();
    }

    @Override
    public boolean test(GasStack stack) {
        return children.stream().anyMatch(child -> child.test(stack));
    }

    @Override
    public int hashCode() {
        return Objects.hash(children);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof CompoundGasIngredient other && other.children() == children;
    }

    public List<GasIngredient> children() {
        return children;
    }
}
