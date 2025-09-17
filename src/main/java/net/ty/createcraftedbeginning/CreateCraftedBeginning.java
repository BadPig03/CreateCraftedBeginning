package net.ty.createcraftedbeginning;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
import net.ty.createcraftedbeginning.advancement.CCBAdvancements;
import net.ty.createcraftedbeginning.advancement.CCBTriggers;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtightcannon.CCBAirtightCannonProjectileBlockHitActions;
import net.ty.createcraftedbeginning.content.airtightcannon.CCBAirtightCannonProjectileEntityHitActions;
import net.ty.createcraftedbeginning.content.airtightcannon.CCBAirtightCannonProjectileRenderModes;
import net.ty.createcraftedbeginning.data.CCBDataGen;
import net.ty.createcraftedbeginning.data.CCBGasRegistry;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import net.ty.createcraftedbeginning.data.CCBTags;
import net.ty.createcraftedbeginning.registry.CCBArmInteractionPointTypes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabs;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBEntityTypes;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.CCBGases;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import net.ty.createcraftedbeginning.registry.CCBOpenPipeEffectHandlers;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import net.ty.createcraftedbeginning.registry.CCBUnpackingHandlers;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(CreateCraftedBeginning.MOD_ID)
public class CreateCraftedBeginning {
    public static final String MOD_ID = "createcraftedbeginning";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final CCBRegistrate CCB_REGISTRATE = CCBRegistrate.create(MOD_ID).defaultCreativeTab(CCBCreativeTabs.CREATIVE_TAB.getKey()).setTooltipModifierFactory(item -> new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE).andThen(TooltipModifier.mapNull(KineticStats.create(item))));

    public CreateCraftedBeginning(IEventBus modEventBus, ModContainer modContainer) {
        CCB_REGISTRATE.registerEventListeners(modEventBus);

        CCBArmInteractionPointTypes.register(modEventBus);
        CCBBlockEntities.register();
        CCBBlocks.register();
        CCBCreativeTabs.register(modEventBus);
        CCBDataComponents.register(modEventBus);
        CCBEntityTypes.register();
        CCBFluids.register();
        CCBItems.register();
        CCBMountedStorage.register();
        CCBPackets.register();
        CCBPartialModels.register();
        CCBParticleTypes.register(modEventBus);
        CCBRecipeTypes.register(modEventBus);
        CCBSoundEvents.prepare();
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

    @Contract("_ -> new")
    public static @NotNull ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static CCBRegistrate registrate() {
        return CCB_REGISTRATE;
    }

    public static void onRegister(final @NotNull RegisterEvent event) {
        CCBAirtightCannonProjectileRenderModes.init();
        CCBAirtightCannonProjectileEntityHitActions.init();
        CCBAirtightCannonProjectileBlockHitActions.init();

        if (event.getRegistry() == BuiltInRegistries.TRIGGER_TYPES) {
            CCBAdvancements.register();
            CCBTriggers.register();
        }
    }

    public static void init(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CCBUnpackingHandlers.register();
            CCBOpenPipeEffectHandlers.register();
        });
    }

    private void addRegistrationListeners(@NotNull IEventBus modEventBus) {
        modEventBus.addListener(this::registerEventListener);
        modEventBus.addListener(this::registerRegistries);

        CCBGases.GAS_REGISTER.register(modEventBus);
    }

    private void registerEventListener(@NotNull RegisterEvent event) {
        event.register(CCBGasRegistry.GAS_REGISTRY_NAME, CCBGasRegistry.EMPTY_GAS_KEY.location(), () -> CCBGasRegistry.EMPTY_GAS);
    }

    private void registerRegistries(@NotNull NewRegistryEvent event) {
        event.register(CCBGasRegistry.GAS_REGISTRY);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }
}
