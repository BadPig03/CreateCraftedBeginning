package net.ty.createcraftedbeginning.ponder;

import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.ponder.scenes.breezes.BreezeChamberScenes;
import net.ty.createcraftedbeginning.ponder.scenes.breezes.BreezeCoolerScenes;
import net.ty.createcraftedbeginning.ponder.scenes.breezes.EmptyBreezeCoolerScenes;
import net.ty.createcraftedbeginning.ponder.scenes.crates.AndesiteCrateScenes;
import net.ty.createcraftedbeginning.ponder.scenes.crates.BrassCrateScenes;
import net.ty.createcraftedbeginning.ponder.scenes.crates.CardboardCrateScenes;
import net.ty.createcraftedbeginning.ponder.scenes.crates.SturdyCrateScenes;
import net.ty.createcraftedbeginning.ponder.scenes.end.EndSculkSilencerScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gascontainers.AirtightTankScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gascontainers.CreativeAirtightTankScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.AirtightEngineScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.AirtightForgingPressScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.AirtightFractionationTowerScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.AirtightHatchScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.AirtightReactorKettleScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.BoilerSteamOutletScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.GasFactoryGaugeScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.GasInjectionChamberScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.GasPackagerScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.GasRepackagerScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.PortableGasInterfaceScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.ResidueOutletScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.TeslaTurbineScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.AirtightCheckValveScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.AirtightEncasedPipeScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.AirtightMeterScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.AirtightPipeScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.AirtightPumpScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.AirtightRegulatorPumpScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.AirtightValveScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.AtmosphereExtractionScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.SmartAirtightPipeScenes;
import net.ty.createcraftedbeginning.ponder.scenes.other.AirVentScenes;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBPonderScenes {
    private static final double DISTRIBUTION_DEVIATION = 0.11485000171139836;

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<RegistryEntry<?, ?>> entryHelper = helper.withKeyFunction(RegistryEntry::getId);
        entryHelper.forComponents(CCBBlocks.ANDESITE_CRATE_BLOCK).addStoryBoard("crates_story_board", AndesiteCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.BRASS_CRATE_BLOCK).addStoryBoard("crates_story_board", BrassCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.STURDY_CRATE_BLOCK).addStoryBoard("crates_story_board", SturdyCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.CARDBOARD_CRATE_BLOCK).addStoryBoard("crates_story_board", CardboardCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);

        entryHelper.forComponents(CCBBlocks.AIRTIGHT_PIPE_BLOCK).addStoryBoard("airtight_pipe_connecting_story_board", AirtightPipeScenes::connecting, CCBPonderTags.GAS_PIPES_TAG_ID).addStoryBoard("airtight_pipe_exchange_story_board", AirtightPipeScenes::exchange, CCBPonderTags.GAS_PIPES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_PIPE_BLOCK, CCBBlocks.AIRTIGHT_PUMP_BLOCK).addStoryBoard("airtight_pipe_exchange_story_board", AtmosphereExtractionScenes::dimensions, CCBPonderTags.GAS_EXTRACTORS_TAG_ID).addStoryBoard("airtight_pipe_exchange_story_board", AtmosphereExtractionScenes::biomes, CCBPonderTags.GAS_EXTRACTORS_TAG_ID).addStoryBoard("airtight_pipe_exchange_story_board", AtmosphereExtractionScenes::bubbleColumns, CCBPonderTags.GAS_EXTRACTORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK).addStoryBoard("airtight_encased_pipe_story_board", AirtightEncasedPipeScenes::scene, CCBPonderTags.GAS_PIPES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_CHECK_VALVE_BLOCK).addStoryBoard("airtight_check_valve_story_board", AirtightCheckValveScenes::scene, CCBPonderTags.GAS_PIPES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_VALVE_BLOCK).addStoryBoard("airtight_valve_story_board", AirtightValveScenes::scene, CCBPonderTags.GAS_PIPES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.SMART_AIRTIGHT_PIPE_BLOCK).addStoryBoard("smart_airtight_pipe_story_board", SmartAirtightPipeScenes::scene, CCBPonderTags.GAS_PIPES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_PUMP_BLOCK).addStoryBoard("airtight_pump_story_board", AirtightPumpScenes::scene, CCBPonderTags.GAS_PIPES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_REGULATOR_PUMP_BLOCK).addStoryBoard("airtight_regulator_pump_story_board", AirtightRegulatorPumpScenes::scene, CCBPonderTags.GAS_PIPES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_MANOMETER_BLOCK).addStoryBoard("airtight_manometer_story_board", AirtightMeterScenes::manometer, CCBPonderTags.GAS_PIPES_TAG_ID, AllCreatePonderTags.DISPLAY_SOURCES);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_FLOWMETER_BLOCK).addStoryBoard("airtight_flowmeter_story_board", AirtightMeterScenes::flowmeter, CCBPonderTags.GAS_PIPES_TAG_ID, AllCreatePonderTags.DISPLAY_SOURCES);

        entryHelper.forComponents(CCBBlocks.AIRTIGHT_TANK_BLOCK).addStoryBoard("airtight_tank_storage_story_board", AirtightTankScenes::storage, CCBPonderTags.GAS_CONTAINERS_TAG_ID).addStoryBoard("airtight_tank_size_story_board", AirtightTankScenes::size, CCBPonderTags.GAS_CONTAINERS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.HORIZONTAL_AIRTIGHT_TANK_BLOCK).addStoryBoard("horizontal_airtight_tank_storage_story_board", AirtightTankScenes::horizontalStorage, CCBPonderTags.GAS_CONTAINERS_TAG_ID).addStoryBoard("horizontal_airtight_tank_size_story_board", AirtightTankScenes::horizontalSize, CCBPonderTags.GAS_CONTAINERS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK).addStoryBoard("creative_airtight_tank_storage_story_board", CreativeAirtightTankScenes::storage, CCBPonderTags.GAS_CONTAINERS_TAG_ID).addStoryBoard("creative_airtight_tank_size_story_board", CreativeAirtightTankScenes::size, CCBPonderTags.GAS_CONTAINERS_TAG_ID);

        entryHelper.forComponents(CCBBlocks.EMPTY_BREEZE_COOLER_BLOCK).addStoryBoard("empty_breeze_cooler_story_board", EmptyBreezeCoolerScenes::scene, CCBPonderTags.BREEZES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.BREEZE_COOLER_BLOCK).addStoryBoard("breeze_cooler_feeding_story_board", BreezeCoolerScenes::feeding, CCBPonderTags.BREEZES_TAG_ID).addStoryBoard("breeze_cooler_basin_story_board", BreezeCoolerScenes::basin, CCBPonderTags.BREEZES_TAG_ID).addStoryBoard("breeze_cooler_bulk_chilling_story_board", BreezeCoolerScenes::bulkChilling, CCBPonderTags.BREEZES_TAG_ID, AllCreatePonderTags.KINETIC_APPLIANCES);
        entryHelper.forComponents(CCBBlocks.BREEZE_CHAMBER_BLOCK).addStoryBoard("breeze_chamber_feeding_story_board", BreezeChamberScenes::feeding, CCBPonderTags.BREEZES_TAG_ID).addStoryBoard("breeze_chamber_processing_story_board", BreezeChamberScenes::processing, CCBPonderTags.BREEZES_TAG_ID).addStoryBoard("airtight_engine_setting_up_story_board", BreezeChamberScenes::assemblyDriver, CCBPonderTags.BREEZES_TAG_ID);

        entryHelper.forComponents(CCBBlocks.AIRTIGHT_ENGINE_BLOCK).addStoryBoard("airtight_engine_setting_up_story_board", AirtightEngineScenes::settingUp, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES).addStoryBoard("airtight_engine_generating_story_board", AirtightEngineScenes::generating, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);
        entryHelper.forComponents(CCBBlocks.RESIDUE_OUTLET_BLOCK).addStoryBoard("residue_outlet_handling_story_board", ResidueOutletScenes::residueOutlet, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.BOILER_STEAM_OUTLET_BLOCK).addStoryBoard("boiler_steam_outlet_story_board", BoilerSteamOutletScenes::scene, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);

        entryHelper.forComponents(CCBBlocks.TESLA_TURBINE_BLOCK).addStoryBoard("tesla_turbine_setting_up_story_board", TeslaTurbineScenes::settingUp, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES).addStoryBoard("tesla_turbine_generating_story_board", TeslaTurbineScenes::generating, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);
        entryHelper.forComponents(CCBBlocks.TESLA_TURBINE_NOZZLE_BLOCK).addStoryBoard("tesla_turbine_setting_up_story_board", TeslaTurbineScenes::settingUp, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBItems.TESLA_TURBINE_ROTOR).addStoryBoard("tesla_turbine_setting_up_story_board", TeslaTurbineScenes::settingUp, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);

        entryHelper.forComponents(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK).addStoryBoard("airtight_reactor_kettle_placement_story_board", AirtightReactorKettleScenes::placement, CCBPonderTags.GAS_MANIPULATORS_TAG_ID).addStoryBoard("airtight_reactor_kettle_processing_story_board", AirtightReactorKettleScenes::processing, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL).addStoryBoard("airtight_fractionation_tower_placement_story_board", AirtightFractionationTowerScenes::placement, CCBPonderTags.GAS_MANIPULATORS_TAG_ID).addStoryBoard("airtight_fractionation_tower_processing_story_board", AirtightFractionationTowerScenes::processing, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK).addStoryBoard("airtight_forging_press_placement_story_board", AirtightForgingPressScenes::placement, CCBPonderTags.GAS_MANIPULATORS_TAG_ID).addStoryBoard("airtight_forging_press_processing_story_board", AirtightForgingPressScenes::processing, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.PORTABLE_GAS_INTERFACE_BLOCK).addStoryBoard("portable_gas_interface_story_board", PortableGasInterfaceScenes::scene, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK).addStoryBoard("gas_injection_chamber_processing_story_board", GasInjectionChamberScenes::processing, CCBPonderTags.GAS_MANIPULATORS_TAG_ID).addStoryBoard("gas_injection_chamber_filter_story_board", GasInjectionChamberScenes::filtering, CCBPonderTags.GAS_MANIPULATORS_TAG_ID).addStoryBoard("gas_injection_chamber_basin_story_board", GasInjectionChamberScenes::basin, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBItems.GAS_INJECTION_CHAMBER_FILTER).addStoryBoard("gas_injection_chamber_filter_story_board", GasInjectionChamberScenes::filtering, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_HATCH_BLOCK).addStoryBoard("airtight_hatch_exchange_story_board", AirtightHatchScenes::exchange, CCBPonderTags.GAS_MANIPULATORS_TAG_ID).addStoryBoard("airtight_hatch_handling_story_board", AirtightHatchScenes::handling, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.GAS_PACKAGER_BLOCK).addStoryBoard("gas_packager_story_board", GasPackagerScenes::scene, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.HIGH_LOGISTICS);
        entryHelper.forComponents(CCBBlocks.GAS_UNPACKAGER_BLOCK).addStoryBoard("gas_packager_story_board", GasPackagerScenes::scene, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.HIGH_LOGISTICS);
        entryHelper.forComponents(CCBBlocks.GAS_REPACKAGER_BLOCK).addStoryBoard("gas_repackager_story_board", GasRepackagerScenes::scene, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.HIGH_LOGISTICS);
        entryHelper.forComponents(CCBBlocks.GAS_FACTORY_GAUGE_BLOCK).addStoryBoard("gas_factory_gauge_restocking_story_board", GasFactoryGaugeScenes::restocking, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.HIGH_LOGISTICS).addStoryBoard("gas_factory_gauge_recipes_story_board", GasFactoryGaugeScenes::recipes, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.HIGH_LOGISTICS).addStoryBoard("gas_factory_gauge_links_story_board", GasFactoryGaugeScenes::links, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.HIGH_LOGISTICS);

        entryHelper.forComponents(CCBBlocks.AIR_VENT_BLOCK).addStoryBoard("air_vent_story_board", AirVentScenes::scene);

        entryHelper.forComponents(CCBBlocks.END_SCULK_SILENCER_BLOCK).addStoryBoard("end_sculk_silencer_story_board", EndSculkSilencerScenes::scene, AllCreatePonderTags.KINETIC_APPLIANCES);
    }

    @Contract("_ -> new")
    public static Vec3 generateItemDropVelocity(RandomSource random) {
        return new Vec3(random.triangle(0, DISTRIBUTION_DEVIATION), random.triangle(0.2, DISTRIBUTION_DEVIATION), random.triangle(0, DISTRIBUTION_DEVIATION));
    }
}
