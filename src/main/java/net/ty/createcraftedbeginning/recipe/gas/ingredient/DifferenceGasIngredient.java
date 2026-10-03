package net.ty.createcraftedbeginning.recipe.gas.ingredient;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DifferenceGasIngredient extends GasIngredient {
    public static final MapCodec<DifferenceGasIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(CODEC_NON_EMPTY.fieldOf("base").forGetter(DifferenceGasIngredient::base), CODEC_NON_EMPTY.fieldOf("subtracted").forGetter(DifferenceGasIngredient::subtracted)).apply(instance, DifferenceGasIngredient::new));
    private final GasIngredient base;
    private final GasIngredient subtracted;

    public DifferenceGasIngredient(GasIngredient base, GasIngredient subtracted) {
        this.base = base;
        this.subtracted = subtracted;
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static GasIngredient of(GasIngredient base, GasIngredient subtracted) {
        return new DifferenceGasIngredient(base, subtracted);
    }

    @Override
    public boolean test(GasStack stack) {
        return base.test(stack) && !subtracted.test(stack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, subtracted);
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof DifferenceGasIngredient other && base.equals(other.base()) && subtracted.equals(other.subtracted());
    }

    @Override
    public boolean isSimple() {
        return base.isSimple() && subtracted.isSimple();
    }

    @Override
    public GasIngredientType<?> getType() {
        return GasIngredientTypes.DIFFERENCE.get();
    }

    @Override
    public Stream<GasStack> generateStacks() {
        return base.generateStacks().filter(subtracted.negate());
    }

    public GasIngredient base() {
        return base;
    }

    public GasIngredient subtracted() {
        return subtracted;
    }
}
