package net.ty.createcraftedbeginning.ponder;

import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.ponder.scenes.breezes.BreezeChamberScenes;
import net.ty.createcraftedbeginning.ponder.scenes.breezes.BreezeCoolerScenes;
import net.ty.createcraftedbeginning.ponder.scenes.breezes.EmptyBreezeCoolerScenes;
import net.ty.createcraftedbeginning.ponder.scenes.cinder.CinderIncinerationBlowerScenes;
import net.ty.createcraftedbeginning.ponder.scenes.crates.AndesiteCrateScenes;
import net.ty.createcraftedbeginning.ponder.scenes.crates.BrassCrateScenes;
import net.ty.createcraftedbeginning.ponder.scenes.crates.CardboardCrateScenes;
import net.ty.createcraftedbeginning.ponder.scenes.crates.SturdyCrateScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gascontainers.AirtightTankScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gascontainers.CreativeAirtightTankScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.AirCompressorScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.AirtightEngineScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.AirtightHatchScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.AirtightReactorKettleScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.GasInjectionChamberScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.PortableGasInterfaceScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.ResidueOutletScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.TeslaTurbineScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.AirtightCheckValveScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.AirtightPipeScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.AirtightPumpScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.SmartAirtightPipeScenes;
import net.ty.createcraftedbeginning.ponder.scenes.other.AirVentScenes;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public class CCBPonderScenes {
    public static void register(@NotNull PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<RegistryEntry<?, ?>> entryHelper = helper.withKeyFunction(RegistryEntry::getId);
        entryHelper.forComponents(CCBBlocks.ANDESITE_CRATE_BLOCK).addStoryBoard("crates_story_board", AndesiteCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.BRASS_CRATE_BLOCK).addStoryBoard("crates_story_board", BrassCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.STURDY_CRATE_BLOCK).addStoryBoard("crates_story_board", SturdyCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.CARDBOARD_CRATE_BLOCK).addStoryBoard("crates_story_board", CardboardCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);

        entryHelper.forComponents(CCBBlocks.AIRTIGHT_PIPE_BLOCK).addStoryBoard("airtight_pipe_moving_story_board", AirtightPipeScenes::moving, CCBPonderTags.GAS_PIPES_TAG_ID).addStoryBoard("airtight_pipe_interaction_story_board", AirtightPipeScenes::interaction, CCBPonderTags.GAS_PIPES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_CHECK_VALVE_BLOCK).addStoryBoard("airtight_check_valve_story_board", AirtightCheckValveScenes::scene, CCBPonderTags.GAS_PIPES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.SMART_AIRTIGHT_PIPE_BLOCK).addStoryBoard("smart_airtight_pipe_story_board", SmartAirtightPipeScenes::scene, CCBPonderTags.GAS_PIPES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_PUMP_BLOCK).addStoryBoard("airtight_pump_story_board", AirtightPumpScenes::scene, CCBPonderTags.GAS_PIPES_TAG_ID);

        entryHelper.forComponents(CCBBlocks.AIRTIGHT_TANK_BLOCK).addStoryBoard("airtight_tank_storage_story_board", AirtightTankScenes::storage, CCBPonderTags.GAS_CONTAINERS_TAG_ID).addStoryBoard("airtight_tank_size_story_board", AirtightTankScenes::size, CCBPonderTags.GAS_CONTAINERS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK).addStoryBoard("creative_airtight_tank_storage_story_board", CreativeAirtightTankScenes::storage, CCBPonderTags.GAS_CONTAINERS_TAG_ID).addStoryBoard("creative_airtight_tank_size_story_board", CreativeAirtightTankScenes::size, CCBPonderTags.GAS_CONTAINERS_TAG_ID);

        entryHelper.forComponents(CCBBlocks.EMPTY_BREEZE_COOLER_BLOCK).addStoryBoard("empty_breeze_cooler_story_board", EmptyBreezeCoolerScenes::scene, CCBPonderTags.BREEZES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.BREEZE_COOLER_BLOCK).addStoryBoard("breeze_cooler_story_board", BreezeCoolerScenes::scene, CCBPonderTags.BREEZES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.BREEZE_CHAMBER_BLOCK).addStoryBoard("breeze_chamber_story_board", BreezeChamberScenes::scene, CCBPonderTags.BREEZES_TAG_ID);

        entryHelper.forComponents(CCBBlocks.AIR_COMPRESSOR_BLOCK).addStoryBoard("air_compressor_story_board", AirCompressorScenes::gasProcessing, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_APPLIANCES).addStoryBoard("air_compressor_story_board", AirCompressorScenes::overheatManagement, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_APPLIANCES);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_ENGINE_BLOCK).addStoryBoard("airtight_engine_setting_up_story_board", AirtightEngineScenes::settingUp, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES).addStoryBoard("airtight_engine_generating_story_board", AirtightEngineScenes::generating, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);
        entryHelper.forComponents(CCBBlocks.RESIDUE_OUTLET_BLOCK).addStoryBoard("residue_outlet_story_board", ResidueOutletScenes::scene, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.TESLA_TURBINE_BLOCK).addStoryBoard("tesla_turbine_setting_up_story_board", TeslaTurbineScenes::settingUp, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES).addStoryBoard("tesla_turbine_generating_story_board", TeslaTurbineScenes::generating, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);
        entryHelper.forComponents(CCBBlocks.TESLA_TURBINE_NOZZLE_BLOCK).addStoryBoard("tesla_turbine_setting_up_story_board", TeslaTurbineScenes::settingUp, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK).addStoryBoard("airtight_reactor_kettle_placement_story_board", AirtightReactorKettleScenes::placement, CCBPonderTags.GAS_MANIPULATORS_TAG_ID).addStoryBoard("airtight_reactor_kettle_processing_story_board", AirtightReactorKettleScenes::processing, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.PORTABLE_GAS_INTERFACE_BLOCK).addStoryBoard("portable_gas_interface_story_board", PortableGasInterfaceScenes::scene, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK).addStoryBoard("gas_injection_chamber_story_board", GasInjectionChamberScenes::scene, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_HATCH_BLOCK).addStoryBoard("airtight_hatch_story_board", AirtightHatchScenes::scene, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);

        entryHelper.forComponents(CCBBlocks.AIR_VENT_BLOCK).addStoryBoard("air_vent_story_board", AirVentScenes::scene);

        entryHelper.forComponents(CCBBlocks.CINDER_INCINERATION_BLOWER_BLOCK).addStoryBoard("cinder_incineration_blower_story_board", CinderIncinerationBlowerScenes::scene, AllCreatePonderTags.KINETIC_APPLIANCES);
    }
}