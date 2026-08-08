package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptionUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.AffordableFuel;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GlobalAirtightUpgradesConsumptionManager {
    private static final Map<UUID, Set<ResourceLocation>> CLIENT_POWERED_UPGRADES = new HashMap<>();
    private static final Map<UUID, Map<ResourceLocation, Long>> POWERED_UPGRADES = new HashMap<>();
    private static final Map<UUID, Set<ResourceLocation>> LAST_SYNCED_POWERED_UPGRADES = new HashMap<>();
    private static final int POWER_REFRESH_INTERVAL = 20;
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private GlobalAirtightUpgradesConsumptionManager() {
    }

    private static double getRawGasConsumption(Player player, AirtightUpgrade upgrade, EquipmentSlot slot, float baseConsumption, AirtightArmorsHandler handler) {
        double rawCost = baseConsumption * handler.getConsumptionMultiplier(slot) * upgrade.getGasConsumptionMultiplier(player);
        return GasConsumptionUtils.isNonNegativeFinite(rawCost) ? rawCost : -1;
    }

    private static void clearExpired(Player player) {
        UUID uuid = player.getUUID();
        Map<ResourceLocation, Long> expiresAtById = POWERED_UPGRADES.get(uuid);
        if (expiresAtById == null) {
            return;
        }

        long now = player.level().getGameTime();
        expiresAtById.entrySet().removeIf(entry -> entry.getValue() < now);
        if (!expiresAtById.isEmpty()) {
            return;
        }

        POWERED_UPGRADES.remove(uuid);
    }

    private static boolean interactWithGasDirectly(Player player, Gas gas, long amount) {
        return amount >= 0 && !gas.isEmpty() && (amount == 0 || CanisterContainerConsumers.interactContainer(player, gas, amount, () -> true, false));
    }

    private static Set<ResourceLocation> getPoweredIds(Player player) {
        Map<ResourceLocation, Long> expiresAtById = POWERED_UPGRADES.get(player.getUUID());
        if (expiresAtById == null || expiresAtById.isEmpty()) {
            return Set.of();
        }

        long now = player.level().getGameTime();
        Set<ResourceLocation> poweredIds = new HashSet<>();
        expiresAtById.forEach((id, expiresAt) -> {
            if (expiresAt >= now) {
                poweredIds.add(id);
            }
        });
        return Set.copyOf(poweredIds);
    }

    public static boolean isPowered(Player player, AirtightUpgrade upgrade) {
        UUID uuid = player.getUUID();
        if (player.level().isClientSide) {
            Set<ResourceLocation> poweredIds = CLIENT_POWERED_UPGRADES.get(uuid);
            return poweredIds != null && poweredIds.contains(upgrade.getID());
        }

        Map<ResourceLocation, Long> expiresAtById = POWERED_UPGRADES.get(uuid);
        if (expiresAtById == null) {
            return false;
        }

        long expiresAt = expiresAtById.getOrDefault(upgrade.getID(), 0L);
        return expiresAt >= player.level().getGameTime();
    }

    public static void syncToClient(Player player) {
        syncToClient(player, false);
    }

    public static void forceSyncToClient(Player player) {
        syncToClient(player, true);
    }

    private static void syncToClient(Player player, boolean force) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        UUID uuid = serverPlayer.getUUID();
        Set<ResourceLocation> poweredIds = getPoweredIds(serverPlayer);
        Set<ResourceLocation> lastSynced = LAST_SYNCED_POWERED_UPGRADES.get(uuid);
        if (!force && poweredIds.equals(lastSynced)) {
            return;
        }

        List<ResourceLocation> packetIds = poweredIds.stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
        CatnipServices.NETWORK.sendToClient(serverPlayer, new AirtightUpgradeSyncPacket(packetIds));
        LAST_SYNCED_POWERED_UPGRADES.put(uuid, poweredIds);
    }

    public static void acceptClientSync(Player player, List<ResourceLocation> poweredIds) {
        if (!player.level().isClientSide) {
            return;
        }

        UUID uuid = player.getUUID();
        if (poweredIds.isEmpty()) {
            CLIENT_POWERED_UPGRADES.remove(uuid);
            return;
        }

        CLIENT_POWERED_UPGRADES.put(uuid, Set.copyOf(poweredIds));
    }

    public static boolean canConsumeGas(Player player, AirtightUpgrade upgrade, EquipmentSlot slot, float consumption, Predicate<AirtightArmorsHandler> handlerPredicate) {
        return findAffordableFuel(player, upgrade, slot, consumption, handlerPredicate).isPresent();
    }

    public static boolean tryConsumeGas(Player player, AirtightUpgrade upgrade, EquipmentSlot slot, float consumption) {
        return tryConsumeGas(player, upgrade, slot, consumption, handler -> true);
    }

    public static boolean tryConsumeGas(Player player, AirtightUpgrade upgrade, EquipmentSlot slot, float consumption, Predicate<AirtightArmorsHandler> handlerPredicate) {
        if (!GasConsumptionUtils.isNonNegativeFinite(consumption)) {
            return false;
        }

        Optional<AffordableFuel> fuel = findAffordableFuel(player, upgrade, slot, consumption, handlerPredicate);
        if (fuel.isEmpty()) {
            return false;
        }

        AffordableFuel selectedFuel = fuel.get();
        return interactWithGasDirectly(player, selectedFuel.gasType(), selectedFuel.amount());
    }

    private static Optional<AffordableFuel> findAffordableFuel(Player player, AirtightUpgrade upgrade, EquipmentSlot slot, float consumption, Predicate<AirtightArmorsHandler> handlerPredicate) {
        if (!GasConsumptionUtils.isNonNegativeFinite(consumption)) {
            return Optional.empty();
        }
        return CanisterContainerConsumers.findAffordableFuel(player, gas -> {
            AirtightArmorsHandler handler = AirtightArmorsHandlerUtils.of(gas);
            if (!handlerPredicate.test(handler)) {
                return -1;
            }
            return getRawGasConsumption(player, upgrade, slot, consumption, handler);
        });
    }

    private static List<RequestedUpgrade> collectRequests(Player player) {
        List<RequestedUpgrade> requests = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack item = player.getItemBySlot(slot);
            for (AirtightUpgrade upgrade : AirtightArmorsUtils.getAllUpgrades(item)) {
                if (!upgrade.isRequesting(player, item)) {
                    continue;
                }

                int consumption = upgrade.getGasConsumptionPerSecond(player, item);
                if (consumption < 0) {
                    continue;
                }

                requests.add(new RequestedUpgrade(upgrade, slot, consumption));
            }
        }
        return requests;
    }

    private static Optional<AffordableFuel> findAffordableFuel(Player player, List<RequestedUpgrade> requests) {
        return CanisterContainerConsumers.findAffordableFuel(player, gas -> {
            AirtightArmorsHandler handler = AirtightArmorsHandlerUtils.of(gas);
            double totalCost = 0;
            for (RequestedUpgrade request : requests) {
                double rawCost = getRawGasConsumption(player, request.upgrade(), request.slot(), request.consumption(), handler);
                if (rawCost < 0) {
                    return -1;
                }

                totalCost += rawCost;
                if (!GasConsumptionUtils.isNonNegativeFinite(totalCost)) {
                    return -1;
                }
            }
            return totalCost;
        });
    }

    private static void clearAndSync(Player player) {
        clear(player);
        syncToClient(player);
    }

    public static void clear(Player player) {
        POWERED_UPGRADES.remove(player.getUUID());
    }

    public static void clearTracking(Player player) {
        UUID uuid = player.getUUID();
        POWERED_UPGRADES.remove(uuid);
        LAST_SYNCED_POWERED_UPGRADES.remove(uuid);
        CLIENT_POWERED_UPGRADES.remove(uuid);
    }

    public static void clearClientTracking() {
        CLIENT_POWERED_UPGRADES.clear();
    }

    public static void tick(Player player) {
        Level level = player.level();
        if (level.isClientSide || player.tickCount % POWER_REFRESH_INTERVAL != 0) {
            return;
        }

        List<RequestedUpgrade> requests = collectRequests(player);
        if (requests.isEmpty()) {
            clearExpired(player);
            syncToClient(player);
            return;
        }

        Optional<AffordableFuel> fuel = findAffordableFuel(player, requests);
        if (fuel.isEmpty()) {
            clearAndSync(player);
            return;
        }

        AffordableFuel selectedFuel = fuel.get();
        if (!interactWithGasDirectly(player, selectedFuel.gasType(), selectedFuel.amount())) {
            clearAndSync(player);
            return;
        }

        Map<ResourceLocation, Long> expiresAtById = POWERED_UPGRADES.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
        long expiresAt = level.getGameTime() + POWER_REFRESH_INTERVAL;
        for (RequestedUpgrade request : requests) {
            expiresAtById.put(request.upgrade().getID(), expiresAt);
        }

        clearExpired(player);
        syncToClient(player);
    }

    private record RequestedUpgrade(AirtightUpgrade upgrade, EquipmentSlot slot, int consumption) {}
}
