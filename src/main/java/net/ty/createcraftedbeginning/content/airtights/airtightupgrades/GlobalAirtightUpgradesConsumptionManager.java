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
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;
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

    private static double getRawGasConsumption(Player player, AirtightUpgrade upgrade, EquipmentSlot equipmentSlot, float baseConsumption, AirtightArmorsHandler armorHandler) {
        double rawGasConsumption = baseConsumption * armorHandler.getConsumptionMultiplier(equipmentSlot) * upgrade.getGasConsumptionMultiplier(player);
        return GasConsumptions.isNonNegativeFinite(rawGasConsumption) ? rawGasConsumption : -1;
    }

    private static void clearExpired(Player player) {
        UUID playerId = player.getUUID();
        Map<ResourceLocation, Long> expirationByUpgradeId = POWERED_UPGRADES.get(playerId);
        if (expirationByUpgradeId == null) {
            return;
        }

        long gameTime = player.level().getGameTime();
        expirationByUpgradeId.entrySet().removeIf(entry -> entry.getValue() < gameTime);
        if (!expirationByUpgradeId.isEmpty()) {
            return;
        }

        POWERED_UPGRADES.remove(playerId);
    }

    private static boolean interactWithGasDirectly(Player player, Gas gasType, long gasAmount) {
        return gasAmount >= 0 && !gasType.isEmpty() && (gasAmount == 0 || CanisterContainerConsumers.interactContainer(player, gasType, gasAmount, () -> true, false));
    }

    private static Set<ResourceLocation> getPoweredIds(Player player) {
        Map<ResourceLocation, Long> expirationByUpgradeId = POWERED_UPGRADES.get(player.getUUID());
        if (expirationByUpgradeId == null || expirationByUpgradeId.isEmpty()) {
            return Set.of();
        }

        long gameTime = player.level().getGameTime();
        Set<ResourceLocation> poweredUpgradeIds = new HashSet<>();
        expirationByUpgradeId.forEach((upgradeId, expirationTime) -> {
            if (expirationTime >= gameTime) {
                poweredUpgradeIds.add(upgradeId);
            }
        });
        return Set.copyOf(poweredUpgradeIds);
    }

    public static boolean isPowered(Player player, AirtightUpgrade upgrade) {
        UUID playerId = player.getUUID();
        if (player.level().isClientSide) {
            Set<ResourceLocation> poweredUpgradeIds = CLIENT_POWERED_UPGRADES.get(playerId);
            return poweredUpgradeIds != null && poweredUpgradeIds.contains(upgrade.getID());
        }

        Map<ResourceLocation, Long> expirationByUpgradeId = POWERED_UPGRADES.get(playerId);
        if (expirationByUpgradeId == null) {
            return false;
        }

        long expirationTime = expirationByUpgradeId.getOrDefault(upgrade.getID(), 0L);
        return expirationTime >= player.level().getGameTime();
    }

    public static void syncToClient(Player player) {
        syncToClient(player, false);
    }

    public static void forceSyncToClient(Player player) {
        syncToClient(player, true);
    }

    private static void syncToClient(Player player, boolean forceSync) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        UUID playerId = serverPlayer.getUUID();
        Set<ResourceLocation> poweredUpgradeIds = getPoweredIds(serverPlayer);
        Set<ResourceLocation> lastSyncedPoweredIds = LAST_SYNCED_POWERED_UPGRADES.get(playerId);
        if (!forceSync && poweredUpgradeIds.equals(lastSyncedPoweredIds)) {
            return;
        }

        List<ResourceLocation> sortedPoweredIds = poweredUpgradeIds.stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
        CatnipServices.NETWORK.sendToClient(serverPlayer, new AirtightUpgradeSyncPacket(sortedPoweredIds));
        LAST_SYNCED_POWERED_UPGRADES.put(playerId, poweredUpgradeIds);
    }

    public static void acceptClientSync(Player player, List<ResourceLocation> poweredUpgradeIds) {
        if (!player.level().isClientSide) {
            return;
        }

        UUID playerId = player.getUUID();
        if (poweredUpgradeIds.isEmpty()) {
            CLIENT_POWERED_UPGRADES.remove(playerId);
            return;
        }

        CLIENT_POWERED_UPGRADES.put(playerId, Set.copyOf(poweredUpgradeIds));
    }

    public static boolean canConsumeGas(Player player, AirtightUpgrade upgrade, EquipmentSlot equipmentSlot, float gasConsumption, Predicate<AirtightArmorsHandler> armorHandlerPredicate) {
        return findAffordableFuel(player, upgrade, equipmentSlot, gasConsumption, armorHandlerPredicate).isPresent();
    }

    public static boolean tryConsumeGas(Player player, AirtightUpgrade upgrade, EquipmentSlot equipmentSlot, float gasConsumption) {
        return tryConsumeGas(player, upgrade, equipmentSlot, gasConsumption, armorHandler -> true);
    }

    public static boolean tryConsumeGas(Player player, AirtightUpgrade upgrade, EquipmentSlot equipmentSlot, float gasConsumption, Predicate<AirtightArmorsHandler> armorHandlerPredicate) {
        if (!GasConsumptions.isNonNegativeFinite(gasConsumption)) {
            return false;
        }

        Optional<AffordableFuel> affordableFuel = findAffordableFuel(player, upgrade, equipmentSlot, gasConsumption, armorHandlerPredicate);
        if (affordableFuel.isEmpty()) {
            return false;
        }

        AffordableFuel selectedFuel = affordableFuel.get();
        return interactWithGasDirectly(player, selectedFuel.gasType(), selectedFuel.amount());
    }

    private static Optional<AffordableFuel> findAffordableFuel(Player player, AirtightUpgrade upgrade, EquipmentSlot equipmentSlot, float gasConsumption, Predicate<AirtightArmorsHandler> armorHandlerPredicate) {
        if (!GasConsumptions.isNonNegativeFinite(gasConsumption)) {
            return Optional.empty();
        }
        return CanisterContainerConsumers.findAffordableFuel(player, gasType -> {
            AirtightArmorsHandler armorHandler = AirtightArmorsHandlerUtils.of(gasType);
            if (!armorHandlerPredicate.test(armorHandler)) {
                return -1;
            }
            return getRawGasConsumption(player, upgrade, equipmentSlot, gasConsumption, armorHandler);
        });
    }

    private static List<RequestedUpgrade> collectRequests(Player player) {
        List<RequestedUpgrade> requestedUpgrades = new ArrayList<>();
        for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
            ItemStack armorStack = player.getItemBySlot(equipmentSlot);
            for (AirtightUpgrade upgrade : AirtightArmorsUtils.getAllUpgrades(armorStack)) {
                if (!upgrade.isRequesting(player, armorStack)) {
                    continue;
                }

                int gasConsumption = upgrade.getGasConsumptionPerSecond(player, armorStack);
                if (gasConsumption < 0) {
                    continue;
                }

                requestedUpgrades.add(new RequestedUpgrade(upgrade, equipmentSlot, gasConsumption));
            }
        }
        return requestedUpgrades;
    }

    private static Optional<AffordableFuel> findAffordableFuel(Player player, List<RequestedUpgrade> requestedUpgrades) {
        return CanisterContainerConsumers.findAffordableFuel(player, gasType -> {
            AirtightArmorsHandler armorHandler = AirtightArmorsHandlerUtils.of(gasType);
            double totalGasConsumption = 0;
            for (RequestedUpgrade requestedUpgrade : requestedUpgrades) {
                double rawGasConsumption = getRawGasConsumption(player, requestedUpgrade.upgrade(), requestedUpgrade.equipmentSlot(), requestedUpgrade.gasConsumption(), armorHandler);
                if (rawGasConsumption < 0) {
                    return -1;
                }

                totalGasConsumption += rawGasConsumption;
                if (!GasConsumptions.isNonNegativeFinite(totalGasConsumption)) {
                    return -1;
                }
            }
            return totalGasConsumption;
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
        UUID playerId = player.getUUID();
        POWERED_UPGRADES.remove(playerId);
        LAST_SYNCED_POWERED_UPGRADES.remove(playerId);
        CLIENT_POWERED_UPGRADES.remove(playerId);
    }

    public static void clearClientTracking() {
        CLIENT_POWERED_UPGRADES.clear();
    }

    public static void tick(Player player) {
        Level level = player.level();
        if (level.isClientSide || player.tickCount % POWER_REFRESH_INTERVAL != 0) {
            return;
        }

        List<RequestedUpgrade> requestedUpgrades = collectRequests(player);
        if (requestedUpgrades.isEmpty()) {
            clearExpired(player);
            syncToClient(player);
            return;
        }

        Optional<AffordableFuel> affordableFuel = findAffordableFuel(player, requestedUpgrades);
        if (affordableFuel.isEmpty()) {
            clearAndSync(player);
            return;
        }

        AffordableFuel selectedFuel = affordableFuel.get();
        if (!interactWithGasDirectly(player, selectedFuel.gasType(), selectedFuel.amount())) {
            clearAndSync(player);
            return;
        }

        Map<ResourceLocation, Long> expirationByUpgradeId = POWERED_UPGRADES.computeIfAbsent(player.getUUID(), ignoredPlayerId -> new HashMap<>());
        long expirationTime = level.getGameTime() + POWER_REFRESH_INTERVAL;
        for (RequestedUpgrade requestedUpgrade : requestedUpgrades) {
            expirationByUpgradeId.put(requestedUpgrade.upgrade().getID(), expirationTime);
        }

        clearExpired(player);
        syncToClient(player);
    }

    private record RequestedUpgrade(AirtightUpgrade upgrade, EquipmentSlot equipmentSlot, int gasConsumption) {}
}
