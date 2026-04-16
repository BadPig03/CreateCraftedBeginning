package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AirtightHelmetUpgradeRegistry {
    private static final Map<ResourceLocation, AirtightUpgrade> REGISTRY = new HashMap<>();
    private static final List<AirtightUpgrade> ORDERED_UPGRADES = new ArrayList<>();

    public static void register(AirtightUpgrade upgrade) {
        REGISTRY.put(upgrade.getID(), upgrade);
        ORDERED_UPGRADES.add(upgrade);
        ORDERED_UPGRADES.sort(Comparator.comparingInt(AirtightUpgrade::getIndex));
    }

    @NotNull
    public static List<AirtightUpgrade> getAll() {
        return new ArrayList<>(ORDERED_UPGRADES);
    }

    @Nullable
    public static AirtightUpgrade getByID(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    @Nullable
    public static AirtightUpgrade getByItem(Item item) {
        return REGISTRY.values().stream().filter(upgrade -> upgrade.getUpgradeItem() == item).findFirst().orElse(null);
    }

    public static void forEach(Consumer<AirtightUpgrade> action) {
        getAll().forEach(action);
    }

    @NotNull
    public static List<AirtightUpgradeStatus> getDefaultUpgradeList() {
        List<AirtightUpgradeStatus> list = new ArrayList<>();
        for (AirtightUpgrade upgrade : getAll()) {
            list.add(new AirtightUpgradeStatus(upgrade.getID(), upgrade.startsEnabled(), upgrade.startsInstalled()));
        }
        return list;
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
