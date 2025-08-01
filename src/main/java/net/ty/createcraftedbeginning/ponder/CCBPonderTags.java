package net.ty.createcraftedbeginning.ponder;

import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

public class CCBPonderTags {
    public static final ResourceLocation CRATES_TAG_ID = ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "crates");
    public static final ResourceLocation KINETIC_SOURCES_TAG_ID = ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "kinetic_sources");

    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(CRATES_TAG_ID).addToIndex().item(CCBBlocks.ANDESITE_CRATE_BLOCK.get()).register();
        helper.registerTag(KINETIC_SOURCES_TAG_ID).addToIndex().item(CCBBlocks.PNEUMATIC_ENGINE_BLOCK.get()).register();

        PonderTagRegistrationHelper<RegistryEntry<?, ?>> ENTRY_HELPER = helper.withKeyFunction(RegistryEntry::getId);
        ENTRY_HELPER.addToTag(CRATES_TAG_ID).add(CCBBlocks.ANDESITE_CRATE_BLOCK);
        ENTRY_HELPER.addToTag(CRATES_TAG_ID).add(CCBBlocks.BRASS_CRATE_BLOCK);
        ENTRY_HELPER.addToTag(CRATES_TAG_ID).add(CCBBlocks.STURDY_CRATE_BLOCK);
        ENTRY_HELPER.addToTag(CRATES_TAG_ID).add(CCBBlocks.CARDBOARD_CRATE_BLOCK);
        ENTRY_HELPER.addToTag(KINETIC_SOURCES_TAG_ID).add(CCBBlocks.PNEUMATIC_ENGINE_BLOCK);
        ENTRY_HELPER.addToTag(AllCreatePonderTags.KINETIC_SOURCES).add(CCBBlocks.PNEUMATIC_ENGINE_BLOCK);
        ENTRY_HELPER.addToTag(KINETIC_SOURCES_TAG_ID).add(CCBBlocks.PHOTO_STRESS_BEARING_BLOCK);
        ENTRY_HELPER.addToTag(AllCreatePonderTags.KINETIC_SOURCES).add(CCBBlocks.PHOTO_STRESS_BEARING_BLOCK);
    }
}