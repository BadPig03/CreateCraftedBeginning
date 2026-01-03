package net.ty.createcraftedbeginning.api.gas.gases;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class IntersectionGasIngredient extends GasIngredient {
    public static final MapCodec<IntersectionGasIngredient> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(LIST_CODEC_NON_EMPTY.fieldOf("children").forGetter(IntersectionGasIngredient::children)).apply(builder, IntersectionGasIngredient::new));
    private final List<GasIngredient> children;

    public IntersectionGasIngredient(@NotNull List<GasIngredient> children) {
        if (children.isEmpty()) {
            throw new IllegalArgumentException("Cannot create an IntersectionGasIngredient with no children, use GasIngredient.of() to create an empty ingredient");
        }

        this.children = children;
    }

    public static GasIngredient of(GasIngredient @NotNull ... ingredients) {
        if (ingredients.length == 0) {
            throw new IllegalArgumentException("Cannot create an IntersectionGasIngredient with no children, use GasIngredient.of() to create an empty ingredient");
        }
        else if (ingredients.length == 1) {
            return ingredients[0];
        }
        else {
            return new IntersectionGasIngredient(Arrays.asList(ingredients));
        }
    }

    @Override
    public Stream<GasStack> generateStacks() {
        return children.stream().flatMap(GasIngredient::generateStacks).filter(this);
    }

    @Override
    public boolean isSimple() {
        return children.stream().allMatch(GasIngredient::isSimple);
    }

    @Override
    public GasIngredientType<?> getType() {
        return CCBGasRegistries.INTERSECTION_GAS_INGREDIENT_TYPE.get();
    }

    @Override
    public boolean test(GasStack stack) {
        return children.stream().allMatch(child -> child.test(stack));
    }

    @Override
    public int hashCode() {
        return Objects.hash(children);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof IntersectionGasIngredient other && other.children() == children;
    }

    public List<GasIngredient> children() {
        return children;
    }
}
