package net.ty.createcraftedbeginning.registry;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import net.ty.createcraftedbeginning.provider.CCBTagsProvider;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBEntityFlags;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBFluidTags;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public class CCBRegistrateTags {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate();

    public static void addGenerators() {
        CCB_REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, CCBRegistrateTags::genBlockTags);
        CCB_REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, CCBRegistrateTags::genItemTags);
        CCB_REGISTRATE.addDataGenerator(ProviderType.FLUID_TAGS, CCBRegistrateTags::genFluidTags);
        CCB_REGISTRATE.addDataGenerator(ProviderType.ENTITY_TAGS, CCBRegistrateTags::genEntityTags);
    }

    private static void genBlockTags(RegistrateTagsProvider<Block> provIn) {
        CCBTagsProvider<Block> provider = new CCBTagsProvider<>(provIn, Block::builtInRegistryHolder);
        provider.tag(CCBBlockTags.GAS_SOURCES.tag).addTag(BlockTags.LEAVES);
        Arrays.stream(CCBBlockTags.values()).filter(tag -> tag.alwaysDataGen).map(tag -> tag.tag).forEach(provider::getOrCreateRawBuilder);
    }

    private static void genItemTags(RegistrateTagsProvider<Item> provIn) {
        CCBTagsProvider<Item> provider = new CCBTagsProvider<>(provIn, Item::builtInRegistryHolder);
        provider.tag(CCBItemTags.END_CASING_RAW_MATERIALS.tag).add(Blocks.CRYING_OBSIDIAN.asItem());
        provider.tag(CCBItemTags.PRESS_HEAD_TOOLS.tag).add(Items.HEAVY_CORE).add(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        provider.tag(ItemTags.PIGLIN_LOVED).add(CCBItems.GOLDEN_ICE_CREAM.get());
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
