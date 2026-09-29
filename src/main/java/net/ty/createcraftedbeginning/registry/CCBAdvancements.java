package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.AllItems;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.PathProvider;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.ty.createcraftedbeginning.advancement.CCBAdvancement;
import net.ty.createcraftedbeginning.advancement.CCBAdvancement.Builder;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

import static net.ty.createcraftedbeginning.advancement.CCBAdvancement.TaskType.GOAL;
import static net.ty.createcraftedbeginning.advancement.CCBAdvancement.TaskType.HIDDEN_GOAL;
import static net.ty.createcraftedbeginning.advancement.CCBAdvancement.TaskType.HIDDEN_TASK;

@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAdvancements implements DataProvider {
    public static final CCBAdvancement ROOT = create("root", builder -> builder.icon(CCBBlocks.BREEZE_COOLER_BLOCK).title("Welcome to Create: Crafted Beginning").description("A brand new experience").awardedForFree().special(HIDDEN_TASK));

    public static final CCBAdvancement END_FIELD = create("end_field", builder -> builder.icon(CCBItems.END_ALLOY).title("End Field").description("Obtain a brand-new ductile alloy").whenIconCollected().after(ROOT));
    public static final CCBAdvancement THE_INTEGRATED_INDUSTRY_AGE = create("the_integrated_industry_age", builder -> builder.icon(CCBBlocks.END_CASING_BLOCK).title("The Integrated Industry Age").description("Use End Alloy on Crying Obsidian to create an expandable casing for your machines").after(END_FIELD));
    public static final CCBAdvancement HOT_HOT_HOT = create("hot_hot_hot", builder -> builder.icon(CCBBlocks.END_INCINERATION_BLOWER_BLOCK).title("Hot, Hot, Hot!").description("Attach an End Incineration Blower to an End Casing, then burn any old thing").after(THE_INTEGRATED_INDUSTRY_AGE));
    public static final CCBAdvancement WARM_HEARTED = create("warm_hearted", builder -> builder.icon(Blocks.SNOW_BLOCK).title("Warm-Hearted").description("Warm up a Snow Golem with an End Incineration Blower").special(HIDDEN_GOAL).after(HOT_HOT_HOT));
    public static final CCBAdvancement STEVES_REDEMPTION = create("steves_redemption", builder -> builder.icon(CCBBlocks.END_SCULK_SILENCER_BLOCK).title("Steve's Redemption").description("Attach an End Sculk Silencer to an End Casing, escaping the grasp of the Warden").after(THE_INTEGRATED_INDUSTRY_AGE));

    public static final CCBAdvancement SHINY_DUST = create("shiny_dust", builder -> builder.icon(CCBItems.POWDERED_AMETHYST).title("Shiny Dust").description("Obtain a Powdered Amethyst").whenIconCollected().after(ROOT));
    public static final CCBAdvancement NATURAL_EVAPORATION = create("natural_evaporation", builder -> builder.icon(CCBFluids.AMETHYST_SUSPENSION.getBucket().orElseThrow()).title("Natural Evaporation").description("Place a bucket of Amethyst Suspension in the Nether\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(SHINY_DUST));
    public static final CCBAdvancement CRYSTAL_UNION = create("crystal_union", builder -> builder.icon(CCBItems.AMETHYST_CRYSTAL_SHEET).title("Crystal Union").description("Obtain an Amethyst Crystal Sheet").whenIconCollected().after(SHINY_DUST));
    public static final CCBAdvancement PRE_CRUSHING = create("pre_crushing", builder -> builder.icon(CCBItems.OBSIDIAN_CHUNK).title("Pre-Crushing").description("Obtain an Obsidian Chunk or a Crying Obsidian Chunk by pressing").whenItemCollected(CCBItemTags.CHUNKS.tag).after(SHINY_DUST));
    public static final CCBAdvancement STONE_COLD_TEARS = create("stone_cold_tears", builder -> builder.icon(CCBItems.POWDERED_CRYING_OBSIDIAN).title("Stone-Cold Tears").description("Obtain Powdered Crying Obsidian through spout filling").whenIconCollected().after(PRE_CRUSHING));
    public static final CCBAdvancement ROCK_SOLID_WISDOM = create("rock_solid_wisdom", builder -> builder.icon(CCBBlocks.CRYING_OBSIDIAN_BRICKS_M).title("Rock-Solid Wisdom").description("Obtain all types of Crying Obsidian Bricks with texts\n§7(Hidden Advancement)").special(HIDDEN_GOAL).whenItemsCollected(new ArrayList<>(List.of(CCBBlocks.CRYING_OBSIDIAN_BRICKS_A, CCBBlocks.CRYING_OBSIDIAN_BRICKS_B, CCBBlocks.CRYING_OBSIDIAN_BRICKS_C, CCBBlocks.CRYING_OBSIDIAN_BRICKS_D, CCBBlocks.CRYING_OBSIDIAN_BRICKS_E, CCBBlocks.CRYING_OBSIDIAN_BRICKS_F, CCBBlocks.CRYING_OBSIDIAN_BRICKS_G, CCBBlocks.CRYING_OBSIDIAN_BRICKS_H, CCBBlocks.CRYING_OBSIDIAN_BRICKS_I, CCBBlocks.CRYING_OBSIDIAN_BRICKS_J, CCBBlocks.CRYING_OBSIDIAN_BRICKS_K, CCBBlocks.CRYING_OBSIDIAN_BRICKS_L, CCBBlocks.CRYING_OBSIDIAN_BRICKS_M, CCBBlocks.CRYING_OBSIDIAN_BRICKS_N, CCBBlocks.CRYING_OBSIDIAN_BRICKS_O, CCBBlocks.CRYING_OBSIDIAN_BRICKS_P, CCBBlocks.CRYING_OBSIDIAN_BRICKS_Q, CCBBlocks.CRYING_OBSIDIAN_BRICKS_R, CCBBlocks.CRYING_OBSIDIAN_BRICKS_S, CCBBlocks.CRYING_OBSIDIAN_BRICKS_T, CCBBlocks.CRYING_OBSIDIAN_BRICKS_U, CCBBlocks.CRYING_OBSIDIAN_BRICKS_V, CCBBlocks.CRYING_OBSIDIAN_BRICKS_W, CCBBlocks.CRYING_OBSIDIAN_BRICKS_X, CCBBlocks.CRYING_OBSIDIAN_BRICKS_Y, CCBBlocks.CRYING_OBSIDIAN_BRICKS_Z))).after(STONE_COLD_TEARS));

    public static final CCBAdvancement PLYWOOD = create("plywood", builder -> builder.icon(CCBItems.AIRTIGHT_SHEET).title("Plywood").description("Assemble an Airtight Sheet").whenIconCollected().after(STONE_COLD_TEARS));
    public static final CCBAdvancement GASEOUS_VARIATIONS = create("gaseous_variations", builder -> builder.icon(ignored -> VirtualGasItems.createVirtualItem(new GasStack(CCBGases.SPORE_AIR, 1))).title("Gaseous Variations").description("Attempt to extract Spore Air").special(GOAL).after(PLYWOOD));
    public static final CCBAdvancement GLACIOLOGIST = create("glaciologist", builder -> builder.icon(CCBItems.BUILDERS_TEA_ICE_CREAM).title("Glaciologist").description("Obtain every type of ice cream").special(GOAL).whenItemsCollected(new ArrayList<>(List.of(CCBItems.ICE_CREAM, CCBItems.MILK_ICE_CREAM, CCBItems.BUILDERS_TEA_ICE_CREAM, CCBItems.AMETHYST_ICE_CREAM, CCBItems.HONEY_ICE_CREAM, CCBItems.CHOCOLATE_ICE_CREAM, CCBItems.GOLDEN_ICE_CREAM))).after(GASEOUS_VARIATIONS));
    public static final CCBAdvancement LUXURY_TREAT = create("luxury_treat", builder -> builder.icon(Items.ENCHANTED_GOLDEN_APPLE).title("Luxury Treat").description("Feed an Enchanted Golden Apple to a Breeze Chamber").special(GOAL).after(GLACIOLOGIST));
    public static final CCBAdvancement SKY_IS_NOT_THE_LIMIT = create("sky_is_not_the_limit", builder -> builder.icon(CCBItems.AIRTIGHT_CHESTPLATE).title("Sky Is Not the Limit").description("Fly to the build height limit while wearing an Airtight Chestplate").special(GOAL).after(LUXURY_TREAT));
    public static final CCBAdvancement FLYWHEEL = create("flywheel", builder -> builder.icon(CCBBlocks.AIRTIGHT_ENGINE_BLOCK).title("Flywheel").description("Run an Airtight Engine at maximum power").special(GOAL).after(SKY_IS_NOT_THE_LIMIT));
    public static final CCBAdvancement MIRACLE_OF_ENGINEERING = create("miracle_of_engineering", builder -> builder.icon(CCBBlocks.TESLA_TURBINE_BLOCK).title("Miracle of Engineering").description("Run a Tesla Turbine at maximum power").special(GOAL).after(FLYWHEEL));

    public static final CCBAdvancement GAS_INDUSTRY_FROM_SCRATCH = create("gas_industry_from_scratch", builder -> builder.icon(CCBBlocks.AIRTIGHT_PIPE_BLOCK).title("Gas Industry from Scratch").description("Craft any component used to transport gas").whenAnyItemCollected(CCBBlocks.AIRTIGHT_PIPE_BLOCK, CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK, CCBBlocks.AIRTIGHT_CHECK_VALVE_BLOCK, CCBBlocks.AIRTIGHT_VALVE_BLOCK, CCBBlocks.SMART_AIRTIGHT_PIPE_BLOCK, CCBBlocks.AIRTIGHT_PUMP_BLOCK, CCBBlocks.AIRTIGHT_REGULATOR_PUMP_BLOCK, CCBBlocks.AIRTIGHT_FLOWMETER_BLOCK, CCBBlocks.AIRTIGHT_MANOMETER_BLOCK, CCBBlocks.PORTABLE_GAS_INTERFACE_BLOCK).after(PLYWOOD));
    public static final CCBAdvancement BRONCHI = create("bronchi", builder -> builder.icon(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK).title("Bronchi").description("Obtain an Airtight Encased Pipe to build a more flexible gas network").whenIconCollected().after(GAS_INDUSTRY_FROM_SCRATCH));
    public static final CCBAdvancement TAKE_A_DEEP_BREATH = create("take_a_deep_breath", builder -> builder.icon(CCBBlocks.AIRTIGHT_PUMP_BLOCK).title("Take a Deep Breath").description("Place and power an Airtight Pump or Airtight Regulator Pump to create a pressure difference in your gas network").after(BRONCHI));
    public static final CCBAdvancement YOU_SHALL_NOT_PASS = create("you_shall_not_pass", builder -> builder.icon(CCBBlocks.AIRTIGHT_CHECK_VALVE_BLOCK).title("You Shall Not Pass!").description("Obtain an Airtight Check Valve").whenIconCollected().after(TAKE_A_DEEP_BREATH));
    public static final CCBAdvancement GAS_FILTRATION = create("gas_filtration", builder -> builder.icon(CCBBlocks.SMART_AIRTIGHT_PIPE_BLOCK).title("Gas Filtration").description("Obtain a Smart Airtight Pipe").whenIconCollected().after(YOU_SHALL_NOT_PASS));
    public static final CCBAdvancement VISUAL_MONITORING = create("visual_monitoring", builder -> builder.icon(CCBItems.AIRTIGHT_METER).title("Visual Monitoring").description("Install an Airtight Meter on a gas tank, or observe a reading change on a Flowmeter or Manometer connected to your gas network").after(YOU_SHALL_NOT_PASS));
    public static final CCBAdvancement ALVEOLI = create("alveoli", builder -> builder.icon(CCBBlocks.GAS_PACKAGER_BLOCK).title("Alveoli").description("Use a Gas Packager to package gas from storage").after(VISUAL_MONITORING));
    public static final CCBAdvancement A_GUIDE_TO_OTHERWORLDLY_LOGISTICS = create("a_guide_to_otherworldly_logistics", builder -> builder.icon(CCBBlocks.GAS_UNPACKAGER_BLOCK).title("A Guide to Otherworldly Logistics").description("Use a Gas Unpackager to unpack a balloon containing gas from another dimension").after(ALVEOLI));
    public static final CCBAdvancement MINTY_FIREWORKS = create("minty_fireworks", builder -> builder.icon(ignored -> VirtualGasItems.createVirtualItem(new GasStack(CCBGases.ETHEREAL_AIR, 1))).title("Minty Fireworks").description("After visiting the End, release a balloon containing Ethereal Air in the Overworld\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(A_GUIDE_TO_OTHERWORLDLY_LOGISTICS));
    public static final CCBAdvancement SMART_GAS_COLLECTION = create("smart_gas_collection", builder -> builder.icon(CCBBlocks.GAS_FACTORY_GAUGE_BLOCK).title("Smart Gas Collection").description("Use a Gas Factory Gauge to automate balloon packaging").after(A_GUIDE_TO_OTHERWORLDLY_LOGISTICS));

    public static final CCBAdvancement LIVING_FREEZER = create("living_freezer", builder -> builder.icon(CCBBlocks.BREEZE_COOLER_BLOCK).title("Living Freezer").description("Obtain a Breeze Cooler").whenIconCollected().after(SMART_GAS_COLLECTION));
    public static final CCBAdvancement REASSIGNMENT_PROTOCOL = create("reassignment_protocol", builder -> builder.icon(CCBBlocks.BREEZE_CHAMBER_BLOCK).title("Reassignment Protocol").description("Convert a Breeze Cooler into a Breeze Chamber").whenIconCollected().after(LIVING_FREEZER));
    public static final CCBAdvancement EMERGING_POWER = create("emerging_power", builder -> builder.icon(CCBBlocks.AIRTIGHT_ENGINE_BLOCK).title("Emerging Power").description("Build an Airtight Assembly Driver and generate power with an Airtight Engine").after(REASSIGNMENT_PROTOCOL));
    public static final CCBAdvancement THERMAL_CYCLE = create("thermal_cycle", builder -> builder.icon(CCBBlocks.BOILER_STEAM_OUTLET_BLOCK).title("Thermal Cycle").description("Connect a Boiler Steam Outlet to an Airtight Assembly Driver and turn boiler steam into power").after(EMERGING_POWER));
    public static final CCBAdvancement DEBRIS_CRAFT = create("debris_craft", builder -> builder.icon(CCBItems.TESLA_TURBINE_ROTOR).title("Debris Craft").description("Assemble a Tesla Turbine Rotor").whenIconCollected().after(REASSIGNMENT_PROTOCOL));
    public static final CCBAdvancement GENIUS_ENGINEER = create("genius_engineer", builder -> builder.icon(CCBBlocks.TESLA_TURBINE_BLOCK).title("Genius Engineer").description("Generate power with a Tesla Turbine; remember to check the direction of gas flow").after(DEBRIS_CRAFT));
    public static final CCBAdvancement TESLA_TURBINE_EASY_AS_PIE = create("tesla_turbine_easy_as_pie", builder -> builder.icon(CCBBlocks.TESLA_TURBINE_BLOCK).title("Tesla Turbine? Easy as Pie!").description("Fill more than one type of gas into a Tesla Turbine and make it explode\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(GENIUS_ENGINEER));
    public static final CCBAdvancement BAD_APPLE = create("bad_apple", builder -> builder.icon(Items.PUFFERFISH).title("Bad Apple").description("Feed improper food to a Breeze Chamber\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(REASSIGNMENT_PROTOCOL));
    public static final CCBAdvancement IS_THIS_EVEN_SCIENTIFIC = create("is_this_even_scientific", builder -> builder.icon(Items.MILK_BUCKET).title("Is This Even Scientific?").description("Use Milk to clear the Ill state of a Breeze Chamber - is this even scientific?\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(BAD_APPLE));
    public static final CCBAdvancement A_MURDER = create("a_murder", builder -> builder.icon(Items.LAVA_BUCKET).title("A Murder").description("Pump lava or an even hotter fluid into a Breeze Cooler\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(LIVING_FREEZER));

    public static final CCBAdvancement FROZEN_AMBROSIA = create("frozen_ambrosia", builder -> builder.icon(CCBItems.ICE_CREAM).title("Frozen Ambrosia").description("Feed a serving of Ice Cream to a Breeze Cooler").after(LIVING_FREEZER));
    public static final CCBAdvancement HUFF_HUFF_HUFF = create("huff_huff_huff", builder -> builder.icon(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK).title("Huff, Huff, Huff").description("Assemble a Gas Injection Chamber to process items with gas injection").whenIconCollected().after(FROZEN_AMBROSIA));
    public static final CCBAdvancement CLANK_CLANK_CLANK = create("clank_clank_clank", builder -> builder.icon(CCBItems.GAS_INJECTION_CHAMBER_FILTER).title("Clank, Clank, Clank").description("Install a filter on a Gas Injection Chamber for a better processing experience than a whirring Encased Fan").after(HUFF_HUFF_HUFF));
    public static final CCBAdvancement COLLISION_COURSE = create("collision_course", builder -> builder.icon(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK).title("Collision Course").description("Assemble an Airtight Reactor Kettle").whenIconCollected().after(FROZEN_AMBROSIA));
    public static final CCBAdvancement BUNDLE_OF_JOY = create("bundle_of_joy", builder -> builder.icon(AllItems.ANDESITE_ALLOY).title("Bundle Of Joy").description("Obtain Andesite Alloy using an Airtight Reactor Kettle").after(COLLISION_COURSE));
    public static final CCBAdvancement BEAT_INTO_SHAPE = create("beat_into_shape", builder -> builder.icon(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK).title("Beat Into Shape").description("Assemble an Airtight Forging Press").whenIconCollected().after(FROZEN_AMBROSIA));
    public static final CCBAdvancement SUPERMASSIVE = create("supermassive", builder -> builder.icon(Items.DIAMOND).title("Supermassive").description("Witness the remarkable transformation of carbon in an Airtight Forging Press").after(BEAT_INTO_SHAPE));
    public static final CCBAdvancement DISSOCIATIVE_RECOMBINATION = create("dissociative_recombination", builder -> builder.icon(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL).title("Dissociative Recombination").description("Build an Airtight Fractionation Tower to separate substances by heated distillation or low-temperature condensation").after(FROZEN_AMBROSIA));
    public static final CCBAdvancement BREAK_THROUGH_THE_DOME = create("break_through_the_dome", builder -> builder.icon(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL).title("Break Through the Dome").description("Build an Airtight Fractionation Tower at its maximum height").after(DISSOCIATIVE_RECOMBINATION));

    public static final CCBAdvancement BETTER_THAN_A_BACKTANK = create("better_than_a_backtank", builder -> builder.icon(CCBItems.GAS_CANISTER).title("Better than a Backtank").description("Obtain a Gas Canister").whenIconCollected().after(PLYWOOD));
    public static final CCBAdvancement NEW_ENERGY_BATTERY_PACK = create("new_energy_battery_pack", builder -> builder.icon(CCBItems.GAS_CANISTER_PACK).title("New Energy Battery Pack").description("Assemble a Gas Canister Pack").whenIconCollected().after(BETTER_THAN_A_BACKTANK));
    public static final CCBAdvancement UPDRAFT = create("updraft", builder -> builder.icon(CCBItems.BREEZE_CORE).title("Updraft").description("Assemble a Breeze Core").whenIconCollected().after(NEW_ENERGY_BATTERY_PACK));
    public static final CCBAdvancement WIND_CHARGED = create("wind_charged", builder -> builder.icon(CCBItems.AIRTIGHT_CANNON).title("Wind-Charged").description("Defeat a mob with an Airtight Cannon").after(UPDRAFT));
    public static final CCBAdvancement LOOKS_LIKE_THE_WEATHERS_CLEARING_UP = create("looks_like_the_weathers_clearing_up", builder -> builder.icon(CCBItems.SUNNY_FLARE).title("Looks Like the Weather's Clearing Up").description("Fire a Sunny Flare with an Airtight Cannon to skip a rainfall").after(WIND_CHARGED));
    public static final CCBAdvancement I_AM_THE_STORM_THAT_IS_APPROACHING = create("i_am_the_storm_that_is_approaching", builder -> builder.icon(CCBItems.THUNDERSTORM_FLARE).title("I Am the Storm That Is Approaching").description("Create an endless thunderstorm\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(LOOKS_LIKE_THE_WEATHERS_CLEARING_UP));
    public static final CCBAdvancement WHO_IS_THE_BREEZE_NOW = create("who_is_the_breeze_now", builder -> builder.icon(CCBItems.ENERGIZED_NATURAL_WIND_CHARGE).title("Who Is the Breeze Now?").description("Defeat a Breeze with an Airtight Cannon\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(WIND_CHARGED));
    public static final CCBAdvancement MINI_TUNNEL_BORER = create("mini_tunnel_borer", builder -> builder.icon(CCBItems.AIRTIGHT_HANDHELD_DRILL).title("Mini Tunnel Borer").description("Mine at least 64 blocks in a single operation using an Airtight Handheld Drill").after(UPDRAFT));
    public static final CCBAdvancement EVEN_HARDER_THAN_OBSIDIAN = create("even_harder_than_obsidian", builder -> builder.icon(Blocks.REINFORCED_DEEPSLATE).title("Even Harder Than Obsidian").description("Mine a Reinforced Deepslate with an Airtight Handheld Drill\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(MINI_TUNNEL_BORER));
    public static final CCBAdvancement ALL_HANDS_ACQUIRED = create("all_hands_acquired", builder -> builder.icon(CCBItems.AIRTIGHT_EXTEND_ARM).title("All Hands Acquired").description("Assemble an Airtight Extend Arm").whenIconCollected().after(UPDRAFT));
    public static final CCBAdvancement THREE_WAY_HANDSHAKE = create("three_way_handshake", builder -> builder.icon(CCBItems.AIRTIGHT_EXTEND_ARM).title("Three-way Handshake").description("Hold two Airtight Extend Arms at the same time\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(ALL_HANDS_ACQUIRED));
    public static final CCBAdvancement SEALED_TO_PERFECTION = create("sealed_to_perfection", builder -> builder.icon(CCBItems.AIRTIGHT_HELMET).title("Sealed to Perfection").description("Wear a full set of Airtight Armor").after(UPDRAFT));
    public static final CCBAdvancement PHANTOM_DIVERS = create("phantom_divers", builder -> builder.icon(CCBItems.AIRTIGHT_BOOTS).title("Phantom Divers").description("Enable every upgrade on a full set of Airtight Armor\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(SEALED_TO_PERFECTION));

    public static final CCBAdvancement ITEM_TANK = create("item_tank", builder -> builder.icon(CCBBlocks.ANDESITE_CRATE_BLOCK).title("Item Tank").description("Obtain an Andesite Crate").whenIconCollected().after(ROOT));
    public static final CCBAdvancement CAPACITY_UPGRADE = create("capacity_upgrade", builder -> builder.icon(CCBBlocks.BRASS_CRATE_BLOCK).title("Capacity Upgrade").description("Obtain a Brass Crate").whenIconCollected().after(ITEM_TANK));
    public static final CCBAdvancement ULTIMATE_STORAGE_STRATEGY = create("ultimate_storage_strategy", builder -> builder.icon(CCBBlocks.STURDY_CRATE_BLOCK).title("Ultimate Storage Strategy").description("Obtain a Sturdy Crate").whenIconCollected().after(CAPACITY_UPGRADE));
    public static final CCBAdvancement TO_VOID = create("to_void", builder -> builder.icon(CCBBlocks.CARDBOARD_CRATE_BLOCK).title("To: Void").description("Obtain a Cardboard Crate").whenIconCollected().after(ULTIMATE_STORAGE_STRATEGY));
    public static final CCBAdvancement CUT_FROM_THE_SAME_CARDBOARD = create("cut_from_the_same_cardboard", builder -> builder.icon(CCBBlocks.CARDBOARD_CRATE_BLOCK).title("Cut from the Same Cardboard").description("Dispose a Cardboard Package with a Cardboard Crate\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(TO_VOID));
    public static final CCBAdvancement A_HOUSE_OF_GOLD_IN_THE_CRATE = create("a_house_of_gold_in_the_crate", builder -> builder.icon(Items.GOLD_INGOT).title("A House of Gold in the Crate").description("Fill a Brass Crate with Gold Ingots").special(GOAL).after(ITEM_TANK));
    public static final CCBAdvancement PORTABLE_LAVA_SEA = create("portable_lava_sea", builder -> builder.icon(Items.LAVA_BUCKET).title("Portable Lava Sea").description("Store at least 10,000 Lava Buckets in a Sturdy Crate").special(GOAL).after(A_HOUSE_OF_GOLD_IN_THE_CRATE));

    private final PackOutput output;
    private final CompletableFuture<Provider> registries;

    public CCBAdvancements(PackOutput output, CompletableFuture<Provider> registries) {
        this.output = output;
        this.registries = registries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registries.thenCompose(provider -> {
            PathProvider pathProvider = output.createPathProvider(Target.DATA_PACK, "advancement");
            List<CompletableFuture<?>> futures = new ArrayList<>();
            CCBAdvancement.all().forEach(advancement -> advancement.save(holder -> futures.add(DataProvider.saveStable(cache, provider, Advancement.CODEC, holder.value(), pathProvider.json(holder.id()))), provider));
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public String getName() {
        return "Create: Crafted Beginning's Advancements";
    }

    public static void provideLang(BiConsumer<String, String> consumer) {
        CCBAdvancement.all().forEach(advancement -> advancement.provideLang(consumer));
    }

    @Contract("_, _ -> new")
    private static CCBAdvancement create(String id, UnaryOperator<Builder> operator) {
        return new CCBAdvancement(id, operator);
    }
}
