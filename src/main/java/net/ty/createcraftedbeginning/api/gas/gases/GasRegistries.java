package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.CompoundGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.DataComponentGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.DifferenceGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.EmptyGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.GasIngredientType;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.IntersectionGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SingleGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.TagGasIngredient;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRegistries {
    public static final ResourceKey<Registry<Gas>> GAS_REGISTRY_KEY = registryKey("gas");
    public static final ResourceKey<Registry<GasIngredientType<?>>> GAS_INGREDIENT_TYPES_KEY = registryKey("gas_ingredient_type");
    public static final ResourceKey<Gas> EMPTY_GAS_KEY = ResourceKey.create(GAS_REGISTRY_KEY, CCBAPI.asResource("empty"));

    @SuppressWarnings("deprecation")
    public static final DefaultedRegistry<Gas> GAS_REGISTRY = (DefaultedRegistry<Gas>) new RegistryBuilder<>(GAS_REGISTRY_KEY).defaultKey(EMPTY_GAS_KEY).sync(true).withIntrusiveHolders().create();
    public static final DeferredRegister<GasIngredientType<?>> GAS_INGREDIENT_TYPES = DeferredRegister.create(GAS_INGREDIENT_TYPES_KEY, CCBAPI.MOD_ID);
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<SingleGasIngredient>> SINGLE_GAS_INGREDIENT_TYPE = GAS_INGREDIENT_TYPES.register("single", () -> new GasIngredientType<>(SingleGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<TagGasIngredient>> TAG_GAS_INGREDIENT_TYPE = GAS_INGREDIENT_TYPES.register("tag", () -> new GasIngredientType<>(TagGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<EmptyGasIngredient>> EMPTY_GAS_INGREDIENT_TYPE = GAS_INGREDIENT_TYPES.register("empty", () -> new GasIngredientType<>(EmptyGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<CompoundGasIngredient>> COMPOUND_GAS_INGREDIENT_TYPE = GAS_INGREDIENT_TYPES.register("compound", () -> new GasIngredientType<>(CompoundGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<DataComponentGasIngredient>> DATA_COMPONENT_GAS_INGREDIENT_TYPE = GAS_INGREDIENT_TYPES.register("components", () -> new GasIngredientType<>(DataComponentGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<DifferenceGasIngredient>> DIFFERENCE_GAS_INGREDIENT_TYPE = GAS_INGREDIENT_TYPES.register("difference", () -> new GasIngredientType<>(DifferenceGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<IntersectionGasIngredient>> INTERSECTION_GAS_INGREDIENT_TYPE = GAS_INGREDIENT_TYPES.register("intersection", () -> new GasIngredientType<>(IntersectionGasIngredient.CODEC));
    public static final Registry<GasIngredientType<?>> GAS_INGREDIENT_TYPES_REGISTRY = new RegistryBuilder<>(GAS_INGREDIENT_TYPES_KEY).sync(true).create();

    private GasRegistries() {
    }

    private static <T> ResourceKey<Registry<T>> registryKey(String name) {
        return ResourceKey.createRegistryKey(CCBAPI.asResource(name));
    }
}
