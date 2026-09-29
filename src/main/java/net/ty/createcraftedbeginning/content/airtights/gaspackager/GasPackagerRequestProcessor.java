package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonFactory;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerRequestPlanner.GasRequestPlan;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerRequestPlanner.PlannedGasRequest;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPackagerRequestProcessor {
    private final List<PackagingRequest> queuedRequests;
    private final GasHandler handler;

    @Internal
    public GasPackagerRequestProcessor(List<PackagingRequest> queuedRequests, GasHandler handler) {
        this.queuedRequests = queuedRequests;
        this.handler = handler;
    }

    @Internal
    public @Nullable Result process(long maxAmount) {
        while (!queuedRequests.isEmpty() && !GasPackagerRequestPlanner.isValidGasRequest(queuedRequests.getFirst())) {
            queuedRequests.removeFirst();
        }
        GasRequestPlan requestPlan = GasPackagerRequestPlanner.planGasRequestBatch(queuedRequests, maxAmount);
        if (requestPlan.isEmpty()) {
            return null;
        }

        GasRequestExtraction requestExtraction = extractGasRequestBatch(requestPlan);
        if (requestExtraction.isEmpty()) {
            return null;
        }

        GasRequestCommit commitResult = commitGasRequestBatch(requestExtraction);
        PackagingRequest metadata = commitResult.metadata();
        ItemStack packedBalloon = BalloonFactory.createOrdered(commitResult.gas(), metadata.address(), metadata.orderId(), metadata.linkIndex(), metadata.finalLink().booleanValue(), commitResult.packageIndexAtLink(), commitResult.finalPackageAtLink(), commitResult.orderContext());
        if (packedBalloon.isEmpty()) {
            return null;
        }

        return new Result(packedBalloon, commitResult.deductions());
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

    private boolean propagatePackageCounter(PackagingRequest completed, int nextPackageIndex) {
        while (!queuedRequests.isEmpty() && GasPackagerRequestPlanner.isSameLink(completed, queuedRequests.getFirst())) {
            PackagingRequest next = queuedRequests.getFirst();
            if (next.getCount() <= 0 || !VirtualGasItems.isVirtualItem(next.item()) || VirtualGasItems.readGasSample(next.item()).isEmpty()) {
                queuedRequests.removeFirst();
                continue;
            }

            next.packageCounter().setValue(nextPackageIndex);
            return false;
        }

        return true;
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

    private GasRequestCommit commitGasRequestBatch(GasRequestExtraction extraction) {
        ExtractedGasRequest firstTransfer = extraction.transfers().getFirst();
        PackagingRequest packageMetadata = firstTransfer.request();
        int packageIndexAtLink = packageMetadata.packageCounter().getAndIncrement();
        boolean finalPackageAtLink = false;
        PackageOrderWithCrafts orderContext = null;
        List<Deduction> deductions = new ArrayList<>();

        for (ExtractedGasRequest transfer : extraction.transfers()) {
            PackagingRequest request = transfer.request();
            if (queuedRequests.isEmpty() || queuedRequests.getFirst() != request) {
                throw new IllegalStateException("Gas packaging request queue changed during commit.");
            }

            PackageOrderWithCrafts context = request.context();
            if (context != null) {
                orderContext = context;
            }

            request.subtract(transfer.amount());
            addGasDeduction(deductions, transfer.token(), transfer.amount());
            if (!request.isEmpty()) {
                break;
            }

            PackagingRequest completed = queuedRequests.removeFirst();
            finalPackageAtLink = propagatePackageCounter(completed, packageIndexAtLink + 1);
            if (!finalPackageAtLink) {
                continue;
            }

            break;
        }

        return new GasRequestCommit(packageMetadata, extraction.gas(), orderContext, packageIndexAtLink, finalPackageAtLink, List.copyOf(deductions));
    }

    @Internal
    public record Result(ItemStack balloon, List<Deduction> deductions) {}

    @Internal
    public record Deduction(ItemStack token, int amount) {}

    private record ExtractedGasRequest(PackagingRequest request, ItemStack token, int amount) {}

    private record GasRequestExtraction(List<ExtractedGasRequest> transfers, GasStack gas) {
        private static final GasRequestExtraction EMPTY = new GasRequestExtraction(List.of(), GasStack.EMPTY);

        private boolean isEmpty() {
            return transfers.isEmpty() || gas.isEmpty();
        }
    }

    private record GasRequestCommit(PackagingRequest metadata, GasStack gas, @Nullable PackageOrderWithCrafts orderContext, int packageIndexAtLink, boolean finalPackageAtLink, List<Deduction> deductions) {}
}
