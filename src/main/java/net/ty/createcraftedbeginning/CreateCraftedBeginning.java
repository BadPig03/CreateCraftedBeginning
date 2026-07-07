package net.ty.createcraftedbeginning;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.item.ItemDescription.Modifier;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.api.drainagehandlers.CCBAirtightDrainageHandlers;
import net.ty.createcraftedbeginning.api.enginehandlers.CCBAirtightEngineHandlers;
import net.ty.createcraftedbeginning.api.fillhandlers.CCBAirtightFillHandlers;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasBuilder;
import net.ty.createcraftedbeginning.api.turbinehandlers.CCBAirtightTurbineHandlers;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.AirtightBootsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.AirtightChestplateUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.AirtightHelmetUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.AirtightLeggingsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.AirtightHandheldDrillUpgradeRegistry;
import net.ty.createcraftedbeginning.content.end.endcasing.EndCasingBlock;
import net.ty.createcraftedbeginning.data.CCBDataGen;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import net.ty.createcraftedbeginning.data.CCBGases;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import net.ty.createcraftedbeginning.api.armorhandlers.CCBAirtightArmorsHandlers;
import net.ty.createcraftedbeginning.api.cannonhandlers.CCBAirtightCannonHandlers;
import net.ty.createcraftedbeginning.api.armhandlers.CCBAirtightArmHandlers;
import net.ty.createcraftedbeginning.api.drillhandlers.CCBAirtightDrillHandlers;
import net.ty.createcraftedbeginning.api.coolantshandlers.CCBAirtightCoolantHandlers;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.CCBAirtightThermoregulatorHandlers;
import net.ty.createcraftedbeginning.registry.CCBUnpackingHandlers;
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
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import net.ty.createcraftedbeginning.registry.CCBTags;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mod(CreateCraftedBeginning.MOD_ID)
public class CreateCraftedBeginning {
    public static final String MOD_ID = "createcraftedbeginning";
    public static final String NAME = "Create Crafted Beginning";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("DataFlowIssue")
    private static final CCBRegistrate CCB_REGISTRATE = CCBRegistrate.create(MOD_ID).defaultCreativeTab((ResourceKey<CreativeModeTab>) null).setTooltipModifierFactory(item -> new Modifier(item, Palette.STANDARD_CREATE).andThen(TooltipModifier.mapNull(KineticStats.create(item))));

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
        CCBMobEffects.register(modEventBus);
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

    public static void onRegister(RegisterEvent event) {
        if (event.getRegistry() != BuiltInRegistries.TRIGGER_TYPES) {
            return;
        }

        CCBAdvancements.register();
        CCBTriggers.register();
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
            CCBAirtightArmorsHandlers.register();
            CCBAirtightCannonHandlers.register();
            CCBAirtightEngineHandlers.register();
            CCBAirtightTurbineHandlers.register();
            CCBAirtightArmHandlers.register();
            CCBAirtightDrillHandlers.register();
            CCBAirtightThermoregulatorHandlers.register();
            CCBAirtightCoolantHandlers.register();
            CCBAirtightDrainageHandlers.register();
            CCBAirtightFillHandlers.register();
            CCBUnpackingHandlers.register();
        });
    }

    @Contract("_ -> new")
    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static CCBRegistrate registrate() {
        return CCB_REGISTRATE;
    }

    private static void addRegistrationListeners(IEventBus modEventBus) {
        modEventBus.addListener(CreateCraftedBeginning::registerEventListener);
        modEventBus.addListener(CreateCraftedBeginning::registerRegistries);

        CCBGases.GAS_REGISTER.register(modEventBus);
        CCBGasRegistries.GAS_INGREDIENT_TYPES.register(modEventBus);
    }

    private static void registerEventListener(RegisterEvent event) {
        event.register(CCBRegistries.GAS_REGISTRY_KEY, CCBGasRegistries.EMPTY_GAS_KEY.location(), () -> new Gas(GasBuilder.builder()));
    }

    private static void registerRegistries(NewRegistryEvent event) {
        event.register(CCBGasRegistries.GAS_REGISTRY);
        event.register(CCBGasRegistries.GAS_INGREDIENT_TYPES_REGISTRY);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }
}
