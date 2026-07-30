package net.ty.createcraftedbeginning.registry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBCreativeTabLayout {
    public static final int ROW_SIZE = 9;
    public static final int VISIBLE_ROW_COUNT = 5;

    private static volatile Map<Item, CCBCreativeTabSection> itemSections = Map.of();
    private static volatile List<PositionedSection> positionedSections = List.of();

    private CCBCreativeTabLayout() {
    }

    public static void setItemSections(Map<Item, CCBCreativeTabSection> sections) {
        itemSections = Collections.unmodifiableMap(new IdentityHashMap<>(sections));
    }

    public static List<ItemStack> rebuildDisplayItems(Collection<ItemStack> baseItems) {
        EnumMap<CCBCreativeTabSection, List<ItemStack>> groupedItems = new EnumMap<>(CCBCreativeTabSection.class);
        for (CCBCreativeTabSection section : CCBCreativeTabSection.values()) {
            groupedItems.put(section, new ArrayList<>());
        }

        List<ItemStack> unassignedItems = new ArrayList<>();
        Map<Item, CCBCreativeTabSection> sections = itemSections;
        for (ItemStack stack : baseItems) {
            CCBCreativeTabSection section = sections.get(stack.getItem());
            if (section == null) {
                unassignedItems.add(stack);
                continue;
            }
            groupedItems.get(section).add(stack);
        }

        List<ItemStack> rebuilt = new ArrayList<>(baseItems.size() + ROW_SIZE * CCBCreativeTabSection.values().length * 2);
        List<PositionedSection> positions = new ArrayList<>(CCBCreativeTabSection.values().length);
        for (CCBCreativeTabSection section : CCBCreativeTabSection.values()) {
            padToRowBoundary(rebuilt);
            int bannerRow = rebuilt.size() / ROW_SIZE;
            addEmptyRow(rebuilt);
            positions.add(new PositionedSection(section, bannerRow));
            rebuilt.addAll(groupedItems.get(section));
        }
        rebuilt.addAll(unassignedItems);
        positionedSections = List.copyOf(positions);
        return rebuilt;
    }

    public static List<PositionedSection> positionedSections() {
        return positionedSections;
    }

    private static void padToRowBoundary(List<ItemStack> items) {
        int remainder = items.size() % ROW_SIZE;
        if (remainder == 0) {
            return;
        }

        for (int slot = remainder; slot < ROW_SIZE; slot++) {
            items.add(ItemStack.EMPTY);
        }
    }

    private static void addEmptyRow(List<ItemStack> items) {
        for (int slot = 0; slot < ROW_SIZE; slot++) {
            items.add(ItemStack.EMPTY);
        }
    }

    public enum CCBCreativeTabSection {
        AIRTIGHTS,
        PHOTO_STRESSES,
        ENDS,
        DECORATIONS,
        CANISTERS
    }

    public record PositionedSection(CCBCreativeTabSection section, int bannerRow) {}
}
