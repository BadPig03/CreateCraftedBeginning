package net.ty.createcraftedbeginning.api.gas.gases.ingredients;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class IntersectionGasIngredient extends GasIngredient {
    public static final MapCodec<IntersectionGasIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(LIST_CODEC_NON_EMPTY.fieldOf("children").forGetter(IntersectionGasIngredient::children)).apply(instance, IntersectionGasIngredient::new));
    private final List<GasIngredient> children;

    /**
     * Creates a new {@code IntersectionGasIngredient} instance.
     *
     * @param children the children to inspect or process
     */
    public IntersectionGasIngredient(List<GasIngredient> children) {
        if (children.isEmpty()) {
            throw new IllegalArgumentException("Cannot create an IntersectionGasIngredient with no children, use GasIngredient.of() to create an empty ingredient");
        }

        this.children = children;
    }

    /**
     * Creates a gas ingredient from the supplied value.
     *
     * @param ingredients the ingredients to add or inspect
     * @return the created value
     */
    public static GasIngredient of(GasIngredient @NotNull ... ingredients) {
        if (ingredients.length == 0) {
            throw new IllegalArgumentException("Cannot create an IntersectionGasIngredient with no children, use GasIngredient.of() to create an empty ingredient");
        }
        if (ingredients.length == 1) {
            return ingredients[0];
        }
        return new IntersectionGasIngredient(Arrays.asList(ingredients));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<GasStack> generateStacks() {
        return children.stream().flatMap(GasIngredient::generateStacks).filter(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSimple() {
        return children.stream().allMatch(GasIngredient::isSimple);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasIngredientType<?> getType() {
        return CCBGasRegistries.INTERSECTION_GAS_INGREDIENT_TYPE.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean test(GasStack stack) {
        return children.stream().allMatch(child -> child.test(stack));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(children);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof IntersectionGasIngredient other && other.children() == children;
    }

    /**
     * Returns the child ingredients that compose this ingredient.
     *
     * @return the resulting values
     */
    public List<GasIngredient> children() {
        return children;
    }
}
