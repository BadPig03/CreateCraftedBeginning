package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightUpgradeRegistry {
    private static final Map<ResourceLocation, AirtightUpgrade> GLOBAL_REGISTRY = new HashMap<>();

    private final String name;
    private Map<ResourceLocation, AirtightUpgrade> upgradesById = Map.of();
    private List<AirtightUpgrade> orderedUpgrades = List.of();
    private List<AirtightUpgradeStatus> defaultStatuses = List.of();
    private List<TickingAirtightUpgrade> tickingUpgrades = List.of();
    private boolean registered;

    public AirtightUpgradeRegistry(String name) {
        this.name = name;
    }

    static @Nullable AirtightUpgrade getGlobalById(ResourceLocation id) {
        return GLOBAL_REGISTRY.get(id);
    }

    public @Nullable AirtightUpgrade getById(ResourceLocation id) {
        return upgradesById.get(id);
    }

    public @Nullable AirtightUpgrade getByStack(ItemStack stack) {
        for (AirtightUpgrade upgrade : orderedUpgrades) {
            if (!upgrade.testUpgradeItem(stack)) {
                continue;
            }

            return upgrade;
        }
        return null;
    }

    public List<AirtightUpgrade> getAll() {
        return orderedUpgrades;
    }

    public List<AirtightUpgradeStatus> getDefaultStatuses() {
        return defaultStatuses;
    }

    public void forEach(Consumer<AirtightUpgrade> action) {
        orderedUpgrades.forEach(action);
    }

    public void tick(Player player, ItemStack item) {
        tickingUpgrades.forEach(upgrade -> upgrade.tick(player, item));
    }

    public boolean allUpgradesEnabled(ItemStack item) {
        return orderedUpgrades.stream().allMatch(upgrade -> upgrade.isEnabled(item));
    }

    public void registerAll(AirtightUpgrade... upgrades) {
        if (registered) {
            throw new IllegalStateException("Airtight upgrade registry '" + name + "' has already been registered");
        }

        List<AirtightUpgrade> registeredUpgrades = List.copyOf(Arrays.asList(upgrades.clone()));
        Map<ResourceLocation, AirtightUpgrade> newUpgradesById = new HashMap<>();
        Set<ResourceLocation> upgradeIds = new HashSet<>();
        for (AirtightUpgrade upgrade : registeredUpgrades) {
            ResourceLocation upgradeId = upgrade.getID();
            if (!upgradeIds.add(upgradeId)) {
                throw new IllegalArgumentException("Duplicate airtight upgrade id '" + upgradeId + "' in registry '" + name + '\'');
            }

            if (GLOBAL_REGISTRY.containsKey(upgradeId)) {
                throw new IllegalArgumentException("Airtight upgrade id '" + upgradeId + "' is already registered");
            }

            if (upgrade.startsEnabled() && !upgrade.startsInstalled()) {
                throw new IllegalArgumentException("Airtight upgrade '" + upgradeId + "' cannot start enabled before it is installed");
            }

            newUpgradesById.put(upgradeId, upgrade);
        }

        List<AirtightUpgradeStatus> newDefaultStatuses = new ArrayList<>(registeredUpgrades.size());
        List<TickingAirtightUpgrade> newTickingUpgrades = new ArrayList<>();
        for (AirtightUpgrade upgrade : registeredUpgrades) {
            newDefaultStatuses.add(new AirtightUpgradeStatus(upgrade.getID(), upgrade.startsEnabled(), upgrade.startsInstalled()));
            if (!(upgrade instanceof TickingAirtightUpgrade tickingUpgrade)) {
                continue;
            }

            newTickingUpgrades.add(tickingUpgrade);
        }

        GLOBAL_REGISTRY.putAll(newUpgradesById);
        upgradesById = Map.copyOf(newUpgradesById);
        orderedUpgrades = registeredUpgrades;
        defaultStatuses = List.copyOf(newDefaultStatuses);
        tickingUpgrades = List.copyOf(newTickingUpgrades);
        registered = true;
    }
}
