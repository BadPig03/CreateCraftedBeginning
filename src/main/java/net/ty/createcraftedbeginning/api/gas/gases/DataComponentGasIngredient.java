package net.ty.createcraftedbeginning.api.gas.gases;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.HolderSetCodec;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class DataComponentGasIngredient extends GasIngredient {
    public static final MapCodec<DataComponentGasIngredient> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(HolderSetCodec.create(CCBRegistries.GAS_REGISTRY_KEY, CCBGasRegistries.GAS_REGISTRY.holderByNameCodec(), false).fieldOf("gases").forGetter(DataComponentGasIngredient::gases), DataComponentPredicate.CODEC.fieldOf("components").forGetter(DataComponentGasIngredient::components), Codec.BOOL.optionalFieldOf("strict", false).forGetter(DataComponentGasIngredient::isStrict)).apply(builder, DataComponentGasIngredient::new));

    private final HolderSet<Gas> gases;
    private final DataComponentPredicate components;
    private final boolean strict;
    private final GasStack[] stacks;

    public DataComponentGasIngredient(@NotNull HolderSet<Gas> gases, DataComponentPredicate components, boolean strict) {
        this.gases = gases;
        this.components = components;
        this.strict = strict;
        stacks = gases.stream().map(i -> new GasStack(i, FluidType.BUCKET_VOLUME, components.asPatch())).toArray(GasStack[]::new);
    }

    @Contract("_, _ -> new")
    public static @NotNull GasIngredient of(boolean strict, @NotNull GasStack stack) {
        return of(strict, stack.getComponents(), stack.getGasType());
    }

    @Contract("_, _, _, _ -> new")
    public static <T> @NotNull GasIngredient of(boolean strict, DataComponentType<? super T> type, T value, Gas... gases) {
        return of(strict, DataComponentPredicate.builder().expect(type, value).build(), gases);
    }

    @Contract("_, _, _, _ -> new")
    public static <T> @NotNull GasIngredient of(boolean strict, @NotNull Supplier<? extends DataComponentType<? super T>> type, T value, Gas... gases) {
        return of(strict, type.get(), value, gases);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull GasIngredient of(boolean strict, DataComponentMap map, Gas... gases) {
        return of(strict, DataComponentPredicate.allOf(map), gases);
    }

    @Contract("_, _, _ -> new")
    @SafeVarargs
    public static @NotNull GasIngredient of(boolean strict, DataComponentMap map, Holder<Gas>... gases) {
        return of(strict, DataComponentPredicate.allOf(map), gases);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull GasIngredient of(boolean strict, DataComponentMap map, HolderSet<Gas> gases) {
        return of(strict, DataComponentPredicate.allOf(map), gases);
    }

    @Contract("_, _, _ -> new")
    @SafeVarargs
    public static @NotNull GasIngredient of(boolean strict, DataComponentPredicate predicate, Holder<Gas>... gases) {
        return of(strict, predicate, HolderSet.direct(gases));
    }

    @Contract("_, _, _ -> new")
    public static @NotNull GasIngredient of(boolean strict, DataComponentPredicate predicate, Gas... gases) {
        return of(strict, predicate, HolderSet.direct(Arrays.stream(gases).map(Gas::getHolder).toList()));
    }

    @Contract("_, _, _ -> new")
    public static @NotNull GasIngredient of(boolean strict, DataComponentPredicate predicate, HolderSet<Gas> gases) {
        return new DataComponentGasIngredient(gases, predicate, strict);
    }

    @Override
    public Stream<GasStack> generateStacks() {
        return Stream.of(stacks);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public GasIngredientType<?> getType() {
        return CCBGasRegistries.DATA_COMPONENT_GAS_INGREDIENT_TYPE.get();
    }

    @Override
    public boolean test(GasStack stack) {
        if (strict) {
            return Arrays.stream(stacks).anyMatch(gasStack -> GasStack.isSameGasSameComponents(stack, gasStack));
        }
        else {
            return gases.contains(stack.getGasHolder()) && components.test(stack);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(gases, components, strict);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof DataComponentGasIngredient other && other.gases() == gases && other.components() == components && other.strict == strict;
    }

    public HolderSet<Gas> gases() {
        return gases;
    }

    public DataComponentPredicate components() {
        return components;
    }

    public boolean isStrict() {
        return strict;
    }
}
