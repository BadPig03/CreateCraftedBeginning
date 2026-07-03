package net.ty.createcraftedbeginning.api.gascanisters;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.api.gascanisters.events.CanisterContainerEvent;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenu;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenuSyncPacket;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CanisterContainerConsumers {
    private static final String COMPOUND_KEY_CANISTER = "Canister";

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
     * @param executeSupplier condition that must return true for the operation to proceed
     * @param simulate        whether to only check if the gas cost can be satisfied without actually consuming gas
     * @return true if the operation was successful, bypassed, or canceled; false if the operation failed due to insufficient gas
     * @see CanisterContainerEvent
     * @see #drainContainer(List, Gas, long, boolean, Player)
     * @see CanisterContainerClientPacket
     * @see GasCanisterPackMenu
     */
    public static boolean interactContainer(Player player, Gas gasType, long amount, Supplier<Boolean> executeSupplier, boolean simulate) {
        if (player.level().isClientSide || player.isCreative() || gasType.isEmpty() || amount <= 0) {
            return true;
        }

        List<IGasCanisterContainer> containerList = CanisterContainerSuppliers.getAllSuppliers(player);
        if (containerList.isEmpty()) {
            return false;
        }

        if (!executeSupplier.get()) {
            return true;
        }

        CanisterContainerEvent event = new CanisterContainerEvent(player, gasType, amount, executeSupplier, simulate);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return true;
        }

        long newAmount = event.getAmount();
        if (newAmount <= 0) {
            return true;
        }

        boolean result = drainContainer(containerList, gasType, newAmount, simulate, player);
        if (!result || !(player instanceof ServerPlayer serverPlayer)) {
            return result;
        }

        if (simulate) {
            return true;
        }

        CatnipServices.NETWORK.sendToClient(serverPlayer, new CanisterContainerClientPacket(new GasStack(gasType, newAmount)));
        return true;
    }

    /**
     * Applies a client-side synchronization of gas container contents based on a given gas stack.
     * <p>
     * This method only executes on the client side. It checks if the provided {@code gasContent}
     * should be applied: if the player is in creative mode, or the gas content is empty or has
     * zero or negative amount, no action is taken.
     * </p>
     * <p>
     * Otherwise, it retrieves all available gas canister containers from the player via
     * {@link CanisterContainerSuppliers#getAllSuppliers(Player)} and drains the specified
     * amount of gas from them using {@link #drainContainer(List, Gas, long, boolean, Player)}.
     * This ensures the client's visual representation of gas containers stays consistent with
     * the server state without requiring a full network update.
     * </p>
     *
     * @param player     the player whose containers should be synchronized (must not be null)
     * @param gasContent the gas stack representing the type and amount to drain; may be empty,
     *                   but must not be null
     * @see #drainContainer(List, Gas, long, boolean, Player)
     * @see CanisterContainerSuppliers#getAllSuppliers(Player)
     */
    public static void applyClientContainerSync(Player player, GasStack gasContent) {
        if (!player.level().isClientSide || player.isCreative() || gasContent.isEmpty() || gasContent.getAmount() <= 0) {
            return;
        }

        List<IGasCanisterContainer> containerList = CanisterContainerSuppliers.getAllSuppliers(player);
        if (containerList.isEmpty()) {
            return;
        }

        drainContainer(containerList, gasContent.getGasType(), gasContent.getAmount(), false, player);
    }

    /**
     * Attempts to satisfy a logical gas cost from the given ordered canister containers.
     *
     * <p>The {@code amount} parameter represents the logical gas cost required by the
     * action. This is not always the same as the amount physically drained from the
     * canisters. Canisters with the Economize enchantment consume less physical gas while
     * still covering the same logical cost.</p>
     *
     * <p>The method first builds a complete simulated drain plan. If the full logical
     * amount cannot be covered by the available containers, the method returns {@code false}
     * and no gas is consumed. If the full amount can be covered, the planned physical drain
     * amounts are either executed or only validated depending on {@code simulate}.</p>
     *
     * <p>Gas canister packs are handled per internal slot, allowing different canisters in
     * the same pack to use different Economize levels. Creative canister slots are treated
     * as unlimited sources for the matching gas type and do not physically lose gas.</p>
     *
     * <p>When {@code simulate} is {@code false} and a gas canister pack menu is open on the
     * server, the menu is updated using the physical drain amount so its displayed internal
     * canister contents stay in sync. Simulated calls never update menus or send sync
     * packets.</p>
     *
     * @param containerList the ordered gas canister containers to consume from
     * @param gasType       the gas type required by the action
     * @param amount        the logical gas amount required by the action
     * @param simulate      whether to only check if the logical cost can be satisfied
     *                      without actually consuming gas
     * @param player        the player performing the action, used for registry access and
     *                      server-side menu synchronization
     * @return {@code true} if the full logical gas amount can be satisfied; {@code false}
     * if the available containers cannot satisfy the full logical cost
     */
    private static boolean drainContainer(List<IGasCanisterContainer> containerList, Gas gasType, long amount, boolean simulate, Player player) {
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

                ItemStack canister = canisterContents.getContainer();
                long physicalWanted = GasCanisterContainerContents.getEconomizedDrainAmount(remaining, canister);
                long physicalDrained = canisterContents.drain(0, physicalWanted, GasAction.SIMULATE).getAmount();
                if (physicalDrained <= 0) {
                    continue;
                }

                long logicalCovered = GasCanisterContainerContents.getLogicalAmountFromEconomizedDrain(physicalDrained, canister);
                logicalCovered = Math.min(logicalCovered, remaining);
                if (logicalCovered <= 0) {
                    continue;
                }

                remaining -= logicalCovered;
                containerMap.put(container, List.of(Pair.of(0, physicalDrained)));
            }
            else if (container instanceof GasCanisterPackContainerContents packContents) {
                if (packContents.isEmpty()) {
                    continue;
                }

                List<Pair<Integer, Long>> pairList = new ArrayList<>();
                for (int i = 0; i < GasCanisterPackContainerContents.MAX_COUNT; i++) {
                    if (remaining <= 0) {
                        break;
                    }

                    if (packContents.isEmpty(i) || !packContents.getGasInTank(i).is(gasType)) {
                        continue;
                    }

                    long physicalDrained;
                    long logicalCovered;
                    if (packContents.getCreatives(i)) {
                        remaining = 0;
                        break;
                    }
                    else {
                        ItemStack innerCanister = ItemStack.parseOptional(player.level().registryAccess(), packContents.getCompoundTag(i).getCompound(COMPOUND_KEY_CANISTER));
                        long physicalWanted = GasCanisterContainerContents.getEconomizedDrainAmount(remaining, innerCanister);
                        physicalDrained = packContents.drain(i, physicalWanted, GasAction.SIMULATE).getAmount();
                        if (physicalDrained <= 0) {
                            continue;
                        }

                        logicalCovered = GasCanisterContainerContents.getLogicalAmountFromEconomizedDrain(physicalDrained, innerCanister);
                        logicalCovered = Math.min(logicalCovered, remaining);
                    }

                    if (logicalCovered <= 0) {
                        continue;
                    }

                    remaining -= logicalCovered;
                    pairList.add(Pair.of(i, physicalDrained));
                }

                if (!pairList.isEmpty()) {
                    containerMap.put(container, pairList);
                }
            }

            if (remaining <= 0) {
                break;
            }
        }

        if (remaining > 0) {
            return false;
        }

        GasAction action = simulate ? GasAction.SIMULATE : GasAction.EXECUTE;
        containerMap.forEach((container, pairList) -> {
            if (container instanceof GasCanisterContainerContents canisterContents) {
                Pair<Integer, Long> pair = pairList.getFirst();
                canisterContents.drain(pair.getFirst(), pair.getSecond(), action);
            }
            else if (container instanceof GasCanisterPackContainerContents packContents) {
                for (Pair<Integer, Long> pair : pairList) {
                    int slot = pair.getFirst();
                    long drainAmount = pair.getSecond();
                    packContents.drain(slot, drainAmount, action);
                    if (simulate || !(player instanceof ServerPlayer serverPlayer) || !GasCanisterPackUtils.isCanisterPackMenuOpened(serverPlayer, packContents.getContainer()) || !(serverPlayer.containerMenu instanceof GasCanisterPackMenu menu)) {
                        continue;
                    }

                    menu.updateCanister(slot, drainAmount);
                    CatnipServices.NETWORK.sendToClient(serverPlayer, new GasCanisterPackMenuSyncPacket(slot, drainAmount));
                }
            }
        });

        return true;
    }
}