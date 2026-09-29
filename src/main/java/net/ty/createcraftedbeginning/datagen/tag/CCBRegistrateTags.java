package net.ty.createcraftedbeginning.datagen.tag;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.Tags;
import net.ty.createcraftedbeginning.registry.CCBEntityTypes;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBEntityFlags;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBFluidTags;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrateProvider;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBRegistrateTags {
    private static final CreateRegistrate CCB_REGISTRATE = CCBRegistrateProvider.get();

    public static void addGenerators() {
        CCB_REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, CCBRegistrateTags::genBlockTags);
        CCB_REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, CCBRegistrateTags::genItemTags);
        CCB_REGISTRATE.addDataGenerator(ProviderType.FLUID_TAGS, CCBRegistrateTags::genFluidTags);
        CCB_REGISTRATE.addDataGenerator(ProviderType.ENTITY_TAGS, CCBRegistrateTags::genEntityTags);
    }

    public static void register() {
    }

    private static void genBlockTags(RegistrateTagsProvider<Block> provider) {
        provider.addTag(CCBBlockTags.GAS_SOURCES.tag).addTag(BlockTags.LEAVES);
        Arrays.stream(CCBBlockTags.values()).filter(tag -> tag.alwaysDataGen).map(tag -> tag.tag).forEach(provider::addTag);
    }

    private static void genItemTags(RegistrateTagsProvider<Item> provider) {
        provider.addTag(CCBItemTags.AIRTIGHT_SHEET_ADHESIVES.tag).addTag(Tags.Items.SLIME_BALLS).add(BuiltInRegistries.ITEM.getResourceKey(AllItems.SUPER_GLUE.get()).orElseThrow()).add(BuiltInRegistries.ITEM.getResourceKey(Items.HONEY_BOTTLE).orElseThrow()).addOptional(ResourceLocation.fromNamespaceAndPath("simulated", "honey_glue"));
        provider.addTag(CCBItemTags.END_CASING_RAW_MATERIALS.tag).add(BuiltInRegistries.ITEM.getResourceKey(Blocks.CRYING_OBSIDIAN.asItem()).orElseThrow());
        provider.addTag(CCBItemTags.PRESS_HEAD_TOOLS.tag).add(BuiltInRegistries.ITEM.getResourceKey(Items.HEAVY_CORE).orElseThrow()).add(BuiltInRegistries.ITEM.getResourceKey(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).orElseThrow()).add(BuiltInRegistries.ITEM.getResourceKey(Items.IRON_BARS).orElseThrow()).add(BuiltInRegistries.ITEM.getResourceKey(Items.IRON_TRAPDOOR).orElseThrow());
        provider.addTag(CCBItemTags.WIND_CHARGING_EXCLUDED.tag).add(BuiltInRegistries.ITEM.getResourceKey(Items.OMINOUS_BOTTLE).orElseThrow()).add(BuiltInRegistries.ITEM.getResourceKey(CCBItems.MILK_ICE_CREAM.get()).orElseThrow());
        provider.addTag(ItemTags.PIGLIN_LOVED).add(BuiltInRegistries.ITEM.getResourceKey(CCBItems.GOLDEN_ICE_CREAM.get()).orElseThrow());
        Arrays.stream(CCBItemTags.values()).filter(tag -> tag.alwaysDataGen).map(tag -> tag.tag).forEach(provider::addTag);
    }

    private static void genFluidTags(RegistrateTagsProvider<Fluid> provider) {
        Arrays.stream(CCBFluidTags.values()).filter(tag -> tag.alwaysDataGen).map(tag -> tag.tag).forEach(provider::addTag);
    }

    private static void genEntityTags(RegistrateTagsProvider<EntityType<?>> provider) {
        provider.addTag(EntityTypeTags.IMPACT_PROJECTILES).add(BuiltInRegistries.ENTITY_TYPE.getResourceKey(CCBEntityTypes.AIRTIGHT_CANNON_WIND_CHARGE_PROJECTILE.get()).orElseThrow());
        provider.addTag(CCBEntityFlags.BREEZE_CHAMBER_CAPTURABLE.tag).add(BuiltInRegistries.ENTITY_TYPE.getResourceKey(EntityType.BREEZE).orElseThrow());
        provider.addTag(CCBEntityFlags.IMMUNE_TO_FUNGAL_INFECTION.tag).add(BuiltInRegistries.ENTITY_TYPE.getResourceKey(EntityType.BOGGED).orElseThrow()).add(BuiltInRegistries.ENTITY_TYPE.getResourceKey(EntityType.MOOSHROOM).orElseThrow());
        Arrays.stream(CCBEntityFlags.values()).filter(tag -> tag.alwaysDataGen).map(tag -> tag.tag).forEach(provider::addTag);
    }
}
