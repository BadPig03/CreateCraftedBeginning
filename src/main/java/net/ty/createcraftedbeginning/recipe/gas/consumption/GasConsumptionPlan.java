package net.ty.createcraftedbeginning.recipe.gas.consumption;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.recipe.pressure.PressureRequirement;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasConsumptionPlan {
    private final List<TankDrain> drains;
    private final long[] tankAmounts;

    GasConsumptionPlan(int tankCount, List<TankDrain> drains) {
        if (tankCount < 0) {
            throw new IllegalArgumentException("Gas consumption plan tank count must be non-negative; got " + tankCount + '.');
        }

        this.drains = List.copyOf(drains);
        tankAmounts = new long[tankCount];
        for (TankDrain drain : this.drains) {
            if (drain.tankIndex < 0 || drain.tankIndex >= tankCount) {
                throw new IllegalArgumentException("Gas consumption plan tank index must be in [0, " + tankCount + "); got " + drain.tankIndex + '.');
            }

            tankAmounts[drain.tankIndex] = Math.addExact(tankAmounts[drain.tankIndex], drain.amount);
        }
    }

    public static GasConsumptionPlan empty(int tankCount) {
        return new GasConsumptionPlan(tankCount, List.of());
    }

    private static void rollback(List<ExecutedDrain> executedDrains) {
        for (int index = executedDrains.size() - 1; index >= 0; index--) {
            ExecutedDrain executedDrain = executedDrains.get(index);
            GasStack drainedGas = executedDrain.gas();
            long restoredAmount = executedDrain.compartment().restoreDrainedGas(drainedGas, GasAction.EXECUTE);
            if (restoredAmount == drainedGas.getAmount()) {
                continue;
            }

            throw new IllegalStateException("Failed to roll back gas consumption drain " + index + ": expected " + drainedGas.getAmount() + " GU, restored " + restoredAmount + " GU.");
        }
    }

    public boolean isEmpty() {
        return drains.isEmpty();
    }

    public long[] tankAmounts() {
        return tankAmounts.clone();
    }

    public double minimumPostConsumptionPressureHeadroomPa() {
        if (drains.isEmpty()) {
            return GasPressure.VACUUM_PA;
        }

        double minimumHeadroomPa = Double.POSITIVE_INFINITY;
        for (TankDrain drain : drains) {
            minimumHeadroomPa = Math.min(minimumHeadroomPa, drain.postConsumptionPressureHeadroomPa());
        }
        return Math.max(GasPressure.VACUUM_PA, minimumHeadroomPa);
    }

    public boolean canExecute() {
        for (TankDrain drain : drains) {
            if (!drain.canExecute()) {
                return false;
            }
        }
        return true;
    }

    public boolean execute() {
        if (!canExecute()) {
            return false;
        }

        List<ExecutedDrain> executedDrains = new ArrayList<>(drains.size());
        for (TankDrain drain : drains) {
            GasStack drainedGas = drain.execute();
            if (!drainedGas.isEmpty()) {
                executedDrains.add(new ExecutedDrain(drain.compartment, drainedGas));
            }
            if (drainedGas.getAmount() == drain.amount && GasStack.isSameGasSameComponents(drainedGas, drain.gas)) {
                continue;
            }

            rollback(executedDrains);
            return false;
        }
        return true;
    }

    static final class TankDrain {
        private final int tankIndex;
        private final GasPressureCompartment compartment;
        private final Object compartmentIdentity;
        private final GasStack gas;
        private final long amount;
        private final PressureRequirement pressure;

        TankDrain(int tankIndex, GasPressureCompartment compartment, GasStack gas, long amount, PressureRequirement pressure) {
            this.tankIndex = tankIndex;
            this.compartment = compartment;
            compartmentIdentity = compartment.getCompartmentIdentity();
            this.gas = gas.copy();
            this.amount = amount;
            this.pressure = pressure;
            if (gas.isEmpty() || amount <= 0) {
                throw new IllegalArgumentException("Gas consumption plan drain requires a non-empty gas stack and a positive amount; got gas='" + gas.getGasType() + "', stackAmount=" + gas.getAmount() + " GU, drainAmount=" + amount + " GU.");
            }
        }

        private boolean canExecute() {
            if (!Objects.equals(compartmentIdentity, compartment.getCompartmentIdentity())) {
                return false;
            }

            GasStack currentGas = compartment.getGasStack();
            if (currentGas.isEmpty() || !GasStack.isSameGasSameComponents(currentGas, gas)) {
                return false;
            }

            long currentPressurePa = compartment.getPressurePa();
            PressureModel pressureModel = compartment.getPressureModel();
            if (pressureModel == PressureModel.FIXED) {
                if (!pressure.allowsPressure(currentPressurePa)) {
                    return false;
                }
            }
            else {
                if (pressure.hasMaximumPressure() && currentPressurePa > pressure.maximumPressurePaOrUnbounded()) {
                    return false;
                }

                long availableAboveFloor = GasPressure.amountAbovePressureFloor(compartment.getStoredAmount(), compartment.getVolume(), pressure.minimumPressurePaOrVacuum());
                if (amount > availableAboveFloor) {
                    return false;
                }
            }

            GasStack request = gas.copyWithAmount(amount);
            GasStack simulatedDrain = compartment.drain(request, GasAction.SIMULATE);
            return simulatedDrain.getAmount() == amount && GasStack.isSameGasSameComponents(simulatedDrain, gas);
        }

        private double postConsumptionPressureHeadroomPa() {
            long baselinePressurePa = Math.max(GasPressure.REFERENCE_PRESSURE_PA, pressure.minimumPressurePaOrVacuum());
            double postConsumptionPressurePa;
            if (compartment.getPressureModel() == PressureModel.FIXED) {
                postConsumptionPressurePa = Math.max(GasPressure.VACUUM_PA, compartment.getPressurePa());
            }
            else {
                long storedAmount = compartment.getStoredAmount();
                long remainingAmount = storedAmount <= amount ? 0 : storedAmount - amount;
                postConsumptionPressurePa = GasPressure.pressureExact(remainingAmount, compartment.getVolume());
            }

            if (Double.isNaN(postConsumptionPressurePa) || postConsumptionPressurePa <= baselinePressurePa) {
                return GasPressure.VACUUM_PA;
            }

            return postConsumptionPressurePa - baselinePressurePa;
        }

        private GasStack execute() {
            return compartment.drain(gas.copyWithAmount(amount), GasAction.EXECUTE);
        }
    }

    private record ExecutedDrain(GasPressureCompartment compartment, GasStack gas) {
        private ExecutedDrain {
            gas = gas.copy();
        }
    }
}
