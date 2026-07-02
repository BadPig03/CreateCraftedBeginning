package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades;

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
public class AirtightHelmetUpgradeRegistry {
    private static final Map<ResourceLocation, AirtightUpgrade> REGISTRY = new HashMap<>();
    private static final List<AirtightUpgrade> ORDERED_UPGRADES = new ArrayList<>();

    public static @Nullable AirtightUpgrade getByID(ResourceLocation id) {
        return REGISTRY.getOrDefault(id, null);
    }

    public static @Nullable AirtightUpgrade getByItem(Item item) {
        return REGISTRY.values().stream().filter(upgrade -> upgrade.getUpgradeItem() == item).findFirst().orElse(null);
    }

    public static boolean allUpgradesActive(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.is(CCBItems.AIRTIGHT_HELMET) && ORDERED_UPGRADES.stream().allMatch(upgrade -> upgrade.isEnabled(helmet));
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
        register(EffectsProtectionUpgrade.INSTANCE);
        register(WaterBreathingUpgrade.INSTANCE);
        register(GogglesUpgrade.INSTANCE);
        register(VisionUpgrade.INSTANCE);
        register(SpectralUpgrade.INSTANCE);
        register(HelmetResistanceUpgrade.INSTANCE);
    }
}
