package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager;

import com.simibubi.create.content.logistics.BigItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPackingLimits;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerExtraction.ExtractionResult;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerOutputs.OutputResult;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.Candidate;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.GasGroupCandidates;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.ScanResult;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

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
        Set<Integer> blockedSlots = new HashSet<>();
        boolean handled = tryHandleCompletedOrder(targetInv, scanResult, ambientPressurePa, blockedSlots) || tryRepackageSimpleGasGroup(targetInv, scanResult, ambientPressurePa, blockedSlots);
        blockEntity.setOutputLimitExceeded(!blockedSlots.isEmpty());
        if (handled) {
            return;
        }

        passThroughFirstReadyPackage(targetInv, scanResult, blockedSlots);
    }

    private boolean tryHandleCompletedOrder(IItemHandler targetInv, ScanResult scan, long ambientPressurePa, Set<Integer> blockedSlots) {
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
                if (candidates.size() > GasRepackagerOutputBudget.MAX_OUTPUT_PACKAGES) {
                    candidates.forEach(candidate -> blockedSlots.add(candidate.slot()));
                    continue;
                }

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

            OutputResult output = GasRepackagerOutputs.createMixedOrderOutput(orderId, candidates, ambientPressurePa, level.getRandom());
            if (output.limitExceeded()) {
                candidates.forEach(candidate -> blockedSlots.add(candidate.slot()));
                continue;
            }

            List<BigItemStack> outputPackages = output.packages();
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

    private boolean tryRepackageSimpleGasGroup(IItemHandler targetInv, ScanResult scan, long ambientPressurePa, Set<Integer> blockedSlots) {
        for (GasGroupCandidates group : scan.simpleGroups()) {
            String address = blockEntity.resolveGasOutputAddress(group.address());
            OutputResult output = GasRepackagerOutputs.createBalloons(group.gas(), address, ambientPressurePa);
            if (output.limitExceeded()) {
                group.candidates().forEach(candidate -> blockedSlots.add(candidate.slot()));
                continue;
            }

            List<BigItemStack> outputPackages = output.packages();
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

    private void passThroughFirstReadyPackage(IItemHandler targetInv, ScanResult scan, Set<Integer> blockedSlots) {
        for (Candidate candidate : scan.passThroughPackages()) {
            if (blockedSlots.contains(candidate.slot())) {
                continue;
            }

            if (!extractCandidatesTransactionally(targetInv, List.of(candidate))) {
                return;
            }

            blockEntity.acceptPassThroughPackage(candidate.box());
            return;
        }
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
