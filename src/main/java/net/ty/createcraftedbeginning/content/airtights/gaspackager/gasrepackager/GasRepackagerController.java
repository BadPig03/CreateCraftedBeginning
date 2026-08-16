package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager;

import com.simibubi.create.content.logistics.BigItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerUtils.Candidate;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerUtils.ExtractionResult;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerUtils.GasGroupCandidates;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerUtils.ScanResult;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasRepackagerController {
    private final GasRepackagerBlockEntity blockEntity;

    public GasRepackagerController(GasRepackagerBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    public void attemptToRepackage(IItemHandler targetInv) {
        ScanResult scan = GasRepackagerUtils.scanPackages(targetInv);
        if (tryHandleCompletedOrder(targetInv, scan) || tryRepackageSimpleGasGroup(targetInv, scan)) {
            return;
        }

        passThroughFirstReadyPackage(targetInv, scan);
    }

    private boolean tryHandleCompletedOrder(IItemHandler targetInv, ScanResult scan) {
        for (Entry<Integer, List<Candidate>> entry : scan.orderedPackagesByOrder().entrySet()) {
            int orderId = entry.getKey();
            List<Candidate> candidates = entry.getValue();
            if (!GasRepackagerUtils.isOrderComplete(candidates)) {
                continue;
            }

            boolean hasGasPackage = candidates.stream().anyMatch(Candidate::isGasPackage);
            List<BigItemStack> output;
            if (hasGasPackage) {
                boolean hasNonStandalonePackage = candidates.stream().anyMatch(candidate -> !GasRepackagerUtils.isStandaloneFinalOrderPackage(candidate.box()));
                if (!hasNonStandalonePackage) {
                    continue;
                }

                output = GasRepackagerUtils.createMixedOrderOutput(orderId, candidates);
            }
            else {
                output = GasRepackagerUtils.createItemOrderPassThroughOutput(candidates);
            }

            if (output.isEmpty()) {
                continue;
            }

            if (!extractCandidatesTransactionally(targetInv, candidates)) {
                return false;
            }

            if (hasGasPackage) {
                blockEntity.enqueueRepackagedBoxes(output);
            }
            else {
                blockEntity.enqueuePassThroughBoxes(output);
            }
            return true;
        }
        return false;
    }

    private boolean tryRepackageSimpleGasGroup(IItemHandler targetInv, ScanResult scan) {
        for (GasGroupCandidates group : scan.simpleGroups()) {
            if (group.candidates().size() < 2) {
                continue;
            }

            String address = blockEntity.resolveGasOutputAddress(group.address());
            List<BigItemStack> output = GasRepackagerUtils.createBalloons(group.outputTemplate(), group.contents(), address);
            if (!GasRepackagerUtils.isRepackUseful(group, output)) {
                continue;
            }

            if (!extractCandidatesTransactionally(targetInv, group.candidates())) {
                return false;
            }

            blockEntity.enqueueRepackagedBoxes(output);
            return true;
        }
        return false;
    }

    private void passThroughFirstReadyPackage(IItemHandler targetInv, ScanResult scan) {
        Candidate candidate = scan.firstPassThroughPackage();
        if (candidate == null || !extractCandidatesTransactionally(targetInv, List.of(candidate))) {
            return;
        }

        blockEntity.acceptPassThroughPackage(candidate.box());
    }

    private boolean extractCandidatesTransactionally(IItemHandler targetInv, List<Candidate> candidates) {
        ExtractionResult result = GasRepackagerUtils.extractCandidates(targetInv, candidates);
        if (result.committed()) {
            return true;
        }

        blockEntity.restoreRollbackRemainders(result.rollbackRemainders());
        return false;
    }
}
