package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasContents;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasPackagerRequestProcessor {
    private GasPackagerRequestProcessor() {
    }

    static @Nullable Result process(List<PackagingRequest> queuedRequests, IGasHandler handler, long capacity) {
        discardInvalidLeadingGasRequests(queuedRequests);
        GasRequestPlan requestPlan = planGasRequestBatch(queuedRequests, capacity);
        if (requestPlan.isEmpty()) {
            return null;
        }

        GasRequestExtraction requestExtraction = extractGasRequestBatch(handler, requestPlan);
        if (requestExtraction.isEmpty()) {
            return null;
        }

        GasRequestCommit commitResult = commitGasRequestBatch(queuedRequests, requestExtraction);
        ItemStack packedBalloon = createRequestedBalloon(commitResult);
        if (packedBalloon.isEmpty()) {
            return null;
        }
        return new Result(packedBalloon, commitResult.deductions());
    }

    private static boolean containsMatchingGas(List<GasStack> gases, GasStack target) {
        return gases.stream().anyMatch(gas -> GasStack.isSameGasSameComponents(gas, target));
    }

    private static void addPackedGas(List<GasStack> packedGases, GasStack gasToAdd) {
        for (int gasIndex = 0; gasIndex < packedGases.size(); gasIndex++) {
            GasStack existingGas = packedGases.get(gasIndex);
            if (!GasStack.isSameGasSameComponents(existingGas, gasToAdd)) {
                continue;
            }

            packedGases.set(gasIndex, existingGas.copyWithAmount(existingGas.getAmount() + gasToAdd.getAmount()));
            return;
        }

        packedGases.add(gasToAdd.copy());
    }

    private static void discardInvalidLeadingGasRequests(List<PackagingRequest> queuedRequests) {
        while (!queuedRequests.isEmpty() && !isValidGasRequest(queuedRequests.getFirst())) {
            queuedRequests.removeFirst();
        }
    }

    private static boolean isValidGasRequest(PackagingRequest request) {
        return request.getCount() > 0 && GasVirtualUtils.isVirtualItem(request.item()) && !GasVirtualUtils.getGasType(request.item()).isEmpty();
    }

    private static GasRequestPlan planGasRequestBatch(List<PackagingRequest> queuedRequests, long capacity) {
        if (queuedRequests.isEmpty() || capacity <= 0) {
            return GasRequestPlan.EMPTY;
        }

        PackagingRequest packageMetadata = queuedRequests.getFirst();
        List<PlannedGasRequest> plannedRequests = new ArrayList<>();
        List<GasStack> plannedGases = new ArrayList<>();
        long remainingCapacity = capacity;
        for (PackagingRequest request : queuedRequests) {
            if (!GasPackagerUtils.isSameLink(packageMetadata, request)) {
                break;
            }
            if (!isValidGasRequest(request)) {
                continue;
            }

            ItemStack gasToken = request.item().copyWithCount(1);
            GasStack requestedGas = GasVirtualUtils.getGasType(gasToken);
            if (!containsMatchingGas(plannedGases, requestedGas) && plannedGases.size() >= BalloonGasContents.MAX_GAS_TYPES) {
                break;
            }

            long requestedAmount = Math.max(0, request.getCount());
            long plannedAmount = Math.min(remainingCapacity, requestedAmount);
            if (plannedAmount <= 0) {
                break;
            }

            plannedRequests.add(new PlannedGasRequest(request, gasToken, requestedGas, plannedAmount));
            addPackedGas(plannedGases, requestedGas.copyWithAmount(plannedAmount));
            remainingCapacity -= plannedAmount;
            if (remainingCapacity > 0) {
                continue;
            }

            break;
        }

        return plannedRequests.isEmpty() ? GasRequestPlan.EMPTY : new GasRequestPlan(List.copyOf(plannedRequests));
    }

    private static GasRequestExtraction extractGasRequestBatch(IGasHandler handler, GasRequestPlan plan) {
        List<ExtractedGasRequest> extractedRequests = new ArrayList<>(plan.requests().size());
        List<GasStack> packedGases = new ArrayList<>();
        for (PlannedGasRequest plannedRequest : plan.requests()) {
            GasStack simulatedDrain = handler.drain(plannedRequest.gasType().copyWithAmount(plannedRequest.amount()), GasAction.SIMULATE);
            if (simulatedDrain.isEmpty() || !GasStack.isSameGasSameComponents(simulatedDrain, plannedRequest.gasType())) {
                break;
            }

            long executableAmount = Math.min(plannedRequest.amount(), simulatedDrain.getAmount());
            GasStack drainedGas = handler.drain(plannedRequest.gasType().copyWithAmount(executableAmount), GasAction.EXECUTE);
            if (drainedGas.isEmpty() || !GasStack.isSameGasSameComponents(drainedGas, plannedRequest.gasType())) {
                break;
            }

            int transferredAmount = Math.min(GasRequestUtils.toLogisticsAmount(drainedGas.getAmount()), GasRequestUtils.toLogisticsAmount(plannedRequest.amount()));
            if (transferredAmount <= 0) {
                break;
            }

            extractedRequests.add(new ExtractedGasRequest(plannedRequest.request(), plannedRequest.token(), transferredAmount));
            addPackedGas(packedGases, drainedGas.copyWithAmount(transferredAmount));
            if (transferredAmount < plannedRequest.amount()) {
                break;
            }
        }

        if (extractedRequests.isEmpty()) {
            return GasRequestExtraction.EMPTY;
        }
        return new GasRequestExtraction(List.copyOf(extractedRequests), new BalloonGasContents(packedGases));
    }

    private static GasRequestCommit commitGasRequestBatch(List<PackagingRequest> queuedRequests, GasRequestExtraction extraction) {
        ExtractedGasRequest firstTransfer = extraction.transfers().getFirst();
        PackagingRequest packageMetadata = firstTransfer.request();
        int packageIndexAtLink = packageMetadata.packageCounter().getAndIncrement();
        boolean finalPackageAtLink = false;
        PackageOrderWithCrafts orderContext = null;
        List<Deduction> deductions = new ArrayList<>();

        for (ExtractedGasRequest transfer : extraction.transfers()) {
            PackagingRequest request = transfer.request();
            if (queuedRequests.isEmpty() || queuedRequests.getFirst() != request) {
                throw new IllegalStateException("Gas packaging request queue changed during commit");
            }
            if (request.context() != null) {
                orderContext = request.context();
            }

            request.subtract(transfer.amount());
            addGasDeduction(deductions, transfer.token(), transfer.amount());
            if (!request.isEmpty()) {
                break;
            }

            PackagingRequest completed = queuedRequests.removeFirst();
            finalPackageAtLink = GasPackagerUtils.propagatePackageCounter(completed, queuedRequests, packageIndexAtLink + 1);
            if (finalPackageAtLink) {
                break;
            }
        }

        return new GasRequestCommit(packageMetadata, extraction.contents(), orderContext, packageIndexAtLink, finalPackageAtLink, List.copyOf(deductions));
    }

    private static void addGasDeduction(List<Deduction> deductions, ItemStack token, int amount) {
        for (int deductionIndex = 0; deductionIndex < deductions.size(); deductionIndex++) {
            Deduction existing = deductions.get(deductionIndex);
            if (!ItemStack.isSameItemSameComponents(existing.token(), token)) {
                continue;
            }

            int mergedAmount = GasRequestUtils.toLogisticsAmount((long) existing.amount() + amount);
            deductions.set(deductionIndex, new Deduction(existing.token(), mergedAmount));
            return;
        }

        deductions.add(new Deduction(token.copyWithCount(1), amount));
    }

    private static ItemStack createRequestedBalloon(GasRequestCommit committed) {
        PackagingRequest packageMetadata = committed.metadata();
        ItemStack packedBalloon = BalloonUtils.containing(committed.contents());
        if (packedBalloon.isEmpty()) {
            return ItemStack.EMPTY;
        }

        PackageItem.clearAddress(packedBalloon);
        PackageItem.addAddress(packedBalloon, packageMetadata.address());
        PackageItem.setOrder(packedBalloon, packageMetadata.orderId(), packageMetadata.linkIndex(), packageMetadata.finalLink().booleanValue(), committed.packageIndexAtLink(), committed.finalPackageAtLink(), committed.orderContext());
        return packedBalloon;
    }

    record Result(ItemStack balloon, List<Deduction> deductions) {}

    record Deduction(ItemStack token, int amount) {}

    private record PlannedGasRequest(PackagingRequest request, ItemStack token, GasStack gasType, long amount) {}

    private record GasRequestPlan(List<PlannedGasRequest> requests) {
        private static final GasRequestPlan EMPTY = new GasRequestPlan(List.of());

        private boolean isEmpty() {
            return requests.isEmpty();
        }
    }

    private record ExtractedGasRequest(PackagingRequest request, ItemStack token, int amount) {}

    private record GasRequestExtraction(List<ExtractedGasRequest> transfers, BalloonGasContents contents) {
        private static final GasRequestExtraction EMPTY = new GasRequestExtraction(List.of(), BalloonGasContents.EMPTY);

        private boolean isEmpty() {
            return transfers.isEmpty() || contents.isEmpty();
        }
    }

    private record GasRequestCommit(PackagingRequest metadata, BalloonGasContents contents, @Nullable PackageOrderWithCrafts orderContext, int packageIndexAtLink, boolean finalPackageAtLink, List<Deduction> deductions) {}
}
