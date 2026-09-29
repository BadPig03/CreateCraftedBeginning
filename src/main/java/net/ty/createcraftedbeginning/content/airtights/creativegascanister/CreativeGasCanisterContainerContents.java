package net.ty.createcraftedbeginning.content.airtights.creativegascanister;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeGasCanisterContainerContents extends GasCanisterContainerContents {
    static final long VOLUME_LITERS = Integer.MAX_VALUE * GasUnits.LITERS_PER_KILOLITER;
    static final long PRESSURE_PA = GasPressureLimits.SAFE_PRESSURE_PA;

    CreativeGasCanisterContainerContents(ItemStack canister) {
        super(canister);
    }

    @Override
    public InjectionMode getInjectionMode() {
        return InjectionMode.DENY;
    }

    @Override
    public long restoreDrainedGas(int tankIndex, GasStack resource, GasAction action) {
        if (tankIndex != 0 || resource.isEmpty()) {
            return 0;
        }

        return resource.getAmount();
    }

    @Override
    public PressureModel getTankPressureModel(int tankIndex) {
        if (isInvalidTank(tankIndex)) {
            return PressureModel.VARIABLE;
        }

        return PressureModel.FIXED;
    }

    @Override
    public boolean isFull() {
        return !getGasInTank(0).isEmpty();
    }

    @Override
    public GasStack drain(int tankIndex, long maxDrainAmount, GasAction action) {
        if (isInvalidTank(tankIndex) || maxDrainAmount <= 0) {
            return GasStack.EMPTY;
        }

        GasStack storedGas = getGasInTank(tankIndex);
        if (storedGas.isEmpty()) {
            return GasStack.EMPTY;
        }

        return storedGas.copyWithAmount(maxDrainAmount);
    }

    @Override
    public GasStack getGasInTank(int tankIndex) {
        if (tankIndex != 0) {
            return GasStack.EMPTY;
        }

        return gas.copyWithAmount(getTankMaxAmount(tankIndex));
    }

    @Override
    public long fill(int tankIndex, GasStack gas, GasAction action) {
        return 0;
    }

    @Override
    public long getTankVolume(int tankIndex) {
        if (isInvalidTank(tankIndex)) {
            return 0;
        }

        return VOLUME_LITERS;
    }

    @Override
    public long getTankMaxPressurePa(int tankIndex) {
        if (isInvalidTank(tankIndex)) {
            return 0;
        }

        return PRESSURE_PA;
    }

    @Override
    public HatchCanisterType getAirtightHatchType() {
        return HatchCanisterType.CREATIVE;
    }

    public void setGasInTank(int tankIndex, GasStack newGas) {
        if (tankIndex != 0) {
            return;
        }

        gas = newGas.copyWithAmount(getTankMaxAmount(tankIndex));
        save();
    }
}
