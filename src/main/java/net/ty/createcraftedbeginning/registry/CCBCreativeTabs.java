package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.AllCreativeModeTabs;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.Builder;
import net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout.CCBCreativeTabSection;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrate;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrateProvider;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CCBAPI.MOD_ID);
    private static final EnumMap<CCBCreativeTabSection, List<ItemProviderEntry<?, ?>>> SECTION_INIT_ITEMS = new EnumMap<>(CCBCreativeTabSection.class);
    private static final EnumMap<CCBCreativeTabSection, List<ItemProviderEntry<?, ?>>> SECTION_TAIL_ITEMS = new EnumMap<>(CCBCreativeTabSection.class);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = REGISTER.register("base", () -> builder().title(CCBLang.translateDirect("item_groups.base_creative_tab")).withTabsBefore(AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey()).icon(() -> new ItemStack(CCBBlocks.BREEZE_COOLER_BLOCK)).displayItems(new RegistrateDisplayItemsGenerator()).build());

    @Internal
    public static void register(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }

    public static void registerSectionInit(CCBCreativeTabSection section, ItemProviderEntry<?, ?>... entries) {
        SECTION_INIT_ITEMS.computeIfAbsent(section, ignored -> new ArrayList<>()).addAll(List.of(entries));
    }

    public static void registerSectionTail(CCBCreativeTabSection section, ItemProviderEntry<?, ?>... entries) {
        SECTION_TAIL_ITEMS.computeIfAbsent(section, ignored -> new ArrayList<>()).addAll(List.of(entries));
    }

    private static Builder builder() {
        return CreativeModeTab.builder();
    }

    private record RegistrateDisplayItemsGenerator() implements DisplayItemsGenerator {
        private static Predicate<Item> makeExclusionPredicate() {
            Set<Item> exclusions = new ReferenceOpenHashSet<>();
            List<ItemProviderEntry<?, ?>> itemsExclusions = new ArrayList<>();
            itemsExclusions.addAll(List.of(CCBItems.INCOMPLETE_AIRTIGHT_SHEET, CCBItems.INCOMPLETE_GAS_CANISTER_PACK, CCBItems.INCOMPLETE_HEAVY_CORE, CCBItems.INCOMPLETE_TESLA_TURBINE_ROTOR, CCBItems.INCOMPLETE_BREEZE_CORE, CCBItems.INCOMPLETE_AIRTIGHT_CANNON, CCBItems.INCOMPLETE_AIRTIGHT_EXTEND_ARM, CCBItems.INCOMPLETE_AIRTIGHT_HANDHELD_DRILL, CCBItems.INCOMPLETE_AIRTIGHT_HELMET, CCBItems.INCOMPLETE_AIRTIGHT_CHESTPLATE, CCBItems.INCOMPLETE_AIRTIGHT_LEGGINGS, CCBItems.INCOMPLETE_AIRTIGHT_BOOTS, CCBItems.INCOMPLETE_WEATHER_FLARE, CCBItems.INCOMPLETE_ANCHOR_FLARE));
            itemsExclusions.addAll(List.of(CCBItems.GAS_CANISTER, CCBItems.GAS_CANISTER_PLACEABLE, CCBItems.CREATIVE_GAS_CANISTER, CCBItems.CREATIVE_GAS_CANISTER_PLACEABLE, CCBItems.NATURAL_WIND_CHARGE, CCBItems.ULTRAWARM_WIND_CHARGE, CCBItems.ETHEREAL_WIND_CHARGE, CCBItems.MOIST_WIND_CHARGE, CCBItems.SPORE_WIND_CHARGE, CCBItems.SCULK_WIND_CHARGE, CCBItems.ENERGIZED_NATURAL_WIND_CHARGE, CCBItems.ENERGIZED_ULTRAWARM_WIND_CHARGE, CCBItems.ENERGIZED_ETHEREAL_WIND_CHARGE, CCBItems.CREATIVE_WIND_CHARGE, CCBItems.GAS_VIRTUAL_ITEM));
            itemsExclusions.addAll(List.of(CCBItems.BALLOON_RARE_REVERTED, CCBItems.BALLOON_RARE_SMILE, CCBItems.BALLOON_RARE_CRY, CCBItems.BALLOON_RARE_EYE, CCBItems.BALLOON_RARE_ISAAC, CCBItems.BALLOON_RARE_GHAST, CCBItems.BALLOON_RARE_TROLLFACE, CCBItems.BALLOON_RARE_TENNA, CCBItems.BALLOON_RARE_PVZ, CCBItems.BALLOON_RARE_QUESTION_MARKS, CCBItems.BALLOON_RARE_POWERFUL, CCBItems.BALLOON_RARE_CHEESE));
            itemsExclusions.stream().map(ItemProviderEntry::asItem).forEach(exclusions::add);

            List<ItemProviderEntry<?, ?>> blocksExclusions = List.of(CCBBlocks.PNEUMATIC_ENGINE_BLOCK);
            blocksExclusions.stream().map(ItemProviderEntry::asItem).forEach(exclusions::add);
            return exclusions::contains;
        }

        private static List<ItemOrdering> makeOrderings() {
            List<ItemOrdering> orderings = new ReferenceArrayList<>();
            orderings.add(ItemOrdering.before(CCBItems.GAS_CANISTER.asItem(), CCBItems.CREATIVE_GAS_CANISTER.asItem()));

            orderings.add(ItemOrdering.after(CCBItems.TESLA_TURBINE_ROTOR.asItem(), CCBBlocks.TESLA_TURBINE_NOZZLE_BLOCK.asItem()));
            orderings.add(ItemOrdering.after(CCBItems.GAS_INJECTION_CHAMBER_FILTER.asItem(), CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK.asItem()));
            return orderings;
        }

        private static void applyOrderings(List<Item> items, List<ItemOrdering> orderings) {
            for (ItemOrdering ordering : orderings) {
                int anchorIndex = items.indexOf(ordering.anchor());
                if (anchorIndex == -1) {
                    continue;
                }

                Item item = ordering.item();
                int itemIndex = items.indexOf(item);
                if (itemIndex != -1) {
                    items.remove(itemIndex);
                    if (itemIndex < anchorIndex) {
                        anchorIndex--;
                    }
                }

                int insertionIndex = ordering.before ? anchorIndex : anchorIndex + 1;
                items.add(insertionIndex, item);
            }
        }

        private static void applySectionItems(List<Item> items, CCBCreativeTabSection section) {
            List<ItemProviderEntry<?, ?>> initEntries = SECTION_INIT_ITEMS.getOrDefault(section, List.of());
            for (ItemProviderEntry<?, ?> entry : initEntries) {
                Item item = entry.asItem();
                if (!items.remove(item)) {
                    continue;
                }

                items.addFirst(item);
            }

            List<ItemProviderEntry<?, ?>> tailEntries = SECTION_TAIL_ITEMS.getOrDefault(section, List.of());
            for (ItemProviderEntry<?, ?> entry : tailEntries) {
                Item item = entry.asItem();
                if (!items.remove(item)) {
                    continue;
                }

                items.add(item);
            }
        }

        private static void outputAll(Output output, List<ItemStack> stacks) {
            stacks.forEach(stack -> output.accept(stack, TabVisibility.PARENT_AND_SEARCH_TABS));
        }

        private static List<Item> collectItems(Predicate<Item> exclusionPredicate, CCBCreativeTabSection section) {
            List<Item> items = new ReferenceArrayList<>();
            for (RegistryEntry<Item, Item> entry : CCBRegistrateProvider.get().getAll(Registries.ITEM)) {
                if (CCBRegistrate.isOutOfCreativeSection(entry, section)) {
                    continue;
                }

                Item item = entry.get();
                if (item instanceof BlockItem || exclusionPredicate.test(item)) {
                    continue;
                }

                items.add(item);
            }
            return items;
        }

        private static List<Item> collectBlocks(Predicate<Item> exclusionPredicate, CCBCreativeTabSection section) {
            List<Item> items = new ReferenceArrayList<>();
            Collection<RegistryEntry<Block, Block>> registryEntries = CCBRegistrateProvider.get().getAll(Registries.BLOCK);
            for (RegistryEntry<Block, Block> entry : registryEntries) {
                if (CCBRegistrate.isOutOfCreativeSection(entry, section)) {
                    continue;
                }

                Item item = entry.get().asItem();
                if (item == Items.AIR || exclusionPredicate.test(item)) {
                    continue;
                }

                items.add(item);
            }
            return new ReferenceArrayList<>(new ReferenceLinkedOpenHashSet<>(items));
        }

        @Override
        public void accept(ItemDisplayParameters parameters, Output output) {
            Predicate<Item> exclusionPredicate = makeExclusionPredicate();
            List<ItemOrdering> orderings = makeOrderings();
            EnumMap<CCBCreativeTabSection, List<ItemStack>> stacksBySection = new EnumMap<>(CCBCreativeTabSection.class);
            Map<Item, CCBCreativeTabSection> itemSections = new IdentityHashMap<>();
            for (CCBCreativeTabSection section : CCBCreativeTabSection.values()) {
                List<Item> items = new LinkedList<>();
                items.addAll(collectBlocks(exclusionPredicate, section));
                items.addAll(collectItems(exclusionPredicate, section));
                applyOrderings(items, orderings);
                applySectionItems(items, section);

                List<ItemStack> stacks = new ArrayList<>(items.size());
                items.stream().map(ItemStack::new).forEach(stacks::add);
                if (section == CCBCreativeTabSection.CANISTERS) {
                    stacks.addAll(GasCanisterUtils.getAllCanisters());
                }

                stacksBySection.put(section, stacks);
                stacks.forEach(stack -> itemSections.put(stack.getItem(), section));
            }

            CCBCreativeTabLayout.setItemSections(itemSections);
            for (CCBCreativeTabSection section : CCBCreativeTabSection.values()) {
                outputAll(output, stacksBySection.get(section));
            }
        }
    }

    private record ItemOrdering(Item item, Item anchor, boolean before) {
        @Contract("_, _ -> new")
        public static ItemOrdering before(Item item, Item anchor) {
            return new ItemOrdering(item, anchor, true);
        }

        @Contract("_, _ -> new")
        public static ItemOrdering after(Item item, Item anchor) {
            return new ItemOrdering(item, anchor, false);
        }
    }
}
