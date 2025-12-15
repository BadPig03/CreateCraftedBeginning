package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.fanprocessing.FreezingFanProcessingType;

public class CCBFanProcessingTypes {
    private static final DeferredRegister<FanProcessingType> TYPES = DeferredRegister.create(CreateRegistries.FAN_PROCESSING_TYPE, CreateCraftedBeginning.MOD_ID);

    public static final DeferredHolder<FanProcessingType, FreezingFanProcessingType> FREEZING = TYPES.register("freezing", FreezingFanProcessingType::new);

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
    }
}