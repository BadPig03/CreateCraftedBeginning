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
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasTransferPlanner {
    private static final int TARGET_PROBE_WORK_BUDGET_PER_TICK = 64;
    private static final int TARGET_PROBE_WORK_BUDGET_PER_PASS = 32;

    private final GasNetworkTraversal traversal;
    private final BlockFace sourceFace;
    private int allocationCursor;
    private int targetProbeBudgetRemaining;

    public GasTransferPlanner(GasNetworkTraversal traversal, BlockFace start) {
        this.traversal = traversal;
        sourceFace = start.getOpposite();
    }

    private static boolean identifiesSameInventory(@Nullable InventoryIdentifier first, BlockFace firstFace, @Nullable InventoryIdentifier second, BlockFace secondFace) {
        return first != null && first == second || first != null && first.contains(secondFace) || second != null && second.contains(firstFace);
    }

    public void beginTick() {
        targetProbeBudgetRemaining = TARGET_PROBE_WORK_BUDGET_PER_TICK;
    }

    public List<PlannedTransfer> createTransferPlan(GasStack available, IGasHandler sourceCap) {
        List<TransferTarget> availableTargets = collectAvailableTargets(sourceCap);
        if (availableTargets.isEmpty()) {
            return Collections.emptyList();
        }

        List<TargetCapacity> capacities = new ArrayList<>();
        for (TransferTarget target : availableTargets) {
            long capacity = Math.clamp(target.handler.fill(available.copy(), GasAction.SIMULATE), 0, available.getAmount());
            if (capacity <= 0) {
                continue;
            }

            capacities.add(new TargetCapacity(target.handler, capacity));
        }
        if (capacities.isEmpty()) {
            return Collections.emptyList();
        }

        long[] capacityAmounts = new long[capacities.size()];
        for (int i = 0; i < capacities.size(); i++) {
            capacityAmounts[i] = capacities.get(i).capacity;
        }

        Result allocation = GasTransferAllocator.allocate(available.getAmount(), capacityAmounts, allocationCursor);
        allocationCursor = allocation.nextCursor();

        List<PlannedTransfer> plan = new ArrayList<>(capacities.size());
        long[] allocations = allocation.allocations();
        for (int i = 0; i < capacities.size(); i++) {
            long amount = allocations[i];
            if (amount <= 0) {
                continue;
            }

            plan.add(new PlannedTransfer(capacities.get(i).handler, amount));
        }
        return plan;
    }

    public boolean hasProbeBudget() {
        return targetProbeBudgetRemaining > 0;
    }

    private List<TransferTarget> collectAvailableTargets(IGasHandler sourceCap) {
        int probeBudget = Math.min(TARGET_PROBE_WORK_BUDGET_PER_PASS, targetProbeBudgetRemaining);
        if (probeBudget <= 0) {
            return Collections.emptyList();
        }

        List<BlockFace> locations = traversal.claimTargetProbeWindow(probeBudget);
        targetProbeBudgetRemaining -= locations.size();
        if (locations.isEmpty()) {
            return Collections.emptyList();
        }

        List<TransferTarget> availableTargets = new ArrayList<>();
        List<IdentifiedInventory> identifiedInventories = new ArrayList<>();
        Set<IGasHandler> handlers = Collections.newSetFromMap(new IdentityHashMap<>());
        InventoryIdentifier sourceIdentifier = traversal.getInventoryIdentifier(sourceFace);
        for (BlockFace location : locations) {
            GasFlowSource target = traversal.refreshTarget(location);
            if (target == null) {
                traversal.invalidateTarget(location);
                continue;
            }

            ICapabilityProvider<IGasHandler> provider = target.getGasHandlerProvider();
            if (provider == null) {
                continue;
            }

            IGasHandler targetHandler = provider.getCapability();
            if (targetHandler == null || targetHandler == sourceCap || !handlers.add(targetHandler)) {
                continue;
            }

            BlockFace targetFace = location.getOpposite();
            InventoryIdentifier identifier = traversal.getInventoryIdentifier(targetFace);
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

            availableTargets.add(new TransferTarget(targetHandler));
        }
        return availableTargets;
    }

    private record TransferTarget(IGasHandler handler) {}

    private record IdentifiedInventory(InventoryIdentifier identifier, BlockFace face) {}

    private record TargetCapacity(IGasHandler handler, long capacity) {}
}
