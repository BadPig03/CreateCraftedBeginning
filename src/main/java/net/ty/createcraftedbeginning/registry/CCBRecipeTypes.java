package net.ty.createcraftedbeginning.registry;

import com.mojang.serialization.Codec;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.createmod.catnip.lang.Lang;
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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.recipes.CuttingWithGasRecipe;
import net.ty.createcraftedbeginning.api.gas.recipes.DeployerApplicationWithGasRecipe;
import net.ty.createcraftedbeginning.api.gas.recipes.FillingWithGasRecipe;
import net.ty.createcraftedbeginning.api.gas.recipes.ItemApplicationWithGasRecipe;
import net.ty.createcraftedbeginning.api.gas.recipes.ItemApplicationWithGasRecipe.Serializer;
import net.ty.createcraftedbeginning.api.gas.recipes.ItemApplicationWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.PressingWithGasRecipe;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipe.Factory;
import net.ty.createcraftedbeginning.api.gas.recipes.SequencedAssemblyWithGasRecipeSerializer;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.ConversionRecipe;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.recipe.EnergizationRecipe;
import net.ty.createcraftedbeginning.recipe.ChillingRecipe;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.PressurizationRecipe;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.ResidueGenerationRecipe;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public enum CCBRecipeTypes implements IRecipeTypeInfo, StringRepresentable {
    CONVERSION(ConversionRecipe::new),
    CHILLING(ChillingRecipe::new),
    COOLING(CoolingRecipe::new),
    GAS_INJECTION(GasInjectionRecipe::new),
    PRESSURIZATION(PressurizationRecipe::new),
    WIND_CHARGING(WindChargingRecipe::new),
    ENERGIZATION(EnergizationRecipe::new),
    REACTOR_KETTLE(ReactorKettleRecipe::new),
    RESIDUE_GENERATION(ResidueGenerationRecipe::new),

    CUTTING_WITH_GAS(CuttingWithGasRecipe::new),
    FILLING_WITH_GAS(FillingWithGasRecipe::new),
    PRESSING_WITH_GAS(PressingWithGasRecipe::new),
    DEPLOYING_WITH_GAS(DeployerApplicationWithGasRecipe::new),
    SEQUENCED_ASSEMBLY_WITH_GAS(SequencedAssemblyWithGasRecipeSerializer::new);

    public static final Codec<CCBRecipeTypes> CODEC = StringRepresentable.fromEnum(CCBRecipeTypes::values);
    public final ResourceLocation id;
    public final Supplier<RecipeSerializer<?>> serializerSupplier;
    private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializerObject;
    private final Supplier<RecipeType<?>> type;

    CCBRecipeTypes(StandardProcessingRecipe.Factory<?> processingFactory) {
        this(() -> new StandardProcessingRecipe.Serializer<>(processingFactory));
    }

    CCBRecipeTypes(Supplier<RecipeSerializer<?>> serializerSupplier) {
        String name = Lang.asId(name());
        id = CreateCraftedBeginning.asResource(name);
        this.serializerSupplier = serializerSupplier;
        serializerObject = Registers.SERIALIZER_REGISTER.register(name, serializerSupplier);
        @Nullable DeferredHolder<RecipeType<?>, RecipeType<?>> typeObject = Registers.TYPE_REGISTER.register(name, () -> RecipeType.simple(id));
        type = typeObject;
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
    public @NotNull String getSerializedName() {
        return id.toString();
    }

    private static class Registers {
        private static final DeferredRegister<RecipeSerializer<?>> SERIALIZER_REGISTER = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, CreateCraftedBeginning.MOD_ID);
        private static final DeferredRegister<RecipeType<?>> TYPE_REGISTER = DeferredRegister.create(Registries.RECIPE_TYPE, CreateCraftedBeginning.MOD_ID);
    }
}
