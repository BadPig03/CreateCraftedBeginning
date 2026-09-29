package net.ty.createcraftedbeginning;

import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.events.RegisterAirtightHandlersEvent;
import net.ty.createcraftedbeginning.api.events.RegisterAtmosphereProvidersEvent;
import net.ty.createcraftedbeginning.api.events.RegisterGameplayPressureProfilesEvent;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasBuilder;
import net.ty.createcraftedbeginning.api.gas.GasRegistries;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.compat.CCBCompatBootstrap;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.AirtightBootsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.AirtightChestplateUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.AirtightHelmetUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.AirtightLeggingsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.AirtightHandheldDrillUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.handlers.CCBBuiltInAirtightHandlers;
import net.ty.createcraftedbeginning.content.airtights.handlers.atmosphere.CCBBuiltInAtmosphereProviders;
import net.ty.createcraftedbeginning.content.end.endcasing.EndCasingBlock;
import net.ty.createcraftedbeginning.datagen.CCBDataGen;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import net.ty.createcraftedbeginning.recipe.gas.ingredient.GasIngredientTypes;
import net.ty.createcraftedbeginning.registry.CCBArmInteractionPointTypes;
import net.ty.createcraftedbeginning.registry.CCBArmorMaterials;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabs;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBDisplaySources;
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

    private static final CreateRegistrate CCB_REGISTRATE = CCBRegistrateProvider.get();

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
        CCBTags.register();
        CCBConfig.register(modContainer);
        CCBStressProviders.register(CCBConfig.server().kinetics);

        addRegistrationListeners(modEventBus);
        modEventBus.addListener(CreateCraftedBeginning::init);
        modEventBus.addListener(EventPriority.HIGHEST, CCBDataGen::gatherDataHighPriority);
        modEventBus.addListener(EventPriority.LOWEST, CCBDataGen::gatherData);
        modEventBus.addListener(CCBSoundEvents::register);
    }

    public static void init(FMLCommonSetupEvent event) {
        CCBCompatBootstrap.commonSetup(event);
        CCBFluids.registerFluidInteractions();
        EndCasingBlock.registerPlacementHelpers();

        AirtightHelmetUpgradeRegistry.registerUpgrades();
        AirtightChestplateUpgradeRegistry.registerUpgrades();
        AirtightLeggingsUpgradeRegistry.registerUpgrades();
        AirtightBootsUpgradeRegistry.registerUpgrades();
        AirtightHandheldDrillUpgradeRegistry.registerUpgrades();
        event.enqueueWork(() -> {
            ModLoader.postEvent(new RegisterGameplayPressureProfilesEvent());
            GameplayPressureProfiles.seal();
            CCBBuiltInAirtightHandlers.register();
            CCBBuiltInAtmosphereProviders.register();
            ModLoader.postEvent(new RegisterAirtightHandlersEvent());
            ModLoader.postEvent(new RegisterAtmosphereProvidersEvent());
            CCBUnpackingHandlers.register();
        });
    }

    @Contract("_ -> new")
    public static ResourceLocation asResource(String path) {
        return CCBAPI.asResource(path);
    }

    public static CreateRegistrate registrate() {
        return CCBRegistrateProvider.get();
    }

    private static void bootstrapRegistrateEntries() {
        CCBMountedStorage.register();
        CCBDisplaySources.register();
        CCBBlocks.register();
        CCBItems.register();
        CCBBlockEntities.register();
        CCBEntityTypes.register();
    }

    private static void addRegistrationListeners(IEventBus modEventBus) {
        modEventBus.addListener(CreateCraftedBeginning::registerEventListener);
        modEventBus.addListener(CreateCraftedBeginning::registerRegistries);

        CCBGases.GAS_REGISTER.register(modEventBus);
        GasIngredientTypes.REGISTER.register(modEventBus);
    }

    private static void registerEventListener(RegisterEvent event) {
        event.register(GasRegistries.GAS_REGISTRY_KEY, GasRegistries.EMPTY_GAS_KEY.location(), () -> new Gas(GasBuilder.builder()));
    }

    private static void registerRegistries(NewRegistryEvent event) {
        event.register(GasRegistries.GAS_REGISTRY);
        event.register(GasIngredientTypes.REGISTRY);
    }

}
