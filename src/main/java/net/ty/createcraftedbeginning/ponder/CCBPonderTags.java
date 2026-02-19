package net.ty.createcraftedbeginning.ponder;

import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

public class CCBPonderTags {
    public static final ResourceLocation CRATES_TAG_ID = ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "crates");
    public static final ResourceLocation GAS_PIPES_TAG_ID = ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "gas_pipes");
    public static final ResourceLocation GAS_CONTAINERS_TAG_ID = ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "gas_containers");

    public static final ResourceLocation GAS_MANIPULATORS_TAG_ID = ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "gas_manipulators");
    public static final ResourceLocation BREEZES_TAG_ID = ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "breezes");

    public static void register(@NotNull PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(CRATES_TAG_ID).addToIndex().item(CCBBlocks.ANDESITE_CRATE_BLOCK.get()).register();
        helper.registerTag(GAS_PIPES_TAG_ID).addToIndex().item(CCBBlocks.AIRTIGHT_PIPE_BLOCK.get()).register();
        helper.registerTag(GAS_CONTAINERS_TAG_ID).addToIndex().item(CCBBlocks.AIRTIGHT_TANK_BLOCK.get()).register();
        helper.registerTag(BREEZES_TAG_ID).addToIndex().item(CCBBlocks.BREEZE_COOLER_BLOCK.get()).register();
        helper.registerTag(GAS_MANIPULATORS_TAG_ID).addToIndex().item(CCBBlocks.AIRTIGHT_ENGINE_BLOCK.get()).register();

        PonderTagRegistrationHelper<RegistryEntry<?, ?>> entryHelper = helper.withKeyFunction(RegistryEntry::getId);
        entryHelper.addToTag(AllCreatePonderTags.CREATIVE).add(CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK);
        entryHelper.addToTag(AllCreatePonderTags.KINETIC_SOURCES).add(CCBBlocks.AIRTIGHT_ENGINE_BLOCK).add(CCBBlocks.TESLA_TURBINE_BLOCK);
        entryHelper.addToTag(AllCreatePonderTags.KINETIC_APPLIANCES).add(CCBBlocks.AIR_COMPRESSOR_BLOCK).add(CCBBlocks.END_INCINERATION_BLOWER_BLOCK);

        entryHelper.addToTag(CRATES_TAG_ID).add(CCBBlocks.ANDESITE_CRATE_BLOCK).add(CCBBlocks.BRASS_CRATE_BLOCK).add(CCBBlocks.STURDY_CRATE_BLOCK).add(CCBBlocks.CARDBOARD_CRATE_BLOCK);
        entryHelper.addToTag(GAS_PIPES_TAG_ID).add(CCBBlocks.AIRTIGHT_PIPE_BLOCK).add(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK).add(CCBBlocks.AIRTIGHT_CHECK_VALVE_BLOCK).add(CCBBlocks.SMART_AIRTIGHT_PIPE_BLOCK).add(CCBBlocks.AIRTIGHT_PUMP_BLOCK);
        entryHelper.addToTag(GAS_CONTAINERS_TAG_ID).add(CCBBlocks.AIRTIGHT_TANK_BLOCK).add(CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK).add(CCBItems.GAS_CANISTER).add(CCBItems.GAS_CANISTER_PACK);
        entryHelper.addToTag(BREEZES_TAG_ID).add(CCBBlocks.EMPTY_BREEZE_COOLER_BLOCK).add(CCBBlocks.BREEZE_COOLER_BLOCK).add(CCBBlocks.BREEZE_CHAMBER_BLOCK);

        entryHelper.addToTag(GAS_MANIPULATORS_TAG_ID).add(CCBBlocks.AIR_COMPRESSOR_BLOCK).add(CCBBlocks.AIRTIGHT_ENGINE_BLOCK).add(CCBBlocks.RESIDUE_OUTLET_BLOCK).add(CCBBlocks.TESLA_TURBINE_BLOCK).add(CCBBlocks.TESLA_TURBINE_NOZZLE_BLOCK).add(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK).add(CCBBlocks.PORTABLE_GAS_INTERFACE_BLOCK).add(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK).add(CCBBlocks.AIRTIGHT_HATCH_BLOCK);
    }
}