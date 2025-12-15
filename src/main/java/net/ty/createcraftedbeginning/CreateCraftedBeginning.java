package net.ty.createcraftedbeginning;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.item.ItemDescription.Modifier;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.ty.createcraftedbeginning.advancement.CCBTriggers;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.GasBuilder;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gas.GasWorldContentsDataManager;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackContentsDataManager;
import net.ty.createcraftedbeginning.data.CCBDataGen;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import net.ty.createcraftedbeginning.data.CCBGases;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import net.ty.createcraftedbeginning.init.CCBAirtightArmorsHandlers;
import net.ty.createcraftedbeginning.init.CCBAirtightCannonHandlers;
import net.ty.createcraftedbeginning.init.CCBAirtightHandheldDrillHandlers;
import net.ty.createcraftedbeginning.init.CCBCoolantStrategyHandlers;
import net.ty.createcraftedbeginning.init.CCBOpenPipeEffectHandlers;
import net.ty.createcraftedbeginning.init.CCBOpenPipeExtractHandlers;
import net.ty.createcraftedbeginning.init.CCBUnpackingHandlers;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBArmInteractionPointTypes;
import net.ty.createcraftedbeginning.registry.CCBArmorMaterials;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabs;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBEntityTypes;
import net.ty.createcraftedbeginning.registry.CCBFanProcessingTypes;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import net.ty.createcraftedbeginning.registry.CCBTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(CreateCraftedBeginning.MOD_ID)
public class CreateCraftedBeginning {
    public static final String MOD_ID = "createcraftedbeginning";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final GasCanisterPackContentsDataManager GAS_CANISTER_PACK_CONTENTS_DATA_MANAGER = new GasCanisterPackContentsDataManager();
    public static final GasWorldContentsDataManager GAS_WORLD_CONTENTS_DATA_MANAGER = new GasWorldContentsDataManager();

    @SuppressWarnings("DataFlowIssue")
    public static final CCBRegistrate CCB_REGISTRATE = CCBRegistrate.create(MOD_ID).defaultCreativeTab((ResourceKey<CreativeModeTab>) null).setTooltipModifierFactory(item -> new Modifier(item, Palette.STANDARD_CREATE).andThen(TooltipModifier.mapNull(KineticStats.create(item))));

    public CreateCraftedBeginning(IEventBus modEventBus, ModContainer modContainer) {
        CCB_REGISTRATE.registerEventListeners(modEventBus);

        CCBSoundEvents.prepare();

        CCBArmInteractionPointTypes.register(modEventBus);
        CCBArmorMaterials.register(modEventBus);
        CCBBlockEntities.register();
        CCBBlocks.register();
        CCBCreativeTabs.register(modEventBus);
        CCBDataComponents.register(modEventBus);
        CCBEntityTypes.register();
        CCBItems.register();
        CCBFanProcessingTypes.register(modEventBus);
        CCBFluids.register();
        CCBMenuTypes.register();
        CCBMountedStorage.register();
        CCBPackets.register();
        CCBPartialModels.register();
        CCBParticleTypes.register(modEventBus);
        CCBRecipeTypes.register(modEventBus);
        CCBTags.register();

        CCBConfig.register(modContainer);

        NeoForge.EVENT_BUS.register(this);
        addRegistrationListeners(modEventBus);
        modEventBus.addListener(CreateCraftedBeginning::init);
        modEventBus.addListener(CreateCraftedBeginning::onRegister);
        modEventBus.addListener(EventPriority.HIGHEST, CCBDataGen::gatherDataHighPriority);
        modEventBus.addListener(EventPriority.LOWEST, CCBDataGen::gatherData);
        modEventBus.addListener(EventPriority.NORMAL, false, RegisterParticleProvidersEvent.class, CCBParticleTypes::registerFactories);
        modEventBus.addListener(CCBSoundEvents::register);
    }

    public static void onRegister(@NotNull RegisterEvent event) {
        if (event.getRegistry() != BuiltInRegistries.TRIGGER_TYPES) {
            return;
        }

        CCBAdvancements.register();
        CCBTriggers.register();
    }

    public static void init(@NotNull FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CCBAirtightArmorsHandlers.register();
            CCBAirtightCannonHandlers.register();
            CCBAirtightHandheldDrillHandlers.register();

            CCBCoolantStrategyHandlers.register();
            CCBOpenPipeEffectHandlers.register();
            CCBOpenPipeExtractHandlers.register();
            CCBUnpackingHandlers.register();
        });
    }

    @Contract("_ -> new")
    public static @NotNull ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static CCBRegistrate registrate() {
        return CCB_REGISTRATE;
    }

    private static void addRegistrationListeners(@NotNull IEventBus modEventBus) {
        modEventBus.addListener(CreateCraftedBeginning::registerEventListener);
        modEventBus.addListener(CreateCraftedBeginning::registerRegistries);

        CCBGases.GAS_REGISTER.register(modEventBus);
    }

    private static void registerEventListener(@NotNull RegisterEvent event) {
        event.register(CCBRegistries.GAS_REGISTRY_KEY, CCBGasRegistries.EMPTY_GAS_KEY.location(), () -> new Gas(GasBuilder.builder()));
    }

    private static void registerRegistries(@NotNull NewRegistryEvent event) {
        event.register(CCBGasRegistries.GAS_REGISTRY);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }
}
