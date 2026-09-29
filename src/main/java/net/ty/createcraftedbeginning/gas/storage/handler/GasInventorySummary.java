package net.ty.createcraftedbeginning.gas.storage.handler;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.foundation.BoundedMath;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasInventorySummary(long amount, long capacity) {
    public static GasInventorySummary finiteStorage(GasStorageHandler gasStorage) {
        long totalAmount = 0;
        long totalCapacity = 0;
        for (int tank = 0; tank < gasStorage.getTanks(); tank++) {
            if (gasStorage.getTankPressureModel(tank) == PressureModel.FIXED) {
                continue;
            }

            long tankCapacity = Math.max(0, gasStorage.getTankMaxAmount(tank));
            GasStack storedGas = gasStorage.getGasInTank(tank);
            long tankAmount = storedGas.isEmpty() ? 0 : Mth.clamp(storedGas.getAmount(), 0, tankCapacity);
            totalAmount = BoundedMath.saturatedAdd(totalAmount, tankAmount);
            totalCapacity = BoundedMath.saturatedAdd(totalCapacity, tankCapacity);
        }
        return new GasInventorySummary(totalAmount, totalCapacity);
    }
}
