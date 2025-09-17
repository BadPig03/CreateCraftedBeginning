package net.ty.createcraftedbeginning.ponder;

import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public class CCBPonderTags {
    public static final ResourceLocation CRATES_TAG_ID = ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "crates");
    public static final ResourceLocation KINETIC_SOURCES_TAG_ID = ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "kinetic_sources");
    public static final ResourceLocation KINETIC_APPLIANCES_TAG_ID = ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "kinetic_appliances");
    public static final ResourceLocation COMPRESSED_AIR_MANIPULATORS_TAG_ID = ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "compressed_air_manipulators");
    public static final ResourceLocation BREEZE_COOLERS_TAG_ID = ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "breeze_coolers");

    public static void register(@NotNull PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(CRATES_TAG_ID).addToIndex().item(CCBBlocks.ANDESITE_CRATE_BLOCK.get()).register();
        helper.registerTag(KINETIC_SOURCES_TAG_ID).addToIndex().item(CCBBlocks.PNEUMATIC_ENGINE_BLOCK.get()).register();
        helper.registerTag(KINETIC_APPLIANCES_TAG_ID).addToIndex().item(CCBBlocks.CINDER_INCINERATION_BLOWER_BLOCK.get()).register();
        helper.registerTag(COMPRESSED_AIR_MANIPULATORS_TAG_ID).addToIndex().item(CCBBlocks.AIRTIGHT_PIPE_BLOCK.get()).register();
        helper.registerTag(BREEZE_COOLERS_TAG_ID).addToIndex().item(CCBBlocks.BREEZE_COOLER_BLOCK.get()).register();

        PonderTagRegistrationHelper<RegistryEntry<?, ?>> ENTRY_HELPER = helper.withKeyFunction(RegistryEntry::getId);
        ENTRY_HELPER.addToTag(CRATES_TAG_ID).add(CCBBlocks.ANDESITE_CRATE_BLOCK);
        ENTRY_HELPER.addToTag(CRATES_TAG_ID).add(CCBBlocks.BRASS_CRATE_BLOCK);
        ENTRY_HELPER.addToTag(CRATES_TAG_ID).add(CCBBlocks.STURDY_CRATE_BLOCK);
        ENTRY_HELPER.addToTag(CRATES_TAG_ID).add(CCBBlocks.CARDBOARD_CRATE_BLOCK);

        ENTRY_HELPER.addToTag(KINETIC_SOURCES_TAG_ID).add(CCBBlocks.PNEUMATIC_ENGINE_BLOCK);
        ENTRY_HELPER.addToTag(AllCreatePonderTags.KINETIC_SOURCES).add(CCBBlocks.PNEUMATIC_ENGINE_BLOCK);
        ENTRY_HELPER.addToTag(KINETIC_SOURCES_TAG_ID).add(CCBBlocks.PHOTO_STRESS_BEARING_BLOCK);
        ENTRY_HELPER.addToTag(AllCreatePonderTags.KINETIC_SOURCES).add(CCBBlocks.PHOTO_STRESS_BEARING_BLOCK);

        ENTRY_HELPER.addToTag(KINETIC_APPLIANCES_TAG_ID).add(CCBBlocks.CINDER_INCINERATION_BLOWER_BLOCK);
        ENTRY_HELPER.addToTag(AllCreatePonderTags.KINETIC_APPLIANCES).add(CCBBlocks.CINDER_INCINERATION_BLOWER_BLOCK);
        ENTRY_HELPER.addToTag(KINETIC_APPLIANCES_TAG_ID).add(CCBBlocks.AIR_COMPRESSOR_BLOCK);
        ENTRY_HELPER.addToTag(AllCreatePonderTags.KINETIC_APPLIANCES).add(CCBBlocks.AIR_COMPRESSOR_BLOCK);

        ENTRY_HELPER.addToTag(COMPRESSED_AIR_MANIPULATORS_TAG_ID).add(CCBBlocks.PNEUMATIC_ENGINE_BLOCK);
        ENTRY_HELPER.addToTag(COMPRESSED_AIR_MANIPULATORS_TAG_ID).add(CCBBlocks.AIRTIGHT_PIPE_BLOCK);
        ENTRY_HELPER.addToTag(COMPRESSED_AIR_MANIPULATORS_TAG_ID).add(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK);
        ENTRY_HELPER.addToTag(COMPRESSED_AIR_MANIPULATORS_TAG_ID).add(CCBBlocks.AIRTIGHT_PUMP_BLOCK);
        ENTRY_HELPER.addToTag(COMPRESSED_AIR_MANIPULATORS_TAG_ID).add(CCBBlocks.AIRTIGHT_TANK_BLOCK);
        ENTRY_HELPER.addToTag(COMPRESSED_AIR_MANIPULATORS_TAG_ID).add(CCBBlocks.AIRTIGHT_INTAKE_PORT_BLOCK);
        ENTRY_HELPER.addToTag(COMPRESSED_AIR_MANIPULATORS_TAG_ID).add(CCBBlocks.AIR_COMPRESSOR_BLOCK);
        ENTRY_HELPER.addToTag(COMPRESSED_AIR_MANIPULATORS_TAG_ID).add(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK);
        ENTRY_HELPER.addToTag(COMPRESSED_AIR_MANIPULATORS_TAG_ID).add(CCBBlocks.AIRTIGHT_ENGINE_BLOCK);
        ENTRY_HELPER.addToTag(COMPRESSED_AIR_MANIPULATORS_TAG_ID).add(CCBBlocks.CONDENSATE_DRAIN_BLOCK);

        ENTRY_HELPER.addToTag(AllCreatePonderTags.ARM_TARGETS).add(CCBBlocks.BREEZE_COOLER_BLOCK);

        ENTRY_HELPER.addToTag(BREEZE_COOLERS_TAG_ID).add(CCBBlocks.EMPTY_BREEZE_COOLER_BLOCK);
        ENTRY_HELPER.addToTag(BREEZE_COOLERS_TAG_ID).add(CCBBlocks.BREEZE_COOLER_BLOCK);
    }
}