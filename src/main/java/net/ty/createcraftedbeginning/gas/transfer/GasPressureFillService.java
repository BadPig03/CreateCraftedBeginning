package net.ty.createcraftedbeginning.gas.transfer;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.gas.network.math.GasPressureTransferMath;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPressureFillService {
    private GasPressureFillService() {
    }

    public static boolean fillAllFromFixedPressure(GasHandler target, List<GasStack> resources, long sourcePressurePa) {
        for (GasStack resource : resources) {
            if (resource == null || resource.isEmpty() || sourcePressurePa > GasPressure.VACUUM_PA && fillExactlyFromFixedPressure(target, resource, sourcePressurePa)) {
                continue;
            }

            return false;
        }
        return true;
    }

    public static boolean fillExactlyFromFixedPressure(GasHandler target, GasStack resource, long sourcePressurePa) {
        if (resource.isEmpty()) {
            return true;
        }

        if (sourcePressurePa <= GasPressure.VACUUM_PA) {
            return false;
        }

        long remainingAmount = resource.getAmount();
        for (boolean matchingPass : Iterate.trueAndFalse) {
            for (int tankIndex = 0; tankIndex < target.getTanks() && remainingAmount > 0; tankIndex++) {
                GasStack storedGas = target.getGasInTank(tankIndex);
                boolean matching = !storedGas.isEmpty() && GasStack.isSameGasSameComponents(storedGas, resource);
                if (matchingPass != matching || !matching && !storedGas.isEmpty()) {
                    continue;
                }

                Optional<GasPressureTransferEndpoint> endpoint = GasPressureTransferEndpoint.tryHandler(target, tankIndex);
                if (endpoint.isEmpty()) {
                    continue;
                }

                long transferableAmount = getTransferableAmount(endpoint.get(), resource, remainingAmount, sourcePressurePa);
                if (transferableAmount <= 0) {
                    continue;
                }

                long filledAmount = endpoint.get().executeFill(resource, transferableAmount, sourcePressurePa);
                filledAmount = Mth.clamp(filledAmount, 0L, transferableAmount);
                remainingAmount -= filledAmount;
            }
        }
        return remainingAmount == 0;
    }

    public static long getTransferableAmount(GasPressureTransferEndpoint target, GasStack gas, long maxAmount, long sourcePressurePa) {
        sourcePressurePa = GasPressureLimits.clampToHardLimit(sourcePressurePa);
        long requestedAmount = Math.max(0, maxAmount);
        if (gas.isEmpty() || requestedAmount <= 0 || sourcePressurePa <= GasPressure.VACUUM_PA) {
            return 0;
        }

        double targetPressurePa = target.getFillPressurePa(gas);
        if (sourcePressurePa <= targetPressurePa) {
            return 0;
        }

        long pressureLimitedAmount = requestedAmount;
        if (target.getPressureModel() == PressureModel.VARIABLE) {
            long safeFill = GasPressureTransferMath.maxFillAmount(target.getStoredAmount(), target.getMaxAmount(), target.getVolume(), targetPressurePa, sourcePressurePa, target.getMaxPressurePa());
            pressureLimitedAmount = Math.min(pressureLimitedAmount, safeFill);
        }
        if (pressureLimitedAmount <= 0) {
            return 0;
        }

        long simulatedFill = target.simulateFillAmount(gas, pressureLimitedAmount, sourcePressurePa);
        return Math.min(pressureLimitedAmount, simulatedFill);
    }
}
