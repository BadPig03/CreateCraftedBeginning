package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.fanprocessing.ChillingFanProcessingType;

@SuppressWarnings("unused")
public class CCBFanProcessingTypes {
    private static final DeferredRegister<FanProcessingType> TYPES = DeferredRegister.create(CreateRegistries.FAN_PROCESSING_TYPE, CreateCraftedBeginning.MOD_ID);

    public static final DeferredHolder<FanProcessingType, ChillingFanProcessingType> CHILLING = TYPES.register("chilling", ChillingFanProcessingType::new);

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
    }
}