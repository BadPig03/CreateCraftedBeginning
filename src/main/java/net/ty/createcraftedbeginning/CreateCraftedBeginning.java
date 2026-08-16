package net.ty.createcraftedbeginning;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.ty.createcraftedbeginning.advancement.triggers.CCBTriggersRegistry;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.events.RegisterAirtightHandlersEvent;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasBuilder;
import net.ty.createcraftedbeginning.api.gas.gases.GasRegistries;
import net.ty.createcraftedbeginning.compat.CCBCompatBootstrap;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.AirtightBootsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.AirtightChestplateUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.AirtightHelmetUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.AirtightLeggingsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.AirtightHandheldDrillUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.handlers.CCBBuiltInAirtightHandlers;
import net.ty.createcraftedbeginning.content.end.endcasing.EndCasingBlock;
import net.ty.createcraftedbeginning.datagen.CCBDataGen;
import net.ty.createcraftedbeginning.recipe.CCBRecipeDataComponents;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
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
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import net.ty.createcraftedbeginning.registry.CCBStressProviders;
import net.ty.createcraftedbeginning.registry.CCBTags;
import net.ty.createcraftedbeginning.registry.CCBUnpackingHandlers;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrate;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrateProvider;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mod(CreateCraftedBeginning.MOD_ID)
public class CreateCraftedBeginning {
    public static final String MOD_ID = CCBAPI.MOD_ID;
    public static final Logger LOGGER = CCBAPI.LOGGER;

    private static final CCBRegistrate CCB_REGISTRATE = CCBRegistrateProvider.get();

    public CreateCraftedBeginning(IEventBus modEventBus, ModContainer modContainer) {
        CCBCompatBootstrap.initialize();
        CCB_REGISTRATE.registerEventListeners(modEventBus);
        bootstrapRegistrateEntries();

        CCBSoundEvents.prepare();
        CCBArmInteractionPointTypes.register(modEventBus);
        CCBArmorMaterials.register(modEventBus);
        CCBCreativeTabs.register(modEventBus);
        CCBDataComponents.register(modEventBus);
        CCBFanProcessingTypes.register(modEventBus);
        CCBFluids.register(modEventBus);
        CCBMenuTypes.register(modEventBus);
        CCBMobEffects.register(modEventBus);
        CCBPackets.register();
        CCBParticleTypes.register(modEventBus);
        CCBRecipeTypes.register(modEventBus);
        CCBRecipeDataComponents.register(modEventBus);
        CCBTags.register();
        CCBConfig.register(modContainer);
        CCBStressProviders.register(CCBConfig.server().stressValues);

        addRegistrationListeners(modEventBus);
        modEventBus.addListener(CreateCraftedBeginning::init);
        modEventBus.addListener(CreateCraftedBeginning::onRegister);
        modEventBus.addListener(EventPriority.HIGHEST, CCBDataGen::gatherDataHighPriority);
        modEventBus.addListener(EventPriority.LOWEST, CCBDataGen::gatherData);
        modEventBus.addListener(CCBSoundEvents::register);
    }

    private static void bootstrapRegistrateEntries() {
        CCBMountedStorage.register();
        CCBBlocks.register();
        CCBItems.register();
        CCBBlockEntities.register();
        CCBEntityTypes.register();
    }

    public static void onRegister(RegisterEvent event) {
        if (event.getRegistry() != BuiltInRegistries.TRIGGER_TYPES) {
            return;
        }

        CCBAdvancements.register();
        CCBTriggersRegistry.register();
    }

    public static void init(FMLCommonSetupEvent event) {
        CCBFluids.registerFluidInteractions();
        EndCasingBlock.registerPlacementHelpers();

        AirtightHelmetUpgradeRegistry.registerUpgrades();
        AirtightChestplateUpgradeRegistry.registerUpgrades();
        AirtightLeggingsUpgradeRegistry.registerUpgrades();
        AirtightBootsUpgradeRegistry.registerUpgrades();
        AirtightHandheldDrillUpgradeRegistry.registerUpgrades();
        event.enqueueWork(() -> {
            CCBBuiltInAirtightHandlers.register();
            NeoForge.EVENT_BUS.post(new RegisterAirtightHandlersEvent());
            CCBUnpackingHandlers.register();
        });
    }

    @Contract("_ -> new")
    public static ResourceLocation asResource(String path) {
        return CCBAPI.asResource(path);
    }

    public static CCBRegistrate registrate() {
        return CCBRegistrateProvider.get();
    }

    private static void addRegistrationListeners(IEventBus modEventBus) {
        modEventBus.addListener(CreateCraftedBeginning::registerEventListener);
        modEventBus.addListener(CreateCraftedBeginning::registerRegistries);

        CCBGases.GAS_REGISTER.register(modEventBus);
        GasRegistries.GAS_INGREDIENT_TYPES.register(modEventBus);
    }

    private static void registerEventListener(RegisterEvent event) {
        event.register(GasRegistries.GAS_REGISTRY_KEY, GasRegistries.EMPTY_GAS_KEY.location(), () -> new Gas(GasBuilder.builder()));
    }

    private static void registerRegistries(NewRegistryEvent event) {
        event.register(GasRegistries.GAS_REGISTRY);
        event.register(GasRegistries.GAS_INGREDIENT_TYPES_REGISTRY);
    }

}
