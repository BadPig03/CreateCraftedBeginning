package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.AirtightBootsItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.AirtightHelmetItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.AirtightLeggingsItem;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonItem;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeItem;
import net.ty.createcraftedbeginning.content.airtights.airtightextendarm.AirtightExtendArmItem;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerInstrumentPanelItem;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.AirtightHandheldDrillItem;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightMeterItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonStyles;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterItem;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterItem.CreativeGasCanisterBlockItem;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterItem;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterItem.GasCanisterBlockItem;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackItem;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterItem;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualItem;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberFilterItem;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.AnchorFlareItem;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.RainFlareItem;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.SunnyFlareItem;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.ThunderstormFlareItem;
import net.ty.createcraftedbeginning.content.breezes.BreezeCoreItem;
import net.ty.createcraftedbeginning.content.icecreams.AmethystIceCreamItem;
import net.ty.createcraftedbeginning.content.icecreams.CreativeIceCreamItem;
import net.ty.createcraftedbeginning.content.icecreams.HoneyIceCreamItem;
import net.ty.createcraftedbeginning.content.icecreams.MilkIceCreamItem;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout.CCBCreativeTabSection;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import net.ty.createcraftedbeginning.registry.registrate.AirtightEquipmentRegistration;
import net.ty.createcraftedbeginning.registry.registrate.BalloonRegistration;
import net.ty.createcraftedbeginning.registry.registrate.CCBCreativeSectionTracker;
import net.ty.createcraftedbeginning.registry.registrate.CCBItemModelTransformer;
import net.ty.createcraftedbeginning.registry.registrate.CCBItemPropertiesTransformer;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrateProvider;
import net.ty.createcraftedbeginning.registry.registrate.GasCanisterRegistration;
import net.ty.createcraftedbeginning.registry.registrate.IceCreamRegistration;
import net.ty.createcraftedbeginning.registry.registrate.WeatherFlareRegistration;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBItems {
    private static final CreateRegistrate CCB_REGISTRATE = CCBRegistrateProvider.get();

    static {
        CCBCreativeSectionTracker.set(CCB_REGISTRATE, CCBCreativeTabSection.AIRTIGHTS);
    }

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_SHEET = CCB_REGISTRATE.item("incomplete_airtight_sheet", SequencedAssemblyItem::new).transform(CCBItemPropertiesTransformer.fireResistant()).register();
    public static final ItemEntry<Item> AIRTIGHT_SHEET = CCB_REGISTRATE.item("airtight_sheet", Item::new).transform(CCBItemPropertiesTransformer.fireResistant()).register();
    public static final ItemEntry<AirtightMeterItem> AIRTIGHT_METER = CCB_REGISTRATE.item("airtight_meter", AirtightMeterItem::new).transform(CCBItemModelTransformer.airtightMeter()).transform(CCBItemPropertiesTransformer.fireResistant()).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.AIRTIGHT_COMPONENTS.tag)).register();

    public static final ItemEntry<AirtightFractionationTowerInstrumentPanelItem> AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL = CCB_REGISTRATE.item("airtight_fractionation_tower_instrument_panel", AirtightFractionationTowerInstrumentPanelItem::new).transform(CCBItemPropertiesTransformer.fireResistant()).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_HEAVY_CORE = CCB_REGISTRATE.item("incomplete_heavy_core", SequencedAssemblyItem::new).transform(CCBItemModelTransformer.existing()).transform(CCBItemPropertiesTransformer.epic()).register();
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_BREEZE_CORE = CCB_REGISTRATE.item("incomplete_breeze_core", SequencedAssemblyItem::new).transform(CCBItemPropertiesTransformer.epic()).register();
    public static final ItemEntry<BreezeCoreItem> BREEZE_CORE = CCB_REGISTRATE.item("breeze_core", BreezeCoreItem::new).properties(properties -> properties.stacksTo(16).rarity(Rarity.EPIC)).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_TESLA_TURBINE_ROTOR = CCB_REGISTRATE.item("incomplete_tesla_turbine_rotor", SequencedAssemblyItem::new).properties(properties -> properties.rarity(Rarity.UNCOMMON).fireResistant()).register();
    public static final ItemEntry<Item> TESLA_TURBINE_ROTOR = CCB_REGISTRATE.item("tesla_turbine_rotor", Item::new).properties(properties -> properties.stacksTo(16).rarity(Rarity.UNCOMMON).fireResistant()).register();

    public static final ItemEntry<GasInjectionChamberFilterItem> GAS_INJECTION_CHAMBER_FILTER = CCB_REGISTRATE.item("gas_injection_chamber_filter", GasInjectionChamberFilterItem::new).transform(CCBItemModelTransformer.gasInjectionChamberFilter()).properties(properties -> properties.stacksTo(16).fireResistant()).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_CANNON = CCB_REGISTRATE.item("incomplete_airtight_cannon", SequencedAssemblyItem::new).transform(AirtightEquipmentRegistration.incompleteAirtightEquipment()).register();
    public static final ItemEntry<AirtightCannonItem> AIRTIGHT_CANNON = CCB_REGISTRATE.item("airtight_cannon", AirtightCannonItem::new).transform(CCBItemModelTransformer.withPartials()).transform(AirtightEquipmentRegistration.airtightEquipment(ItemTags.BOW_ENCHANTABLE, ItemTags.CROSSBOW_ENCHANTABLE, ItemTags.VANISHING_ENCHANTABLE)).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_EXTEND_ARM = CCB_REGISTRATE.item("incomplete_airtight_extend_arm", SequencedAssemblyItem::new).transform(AirtightEquipmentRegistration.incompleteAirtightEquipment()).register();
    public static final ItemEntry<AirtightExtendArmItem> AIRTIGHT_EXTEND_ARM = CCB_REGISTRATE.item("airtight_extend_arm", AirtightExtendArmItem::new).transform(CCBItemModelTransformer.withPartials()).transform(AirtightEquipmentRegistration.airtightEquipment()).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_HANDHELD_DRILL = CCB_REGISTRATE.item("incomplete_airtight_handheld_drill", SequencedAssemblyItem::new).transform(AirtightEquipmentRegistration.incompleteAirtightEquipment()).register();
    public static final ItemEntry<AirtightHandheldDrillItem> AIRTIGHT_HANDHELD_DRILL = CCB_REGISTRATE.item("airtight_handheld_drill", properties -> new AirtightHandheldDrillItem(Tiers.NETHERITE, properties)).transform(CCBItemModelTransformer.withPartials()).transform(AirtightEquipmentRegistration.airtightEquipment(ItemTags.MINING_ENCHANTABLE, ItemTags.VANISHING_ENCHANTABLE)).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_HELMET = CCB_REGISTRATE.item("incomplete_airtight_helmet", SequencedAssemblyItem::new).transform(AirtightEquipmentRegistration.incompleteAirtightEquipment()).register();
    public static final ItemEntry<AirtightHelmetItem> AIRTIGHT_HELMET = CCB_REGISTRATE.item("airtight_helmet", AirtightHelmetItem::new).transform(CCBItemModelTransformer.airtightArmor()).transform(AirtightEquipmentRegistration.airtightArmor(ItemTags.HEAD_ARMOR_ENCHANTABLE)).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_CHESTPLATE = CCB_REGISTRATE.item("incomplete_airtight_chestplate", SequencedAssemblyItem::new).transform(AirtightEquipmentRegistration.incompleteAirtightEquipment()).register();
    public static final ItemEntry<AirtightChestplateItem> AIRTIGHT_CHESTPLATE = CCB_REGISTRATE.item("airtight_chestplate", AirtightChestplateItem::new).transform(CCBItemModelTransformer.airtightArmor()).transform(AirtightEquipmentRegistration.airtightArmor(ItemTags.CHEST_ARMOR_ENCHANTABLE)).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_LEGGINGS = CCB_REGISTRATE.item("incomplete_airtight_leggings", SequencedAssemblyItem::new).transform(AirtightEquipmentRegistration.incompleteAirtightEquipment()).register();
    public static final ItemEntry<AirtightLeggingsItem> AIRTIGHT_LEGGINGS = CCB_REGISTRATE.item("airtight_leggings", AirtightLeggingsItem::new).transform(CCBItemModelTransformer.airtightArmor()).transform(AirtightEquipmentRegistration.airtightArmor(ItemTags.LEG_ARMOR_ENCHANTABLE)).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_BOOTS = CCB_REGISTRATE.item("incomplete_airtight_boots", SequencedAssemblyItem::new).transform(AirtightEquipmentRegistration.incompleteAirtightEquipment()).register();
    public static final ItemEntry<AirtightBootsItem> AIRTIGHT_BOOTS = CCB_REGISTRATE.item("airtight_boots", AirtightBootsItem::new).transform(CCBItemModelTransformer.airtightArmor()).transform(AirtightEquipmentRegistration.airtightArmor(ItemTags.FOOT_ARMOR_ENCHANTABLE)).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_WEATHER_FLARE = CCB_REGISTRATE.item("incomplete_weather_flare", SequencedAssemblyItem::new).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<Item> UNFILLED_WEATHER_FLARE = CCB_REGISTRATE.item("unfilled_weather_flare", Item::new).transform(CCBItemPropertiesTransformer.stack16()).register();
    public static final ItemEntry<SunnyFlareItem> SUNNY_FLARE = CCB_REGISTRATE.item("sunny_flare", SunnyFlareItem::new).transform(WeatherFlareRegistration.weatherFlare()).register();
    public static final ItemEntry<RainFlareItem> RAIN_FLARE = CCB_REGISTRATE.item("rain_flare", RainFlareItem::new).transform(WeatherFlareRegistration.weatherFlare()).register();
    public static final ItemEntry<ThunderstormFlareItem> THUNDERSTORM_FLARE = CCB_REGISTRATE.item("thunderstorm_flare", ThunderstormFlareItem::new).transform(WeatherFlareRegistration.weatherFlare()).register();
    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_ANCHOR_FLARE = CCB_REGISTRATE.item("incomplete_anchor_flare", SequencedAssemblyItem::new).transform(CCBItemPropertiesTransformer.uncommon()).register();
    public static final ItemEntry<AnchorFlareItem> ANCHOR_FLARE = CCB_REGISTRATE.item("anchor_flare", AnchorFlareItem::new).transform(WeatherFlareRegistration.anchorFlare()).register();

    static {
        CCBCreativeSectionTracker.set(CCB_REGISTRATE, CCBCreativeTabSection.OPTICAL_POWER);
    }

    public static final ItemEntry<Item> POWDERED_AMETHYST = CCB_REGISTRATE.item("powdered_amethyst", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.DUSTS.tag, CCBItemTags.DUSTS_AMETHYST.tag)).register();
    public static final ItemEntry<Item> AMETHYST_CRYSTAL_SHEET = CCB_REGISTRATE.item("amethyst_crystal_sheet", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.PLATES_AMETHYST_CRYSTAL.tag)).register();

    static {
        CCBCreativeSectionTracker.set(CCB_REGISTRATE, CCBCreativeTabSection.ENDS);
    }

    public static final ItemEntry<Item> CHORUS_FLOWER_POWDER = CCB_REGISTRATE.item("chorus_flower_powder", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.DUSTS.tag, CCBItemTags.DUSTS_CHORUS_FLOWER.tag)).register();

    public static final ItemEntry<Item> END_ALLOY = CCB_REGISTRATE.item("end_alloy", Item::new).transform(CCBItemPropertiesTransformer.uncommonMaterial(CCBItemTags.INGOTS_END_ALLOY.tag)).register();
    public static final ItemEntry<Item> END_ALLOY_SHEET = CCB_REGISTRATE.item("end_alloy_sheet", Item::new).transform(CCBItemPropertiesTransformer.uncommonMaterial(CCBItemTags.PLATES_END_ALLOY.tag)).register();

    static {
        CCBCreativeSectionTracker.set(CCB_REGISTRATE, CCBCreativeTabSection.DECORATIONS);
    }

    public static final ItemEntry<Item> OBSIDIAN_CHUNK = CCB_REGISTRATE.item("obsidian_chunk", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.CHUNKS.tag)).register();
    public static final ItemEntry<Item> OBSIDIAN_BRICK = CCB_REGISTRATE.item("obsidian_brick", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.BRICKS.tag, CCBItemTags.BRICKS_OBSIDIAN.tag)).register();
    public static final ItemEntry<Item> CRYING_OBSIDIAN_CHUNK = CCB_REGISTRATE.item("crying_obsidian_chunk", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.CHUNKS.tag)).register();
    public static final ItemEntry<Item> CRYING_OBSIDIAN_BRICK = CCB_REGISTRATE.item("crying_obsidian_brick", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.BRICKS.tag, CCBItemTags.BRICKS_CRYING_OBSIDIAN.tag)).register();
    public static final ItemEntry<Item> POWDERED_CRYING_OBSIDIAN = CCB_REGISTRATE.item("powdered_crying_obsidian", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.DUSTS.tag, CCBItemTags.DUSTS_CRYING_OBSIDIAN.tag)).register();

    public static final ItemEntry<Item> ICE_CREAM_CONE = CCB_REGISTRATE.item("ice_cream_cone", Item::new).transform(IceCreamRegistration.iceCreamCone()).register();
    public static final ItemEntry<Item> ICE_CREAM = CCB_REGISTRATE.item("ice_cream", Item::new).transform(IceCreamRegistration.iceCream()).register();
    public static final ItemEntry<MilkIceCreamItem> MILK_ICE_CREAM = CCB_REGISTRATE.item("milk_ice_cream", MilkIceCreamItem::new).transform(IceCreamRegistration.flavoredIceCream(4, 0.6F)).register();
    public static final ItemEntry<Item> BUILDERS_TEA_ICE_CREAM = CCB_REGISTRATE.item("builders_tea_ice_cream", Item::new).transform(IceCreamRegistration.buildersTeaIceCream()).register();
    public static final ItemEntry<AmethystIceCreamItem> AMETHYST_ICE_CREAM = CCB_REGISTRATE.item("amethyst_ice_cream", AmethystIceCreamItem::new).transform(IceCreamRegistration.flavoredIceCream(8, 0.5F)).register();
    public static final ItemEntry<HoneyIceCreamItem> HONEY_ICE_CREAM = CCB_REGISTRATE.item("honey_ice_cream", HoneyIceCreamItem::new).transform(IceCreamRegistration.flavoredIceCream(9, 0.6F)).register();
    public static final ItemEntry<Item> CHOCOLATE_ICE_CREAM = CCB_REGISTRATE.item("chocolate_ice_cream", Item::new).transform(IceCreamRegistration.flavoredIceCream(9, 0.8F)).register();
    public static final ItemEntry<Item> GOLDEN_ICE_CREAM = CCB_REGISTRATE.item("golden_ice_cream", Item::new).transform(IceCreamRegistration.flavoredIceCream(8, 1.2F)).register();
    public static final ItemEntry<CreativeIceCreamItem> CREATIVE_ICE_CREAM = CCB_REGISTRATE.item("creative_ice_cream", CreativeIceCreamItem::new).transform(IceCreamRegistration.creativeIceCream()).register();

    static {
        CCBCreativeSectionTracker.set(CCB_REGISTRATE, CCBCreativeTabSection.CANISTERS);
    }

    public static final ItemEntry<AirtightCannonWindChargeItem> NATURAL_WIND_CHARGE = CCB_REGISTRATE.item("natural_wind_charge", properties -> new AirtightCannonWindChargeItem(properties, CCBGases.NATURAL_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> ULTRAWARM_WIND_CHARGE = CCB_REGISTRATE.item("ultrawarm_wind_charge", properties -> new AirtightCannonWindChargeItem(properties, CCBGases.ULTRAWARM_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> ETHEREAL_WIND_CHARGE = CCB_REGISTRATE.item("ethereal_wind_charge", properties -> new AirtightCannonWindChargeItem(properties, CCBGases.ETHEREAL_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> MOIST_WIND_CHARGE = CCB_REGISTRATE.item("moist_wind_charge", properties -> new AirtightCannonWindChargeItem(properties, CCBGases.MOIST_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> SPORE_WIND_CHARGE = CCB_REGISTRATE.item("spore_wind_charge", properties -> new AirtightCannonWindChargeItem(properties, CCBGases.SPORE_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> SCULK_WIND_CHARGE = CCB_REGISTRATE.item("sculk_wind_charge", properties -> new AirtightCannonWindChargeItem(properties, CCBGases.SCULK_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> STEAM_WIND_CHARGE = CCB_REGISTRATE.item("steam_wind_charge", properties -> new AirtightCannonWindChargeItem(properties, CCBGases.STEAM)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> ENERGIZED_NATURAL_WIND_CHARGE = CCB_REGISTRATE.item("energized_natural_wind_charge", properties -> new AirtightCannonWindChargeItem(properties, CCBGases.ENERGIZED_NATURAL_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> ENERGIZED_ULTRAWARM_WIND_CHARGE = CCB_REGISTRATE.item("energized_ultrawarm_wind_charge", properties -> new AirtightCannonWindChargeItem(properties, CCBGases.ENERGIZED_ULTRAWARM_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> ENERGIZED_ETHEREAL_WIND_CHARGE = CCB_REGISTRATE.item("energized_ethereal_wind_charge", properties -> new AirtightCannonWindChargeItem(properties, CCBGases.ENERGIZED_ETHEREAL_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> CREATIVE_WIND_CHARGE = CCB_REGISTRATE.item("creative_wind_charge", properties -> new AirtightCannonWindChargeItem(properties, CCBGases.CREATIVE_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();

    public static final ItemEntry<BalloonItem> BALLOON_10X8 = CCB_REGISTRATE.item("balloon_10x8", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_10_8, false)).transform(CCBItemModelTransformer.balloon(BalloonStyles.BALLOON_10_8)).transform(BalloonRegistration.balloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_10X12 = CCB_REGISTRATE.item("balloon_10x12", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_10_12, false)).transform(CCBItemModelTransformer.balloon(BalloonStyles.BALLOON_10_12)).transform(BalloonRegistration.balloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_12X10 = CCB_REGISTRATE.item("balloon_12x10", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_12_10, false)).transform(CCBItemModelTransformer.balloon(BalloonStyles.BALLOON_12_10)).transform(BalloonRegistration.balloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_12X12 = CCB_REGISTRATE.item("balloon_12x12", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_12_12, false)).transform(CCBItemModelTransformer.balloon(BalloonStyles.BALLOON_12_12)).transform(BalloonRegistration.balloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_REVERTED = CCB_REGISTRATE.item("balloon_rare_reverted", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_RARE_REVERTED, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyles.BALLOON_RARE_REVERTED)).transform(BalloonRegistration.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_SMILE = CCB_REGISTRATE.item("balloon_rare_smile", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_RARE_SMILE, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyles.BALLOON_RARE_SMILE)).transform(BalloonRegistration.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_CRY = CCB_REGISTRATE.item("balloon_rare_cry", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_RARE_CRY, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyles.BALLOON_RARE_CRY)).transform(BalloonRegistration.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_EYE = CCB_REGISTRATE.item("balloon_rare_eye", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_RARE_EYE, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyles.BALLOON_RARE_EYE)).transform(BalloonRegistration.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_ISAAC = CCB_REGISTRATE.item("balloon_rare_isaac", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_RARE_ISAAC, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyles.BALLOON_RARE_ISAAC)).transform(BalloonRegistration.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_GHAST = CCB_REGISTRATE.item("balloon_rare_ghast", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_RARE_GHAST, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyles.BALLOON_RARE_GHAST)).transform(BalloonRegistration.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_TROLLFACE = CCB_REGISTRATE.item("balloon_rare_trollface", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_RARE_TROLLFACE, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyles.BALLOON_RARE_TROLLFACE)).transform(BalloonRegistration.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_TENNA = CCB_REGISTRATE.item("balloon_rare_tenna", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_RARE_TENNA, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyles.BALLOON_RARE_TENNA)).transform(BalloonRegistration.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_PVZ = CCB_REGISTRATE.item("balloon_rare_pvz", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_RARE_PVZ, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyles.BALLOON_RARE_PVZ)).transform(BalloonRegistration.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_QUESTION_MARKS = CCB_REGISTRATE.item("balloon_rare_question_marks", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_RARE_QUESTION_MARKS, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyles.BALLOON_RARE_QUESTION_MARKS)).transform(BalloonRegistration.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_POWERFUL = CCB_REGISTRATE.item("balloon_rare_powerful", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_RARE_POWERFUL, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyles.BALLOON_RARE_POWERFUL)).transform(BalloonRegistration.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_CHEESE = CCB_REGISTRATE.item("balloon_rare_cheese", properties -> new BalloonItem(properties, BalloonStyles.BALLOON_RARE_CHEESE, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyles.BALLOON_RARE_CHEESE)).transform(BalloonRegistration.rareBalloon()).register();

    public static final ItemEntry<GasVirtualItem> GAS_VIRTUAL_ITEM = CCB_REGISTRATE.item("gas_virtual_item", GasVirtualItem::new).transform(CCBItemPropertiesTransformer.stack1()).register();

    public static final ItemEntry<GasFilterItem> GAS_FILTER = CCB_REGISTRATE.item("gas_filter", GasFilterItem::new).transform(CCBItemPropertiesTransformer.defaultProperties()).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_GAS_CANISTER_PACK = CCB_REGISTRATE.item("incomplete_gas_canister_pack", SequencedAssemblyItem::new).transform(CCBItemPropertiesTransformer.fireResistant()).register();
    public static final ItemEntry<GasCanisterPackItem> GAS_CANISTER_PACK = CCB_REGISTRATE.item("gas_canister_pack", GasCanisterPackItem::new).transform(CCBItemModelTransformer.gasCanisterPack()).transform(GasCanisterRegistration.gasCanisterPack()).register();

    public static final ItemEntry<GasCanisterBlockItem> GAS_CANISTER_PLACEABLE = CCB_REGISTRATE.item("gas_canister_placeable", properties -> new GasCanisterBlockItem(CCBBlocks.GAS_CANISTER_BLOCK.get(), CCBItems.GAS_CANISTER::get, properties)).transform(CCBItemModelTransformer.gasCanister()).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<GasCanisterItem> GAS_CANISTER = CCB_REGISTRATE.item("gas_canister", properties -> new GasCanisterItem(properties, GAS_CANISTER_PLACEABLE)).transform(GasCanisterRegistration.gasCanister()).register();

    public static final ItemEntry<CreativeGasCanisterBlockItem> CREATIVE_GAS_CANISTER_PLACEABLE = CCB_REGISTRATE.item("creative_gas_canister_placeable", properties -> new CreativeGasCanisterBlockItem(CCBBlocks.CREATIVE_GAS_CANISTER_BLOCK.get(), CCBItems.CREATIVE_GAS_CANISTER::get, properties)).transform(CCBItemModelTransformer.creativeGasCanister()).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<CreativeGasCanisterItem> CREATIVE_GAS_CANISTER = CCB_REGISTRATE.item("creative_gas_canister", properties -> new CreativeGasCanisterItem(properties, CREATIVE_GAS_CANISTER_PLACEABLE)).transform(GasCanisterRegistration.creativeGasCanister()).register();

    public static void register() {
        CCBCreativeTabs.registerSectionInit(CCBCreativeTabSection.AIRTIGHTS, AIRTIGHT_SHEET);
        CCBCreativeTabs.registerSectionInit(CCBCreativeTabSection.OPTICAL_POWER, POWDERED_AMETHYST, AMETHYST_CRYSTAL_SHEET);
        CCBCreativeTabs.registerSectionInit(CCBCreativeTabSection.ENDS, END_ALLOY, END_ALLOY_SHEET);
    }
}
