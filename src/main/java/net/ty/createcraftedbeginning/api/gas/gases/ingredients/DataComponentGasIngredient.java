package net.ty.createcraftedbeginning.api.gas.gases.ingredients;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.HolderSetCodec;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DataComponentGasIngredient extends GasIngredient {
    public static final MapCodec<DataComponentGasIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(HolderSetCodec.create(CCBRegistries.GAS_REGISTRY_KEY, CCBGasRegistries.GAS_REGISTRY.holderByNameCodec(), false).fieldOf("gases").forGetter(DataComponentGasIngredient::gases), DataComponentPredicate.CODEC.fieldOf("components").forGetter(DataComponentGasIngredient::components), Codec.BOOL.optionalFieldOf("strict", false).forGetter(DataComponentGasIngredient::isStrict)).apply(instance, DataComponentGasIngredient::new));

    private final HolderSet<Gas> gases;
    private final DataComponentPredicate components;
    private final boolean strict;
    private final GasStack[] stacks;

    /**
     * Creates a new {@code DataComponentGasIngredient} instance.
     *
     * @param gases      the gases to inspect or process
     * @param components the data components to apply
     * @param strict     whether strict matching rules should be used
     */
    public DataComponentGasIngredient(HolderSet<Gas> gases, DataComponentPredicate components, boolean strict) {
        this.gases = gases;
        this.components = components;
        this.strict = strict;
        stacks = gases.stream().map(gas -> new GasStack(gas, FluidType.BUCKET_VOLUME, components.asPatch())).toArray(GasStack[]::new);
    }

    /**
     * Creates a gas ingredient from the supplied values.
     *
     * @param strict whether strict matching rules should be used
     * @param stack  the stack to inspect or process
     * @return the created value
     */
    @Contract("_, _ -> new")
    public static GasIngredient of(boolean strict, GasStack stack) {
        return of(strict, stack.getComponents(), stack.getGasType());
    }

    /**
     * Creates a gas ingredient from the supplied values.
     *
     * @param <T>    the value type used by this operation
     * @param strict whether strict matching rules should be used
     * @param type   the type to use
     * @param value  the value to inspect or process
     * @param gases  the gases to use
     * @return the created value
     */
    @Contract("_, _, _, _ -> new")
    public static <T> @NotNull GasIngredient of(boolean strict, DataComponentType<? super T> type, T value, Gas... gases) {
        return of(strict, DataComponentPredicate.builder().expect(type, value).build(), gases);
    }

    /**
     * Creates a gas ingredient from the supplied values.
     *
     * @param <T>    the value type used by this operation
     * @param strict whether strict matching rules should be used
     * @param type   the type to use
     * @param value  the value to inspect or process
     * @param gases  the gases to use
     * @return the created value
     */
    @Contract("_, _, _, _ -> new")
    public static <T> @NotNull GasIngredient of(boolean strict, Supplier<? extends DataComponentType<? super T>> type, T value, Gas... gases) {
        return of(strict, type.get(), value, gases);
    }

    /**
     * Creates a gas ingredient from the supplied values.
     *
     * @param strict whether strict matching rules should be used
     * @param map    the map to use
     * @param gases  the gases to use
     * @return the created value
     */
    @Contract("_, _, _ -> new")
    public static GasIngredient of(boolean strict, DataComponentMap map, Gas... gases) {
        return of(strict, DataComponentPredicate.allOf(map), gases);
    }

    /**
     * Creates a gas ingredient from the supplied values.
     *
     * @param strict whether strict matching rules should be used
     * @param map    the map to use
     * @param gases  the gases to use
     * @return the created value
     */
    @Contract("_, _, _ -> new")
    @SafeVarargs
    public static GasIngredient of(boolean strict, DataComponentMap map, Holder<Gas>... gases) {
        return of(strict, DataComponentPredicate.allOf(map), gases);
    }

    /**
     * Creates a gas ingredient from the supplied values.
     *
     * @param strict whether strict matching rules should be used
     * @param map    the map to use
     * @param gases  the gases to inspect or process
     * @return the created value
     */
    @Contract("_, _, _ -> new")
    public static GasIngredient of(boolean strict, DataComponentMap map, HolderSet<Gas> gases) {
        return of(strict, DataComponentPredicate.allOf(map), gases);
    }

    /**
     * Creates a gas ingredient from the supplied values.
     *
     * @param strict    whether strict matching rules should be used
     * @param predicate the predicate used to select matching values
     * @param gases     the gases to use
     * @return the created value
     */
    @Contract("_, _, _ -> new")
    @SafeVarargs
    public static GasIngredient of(boolean strict, DataComponentPredicate predicate, Holder<Gas>... gases) {
        return of(strict, predicate, HolderSet.direct(gases));
    }

    /**
     * Creates a gas ingredient from the supplied values.
     *
     * @param strict    whether strict matching rules should be used
     * @param predicate the predicate used to select matching values
     * @param gases     the gases to use
     * @return the created value
     */
    @Contract("_, _, _ -> new")
    public static GasIngredient of(boolean strict, DataComponentPredicate predicate, Gas... gases) {
        return of(strict, predicate, HolderSet.direct(Arrays.stream(gases).map(Gas::getHolder).toList()));
    }

    /**
     * Creates a gas ingredient from the supplied values.
     *
     * @param strict    whether strict matching rules should be used
     * @param predicate the predicate used to select matching values
     * @param gases     the gases to inspect or process
     * @return the created value
     */
    @Contract("_, _, _ -> new")
    public static GasIngredient of(boolean strict, DataComponentPredicate predicate, HolderSet<Gas> gases) {
        return new DataComponentGasIngredient(gases, predicate, strict);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<GasStack> generateStacks() {
        return Stream.of(stacks);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSimple() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GasIngredientType<?> getType() {
        return CCBGasRegistries.DATA_COMPONENT_GAS_INGREDIENT_TYPE.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean test(GasStack stack) {
        if (!strict) {
            return gases.contains(stack.getGasHolder()) && components.test(stack);
        }

        for (GasStack gasStack : stacks) {
            if (!GasStack.isSameGasSameComponents(stack, gasStack)) {
                continue;
            }

            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(gases, components, strict);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof DataComponentGasIngredient other && other.gases() == gases && other.components() == components && other.strict == strict;
    }

    /**
     * Sets the gas ingredients used by this recipe builder.
     *
     * @return the resulting holder set
     */
    public HolderSet<Gas> gases() {
        return gases;
    }

    /**
     * Sets the data components used by this builder.
     *
     * @return the resulting data component predicate
     */
    public DataComponentPredicate components() {
        return components;
    }

    /**
     * Checks whether this value is strict.
     *
     * @return {@code true} if this value is strict; otherwise {@code false}
     */
    public boolean isStrict() {
        return strict;
    }
}
