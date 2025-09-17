package net.ty.createcraftedbeginning.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public class CCBDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS_REGISTER = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, CreateCraftedBeginning.MOD_ID);

    public static final DataComponentType<GasStack> CANISTER_CONTENT = register("canister_content", builder -> builder.persistent(GasStack.OPTIONAL_CODEC).networkSynchronized(GasStack.OPTIONAL_STREAM_CODEC));
    public static final DataComponentType<Integer> CANISTER_AIR = register("canister_air", builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DataComponentType<ItemContainerContents> FILTER_ITEM = register("filter_item", builder -> builder.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC));
    public static final DataComponentType<Integer> BREEZE_TIME = register("breeze_time", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private static <T> @NotNull DataComponentType<T> register(String name, @NotNull UnaryOperator<DataComponentType.Builder<T>> builder) {
        DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
        COMPONENTS_REGISTER.register(name, () -> type);
        return type;
    }

    public static void register(IEventBus eventBus) {
        COMPONENTS_REGISTER.register(eventBus);
    }
}