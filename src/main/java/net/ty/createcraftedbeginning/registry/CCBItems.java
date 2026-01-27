package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties.Builder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.common.Tags.Items;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.recipes.SequencedAssemblyWithGasItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsTrimsModelGenerator;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightBootsItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightChestplateItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightHelmetItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightLeggingsItem;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonItem;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeItem;
import net.ty.createcraftedbeginning.content.airtights.airtightextendarm.AirtightExtendArmItem;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillItem;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterItem;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterItem.GasCanisterBlockItem;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackItem;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackOverrides;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterItem;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualItem;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.AnchorFlareItem;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.RainFlareItem;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.SunnyFlareItem;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.ThunderstormFlareItem;
import net.ty.createcraftedbeginning.content.breezes.BreezeCoreItem;
import net.ty.createcraftedbeginning.content.icecreams.AmethystIceCreamItem;
import net.ty.createcraftedbeginning.content.icecreams.CreativeIceCreamItem;
import net.ty.createcraftedbeginning.content.icecreams.HoneyIceCreamItem;
import net.ty.createcraftedbeginning.content.icecreams.MilkIceCreamItem;
import net.ty.createcraftedbeginning.data.CCBGases;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;

@SuppressWarnings("unused")
public class CCBItems {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate().setCreativeTab(CCBCreativeTabs.BASE_CREATIVE_TAB);

    public static final ItemEntry<Item> POWDERED_AMETHYST = CCB_REGISTRATE.item("powdered_amethyst", Item::new).properties(p -> p.stacksTo(64)).tag(CCBItemTags.AMETHYST.tag).register();
    public static final ItemEntry<Item> POWDERED_CRYING_OBSIDIAN = CCB_REGISTRATE.item("powdered_crying_obsidian", Item::new).properties(p -> p.stacksTo(64)).tag(CCBItemTags.CRYING_OBSIDIAN.tag).register();
    public static final ItemEntry<Item> AMETHYST_CRYSTAL_SHEET = CCB_REGISTRATE.item("amethyst_crystal_sheet", Item::new).properties(p -> p.stacksTo(64)).tag(CCBItemTags.AMETHYST_CRYSTAL_PLATE.tag).register();
    public static final ItemEntry<Item> CHORUS_FLOWER_POWDER = CCB_REGISTRATE.item("chorus_flower_powder", Item::new).tag(CCBItemTags.CHORUS_FLOWER.tag).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_SHEET = CCB_REGISTRATE.item("incomplete_airtight_sheet", SequencedAssemblyItem::new).properties(Properties::fireResistant).register();
    public static final ItemEntry<Item> AIRTIGHT_SHEET = CCB_REGISTRATE.item("airtight_sheet", Item::new).properties(p -> p.stacksTo(64).fireResistant()).register();
    public static final ItemEntry<Item> CINDER_ALLOY = CCB_REGISTRATE.item("cinder_alloy", Item::new).properties(p -> p.stacksTo(64).fireResistant()).tag(CCBItemTags.CINDER_ALLOY.tag).register();
    public static final ItemEntry<Item> END_ALLOY = CCB_REGISTRATE.item("end_alloy", Item::new).tag(CCBItemTags.END_ALLOY.tag).register();

    public static final ItemEntry<GasFilterItem> GAS_FILTER = CCB_REGISTRATE.item("gas_filter", GasFilterItem::new).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_GAS_CANISTER_PACK = CCB_REGISTRATE.item("incomplete_gas_canister_pack", SequencedAssemblyItem::new).properties(Properties::fireResistant).register();
    public static final ItemEntry<GasCanisterPackItem> GAS_CANISTER_PACK = CCB_REGISTRATE.item("gas_canister_pack", GasCanisterPackItem::new).properties(p -> p.stacksTo(1).fireResistant().rarity(Rarity.UNCOMMON)).onRegister(GasCanisterPackItem::registerModelOverrides).model(GasCanisterPackOverrides::addOverrideModels).register();

