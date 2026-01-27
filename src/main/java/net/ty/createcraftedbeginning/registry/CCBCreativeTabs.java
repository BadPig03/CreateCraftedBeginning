package net.ty.createcraftedbeginning.registry;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class CCBCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateCraftedBeginning.MOD_ID);

    @Internal
    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }

    private record RegistrateDisplayItemsGenerator(DeferredHolder<CreativeModeTab, CreativeModeTab> tabFilter) implements DisplayItemsGenerator {
        private static @NotNull Predicate<Item> makeExclusionPredicate() {
            Set<Item> exclusions = new ReferenceOpenHashSet<>();
            List<ItemProviderEntry<?, ?>> itemsExclusions = List.of(CCBItems.INCOMPLETE_AIRTIGHT_SHEET, CCBItems.INCOMPLETE_GAS_CANISTER_PACK, CCBItems.INCOMPLETE_HEAVY_CORE, CCBItems.INCOMPLETE_TESLA_TURBINE_ROTOR, CCBItems.INCOMPLETE_BREEZE_CORE, CCBItems.INCOMPLETE_AIRTIGHT_CANNON, CCBItems.INCOMPLETE_AIRTIGHT_EXTEND_ARM, CCBItems.INCOMPLETE_AIRTIGHT_HANDHELD_DRILL, CCBItems.INCOMPLETE_AIRTIGHT_HELMET, CCBItems.INCOMPLETE_AIRTIGHT_CHESTPLATE, CCBItems.INCOMPLETE_AIRTIGHT_LEGGINGS, CCBItems.INCOMPLETE_AIRTIGHT_BOOTS, CCBItems.INCOMPLETE_WEATHER_FLARE, CCBItems.INCOMPLETE_ANCHOR_FLARE, CCBItems.GAS_CANISTER_PLACEABLE, CCBItems.NATURAL_WIND_CHARGE, CCBItems.ULTRAWARM_WIND_CHARGE, CCBItems.ETHEREAL_WIND_CHARGE, CCBItems.MOIST_WIND_CHARGE, CCBItems.SPORE_WIND_CHARGE, CCBItems.ENERGIZED_NATURAL_WIND_CHARGE, CCBItems.ENERGIZED_ULTRAWARM_WIND_CHARGE, CCBItems.ENERGIZED_ETHEREAL_WIND_CHARGE, CCBItems.GAS_VIRTUAL_ITEM);
            itemsExclusions.stream().map(ItemProviderEntry::asItem).forEach(exclusions::add);

            List<ItemProviderEntry<?, ?>> blocksExclusions = List.of(CCBBlocks.AIRTIGHT_INTAKE_PORT_BLOCK, CCBBlocks.CINDER_ALLOY_BLOCK, CCBBlocks.CINDER_CASING_BLOCK, CCBBlocks.CINDER_INCINERATION_BLOWER_BLOCK, CCBBlocks.PHOTO_STRESS_BEARING_BLOCK, CCBBlocks.PNEUMATIC_ENGINE_BLOCK);
            blocksExclusions.stream().map(ItemProviderEntry::asItem).forEach(exclusions::add);
            return exclusions::contains;
        }

        private static @NotNull List<ItemOrdering> makeOrderings() {
            List<ItemOrdering> orderings = new ReferenceArrayList<>();

            Map<ItemProviderEntry<?, ?>, ItemProviderEntry<?, ?>> simpleBeforeOrderings = Map.of(CCBItems.GAS_CANISTER, CCBItems.GAS_CANISTER_PACK);
            simpleBeforeOrderings.forEach((entry, otherEntry) -> orderings.add(ItemOrdering.order(entry.asItem(), otherEntry.asItem())));
            return orderings;
        }

        @SuppressWarnings("RedundantOperationOnEmptyContainer")
        private static @NotNull Function<Item, ItemStack> makeStackFunc() {
            Map<Item, Function<Item, ItemStack>> factories = new Reference2ReferenceOpenHashMap<>();
            Map<ItemProviderEntry<?, ?>, Function<Item, ItemStack>> simpleFactories = Map.of();
            simpleFactories.forEach((entry, factory) -> factories.put(entry.asItem(), factory));

            return item -> {
                Function<Item, ItemStack> factory = factories.get(item);
                return factory != null ? factory.apply(item) : new ItemStack(item);
            };
        }

        private static void applyOrderings(List<Item> items, @NotNull List<ItemOrdering> orderings) {
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

                items.add(anchorIndex, item);
            }
        }

        private static void outputAll(Output output, @NotNull List<Item> items, Function<Item, ItemStack> stackFunc) {
            items.forEach(item -> output.accept(stackFunc.apply(item), TabVisibility.PARENT_AND_SEARCH_TABS));
        }

        @Override
        public void accept(@NotNull ItemDisplayParameters parameters, @NotNull Output output) {
            Predicate<Item> exclusionPredicate = makeExclusionPredicate();
            List<ItemOrdering> orderings = makeOrderings();
            Function<Item, ItemStack> stackFunc = makeStackFunc();

            List<Item> items = new LinkedList<>(collectBlocks(exclusionPredicate));
            items.addAll(collectItems(exclusionPredicate));

            applyOrderings(items, orderings);
            outputAll(output, items, stackFunc);
        }

        private @NotNull List<Item> collectItems(Predicate<Item> exclusionPredicate) {
            List<Item> items = new ReferenceArrayList<>();
            for (RegistryEntry<Item, Item> entry : CreateCraftedBeginning.registrate().getAll(Registries.ITEM)) {
                if (CCBRegistrate.isOutOfCreativeTab(entry, tabFilter)) {
                    continue;
                }

                Item item = entry.get();
                if (item instanceof BlockItem) {
                    continue;
                }

                if (!exclusionPredicate.test(item)) {
                    items.add(item);
                }
            }
            return items;
        }

        private @NotNull List<Item> collectBlocks(Predicate<Item> exclusionPredicate) {
            List<Item> items = new ReferenceArrayList<>();
            Collection<RegistryEntry<Block, Block>> registryEntries = CreateCraftedBeginning.registrate().getAll(Registries.BLOCK);
            for (RegistryEntry<Block, Block> entry : registryEntries) {
                if (CCBRegistrate.isOutOfCreativeTab(entry, tabFilter)) {
                    continue;
                }

                Item item = entry.get().asItem();
                if (item == Items.AIR) {
                    continue;
                }

                if (!exclusionPredicate.test(item)) {
                    items.add(item);
                }
            }
            items = new ReferenceArrayList<>(new ReferenceLinkedOpenHashSet<>(items));
            return items;
        }
    }

    private record ItemOrdering(Item item, Item anchor) {
        @Contract("_, _ -> new")
        public static @NotNull ItemOrdering order(Item item, Item anchor) {
            return new ItemOrdering(item, anchor);
        }
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BASE_CREATIVE_TAB = TABS.register("base", () -> CreativeModeTab.builder().title(CCBLang.translateDirect("item_groups.creative_tab")).withTabsBefore(CreativeModeTabs.SPAWN_EGGS).icon(CCBBlocks.BREEZE_COOLER_BLOCK::asStack).displayItems(new RegistrateDisplayItemsGenerator(CCBCreativeTabs.BASE_CREATIVE_TAB)).build());
}
