package net.ty.createcraftedbeginning.ponder;

import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.ponder.scenes.AirCompressorScenes;
import net.ty.createcraftedbeginning.ponder.scenes.AirtightEncasedPipeScenes;
import net.ty.createcraftedbeginning.ponder.scenes.AirtightEngineScenes;
import net.ty.createcraftedbeginning.ponder.scenes.AirtightIntakePortScenes;
import net.ty.createcraftedbeginning.ponder.scenes.AirtightPipeScenes;
import net.ty.createcraftedbeginning.ponder.scenes.AirtightPumpScenes;
import net.ty.createcraftedbeginning.ponder.scenes.AirtightTankScenes;
import net.ty.createcraftedbeginning.ponder.scenes.AndesiteCrateScenes;
import net.ty.createcraftedbeginning.ponder.scenes.BrassCrateScenes;
import net.ty.createcraftedbeginning.ponder.scenes.BreezeChamberScenes;
import net.ty.createcraftedbeginning.ponder.scenes.CardboardCrateScenes;
import net.ty.createcraftedbeginning.ponder.scenes.CinderIncinerationBlowerScenes;
import net.ty.createcraftedbeginning.ponder.scenes.CompressedAirScenes;
import net.ty.createcraftedbeginning.ponder.scenes.CondensateDrainScenes;
import net.ty.createcraftedbeginning.ponder.scenes.EmptyBreezeChamberScenes;
import net.ty.createcraftedbeginning.ponder.scenes.GasInjectionChamberScenes;
import net.ty.createcraftedbeginning.ponder.scenes.PhotoStressBearingScenes;
import net.ty.createcraftedbeginning.ponder.scenes.PneumaticEngineScenes;
import net.ty.createcraftedbeginning.ponder.scenes.SturdyCrateScenes;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

public class CCBPonderScenes {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<RegistryEntry<?, ?>> ENTRY_HELPER = helper.withKeyFunction(RegistryEntry::getId);

        ENTRY_HELPER.forComponents(CCBBlocks.ANDESITE_CRATE_BLOCK).addStoryBoard("crates_story_board", AndesiteCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);
        ENTRY_HELPER.forComponents(CCBBlocks.BRASS_CRATE_BLOCK).addStoryBoard("crates_story_board", BrassCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);
        ENTRY_HELPER.forComponents(CCBBlocks.STURDY_CRATE_BLOCK).addStoryBoard("crates_story_board", SturdyCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);
        ENTRY_HELPER.forComponents(CCBBlocks.CARDBOARD_CRATE_BLOCK).addStoryBoard("crates_story_board", CardboardCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);

        ENTRY_HELPER.forComponents(CCBBlocks.PNEUMATIC_ENGINE_BLOCK).addStoryBoard("crates_story_board", PneumaticEngineScenes::scene, CCBPonderTags.KINETIC_SOURCES_TAG_ID, CCBPonderTags.COMPRESSED_AIR_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES).addStoryBoard("crates_story_board", PneumaticEngineScenes::limitation, CCBPonderTags.KINETIC_SOURCES_TAG_ID, CCBPonderTags.COMPRESSED_AIR_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);
        ENTRY_HELPER.forComponents(CCBBlocks.PHOTO_STRESS_BEARING_BLOCK).addStoryBoard("crates_story_board", PhotoStressBearingScenes::scene, CCBPonderTags.KINETIC_SOURCES_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES).addStoryBoard("crates_story_board_nether_end", PhotoStressBearingScenes::other_dimension, CCBPonderTags.KINETIC_SOURCES_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);

        ENTRY_HELPER.forComponents(CCBBlocks.CINDER_INCINERATION_BLOWER_BLOCK).addStoryBoard("crates_story_board_larger", CinderIncinerationBlowerScenes::scene, CCBPonderTags.KINETIC_APPLIANCES_TAG_ID, AllCreatePonderTags.KINETIC_APPLIANCES).addStoryBoard("crates_story_board_larger", CinderIncinerationBlowerScenes::working_range, CCBPonderTags.KINETIC_APPLIANCES_TAG_ID, AllCreatePonderTags.KINETIC_APPLIANCES);

        ENTRY_HELPER.forComponents(CCBBlocks.AIRTIGHT_PIPE_BLOCK).addStoryBoard("crates_story_board_compressed_air", CompressedAirScenes::scene, CCBPonderTags.COMPRESSED_AIR_MANIPULATORS_TAG_ID);

        ENTRY_HELPER.forComponents(CCBBlocks.AIRTIGHT_PIPE_BLOCK).addStoryBoard("crates_story_board_compressed_air", AirtightPipeScenes::scene, CCBPonderTags.COMPRESSED_AIR_MANIPULATORS_TAG_ID);
        ENTRY_HELPER.forComponents(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK).addStoryBoard("crates_story_board_airtight_encased_pipe", AirtightEncasedPipeScenes::scene, CCBPonderTags.COMPRESSED_AIR_MANIPULATORS_TAG_ID);
        ENTRY_HELPER.forComponents(CCBBlocks.AIRTIGHT_PUMP_BLOCK).addStoryBoard("crates_story_board_airtight_tank", AirtightPumpScenes::scene, CCBPonderTags.COMPRESSED_AIR_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);
        ENTRY_HELPER.forComponents(CCBBlocks.AIRTIGHT_TANK_BLOCK).addStoryBoard("crates_story_board_airtight_tank", AirtightTankScenes::scene, CCBPonderTags.COMPRESSED_AIR_MANIPULATORS_TAG_ID).addStoryBoard("crates_story_board_airtight_tank_max", AirtightTankScenes::max, CCBPonderTags.COMPRESSED_AIR_MANIPULATORS_TAG_ID);
        ENTRY_HELPER.forComponents(CCBBlocks.AIRTIGHT_INTAKE_PORT_BLOCK).addStoryBoard("crates_story_board_larger", AirtightIntakePortScenes::scene, CCBPonderTags.COMPRESSED_AIR_MANIPULATORS_TAG_ID);
        ENTRY_HELPER.forComponents(CCBBlocks.AIR_COMPRESSOR_BLOCK).addStoryBoard("crates_story_board", AirCompressorScenes::scene, CCBPonderTags.COMPRESSED_AIR_MANIPULATORS_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);
        ENTRY_HELPER.forComponents(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK).addStoryBoard("crates_story_board_gas_injection_chamber", GasInjectionChamberScenes::scene, CCBPonderTags.COMPRESSED_AIR_MANIPULATORS_TAG_ID);
        ENTRY_HELPER.forComponents(CCBBlocks.AIRTIGHT_ENGINE_BLOCK).addStoryBoard("crates_story_board_larger_airtight_engine", AirtightEngineScenes::scene, CCBPonderTags.COMPRESSED_AIR_MANIPULATORS_TAG_ID);
        ENTRY_HELPER.forComponents(CCBBlocks.CONDENSATE_DRAIN_BLOCK).addStoryBoard("crates_story_board_larger", CondensateDrainScenes::scene, CCBPonderTags.COMPRESSED_AIR_MANIPULATORS_TAG_ID);

        ENTRY_HELPER.forComponents(CCBBlocks.EMPTY_BREEZE_CHAMBER_BLOCK).addStoryBoard("crates_story_board_empty_breeze_chamber", EmptyBreezeChamberScenes::scene);
        ENTRY_HELPER.forComponents(CCBBlocks.BREEZE_CHAMBER_BLOCK).addStoryBoard("crates_story_board_breeze_chamber", BreezeChamberScenes::scene);
    }
}