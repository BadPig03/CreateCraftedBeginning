package net.ty.createcraftedbeginning.data;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import net.ty.createcraftedbeginning.data.CCBTagGen.CCBTagsProvider;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;

@SuppressWarnings("deprecation")
public class CCBRegistrateTags {
    private static final CreateRegistrate CREATE_REGISTRATE = CreateCraftedBeginning.registrate();

    public static void addGenerators() {
        CREATE_REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, CCBRegistrateTags::genBlockTags);
        CREATE_REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, CCBRegistrateTags::genItemTags);
    }

    private static void genBlockTags(RegistrateTagsProvider<Block> provIn) {
        CCBTagsProvider<Block> prov = new CCBTagsProvider<>(provIn, Block::builtInRegistryHolder);

        for (CCBBlockTags tag : CCBBlockTags.values()) {
            if (tag.alwaysDataGen) {
                prov.getOrCreateRawBuilder(tag.tag);
            }
        }
    }

    private static void genItemTags(RegistrateTagsProvider<Item> provIn) {
        CCBTagsProvider<Item> prov = new CCBTagsProvider<>(provIn, Item::builtInRegistryHolder);

        prov.tag(CCBItemTags.CINDER_CASING_RAW_MATERIALS.tag)
            .add(Blocks.STRIPPED_CRIMSON_STEM.asItem())
            .add(Blocks.STRIPPED_CRIMSON_HYPHAE.asItem())
            .add(Blocks.STRIPPED_WARPED_STEM.asItem())
            .add(Blocks.STRIPPED_WARPED_HYPHAE.asItem());

        prov.tag(Tags.Items.INGOTS).addTag(CCBItemTags.CINDER_ALLOY.tag);

        for (CCBItemTags tag : CCBItemTags.values()) {
            if (tag.alwaysDataGen) {
                prov.getOrCreateRawBuilder(tag.tag);
            }
        }
    }

    public static void register() {}
}
