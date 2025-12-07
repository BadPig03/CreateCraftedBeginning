package net.ty.createcraftedbeginning.registry;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.Tags.Items;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.compat.Mods;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import net.ty.createcraftedbeginning.provider.CCBTagsProvider;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBEntityFlags;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBFluidTags;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;

import java.util.Arrays;

@SuppressWarnings("deprecation")
public class CCBRegistrateTags {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate();

    private static final ResourceLocation DRAGONS_PLUS_FREEZERS = Mods.DRAGONS_PLUS.asResource("passive_block_freezers");

    public static void addGenerators() {
        CCB_REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, CCBRegistrateTags::genBlockTags);
        CCB_REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, CCBRegistrateTags::genItemTags);
        CCB_REGISTRATE.addDataGenerator(ProviderType.FLUID_TAGS, CCBRegistrateTags::genFluidTags);
        CCB_REGISTRATE.addDataGenerator(ProviderType.ENTITY_TAGS, CCBRegistrateTags::genEntityTags);
    }

    private static void genBlockTags(RegistrateTagsProvider<Block> provIn) {
        CCBTagsProvider<Block> provider = new CCBTagsProvider<>(provIn, Block::builtInRegistryHolder);
        provider.tag(BlockTags.create(DRAGONS_PLUS_FREEZERS)).add(CCBBlocks.BREEZE_COOLER_BLOCK.get());
        Arrays.stream(CCBBlockTags.values()).filter(tag -> tag.alwaysDataGen).map(tag -> tag.tag).forEach(provider::getOrCreateRawBuilder);
    }

    private static void genItemTags(RegistrateTagsProvider<Item> provIn) {
        CCBTagsProvider<Item> provider = new CCBTagsProvider<>(provIn, Item::builtInRegistryHolder);
        provider.tag(CCBItemTags.CINDER_CASING_RAW_MATERIALS.tag).add(Blocks.STRIPPED_CRIMSON_STEM.asItem()).add(Blocks.STRIPPED_CRIMSON_HYPHAE.asItem()).add(Blocks.STRIPPED_WARPED_STEM.asItem()).add(Blocks.STRIPPED_WARPED_HYPHAE.asItem());
        provider.tag(Items.INGOTS).addTag(CCBItemTags.CINDER_ALLOY.tag);
        Arrays.stream(CCBItemTags.values()).filter(tag -> tag.alwaysDataGen).map(tag -> tag.tag).forEach(provider::getOrCreateRawBuilder);
    }

    private static void genFluidTags(RegistrateTagsProvider<Fluid> provIn) {
        CCBTagsProvider<Fluid> provider = new CCBTagsProvider<>(provIn, Fluid::builtInRegistryHolder);
        Arrays.stream(CCBFluidTags.values()).filter(tag -> tag.alwaysDataGen).map(tag -> tag.tag).forEach(provider::getOrCreateRawBuilder);
    }

    private static void genEntityTags(RegistrateTagsProvider<EntityType<?>> provIn) {
        CCBTagsProvider<EntityType<?>> provider = new CCBTagsProvider<>(provIn, EntityType::builtInRegistryHolder);
        provider.tag(CCBEntityFlags.BREEZE_CHAMBER_CAPTURABLE.tag).add(EntityType.BREEZE);
        Arrays.stream(CCBEntityFlags.values()).filter(tag -> tag.alwaysDataGen).map(tag -> tag.tag).forEach(provider::getOrCreateRawBuilder);
    }

    public static void register() {
    }
}
