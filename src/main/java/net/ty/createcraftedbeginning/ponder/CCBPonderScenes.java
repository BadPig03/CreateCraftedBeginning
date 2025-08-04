package net.ty.createcraftedbeginning.ponder;

import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.ponder.scenes.*;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

public class CCBPonderScenes {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<RegistryEntry<?, ?>> ENTRY_HELPER = helper.withKeyFunction(RegistryEntry::getId);

        ENTRY_HELPER.forComponents(CCBBlocks.ANDESITE_CRATE_BLOCK).addStoryBoard("crates_story_board", AndesiteCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);
        ENTRY_HELPER.forComponents(CCBBlocks.BRASS_CRATE_BLOCK).addStoryBoard("crates_story_board", BrassCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);
        ENTRY_HELPER.forComponents(CCBBlocks.STURDY_CRATE_BLOCK).addStoryBoard("crates_story_board", SturdyCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);
        ENTRY_HELPER.forComponents(CCBBlocks.CARDBOARD_CRATE_BLOCK).addStoryBoard("crates_story_board", CardboardCrateScenes::scene, CCBPonderTags.CRATES_TAG_ID);

        ENTRY_HELPER.forComponents(CCBBlocks.PNEUMATIC_ENGINE_BLOCK).addStoryBoard("crates_story_board", PneumaticEngineScenes::scene, CCBPonderTags.KINETIC_SOURCES_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);
        ENTRY_HELPER.forComponents(CCBBlocks.PNEUMATIC_ENGINE_BLOCK).addStoryBoard("crates_story_board", PneumaticEngineScenes::limitation, CCBPonderTags.KINETIC_SOURCES_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);
        ENTRY_HELPER.forComponents(CCBBlocks.PHOTO_STRESS_BEARING_BLOCK).addStoryBoard("crates_story_board", PhotoStressBearingScenes::scene, CCBPonderTags.KINETIC_SOURCES_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);
        ENTRY_HELPER.forComponents(CCBBlocks.PHOTO_STRESS_BEARING_BLOCK).addStoryBoard("crates_story_board_nether_end", PhotoStressBearingScenes::other_dimension, CCBPonderTags.KINETIC_SOURCES_TAG_ID, AllCreatePonderTags.KINETIC_SOURCES);

        ENTRY_HELPER.forComponents(CCBBlocks.CINDER_NOZZLE_BLOCK).addStoryBoard("crates_story_board_larger", CinderNozzleScenes::scene, CCBPonderTags.KINETIC_APPLIANCES_TAG_ID, AllCreatePonderTags.KINETIC_APPLIANCES);
        ENTRY_HELPER.forComponents(CCBBlocks.CINDER_NOZZLE_BLOCK).addStoryBoard("crates_story_board_larger", CinderNozzleScenes::working_range, CCBPonderTags.KINETIC_APPLIANCES_TAG_ID, AllCreatePonderTags.KINETIC_APPLIANCES);
    }
}