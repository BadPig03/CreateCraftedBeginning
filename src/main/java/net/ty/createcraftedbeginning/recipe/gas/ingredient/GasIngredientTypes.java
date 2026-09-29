package net.ty.createcraftedbeginning.recipe.gas.ingredient;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasIngredientTypes {
    public static final ResourceKey<Registry<GasIngredientType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(CCBAPI.asResource("gas_ingredient_type"));
    public static final DeferredRegister<GasIngredientType<?>> REGISTER = DeferredRegister.create(REGISTRY_KEY, CCBAPI.MOD_ID);

    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<SingleGasIngredient>> SINGLE = REGISTER.register("single", () -> new GasIngredientType<>(SingleGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<TagGasIngredient>> TAG = REGISTER.register("tag", () -> new GasIngredientType<>(TagGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<EmptyGasIngredient>> EMPTY = REGISTER.register("empty", () -> new GasIngredientType<>(EmptyGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<CompoundGasIngredient>> COMPOUND = REGISTER.register("compound", () -> new GasIngredientType<>(CompoundGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<DataComponentGasIngredient>> DATA_COMPONENT = REGISTER.register("components", () -> new GasIngredientType<>(DataComponentGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<DifferenceGasIngredient>> DIFFERENCE = REGISTER.register("difference", () -> new GasIngredientType<>(DifferenceGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<IntersectionGasIngredient>> INTERSECTION = REGISTER.register("intersection", () -> new GasIngredientType<>(IntersectionGasIngredient.CODEC));

    public static final Registry<GasIngredientType<?>> REGISTRY = new RegistryBuilder<>(REGISTRY_KEY).sync(true).create();

    private GasIngredientTypes() {
    }
}
