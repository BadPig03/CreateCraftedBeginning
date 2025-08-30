package net.ty.createcraftedbeginning.advancement;

import com.google.common.collect.Sets;
import com.simibubi.create.AllBlocks;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.data.CCBTags;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static net.ty.createcraftedbeginning.advancement.CCBAdvancement.TaskType.CHALLENGE;
import static net.ty.createcraftedbeginning.advancement.CCBAdvancement.TaskType.EXPERT;
import static net.ty.createcraftedbeginning.advancement.CCBAdvancement.TaskType.NOISY;
import static net.ty.createcraftedbeginning.advancement.CCBAdvancement.TaskType.SECRET;
import static net.ty.createcraftedbeginning.advancement.CCBAdvancement.TaskType.SILENT;

@SuppressWarnings("unused")
public class CCBAdvancements implements DataProvider {
    public static final List<CCBAdvancement> ENTRIES = new ArrayList<>();
    public static final CCBAdvancement START = null,

    ROOT = create("root", b -> b.icon(CCBBlocks.EMPTY_BREEZE_CHAMBER_BLOCK).title("Welcome to Create: Crafted Beginning").description("A Brand New Experience").awardedForFree().special(SILENT)),

    PNEUMATIC_ENGINE = create("pneumatic_engine", b -> b.icon(CCBBlocks.PNEUMATIC_ENGINE_BLOCK).title("Stress Battery").description("Place a Pneumatic Engine, then power it using Compressed Air from a Backtank").after(ROOT).special(NOISY)),

    AIRTIGHT_SHEET = create("airtight_sheet", b -> b.icon(CCBItems.AIRTIGHT_SHEET).title("Materials Science Marvel").description("Assemble an Airtight Sheet").whenIconCollected().after(PNEUMATIC_ENGINE).special(NOISY)),

    BREEZE_CHAMBER = create("breeze_chamber", b -> b.icon(CCBBlocks.BREEZE_CHAMBER_BLOCK).title("Fairy Spin").description("Find a frosty new home for your Breeze friend").whenIconCollected().after(AIRTIGHT_SHEET).special(NOISY)),

    BREEZE_CHAMBER_GALLING = create("breeze_chamber_galling", b -> b.icon(Items.POWDER_SNOW_BUCKET).title("Perfect Freeze").description("Pour slush into the Breeze Chamber to trigger your Breeze's Galling state").after(BREEZE_CHAMBER).special(EXPERT)),

    BREEZE_CHAMBER_LAVA = create("breeze_chamber_lava", b -> b.icon(Items.LAVA_BUCKET).title("An Engine Murder").description("Flood the Breeze Chamber with lava—accidentally or intentionally\n§7(Hidden Advancement)").after(BREEZE_CHAMBER).special(SECRET)),

    ICE_CREAM = create("ice_cream", b -> b.icon(CCBItems.ICE_CREAM).title("Ambrosia Gelida").description("Make yourself an ice cream treat using slush").whenIconCollected().after(BREEZE_CHAMBER).special(NOISY)),

    ICE_CREAM_NEW = create("ice_cream_new", b -> b.icon(CCBItems.CHOCOLATE_ICE_CREAM).title("Infusion!").description("Create an ice cream with exotic flavors using a Spout").whenItemCollected(CCBTags.CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).after(ICE_CREAM).special(NOISY)),

    ICE_CREAM_ALL = create("ice_cream_all", b -> b.icon(CCBItems.CREATIVE_ICE_CREAM).title("Perfect Glacialist").description("Acquire all ice cream varieties").whenItemCollected(CCBItems.ICE_CREAM).whenItemCollected(CCBItems.MILK_ICE_CREAM).whenItemCollected(CCBItems.CHOCOLATE_ICE_CREAM).whenItemCollected(CCBItems.HONEY_ICE_CREAM).whenItemCollected(CCBItems.AMETHYST_ICE_CREAM).after(ICE_CREAM_NEW).special(CHALLENGE)),

