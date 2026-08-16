package net.ty.createcraftedbeginning.api.gas.gases.interfaces;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IGasHandler {
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

    long getTankCapacity(int tank);

    enum AtomicFillResult {
        SUCCESS,
        REJECTED,
        UNSUPPORTED;

        public boolean isSuccess() {
            return this == SUCCESS;
        }
    }
}
