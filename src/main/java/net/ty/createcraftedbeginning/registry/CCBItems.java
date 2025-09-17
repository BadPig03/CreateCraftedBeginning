package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.Tags;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasCanisterItem;
import net.ty.createcraftedbeginning.content.airtightcannon.AirtightCannonItem;
import net.ty.createcraftedbeginning.content.compressedair.CompressedAirCanisterItem;
import net.ty.createcraftedbeginning.content.icecreams.AmethystIceCreamItem;
import net.ty.createcraftedbeginning.content.icecreams.CreativeIceCreamItem;
import net.ty.createcraftedbeginning.content.icecreams.HoneyIceCreamItem;
import net.ty.createcraftedbeginning.content.icecreams.MilkIceCreamItem;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import net.ty.createcraftedbeginning.data.CCBTags;
import net.ty.createcraftedbeginning.data.CCBTags.CCBItemTags;

@SuppressWarnings("unused")
public class CCBItems {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate();

    public static final ItemEntry<Item> AIRTIGHT_SHEET = CCB_REGISTRATE.item("airtight_sheet", Item::new).properties(p -> p.stacksTo(64).fireResistant()).register();
    public static final ItemEntry<AirtightCannonItem> AIRTIGHT_CANNON = CCB_REGISTRATE.item("airtight_cannon", AirtightCannonItem::new).properties(p -> p.durability(256).fireResistant()).model(AssetLookup.itemModelWithPartials()).tag(Tags.Items.ENCHANTABLES, ItemTags.DURABILITY_ENCHANTABLE, ItemTags.BOW_ENCHANTABLE).register();
    public static final ItemEntry<? extends GasCanisterItem> GAS_CANISTER = CCB_REGISTRATE.item("gas_canister", GasCanisterItem::new).properties(p -> p.stacksTo(1).fireResistant()).register();

    public static final ItemEntry<Item> CINDER_ALLOY = CCB_REGISTRATE.item("cinder_alloy", Item::new).properties(p -> p.stacksTo(64).fireResistant()).tag(CCBItemTags.CINDER_ALLOY.tag).register();
    public static final ItemEntry<Item> AMETHYST_CRYSTAL_SHEET = CCB_REGISTRATE.item("amethyst_crystal_sheet", Item::new).properties(p -> p.stacksTo(64)).tag(CCBItemTags.AMETHYST_CRYSTAL_PLATE.tag).register();
    public static final ItemEntry<Item> POWDERED_CRYING_OBSIDIAN = CCB_REGISTRATE.item("powdered_crying_obsidian", Item::new).properties(p -> p.stacksTo(64)).tag(CCBItemTags.CRYING_OBSIDIAN.tag).register();
    public static final ItemEntry<Item> POWDERED_AMETHYST = CCB_REGISTRATE.item("powdered_amethyst", Item::new).properties(p -> p.stacksTo(64)).tag(CCBItemTags.AMETHYST.tag).register();
    public static final ItemEntry<Item> ICE_CREAM_CONE = CCB_REGISTRATE.item("ice_cream_cone", Item::new).properties(p -> p.food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build())).register();
    public static final ItemEntry<Item> ICE_CREAM = CCB_REGISTRATE.item("ice_cream", Item::new).properties(p -> p.stacksTo(16).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.5f).build())).register();
    public static final ItemEntry<MilkIceCreamItem> MILK_ICE_CREAM = CCB_REGISTRATE.item("milk_ice_cream", MilkIceCreamItem::new).properties(p -> p.stacksTo(16).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.5f).build())).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).register();
    public static final ItemEntry<Item> CHOCOLATE_ICE_CREAM = CCB_REGISTRATE.item("chocolate_ice_cream", Item::new).properties(p -> p.stacksTo(16).food(new FoodProperties.Builder().nutrition(9).saturationModifier(0.8f).build())).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).register();
    public static final ItemEntry<HoneyIceCreamItem> HONEY_ICE_CREAM = CCB_REGISTRATE.item("honey_ice_cream", HoneyIceCreamItem::new).properties(p -> p.stacksTo(16).food(new FoodProperties.Builder().nutrition(9).saturationModifier(0.6f).build())).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).register();
    public static final ItemEntry<AmethystIceCreamItem> AMETHYST_ICE_CREAM = CCB_REGISTRATE.item("amethyst_ice_cream", AmethystIceCreamItem::new).properties(p -> p.stacksTo(16).food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.5f).build())).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).register();
    public static final ItemEntry<CreativeIceCreamItem> CREATIVE_ICE_CREAM = CCB_REGISTRATE.item("creative_ice_cream", CreativeIceCreamItem::new).properties(p -> p.stacksTo(16).rarity(Rarity.EPIC).food(new FoodProperties.Builder().nutrition(20).saturationModifier(1f).build())).register();
    public static final ItemEntry<? extends CompressedAirCanisterItem> LOW_PRESSURE_COMPRESSED_AIR_CANISTER = CCB_REGISTRATE.item("low_pressure_compressed_air_canister", p -> new CompressedAirCanisterItem(CCBFluids.LOW_PRESSURE_COMPRESSED_AIR.get(), CCBTags.CCBFluidTags.LOW_PRESSURE_COMPRESSED_AIR.tag, p)).tag(CCBItemTags.COMPRESSED_AIR_CANISTER.tag).tag(CCBItemTags.LOW_PRESSURE_COMPRESSED_AIR_CANISTER.tag).properties(prop -> prop.stacksTo(1)).register();
    public static final ItemEntry<? extends CompressedAirCanisterItem> COMPRESSED_AIR_CANISTER = CCB_REGISTRATE.item("compressed_air_canister", p -> new CompressedAirCanisterItem(CCBFluids.MEDIUM_PRESSURE_COMPRESSED_AIR.get(), CCBTags.CCBFluidTags.MEDIUM_PRESSURE_COMPRESSED_AIR.tag, p)).tag(CCBItemTags.COMPRESSED_AIR_CANISTER.tag).tag(CCBItemTags.MEDIUM_PRESSURE_COMPRESSED_AIR_CANISTER.tag).properties(prop -> prop.stacksTo(1)).register();
    public static final ItemEntry<? extends CompressedAirCanisterItem> HIGH_PRESSURE_COMPRESSED_AIR_CANISTER = CCB_REGISTRATE.item("high_pressure_compressed_air_canister", p -> new CompressedAirCanisterItem(CCBFluids.HIGH_PRESSURE_COMPRESSED_AIR.get(), CCBTags.CCBFluidTags.HIGH_PRESSURE_COMPRESSED_AIR.tag, p)).tag(CCBItemTags.COMPRESSED_AIR_CANISTER.tag).tag(CCBItemTags.HIGH_PRESSURE_COMPRESSED_AIR_CANISTER.tag).properties(p -> p.stacksTo(1)).register();
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_SHEET = CCB_REGISTRATE.item("incomplete_airtight_sheet", SequencedAssemblyItem::new).register();
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_HEAVY_CORE = CCB_REGISTRATE.item("incomplete_heavy_core", SequencedAssemblyItem::new).model(AssetLookup.existingItemModel()).register();

    public static void register() {
    }
}
