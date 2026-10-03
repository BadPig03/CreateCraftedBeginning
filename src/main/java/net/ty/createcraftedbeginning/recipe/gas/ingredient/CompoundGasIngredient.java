package net.ty.createcraftedbeginning.recipe.gas.ingredient;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CompoundGasIngredient extends GasIngredient {
    public static final MapCodec<CompoundGasIngredient> CODEC = NeoForgeExtraCodecs.aliasedFieldOf(LIST_CODEC_NON_EMPTY, "children", "ingredients").xmap(CompoundGasIngredient::new, CompoundGasIngredient::children);

    private final List<GasIngredient> children;

    public CompoundGasIngredient(List<? extends GasIngredient> children) {
        if (children.isEmpty()) {
            throw new IllegalArgumentException("Compound gas ingredient must have at least one child.");
        }

        this.children = List.copyOf(children);
    }

    public static GasIngredient of(GasIngredient @NotNull ... children) {
        if (children.length == 0) {
            return empty();
        }

        if (children.length == 1) {
            return children[0];
        }

        return new CompoundGasIngredient(List.of(children));
    }

    public static GasIngredient of(List<GasIngredient> children) {
        if (children.isEmpty()) {
            return empty();
        }

        if (children.size() == 1) {
            return children.getFirst();
        }

        return new CompoundGasIngredient(children);
    }

    public static GasIngredient of(Stream<GasIngredient> stream) {
        return of(stream.toList());
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
    public boolean equals(Object object) {
        return this == object || object instanceof CompoundGasIngredient other && children.equals(other.children());
    }

    @Override
    public boolean isSimple() {
        return children.stream().allMatch(GasIngredient::isSimple);
    }

    @Override
    public GasIngredientType<?> getType() {
        return GasIngredientTypes.COMPOUND.get();
    }

    @Override
    public Stream<GasStack> generateStacks() {
        return children.stream().flatMap(GasIngredient::generateStacks);
    }

    public List<GasIngredient> children() {
        return children;
    }
}
