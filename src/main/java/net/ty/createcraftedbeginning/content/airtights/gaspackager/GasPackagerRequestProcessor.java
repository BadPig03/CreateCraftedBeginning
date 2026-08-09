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
        GasRequestPlan plan = planGasRequestBatch(queuedRequests, capacity);
        if (plan.isEmpty()) {
            return null;
        }

        GasRequestExtraction extraction = extractGasRequestBatch(handler, plan);
        if (extraction.isEmpty()) {
            return null;
        }

        GasRequestCommit committed = commitGasRequestBatch(queuedRequests, extraction);
        ItemStack balloon = createRequestedBalloon(committed);
        if (balloon.isEmpty()) {
            return null;
        }
        return new Result(balloon, committed.deductions());
    }

    private static boolean containsMatchingGas(List<GasStack> gases, GasStack target) {
        return gases.stream().anyMatch(gas -> GasStack.isSameGasSameComponents(gas, target));
    }

    private static void addPackedGas(List<GasStack> gases, GasStack added) {
        for (int i = 0; i < gases.size(); i++) {
            GasStack existing = gases.get(i);
            if (!GasStack.isSameGasSameComponents(existing, added)) {
                continue;
            }

            gases.set(i, existing.copyWithAmount(existing.getAmount() + added.getAmount()));
            return;
        }

        gases.add(added.copy());
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

        PackagingRequest metadata = queuedRequests.getFirst();
        List<PlannedGasRequest> planned = new ArrayList<>();
        List<GasStack> plannedGases = new ArrayList<>();
        long remaining = capacity;
        for (PackagingRequest request : queuedRequests) {
            if (!GasPackagerUtils.isSameLink(metadata, request)) {
                break;
            }
            if (!isValidGasRequest(request)) {
                continue;
            }

            ItemStack token = request.item().copyWithCount(1);
            GasStack gasType = GasVirtualUtils.getGasType(token);
            if (!containsMatchingGas(plannedGases, gasType) && plannedGases.size() >= BalloonGasContents.MAX_GAS_TYPES) {
                break;
            }

            long requested = Math.max(0, request.getCount());
            long amount = Math.min(remaining, requested);
            if (amount <= 0) {
                break;
            }

            planned.add(new PlannedGasRequest(request, token, gasType, amount));
            addPackedGas(plannedGases, gasType.copyWithAmount(amount));
            remaining -= amount;
            if (remaining > 0) {
                continue;
            }

            break;
        }

        return planned.isEmpty() ? GasRequestPlan.EMPTY : new GasRequestPlan(List.copyOf(planned));
    }

    private static GasRequestExtraction extractGasRequestBatch(IGasHandler handler, GasRequestPlan plan) {
        List<ExtractedGasRequest> transfers = new ArrayList<>(plan.requests().size());
        List<GasStack> packedGases = new ArrayList<>();
        for (PlannedGasRequest planned : plan.requests()) {
            GasStack simulated = handler.drain(planned.gasType().copyWithAmount(planned.amount()), GasAction.SIMULATE);
            if (simulated.isEmpty() || !GasStack.isSameGasSameComponents(simulated, planned.gasType())) {
                break;
            }

            long executableAmount = Math.min(planned.amount(), simulated.getAmount());
            GasStack drained = handler.drain(planned.gasType().copyWithAmount(executableAmount), GasAction.EXECUTE);
            if (drained.isEmpty() || !GasStack.isSameGasSameComponents(drained, planned.gasType())) {
                break;
            }

            int transferred = Math.min(GasRequestUtils.toLogisticsAmount(drained.getAmount()), GasRequestUtils.toLogisticsAmount(planned.amount()));
            if (transferred <= 0) {
                break;
            }

            transfers.add(new ExtractedGasRequest(planned.request(), planned.token(), transferred));
            addPackedGas(packedGases, drained.copyWithAmount(transferred));
            if (transferred < planned.amount()) {
                break;
            }
        }

        if (transfers.isEmpty()) {
            return GasRequestExtraction.EMPTY;
        }
        return new GasRequestExtraction(List.copyOf(transfers), new BalloonGasContents(packedGases));
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
        for (int i = 0; i < deductions.size(); i++) {
            Deduction existing = deductions.get(i);
            if (!ItemStack.isSameItemSameComponents(existing.token(), token)) {
                continue;
            }

            int mergedAmount = GasRequestUtils.toLogisticsAmount((long) existing.amount() + amount);
            deductions.set(i, new Deduction(existing.token(), mergedAmount));
            return;
        }

        deductions.add(new Deduction(token.copyWithCount(1), amount));
    }

    private static ItemStack createRequestedBalloon(GasRequestCommit committed) {
        PackagingRequest metadata = committed.metadata();
        ItemStack balloon = BalloonUtils.containing(committed.contents());
        if (balloon.isEmpty()) {
            return ItemStack.EMPTY;
        }

        PackageItem.clearAddress(balloon);
        PackageItem.addAddress(balloon, metadata.address());
        PackageItem.setOrder(balloon, metadata.orderId(), metadata.linkIndex(), metadata.finalLink().booleanValue(), committed.packageIndexAtLink(), committed.finalPackageAtLink(), committed.orderContext());
        return balloon;
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
