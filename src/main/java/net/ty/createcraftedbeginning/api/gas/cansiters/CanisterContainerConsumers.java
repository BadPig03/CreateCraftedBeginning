package net.ty.createcraftedbeginning.api.gas.cansiters;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.api.gas.cansiters.events.CanisterContainerEvent;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenu;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenuSyncPacket;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class CanisterContainerConsumers {
    private CanisterContainerConsumers() {
    }

    /**
     * Interacts with the player's gas canister containers by draining gas from multiple containers if needed,
     * with support for menu synchronization when gas canister pack menus are open.
     * <p>
     * This method handles gas drainage from all available gas canister containers belonging to the player.
     * It supports creative mode players (bypasses actual gas transfer) and survival mode players.
     * Before performing the gas transfer, it posts a {@link CanisterContainerEvent} to allow
     * other mods to modify or cancel the operation.
     * </p>
     * <p>
     * The method distributes the drainage amount across multiple containers if a single container
     * doesn't have sufficient gas. If the total available gas across all containers is insufficient,
     * the operation fails. When draining from gas canister packs that have their menu open,
     * the menu display is automatically updated to reflect the gas amount changes.
     * </p>
     * <p>
     * After successful gas transfer, it sends a client packet to synchronize the visual effects.
     * </p>
     *
     * @param player          the player whose gas containers will be drained (must not be null)
     * @param gasType         the type of gas to drain
     * @param amount          the amount of gas to drain (must be positive)
     * @param executeSupplier optional condition that must return true for the operation to proceed
     * @return true if the operation was successful, bypassed, or cancelled; false if the operation failed due to insufficient gas
     * @see CanisterContainerEvent
     * @see #drainContainer(List, Gas, long, Player)
     * @see CanisterContainerClientPacket
     * @see GasCanisterPackMenu
     */
    public static boolean interactContainer(@NotNull Player player, Gas gasType, long amount, Supplier<Boolean> executeSupplier) {
        if (player.isCreative() || gasType.isEmpty() || amount <= 0) {
            return true;
        }

        List<IGasCanisterContainer> containerList = CanisterContainerSuppliers.getAllSuppliers(player);
        if (containerList.isEmpty()) {
            return false;
        }

        CanisterContainerEvent event = new CanisterContainerEvent(player, gasType, amount, executeSupplier);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled() || executeSupplier != null && !executeSupplier.get()) {
            return true;
        }

        long newAmount = event.getAmount();
        if (newAmount <= 0) {
            return true;
        }

        boolean result = drainContainer(containerList, gasType, newAmount, player);
        if (!result || !(player instanceof ServerPlayer serverPlayer)) {
            return result;
        }

        CatnipServices.NETWORK.sendToClient(serverPlayer, new CanisterContainerClientPacket(new GasStack(gasType, newAmount)));
        return true;
    }

    /**
     * Drains a specified amount of gas from multiple containers, distributing the amount across available containers
     * and synchronizing the state with any open gas canister pack menus.
     * <p>
     * This private method attempts to extract the specified amount of gas from a list of containers,
     * distributing the drain operation across multiple containers if necessary. It performs the operation
     * in two phases: simulation followed by execution.
     * </p>
     * <p>
     * The method first simulates draining from all available containers that contain the specified gas type,
     * tracking how much can be drained from each container. If the total available amount meets or exceeds
     * the requested amount, it then executes the actual drain operation across all participating containers.
     * </p>
     * <p>
     * For gas canister packs, if the player has the corresponding menu open, it updates the menu state
     * and sends synchronization packets to the client to ensure the UI reflects the changes.
     * </p>
     *
     * @param containerList the list of gas canister containers to drain from
     * @param gasType       the type of gas to drain (must not be null)
     * @param amount        the total amount of gas to drain (must be positive)
     * @param player        the player whose containers are being drained (used for menu synchronization)
     * @return true if the requested amount was successfully drained from the containers, false otherwise
     * @see GasCanisterContainerContents
     * @see GasCanisterPackContainerContents
     * @see GasCanisterPackUtils#isCanisterPackMenuOpened(Player, ItemStack) 
     * @see GasCanisterPackMenuSyncPacket
     */
    private static boolean drainContainer(List<IGasCanisterContainer> containerList, @NotNull Gas gasType, long amount, Player player) {
        if (gasType.isEmpty() || amount <= 0) {
            return true;
        }

        long remaining = amount;
        Map<IGasCanisterContainer, List<Pair<Integer, Long>>> containerMap = new HashMap<>();
        for (IGasCanisterContainer container : containerList) {
            if (container instanceof GasCanisterContainerContents canisterContents) {
                if (container.isEmpty() || !canisterContents.getGasInTank(0).is(gasType)) {
                    continue;
                }

                long drained = canisterContents.drain(0, remaining, GasAction.SIMULATE).getAmount();
                remaining -= drained;
                containerMap.put(container, List.of(Pair.of(0, drained)));
            }
            else if (container instanceof GasCanisterPackContainerContents packContents) {
                if (packContents.isEmpty()) {
                    continue;
                }

                List<Pair<Integer, Long>> pairList = new ArrayList<>();
                for (int i = 0; i < GasCanisterPackContainerContents.MAX_COUNT; i++) {
                    if (packContents.isEmpty(i) || !packContents.getGasInTank(i).is(gasType)) {
                        continue;
                    }

                    long drained = packContents.drain(i, remaining, GasAction.SIMULATE).getAmount();
                    remaining -= drained;
                    pairList.add(Pair.of(i, drained));
                }
                containerMap.put(container, pairList);
            }

            if (remaining <= 0) {
                break;
            }
        }

        if (remaining > 0) {
            return false;
        }

        containerMap.forEach((container, pairList) -> {
            if (container instanceof GasCanisterContainerContents canisterContents) {
                Pair<Integer, Long> pair = pairList.getFirst();
                canisterContents.drain(pair.getFirst(), pair.getSecond(), GasAction.EXECUTE);
            }
            else if (container instanceof GasCanisterPackContainerContents packContents) {
                for (Pair<Integer, Long> pair : pairList) {
                    int slot = pair.getFirst();
                    long drainAmount = pair.getSecond();
                    packContents.drain(slot, drainAmount, GasAction.EXECUTE);
                    if (!(player instanceof ServerPlayer serverPlayer) || !GasCanisterPackUtils.isCanisterPackMenuOpened(serverPlayer, packContents.getContainer()) || !(serverPlayer.containerMenu instanceof GasCanisterPackMenu menu)) {
                        continue;
                    }

                    menu.updateCanister(slot, amount);
                    CatnipServices.NETWORK.sendToClient(serverPlayer, new GasCanisterPackMenuSyncPacket(slot, amount));
                }
            }
        });
        return true;
    }
}