package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.recipes.SequencedAssemblyWithGasItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.AirtightBootsItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.AirtightHelmetItem;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.AirtightLeggingsItem;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonItem;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeItem;
import net.ty.createcraftedbeginning.content.airtights.airtightextendarm.AirtightExtendArmItem;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonStyleUtils;
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
import net.ty.createcraftedbeginning.data.CCBGases;
import net.ty.createcraftedbeginning.data.CCBItemModelTransformer;
import net.ty.createcraftedbeginning.data.CCBItemPropertiesTransformer;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout.CCBCreativeTabSection;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBItems {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate();

    static {
        CCB_REGISTRATE.setCreativeSection(CCBCreativeTabSection.AIRTIGHTS);
    }

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_SHEET = CCB_REGISTRATE.item("incomplete_airtight_sheet", SequencedAssemblyItem::new).transform(CCBItemPropertiesTransformer.fireResistant()).register();
    public static final ItemEntry<Item> AIRTIGHT_SHEET = CCB_REGISTRATE.item("airtight_sheet", Item::new).transform(CCBItemPropertiesTransformer.fireResistant()).register();

    public static final ItemEntry<SequencedAssemblyWithGasItem> INCOMPLETE_HEAVY_CORE = CCB_REGISTRATE.item("incomplete_heavy_core", SequencedAssemblyWithGasItem::new).transform(CCBItemModelTransformer.existing()).transform(CCBItemPropertiesTransformer.epic()).register();
    public static final ItemEntry<SequencedAssemblyWithGasItem> INCOMPLETE_BREEZE_CORE = CCB_REGISTRATE.item("incomplete_breeze_core", SequencedAssemblyWithGasItem::new).transform(CCBItemPropertiesTransformer.epic()).register();
    public static final ItemEntry<BreezeCoreItem> BREEZE_CORE = CCB_REGISTRATE.item("breeze_core", BreezeCoreItem::new).transform(CCBItemPropertiesTransformer.breezeCore()).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_TESLA_TURBINE_ROTOR = CCB_REGISTRATE.item("incomplete_tesla_turbine_rotor", SequencedAssemblyItem::new).transform(CCBItemPropertiesTransformer.incompleteTeslaTurbineRotor()).register();
    public static final ItemEntry<Item> TESLA_TURBINE_ROTOR = CCB_REGISTRATE.item("tesla_turbine_rotor", Item::new).transform(CCBItemPropertiesTransformer.teslaTurbineRotor()).register();

    public static final ItemEntry<GasInjectionChamberFilterItem> GAS_INJECTION_CHAMBER_FILTER = CCB_REGISTRATE.item("gas_injection_chamber_filter", GasInjectionChamberFilterItem::new).transform(CCBItemModelTransformer.gasInjectionChamberFilter()).transform(CCBItemPropertiesTransformer.gasInjectionChamberFilter()).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_CANNON = CCB_REGISTRATE.item("incomplete_airtight_cannon", SequencedAssemblyItem::new).transform(CCBItemPropertiesTransformer.incompleteAirtightEquipment()).register();
    public static final ItemEntry<AirtightCannonItem> AIRTIGHT_CANNON = CCB_REGISTRATE.item("airtight_cannon", AirtightCannonItem::new).transform(CCBItemModelTransformer.withPartials()).transform(CCBItemPropertiesTransformer.airtightEquipment(ItemTags.BOW_ENCHANTABLE, ItemTags.CROSSBOW_ENCHANTABLE, ItemTags.VANISHING_ENCHANTABLE)).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_EXTEND_ARM = CCB_REGISTRATE.item("incomplete_airtight_extend_arm", SequencedAssemblyItem::new).transform(CCBItemPropertiesTransformer.incompleteAirtightEquipment()).register();
    public static final ItemEntry<AirtightExtendArmItem> AIRTIGHT_EXTEND_ARM = CCB_REGISTRATE.item("airtight_extend_arm", AirtightExtendArmItem::new).transform(CCBItemModelTransformer.withPartials()).transform(CCBItemPropertiesTransformer.airtightEquipment()).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_HANDHELD_DRILL = CCB_REGISTRATE.item("incomplete_airtight_handheld_drill", SequencedAssemblyItem::new).transform(CCBItemPropertiesTransformer.incompleteAirtightEquipment()).register();
    public static final ItemEntry<AirtightHandheldDrillItem> AIRTIGHT_HANDHELD_DRILL = CCB_REGISTRATE.item("airtight_handheld_drill", p -> new AirtightHandheldDrillItem(Tiers.NETHERITE, p)).transform(CCBItemModelTransformer.withPartials()).transform(CCBItemPropertiesTransformer.airtightEquipment(ItemTags.MINING_ENCHANTABLE, ItemTags.VANISHING_ENCHANTABLE)).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_HELMET = CCB_REGISTRATE.item("incomplete_airtight_helmet", SequencedAssemblyItem::new).transform(CCBItemPropertiesTransformer.incompleteAirtightEquipment()).register();
    public static final ItemEntry<AirtightHelmetItem> AIRTIGHT_HELMET = CCB_REGISTRATE.item("airtight_helmet", AirtightHelmetItem::new).transform(CCBItemModelTransformer.airtightArmor()).transform(CCBItemPropertiesTransformer.airtightArmor(ItemTags.HEAD_ARMOR_ENCHANTABLE)).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_CHESTPLATE = CCB_REGISTRATE.item("incomplete_airtight_chestplate", SequencedAssemblyItem::new).transform(CCBItemPropertiesTransformer.incompleteAirtightEquipment()).register();
    public static final ItemEntry<AirtightChestplateItem> AIRTIGHT_CHESTPLATE = CCB_REGISTRATE.item("airtight_chestplate", AirtightChestplateItem::new).transform(CCBItemModelTransformer.airtightArmor()).transform(CCBItemPropertiesTransformer.airtightArmor(ItemTags.CHEST_ARMOR_ENCHANTABLE)).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_LEGGINGS = CCB_REGISTRATE.item("incomplete_airtight_leggings", SequencedAssemblyItem::new).transform(CCBItemPropertiesTransformer.incompleteAirtightEquipment()).register();
    public static final ItemEntry<AirtightLeggingsItem> AIRTIGHT_LEGGINGS = CCB_REGISTRATE.item("airtight_leggings", AirtightLeggingsItem::new).transform(CCBItemModelTransformer.airtightArmor()).transform(CCBItemPropertiesTransformer.airtightArmor(ItemTags.LEG_ARMOR_ENCHANTABLE)).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_BOOTS = CCB_REGISTRATE.item("incomplete_airtight_boots", SequencedAssemblyItem::new).transform(CCBItemPropertiesTransformer.incompleteAirtightEquipment()).register();
    public static final ItemEntry<AirtightBootsItem> AIRTIGHT_BOOTS = CCB_REGISTRATE.item("airtight_boots", AirtightBootsItem::new).transform(CCBItemModelTransformer.airtightArmor()).transform(CCBItemPropertiesTransformer.airtightArmor(ItemTags.FOOT_ARMOR_ENCHANTABLE)).register();

    public static final ItemEntry<SequencedAssemblyWithGasItem> INCOMPLETE_WEATHER_FLARE = CCB_REGISTRATE.item("incomplete_weather_flare", SequencedAssemblyWithGasItem::new).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<Item> UNFILLED_WEATHER_FLARE = CCB_REGISTRATE.item("unfilled_weather_flare", Item::new).transform(CCBItemPropertiesTransformer.stack16()).register();
    public static final ItemEntry<SunnyFlareItem> SUNNY_FLARE = CCB_REGISTRATE.item("sunny_flare", SunnyFlareItem::new).transform(CCBItemPropertiesTransformer.weatherFlare()).register();
    public static final ItemEntry<RainFlareItem> RAIN_FLARE = CCB_REGISTRATE.item("rain_flare", RainFlareItem::new).transform(CCBItemPropertiesTransformer.weatherFlare()).register();
    public static final ItemEntry<ThunderstormFlareItem> THUNDERSTORM_FLARE = CCB_REGISTRATE.item("thunderstorm_flare", ThunderstormFlareItem::new).transform(CCBItemPropertiesTransformer.weatherFlare()).register();
    public static final ItemEntry<SequencedAssemblyWithGasItem> INCOMPLETE_ANCHOR_FLARE = CCB_REGISTRATE.item("incomplete_anchor_flare", SequencedAssemblyWithGasItem::new).transform(CCBItemPropertiesTransformer.uncommon()).register();
    public static final ItemEntry<AnchorFlareItem> ANCHOR_FLARE = CCB_REGISTRATE.item("anchor_flare", AnchorFlareItem::new).transform(CCBItemPropertiesTransformer.anchorFlare()).register();

    static {
        CCB_REGISTRATE.setCreativeSection(CCBCreativeTabSection.PHOTO_STRESSES);
    }

    public static final ItemEntry<Item> POWDERED_AMETHYST = CCB_REGISTRATE.item("powdered_amethyst", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.DUSTS.tag, CCBItemTags.DUSTS_AMETHYST.tag)).register();
    public static final ItemEntry<Item> AMETHYST_CRYSTAL_SHEET = CCB_REGISTRATE.item("amethyst_crystal_sheet", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.PLATES_AMETHYST_CRYSTAL.tag)).register();

    static {
        CCB_REGISTRATE.setCreativeSection(CCBCreativeTabSection.ENDS);
    }

    public static final ItemEntry<Item> OBSIDIAN_CHUNK = CCB_REGISTRATE.item("obsidian_chunk", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.CHUNKS.tag)).register();
    public static final ItemEntry<Item> OBSIDIAN_BRICK = CCB_REGISTRATE.item("obsidian_brick", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.BRICKS.tag, CCBItemTags.BRICKS_OBSIDIAN.tag)).register();
    public static final ItemEntry<Item> CRYING_OBSIDIAN_CHUNK = CCB_REGISTRATE.item("crying_obsidian_chunk", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.CHUNKS.tag)).register();
    public static final ItemEntry<Item> CRYING_OBSIDIAN_BRICK = CCB_REGISTRATE.item("crying_obsidian_brick", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.BRICKS.tag, CCBItemTags.BRICKS_CRYING_OBSIDIAN.tag)).register();
    public static final ItemEntry<Item> POWDERED_CRYING_OBSIDIAN = CCB_REGISTRATE.item("powdered_crying_obsidian", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.DUSTS.tag, CCBItemTags.DUSTS_CRYING_OBSIDIAN.tag)).register();

    public static final ItemEntry<Item> CHORUS_FLOWER_POWDER = CCB_REGISTRATE.item("chorus_flower_powder", Item::new).transform(CCBItemPropertiesTransformer.tags(CCBItemTags.DUSTS.tag, CCBItemTags.DUSTS_CHORUS_FLOWER.tag)).register();

    public static final ItemEntry<Item> END_ALLOY = CCB_REGISTRATE.item("end_alloy", Item::new).transform(CCBItemPropertiesTransformer.uncommonMaterial(CCBItemTags.INGOTS_END_ALLOY.tag)).register();
    public static final ItemEntry<Item> END_ALLOY_SHEET = CCB_REGISTRATE.item("end_alloy_sheet", Item::new).transform(CCBItemPropertiesTransformer.uncommonMaterial(CCBItemTags.PLATES_END_ALLOY.tag)).register();

    static {
        CCB_REGISTRATE.setCreativeSection(CCBCreativeTabSection.DECORATIONS);
    }

    public static final ItemEntry<Item> ICE_CREAM_CONE = CCB_REGISTRATE.item("ice_cream_cone", Item::new).transform(CCBItemPropertiesTransformer.iceCreamCone()).register();
    public static final ItemEntry<Item> ICE_CREAM = CCB_REGISTRATE.item("ice_cream", Item::new).transform(CCBItemPropertiesTransformer.iceCream()).register();
    public static final ItemEntry<MilkIceCreamItem> MILK_ICE_CREAM = CCB_REGISTRATE.item("milk_ice_cream", MilkIceCreamItem::new).transform(CCBItemPropertiesTransformer.flavoredIceCream(4, 0.6f)).register();
    public static final ItemEntry<Item> BUILDERS_TEA_ICE_CREAM = CCB_REGISTRATE.item("builders_tea_ice_cream", Item::new).transform(CCBItemPropertiesTransformer.buildersTeaIceCream()).register();
    public static final ItemEntry<AmethystIceCreamItem> AMETHYST_ICE_CREAM = CCB_REGISTRATE.item("amethyst_ice_cream", AmethystIceCreamItem::new).transform(CCBItemPropertiesTransformer.flavoredIceCream(8, 0.5f)).register();
    public static final ItemEntry<HoneyIceCreamItem> HONEY_ICE_CREAM = CCB_REGISTRATE.item("honey_ice_cream", HoneyIceCreamItem::new).transform(CCBItemPropertiesTransformer.flavoredIceCream(9, 0.6f)).register();
    public static final ItemEntry<Item> CHOCOLATE_ICE_CREAM = CCB_REGISTRATE.item("chocolate_ice_cream", Item::new).transform(CCBItemPropertiesTransformer.flavoredIceCream(9, 0.8f)).register();
    public static final ItemEntry<Item> GOLDEN_ICE_CREAM = CCB_REGISTRATE.item("golden_ice_cream", Item::new).transform(CCBItemPropertiesTransformer.flavoredIceCream(8, 1.2f)).register();
    public static final ItemEntry<CreativeIceCreamItem> CREATIVE_ICE_CREAM = CCB_REGISTRATE.item("creative_ice_cream", CreativeIceCreamItem::new).transform(CCBItemPropertiesTransformer.creativeIceCream()).register();

    static {
        CCB_REGISTRATE.setCreativeSection(CCBCreativeTabSection.CANISTERS);
    }

    public static final ItemEntry<AirtightCannonWindChargeItem> NATURAL_WIND_CHARGE = CCB_REGISTRATE.item("natural_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.NATURAL_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> ULTRAWARM_WIND_CHARGE = CCB_REGISTRATE.item("ultrawarm_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.ULTRAWARM_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> ETHEREAL_WIND_CHARGE = CCB_REGISTRATE.item("ethereal_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.ETHEREAL_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> MOIST_WIND_CHARGE = CCB_REGISTRATE.item("moist_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.MOIST_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> SPORE_WIND_CHARGE = CCB_REGISTRATE.item("spore_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.SPORE_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> SCULK_WIND_CHARGE = CCB_REGISTRATE.item("sculk_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.SCULK_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> ENERGIZED_NATURAL_WIND_CHARGE = CCB_REGISTRATE.item("energized_natural_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.ENERGIZED_NATURAL_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> ENERGIZED_ULTRAWARM_WIND_CHARGE = CCB_REGISTRATE.item("energized_ultrawarm_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.ENERGIZED_ULTRAWARM_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> ENERGIZED_ETHEREAL_WIND_CHARGE = CCB_REGISTRATE.item("energized_ethereal_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.ENERGIZED_ETHEREAL_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<AirtightCannonWindChargeItem> CREATIVE_WIND_CHARGE = CCB_REGISTRATE.item("creative_wind_charge", p -> new AirtightCannonWindChargeItem(p, CCBGases.CREATIVE_AIR)).transform(CCBItemPropertiesTransformer.defaultProperties()).register();

    public static final ItemEntry<BalloonItem> BALLOON_10X8 = CCB_REGISTRATE.item("balloon_10x8", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_10_8, false)).transform(CCBItemModelTransformer.balloon(BalloonStyleUtils.BALLOON_10_8)).transform(CCBItemPropertiesTransformer.balloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_10X12 = CCB_REGISTRATE.item("balloon_10x12", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_10_12, false)).transform(CCBItemModelTransformer.balloon(BalloonStyleUtils.BALLOON_10_12)).transform(CCBItemPropertiesTransformer.balloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_12X10 = CCB_REGISTRATE.item("balloon_12x10", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_12_10, false)).transform(CCBItemModelTransformer.balloon(BalloonStyleUtils.BALLOON_12_10)).transform(CCBItemPropertiesTransformer.balloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_12X12 = CCB_REGISTRATE.item("balloon_12x12", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_12_12, false)).transform(CCBItemModelTransformer.balloon(BalloonStyleUtils.BALLOON_12_12)).transform(CCBItemPropertiesTransformer.balloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_REVERTED = CCB_REGISTRATE.item("balloon_rare_reverted", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_RARE_REVERTED, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyleUtils.BALLOON_RARE_REVERTED)).transform(CCBItemPropertiesTransformer.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_SMILE = CCB_REGISTRATE.item("balloon_rare_smile", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_RARE_SMILE, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyleUtils.BALLOON_RARE_SMILE)).transform(CCBItemPropertiesTransformer.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_CRY = CCB_REGISTRATE.item("balloon_rare_cry", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_RARE_CRY, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyleUtils.BALLOON_RARE_CRY)).transform(CCBItemPropertiesTransformer.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_EYE = CCB_REGISTRATE.item("balloon_rare_eye", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_RARE_EYE, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyleUtils.BALLOON_RARE_EYE)).transform(CCBItemPropertiesTransformer.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_ISAAC = CCB_REGISTRATE.item("balloon_rare_isaac", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_RARE_ISAAC, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyleUtils.BALLOON_RARE_ISAAC)).transform(CCBItemPropertiesTransformer.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_GHAST = CCB_REGISTRATE.item("balloon_rare_ghast", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_RARE_GHAST, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyleUtils.BALLOON_RARE_GHAST)).transform(CCBItemPropertiesTransformer.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_TROLLFACE = CCB_REGISTRATE.item("balloon_rare_trollface", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_RARE_TROLLFACE, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyleUtils.BALLOON_RARE_TROLLFACE)).transform(CCBItemPropertiesTransformer.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_TENNA = CCB_REGISTRATE.item("balloon_rare_tenna", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_RARE_TENNA, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyleUtils.BALLOON_RARE_TENNA)).transform(CCBItemPropertiesTransformer.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_PVZ = CCB_REGISTRATE.item("balloon_rare_pvz", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_RARE_PVZ, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyleUtils.BALLOON_RARE_PVZ)).transform(CCBItemPropertiesTransformer.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_QUESTION_MARKS = CCB_REGISTRATE.item("balloon_rare_question_marks", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_RARE_QUESTION_MARKS, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyleUtils.BALLOON_RARE_QUESTION_MARKS)).transform(CCBItemPropertiesTransformer.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_POWERFUL = CCB_REGISTRATE.item("balloon_rare_powerful", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_RARE_POWERFUL, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyleUtils.BALLOON_RARE_POWERFUL)).transform(CCBItemPropertiesTransformer.rareBalloon()).register();
    public static final ItemEntry<BalloonItem> BALLOON_RARE_CHEESE = CCB_REGISTRATE.item("balloon_rare_cheese", p -> new BalloonItem(p, BalloonStyleUtils.BALLOON_RARE_CHEESE, true)).transform(CCBItemModelTransformer.rareBalloon(BalloonStyleUtils.BALLOON_RARE_CHEESE)).transform(CCBItemPropertiesTransformer.rareBalloon()).register();

    public static final ItemEntry<GasVirtualItem> GAS_VIRTUAL_ITEM = CCB_REGISTRATE.item("gas_virtual_item", GasVirtualItem::new).transform(CCBItemPropertiesTransformer.stack1()).register();

    public static final ItemEntry<GasFilterItem> GAS_FILTER = CCB_REGISTRATE.item("gas_filter", GasFilterItem::new).transform(CCBItemPropertiesTransformer.defaultProperties()).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_GAS_CANISTER_PACK = CCB_REGISTRATE.item("incomplete_gas_canister_pack", SequencedAssemblyItem::new).transform(CCBItemPropertiesTransformer.fireResistant()).register();
    public static final ItemEntry<GasCanisterPackItem> GAS_CANISTER_PACK = CCB_REGISTRATE.item("gas_canister_pack", GasCanisterPackItem::new).transform(CCBItemModelTransformer.gasCanisterPack()).transform(CCBItemPropertiesTransformer.gasCanisterPack()).register();

    public static final ItemEntry<GasCanisterBlockItem> GAS_CANISTER_PLACEABLE = CCB_REGISTRATE.item("gas_canister_placeable", p -> new GasCanisterBlockItem(CCBBlocks.GAS_CANISTER_BLOCK.get(), CCBItems.GAS_CANISTER::get, p)).transform(CCBItemModelTransformer.gasCanister()).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<GasCanisterItem> GAS_CANISTER = CCB_REGISTRATE.item("gas_canister", p -> new GasCanisterItem(p, GAS_CANISTER_PLACEABLE)).transform(CCBItemPropertiesTransformer.gasCanister()).register();

    public static final ItemEntry<CreativeGasCanisterBlockItem> CREATIVE_GAS_CANISTER_PLACEABLE = CCB_REGISTRATE.item("creative_gas_canister_placeable", p -> new CreativeGasCanisterBlockItem(CCBBlocks.CREATIVE_GAS_CANISTER_BLOCK.get(), CCBItems.CREATIVE_GAS_CANISTER::get, p)).transform(CCBItemModelTransformer.creativeGasCanister()).transform(CCBItemPropertiesTransformer.defaultProperties()).register();
    public static final ItemEntry<CreativeGasCanisterItem> CREATIVE_GAS_CANISTER = CCB_REGISTRATE.item("creative_gas_canister", p -> new CreativeGasCanisterItem(p, CREATIVE_GAS_CANISTER_PLACEABLE)).transform(CCBItemPropertiesTransformer.creativeGasCanister()).register();

    public static void register() {
    }
}