    public static final ItemEntry<SequencedAssemblyWithGasItem> INCOMPLETE_HEAVY_CORE = CCB_REGISTRATE.item("incomplete_heavy_core", SequencedAssemblyWithGasItem::new).properties(p -> p.rarity(Rarity.EPIC)).model(AssetLookup.existingItemModel()).register();
    public static final ItemEntry<SequencedAssemblyWithGasItem> INCOMPLETE_BREEZE_CORE = CCB_REGISTRATE.item("incomplete_breeze_core", SequencedAssemblyWithGasItem::new).properties(p -> p.rarity(Rarity.EPIC)).register();
    public static final ItemEntry<BreezeCoreItem> BREEZE_CORE = CCB_REGISTRATE.item("breeze_core", BreezeCoreItem::new).properties(p -> p.stacksTo(16).rarity(Rarity.EPIC)).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_TESLA_TURBINE_ROTOR = CCB_REGISTRATE.item("incomplete_tesla_turbine_rotor", SequencedAssemblyItem::new).properties(p -> p.rarity(Rarity.UNCOMMON).fireResistant()).register();
    public static final ItemEntry<Item> TESLA_TURBINE_ROTOR = CCB_REGISTRATE.item("tesla_turbine_rotor", Item::new).properties(p -> p.stacksTo(16).rarity(Rarity.UNCOMMON).fireResistant()).register();

    public static final ItemEntry<GasCanisterBlockItem> GAS_CANISTER_PLACEABLE = CCB_REGISTRATE.item("gas_canister_placeable", p -> new GasCanisterBlockItem(CCBBlocks.GAS_CANISTER_BLOCK.get(), CCBItems.GAS_CANISTER::get, p)).model((c, p) -> p.withExistingParent(c.getName(), p.modLoc("block/gas_canister"))).register();
    public static final ItemEntry<GasCanisterItem> GAS_CANISTER = CCB_REGISTRATE.item("gas_canister", p -> new GasCanisterItem(p, GAS_CANISTER_PLACEABLE)).properties(p -> p.stacksTo(1).fireResistant()).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_CANNON = CCB_REGISTRATE.item("incomplete_airtight_cannon", SequencedAssemblyItem::new).properties(p -> p.rarity(Rarity.EPIC).fireResistant()).register();
    public static final ItemEntry<AirtightCannonItem> AIRTIGHT_CANNON = CCB_REGISTRATE.item("airtight_cannon", AirtightCannonItem::new).properties(p -> p.rarity(Rarity.EPIC).fireResistant().stacksTo(1)).model(AssetLookup.itemModelWithPartials()).tag(Items.ENCHANTABLES).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_EXTEND_ARM = CCB_REGISTRATE.item("incomplete_airtight_extend_arm", SequencedAssemblyItem::new).properties(p -> p.rarity(Rarity.EPIC).fireResistant()).register();
    public static final ItemEntry<AirtightExtendArmItem> AIRTIGHT_EXTEND_ARM = CCB_REGISTRATE.item("airtight_extend_arm", AirtightExtendArmItem::new).properties(p -> p.rarity(Rarity.EPIC).fireResistant().stacksTo(1)).model(AssetLookup.itemModelWithPartials()).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_HANDHELD_DRILL = CCB_REGISTRATE.item("incomplete_airtight_handheld_drill", SequencedAssemblyItem::new).properties(p -> p.rarity(Rarity.EPIC).fireResistant()).register();
    public static final ItemEntry<AirtightHandheldDrillItem> AIRTIGHT_HANDHELD_DRILL = CCB_REGISTRATE.item("airtight_handheld_drill", p -> new AirtightHandheldDrillItem(Tiers.NETHERITE, p)).properties(p -> p.rarity(Rarity.EPIC).fireResistant().stacksTo(1)).model(AssetLookup.itemModelWithPartials()).tag(Items.ENCHANTABLES).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_HELMET = CCB_REGISTRATE.item("incomplete_airtight_helmet", SequencedAssemblyItem::new).properties(p -> p.rarity(Rarity.EPIC).fireResistant()).register();
    public static final ItemEntry<AirtightHelmetItem> AIRTIGHT_HELMET = CCB_REGISTRATE.item("airtight_helmet", AirtightHelmetItem::new).properties(p -> p.rarity(Rarity.EPIC).fireResistant().stacksTo(1)).tag(ItemTags.HEAD_ARMOR).tag(CCBItemTags.AIRTIGHT_ARMOR.tag).model(AirtightArmorsTrimsModelGenerator::generate).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_CHESTPLATE = CCB_REGISTRATE.item("incomplete_airtight_chestplate", SequencedAssemblyItem::new).properties(p -> p.rarity(Rarity.EPIC).fireResistant()).register();
    public static final ItemEntry<AirtightChestplateItem> AIRTIGHT_CHESTPLATE = CCB_REGISTRATE.item("airtight_chestplate", AirtightChestplateItem::new).properties(p -> p.rarity(Rarity.EPIC).fireResistant().stacksTo(1)).tag(ItemTags.CHEST_ARMOR).tag(CCBItemTags.AIRTIGHT_ARMOR.tag).model(AirtightArmorsTrimsModelGenerator::generate).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_LEGGINGS = CCB_REGISTRATE.item("incomplete_airtight_leggings", SequencedAssemblyItem::new).properties(p -> p.rarity(Rarity.EPIC).fireResistant()).register();
    public static final ItemEntry<AirtightLeggingsItem> AIRTIGHT_LEGGINGS = CCB_REGISTRATE.item("airtight_leggings", AirtightLeggingsItem::new).properties(p -> p.rarity(Rarity.EPIC).fireResistant().stacksTo(1)).tag(ItemTags.LEG_ARMOR).tag(CCBItemTags.AIRTIGHT_ARMOR.tag).model(AirtightArmorsTrimsModelGenerator::generate).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_BOOTS = CCB_REGISTRATE.item("incomplete_airtight_boots", SequencedAssemblyItem::new).properties(p -> p.rarity(Rarity.EPIC).fireResistant()).register();
    public static final ItemEntry<AirtightBootsItem> AIRTIGHT_BOOTS = CCB_REGISTRATE.item("airtight_boots", AirtightBootsItem::new).properties(p -> p.rarity(Rarity.EPIC).fireResistant().stacksTo(1)).tag(ItemTags.FOOT_ARMOR).tag(CCBItemTags.AIRTIGHT_ARMOR.tag).model(AirtightArmorsTrimsModelGenerator::generate).register();