    AIRTIGHT_COMPONENTS = create("airtight_components", b -> b.icon(CCBBlocks.AIRTIGHT_TANK_BLOCK).title("Fluid Dynamics Revolution").description("Craft any airtight component with Airtight Sheets to withstand immense pressures in Compressed Air systems").whenItemCollected(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).after(AIRTIGHT_SHEET).special(NOISY)),

    AIRTIGHT_INTAKE_PORT = create("airtight_intake_port", b -> b.icon(CCBBlocks.AIRTIGHT_INTAKE_PORT_BLOCK).title("Critical Pressure Breakthrough").description("Initiate Compressed Air generation using the Airtight Intake Port").after(AIRTIGHT_COMPONENTS).special(NOISY)),

    AIRTIGHT_INTAKE_PORT_MAXED = create("airtight_intake_port_maxed", b -> b.icon(CCBBlocks.AIRTIGHT_INTAKE_PORT_BLOCK).title("Take a Deep Breath").description("Operate an Airtight Intake Port at maximum intake rate").after(AIRTIGHT_INTAKE_PORT).special(EXPERT)),

    COMPRESSED_AIR_EXPLOSION = create("compressed_air_explosion", b -> b.icon(AllBlocks.FLUID_TANK).title("Is It a Firecracker?").description("Witness a copper fluid component being blown up by excessive pressure\n§7(Hidden Advancement)").after(AIRTIGHT_COMPONENTS).special(SECRET)),

    AIR_COMPRESSOR = create("air_compressor", b -> b.icon(CCBBlocks.AIR_COMPRESSOR_BLOCK).title("Intense Pressure").description("Pressurize Compressed Air using an Air Compressor").after(AIRTIGHT_COMPONENTS).special(NOISY)),

    AIR_COMPRESSOR_EXPLOSION = create("air_compressor_explosion", b -> b.icon(CCBBlocks.AIR_COMPRESSOR_BLOCK).title("Pressure Overload").description("Attempt to re-pressurize High-Pressure Compressed Air\n§7(Hidden Advancement)").after(AIR_COMPRESSOR).special(SECRET)),

    GAS_INJECTION_CHAMBER = create("gas_injection_chamber", b -> b.icon(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK).title("Poof!").description("Process any item using the Gas Injection Chamber").after(AIRTIGHT_COMPONENTS).special(NOISY)),

    AIRTIGHT_ENCASED_PIPE_SEALED = create("airtight_encased_pipe_sealed", b -> b.icon(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK).title("Airtightness 600%").description("Fully seal an Airtight Encased Pipe with airtight sheets, and then reevaluate your life choices\n§7(Hidden Advancement)").after(AIRTIGHT_COMPONENTS).special(SECRET)),

    AIRTIGHT_ENCASED_PIPE_EXPLOSION = create("airtight_encased_pipe_explosion", b -> b.icon(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK).title("Goodnight, Terra").description("Ignite an epic explosion using Airtight Encased Pipes\n§7(Hidden Advancement)").after(AIRTIGHT_COMPONENTS).special(SECRET)),

    END = null;

    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public CCBAdvancements(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.output = output;
        this.registries = registries;
    }

    private static CCBAdvancement create(String id, UnaryOperator<CCBAdvancement.Builder> b) {
        return new CCBAdvancement(id, b);
    }

    public static void provideLang(BiConsumer<String, String> consumer) {
        for (CCBAdvancement advancement : ENTRIES) {
            advancement.provideLang(consumer);
        }
    }

    public static void register() {
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cache) {
        return this.registries.thenCompose(provider -> {
            PackOutput.PathProvider pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancement");
            List<CompletableFuture<?>> futures = new ArrayList<>();

            Set<ResourceLocation> set = Sets.newHashSet();
            Consumer<AdvancementHolder> consumer = (advancement) -> {
                ResourceLocation id = advancement.id();
                Path path = pathProvider.json(id);
                futures.add(DataProvider.saveStable(cache, provider, Advancement.CODEC, advancement.value(), path));
            };

            for (CCBAdvancement advancement : ENTRIES) {
                advancement.save(consumer, provider);
            }

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public @NotNull String getName() {
        return "Create: Crafted Beginning's Advancements";
    }
}
