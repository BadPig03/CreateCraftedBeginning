package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager;

import com.simibubi.create.content.logistics.BigItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPackingLimits;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerExtraction.ExtractionResult;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.Candidate;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.GasGroupCandidates;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.ScanResult;

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
        ScanResult scanResult = GasRepackagerScan.scanPackages(targetInv);
        long ambientPressurePa = blockEntity.ambientPressurePa();
        if (tryHandleCompletedOrder(targetInv, scanResult, ambientPressurePa) || tryRepackageSimpleGasGroup(targetInv, scanResult, ambientPressurePa)) {
            return;
        }

        passThroughFirstReadyPackage(targetInv, scanResult);
    }

    private boolean tryHandleCompletedOrder(IItemHandler targetInv, ScanResult scan, long ambientPressurePa) {
        for (Entry<Integer, List<Candidate>> orderEntry : scan.orderedPackagesByOrder().entrySet()) {
            int orderId = orderEntry.getKey();
            List<Candidate> candidates = orderEntry.getValue();
            if (!GasRepackagerPlanner.isOrderComplete(candidates)) {
                continue;
            }

            boolean hasGasPackage = candidates.stream().anyMatch(Candidate::isGasPackage);
            if (!hasGasPackage) {
                blockEntity.attemptVanillaItemRepackage(targetInv);
                return true;
            }

            if (BalloonPackingLimits.getLocalPackingLimit(ambientPressurePa) <= 0) {
                if (!extractCandidatesTransactionally(targetInv, candidates)) {
                    return false;
                }

                GasRepackagerPlanner.sortByOrderPosition(candidates).forEach(candidate -> blockEntity.acceptPassThroughPackage(candidate.box()));
                return true;
            }

            boolean hasNonStandalonePackage = candidates.stream().anyMatch(candidate -> !GasRepackagerScan.isStandaloneFinalOrderPackage(candidate.box()));
            boolean needsLocalGasRepack = GasRepackagerPlanner.needsLocalGasRepack(candidates, ambientPressurePa);
            if (!hasNonStandalonePackage && !needsLocalGasRepack) {
                continue;
            }

            Level level = blockEntity.getLevel();
            if (level == null) {
                continue;
            }

            List<BigItemStack> outputPackages = GasRepackagerOutputs.createMixedOrderOutput(orderId, candidates, ambientPressurePa, level.getRandom());
            if (outputPackages.isEmpty()) {
                continue;
            }

            if (!extractCandidatesTransactionally(targetInv, candidates)) {
                return false;
            }

            blockEntity.enqueueRepackagedBoxes(outputPackages);
            return true;
        }

        return false;
    }

    private boolean tryRepackageSimpleGasGroup(IItemHandler targetInv, ScanResult scan, long ambientPressurePa) {
        for (GasGroupCandidates group : scan.simpleGroups()) {
            String address = blockEntity.resolveGasOutputAddress(group.address());
            List<BigItemStack> outputPackages = GasRepackagerOutputs.createBalloons(group.gas(), address, ambientPressurePa);
            if (!GasRepackagerPlanner.isRepackUseful(group, outputPackages)) {
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
        ExtractionResult extractionResult = GasRepackagerExtraction.extractCandidates(targetInv, candidates);
        if (extractionResult.committed()) {
            return true;
        }

        blockEntity.restoreRollbackRemainders(extractionResult.rollbackRemainders());
        return false;
    }
}
