package net.ty.createcraftedbeginning.data;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.CompoundGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.DataComponentGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.DifferenceGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.EmptyGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasIngredientType;
import net.ty.createcraftedbeginning.api.gas.gases.IntersectionGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.SingleGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.TagGasIngredient;
import net.ty.createcraftedbeginning.registry.CCBRegistries;

public class CCBGasRegistries {
    public static final ResourceKey<Gas> EMPTY_GAS_KEY = ResourceKey.create(CCBRegistries.GAS_REGISTRY_KEY, CreateCraftedBeginning.asResource("empty"));

    public static final DeferredRegister<GasIngredientType<?>> GAS_INGREDIENT_TYPES = DeferredRegister.create(CCBRegistries.GAS_INGREDIENT_TYPES, CreateCraftedBeginning.MOD_ID);
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<SingleGasIngredient>> SINGLE_GAS_INGREDIENT_TYPE = GAS_INGREDIENT_TYPES.register("single", () -> new GasIngredientType<>(SingleGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<TagGasIngredient>> TAG_GAS_INGREDIENT_TYPE = GAS_INGREDIENT_TYPES.register("tag", () -> new GasIngredientType<>(TagGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<EmptyGasIngredient>> EMPTY_GAS_INGREDIENT_TYPE = GAS_INGREDIENT_TYPES.register("empty", () -> new GasIngredientType<>(EmptyGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<CompoundGasIngredient>> COMPOUND_GAS_INGREDIENT_TYPE = GAS_INGREDIENT_TYPES.register("compound", () -> new GasIngredientType<>(CompoundGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<DataComponentGasIngredient>> DATA_COMPONENT_GAS_INGREDIENT_TYPE = GAS_INGREDIENT_TYPES.register("components", () -> new GasIngredientType<>(DataComponentGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<DifferenceGasIngredient>> DIFFERENCE_GAS_INGREDIENT_TYPE = GAS_INGREDIENT_TYPES.register("difference", () -> new GasIngredientType<>(DifferenceGasIngredient.CODEC));
    public static final DeferredHolder<GasIngredientType<?>, GasIngredientType<IntersectionGasIngredient>> INTERSECTION_GAS_INGREDIENT_TYPE = GAS_INGREDIENT_TYPES.register("intersection", () -> new GasIngredientType<>(IntersectionGasIngredient.CODEC));

    @SuppressWarnings("deprecation")
    public static final DefaultedRegistry<Gas> GAS_REGISTRY = (DefaultedRegistry<Gas>) new RegistryBuilder<>(CCBRegistries.GAS_REGISTRY_KEY).defaultKey(EMPTY_GAS_KEY).sync(true).withIntrusiveHolders().create();
    public static final Registry<GasIngredientType<?>> GAS_INGREDIENT_TYPES_REGISTRY = new RegistryBuilder<>(CCBRegistries.GAS_INGREDIENT_TYPES).sync(true).create();
}