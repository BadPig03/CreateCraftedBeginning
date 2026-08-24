package net.ty.createcraftedbeginning.content.airtights.gascanister.container;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.api.gascanisters.events.CanisterContainerEvent;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClientPacket.InventoryStackSync;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenu;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenuSyncPacket;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackUtils;
import net.ty.createcraftedbeginning.core.ResourceTransaction;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CanisterContainerConsumers {
    private CanisterContainerConsumers() {
    }

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
            return canCoverCost(containers, gasType, amount);
        }

        if (!executeSupplier.get()) {
            return true;
        }

        CanisterContainerEvent containerEvent = new CanisterContainerEvent(player, gasType, amount, executeSupplier, simulate);
        NeoForge.EVENT_BUS.post(containerEvent);
        if (containerEvent.isCanceled()) {
            return true;
        }

        long adjustedAmount = containerEvent.getAmount();
        if (adjustedAmount <= 0) {
            return true;
        }

        Optional<List<InventoryStackSync>> drainResult = drainContainer(containers, gasType, adjustedAmount, simulate, player);
        if (drainResult.isEmpty()) {
            return false;
        }

        if (simulate || !(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }

        List<InventoryStackSync> inventoryUpdates = drainResult.get();
        if (!inventoryUpdates.isEmpty()) {
            CatnipServices.NETWORK.sendToClient(serverPlayer, new CanisterContainerClientPacket(inventoryUpdates));
        }
        return true;
    }

    public static Optional<AffordableFuel> findAffordableFuel(Player player, ToDoubleFunction<Gas> rawCostFunction) {
        Set<Gas> checkedGases = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IGasCanisterContainer container : CanisterContainerSuppliers.getAllSuppliers(player)) {
            for (int tankIndex = 0; tankIndex < container.getTanks(); tankIndex++) {
                GasStack storedGas = container.getGasInTank(tankIndex);
                if (storedGas.isEmpty()) {
                    continue;
                }

                Gas gasType = storedGas.getGasType();
                if (!checkedGases.add(gasType)) {
                    continue;
                }

                long requiredAmount = GasConsumptions.roundUp(rawCostFunction.applyAsDouble(gasType));
                if (requiredAmount < 0 || !interactContainer(player, gasType, requiredAmount, () -> true, true)) {
                    continue;
                }

                return Optional.of(new AffordableFuel(storedGas, requiredAmount));
            }
        }
        return Optional.empty();
    }

    public static Optional<AffordableFuel> findAffordableFuel(Player player, Gas selectedGas, ToDoubleFunction<Gas> rawCostFunction) {
        if (selectedGas.isEmpty()) {
            return Optional.empty();
        }

        GasStack selectedGasContent = GasStack.EMPTY;
        for (IGasCanisterContainer container : CanisterContainerSuppliers.getAllSuppliers(player)) {
            for (int tankIndex = 0; tankIndex < container.getTanks(); tankIndex++) {
                GasStack storedGas = container.getGasInTank(tankIndex);
                if (storedGas.isEmpty() || !storedGas.is(selectedGas)) {
                    continue;
                }

                selectedGasContent = storedGas;
                break;
            }

            if (selectedGasContent.isEmpty()) {
                continue;
            }

            break;
        }

        if (selectedGasContent.isEmpty()) {
            return Optional.empty();
        }

        long requiredAmount = GasConsumptions.roundUp(rawCostFunction.applyAsDouble(selectedGas));
        if (requiredAmount < 0 || !interactContainer(player, selectedGas, requiredAmount, () -> true, true)) {
            return Optional.empty();
        }
        return Optional.of(new AffordableFuel(selectedGasContent, requiredAmount));
    }

    private static Optional<List<InventoryStackSync>> drainContainer(List<IGasCanisterContainer> containers, Gas gasType, long amount, boolean simulate, Player player) {
        if (gasType.isEmpty() || amount <= 0) {
            return Optional.of(List.of());
        }

        if (simulate) {
            return canCoverCost(containers, gasType, amount) ? Optional.of(List.of()) : Optional.empty();
        }

        long remainingAmount = amount;
        Map<IGasCanisterContainer, List<PlannedContainerDrain>> drainPlan = new LinkedHashMap<>();
        for (IGasCanisterContainer container : containers) {
            if (container instanceof GasCanisterContainerContents canisterContents) {
                remainingAmount = planCanisterDrain(canisterContents, gasType, remainingAmount, drainPlan);
            }
            else if (container instanceof GasCanisterPackContainerContents packContents) {
                remainingAmount = planPackDrain(packContents, gasType, remainingAmount, drainPlan);
            }
            else {
                remainingAmount = planGenericDrain(container, gasType, remainingAmount, drainPlan);
            }
            if (remainingAmount > 0) {
                continue;
            }

            break;
        }

        if (remainingAmount > 0) {
            return Optional.empty();
        }

        Map<Integer, ItemStack> inventorySnapshot = snapshotPlannedInventory(player.getInventory(), drainPlan.keySet());
        if (!executeDrainPlan(drainPlan, player)) {
            return Optional.empty();
        }
        return Optional.of(collectInventoryUpdates(player.getInventory(), inventorySnapshot));
    }

    private static long planCanisterDrain(GasCanisterContainerContents canisterContents, Gas gasType, long remainingAmount, Map<IGasCanisterContainer, List<PlannedContainerDrain>> drainPlan) {
        if (canisterContents.isEmpty() || !canisterContents.getGasInTank(0).is(gasType)) {
            return remainingAmount;
        }

        ItemStack canisterStack = canisterContents.getContainer();
        long requestedDrainAmount = GasCanisterContainerContents.getEconomizedDrainAmount(remainingAmount, canisterStack);
        GasStack simulatedDrain = canisterContents.drain(0, requestedDrainAmount, GasAction.SIMULATE);
        if (simulatedDrain.isEmpty()) {
            return remainingAmount;
        }

        long coveredAmount = GasCanisterContainerContents.getLogicalAmountFromEconomizedDrain(simulatedDrain.getAmount(), canisterStack);
        coveredAmount = Math.min(coveredAmount, remainingAmount);
        if (coveredAmount <= 0) {
            return remainingAmount;
        }

        drainPlan.put(canisterContents, List.of(new PlannedContainerDrain(0, simulatedDrain.copy())));
        return remainingAmount - coveredAmount;
    }

    private static long planPackDrain(GasCanisterPackContainerContents packContents, Gas gasType, long remainingAmount, Map<IGasCanisterContainer, List<PlannedContainerDrain>> drainPlan) {
        if (packContents.isEmpty()) {
            return remainingAmount;
        }

        List<PlannedContainerDrain> plannedDrains = new ArrayList<>();
        for (int canisterSlot = 0; canisterSlot < GasCanisterPackContainerContents.MAX_COUNT; canisterSlot++) {
            if (remainingAmount <= 0) {
                break;
            }
            if (packContents.isEmpty(canisterSlot) || !packContents.getGasInTank(canisterSlot).is(gasType)) {
                continue;
            }

            if (packContents.isCreative(canisterSlot)) {
                remainingAmount = 0;
                break;
            }

            ItemStack canisterStack = packContents.getCanister(canisterSlot);
            long requestedDrainAmount = GasCanisterContainerContents.getEconomizedDrainAmount(remainingAmount, canisterStack);
            GasStack simulatedDrain = packContents.drain(canisterSlot, requestedDrainAmount, GasAction.SIMULATE);
            if (simulatedDrain.isEmpty()) {
                continue;
            }

            long coveredAmount = GasCanisterContainerContents.getLogicalAmountFromEconomizedDrain(simulatedDrain.getAmount(), canisterStack);
            coveredAmount = Math.min(coveredAmount, remainingAmount);
            if (coveredAmount <= 0) {
                continue;
            }

            remainingAmount -= coveredAmount;
            plannedDrains.add(new PlannedContainerDrain(canisterSlot, simulatedDrain.copy()));
        }

        if (plannedDrains.isEmpty()) {
            return remainingAmount;
        }

        drainPlan.put(packContents, List.copyOf(plannedDrains));
        return remainingAmount;
    }

    private static long planGenericDrain(IGasCanisterContainer container, Gas gasType, long remainingAmount, Map<IGasCanisterContainer, List<PlannedContainerDrain>> drainPlan) {
        List<PlannedContainerDrain> plannedDrains = new ArrayList<>();
        for (int tankIndex = 0; tankIndex < container.getTanks() && remainingAmount > 0; tankIndex++) {
            GasStack storedGas = container.getGasInTank(tankIndex);
            if (storedGas.isEmpty() || !storedGas.is(gasType)) {
                continue;
            }

            GasStack simulatedDrain = container.drain(tankIndex, remainingAmount, GasAction.SIMULATE);
            if (simulatedDrain.isEmpty() || !simulatedDrain.is(gasType)) {
                continue;
            }

            long coveredAmount = Math.min(simulatedDrain.getAmount(), remainingAmount);
            if (coveredAmount <= 0) {
                continue;
            }

            remainingAmount -= coveredAmount;
            plannedDrains.add(new PlannedContainerDrain(tankIndex, simulatedDrain.copyWithAmount(coveredAmount)));
        }

        if (!plannedDrains.isEmpty()) {
            drainPlan.put(container, List.copyOf(plannedDrains));
        }
        return remainingAmount;
    }

    private static boolean executeDrainPlan(Map<IGasCanisterContainer, List<PlannedContainerDrain>> drainPlan, Player player) {
        ResourceTransaction transaction = new ResourceTransaction();
        drainPlan.forEach((container, plannedDrains) -> plannedDrains.forEach(plannedDrain -> transaction.add(ResourceTransaction.participant(() -> GasStack.matches(container.drain(plannedDrain.tankIndex(), plannedDrain.drainedGas(), GasAction.SIMULATE), plannedDrain.drainedGas()), () -> container.getGasInTank(plannedDrain.tankIndex()).copy(), () -> executePlannedDrain(container, plannedDrain), tankSnapshot -> restoreContainerTank(container, plannedDrain.tankIndex(), tankSnapshot)))));
        if (!transaction.commit()) {
            return false;
        }

        drainPlan.forEach((container, plannedDrains) -> {
            if (!(container instanceof GasCanisterPackContainerContents packContents)) {
                return;
            }

            plannedDrains.forEach(plannedDrain -> syncPackMenu(player, packContents, plannedDrain.tankIndex()));
        });
        return true;
    }

    private static boolean executePlannedDrain(IGasCanisterContainer container, PlannedContainerDrain drain) {
        if (!GasStack.matches(container.drain(drain.tankIndex(), drain.drainedGas(), GasAction.EXECUTE), drain.drainedGas())) {
            return false;
        }

        if (isGenericContainer(container)) {
            container.save();
        }
        return true;
    }

    private static void restoreContainerTank(IGasCanisterContainer container, int tankIndex, GasStack tankSnapshot) {
        GasStack currentGas = container.getGasInTank(tankIndex).copy();
        if (GasStack.matches(currentGas, tankSnapshot)) {
            return;
        }

        if (!currentGas.isEmpty()) {
            GasStack removedGas = container.drain(tankIndex, currentGas, GasAction.EXECUTE);
            if (!GasStack.matches(removedGas, currentGas)) {
                throw new IllegalStateException("Failed to clear gas canister container during transaction rollback");
            }
        }
        if (!tankSnapshot.isEmpty() && container.fill(tankIndex, tankSnapshot.copy(), GasAction.EXECUTE) != tankSnapshot.getAmount()) {
            throw new IllegalStateException("Failed to restore gas canister container during transaction rollback");
        }
        if (!GasStack.matches(container.getGasInTank(tankIndex), tankSnapshot)) {
            throw new IllegalStateException("Gas canister container rollback produced an unexpected state");
        }
        if (!isGenericContainer(container)) {
            return;
        }

        container.save();
    }

    private static boolean isGenericContainer(IGasCanisterContainer container) {
        return !(container instanceof GasCanisterContainerContents) && !(container instanceof GasCanisterPackContainerContents);
    }

    private static void syncPackMenu(Player player, GasCanisterPackContainerContents packContents, int canisterSlot) {
        if (!(player instanceof ServerPlayer serverPlayer) || !GasCanisterPackUtils.isCanisterPackMenuOpened(serverPlayer, packContents.getContainer()) || !(serverPlayer.containerMenu instanceof GasCanisterPackMenu menu)) {
            return;
        }

        ItemStack canisterStack = packContents.getCanister(canisterSlot);
        menu.updateCanister(canisterSlot, canisterStack);
        CatnipServices.NETWORK.sendToClient(serverPlayer, new GasCanisterPackMenuSyncPacket(menu.containerId, canisterSlot, canisterStack));
    }

    private static Map<Integer, ItemStack> snapshotPlannedInventory(Inventory inventory, Iterable<IGasCanisterContainer> containers) {
        Map<Integer, ItemStack> inventorySnapshot = new LinkedHashMap<>();
        for (IGasCanisterContainer container : containers) {
            int inventorySlot = findInventorySlot(inventory, container.getContainer());
            if (inventorySlot < 0 || inventorySnapshot.containsKey(inventorySlot)) {
                continue;
            }

            inventorySnapshot.put(inventorySlot, inventory.getItem(inventorySlot).copy());
        }
        return inventorySnapshot;
    }

    private static int findInventorySlot(Inventory inventory, ItemStack targetStack) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot) != targetStack) {
                continue;
            }

            return slot;
        }
        return -1;
    }

    private static @Unmodifiable List<InventoryStackSync> collectInventoryUpdates(Inventory inventory, Map<Integer, ItemStack> inventorySnapshot) {
        List<InventoryStackSync> inventoryUpdates = new ArrayList<>();
        inventorySnapshot.forEach((slot, expectedStack) -> {
            ItemStack currentStack = inventory.getItem(slot);
            if (ItemStack.matches(expectedStack, currentStack)) {
                return;
            }

            inventoryUpdates.add(new InventoryStackSync(slot, expectedStack, currentStack));
        });
        return List.copyOf(inventoryUpdates);
    }

    private static boolean canCoverCost(List<IGasCanisterContainer> containers, Gas gasType, long amount) {
        long remainingAmount = amount;
        for (IGasCanisterContainer container : containers) {
            if (container instanceof GasCanisterContainerContents contents) {
                if (container.isEmpty() || !contents.getGasInTank(0).is(gasType)) {
                    continue;
                }

                ItemStack canisterStack = contents.getContainer();
                long requestedDrainAmount = GasCanisterContainerContents.getEconomizedDrainAmount(remainingAmount, canisterStack);
                long drainedAmount = contents.drain(0, requestedDrainAmount, GasAction.SIMULATE).getAmount();
                if (drainedAmount <= 0) {
                    continue;
                }

                long coveredAmount = GasCanisterContainerContents.getLogicalAmountFromEconomizedDrain(drainedAmount, canisterStack);
                remainingAmount -= Math.min(coveredAmount, remainingAmount);
            }
            else if (container instanceof GasCanisterPackContainerContents contents) {
                if (contents.isEmpty()) {
                    continue;
                }

                for (int canisterSlot = 0; canisterSlot < GasCanisterPackContainerContents.MAX_COUNT && remainingAmount > 0; canisterSlot++) {
                    if (contents.isEmpty(canisterSlot) || !contents.getGasInTank(canisterSlot).is(gasType)) {
                        continue;
                    }

                    if (contents.isCreative(canisterSlot)) {
                        return true;
                    }

                    ItemStack canisterStack = contents.getCanister(canisterSlot);
                    long requestedDrainAmount = GasCanisterContainerContents.getEconomizedDrainAmount(remainingAmount, canisterStack);
                    long drainedAmount = contents.drain(canisterSlot, requestedDrainAmount, GasAction.SIMULATE).getAmount();
                    if (drainedAmount <= 0) {
                        continue;
                    }

                    long coveredAmount = GasCanisterContainerContents.getLogicalAmountFromEconomizedDrain(drainedAmount, canisterStack);
                    remainingAmount -= Math.min(coveredAmount, remainingAmount);
                }
            }
            else {
                for (int tankIndex = 0; tankIndex < container.getTanks() && remainingAmount > 0; tankIndex++) {
                    GasStack storedGas = container.getGasInTank(tankIndex);
                    if (storedGas.isEmpty() || !storedGas.is(gasType)) {
                        continue;
                    }

                    GasStack simulatedDrain = container.drain(tankIndex, remainingAmount, GasAction.SIMULATE);
                    if (simulatedDrain.isEmpty() || !simulatedDrain.is(gasType)) {
                        continue;
                    }

                    remainingAmount -= Math.min(simulatedDrain.getAmount(), remainingAmount);
                }
            }

            if (remainingAmount <= 0) {
                return true;
            }
        }
        return false;
    }

    private record PlannedContainerDrain(int tankIndex, GasStack drainedGas) {
        private PlannedContainerDrain {
            drainedGas = drainedGas.copy();
        }
    }

    public record AffordableFuel(GasStack gasContent, long amount) {
        public AffordableFuel {
            gasContent = gasContent.copy();
            if (gasContent.isEmpty()) {
                throw new IllegalArgumentException("Affordable fuel must contain a non-empty gas stack");
            }
            if (amount < 0) {
                throw new IllegalArgumentException("Affordable fuel amount must be non-negative: " + amount);
            }
        }

        public Gas gasType() {
            return gasContent.getGasType();
        }
    }
}
