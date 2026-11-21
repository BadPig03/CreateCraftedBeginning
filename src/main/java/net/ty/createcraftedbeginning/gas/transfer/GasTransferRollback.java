package net.ty.createcraftedbeginning.gas.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.handler.GasTransferSnapshot;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasTransferRollback {
    private GasTransferRollback() {
    }

    public static boolean restore(@Nullable GasTransferSnapshot snapshot, Throwable failure) {
        if (snapshot == null) {
            return false;
        }

        boolean restored = false;
        try {
            snapshot.restore();
            restored = true;
            snapshot.afterRestore();
        }
        catch (RuntimeException | Error rollbackFailure) {
            if (rollbackFailure != failure) {
                failure.addSuppressed(rollbackFailure);
            }
        }
        return restored;
    }
}
