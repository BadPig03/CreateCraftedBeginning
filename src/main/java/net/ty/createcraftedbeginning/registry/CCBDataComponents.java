package net.ty.createcraftedbeginning.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.component.DataComponentType;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

public class CCBDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS_REGISTER = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, CreateCraftedBeginning.MOD_ID);

    public static void register(IEventBus eventBus) {
        COMPONENTS_REGISTER.register(eventBus);
    }
}