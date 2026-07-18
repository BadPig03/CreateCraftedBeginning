package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.foundation.ICapabilityProvider;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.gases.GasPipeConnection.AirFlow;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.flowsources.GasFlowSource;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasInventoryIdentifierProvider;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasNetwork {
    private static final int TRAVERSAL_WORK_BUDGET_PER_TICK = 256;
    private static final int QUEUED_ENTRY_WORK = 1;
    private static final int FRONTIER_ENTRY_WORK = 1 + Direction.values().length;
    private static final int PAUSE_INTERVAL = 2;

    private final Level level;
    private final BlockFace start;
    private final Deque<BlockFace> queued;
    private final Deque<BlockFace> frontier;
    private final Set<BlockFace> frontierMembership;
    private final Set<BlockPos> visited;
    private final Map<BlockFace, GasFlowSource> targets;
    private final Map<BlockPos, WeakReference<GasTransportBehaviour>> cache;
    private final Supplier<@Nullable ICapabilityProvider<IGasHandler>> sourceSupplier;
    private @Nullable PendingTransfer pendingTransfer;
    private long transferSpeed;
    private int pauseBeforePropagation;
    private int allocationCursor;
    private boolean active;
    private GasStack gas;

    /**
     * Creates a new {@code GasNetwork} instance.
     *
     * @param level          the level in which the operation is performed
     * @param location       the resource location identifying the target value
     * @param sourceSupplier the supplier used to obtain the source
     */
    public GasNetwork(Level level, BlockFace location, Supplier<@Nullable ICapabilityProvider<IGasHandler>> sourceSupplier) {
        this.level = level;
        this.sourceSupplier = sourceSupplier;
        start = location;
        gas = GasStack.EMPTY;
        frontier = new ArrayDeque<>();
        frontierMembership = new HashSet<>();
        visited = new HashSet<>();
        targets = new LinkedHashMap<>();
        cache = new HashMap<>();
        queued = new ArrayDeque<>();
        reset();
    }

    private static GasStack executeTransferPlan(IGasHandler sourceCap, GasStack gasType, List<PlannedTransfer> transferPlan) {
        long plannedAmount = 0;
        for (PlannedTransfer plannedTransfer : transferPlan) {
            plannedAmount = Math.min(gasType.getAmount(), plannedAmount + plannedTransfer.amount);
        }
        if (plannedAmount <= 0) {
            return GasStack.EMPTY;
        }

        GasStack drained = executeSourceDrain(sourceCap, gasType.copyWithAmount(plannedAmount));
        if (drained.isEmpty() || !GasStack.isSameGasSameComponents(drained, gasType)) {
            return drained;
        }

        long remainingBudget = Math.min(plannedAmount, drained.getAmount());
        for (PlannedTransfer plannedTransfer : transferPlan) {
            if (remainingBudget <= 0 || drained.isEmpty()) {
                break;
            }

            long offeredAmount = Math.min(plannedTransfer.amount, Math.min(remainingBudget, drained.getAmount()));
            if (offeredAmount <= 0) {
                continue;
            }

            GasStack offered = drained.copyWithAmount(offeredAmount);
            long filled = plannedTransfer.handler.fill(offered, GasAction.EXECUTE);
            filled = Math.clamp(filled, 0, offeredAmount);
            drained.shrink(filled);
            remainingBudget -= filled;
        }
        return drained;
    }

    private static GasStack executeSourceDrain(IGasHandler sourceCap, GasStack request) {
        if (request.isEmpty()) {
            return GasStack.EMPTY;
        }

        GasStack drained = sourceCap.drain(request, GasAction.EXECUTE);
        if (!drained.isEmpty()) {
            return drained;
        }

        GasStack genericPreview = sourceCap.drain(request.getAmount(), GasAction.SIMULATE);
        if (genericPreview.isEmpty() || !GasStack.isSameGasSameComponents(genericPreview, request)) {
            return GasStack.EMPTY;
        }

        return sourceCap.drain(request.getAmount(), GasAction.EXECUTE);
    }

    private static boolean identifiesSameInventory(@Nullable InventoryIdentifier first, BlockFace firstFace, @Nullable InventoryIdentifier second, BlockFace secondFace) {
        return first != null && first == second || first != null && first.contains(secondFace) || second != null && second.contains(firstFace);
    }

    private static int compareBlockFaces(BlockFace first, BlockFace second) {
        BlockPos firstPos = first.getPos();
        BlockPos secondPos = second.getPos();
        int x = Integer.compare(firstPos.getX(), secondPos.getX());
        if (x != 0) {
            return x;
        }

        int y = Integer.compare(firstPos.getY(), secondPos.getY());
        if (y != 0) {
            return y;
        }

        int z = Integer.compare(firstPos.getZ(), secondPos.getZ());
        return z != 0 ? z : Integer.compare(first.getFace().ordinal(), second.getFace().ordinal());
    }

    private List<PlannedTransfer> createTransferPlan(GasStack available, List<TransferTarget> availableTargets) {
        List<TargetCapacity> capacities = new ArrayList<>();
        for (TransferTarget target : availableTargets) {
            long capacity = Math.clamp(target.handler.fill(available.copy(), GasAction.SIMULATE), 0, available.getAmount());
            if (capacity <= 0) {
                continue;
            }

            capacities.add(new TargetCapacity(target, capacity));
        }
        if (capacities.isEmpty()) {
            return Collections.emptyList();
        }

        Map<BlockFace, Long> allocations = new HashMap<>();
        List<TargetCapacity> uncappedTargets = new ArrayList<>(capacities);
        long remaining = available.getAmount();
        while (!uncappedTargets.isEmpty() && remaining > 0) {
            long equalShare = remaining / uncappedTargets.size();
            boolean removedTarget = false;
            for (Iterator<TargetCapacity> iterator = uncappedTargets.iterator(); iterator.hasNext(); ) {
                TargetCapacity target = iterator.next();
                if (target.capacity > equalShare) {
                    continue;
                }

                allocations.put(target.target.location, target.capacity);
                remaining -= target.capacity;
                iterator.remove();
                removedTarget = true;
            }
            if (!removedTarget) {
                break;
            }
        }

        if (!uncappedTargets.isEmpty() && remaining > 0) {
            long equalShare = remaining / uncappedTargets.size();
            long remainder = remaining % uncappedTargets.size();
            for (TargetCapacity target : uncappedTargets) {
                if (equalShare <= 0) {
                    continue;
                }

                allocations.put(target.target.location, equalShare);
            }

            int remainderStart = Math.floorMod(allocationCursor, uncappedTargets.size());
            for (int i = 0; i < remainder; i++) {
                TargetCapacity target = uncappedTargets.get((remainderStart + i) % uncappedTargets.size());
                allocations.merge(target.target.location, 1L, Long::sum);
            }
            if (remainder > 0) {
                allocationCursor = (remainderStart + (int) remainder) % uncappedTargets.size();
            }
        }

        List<PlannedTransfer> plan = new ArrayList<>(allocations.size());
        for (TargetCapacity target : capacities) {
            long amount = allocations.getOrDefault(target.target.location, 0L);
            if (amount <= 0) {
                continue;
            }

            plan.add(new PlannedTransfer(target.target.handler, amount));
        }
        return plan;
    }

    /**
     * Resets this object to its initial state.
     */
    public void reset() {
        recoverPendingTransferInternal();
        clearTraversalState();
        queued.addLast(start);
        pauseBeforePropagation = PAUSE_INTERVAL;
        active = true;
    }

    /**
     * Stops this network and clears its active state.
     */
    public void stop() {
        clearTraversalState();
        active = false;
    }

    /**
     * Checks whether this value is active.
     *
     * @return {@code true} if this value is active; otherwise {@code false}
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Recovers the pending transfer.
     *
     * @return {@code true} if the condition is satisfied; otherwise {@code false}
     */
    public boolean recoverPendingTransfer() {
        return recoverPendingTransferInternal();
    }

    private void clearTraversalState() {
        frontier.clear();
        frontierMembership.clear();
        visited.clear();
        targets.clear();
        cache.clear();
        queued.clear();
        gas = GasStack.EMPTY;
        transferSpeed = 0;
        pauseBeforePropagation = 0;
    }

    /**
     * Updates this object for one game tick.
     */
    public void tick() {
        if (!recoverPendingTransferInternal() || !active) {
            return;
        }

        if (pauseBeforePropagation > 0) {
            pauseBeforePropagation--;
            return;
        }

        int remainingWork = TRAVERSAL_WORK_BUDGET_PER_TICK;
        Deque<BlockFace> deferredFrontier = new ArrayDeque<>();
        while (remainingWork > 0) {
            boolean performedWork = false;
            if (!queued.isEmpty()) {
                processQueuedEntry(queued.removeFirst());
                remainingWork -= QUEUED_ENTRY_WORK;
                performedWork = true;
            }

            if (!frontier.isEmpty() && remainingWork >= FRONTIER_ENTRY_WORK) {
                BlockFace blockFace = frontier.removeFirst();
                remainingWork -= FRONTIER_ENTRY_WORK;
                performedWork = true;
                if (processFrontierEntry(blockFace)) {
                    deferredFrontier.addLast(blockFace);
                }
                else {
                    frontierMembership.remove(blockFace);
                }
            }

            if (!performedWork) {
                break;
            }
        }

        frontier.addAll(deferredFrontier);
        transferGas();
    }

    private void processQueuedEntry(BlockFace blockFace) {
        if (!isPresent(blockFace)) {
            return;
        }

        GasPipeConnection connection = get(blockFace);
        if (connection == null) {
            return;
        }

        if (blockFace.equals(start)) {
            transferSpeed = (int) Math.max(1, connection.getInboundPressure() / 2.0f);
        }
        if (frontierMembership.add(blockFace)) {
            frontier.addLast(blockFace);
        }
    }

    private boolean processFrontierEntry(BlockFace blockFace) {
        if (!isPresent(blockFace)) {
            return false;
        }

        GasPipeConnection connection = get(blockFace);
        if (connection == null) {
            return false;
        }

        AirFlow flow = connection.getFlow();
        if (flow == null) {
            return true;
        }

        if (!gas.isEmpty() && !GasStack.isSameGasSameComponents(flow.gas, gas)) {
            return false;
        }

        if (!flow.inbound) {
            return connection.comparePressure() < 0;
        }

        if (gas.isEmpty()) {
            gas = flow.gas;
        }
        boolean keepInFrontier = false;
        for (Direction side : Iterate.directions) {
            if (side == blockFace.getFace()) {
                continue;
            }

            BlockFace adjacentLocation = new BlockFace(blockFace.getPos(), side);
            GasPipeConnection adjacent = get(adjacentLocation);
            if (adjacent == null) {
                continue;
            }

            AirFlow outFlow = adjacent.getFlow();
            if (outFlow == null) {
                if (adjacent.hasPressure() && adjacent.getOutwardPressure() > 0) {
                    keepInFrontier = true;
                }
                continue;
            }

            if (outFlow.inbound) {
                if (adjacent.comparePressure() > 0) {
                    keepInFrontier = true;
                }
                continue;
            }

            if (adjacent.getSource() == null && !adjacent.determineSource(level, blockFace.getPos())) {
                keepInFrontier = true;
                continue;
            }

            GasFlowSource adjacentSource = adjacent.getSource();
            if (adjacentSource != null && adjacentSource.isEndpoint()) {
                targets.put(adjacentLocation, adjacentSource);
                continue;
            }

            if (!visited.add(adjacentLocation.getConnectedPos())) {
                continue;
            }

            queued.addLast(adjacentLocation.getOpposite());
        }
        return keepInFrontier;
    }

    private void transferGas() {
        if (!recoverPendingTransferInternal() || gas.isEmpty() || transferSpeed <= 0) {
            return;
        }

        ICapabilityProvider<IGasHandler> sourceProvider = sourceSupplier.get();
        if (sourceProvider == null) {
            return;
        }

        IGasHandler sourceCap = sourceProvider.getCapability();
        if (sourceCap == null) {
            return;
        }

        if (targets.isEmpty()) {
            return;
        }

        GasStack available = simulateSourceDrain(sourceCap, transferSpeed);
        if (available.isEmpty()) {
            return;
        }

        List<TransferTarget> availableTargets = collectAvailableTargets(sourceCap);
        if (availableTargets.isEmpty()) {
            return;
        }

        List<PlannedTransfer> transferPlan = createTransferPlan(available, availableTargets);
        if (transferPlan.isEmpty()) {
            return;
        }

        GasStack remainder = executeTransferPlan(sourceCap, available, transferPlan);
        if (remainder.isEmpty()) {
            return;
        }

        pendingTransfer = new PendingTransfer(sourceProvider, remainder.copy());
        recoverPendingTransferInternal();
    }

    private boolean recoverPendingTransferInternal() {
        PendingTransfer pending = pendingTransfer;
        if (pending == null) {
            return true;
        }

        IGasHandler sourceCap = pending.sourceProvider.getCapability();
        if (sourceCap == null) {
            ICapabilityProvider<IGasHandler> currentSource = sourceSupplier.get();
            if (currentSource == null) {
                return false;
            }

            sourceCap = currentSource.getCapability();
            if (sourceCap == null) {
                return false;
            }

            pending.sourceProvider = currentSource;
        }

        long returned = sourceCap.fill(pending.remainder.copy(), GasAction.EXECUTE);
        returned = Math.clamp(returned, 0, pending.remainder.getAmount());
        pending.remainder.shrink(returned);
        if (!pending.remainder.isEmpty()) {
            return false;
        }

        pendingTransfer = null;
        return true;
    }

    private GasStack simulateSourceDrain(IGasHandler sourceCap, long maxAmount) {
        if (maxAmount <= 0 || gas.isEmpty()) {
            return GasStack.EMPTY;
        }

        for (int i = 0; i < sourceCap.getTanks(); i++) {
            GasStack contained = sourceCap.getGasInTank(i);
            if (contained.isEmpty() || !GasStack.isSameGasSameComponents(contained, gas)) {
                continue;
            }

            GasStack drained = sourceCap.drain(contained.copyWithAmount(maxAmount), GasAction.SIMULATE);
            if (drained.isEmpty()) {
                break;
            }

            return GasStack.isSameGasSameComponents(drained, gas) ? drained : GasStack.EMPTY;
        }

        GasStack drained = sourceCap.drain(maxAmount, GasAction.SIMULATE);
        return !drained.isEmpty() && GasStack.isSameGasSameComponents(drained, gas) ? drained : GasStack.EMPTY;
    }

    private List<TransferTarget> collectAvailableTargets(IGasHandler sourceCap) {
        refreshTargets();
        List<TransferTarget> availableTargets = new ArrayList<>();
        List<IdentifiedInventory> identifiedInventories = new ArrayList<>();
        Set<IGasHandler> handlers = Collections.newSetFromMap(new IdentityHashMap<>());
        BlockFace sourceFace = start.getOpposite();
        InventoryIdentifier sourceIdentifier = getInventoryIdentifier(sourceFace);
        List<Entry<BlockFace, GasFlowSource>> targetEntries = new ArrayList<>(targets.entrySet());
        targetEntries.sort((first, second) -> compareBlockFaces(first.getKey(), second.getKey()));
        for (Entry<BlockFace, GasFlowSource> entry : targetEntries) {
            ICapabilityProvider<IGasHandler> provider = entry.getValue().getGasHandlerProvider();
            if (provider == null) {
                continue;
            }

            IGasHandler targetHandler = provider.getCapability();
            if (targetHandler == null || targetHandler == sourceCap || !handlers.add(targetHandler)) {
                continue;
            }

            BlockFace targetFace = entry.getKey().getOpposite();
            InventoryIdentifier identifier = getInventoryIdentifier(targetFace);
            if (identifier != null) {
                if (identifiesSameInventory(sourceIdentifier, sourceFace, identifier, targetFace)) {
                    continue;
                }

                boolean duplicate = identifiedInventories.stream().anyMatch(existing -> identifiesSameInventory(existing.identifier, existing.face, identifier, targetFace));
                if (duplicate) {
                    continue;
                }

                identifiedInventories.add(new IdentifiedInventory(identifier, targetFace));
            }

            availableTargets.add(new TransferTarget(entry.getKey(), targetHandler));
        }
        return availableTargets;
    }

    @Nullable
    private InventoryIdentifier getInventoryIdentifier(BlockFace inventoryFace) {
        BlockEntity blockEntity = level.getBlockEntity(inventoryFace.getPos());
        if (!(blockEntity instanceof IGasInventoryIdentifierProvider provider)) {
            return null;
        }
        return provider.getGasInventoryIdentifier(inventoryFace.getFace());
    }

    private void refreshTargets() {
        for (Iterator<Entry<BlockFace, GasFlowSource>> iterator = targets.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<BlockFace, GasFlowSource> entry = iterator.next();
            GasFlowSource refreshed = refreshTarget(entry.getKey());
            if (refreshed == null) {
                iterator.remove();
                continue;
            }

            entry.setValue(refreshed);
        }
    }

    @Nullable
    private GasFlowSource refreshTarget(BlockFace location) {
        if (!isPresent(location)) {
            return null;
        }

        GasPipeConnection connection = get(location);
        if (connection == null) {
            return null;
        }

        AirFlow flow = connection.getFlow();
        if (flow == null || flow.inbound || !GasStack.isSameGasSameComponents(flow.gas, gas)) {
            return null;
        }

        if (connection.getSource() == null && !connection.determineSource(level, location.getPos())) {
            return null;
        }
        GasFlowSource source = connection.getSource();
        if (source == null || !source.isEndpoint()) {
            return null;
        }

        return source;
    }

    @Nullable
    private GasPipeConnection get(BlockFace location) {
        BlockPos pos = location.getPos();
        GasTransportBehaviour transfer = getGasTransfer(pos);
        if (transfer == null) {
            return null;
        }
        return transfer.getConnection(location.getFace());
    }

    @Nullable
    private GasTransportBehaviour getGasTransfer(BlockPos pos) {
        WeakReference<GasTransportBehaviour> reference = cache.get(pos);
        GasTransportBehaviour cachedBehaviour = reference == null ? null : reference.get();
        if (cachedBehaviour != null && !cachedBehaviour.blockEntity.isRemoved()) {
            return cachedBehaviour;
        }

        GasTransportBehaviour behaviour = BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
        if (behaviour != null) {
            cache.put(pos, new WeakReference<>(behaviour));
        }
        return behaviour;
    }

    private boolean isPresent(BlockFace location) {
        return level.isLoaded(location.getPos());
    }

    private record TransferTarget(BlockFace location, IGasHandler handler) {}

    private record IdentifiedInventory(InventoryIdentifier identifier, BlockFace face) {}

    private record TargetCapacity(TransferTarget target, long capacity) {}

    private record PlannedTransfer(IGasHandler handler, long amount) {}

    private static final class PendingTransfer {
        private final GasStack remainder;
        private ICapabilityProvider<IGasHandler> sourceProvider;

        private PendingTransfer(ICapabilityProvider<IGasHandler> sourceProvider, GasStack remainder) {
            this.sourceProvider = sourceProvider;
            this.remainder = remainder;
        }
    }
}
