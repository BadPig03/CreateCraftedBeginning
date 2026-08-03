package net.ty.createcraftedbeginning.api.gas.gases.ingredients;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
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

    /**
     * Creates a new {@code CompoundGasIngredient} instance.
     *
     * @param children the children to inspect or process
     */
    public CompoundGasIngredient(List<? extends GasIngredient> children) {
        if (children.isEmpty()) {
            throw new IllegalArgumentException("Compound gas ingredient must have at least one child");
        }
        this.children = List.copyOf(children);
    }

    /**
     * Creates a gas ingredient from the supplied value.
     *
     * @param children the children to use
     * @return the created value
     */
    public static GasIngredient of(GasIngredient @NotNull ... children) {
        if (children.length == 0) {
            return empty();
        }

        if (children.length == 1) {
            return children[0];
        }
        return new CompoundGasIngredient(List.of(children));
    }

    /**
     * Creates a gas ingredient from the supplied value.
     *
     * @param children the children to inspect or process
     * @return the created value
     */
    public static GasIngredient of(List<GasIngredient> children) {
        if (children.isEmpty()) {
            return empty();
        }

        if (children.size() == 1) {
            return children.getFirst();
        }
        return new CompoundGasIngredient(children);
    }

    /**
     * Creates a gas ingredient from the supplied value.
     *
     * @param stream the stream to use
     * @return the created value
     */
    public static GasIngredient of(Stream<GasIngredient> stream) {
        return of(stream.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<GasStack> generateStacks() {
        return children.stream().flatMap(GasIngredient::generateStacks);
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
        return CCBGasRegistries.COMPOUND_GAS_INGREDIENT_TYPE.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean test(GasStack stack) {
        return children.stream().anyMatch(child -> child.test(stack));
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
        return this == obj || obj instanceof CompoundGasIngredient other && other.children() == children;
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
