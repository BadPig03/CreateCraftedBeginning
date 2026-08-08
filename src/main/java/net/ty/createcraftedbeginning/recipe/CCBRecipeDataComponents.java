package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe.SequencedAssemblyWithGas;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBRecipeDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, CCBAPI.MOD_ID);

    public static final DataComponentType<SequencedAssemblyWithGas> SEQUENCED_ASSEMBLY_WITH_GAS = registerSequencedAssembly();

    private CCBRecipeDataComponents() {
    }

    private static DataComponentType<SequencedAssemblyWithGas> registerSequencedAssembly() {
        DataComponentType<SequencedAssemblyWithGas> type = DataComponentType.<SequencedAssemblyWithGas>builder().persistent(SequencedAssemblyWithGas.CODEC).networkSynchronized(SequencedAssemblyWithGas.STREAM_CODEC).build();
        COMPONENTS.register("sequenced_assembly_with_gas", () -> type);
        return type;
    }

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}