    public static final ItemEntry<SequencedAssemblyWithGasItem> INCOMPLETE_WEATHER_FLARE = CCB_REGISTRATE.item("incomplete_weather_flare", SequencedAssemblyWithGasItem::new).register();
    public static final ItemEntry<Item> UNFILLED_WEATHER_FLARE = CCB_REGISTRATE.item("unfilled_weather_flare", Item::new).properties(p -> p.stacksTo(16)).register();
    public static final ItemEntry<SunnyFlareItem> SUNNY_FLARE = CCB_REGISTRATE.item("sunny_flare", SunnyFlareItem::new).properties(p -> p.stacksTo(16)).tag(CCBItemTags.WEATHER_FLARE.tag).register();
    public static final ItemEntry<RainFlareItem> RAIN_FLARE = CCB_REGISTRATE.item("rain_flare", RainFlareItem::new).properties(p -> p.stacksTo(16)).tag(CCBItemTags.WEATHER_FLARE.tag).register();
    public static final ItemEntry<ThunderstormFlareItem> THUNDERSTORM_FLARE = CCB_REGISTRATE.item("thunderstorm_flare", ThunderstormFlareItem::new).properties(p -> p.stacksTo(16)).tag(CCBItemTags.WEATHER_FLARE.tag).register();
    public static final ItemEntry<SequencedAssemblyWithGasItem> INCOMPLETE_ANCHOR_FLARE = CCB_REGISTRATE.item("incomplete_anchor_flare", SequencedAssemblyWithGasItem::new).properties(p -> p.rarity(Rarity.UNCOMMON)).register();
    public static final ItemEntry<AnchorFlareItem> ANCHOR_FLARE = CCB_REGISTRATE.item("anchor_flare", AnchorFlareItem::new).properties(p -> p.stacksTo(16).rarity(Rarity.UNCOMMON)).tag(CCBItemTags.WEATHER_FLARE.tag).register();

