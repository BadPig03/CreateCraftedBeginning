package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Factory;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Serializer;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum CCBRecipeTypes implements IRecipeTypeInfo, StringRepresentable {
    CHILLING(ChillingRecipe::new),
    CHILLED_COMPACTING(AllRecipeTypes.COMPACTING, ChilledCompactingRecipe::new),
    CHILLED_MIXING(AllRecipeTypes.MIXING, ChilledMixingRecipe::new),
    COOLING(CoolingRecipe::new),
    DISSIPATION(() -> new StandardGasProcessingRecipe.Serializer<>(DissipationRecipe::new)),
    ENERGIZATION(() -> new StandardGasProcessingRecipe.Serializer<>(EnergizationRecipe::new)),
    FORGING_PRESS(() -> new StandardGasProcessingRecipe.Serializer<>(ForgingPressRecipe::new)),
    FRACTIONATION_TOWER(FractionationTowerRecipe.Serializer::new),
    GAS_INJECTION(() -> new StandardGasProcessingRecipe.Serializer<>(GasInjectionRecipe::new)),
    REACTOR_KETTLE(() -> new ReactorKettleRecipe.Serializer<>(ReactorKettleRecipe::new)),
    RESIDUE_GENERATION(() -> new StandardGasProcessingRecipe.Serializer<>(ResidueGenerationRecipe::new)),
    WIND_CHARGING(WindChargingRecipe.Serializer::new);

    private final ResourceLocation id;
    private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializerObject;
    private final Supplier<RecipeType<?>> type;

    CCBRecipeTypes(Factory<?> processingFactory) {
        this(() -> new Serializer<>(processingFactory));
    }

    CCBRecipeTypes(IRecipeTypeInfo runtimeType, Factory<?> processingFactory) {
        String recipeName = Lang.asId(name());
        id = CCBAPI.asResource(recipeName);
        serializerObject = Registers.SERIALIZER_REGISTER.register(recipeName, () -> new Serializer<>(processingFactory));
        type = runtimeType::getType;
    }

    CCBRecipeTypes(Supplier<RecipeSerializer<?>> serializerSupplier) {
        String recipeName = Lang.asId(name());
        id = CCBAPI.asResource(recipeName);
        serializerObject = Registers.SERIALIZER_REGISTER.register(recipeName, serializerSupplier);
        type = Registers.TYPE_REGISTER.register(recipeName, () -> RecipeType.simple(id));
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RecipeSerializer<?>> @NotNull T getSerializer() {
        return (T) serializerObject.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
        return (RecipeType<R>) type.get();
    }

    @Override
    public String getSerializedName() {
        return id.toString();
    }

    @Internal
    public static void register(IEventBus modEventBus) {
        Registers.SERIALIZER_REGISTER.register(modEventBus);
        Registers.TYPE_REGISTER.register(modEventBus);
    }

    private static class Registers {
        private static final DeferredRegister<RecipeSerializer<?>> SERIALIZER_REGISTER = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, CCBAPI.MOD_ID);
        private static final DeferredRegister<RecipeType<?>> TYPE_REGISTER = DeferredRegister.create(Registries.RECIPE_TYPE, CCBAPI.MOD_ID);
    }
}
