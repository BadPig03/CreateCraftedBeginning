package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
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
import net.ty.createcraftedbeginning.api.gas.recipes.ItemApplicationWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipe.Factory;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.CuttingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.DeployerApplicationWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.FillingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.ItemApplicationWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.ItemApplicationWithGasRecipe.Serializer;
import net.ty.createcraftedbeginning.recipe.gas.PressingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.gas.SequencedAssemblyWithGasRecipeSerializer;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum CCBRecipeTypes implements IRecipeTypeInfo, StringRepresentable {
    CHILLING(ChillingRecipe::new),
    COOLING(CoolingRecipe::new),
    DISSIPATION(DissipationRecipe::new),
    ENERGIZATION(EnergizationRecipe::new),
    FORGING_PRESS(ForgingPressRecipe::new),
    GAS_INJECTION(GasInjectionRecipe::new),
    PRESSURIZATION(PressurizationRecipe::new),
    REACTOR_KETTLE(ReactorKettleRecipe::new),
    RESIDUE_GENERATION(ResidueGenerationRecipe::new),
    WIND_CHARGING(WindChargingRecipe.Serializer::new),

    CUTTING_WITH_GAS(CuttingWithGasRecipe::new),
    DEPLOYING_WITH_GAS(DeployerApplicationWithGasRecipe::new),
    FILLING_WITH_GAS(FillingWithGasRecipe::new),
    PRESSING_WITH_GAS(PressingWithGasRecipe::new),
    SEQUENCED_ASSEMBLY_WITH_GAS(SequencedAssemblyWithGasRecipeSerializer::new);

    private final ResourceLocation id;
    private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializerObject;
    private final Supplier<RecipeType<?>> type;

    CCBRecipeTypes(StandardProcessingRecipe.Factory<?> processingFactory) {
        this(() -> new StandardProcessingRecipe.Serializer<>(processingFactory));
    }

    CCBRecipeTypes(Supplier<RecipeSerializer<?>> serializerSupplier) {
        String recipeName = Lang.asId(name());
        id = CCBAPI.asResource(recipeName);
        serializerObject = Registers.SERIALIZER_REGISTER.register(recipeName, serializerSupplier);
        type = Registers.TYPE_REGISTER.register(recipeName, () -> RecipeType.simple(id));
    }

    CCBRecipeTypes(StandardProcessingWithGasRecipe.Factory<?> processingFactory) {
        this(() -> new StandardProcessingWithGasRecipe.Serializer<>(processingFactory));
    }

    CCBRecipeTypes(Factory<ItemApplicationWithGasRecipeParams, ? extends ItemApplicationWithGasRecipe> itemApplicationFactory) {
        this(() -> new Serializer<>(itemApplicationFactory));
    }

    @Internal
    public static void register(IEventBus modEventBus) {
        Registers.SERIALIZER_REGISTER.register(modEventBus);
        Registers.TYPE_REGISTER.register(modEventBus);
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

    private static class Registers {
        private static final DeferredRegister<RecipeSerializer<?>> SERIALIZER_REGISTER = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, CCBAPI.MOD_ID);
        private static final DeferredRegister<RecipeType<?>> TYPE_REGISTER = DeferredRegister.create(Registries.RECIPE_TYPE, CCBAPI.MOD_ID);
    }
}
