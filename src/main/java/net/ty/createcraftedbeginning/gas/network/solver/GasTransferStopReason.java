package net.ty.createcraftedbeginning.gas.network.solver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum GasTransferStopReason {
    NO_SOURCE,
    NO_SINK,
    NO_ROUTED_PATH,
    GAS_COLLISION,
    HANDLER_REJECTED,
    NO_TRANSFER_COMPONENT,
    NO_INTEGER_PLAN,
    DID_NOT_CONVERGE,
    BUDGET_EXHAUSTED,
    EMPTY_APPROXIMATION;

    static GasTransferStopReason fromEmptySolution(List<GasNetworkPressureEndpoint> endpoints) {
        boolean source = false;
        boolean sink = false;
        for (GasNetworkPressureEndpoint endpoint : endpoints) {
            source |= endpoint.transferLimits().drainLimit() > 0 && !endpoint.access().drainFaces().isEmpty();
            sink |= endpoint.transferLimits().fillLimit() > 0 && !endpoint.access().fillFaces().isEmpty();
            if (!source || !sink) {
                continue;
            }

            return NO_TRANSFER_COMPONENT;
        }
        if (!source) {
            return NO_SOURCE;
        }

        return NO_SINK;
    }
}
