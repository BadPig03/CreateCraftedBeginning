package net.ty.createcraftedbeginning.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import java.util.function.UnaryOperator;

public class CCBDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS_REGISTER = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, CreateCraftedBeginning.MOD_ID);

    public static final DataComponentType<Integer> CANISTER_AIR = register("canister_air", builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        COMPONENTS_REGISTER.register(name, () -> type);
        return type;
    }

    public static void register(IEventBus eventBus) {
        COMPONENTS_REGISTER.register(eventBus);
    }
}