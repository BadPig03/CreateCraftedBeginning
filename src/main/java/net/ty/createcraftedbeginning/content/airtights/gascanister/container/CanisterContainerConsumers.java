package net.ty.createcraftedbeginning.content.airtights.gascanister.container;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.api.canister.event.CanisterContainerEvent;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUsageContext;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClientPacket.InventoryStackSync;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenu;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenuSyncPacket;
import net.ty.createcraftedbeginning.foundation.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.foundation.transaction.TransactionParticipant;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

    public static boolean interactContainer(Player player, AffordableFuel fuel, Supplier<Boolean> executeSupplier, boolean simulate) {
        return interactContainer(player, fuel.gasType(), fuel.amount(), executeSupplier, simulate, null, null);
    }

    public static boolean executeWithFuel(Player player, AffordableFuel fuel, TransactionParticipant<?> operation) {
        return !player.level().isClientSide && interactContainer(player, fuel.gasType(), fuel.amount(), () -> true, false, operation, null);
    }

    public static Optional<AffordableFuel> findAffordableFuel(Player player, ToDoubleFunction<GasUsageContext> rawCostFunction) {
        Set<Gas> checkedGases = Collections.newSetFromMap(new IdentityHashMap<>());
        for (GasCanisterContainer container : CanisterContainerSuppliers.getAllSuppliers(player)) {
            for (int tankIndex = 0; tankIndex < container.getTanks(); tankIndex++) {
                GasStack storedGas = container.getGasInTank(tankIndex);
                if (storedGas.isEmpty()) {
                    continue;
                }

                long sourcePressurePa = container.getTankPressurePa(tankIndex);
                Gas gasType = storedGas.getGasType();
                if (!gasType.isUsableInEquipment() || !checkedGases.add(gasType)) {
                    continue;
                }

                Optional<AffordableFuel> fuel = createAffordableFuel(player, storedGas, sourcePressurePa, rawCostFunction);
                if (fuel.isPresent()) {
                    return fuel;
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<AffordableFuel> findAffordableFuel(Player player, Gas selectedGas, ToDoubleFunction<GasUsageContext> rawCostFunction) {
        if (selectedGas.isEmpty() || !selectedGas.isUsableInEquipment()) {
            return Optional.empty();
        }

        for (GasCanisterContainer container : CanisterContainerSuppliers.getAllSuppliers(player)) {
            for (int tankIndex = 0; tankIndex < container.getTanks(); tankIndex++) {
                GasStack storedGas = container.getGasInTank(tankIndex);
                if (storedGas.isEmpty() || !storedGas.is(selectedGas)) {
                    continue;
                }

                long sourcePressurePa = container.getTankPressurePa(tankIndex);
                return createAffordableFuel(player, storedGas, sourcePressurePa, rawCostFunction);
            }
        }
        return Optional.empty();
    }

    private static boolean interactContainer(Player player, Gas gasType, long amount, Supplier<Boolean> executeSupplier, boolean simulate, @Nullable TransactionParticipant<?> operation, @Nullable ConsumptionBatch batch) {
        if (!gasType.isUsableInEquipment()) {
            return false;
        }

        if (player.isCreative() || gasType.isEmpty() || amount <= 0) {
            return commitWithoutDrain(operation);
        }

        if (player.level().isClientSide && !simulate) {
            return true;
        }

        List<GasCanisterContainer> containers = CanisterContainerSuppliers.getAllSuppliers(player);
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
            return commitWithoutDrain(operation);
        }

        long adjustedAmount = containerEvent.getAmount();
        if (adjustedAmount <= 0) {
            return commitWithoutDrain(operation);
        }

        if (batch != null) {
            CanisterContainerSuppliers.invalidateCache(player);
            containers = new ArrayList<>(CanisterContainerSuppliers.getAllSuppliers(player));
            for (GasCanisterContainer container : containers) {
                batch.supplierOrder.computeIfAbsent(container.getContainer(), stack -> batch.supplierOrder.size());
            }
            containers.sort(Comparator.comparingInt(container -> batch.supplierOrder.get(container.getContainer())));
        }
        Optional<List<InventoryStackSync>> drainResult = drainContainer(containers, gasType, adjustedAmount, simulate, player, operation, batch);
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

    private static boolean commitWithoutDrain(@Nullable TransactionParticipant<?> operation) {
        return operation == null || new ResourceTransaction().add(operation).commit();
    }

    private static Optional<AffordableFuel> createAffordableFuel(Player player, GasStack storedGas, long sourcePressurePa, ToDoubleFunction<GasUsageContext> rawCostFunction) {
        GasUsageContext usageContext = new GasUsageContext(storedGas.copyWithAmount(1), sourcePressurePa);
        long requiredAmount = GasConsumptionMath.roundUp(rawCostFunction.applyAsDouble(usageContext));
        if (requiredAmount < 0 || !interactContainer(player, storedGas.getGasType(), requiredAmount, () -> true, true, null, null)) {
            return Optional.empty();
        }

        return Optional.of(new AffordableFuel(storedGas, sourcePressurePa, requiredAmount));
    }

    private static Optional<List<InventoryStackSync>> drainContainer(List<GasCanisterContainer> containers, Gas gasType, long amount, boolean simulate, Player player, @Nullable TransactionParticipant<?> operation, @Nullable ConsumptionBatch batch) {
        if (gasType.isEmpty() || amount <= 0) {
            return Optional.of(List.of());
        }

        if (simulate) {
            if (!canCoverCost(containers, gasType, amount)) {
                return Optional.empty();
            }

            return Optional.of(List.of());
        }

        long remainingAmount = amount;
        Map<GasCanisterContainer, List<PlannedContainerDrain>> drainPlan = new LinkedHashMap<>();
        Map<ContainerTank, EconomizedConsumption> pendingConsumption = new HashMap<>();
        for (GasCanisterContainer container : containers) {
            if (batch != null && (container instanceof GasCanisterContainerContents || container instanceof GasCanisterPackContainerContents)) {
                remainingAmount = batch.planDrain(container, gasType, remainingAmount, drainPlan, pendingConsumption);
            }
            else if (container instanceof GasCanisterContainerContents canisterContents) {
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
        if (!executeDrainPlan(drainPlan, player, operation)) {
            return Optional.empty();
        }

        if (batch != null) {
            batch.consumption.putAll(pendingConsumption);
        }
        return Optional.of(collectInventoryUpdates(player.getInventory(), inventorySnapshot));
    }

    private static long planCanisterDrain(GasCanisterContainerContents canisterContents, Gas gasType, long remainingAmount, Map<GasCanisterContainer, List<PlannedContainerDrain>> drainPlan) {
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

    private static long planPackDrain(GasCanisterPackContainerContents packContents, Gas gasType, long remainingAmount, Map<GasCanisterContainer, List<PlannedContainerDrain>> drainPlan) {
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

    private static long planGenericDrain(GasCanisterContainer container, Gas gasType, long remainingAmount, Map<GasCanisterContainer, List<PlannedContainerDrain>> drainPlan) {
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

    private static boolean executeDrainPlan(Map<GasCanisterContainer, List<PlannedContainerDrain>> drainPlan, Player player, @Nullable TransactionParticipant<?> operation) {
        ResourceTransaction transaction = new ResourceTransaction();
        drainPlan.forEach((container, plannedDrains) -> plannedDrains.forEach(plannedDrain -> transaction.add(ResourceTransaction.participant(() -> GasStack.matches(container.drain(plannedDrain.tankIndex(), plannedDrain.drainedGas(), GasAction.SIMULATE), plannedDrain.drainedGas()), () -> container.getGasInTank(plannedDrain.tankIndex()).copy(), () -> executePlannedDrain(container, plannedDrain), tankSnapshot -> restoreContainerTank(container, plannedDrain.tankIndex(), tankSnapshot)))));
        if (operation != null) {
            transaction.add(operation);
        }
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

    private static boolean executePlannedDrain(GasCanisterContainer container, PlannedContainerDrain drain) {
        if (!GasStack.matches(container.drain(drain.tankIndex(), drain.drainedGas(), GasAction.EXECUTE), drain.drainedGas())) {
            return false;
        }

        if (isGenericContainer(container)) {
            container.save();
        }
        return true;
    }

    private static void restoreContainerTank(GasCanisterContainer container, int tankIndex, GasStack tankSnapshot) {
        GasStack currentGas = container.getGasInTank(tankIndex).copy();
        if (GasStack.matches(currentGas, tankSnapshot)) {
            return;
        }

        boolean canReturnDifference = container.supportsExactDrainRecovery(tankIndex) && currentGas.getAmount() <= tankSnapshot.getAmount() && (currentGas.isEmpty() || GasStack.isSameGasSameComponents(currentGas, tankSnapshot));
        if (!canReturnDifference && !currentGas.isEmpty()) {
            GasStack removedGas = container.drain(tankIndex, currentGas, GasAction.EXECUTE);
            if (!GasStack.matches(removedGas, currentGas)) {
                throw new IllegalStateException("Failed to clear gas canister container tank " + tankIndex + " during transaction rollback: expected to drain " + currentGas.getAmount() + " GU of '" + currentGas.getGasType() + "', got " + removedGas.getAmount() + " GU of '" + removedGas.getGasType() + "'; gas components must also match.");
            }
        }
        if (!tankSnapshot.isEmpty()) {
            long missingAmount = canReturnDifference ? tankSnapshot.getAmount() - currentGas.getAmount() : tankSnapshot.getAmount();
            long restoredAmount = canReturnDifference ? container.restoreDrainedGas(tankIndex, tankSnapshot.copyWithAmount(missingAmount), GasAction.EXECUTE) : container.fill(tankIndex, tankSnapshot.copy(), GasAction.EXECUTE);
            if (restoredAmount != missingAmount) {
                throw new IllegalStateException("Failed to restore gas canister container tank " + tankIndex + " during transaction rollback: expected to restore " + missingAmount + " GU of '" + tankSnapshot.getGasType() + "'.");
            }
        }

        if (!GasStack.matches(container.getGasInTank(tankIndex), tankSnapshot)) {
            throw new IllegalStateException("Gas canister container tank " + tankIndex + " does not match its snapshot after transaction rollback.");
        }

        if (!isGenericContainer(container)) {
            return;
        }

        container.save();
    }

    private static boolean isGenericContainer(GasCanisterContainer container) {
        return !(container instanceof GasCanisterContainerContents) && !(container instanceof GasCanisterPackContainerContents);
    }

    private static void syncPackMenu(Player player, GasCanisterPackContainerContents packContents, int canisterSlot) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(serverPlayer.containerMenu instanceof GasCanisterPackMenu menu) || !ItemStack.matches(menu.contentHolder, packContents.getContainer())) {
            return;
        }

        ItemStack expectedCanister = menu.getSlot(Inventory.INVENTORY_SIZE + canisterSlot).getItem();
        ItemStack canisterStack = packContents.getCanister(canisterSlot);
        menu.updateCanister(canisterSlot, canisterStack);
        CatnipServices.NETWORK.sendToClient(serverPlayer, new GasCanisterPackMenuSyncPacket(menu.containerId, canisterSlot, expectedCanister, canisterStack));
    }

    private static Map<Integer, ItemStack> snapshotPlannedInventory(Inventory inventory, Iterable<GasCanisterContainer> containers) {
        Map<Integer, ItemStack> inventorySnapshot = new LinkedHashMap<>();
        for (GasCanisterContainer container : containers) {
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

    private static boolean canCoverCost(List<GasCanisterContainer> containers, Gas gasType, long amount) {
        long remainingAmount = amount;
        for (GasCanisterContainer container : containers) {
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

    public static final class ConsumptionBatch {
        private final Gas gasType;
        private final Map<ItemStack, Integer> supplierOrder = new IdentityHashMap<>();
        private final Map<ContainerTank, EconomizedConsumption> consumption = new HashMap<>();

        public ConsumptionBatch(Gas gasType) {
            this.gasType = gasType;
        }

        public boolean execute(Player player, AffordableFuel fuel, TransactionParticipant<?> operation) {
            if (fuel.gasType() != gasType) {
                throw new IllegalArgumentException("A consumption batch must use a single gas type.");
            }

            if (player.level().isClientSide) {
                return false;
            }

            CanisterContainerSuppliers.invalidateCache(player);
            return interactContainer(player, fuel.gasType(), fuel.amount(), () -> true, false, operation, this);
        }

        private long planDrain(GasCanisterContainer container, Gas gasType, long remainingAmount, Map<GasCanisterContainer, List<PlannedContainerDrain>> drainPlan, Map<ContainerTank, EconomizedConsumption> pendingConsumption) {
            List<PlannedContainerDrain> plannedDrains = new ArrayList<>();
            for (int tankIndex = 0; tankIndex < container.getTanks() && remainingAmount > 0; tankIndex++) {
                ItemStack canister = container.getContainer();
                if (container instanceof GasCanisterPackContainerContents packContents) {
                    canister = packContents.getCanister(tankIndex);
                    if (packContents.isCreative(tankIndex) && packContents.getGasInTank(tankIndex).is(gasType)) {
                        remainingAmount = 0;
                        break;
                    }
                }

                ContainerTank tank = new ContainerTank(container.getContainer(), tankIndex);
                EconomizedConsumption previous = consumption.getOrDefault(tank, EconomizedConsumption.EMPTY);
                GasStack storedGas = container.getGasInTank(tankIndex);
                if (!storedGas.isEmpty() && !storedGas.is(gasType) || storedGas.isEmpty() && previous.physicalAmount().signum() == 0) {
                    continue;
                }

                int costPercent = GasCanisterContainerContents.getEconomizeCostPercent(canister);
                long requestedAmount = previous.additionalDrain(remainingAmount, costPercent);
                GasStack simulatedDrain = container.drain(tankIndex, requestedAmount, GasAction.SIMULATE);
                BigInteger physicalAmount = previous.physicalAmount().add(BigInteger.valueOf(simulatedDrain.getAmount()));
                long coveredAmount = previous.additionalCoverage(physicalAmount, remainingAmount, costPercent);
                if (coveredAmount <= 0) {
                    continue;
                }

                remainingAmount -= coveredAmount;
                pendingConsumption.put(tank, new EconomizedConsumption(previous.logicalAmount().add(BigInteger.valueOf(coveredAmount)), physicalAmount));
                if (!simulatedDrain.isEmpty()) {
                    plannedDrains.add(new PlannedContainerDrain(tankIndex, simulatedDrain));
                }
            }

            if (!plannedDrains.isEmpty()) {
                drainPlan.put(container, List.copyOf(plannedDrains));
            }
            return remainingAmount;
        }
    }

    private record ContainerTank(ItemStack container, int tankIndex) {}

    private record EconomizedConsumption(BigInteger logicalAmount, BigInteger physicalAmount) {
        private static final EconomizedConsumption EMPTY = new EconomizedConsumption(BigInteger.ZERO, BigInteger.ZERO);
        private static final BigInteger HUNDRED = BigInteger.valueOf(100);
        private static final BigInteger MAX_DRAIN = BigInteger.valueOf(Long.MAX_VALUE);

        private long additionalDrain(long remainingAmount, int costPercent) {
            BigInteger totalAmount = logicalAmount.add(BigInteger.valueOf(remainingAmount));
            BigInteger required = totalAmount.multiply(BigInteger.valueOf(costPercent)).add(BigInteger.valueOf(99)).divide(HUNDRED).max(BigInteger.ONE);
            return required.subtract(physicalAmount).max(BigInteger.ZERO).min(MAX_DRAIN).longValueExact();
        }

        private long additionalCoverage(BigInteger totalPhysical, long remainingAmount, int costPercent) {
            if (totalPhysical.signum() == 0) {
                return 0;
            }

            if (costPercent == 0) {
                return remainingAmount;
            }

            BigInteger covered = totalPhysical.multiply(HUNDRED).divide(BigInteger.valueOf(costPercent)).subtract(logicalAmount);
            return covered.max(BigInteger.ZERO).min(BigInteger.valueOf(remainingAmount)).longValueExact();
        }
    }

    private record PlannedContainerDrain(int tankIndex, GasStack drainedGas) {
        private PlannedContainerDrain {
            drainedGas = drainedGas.copy();
        }
    }

    public record AffordableFuel(GasStack gasContent, long sourcePressurePa, long amount) {
        public AffordableFuel {
            gasContent = gasContent.copy();
            if (gasContent.isEmpty()) {
                throw new IllegalArgumentException("Affordable fuel must contain a non-empty gas stack.");
            }

            if (sourcePressurePa < 0) {
                throw new IllegalArgumentException("Affordable fuel source pressure must be non-negative; got " + sourcePressurePa + " Pa.");
            }

            if (amount < 0) {
                throw new IllegalArgumentException("Affordable fuel amount must be non-negative; got " + amount + " GU.");
            }
        }

        public Gas gasType() {
            return gasContent.getGasType();
        }

        public GameplayPressureProfile pressureProfile() {
            return GameplayPressureProfiles.resolve(sourcePressurePa);
        }

        public GasUsageContext usageContext() {
            return new GasUsageContext(gasContent.copyWithAmount(1), sourcePressurePa);
        }
    }
}
