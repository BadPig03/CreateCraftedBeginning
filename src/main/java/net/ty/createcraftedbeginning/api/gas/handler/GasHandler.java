package net.ty.createcraftedbeginning.api.gas.handler;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasHandler {
    /**
     * Captures a checkpoint before an EXECUTE call. The default keeps existing
     * handlers usable without promising rollback after an exception. Override when
     * all state changed by the operation can be restored; return null otherwise.
     * Capturing must not change state. Subclasses adding transactional side effects
     * must extend the checkpoint or opt out, rather than inherit incomplete recovery.
     */
    default @Nullable GasTransferSnapshot captureTransferSnapshot() {
        return null;
    }

    boolean isGasValid(int tank, GasStack stack);

    GasStack drain(GasStack resource, GasAction action);

    GasStack drain(long maxDrain, GasAction action);

    GasStack getGasInTank(int tank);

    int getTanks();

    long fill(GasStack resource, GasAction action);

    default AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        for (GasStack resource : resources) {
            if (resource == null || resource.isEmpty()) {
                continue;
            }

            return AtomicFillResult.UNSUPPORTED;
        }
        return AtomicFillResult.SUCCESS;
    }

    /**
     * Attempts to accept every resource from one fixed-pressure source as a single transaction.
     * Implementations must leave their observable state unchanged for {@link GasAction#SIMULATE},
     * {@link AtomicFillResult#REJECTED}, and {@link AtomicFillResult#UNSUPPORTED}.
     */
    default AtomicFillResult tryFillAtomicallyFromPressure(List<GasStack> resources, long sourcePressurePa, GasAction action) {
        for (GasStack resource : resources) {
            if (resource == null || resource.isEmpty()) {
                continue;
            }

            return AtomicFillResult.UNSUPPORTED;
        }
        return AtomicFillResult.SUCCESS;
    }

    enum AtomicFillResult {
        SUCCESS,
        REJECTED,
        UNSUPPORTED;

        public boolean isSuccess() {
            return this == SUCCESS;
        }
    }
}