    public static final ItemEntry<Item> ICE_CREAM_CONE = CCB_REGISTRATE.item("ice_cream_cone", Item::new).properties(p -> p.food(new Builder().nutrition(4).saturationModifier(0.6f).build())).register();
    public static final ItemEntry<Item> ICE_CREAM = CCB_REGISTRATE.item("ice_cream", Item::new).properties(p -> p.stacksTo(16).food(new Builder().nutrition(4).saturationModifier(0.6f).alwaysEdible().build())).tag(CCBItemTags.ICE_CREAMS.tag).register();
    public static final ItemEntry<MilkIceCreamItem> MILK_ICE_CREAM = CCB_REGISTRATE.item("milk_ice_cream", MilkIceCreamItem::new).properties(p -> p.stacksTo(16).food(new Builder().nutrition(4).saturationModifier(0.6f).alwaysEdible().build())).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).tag(CCBItemTags.ICE_CREAMS.tag).register();
    public static final ItemEntry<Item> BUILDERS_TEA_ICE_CREAM = CCB_REGISTRATE.item("builders_tea_ice_cream", Item::new).properties(p -> p.stacksTo(16).food(new Builder().nutrition(4).saturationModifier(0.6f).alwaysEdible().effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0, false, false, false), 1.0F).build())).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).tag(CCBItemTags.ICE_CREAMS.tag).register();
    public static final ItemEntry<AmethystIceCreamItem> AMETHYST_ICE_CREAM = CCB_REGISTRATE.item("amethyst_ice_cream", AmethystIceCreamItem::new).properties(p -> p.stacksTo(16).food(new Builder().nutrition(8).saturationModifier(0.5f).alwaysEdible().build())).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).tag(CCBItemTags.ICE_CREAMS.tag).register();
    public static final ItemEntry<HoneyIceCreamItem> HONEY_ICE_CREAM = CCB_REGISTRATE.item("honey_ice_cream", HoneyIceCreamItem::new).properties(p -> p.stacksTo(16).food(new Builder().nutrition(9).saturationModifier(0.6f).alwaysEdible().build())).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).tag(CCBItemTags.ICE_CREAMS.tag).register();
    public static final ItemEntry<Item> CHOCOLATE_ICE_CREAM = CCB_REGISTRATE.item("chocolate_ice_cream", Item::new).properties(p -> p.stacksTo(16).food(new Builder().nutrition(9).saturationModifier(0.8f).alwaysEdible().build())).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).tag(CCBItemTags.ICE_CREAMS.tag).register();
    public static final ItemEntry<Item> GOLDEN_ICE_CREAM = CCB_REGISTRATE.item("golden_ice_cream", Item::new).properties(p -> p.stacksTo(16).food(new Builder().nutrition(8).saturationModifier(1.2f).alwaysEdible().build())).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).tag(CCBItemTags.ICE_CREAMS.tag).register();
    public static final ItemEntry<CreativeIceCreamItem> CREATIVE_ICE_CREAM = CCB_REGISTRATE.item("creative_ice_cream", CreativeIceCreamItem::new).properties(p -> p.stacksTo(16).rarity(Rarity.EPIC).food(new Builder().nutrition(20).saturationModifier(1.0f).alwaysEdible().build())).register();

    public static final ItemEntry<AirtightCannonWindChargeItem> NATURAL_WIND_CHARGE = CCB_REGISTRATE.item("natural_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.NATURAL_AIR)).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> ULTRAWARM_WIND_CHARGE = CCB_REGISTRATE.item("ultrawarm_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.ULTRAWARM_AIR)).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> ETHEREAL_WIND_CHARGE = CCB_REGISTRATE.item("ethereal_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.ETHEREAL_AIR)).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> MOIST_WIND_CHARGE = CCB_REGISTRATE.item("moist_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.MOIST_AIR)).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> SPORE_WIND_CHARGE = CCB_REGISTRATE.item("spore_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.SPORE_AIR)).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> ENERGIZED_NATURAL_WIND_CHARGE = CCB_REGISTRATE.item("energized_natural_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.ENERGIZED_NATURAL_AIR)).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> ENERGIZED_ULTRAWARM_WIND_CHARGE = CCB_REGISTRATE.item("energized_ultrawarm_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.ENERGIZED_ULTRAWARM_AIR)).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> ENERGIZED_ETHEREAL_WIND_CHARGE = CCB_REGISTRATE.item("energized_ethereal_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.ENERGIZED_ETHEREAL_AIR)).register();

    public static final ItemEntry<GasVirtualItem> GAS_VIRTUAL_ITEM = CCB_REGISTRATE.item("gas_virtual_item", GasVirtualItem::new).properties(p -> p.stacksTo(1)).register();

    public static void register() {
    }
}
