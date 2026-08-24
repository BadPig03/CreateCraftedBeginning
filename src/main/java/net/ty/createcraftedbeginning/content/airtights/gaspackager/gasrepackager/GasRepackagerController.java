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
final class GasRepackagerController {
    private final GasRepackagerBlockEntity blockEntity;

    GasRepackagerController(GasRepackagerBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    void attemptToRepackage(IItemHandler targetInv) {
        ScanResult scanResult = GasRepackagerUtils.scanPackages(targetInv);
        if (tryHandleCompletedOrder(targetInv, scanResult) || tryRepackageSimpleGasGroup(targetInv, scanResult)) {
            return;
        }

        passThroughFirstReadyPackage(targetInv, scanResult);
    }

    private boolean tryHandleCompletedOrder(IItemHandler targetInv, ScanResult scan) {
        for (Entry<Integer, List<Candidate>> orderEntry : scan.orderedPackagesByOrder().entrySet()) {
            int orderId = orderEntry.getKey();
            List<Candidate> candidates = orderEntry.getValue();
            if (!GasRepackagerUtils.isOrderComplete(candidates)) {
                continue;
            }

            boolean hasGasPackage = candidates.stream().anyMatch(Candidate::isGasPackage);
            List<BigItemStack> outputPackages;
            if (hasGasPackage) {
                boolean hasNonStandalonePackage = candidates.stream().anyMatch(candidate -> !GasRepackagerUtils.isStandaloneFinalOrderPackage(candidate.box()));
                if (!hasNonStandalonePackage) {
                    continue;
                }

                outputPackages = GasRepackagerUtils.createMixedOrderOutput(orderId, candidates);
            }
            else {
                outputPackages = GasRepackagerUtils.createItemOrderPassThroughOutput(candidates);
            }

            if (outputPackages.isEmpty()) {
                continue;
            }

            if (!extractCandidatesTransactionally(targetInv, candidates)) {
                return false;
            }

            if (hasGasPackage) {
                blockEntity.enqueueRepackagedBoxes(outputPackages);
            }
            else {
                blockEntity.enqueuePassThroughBoxes(outputPackages);
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
            List<BigItemStack> outputPackages = GasRepackagerUtils.createBalloons(group.outputTemplate(), group.contents(), address);
            if (!GasRepackagerUtils.isRepackUseful(group, outputPackages)) {
                continue;
            }

            if (!extractCandidatesTransactionally(targetInv, group.candidates())) {
                return false;
            }

            blockEntity.enqueueRepackagedBoxes(outputPackages);
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
        ExtractionResult extractionResult = GasRepackagerUtils.extractCandidates(targetInv, candidates);
        if (extractionResult.committed()) {
            return true;
        }

        blockEntity.restoreRollbackRemainders(extractionResult.rollbackRemainders());
        return false;
    }
}
