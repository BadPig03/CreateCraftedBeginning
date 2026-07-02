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
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualItem;
import net.ty.createcraftedbeginning.data.CCBGases;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
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

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBAdvancements implements DataProvider {
    public static final List<CCBAdvancement> ENTRIES = new ArrayList<>();
    public static final CCBAdvancement ROOT = create("root", b -> b.icon(CCBBlocks.BREEZE_COOLER_BLOCK).title("Welcome to Create: Crafted Beginning").description("A brand new experience").awardedForFree().special(HIDDEN_TASK));

    public static final CCBAdvancement END_FIELD = create("end_field", b -> b.icon(CCBItems.END_ALLOY).title("End Field").description("Obtain a brand-new ductile alloy").whenIconCollected().after(ROOT));
    public static final CCBAdvancement THE_INTEGRATED_INDUSTRY_AGE = create("the_integrated_industry_age", b -> b.icon(CCBBlocks.END_CASING_BLOCK).title("The Integrated Industry Age").description("Use End Alloy on Crying Obsidian to create an expandable casing for your machines").after(END_FIELD));
    public static final CCBAdvancement HOT_HOT_HOT = create("hot_hot_hot", b -> b.icon(CCBBlocks.END_INCINERATION_BLOWER_BLOCK).title("Hot, Hot, Hot!").description("Attach an End Incineration Blower to an End Casing, then burn any old thing").after(THE_INTEGRATED_INDUSTRY_AGE));
    public static final CCBAdvancement WARM_HEARTED = create("warm_hearted", b -> b.icon(Blocks.SNOW_BLOCK).title("Warm-Hearted").description("Warm up a Snow Golem with an End Incineration Blower").special(HIDDEN_GOAL).after(HOT_HOT_HOT));
    public static final CCBAdvancement STEVES_REDEMPTION = create("steves_redemption", b -> b.icon(CCBBlocks.END_SCULK_SILENCER_BLOCK).title("Steve's Redemption").description("Attach an End Sculk Silencer to an End Casing, escaping the grasp of the Warden").after(THE_INTEGRATED_INDUSTRY_AGE));

    public static final CCBAdvancement SHINY_DUST = create("shiny_dust", b -> b.icon(CCBItems.POWDERED_AMETHYST).title("Shiny Dust").description("Obtain a Powdered Amethyst").whenIconCollected().after(ROOT));
    public static final CCBAdvancement SILICON_DIOXIDE = create("silicon_dioxide", b -> b.icon(CCBItems.AMETHYST_CRYSTAL_SHEET).title("Silicon Dioxide").description("Obtain an Amethyst Crystal Sheet").whenIconCollected().after(SHINY_DUST));
    public static final CCBAdvancement DRYING_OUT = create("drying_out", b -> b.icon(CCBFluids.AMETHYST_SUSPENSION.getBucket().orElseThrow()).title("Drying Out").description("Place a bucket of Amethyst Suspension in the Nether\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(SHINY_DUST));
    public static final CCBAdvancement PRE_CRUSHING = create("pre_crushing", b -> b.icon(CCBItems.OBSIDIAN_CHUNK).title("Pre-Crushing").description("Obtain an Obsidian Chunk or a Crying Obsidian Chunk by pressing").whenItemCollected(CCBItemTags.CHUNKS.tag).after(SHINY_DUST));
    public static final CCBAdvancement STONE_COLD_TEARS = create("stone_cold_tears", b -> b.icon(CCBItems.POWDERED_CRYING_OBSIDIAN).title("Stone-Cold Tears").description("Obtain a Powdered Crying Obsidian").whenIconCollected().after(PRE_CRUSHING));
    public static final CCBAdvancement ROCK_SOLID_WISDOM = create("rock_solid_wisdom", b -> b.icon(CCBBlocks.CRYING_OBSIDIAN_BRICKS_M).title("Rock-Solid Wisdom").description("Obtain all types of Crying Obsidian Bricks with texts\n§7(Hidden Advancement)").special(HIDDEN_GOAL).whenItemsCollected(new ArrayList<>(List.of(CCBBlocks.CRYING_OBSIDIAN_BRICKS_A, CCBBlocks.CRYING_OBSIDIAN_BRICKS_B, CCBBlocks.CRYING_OBSIDIAN_BRICKS_C, CCBBlocks.CRYING_OBSIDIAN_BRICKS_D, CCBBlocks.CRYING_OBSIDIAN_BRICKS_E, CCBBlocks.CRYING_OBSIDIAN_BRICKS_F, CCBBlocks.CRYING_OBSIDIAN_BRICKS_G, CCBBlocks.CRYING_OBSIDIAN_BRICKS_H, CCBBlocks.CRYING_OBSIDIAN_BRICKS_I, CCBBlocks.CRYING_OBSIDIAN_BRICKS_J, CCBBlocks.CRYING_OBSIDIAN_BRICKS_K, CCBBlocks.CRYING_OBSIDIAN_BRICKS_L, CCBBlocks.CRYING_OBSIDIAN_BRICKS_M, CCBBlocks.CRYING_OBSIDIAN_BRICKS_N, CCBBlocks.CRYING_OBSIDIAN_BRICKS_O, CCBBlocks.CRYING_OBSIDIAN_BRICKS_P, CCBBlocks.CRYING_OBSIDIAN_BRICKS_Q, CCBBlocks.CRYING_OBSIDIAN_BRICKS_R, CCBBlocks.CRYING_OBSIDIAN_BRICKS_S, CCBBlocks.CRYING_OBSIDIAN_BRICKS_T, CCBBlocks.CRYING_OBSIDIAN_BRICKS_U, CCBBlocks.CRYING_OBSIDIAN_BRICKS_V, CCBBlocks.CRYING_OBSIDIAN_BRICKS_W, CCBBlocks.CRYING_OBSIDIAN_BRICKS_X, CCBBlocks.CRYING_OBSIDIAN_BRICKS_Y, CCBBlocks.CRYING_OBSIDIAN_BRICKS_Z))).after(STONE_COLD_TEARS));
    public static final CCBAdvancement PLYWOOD = create("plywood", b -> b.icon(CCBItems.AIRTIGHT_SHEET).title("Plywood").description("Assemble an Airtight Sheet").whenIconCollected().after(STONE_COLD_TEARS));
    public static final CCBAdvancement GASEOUS_VARIATIONS = create("gaseous_variations", b -> b.icon(p -> GasVirtualItem.getVirtualItem(new GasStack(CCBGases.SPORE_AIR, 1))).title("Gaseous Variations").description("Attempt to extract Spore Air in Mushroom Fields\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(PLYWOOD));
    public static final CCBAdvancement A_MURDER = create("a_murder", b -> b.icon(Items.LAVA_BUCKET).title("A Murder").description("Pump lava or an even hotter fluid into a Breeze Cooler\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(GASEOUS_VARIATIONS));
    public static final CCBAdvancement WHO_IS_THE_BREEZE_NOW = create("who_is_the_breeze_now", b -> b.icon(CCBItems.ENERGIZED_NATURAL_WIND_CHARGE).title("Who Is the Breeze Now?").description("Defeat a Breeze with an Airtight Cannon\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(A_MURDER));
    public static final CCBAdvancement BACK_TO_BASICS = create("back_to_basics", b -> b.icon(AllItems.ANDESITE_ALLOY).title("Back to Basics").description("Obtain an Andesite Alloy using an Airtight Reactor Kettle\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(WHO_IS_THE_BREEZE_NOW));
    public static final CCBAdvancement GLACIOLOGIST = create("glaciologist", b -> b.icon(CCBItems.BUILDERS_TEA_ICE_CREAM).title("Glaciologist").description("Obtain every type of ice cream").special(GOAL).whenItemsCollected(new ArrayList<>(List.of(CCBItems.ICE_CREAM, CCBItems.MILK_ICE_CREAM, CCBItems.BUILDERS_TEA_ICE_CREAM, CCBItems.AMETHYST_ICE_CREAM, CCBItems.HONEY_ICE_CREAM, CCBItems.CHOCOLATE_ICE_CREAM, CCBItems.BUILDERS_TEA_ICE_CREAM))).after(PLYWOOD));
    public static final CCBAdvancement LUXURY_TREAT = create("luxury_treat", b -> b.icon(Items.ENCHANTED_GOLDEN_APPLE).title("Luxury Treat").description("Feed an Enchanted Golden Apple to a Breeze Chamber").special(GOAL).after(GLACIOLOGIST));
    public static final CCBAdvancement MINTY_FRESH = create("minty_fresh", b -> b.icon(p -> GasVirtualItem.getVirtualItem(new GasStack(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR, 1))).title("Minty Fresh").description("Drain Pressurized Energized Ethereal Air into the world").special(GOAL).after(LUXURY_TREAT));
    public static final CCBAdvancement SKY_IS_NOT_THE_LIMIT = create("sky_is_not_the_limit", b -> b.icon(CCBItems.AIRTIGHT_CHESTPLATE).title("Sky Is Not the Limit").description("Fly to the build height limit while wearing an Airtight Chestplate").special(GOAL).after(MINTY_FRESH));
    public static final CCBAdvancement FLYWHEEL = create("flywheel", b -> b.icon(CCBBlocks.AIRTIGHT_ENGINE_BLOCK).title("Flywheel").description("Run an Airtight Assembly Driver at maximum power").special(GOAL).after(SKY_IS_NOT_THE_LIMIT));
    public static final CCBAdvancement MIRACLE_OF_ENGINEERING = create("miracle_of_engineering", b -> b.icon(CCBBlocks.TESLA_TURBINE_BLOCK).title("Miracle of Engineering").description("Run a Tesla Turbine at maximum power").special(GOAL).after(FLYWHEEL));
    public static final CCBAdvancement TAKE_A_DEEP_BREATH = create("take_a_deep_breath", b -> b.icon(CCBBlocks.AIRTIGHT_PUMP_BLOCK).title("Take a Deep Breathe").description("Place and power an Airtight Pump").after(PLYWOOD));
    public static final CCBAdvancement WATER_HAMMER_EFFECT = create("water_hammer_effect", b -> b.icon(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK).title("Water Hammer Effect").description("Obtain an Airtight Encased Pipe").whenIconCollected().after(TAKE_A_DEEP_BREATH));
    public static final CCBAdvancement YOU_SHALL_NOT_PASS = create("you_shall_not_pass", b -> b.icon(CCBBlocks.AIRTIGHT_CHECK_VALVE_BLOCK).title("You Shall Not Pass!").description("Obtain an Airtight Check Valve").whenIconCollected().after(WATER_HAMMER_EFFECT));
    public static final CCBAdvancement GAS_FILTRATION = create("gas_filtration", b -> b.icon(CCBBlocks.SMART_AIRTIGHT_PIPE_BLOCK).title("Gas Filtration").description("Obtain a Smart Airtight Pipe").whenIconCollected().after(YOU_SHALL_NOT_PASS));
    public static final CCBAdvancement LIVING_FREEZER = create("living_freezer", b -> b.icon(CCBBlocks.BREEZE_COOLER_BLOCK).title("Living Freezer").description("Obtain a Breeze Cooler").whenIconCollected().after(GAS_FILTRATION));
    public static final CCBAdvancement FROZEN_AMBROSIA = create("frozen_ambrosia", b -> b.icon(CCBItems.ICE_CREAM).title("Frozen Ambrosia").description("Feed a serving of Ice Cream to a Breeze Cooler").after(LIVING_FREEZER));
    public static final CCBAdvancement FEELING_THE_PRESSURE = create("feeling_the_pressure", b -> b.icon(CCBBlocks.AIR_COMPRESSOR_BLOCK).title("Feeling the Pressure").description("Place and power an Air Compressor").after(GAS_FILTRATION));
    public static final CCBAdvancement A_CLOSE_CALL = create("a_close_call", b -> b.icon(CCBBlocks.AIR_COMPRESSOR_BLOCK).title("A Close Call").description("Cool an Air Compressor right before it melts down\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(FEELING_THE_PRESSURE));
    public static final CCBAdvancement BIG_PRESSURE_COOKER = create("big_pressure_cooker", b -> b.icon(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK).title("Big Pressure Cooker").description("Assemble an Airtight Reactor Kettle").whenIconCollected().after(FEELING_THE_PRESSURE));
    public static final CCBAdvancement PRESS_IT_ALL = create("press_it_all", b -> b.icon(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK).title("Press It All").description("Assemble an Airtight Forging Press").whenIconCollected().after(BIG_PRESSURE_COOKER));
    public static final CCBAdvancement RETROFITTED = create("retrofitted", b -> b.icon(CCBBlocks.BREEZE_CHAMBER_BLOCK).title("Retrofitted!").description("Obtain a Breeze Chamber").whenIconCollected().after(FROZEN_AMBROSIA));
    public static final CCBAdvancement EMERGING_POWER = create("emerging_power", b -> b.icon(CCBBlocks.AIRTIGHT_ENGINE_BLOCK).title("Emerging Power").description("Generate power with an Airtight Engine").after(RETROFITTED));
    public static final CCBAdvancement DEBRIS_CRAFT = create("debris_craft", b -> b.icon(CCBItems.TESLA_TURBINE_ROTOR).title("Debris Craft").description("Assemble a Tesla Turbine Rotor").whenIconCollected().after(EMERGING_POWER));
    public static final CCBAdvancement GENIUS_ENGINEER = create("genius_engineer", b -> b.icon(CCBBlocks.TESLA_TURBINE_BLOCK).title("Genius Engineer").description("Generate power with a Tesla Turbine").after(DEBRIS_CRAFT));
    public static final CCBAdvancement TESLA_TURBINE_EASY_AS_PIE = create("tesla_turbine_easy_as_pie", b -> b.icon(CCBBlocks.TESLA_TURBINE_BLOCK).title("Tesla Turbine? Easy as Pie!").description("Fill more than one type of gas into a Tesla Turbine and make it explode\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(DEBRIS_CRAFT));
    public static final CCBAdvancement BAD_APPLE = create("bad_apple", b -> b.icon(Items.PUFFERFISH).title("Bad Apple").description("Feed improper food to a Breeze Chamber\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(RETROFITTED));
    public static final CCBAdvancement UNIVERSAL_ANTIDOTE = create("universal_antidote", b -> b.icon(Items.MILK_BUCKET).title("Universal Antidote").description("Use Milk to clear the Ill state of a Breeze Chamber - is this even scientific?\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(BAD_APPLE));
    public static final CCBAdvancement BETTER_THAN_A_BACKTANK = create("better_than_a_backtank", b -> b.icon(CCBItems.GAS_CANISTER).title("Better than a Backtank").description("Obtain a Gas Canister").whenIconCollected().after(PLYWOOD));
    public static final CCBAdvancement NEW_ENERGY_BATTERY_PACK = create("new_energy_battery_pack", b -> b.icon(CCBItems.GAS_CANISTER_PACK).title("New Energy Battery Pack").description("Assemble a Gas Canister Pack").whenIconCollected().after(BETTER_THAN_A_BACKTANK));
    public static final CCBAdvancement UPDRAFT = create("updraft", b -> b.icon(CCBItems.BREEZE_CORE).title("Updraft").description("Assemble a Breeze Core").whenIconCollected().after(NEW_ENERGY_BATTERY_PACK));
    public static final CCBAdvancement WIND_CHARGED = create("wind_charged", b -> b.icon(CCBItems.AIRTIGHT_CANNON).title("Wind-Charged").description("Defeat a mob with an Airtight Cannon").after(UPDRAFT));
    public static final CCBAdvancement LOOKS_LIKE_THE_WEATHERS_CLEARING_UP = create("looks_like_the_weathers_clearing_up", b -> b.icon(CCBItems.SUNNY_FLARE).title("Looks Like the Weather's Clearing Up").description("Fire a Sunny Flare with an Airtight Cannon to skip a rainfall").after(WIND_CHARGED));
    public static final CCBAdvancement I_AM_THE_STORM_THAT_IS_APPROACHING = create("i_am_the_storm_that_is_approaching", b -> b.icon(CCBItems.THUNDERSTORM_FLARE).title("I Am the Storm That Is Approaching").description("Create an endless thunderstorm\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(LOOKS_LIKE_THE_WEATHERS_CLEARING_UP));
    public static final CCBAdvancement MINI_TUNNEL_BORER = create("mini_tunnel_borer", b -> b.icon(CCBItems.AIRTIGHT_HANDHELD_DRILL).title("Mini Tunnel Borer").description("Mine 64 blocks at the same time using an Airtight Handheld Drill").after(UPDRAFT));
    public static final CCBAdvancement EVEN_HARDER_THAN_OBSIDIAN = create("even_harder_than_obsidian", b -> b.icon(Blocks.REINFORCED_DEEPSLATE).title("Even Harder Than Obsidian").description("Mine a Reinforced Deepslate with an Airtight Handheld Drill\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(MINI_TUNNEL_BORER));
    public static final CCBAdvancement ALL_HANDS_ACQUIRED = create("all_hands_acquired", b -> b.icon(CCBItems.AIRTIGHT_EXTEND_ARM).title("All Hands Acquired").description("Assembly an Airtight Extend Arm").whenIconCollected().after(UPDRAFT));
    public static final CCBAdvancement THREE_WAY_HANDSHAKE = create("three_way_handshake", b -> b.icon(CCBItems.AIRTIGHT_EXTEND_ARM).title("Three-way Handshake").description("Hold two Airtight Extend Arms at the same time\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(ALL_HANDS_ACQUIRED));
    public static final CCBAdvancement SEALED_TO_PERFECTION = create("sealed_to_perfection", b -> b.icon(CCBItems.AIRTIGHT_HELMET).title("Sealed to Perfection").description("Wear a full set of Airtight Armor").after(UPDRAFT));
    public static final CCBAdvancement PHANTOM_DIVER = create("phantom_diver", b-> b.icon(CCBItems.AIRTIGHT_BOOTS).title("Phantom Divers").description("Activate every upgrade on a full set of Airtight Armor\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(SEALED_TO_PERFECTION));

    public static final CCBAdvancement ITEM_TANK = create("item_tank", b -> b.icon(CCBBlocks.ANDESITE_CRATE_BLOCK).title("Item Tank").description("Obtain an Andesite Crate").whenIconCollected().after(ROOT));
    public static final CCBAdvancement CAPACITY_UPGRADE = create("capacity_upgrade", b -> b.icon(CCBBlocks.BRASS_CRATE_BLOCK).title("Capacity Upgrade").description("Obtain a Brass Crate").whenIconCollected().after(ITEM_TANK));
    public static final CCBAdvancement ULTIMATE_STORAGE_STRATEGY = create("ultimate_storage_strategy", b -> b.icon(CCBBlocks.STURDY_CRATE_BLOCK).title("Ultimate Storage Strategy").description("Obtain a Sturdy Crate").whenIconCollected().after(CAPACITY_UPGRADE));
    public static final CCBAdvancement BOTTOMLESS_PIT = create("bottomless_pit", b -> b.icon(CCBBlocks.CARDBOARD_CRATE_BLOCK).title("Bottomless Pit").description("Obtain a Cardboard Crate").whenIconCollected().after(ULTIMATE_STORAGE_STRATEGY));
    public static final CCBAdvancement CUT_FROM_THE_SAME_CARDBOARD = create("cut_from_the_same_cardboard", b -> b.icon(CCBBlocks.CARDBOARD_CRATE_BLOCK).title("Cut from the Same Cardboard").description("Dispose a Cardboard Package with a Cardboard Crate\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(BOTTOMLESS_PIT));
    public static final CCBAdvancement A_HOUSE_OF_GOLD_IN_THE_CRATE = create("a_house_of_gold_in_the_crate", b -> b.icon(Items.GOLD_INGOT).title("A House of Gold in the Crate").description("Fill a Brass Crate with Gold Ingots\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(ITEM_TANK));
    public static final CCBAdvancement PORTABLE_LAVA_SEA = create("portable_lava_sea", b -> b.icon(Items.LAVA_BUCKET).title("Portable Lava Sea").description("Store at least 10,000 Lava Buckets in a Sturdy Crate\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(A_HOUSE_OF_GOLD_IN_THE_CRATE));

    private final PackOutput output;
    private final CompletableFuture<Provider> registries;

    public CCBAdvancements(PackOutput output, CompletableFuture<Provider> registries) {
        this.output = output;
        this.registries = registries;
    }

    @Contract("_, _ -> new")
    private static CCBAdvancement create(String id, UnaryOperator<Builder> operator) {
        return new CCBAdvancement(id, operator);
    }

    public static void provideLang(BiConsumer<String, String> consumer) {
        ENTRIES.forEach(advancement -> advancement.provideLang(consumer));
    }

    public static void register() {
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registries.thenCompose(provider -> {
            PathProvider pathProvider = output.createPathProvider(Target.DATA_PACK, "advancement");
            List<CompletableFuture<?>> futures = new ArrayList<>();
            ENTRIES.forEach(advancement -> advancement.save(holder -> futures.add(DataProvider.saveStable(cache, provider, Advancement.CODEC, holder.value(), pathProvider.json(holder.id()))), provider));
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public String getName() {
        return "Create: Crafted Beginning's Advancements";
    }
}
