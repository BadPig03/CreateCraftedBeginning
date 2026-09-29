package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.content.logistics.packager.PackagingRequest;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPackagerRequestPlanner {

    private GasPackagerRequestPlanner() {
    }

    @Internal
    public static GasRequestPlan planGasRequestBatch(List<PackagingRequest> queuedRequests, long maxAmount) {
        if (queuedRequests.isEmpty() || maxAmount <= 0) {
            return GasRequestPlan.EMPTY;
        }

        PackagingRequest packageMetadata = queuedRequests.getFirst();
        List<PlannedGasRequest> plannedRequests = new ArrayList<>();
        GasStack plannedGasType = GasStack.EMPTY;
        long remainingAmountLimit = maxAmount;
        for (PackagingRequest request : queuedRequests) {
            if (!isSameLink(packageMetadata, request)) {
                break;
            }

            if (!isValidGasRequest(request)) {
                continue;
            }

            ItemStack gasToken = request.item().copyWithCount(1);
            GasStack requestedGas = VirtualGasItems.readGasSample(gasToken);
            if (!plannedGasType.isEmpty() && !GasStack.isSameGasSameComponents(plannedGasType, requestedGas)) {
                break;
            }

            long requestedAmount = Math.max(0, request.getCount());
            long plannedAmount = Math.min(remainingAmountLimit, requestedAmount);
            if (plannedAmount <= 0) {
                break;
            }

            if (plannedGasType.isEmpty()) {
                plannedGasType = requestedGas.copyWithAmount(1);
            }
            plannedRequests.add(new PlannedGasRequest(request, gasToken, requestedGas, plannedAmount));
            remainingAmountLimit -= plannedAmount;
            if (remainingAmountLimit > 0) {
                continue;
            }

            break;
        }

        if (plannedRequests.isEmpty()) {
            return GasRequestPlan.EMPTY;
        }

        return new GasRequestPlan(List.copyOf(plannedRequests));
    }

    static boolean isSameLink(PackagingRequest first, PackagingRequest second) {
        return first.orderId() == second.orderId() && first.linkIndex() == second.linkIndex() && first.address().equals(second.address());
    }

    static boolean isValidGasRequest(PackagingRequest request) {
        return request.getCount() > 0 && VirtualGasItems.isVirtualItem(request.item()) && !VirtualGasItems.readGasSample(request.item()).isEmpty();
    }

    @Internal
    public record PlannedGasRequest(PackagingRequest request, ItemStack token, GasStack gasType, long amount) {}

    @Internal
    public record GasRequestPlan(List<PlannedGasRequest> requests) {
        static final GasRequestPlan EMPTY = new GasRequestPlan(List.of());

        boolean isEmpty() {
            return requests.isEmpty();
        }
    }
}
