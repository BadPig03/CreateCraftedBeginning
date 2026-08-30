package net.ty.createcraftedbeginning.content.airtights.gas.transport;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.gas.flowsources.GasFlowSource;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasTransferAllocator.Result;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasTransferExecutor.PlannedTransfer;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasTransferPlanner {
    private static final int TARGET_PROBE_WORK_BUDGET_PER_TICK = 64;
    private static final int TARGET_PROBE_WORK_BUDGET_PER_PASS = 32;

    private final GasNetworkTraversal traversal;
    private final BlockFace sourceFace;
    private int allocationCursor;
    private int targetProbeBudgetRemaining;

    GasTransferPlanner(GasNetworkTraversal traversal, BlockFace startFace) {
        this.traversal = traversal;
        sourceFace = startFace.getOpposite();
    }

    private static boolean identifiesSameInventory(@Nullable InventoryIdentifier firstIdentifier, BlockFace firstFace, @Nullable InventoryIdentifier secondIdentifier, BlockFace secondFace) {
        return firstIdentifier != null && firstIdentifier == secondIdentifier || firstIdentifier != null && firstIdentifier.contains(secondFace) || secondIdentifier != null && secondIdentifier.contains(firstFace);
    }

    void beginTick() {
        targetProbeBudgetRemaining = TARGET_PROBE_WORK_BUDGET_PER_TICK;
    }

    List<PlannedTransfer> createTransferPlan(GasStack availableGas, IGasHandler sourceHandler) {
        List<TransferTarget> availableTargets = collectAvailableTargets(sourceHandler);
        if (availableTargets.isEmpty()) {
            return Collections.emptyList();
        }

        List<TargetCapacity> targetCapacities = new ArrayList<>();
        for (TransferTarget target : availableTargets) {
            long fillCapacity = CCBMathUtils.clampNonNegative(target.handler.fill(availableGas.copy(), GasAction.SIMULATE), availableGas.getAmount());
            if (fillCapacity <= 0) {
                continue;
            }

            targetCapacities.add(new TargetCapacity(target.handler, fillCapacity));
        }
        if (targetCapacities.isEmpty()) {
            return Collections.emptyList();
        }

        long[] capacityAmounts = new long[targetCapacities.size()];
        for (int targetIndex = 0; targetIndex < targetCapacities.size(); targetIndex++) {
            capacityAmounts[targetIndex] = targetCapacities.get(targetIndex).capacity;
        }

        Result allocation = GasTransferAllocator.allocate(availableGas.getAmount(), capacityAmounts, allocationCursor);
        allocationCursor = allocation.nextCursor();

        List<PlannedTransfer> transferPlan = new ArrayList<>(targetCapacities.size());
        long[] allocations = allocation.allocations();
        for (int targetIndex = 0; targetIndex < targetCapacities.size(); targetIndex++) {
            long allocatedAmount = allocations[targetIndex];
            if (allocatedAmount <= 0) {
                continue;
            }

            transferPlan.add(new PlannedTransfer(targetCapacities.get(targetIndex).handler, allocatedAmount));
        }
        return transferPlan;
    }

    boolean hasProbeBudget() {
        return targetProbeBudgetRemaining > 0;
    }

    private List<TransferTarget> collectAvailableTargets(IGasHandler sourceHandler) {
        int probeBudget = Math.min(TARGET_PROBE_WORK_BUDGET_PER_PASS, targetProbeBudgetRemaining);
        if (probeBudget <= 0) {
            return Collections.emptyList();
        }

        List<BlockFace> targetLocations = traversal.claimTargetProbeWindow(probeBudget);
        targetProbeBudgetRemaining -= targetLocations.size();
        if (targetLocations.isEmpty()) {
            return Collections.emptyList();
        }

        List<TransferTarget> availableTargets = new ArrayList<>();
        List<IdentifiedInventory> identifiedInventories = new ArrayList<>();
        Set<IGasHandler> uniqueHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
        InventoryIdentifier sourceIdentifier = traversal.getInventoryIdentifier(sourceFace);
        for (BlockFace targetLocation : targetLocations) {
            GasFlowSource targetSource = traversal.refreshTarget(targetLocation);
            if (targetSource == null) {
                traversal.invalidateTarget(targetLocation);
                continue;
            }

            ICapabilityProvider<IGasHandler> targetProvider = targetSource.getGasHandlerProvider();
            if (targetProvider == null) {
                continue;
            }

            IGasHandler targetHandler = targetProvider.getCapability();
            if (targetHandler == null || targetHandler == sourceHandler || !uniqueHandlers.add(targetHandler)) {
                continue;
            }

            BlockFace targetFace = targetLocation.getOpposite();
            InventoryIdentifier targetIdentifier = traversal.getInventoryIdentifier(targetFace);
            if (targetIdentifier != null) {
                if (identifiesSameInventory(sourceIdentifier, sourceFace, targetIdentifier, targetFace)) {
                    continue;
                }

                boolean isDuplicateInventory = identifiedInventories.stream().anyMatch(identifiedInventory -> identifiesSameInventory(identifiedInventory.identifier, identifiedInventory.face, targetIdentifier, targetFace));
                if (isDuplicateInventory) {
                    continue;
                }

                identifiedInventories.add(new IdentifiedInventory(targetIdentifier, targetFace));
            }

            availableTargets.add(new TransferTarget(targetHandler));
        }
        return availableTargets;
    }

    private record TransferTarget(IGasHandler handler) {}

    private record IdentifiedInventory(InventoryIdentifier identifier, BlockFace face) {}

    private record TargetCapacity(IGasHandler handler, long capacity) {}
}
