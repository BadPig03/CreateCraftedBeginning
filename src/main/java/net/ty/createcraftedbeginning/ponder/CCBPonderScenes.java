package net.ty.createcraftedbeginning.ponder;

import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.AirCompressorScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.AirtightEngineScenes;
import net.ty.createcraftedbeginning.ponder.scenes.breezes.BreezeChamberScenes;
import net.ty.createcraftedbeginning.ponder.scenes.breezes.BreezeCoolerScenes;
import net.ty.createcraftedbeginning.ponder.scenes.breezes.EmptyBreezeCoolerScenes;
import net.ty.createcraftedbeginning.ponder.scenes.crates.AndesiteCrateScenes;
import net.ty.createcraftedbeginning.ponder.scenes.crates.BrassCrateScenes;
import net.ty.createcraftedbeginning.ponder.scenes.crates.CardboardCrateScenes;
import net.ty.createcraftedbeginning.ponder.scenes.crates.SturdyCrateScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gascontainers.AirtightTankScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gascontainers.CreativeAirtightTankScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.AirtightHatchScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.GasInjectionChamberScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.PortableGasInterfaceScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.ResidueOutletScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators.TeslaTurbineScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.AirtightCheckValveScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.AirtightPipeScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.AirtightPumpScenes;
import net.ty.createcraftedbeginning.ponder.scenes.gaspipes.SmartAirtightPipeScenes;
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
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_CHECK_VALVE_BLOCK).addStoryBoard("airtight_check_valve_transport_story_board", AirtightCheckValveScenes::transport, CCBPonderTags.GAS_PIPES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.SMART_AIRTIGHT_PIPE_BLOCK).addStoryBoard("smart_airtight_pipe_filter_story_board", SmartAirtightPipeScenes::filter, CCBPonderTags.GAS_PIPES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_PUMP_BLOCK).addStoryBoard("airtight_pump_transportation_story_board", AirtightPumpScenes::transport, CCBPonderTags.GAS_PIPES_TAG_ID);

        entryHelper.forComponents(CCBBlocks.AIRTIGHT_TANK_BLOCK).addStoryBoard("airtight_tank_storage_story_board", AirtightTankScenes::storage, CCBPonderTags.GAS_CONTAINERS_TAG_ID).addStoryBoard("airtight_tank_size_story_board", AirtightTankScenes::size, CCBPonderTags.GAS_CONTAINERS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK).addStoryBoard("creative_airtight_tank_storage_story_board", CreativeAirtightTankScenes::storage, CCBPonderTags.GAS_CONTAINERS_TAG_ID).addStoryBoard("creative_airtight_tank_size_story_board", CreativeAirtightTankScenes::size, CCBPonderTags.GAS_CONTAINERS_TAG_ID);

        entryHelper.forComponents(CCBBlocks.EMPTY_BREEZE_COOLER_BLOCK).addStoryBoard("empty_breeze_cooler_using_story_board", EmptyBreezeCoolerScenes::using, CCBPonderTags.BREEZES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.BREEZE_COOLER_BLOCK).addStoryBoard("breeze_cooler_feeding_story_board", BreezeCoolerScenes::feeding, CCBPonderTags.BREEZES_TAG_ID);
        entryHelper.forComponents(CCBBlocks.BREEZE_CHAMBER_BLOCK).addStoryBoard("breeze_chamber_feeding_story_board", BreezeChamberScenes::feeding, CCBPonderTags.BREEZES_TAG_ID);

        entryHelper.forComponents(CCBBlocks.AIR_COMPRESSOR_BLOCK).addStoryBoard("air_compressor_story_board", AirCompressorScenes::gasProcessing, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_APPLIANCES).addStoryBoard("air_compressor_story_board", AirCompressorScenes::overheatManagement, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_APPLIANCES);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_ENGINE_BLOCK).addStoryBoard("airtight_engine_setting_up_story_board", AirtightEngineScenes::settingUp, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES).addStoryBoard("airtight_engine_generating_rotational_force_story_board", AirtightEngineScenes::generatingRotationalForce, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);
        entryHelper.forComponents(CCBBlocks.RESIDUE_OUTLET_BLOCK).addStoryBoard("residue_outlet_expelling_residue_story_board", ResidueOutletScenes::generatingResidue, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.TESLA_TURBINE_BLOCK).addStoryBoard("tesla_turbine_setting_up_story_board", TeslaTurbineScenes::setting_up, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES).addStoryBoard("tesla_turbine_generating_rotational_force_story_board", TeslaTurbineScenes::generating_rotational_force, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);
        entryHelper.forComponents(CCBBlocks.TESLA_TURBINE_NOZZLE_BLOCK).addStoryBoard("tesla_turbine_setting_up_story_board", TeslaTurbineScenes::setting_up, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.PORTABLE_GAS_INTERFACE_BLOCK).addStoryBoard("portable_gas_interface_transfer_story_board", PortableGasInterfaceScenes::transfer, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK).addStoryBoard("gas_injection_chamber_gas_injection_story_board", GasInjectionChamberScenes::gasInjection, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        entryHelper.forComponents(CCBBlocks.AIRTIGHT_HATCH_BLOCK).addStoryBoard("airtight_hatch_transfer_story_board", AirtightHatchScenes::transfer, CCBPonderTags.GAS_MANIPULATORS_TAG_ID);
        /*entryHelper.forComponents(CCBBlocks.PNEUMATIC_ENGINE_BLOCK).addStoryBoard("crates_story_board", PneumaticEngineScenes::scene, CCBPonderTags.KINETIC_SOURCES_TAG_ID, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES).addStoryBoard("crates_story_board", PneumaticEngineScenes::limitation, CCBPonderTags.KINETIC_SOURCES_TAG_ID, CCBPonderTags.GAS_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);
        entryHelper.forComponents(CCBBlocks.PHOTO_STRESS_BEARING_BLOCK).addStoryBoard("crates_story_board", PhotoStressBearingScenes::scene, CCBPonderTags.KINETIC_SOURCES_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES).addStoryBoard("crates_story_board_nether_end", PhotoStressBearingScenes::other_dimension, CCBPonderTags.KINETIC_SOURCES_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);
        entryHelper.forComponents(CCBBlocks.CINDER_INCINERATION_BLOWER_BLOCK).addStoryBoard("crates_story_board_larger", CinderIncinerationBlowerScenes::scene, CCBPonderTags.KINETIC_APPLIANCES_TAG_ID, AllCreatePonderTags.KINETIC_APPLIANCES).addStoryBoard("crates_story_board_larger", CinderIncinerationBlowerScenes::working_range, CCBPonderTags.KINETIC_APPLIANCES_TAG_ID, AllCreatePonderTags.KINETIC_APPLIANCES);*/
    }
}