package net.ty.createcraftedbeginning.api.gas.gases;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.Stream;

public class DifferenceGasIngredient extends GasIngredient {
    public static final MapCodec<DifferenceGasIngredient> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(CODEC_NON_EMPTY.fieldOf("base").forGetter(DifferenceGasIngredient::base), CODEC_NON_EMPTY.fieldOf("subtracted").forGetter(DifferenceGasIngredient::subtracted)).apply(builder, DifferenceGasIngredient::new));
    private final GasIngredient base;
    private final GasIngredient subtracted;

    public DifferenceGasIngredient(GasIngredient base, GasIngredient subtracted) {
        this.base = base;
        this.subtracted = subtracted;
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull GasIngredient of(GasIngredient base, GasIngredient subtracted) {
        return new DifferenceGasIngredient(base, subtracted);
    }

    @Override
    public Stream<GasStack> generateStacks() {
        return base.generateStacks().filter(subtracted.negate());
    }

    @Override
    public boolean isSimple() {
        return base.isSimple() && subtracted.isSimple();
    }

    @Override
    public GasIngredientType<?> getType() {
        return CCBGasRegistries.DIFFERENCE_GAS_INGREDIENT_TYPE.get();
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
    public boolean equals(Object obj) {
        return this == obj || obj instanceof DifferenceGasIngredient other && other.base() == base && other.subtracted() == subtracted;
    }

    public GasIngredient base() {
        return base;
    }

    public GasIngredient subtracted() {
        return subtracted;
    }
}
