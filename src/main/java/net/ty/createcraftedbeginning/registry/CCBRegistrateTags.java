package net.ty.createcraftedbeginning.registry;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.Tags;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.compat.Mods;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import net.ty.createcraftedbeginning.data.CCBTagGen.CCBTagsProvider;
import net.ty.createcraftedbeginning.data.CCBTags;
import net.ty.createcraftedbeginning.data.CCBTags.CCBBlockTags;
import net.ty.createcraftedbeginning.data.CCBTags.CCBEntityFlags;
import net.ty.createcraftedbeginning.data.CCBTags.CCBFluidTags;
import net.ty.createcraftedbeginning.data.CCBTags.CCBItemTags;

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
        CCBTagsProvider<Block> prov = new CCBTagsProvider<>(provIn, Block::builtInRegistryHolder);

        prov.tag(BlockTags.create(Mods.DRAGONS_PLUS.asResource("passive_block_freezers"))).add(CCBBlocks.BREEZE_CHAMBER_BLOCK.get());

        for (CCBBlockTags tag : CCBBlockTags.values()) {
            if (tag.alwaysDataGen) {
                prov.getOrCreateRawBuilder(tag.tag);
            }
        }
    }

    private static void genItemTags(RegistrateTagsProvider<Item> provIn) {
        CCBTagsProvider<Item> prov = new CCBTagsProvider<>(provIn, Item::builtInRegistryHolder);

        prov.tag(CCBItemTags.CINDER_CASING_RAW_MATERIALS.tag).add(Blocks.STRIPPED_CRIMSON_STEM.asItem()).add(Blocks.STRIPPED_CRIMSON_HYPHAE.asItem()).add(Blocks.STRIPPED_WARPED_STEM.asItem()).add(Blocks.STRIPPED_WARPED_HYPHAE.asItem());

        prov.tag(Tags.Items.INGOTS).addTag(CCBItemTags.CINDER_ALLOY.tag);

        for (CCBItemTags tag : CCBItemTags.values()) {
            if (tag.alwaysDataGen) {
                prov.getOrCreateRawBuilder(tag.tag);
            }
        }
    }

    private static void genFluidTags(RegistrateTagsProvider<Fluid> provIn) {
        CCBTagsProvider<Fluid> prov = new CCBTagsProvider<>(provIn, Fluid::builtInRegistryHolder);

        for (CCBFluidTags tag : CCBFluidTags.values()) {
            if (tag.alwaysDataGen) {
                prov.getOrCreateRawBuilder(tag.tag);
            }
        }
    }

    private static void genEntityTags(RegistrateTagsProvider<EntityType<?>> provIn) {
        CCBTagsProvider<EntityType<?>> prov = new CCBTagsProvider<>(provIn, EntityType::builtInRegistryHolder);

        prov.tag(CCBTags.CCBEntityFlags.BREEZE_CHAMBER_CAPTURABLE.tag).add(EntityType.BREEZE);

        for (CCBEntityFlags tag : CCBEntityFlags.values()) {
            if (tag.alwaysDataGen) {
                prov.getOrCreateRawBuilder(tag.tag);
            }
        }
    }

    public static void register() {
    }
}
