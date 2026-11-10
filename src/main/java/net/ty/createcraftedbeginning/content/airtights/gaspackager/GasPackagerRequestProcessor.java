package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonFactory;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerRequestPlanner.GasRequestPlan;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerRequestPlanner.PlannedGasRequest;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasPackagerRequestProcessor {
    private final List<PackagingRequest> queuedRequests;
    private final GasHandler handler;

    GasPackagerRequestProcessor(List<PackagingRequest> queuedRequests, GasHandler handler) {
        this.queuedRequests = queuedRequests;
        this.handler = handler;
    }

    private static void addGasDeduction(List<Deduction> deductions, ItemStack token, int amount) {
        for (int deductionIndex = 0; deductionIndex < deductions.size(); deductionIndex++) {
            Deduction existing = deductions.get(deductionIndex);
            if (!ItemStack.isSameItemSameComponents(existing.token(), token)) {
                continue;
            }

            int mergedAmount = (int) Mth.clamp((long) existing.amount() + amount, 0L, BigItemStack.INF);
            deductions.set(deductionIndex, new Deduction(existing.token(), mergedAmount));
            return;
        }

        deductions.add(new Deduction(token.copyWithCount(1), amount));
    }

    @Nullable Result process(long maxAmount) {
        int firstValidIndex = 0;
        while (firstValidIndex < queuedRequests.size() && !GasPackagerRequestPlanner.isValidGasRequest(queuedRequests.get(firstValidIndex))) {
            firstValidIndex++;
        }
        if (firstValidIndex == queuedRequests.size()) {
            queuedRequests.clear();
            return null;
        }

        GasRequestPlan requestPlan = GasPackagerRequestPlanner.planGasRequestBatch(queuedRequests.subList(firstValidIndex, queuedRequests.size()), maxAmount);
        if (requestPlan.isEmpty()) {
            return null;
        }

        PlannedGasRequest firstPlanned = requestPlan.requests().getFirst();
        long preparedAmount = requestPlan.requests().stream().mapToLong(PlannedGasRequest::amount).sum();
        GasStack preparedGas = firstPlanned.gasType().copyWithAmount(preparedAmount);
        ItemStack packedBalloon = BalloonFactory.create(preparedGas, firstPlanned.request().address());
        if (!GasStack.matches(BalloonItem.getGas(packedBalloon), preparedGas)) {
            return null;
        }

        GasRequestExtraction requestExtraction = extractGasRequestBatch(requestPlan);
        if (requestExtraction.isEmpty()) {
            return null;
        }

        GasRequestCommit commit = planGasRequestCommit(requestExtraction, firstValidIndex);
        PackagingRequest metadata = requestExtraction.transfers().getFirst().request();
        BalloonItem.setGas(packedBalloon, requestExtraction.gas());
        PackageItem.setOrder(packedBalloon, metadata.orderId(), metadata.linkIndex(), metadata.finalLink().booleanValue(), commit.packageIndexAtLink(), commit.finalPackageAtLink(), commit.orderContext());
        for (ExtractedGasRequest transfer : requestExtraction.transfers()) {
            transfer.request().subtract(transfer.amount());
            transfer.request().packageCounter().setValue(commit.packageIndexAtLink() + 1);
        }
        queuedRequests.subList(0, commit.removedRequests()).clear();
        if (!queuedRequests.isEmpty() && GasPackagerRequestPlanner.isSameLink(metadata, queuedRequests.getFirst())) {
            queuedRequests.getFirst().packageCounter().setValue(commit.packageIndexAtLink() + 1);
        }
        return new Result(packedBalloon, commit.deductions());
    }

    private GasRequestExtraction extractGasRequestBatch(GasRequestPlan plan) {
        List<ExtractedGasRequest> extractedRequests = new ArrayList<>(plan.requests().size());
        GasStack packedGas = GasStack.EMPTY;
        long packedAmount = 0;
        for (PlannedGasRequest plannedRequest : plan.requests()) {
            GasStack drainedGas = GasPackagerGasTransfer.drainGasForPackaging(handler, plannedRequest.gasType().copyWithAmount(plannedRequest.amount()), plannedRequest.amount());
            if (drainedGas.isEmpty() || !GasStack.isSameGasSameComponents(drainedGas, plannedRequest.gasType())) {
                break;
            }

            int transferredAmount = Math.min((int) Mth.clamp(drainedGas.getAmount(), 0L, BigItemStack.INF), (int) Mth.clamp(plannedRequest.amount(), 0L, BigItemStack.INF));
            if (transferredAmount <= 0) {
                break;
            }

            if (!packedGas.isEmpty() && !GasStack.isSameGasSameComponents(packedGas, drainedGas)) {
                break;
            }

            if (packedGas.isEmpty()) {
                packedGas = drainedGas.copyWithAmount(1);
            }
            packedAmount = BoundedMath.saturatedAdd(packedAmount, transferredAmount);
            extractedRequests.add(new ExtractedGasRequest(plannedRequest.request(), plannedRequest.token(), transferredAmount));
            if (transferredAmount >= plannedRequest.amount()) {
                continue;
            }

            break;
        }

        if (extractedRequests.isEmpty() || packedGas.isEmpty() || packedAmount <= 0) {
            return GasRequestExtraction.EMPTY;
        }

        return new GasRequestExtraction(List.copyOf(extractedRequests), packedGas.copyWithAmount(packedAmount));
    }

    private GasRequestCommit planGasRequestCommit(GasRequestExtraction extraction, int firstValidIndex) {
        PackagingRequest metadata = extraction.transfers().getFirst().request();
        int packageIndexAtLink = metadata.packageCounter().intValue();
        int queueIndex = firstValidIndex;
        boolean finalPackageAtLink = false;
        PackageOrderWithCrafts orderContext = null;
        List<Deduction> deductions = new ArrayList<>();
        for (ExtractedGasRequest transfer : extraction.transfers()) {
            PackagingRequest request = transfer.request();
            if (queueIndex >= queuedRequests.size() || queuedRequests.get(queueIndex) != request) {
                throw new IllegalStateException("Gas packaging request queue changed during commit planning.");
            }

            PackageOrderWithCrafts context = request.context();
            if (context != null) {
                orderContext = context;
            }
            addGasDeduction(deductions, transfer.token(), transfer.amount());
            if (request.getCount() > transfer.amount()) {
                break;
            }

            queueIndex++;
            while (queueIndex < queuedRequests.size() && GasPackagerRequestPlanner.isSameLink(metadata, queuedRequests.get(queueIndex)) && !GasPackagerRequestPlanner.isValidGasRequest(queuedRequests.get(queueIndex))) {
                queueIndex++;
            }
            finalPackageAtLink = queueIndex == queuedRequests.size() || !GasPackagerRequestPlanner.isSameLink(metadata, queuedRequests.get(queueIndex));
        }
        return new GasRequestCommit(orderContext, packageIndexAtLink, finalPackageAtLink, queueIndex, List.copyOf(deductions));
    }

    record Result(ItemStack balloon, List<Deduction> deductions) {}

    record Deduction(ItemStack token, int amount) {}

    private record ExtractedGasRequest(PackagingRequest request, ItemStack token, int amount) {}

    private record GasRequestExtraction(List<ExtractedGasRequest> transfers, GasStack gas) {
        private static final GasRequestExtraction EMPTY = new GasRequestExtraction(List.of(), GasStack.EMPTY);

        private boolean isEmpty() {
            return transfers.isEmpty() || gas.isEmpty();
        }
    }

    private record GasRequestCommit(@Nullable PackageOrderWithCrafts orderContext, int packageIndexAtLink, boolean finalPackageAtLink, int removedRequests, List<Deduction> deductions) {}
}
