package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.compressedair.CompressedAirCanisterItem;
import net.ty.createcraftedbeginning.content.icecreams.CreativeIceCreamItem;
import net.ty.createcraftedbeginning.content.icecreams.HoneyIceCreamItem;
import net.ty.createcraftedbeginning.content.icecreams.MilkIceCreamItem;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import net.ty.createcraftedbeginning.data.CCBTags;
import net.ty.createcraftedbeginning.data.CCBTags.CCBItemTags;

public class CCBItems {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate();

    public static final ItemEntry<Item> CINDER_ALLOY = CCB_REGISTRATE.item("cinder_alloy", Item::new).properties(p -> p.stacksTo(64).fireResistant()).tag(CCBItemTags.CINDER_ALLOY.tag).register();

    public static final ItemEntry<Item> AMETHYST_CRYSTAL_SHEET = CCB_REGISTRATE.item("amethyst_crystal_sheet", Item::new).properties(p -> p.stacksTo(64)).tag(CCBItemTags.AMETHYST_CRYSTAL_PLATE.tag).register();

    public static final ItemEntry<Item> AIRTIGHT_SHEET = CCB_REGISTRATE.item("airtight_sheet", Item::new).properties(p -> p.stacksTo(64)).register();

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_AIRTIGHT_SHEET = CCB_REGISTRATE.item("incomplete_airtight_sheet", SequencedAssemblyItem::new).register();

    public static final ItemEntry<Item> ICE_CREAM_CONE = CCB_REGISTRATE.item("ice_cream_cone", Item::new).properties(p -> p.food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6F).build())).register();

    public static final ItemEntry<Item> ICE_CREAM = CCB_REGISTRATE.item("ice_cream", Item::new).properties(p -> p.stacksTo(16).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.5F).build())).register();

    public static final ItemEntry<MilkIceCreamItem> MILK_ICE_CREAM = CCB_REGISTRATE.item("milk_ice_cream", MilkIceCreamItem::new).properties(p -> p.stacksTo(16).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.5F).build())).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).register();

    public static final ItemEntry<Item> CHOCOLATE_ICE_CREAM = CCB_REGISTRATE.item("chocolate_ice_cream", Item::new).properties(p -> p.stacksTo(16).food(new FoodProperties.Builder().nutrition(9).saturationModifier(0.8F).build())).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).register();

    public static final ItemEntry<HoneyIceCreamItem> HONEY_ICE_CREAM = CCB_REGISTRATE.item("honey_ice_cream", HoneyIceCreamItem::new).properties(p -> p.stacksTo(16).food(new FoodProperties.Builder().nutrition(9).saturationModifier(0.6F).build())).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).register();

    public static final ItemEntry<CreativeIceCreamItem> CREATIVE_ICE_CREAM = CCB_REGISTRATE.item("creative_ice_cream", CreativeIceCreamItem::new).properties(p -> p.stacksTo(16).rarity(Rarity.EPIC).food(new FoodProperties.Builder().nutrition(0).saturationModifier(0F).build())).register();

    public static final ItemEntry<? extends CompressedAirCanisterItem> COMPRESSED_AIR_CANISTER = CCB_REGISTRATE.item("compressed_air_canister", p -> new CompressedAirCanisterItem(CCBFluids.MEDIUM_PRESSURE_COMPRESSED_AIR.get(), CCBTags.CCBFluidTags.MEDIUM_PRESSURE_COMPRESSED_AIR.tag, p)).properties(prop -> prop.stacksTo(1)).register();

    public static final ItemEntry<? extends CompressedAirCanisterItem> HIGH_PRESSURE_COMPRESSED_AIR_CANISTER = CCB_REGISTRATE.item("high_pressure_compressed_air_canister", p -> new CompressedAirCanisterItem(CCBFluids.HIGH_PRESSURE_COMPRESSED_AIR.get(), CCBTags.CCBFluidTags.HIGH_PRESSURE_COMPRESSED_AIR.tag, p)).properties(p -> p.stacksTo(1)).register();

    public static void register() {
    }
}
