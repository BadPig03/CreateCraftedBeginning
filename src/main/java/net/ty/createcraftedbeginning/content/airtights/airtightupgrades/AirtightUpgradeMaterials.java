package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightUpgradeMaterials {
    private static final Map<ResourceLocation, Item> MATERIALS = new HashMap<>();
    private static Map<ResourceLocation, Item> clientMaterials = Map.of();

    private AirtightUpgradeMaterials() {
    }

    public static void set(ResourceLocation upgradeId, ResourceLocation itemId) {
        if (!MATERIALS.containsKey(upgradeId)) {
            throw new IllegalArgumentException("Airtight upgrade must be registered and require installation; got '" + upgradeId + "'.");
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (!BuiltInRegistries.ITEM.containsKey(itemId) || item == Items.AIR) {
            throw new IllegalArgumentException("Airtight upgrade material must be a registered non-air item; got '" + itemId + "'.");
        }

        MATERIALS.put(upgradeId, item);
    }

    @Internal
    public static Item resolveMaterial(AirtightUpgrade upgrade, boolean clientSide) {
        Map<ResourceLocation, Item> materials = clientSide ? clientMaterials : MATERIALS;
        return materials.getOrDefault(upgrade.getID(), upgrade.getDefaultUpgradeItem());
    }

    @Internal
    public static @Unmodifiable Map<ResourceLocation, ResourceLocation> createServerSnapshot() {
        Map<ResourceLocation, ResourceLocation> snapshot = new HashMap<>();
        MATERIALS.forEach((upgradeId, item) -> snapshot.put(upgradeId, BuiltInRegistries.ITEM.getKey(item)));
        return Map.copyOf(snapshot);
    }

    @Internal
    public static void acceptClientSync(Map<ResourceLocation, ResourceLocation> snapshot) {
        Map<ResourceLocation, Item> materials = new HashMap<>();
        snapshot.forEach((upgradeId, itemId) -> materials.put(upgradeId, BuiltInRegistries.ITEM.get(itemId)));
        clientMaterials = Map.copyOf(materials);
    }

    @Internal
    public static void clearClientMaterials() {
        clientMaterials = Map.of();
    }

    static void register(AirtightUpgrade upgrade) {
        if (upgrade.startsInstalled()) {
            return;
        }

        MATERIALS.put(upgrade.getID(), upgrade.getDefaultUpgradeItem());
    }
}
