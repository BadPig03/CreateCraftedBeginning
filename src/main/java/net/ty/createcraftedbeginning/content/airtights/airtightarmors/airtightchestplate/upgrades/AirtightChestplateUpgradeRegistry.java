package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightChestplateUpgradeRegistry {
    private static final Map<ResourceLocation, AirtightUpgrade> REGISTRY = new HashMap<>();
    private static final List<AirtightUpgrade> ORDERED_UPGRADES = new ArrayList<>();

    public static @Nullable AirtightUpgrade getByID(ResourceLocation id) {
        return REGISTRY.getOrDefault(id, null);
    }

    public static @Nullable AirtightUpgrade getByItem(Item item) {
        return REGISTRY.values().stream().filter(upgrade -> upgrade.getUpgradeItem() == item).findFirst().orElse(null);
    }

    public static boolean allUpgradesActive(Player player) {
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        return chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE) && ORDERED_UPGRADES.stream().allMatch(upgrade -> upgrade.isEnabled(chestplate));
    }

    public static List<AirtightUpgrade> getAll() {
        return new ArrayList<>(ORDERED_UPGRADES);
    }

    public static List<AirtightUpgradeStatus> getDefaultUpgradeList() {
        List<AirtightUpgradeStatus> list = new ArrayList<>();
        for (AirtightUpgrade upgrade : getAll()) {
            list.add(new AirtightUpgradeStatus(upgrade.getID(), upgrade.startsEnabled(), upgrade.startsInstalled()));
        }
        return list;
    }

    public static void forEach(Consumer<AirtightUpgrade> action) {
        getAll().forEach(action);
    }

    public static void register(AirtightUpgrade upgrade) {
        REGISTRY.put(upgrade.getID(), upgrade);
        ORDERED_UPGRADES.add(upgrade);
        ORDERED_UPGRADES.sort(Comparator.comparingInt(AirtightUpgrade::getIndex));
    }

    public static void registerUpgrades() {
        register(ElytraUpgrade.INSTANCE);
        register(CreativeFlightUpgrade.INSTANCE);
        register(InvisibilityUpgrade.INSTANCE);
        register(RegenerationUpgrade.INSTANCE);
        register(HasteUpgrade.INSTANCE);
        register(ChestplateResistanceUpgrade.INSTANCE);
    }
}
