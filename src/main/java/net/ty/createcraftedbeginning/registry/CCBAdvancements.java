package net.ty.createcraftedbeginning.registry;

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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

import static net.ty.createcraftedbeginning.advancement.CCBAdvancement.TaskType.GOAL;
import static net.ty.createcraftedbeginning.advancement.CCBAdvancement.TaskType.HIDDEN_GOAL;
import static net.ty.createcraftedbeginning.advancement.CCBAdvancement.TaskType.HIDDEN_TASK;

@SuppressWarnings("unused")
public class CCBAdvancements implements DataProvider {
    public static final List<CCBAdvancement> ENTRIES = new ArrayList<>();
    public static final CCBAdvancement START = null;
    public static final CCBAdvancement ROOT = create("root", b -> b.icon(CCBBlocks.BREEZE_COOLER_BLOCK).title("Welcome to Create: Crafted Beginning").description("A brand new experience").awardedForFree().special(HIDDEN_TASK));

    public static final CCBAdvancement ITEM_TANK = create("item_tank", b -> b.icon(CCBBlocks.ANDESITE_CRATE_BLOCK).title("Item Tank").description("Obtain an Andesite Crate").whenIconCollected().after(ROOT));
    public static final CCBAdvancement GETTING_AN_UPGRADE = create("getting_an_upgrade", b -> b.icon(CCBBlocks.BRASS_CRATE_BLOCK).title("Getting an Upgrade").description("Obtain a Brass Crate").whenIconCollected().after(ITEM_TANK));
    public static final CCBAdvancement FINAL_UPGRADE = create("final_upgrade", b -> b.icon(CCBBlocks.STURDY_CRATE_BLOCK).title("Final Upgrade").description("Obtain a Sturdy Crate").whenIconCollected().after(GETTING_AN_UPGRADE));
    public static final CCBAdvancement TRASH_CAN = create("trash_can", b -> b.icon(CCBBlocks.CARDBOARD_CRATE_BLOCK).title("Trash Can").description("Obtain a Cardboard Crate").whenIconCollected().after(FINAL_UPGRADE));
    public static final CCBAdvancement BOX_A_HOUSE_OF_GOLD = create("box_a_house_of_gold", b -> b.icon(Items.GOLD_INGOT).title("Box A House of Gold").description("Fill a Brass Crate with Gold Ingots").special(GOAL).after(ITEM_TANK));
    public static final CCBAdvancement PORTABLE_LAVA_SEA = create("portable_lava_sea", b -> b.icon(Items.LAVA_BUCKET).title("Portable Lava Sea").description("Store at least 10,000 Lava Buckets in a Sturdy Crate").special(GOAL).after(BOX_A_HOUSE_OF_GOLD));
    public static final CCBAdvancement BORN_FROM_THE_SAME_ROOT = create("born_from_the_same_root", b -> b.icon(CCBBlocks.CARDBOARD_CRATE_BLOCK).title("Born from the Same Root").description("Use a Cardboard Crate to dispose a Cardboard Package Item\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(ITEM_TANK));

    public static final CCBAdvancement GIVE_IT_ANOTHER_CRUSH = create("give_it_another_crush", b -> b.icon(CCBItems.POWDERED_AMETHYST).title("Give it Another Crush").description("Obtain a Powdered Amethyst").whenIconCollected().after(ROOT));
    public static final CCBAdvancement SILICON_DIOXIDE = create("silicon_dioxide", b -> b.icon(CCBItems.AMETHYST_CRYSTAL_SHEET).title("Silicon Dioxide").description("Obtain an Amethyst Crystal Sheet").whenIconCollected().after(GIVE_IT_ANOTHER_CRUSH));
    public static final CCBAdvancement NATURAL_EVAPORATION = create("natural_evaporation", b -> b.icon(CCBFluids.AMETHYST_SUSPENSION.getBucket().orElseThrow()).title("Natural Evaporation").description("Attempt to place a bucket of Amethyst Suspension in the Nether\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(GIVE_IT_ANOTHER_CRUSH));

    public static final CCBAdvancement PLYWOOD = create("plywood", b -> b.icon(CCBItems.AIRTIGHT_SHEET).title("Plywood").description("Assemble an Airtight Sheet").whenIconCollected().after(ROOT));

    public static final CCBAdvancement TAKE_A_DEEP_BREATH = create("take_a_deep_breath", b -> b.icon(CCBBlocks.AIRTIGHT_PUMP_BLOCK).title("Take a Deep Breathe").description("Place and power an Airtight Pump").after(PLYWOOD));
    public static final CCBAdvancement WATER_HAMMER_EFFECT = create("water_hammer_effect", b -> b.icon(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK).title("Water Hammer Effect").description("Obtain an Airtight Encased Pipe").whenIconCollected().after(TAKE_A_DEEP_BREATH));
    public static final CCBAdvancement YOU_SHALL_NOT_PASS = create("you_shall_not_pass", b -> b.icon(CCBBlocks.AIRTIGHT_CHECK_VALVE_BLOCK).title("You Shall Not Pass!").description("Obtain an Airtight Check Valve").whenIconCollected().after(WATER_HAMMER_EFFECT));
    public static final CCBAdvancement FILTERING_GAS = create("filtering_gas", b -> b.icon(CCBBlocks.SMART_AIRTIGHT_PIPE_BLOCK).title("Filtering Gas").description("Obtain a Smart Airtight Pipe").whenIconCollected().after(YOU_SHALL_NOT_PASS));
    public static final CCBAdvancement UNDER_IMMENSE_PRESSURE = create("under_immense_pressure", b -> b.icon(CCBBlocks.AIR_COMPRESSOR_BLOCK).title("Under Immense Pressure").description("Place and power an Air Compressor").after(FILTERING_GAS));
    public static final CCBAdvancement INDUSTRIAL_AUTOCLAVE = create("industrial_autoclave", b-> b.icon(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK).title("Industrial Autoclave").description("Assemble an Airtight Reactor Kettle").whenIconCollected().after(UNDER_IMMENSE_PRESSURE));
    public static final CCBAdvancement RISING_FORCE = create("rising_force", b -> b.icon(CCBBlocks.AIRTIGHT_ENGINE_BLOCK).title("Rising Force").description("Use an Airtight Engine to generate torque").after(INDUSTRIAL_AUTOCLAVE));
    public static final CCBAdvancement DEBRIS_CRAFTING = create("debris_crafting", b -> b.icon(CCBItems.TESLA_TURBINE_ROTOR).title("Debris Crafting").description("Assemble a Tesla Turbine Rotor").whenIconCollected().after(RISING_FORCE));
    public static final CCBAdvancement ENGINEERING_PRODIGY = create("engineering_prodigy", b -> b.icon(CCBBlocks.TESLA_TURBINE_BLOCK).title("Engineering Prodigy").description("Obtain a Tesla Turbine").whenIconCollected().after(DEBRIS_CRAFTING));

    public static final CCBAdvancement LIVING_FREEZER = create("living_freezer", b -> b.icon(CCBBlocks.BREEZE_COOLER_BLOCK).title("Living Freezer").description("Obtain a Breeze Cooler").whenIconCollected().after(PLYWOOD));
    public static final CCBAdvancement FROZEN_NECTAR = create("frozen_nectar", b -> b.icon(CCBItems.ICE_CREAM).title("Frozen Nectar").description("Feed an Ice Cream to the Breeze Cooler").after(LIVING_FREEZER));
    public static final CCBAdvancement RETROFIT = create("retrofit", b -> b.icon(CCBBlocks.BREEZE_CHAMBER_BLOCK).title("Retrofit!").description("Obtain a Breeze Chamber").whenIconCollected().after(FROZEN_NECTAR));
    public static final CCBAdvancement BAD_APPLE = create("bad_apple", b -> b.icon(Items.PUFFERFISH).title("Bad Apple").description("Feed a negative food item to the Breeze Chamber").after(RETROFIT));
    public static final CCBAdvancement UNIVERSAL_ANTIDOTE = create("universal_antidote", b -> b.icon(Items.MILK_BUCKET).title("Universal Antidote").description("Feed a bucket of milk to a Breeze Chamber in Ill state").after(BAD_APPLE));

    public static final CCBAdvancement PORTABLE_ENERGY_SOURCE = create("portable_energy_source", b -> b.icon(CCBItems.GAS_CANISTER).title("Portable Energy Source").description("Obtain a Gas Canister").whenIconCollected().after(PLYWOOD));
    public static final CCBAdvancement NEW_ENERGY_POWER_CELL = create("new_energy_power_cell", b -> b.icon(CCBItems.GAS_CANISTER_PACK).title("New-Energy Power Cell").description("Assemble a Gas Canister Pack").whenIconCollected().after(PORTABLE_ENERGY_SOURCE));
    public static final CCBAdvancement STRIPPED = create("stripped", b -> b.icon(CCBItems.BREEZE_CORE).title("Stripped").description("Assemble a Breeze Core").whenIconCollected().after(NEW_ENERGY_POWER_CELL));
    public static final CCBAdvancement WIND_CHARGER = create("wind_charger", b -> b.icon(CCBItems.AIRTIGHT_CANNON).title("Wind Charger").description("Defeat any mob with an Airtight Cannon").after(STRIPPED));
    public static final CCBAdvancement THE_SKIES_ARE_CLEARING_UP_NOW = create("the_skies_are_clearing_up_now", b -> b.icon(CCBItems.SUNNY_FLARE).title("The Skies Are Clearing Up Now").description("Fire a Sunny Flare using an Airtight Cannon to clear a rainy weather").after(WIND_CHARGER));
    public static final CCBAdvancement MINI_TBM = create("mini_tbm", b -> b.icon(CCBItems.AIRTIGHT_HANDHELD_DRILL).title("Mini-TBM").description("Mine 64 blocks at the same time using an Airtight Handheld Drill").after(THE_SKIES_ARE_CLEARING_UP_NOW));

    public static final CCBAdvancement GLACIOLOGIST = create("glaciologist", b -> b.icon(CCBItems.BUILDERS_TEA_ICE_CREAM).title("Glaciologist").description("Obtain every kind of ice cream").special(GOAL).whenItemCollected(CCBItems.ICE_CREAM).whenItemCollected(CCBItems.MILK_ICE_CREAM).whenItemCollected(CCBItems.BUILDERS_TEA_ICE_CREAM).whenItemCollected(CCBItems.AMETHYST_ICE_CREAM).whenItemCollected(CCBItems.HONEY_ICE_CREAM).whenItemCollected(CCBItems.CHOCOLATE_ICE_CREAM).whenItemCollected(CCBItems.BUILDERS_TEA_ICE_CREAM).after(PLYWOOD));
    public static final CCBAdvancement PHANTOM_DIVERS = create("phantom_divers", b-> b.icon(CCBItems.AIRTIGHT_HELMET).title("Phantom Divers").description("Get a full suit of Airtight Armors").special(GOAL).whenItemCollected(CCBItems.AIRTIGHT_HELMET).whenItemCollected(CCBItems.AIRTIGHT_CHESTPLATE).whenItemCollected(CCBItems.AIRTIGHT_LEGGINGS).whenItemCollected(CCBItems.AIRTIGHT_BOOTS).after(GLACIOLOGIST));
    public static final CCBAdvancement FLYWHEEL = create("flywheel", b -> b.icon(CCBBlocks.AIRTIGHT_ENGINE_BLOCK).title("Flywheel").description("Run an Airtight Assembly Driver at the maximum level of power").special(GOAL).after(PHANTOM_DIVERS));
    public static final CCBAdvancement MIRACLE_OF_ENGINEERING = create("miracle_of_engineering", b -> b.icon(CCBBlocks.TESLA_TURBINE_BLOCK).title("Miracle of Engineering").description("Run a Tesla Turbine at the maximum level of power").special(GOAL).after(FLYWHEEL));

    public static final CCBAdvancement HERMETIC_SEAL_600 = create("hermetic_seal_600", b -> b.icon(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK).title("Hermetic Seal 600%").description("Seal all six openings of an Airtight Encased Pipe using Airtight Sheets\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(PLYWOOD));
    public static final CCBAdvancement A_MURDER = create("a_murder", b -> b.icon(Items.LAVA_BUCKET).title("A Murder").description("Pump lava or hotter fluids into a Breeze Cooler\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(HERMETIC_SEAL_600));
    public static final CCBAdvancement A_ROYAL_FEAST = create("a_royal_feast", b -> b.icon(Items.ENCHANTED_GOLDEN_APPLE).title("A Royal Feast").description("Feed an Enchanted Golden Apple to a Breeze Chamber\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(A_MURDER));
    public static final CCBAdvancement SO_CLOSE = create("so_close", b -> b.icon(CCBBlocks.AIR_COMPRESSOR_BLOCK).title("So Close").description("Cool down an Air Compressor at the last second before it meltdowns\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(A_ROYAL_FEAST));
    public static final CCBAdvancement TESLA_TURBINE_EASY_AS_PIE = create("tesla_turbine_easy_as_pie", b -> b.icon(CCBBlocks.TESLA_TURBINE_BLOCK).title("Tesla Turbine? Easy as Pie!").description("Trigger an explosion by inputting multiple types of gas into a Tesla Turbine\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(SO_CLOSE));
    public static final CCBAdvancement WHO_IS_THE_BREEZE_NOW = create("who_is_the_breeze_now", b -> b.icon(CCBItems.ENERGIZED_NATURAL_WIND_CHARGE).title("Who Is the Breeze Now?").description("Defeat a Breeze with an Airtight Cannon\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(TESLA_TURBINE_EASY_AS_PIE));
    public static final CCBAdvancement EVEN_IF_HARDER_THAN_OBSIDIAN = create("even_if_harder_than_obsidian", b -> b.icon(Blocks.REINFORCED_DEEPSLATE).title("Even if Harder Than Obsidian").description("Mine a Reinforced Deepslate using an Airtight Handheld Drill\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(WHO_IS_THE_BREEZE_NOW));
    public static final CCBAdvancement SKY_IS_NOT_THE_LIMIT = create("sky_is_not_the_limit", b -> b.icon(CCBItems.AIRTIGHT_CHESTPLATE).title("Sky Is Not the Limit").description("Fly to the world build height limit while wearing an Airtight Chestplate\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(EVEN_IF_HARDER_THAN_OBSIDIAN));
    public static final CCBAdvancement I_AM_THE_STORM_THAT_IS_APPROACHING = create("i_am_the_storm_that_is_approaching", b -> b.icon(CCBItems.THUNDERSTORM_FLARE).title("I Am the Storm That Is Approaching").description("Fire a Thunderstorm Flare using an Airtight Cannon to set the weather to a thunderstorm\n§7(Hidden Advancement)").special(HIDDEN_GOAL).after(SKY_IS_NOT_THE_LIMIT));

    public static final CCBAdvancement END = null;

    private final PackOutput output;
    private final CompletableFuture<Provider> registries;

    public CCBAdvancements(PackOutput output, CompletableFuture<Provider> registries) {
        this.output = output;
        this.registries = registries;
    }

    @Contract("_, _ -> new")
    private static @NotNull CCBAdvancement create(String id, UnaryOperator<Builder> operator) {
        return new CCBAdvancement(id, operator);
    }

    public static void provideLang(BiConsumer<String, String> consumer) {
        ENTRIES.forEach(advancement -> advancement.provideLang(consumer));
    }

    public static void register() {
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cache) {
        return registries.thenCompose(provider -> {
            PathProvider pathProvider = output.createPathProvider(Target.DATA_PACK, "advancement");
            List<CompletableFuture<?>> futures = new ArrayList<>();
            ENTRIES.forEach(advancement -> advancement.save(holder -> futures.add(DataProvider.saveStable(cache, provider, Advancement.CODEC, holder.value(), pathProvider.json(holder.id()))), provider));
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public @NotNull String getName() {
        return "Create: Crafted Beginning's Advancements";
    }
}
