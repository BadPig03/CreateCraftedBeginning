package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GlobalAirtightUpgradesConsumptionManager {
    private static final Map<UUID, Map<ResourceLocation, Long>> CLIENT_POWERED_UPGRADES = new HashMap<>();
    private static final Map<UUID, Map<ResourceLocation, Long>> POWERED_UPGRADES = new HashMap<>();
    private static final Map<Integer, EquipmentSlot> ARMOR_SLOTS = Map.of(0, EquipmentSlot.HEAD, 1, EquipmentSlot.CHEST, 2, EquipmentSlot.LEGS, 3, EquipmentSlot.FEET);

    private GlobalAirtightUpgradesConsumptionManager() {
    }

    private static int getEffectiveGasConsumption(Player player, AirtightUpgrade upgrade, int index, float baseConsumption) {
        Gas gasType = CanisterContainerSuppliers.getFirstAvailableGasContent(player).getGasType();
        if (gasType.isEmpty()) {
            return -1;
        }

        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasType);
        if (armorsHandler == null) {
            return -1;
        }

        float[] multipliers = armorsHandler.getConsumptionMultiplier();
        if (index < 0 || index >= multipliers.length) {
            return -1;
        }

        return Mth.ceil(baseConsumption * multipliers[index] * upgrade.getGasConsumptionMultiplier(player));
    }

    private static void clearExpired(Player player) {
        UUID uuid = player.getUUID();
        Map<ResourceLocation, Long> playerState = POWERED_UPGRADES.get(uuid);
        if (playerState == null) {
            return;
        }

        long nowTime = player.level().getGameTime();
        playerState.entrySet().removeIf(entry -> entry.getValue() < nowTime);
        if (!playerState.isEmpty()) {
            return;
        }

        POWERED_UPGRADES.remove(uuid);
    }

    private static boolean tryConsumeGasDirectly(Player player, int amount) {
        if (amount < 0) {
            return false;
        }
        else if (amount == 0) {
            return true;
        }

        Gas gasType = CanisterContainerSuppliers.getFirstAvailableGasContent(player).getGasType();
        return !gasType.isEmpty() && CanisterContainerConsumers.interactContainer(player, gasType, amount, () -> true, false);
    }

    public static boolean hasValidGas(Player player) {
        Gas gasType = CanisterContainerSuppliers.getFirstAvailableGasContent(player).getGasType();
        return !gasType.isEmpty() && AirtightArmorsHandler.REGISTRY.get(gasType) != null;
    }

    public static boolean isPowered(Player player, AirtightUpgrade upgrade) {
        Map<UUID, Map<ResourceLocation, Long>> source;
        if (player.level().isClientSide) {
            source = CLIENT_POWERED_UPGRADES;
        }
        else {
            source = POWERED_UPGRADES;
        }

        Map<ResourceLocation, Long> playerState = source.get(player.getUUID());
        if (playerState == null) {
            return false;
        }

        long expiresAt = playerState.getOrDefault(upgrade.getID(), 0L);
        if (expiresAt >= player.level().getGameTime()) {
            return true;
        }

        if (!player.level().isClientSide) {
            return false;
        }

        playerState.remove(upgrade.getID());
        if (!playerState.isEmpty()) {
            return false;
        }

        CLIENT_POWERED_UPGRADES.remove(player.getUUID());
        return false;
    }

    public static void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Map<ResourceLocation, Long> playerState = POWERED_UPGRADES.get(serverPlayer.getUUID());
        if (playerState == null || playerState.isEmpty()) {
            CatnipServices.NETWORK.sendToClient(serverPlayer, new AirtightUpgradeSyncPacket(List.of()));
            return;
        }

        List<ResourceLocation> poweredIds = new ArrayList<>();
        long now = serverPlayer.level().getGameTime();
        playerState.forEach((id, expiresAt) -> {
            if (expiresAt < now) {
                return;
            }

            poweredIds.add(id);
        });
        CatnipServices.NETWORK.sendToClient(serverPlayer, new AirtightUpgradeSyncPacket(poweredIds));
    }

    public static void acceptClientSync(Player player, List<ResourceLocation> poweredIds) {
        if (!player.level().isClientSide) {
            return;
        }

        if (poweredIds.isEmpty()) {
            CLIENT_POWERED_UPGRADES.remove(player.getUUID());
            return;
        }

        Map<ResourceLocation, Long> playerState = CLIENT_POWERED_UPGRADES.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
        long expiresAt = player.level().getGameTime() + 25L;
        playerState.clear();
        poweredIds.forEach(id -> playerState.put(id, expiresAt));
    }

    public static boolean tryConsumeGas(Player player, AirtightUpgrade upgrade, EquipmentSlot slot, float consumption) {
        if (consumption < 0) {
            return false;
        }
        else if (consumption == 0) {
            return true;
        }

        int slotIndex = 3 - slot.getIndex();
        int effectiveConsumption = getEffectiveGasConsumption(player, upgrade, slotIndex, consumption);
        if (effectiveConsumption < 0) {
            return false;
        }
        else if (effectiveConsumption == 0) {
            return true;
        }
        return tryConsumeGasDirectly(player, effectiveConsumption);
    }

    public static void clear(Player player) {
        POWERED_UPGRADES.remove(player.getUUID());
    }

    public static void tick(Player player) {
        Level level = player.level();
        if (level.isClientSide || player.tickCount % 20 != 0) {
            return;
        }

        List<AirtightUpgrade> requestedUpgrades = new ArrayList<>();
        int totalCost = 0;
        for (Entry<Integer, EquipmentSlot> entry : ARMOR_SLOTS.entrySet()) {
            ItemStack item = player.getItemBySlot(entry.getValue());
            for (AirtightUpgrade upgrade : AirtightArmorsUtils.getAllUpgrades(item)) {
                if (!upgrade.isRequesting(player, item)) {
                    continue;
                }

                int consumption = upgrade.getGasConsumptionPerSecond(player, item);
                if (consumption <= 0) {
                    continue;
                }

                int effectiveConsumption = getEffectiveGasConsumption(player, upgrade, entry.getKey(), consumption);
                if (effectiveConsumption < 0) {
                    continue;
                }

                requestedUpgrades.add(upgrade);
                totalCost += effectiveConsumption;
            }
        }

        if (requestedUpgrades.isEmpty()) {
            clearExpired(player);
            syncToClient(player);
            return;
        }

        if (totalCost > 0 && !tryConsumeGasDirectly(player, totalCost)) {
            clear(player);
            syncToClient(player);
            return;
        }

        Map<ResourceLocation, Long> playerState = POWERED_UPGRADES.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>());
        long expiresAt = level.getGameTime() + 20L;
        requestedUpgrades.forEach(upgrade -> playerState.put(upgrade.getID(), expiresAt));
        clearExpired(player);
        syncToClient(player);
    }
}
