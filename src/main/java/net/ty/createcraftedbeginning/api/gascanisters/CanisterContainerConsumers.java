package net.ty.createcraftedbeginning.api.gascanisters;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.events.CanisterContainerEvent;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenu;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenuSyncPacket;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CanisterContainerConsumers {
    private static final String COMPOUND_KEY_CANISTER = "Canister";

    private CanisterContainerConsumers() {
    }

    /**
     * Handles interaction with the container.
     *
     * @param player          the player performing the operation
     * @param gasType         the gas type to inspect or process
     * @param amount          the amount to use
     * @param executeSupplier the supplier used to obtain the execute
     * @param simulate        whether the operation should be simulated
     * @return {@code true} if the condition is satisfied; otherwise {@code false}
     */
    public static boolean interactContainer(Player player, Gas gasType, long amount, Supplier<Boolean> executeSupplier, boolean simulate) {
        if (player.isCreative() || gasType.isEmpty() || amount <= 0) {
            return true;
        }

        if (player.level().isClientSide && !simulate) {
            return true;
        }

        List<IGasCanisterContainer> containers = CanisterContainerSuppliers.getAllSuppliers(player);
        if (containers.isEmpty()) {
            return false;
        }

        if (player.level().isClientSide) {
            return canCoverCost(containers, gasType, amount, player);
        }

        if (!executeSupplier.get()) {
            return true;
        }

        CanisterContainerEvent event = new CanisterContainerEvent(player, gasType, amount, executeSupplier, simulate);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return true;
        }

        long adjustedAmount = event.getAmount();
        if (adjustedAmount <= 0) {
            return true;
        }

        if (!drainContainer(containers, gasType, adjustedAmount, simulate, player)) {
            return false;
        }

        if (simulate || !(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }

        CatnipServices.NETWORK.sendToClient(serverPlayer, new CanisterContainerClientPacket(new GasStack(gasType, adjustedAmount)));
        return true;
    }

    /**
     * Finds the affordable fuel.
     *
     * @param player          the player performing the operation
     * @param rawCostFunction the raw cost function to use
     * @return an optional containing the matching value, or an empty optional when none is found
     */
    public static Optional<AffordableFuel> findAffordableFuel(Player player, ToDoubleFunction<Gas> rawCostFunction) {
        Set<Gas> checkedGases = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IGasCanisterContainer container : CanisterContainerSuppliers.getAllSuppliers(player)) {
            for (int tank = 0; tank < container.getTanks(); tank++) {
                GasStack content = container.getGasInTank(tank);
                if (content.isEmpty()) {
                    continue;
                }

                Gas gas = content.getGasType();
                if (!checkedGases.add(gas)) {
                    continue;
                }

                long amount = GasConsumptionUtils.roundUp(rawCostFunction.applyAsDouble(gas));
                if (amount < 0 || !interactContainer(player, gas, amount, () -> true, true)) {
                    continue;
                }

                return Optional.of(new AffordableFuel(content, amount));
            }
        }

        return Optional.empty();
    }

    /**
     * Applies the client container sync.
     *
     * @param player     the player performing the operation
     * @param gasContent the gas content to use
     */
    public static void applyClientContainerSync(Player player, GasStack gasContent) {
        if (!player.level().isClientSide || player.isCreative() || gasContent.isEmpty() || gasContent.getAmount() <= 0) {
            return;
        }

        List<IGasCanisterContainer> containers = CanisterContainerSuppliers.getAllSuppliers(player);
        if (containers.isEmpty()) {
            return;
        }

        drainContainer(containers, gasContent.getGasType(), gasContent.getAmount(), false, player);
    }

    /**
     * Attempts to satisfy a logical gas cost from the ordered canister containers.
     * Execution first builds a complete drain plan so an insufficient balance never causes a partial drain.
     *
     * @param containers the ordered canister containers to consume from
     * @param gasType    the gas type required by the action
     * @param amount     the logical gas amount required by the action
     * @param simulate   whether to test coverage without consuming gas
     * @param player     the player used for registry access and menu synchronization
     * @return {@code true} if the complete logical cost can be covered; otherwise {@code false}
     */
    private static boolean drainContainer(List<IGasCanisterContainer> containers, Gas gasType, long amount, boolean simulate, Player player) {
        if (gasType.isEmpty() || amount <= 0) {
            return true;
        }
        if (simulate) {
            return canCoverCost(containers, gasType, amount, player);
        }

        long remaining = amount;
        Map<IGasCanisterContainer, List<Pair<Integer, Long>>> drainPlan = new HashMap<>();
        for (IGasCanisterContainer container : containers) {
            if (container instanceof GasCanisterContainerContents canister) {
                remaining = planCanisterDrain(canister, gasType, remaining, drainPlan);
            }
            else if (container instanceof GasCanisterPackContainerContents pack) {
                remaining = planPackDrain(pack, gasType, remaining, player, drainPlan);
            }

            if (remaining <= 0) {
                break;
            }
        }

        if (remaining > 0) {
            return false;
        }

        executeDrainPlan(drainPlan, player);
        return true;
    }

    private static long planCanisterDrain(GasCanisterContainerContents contents, Gas gasType, long remaining, Map<IGasCanisterContainer, List<Pair<Integer, Long>>> drainPlan) {
        if (contents.isEmpty() || !contents.getGasInTank(0).is(gasType)) {
            return remaining;
        }

        ItemStack canister = contents.getContainer();
        long requestedDrain = GasCanisterContainerContents.getEconomizedDrainAmount(remaining, canister);
        long drained = contents.drain(0, requestedDrain, GasAction.SIMULATE).getAmount();
        if (drained <= 0) {
            return remaining;
        }

        long covered = GasCanisterContainerContents.getLogicalAmountFromEconomizedDrain(drained, canister);
        covered = Math.min(covered, remaining);
        if (covered <= 0) {
            return remaining;
        }

        drainPlan.put(contents, List.of(Pair.of(0, drained)));
        return remaining - covered;
    }

    private static long planPackDrain(GasCanisterPackContainerContents contents, Gas gasType, long remaining, Player player, Map<IGasCanisterContainer, List<Pair<Integer, Long>>> drainPlan) {
        if (contents.isEmpty()) {
            return remaining;
        }

        List<Pair<Integer, Long>> drains = new ArrayList<>();
        for (int slot = 0; slot < GasCanisterPackContainerContents.MAX_COUNT; slot++) {
            if (remaining <= 0) {
                break;
            }
            if (contents.isEmpty(slot) || !contents.getGasInTank(slot).is(gasType)) {
                continue;
            }
            if (contents.getCreatives(slot)) {
                remaining = 0;
                break;
            }

            ItemStack canister = ItemStack.parseOptional(player.level().registryAccess(), contents.getCompoundTag(slot).getCompound(COMPOUND_KEY_CANISTER));
            long requestedDrain = GasCanisterContainerContents.getEconomizedDrainAmount(remaining, canister);
            long drained = contents.drain(slot, requestedDrain, GasAction.SIMULATE).getAmount();
            if (drained <= 0) {
                continue;
            }

            long covered = GasCanisterContainerContents.getLogicalAmountFromEconomizedDrain(drained, canister);
            covered = Math.min(covered, remaining);
            if (covered <= 0) {
                continue;
            }

            remaining -= covered;
            drains.add(Pair.of(slot, drained));
        }

        if (!drains.isEmpty()) {
            drainPlan.put(contents, drains);
        }
        return remaining;
    }

    private static void executeDrainPlan(Map<IGasCanisterContainer, List<Pair<Integer, Long>>> drainPlan, Player player) {
        drainPlan.forEach((container, drains) -> {
            if (container instanceof GasCanisterContainerContents contents) {
                Pair<Integer, Long> drain = drains.getFirst();
                contents.drain(drain.getFirst(), drain.getSecond(), GasAction.EXECUTE);
                return;
            }
            if (!(container instanceof GasCanisterPackContainerContents contents)) {
                return;
            }

            for (Pair<Integer, Long> drain : drains) {
                int slot = drain.getFirst();
                long amount = drain.getSecond();
                contents.drain(slot, amount, GasAction.EXECUTE);
                syncPackMenu(player, contents, slot, amount);
            }
        });
    }

    private static void syncPackMenu(Player player, GasCanisterPackContainerContents contents, int slot, long amount) {
        if (!(player instanceof ServerPlayer serverPlayer) || !GasCanisterPackUtils.isCanisterPackMenuOpened(serverPlayer, contents.getContainer()) || !(serverPlayer.containerMenu instanceof GasCanisterPackMenu menu)) {
            return;
        }

        menu.updateCanister(slot, amount);
        CatnipServices.NETWORK.sendToClient(serverPlayer, new GasCanisterPackMenuSyncPacket(slot, amount));
    }

    /**
     * Checks whether the ordered canister containers can cover the requested logical gas cost.
     * This simulation path avoids allocating a drain plan and never mutates container contents.
     *
     * @param containers the ordered canister containers to inspect
     * @param gasType    the gas type required by the action
     * @param amount     the logical gas amount required by the action
     * @param player     the player used to decode canisters stored inside packs
     * @return {@code true} if the complete logical cost can be covered; otherwise {@code false}
     */
    private static boolean canCoverCost(List<IGasCanisterContainer> containers, Gas gasType, long amount, Player player) {
        long remaining = amount;
        for (IGasCanisterContainer container : containers) {
            if (container instanceof GasCanisterContainerContents contents) {
                if (container.isEmpty() || !contents.getGasInTank(0).is(gasType)) {
                    continue;
                }

                ItemStack canister = contents.getContainer();
                long requestedDrain = GasCanisterContainerContents.getEconomizedDrainAmount(remaining, canister);
                long drained = contents.drain(0, requestedDrain, GasAction.SIMULATE).getAmount();
                if (drained <= 0) {
                    continue;
                }

                long covered = GasCanisterContainerContents.getLogicalAmountFromEconomizedDrain(drained, canister);
                remaining -= Math.min(covered, remaining);
            }
            else if (container instanceof GasCanisterPackContainerContents contents) {
                if (contents.isEmpty()) {
                    continue;
                }

                for (int slot = 0; slot < GasCanisterPackContainerContents.MAX_COUNT && remaining > 0; slot++) {
                    if (contents.isEmpty(slot) || !contents.getGasInTank(slot).is(gasType)) {
                        continue;
                    }
                    if (contents.getCreatives(slot)) {
                        return true;
                    }

                    ItemStack canister = ItemStack.parseOptional(player.level().registryAccess(), contents.getCompoundTag(slot).getCompound(COMPOUND_KEY_CANISTER));
                    long requestedDrain = GasCanisterContainerContents.getEconomizedDrainAmount(remaining, canister);
                    long drained = contents.drain(slot, requestedDrain, GasAction.SIMULATE).getAmount();
                    if (drained <= 0) {
                        continue;
                    }

                    long covered = GasCanisterContainerContents.getLogicalAmountFromEconomizedDrain(drained, canister);
                    remaining -= Math.min(covered, remaining);
                }
            }

            if (remaining <= 0) {
                return true;
            }
        }

        return false;
    }

    public record AffordableFuel(GasStack gasContent, long amount) {
        /**
         * Creates a new {@code AffordableFuel} instance.
         *
         * @param gasContent the gas content to use
         * @param amount     the amount to use
         */
        public AffordableFuel {
            gasContent = gasContent.copy();
            if (gasContent.isEmpty()) {
                throw new IllegalArgumentException("Affordable fuel must contain a non-empty gas stack");
            }
            if (amount < 0) {
                throw new IllegalArgumentException("Affordable fuel amount must be non-negative: " + amount);
            }
        }

        /**
         * Sets the gas type stored by this builder.
         *
         * @return the resulting gas
         */
        public Gas gasType() {
            return gasContent.getGasType();
        }
    }
}
