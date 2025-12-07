package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterQueryUtils;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class GasCanisterPackUtils {
    public static final UUID FALLBACK_UUID = UUID.fromString("9e2faded-cafe-4ec2-c314-dad129ae971d");

    private GasCanisterPackUtils() {
    }

    public static boolean shouldCauseBlockBreakReset(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {
        if (!newStack.is(oldStack.getItem())) {
            return true;
        }

        if (!newStack.isDamageableItem() || !oldStack.isDamageableItem()) {
            return !ItemStack.isSameItemSameComponents(newStack, oldStack);
        }

        DataComponentMap newComponents = newStack.getComponents();
        DataComponentMap oldComponents = oldStack.getComponents();
        if (newComponents.isEmpty() || oldComponents.isEmpty()) {
            return !(newComponents.isEmpty() && oldComponents.isEmpty());
        }

        Set<DataComponentType<?>> newKeys = new HashSet<>(newComponents.keySet());
        Set<DataComponentType<?>> oldKeys = new HashSet<>(oldComponents.keySet());
        newKeys.remove(CCBDataComponents.GAS_CANISTER_PACK_FLAGS);
        oldKeys.remove(CCBDataComponents.GAS_CANISTER_PACK_FLAGS);
        return !newKeys.equals(oldKeys) || !newKeys.stream().allMatch(key -> Objects.equals(newComponents.get(key), oldComponents.get(key)));
    }

    public static @NotNull UUID getCanisterPackUUID(@NotNull ItemStack pack) {
        return GasCanisterQueryUtils.isValidCanisterPack(pack) ? pack.getOrDefault(CCBDataComponents.GAS_CANISTER_PACK_UUID, FALLBACK_UUID) : FALLBACK_UUID;
    }

    public static void setCanisterPackUUID(@NotNull ItemStack pack, UUID uuid) {
        if (!GasCanisterQueryUtils.isValidCanisterPack(pack)) {
            return;
        }

        pack.set(CCBDataComponents.GAS_CANISTER_PACK_UUID, uuid);
    }

    public static void resetCanisterPackUUID(@NotNull ItemStack pack) {
        UUID uuid = pack.getOrDefault(CCBDataComponents.GAS_CANISTER_PACK_UUID, FALLBACK_UUID);
        if (uuid.compareTo(FALLBACK_UUID) != 0) {
            return;
        }

        UUID newUUID = UUID.randomUUID();
        setCanisterPackUUID(pack, newUUID);
        CatnipServices.NETWORK.sendToAllClients(new GasCanisterPackUUIDPacket(pack, newUUID));
    }
}
