package net.ty.createcraftedbeginning.content.airtights.creativegascanister;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeGasCanisterContainerContents extends GasCanisterContainerContents {
    private static final long CAPACITY = Integer.MAX_VALUE * GasAmounts.MILLIBUCKETS_PER_BUCKET;

    public CreativeGasCanisterContainerContents(ItemStack canister) {
        super(canister);
    }

    public static long getDefaultCapacity() {
        return CAPACITY;
    }

    @Override
    public HatchCanisterType getAirtightHatchType() {
        return HatchCanisterType.CREATIVE;
    }

    @Override
    public MachineFillingMode getMachineFillingMode() {
        return MachineFillingMode.DENY;
    }

    @Override
    public boolean isFull() {
        return !getGasInTank(0).isEmpty();
    }

    @Override
    public GasStack drain(int tank, long maxDrain, GasAction action) {
        if (isInvalidTank(tank)) {
            return GasStack.EMPTY;
        }

        GasStack storedGas = getGasInTank(tank);
        if (storedGas.isEmpty()) {
            return GasStack.EMPTY;
        }
        return storedGas.copyWithAmount(maxDrain);
    }

    @Override
    public GasStack getGasInTank(int tank) {
        if (tank != 0) {
            return GasStack.EMPTY;
        }
        return gas.copyWithAmount(CAPACITY);
    }

    @Override
    public long fill(int tank, GasStack resource, GasAction action) {
        return 0;
    }

    @Override
    public long getTankCapacity(int tank) {
        return isInvalidTank(tank) ? 0 : CAPACITY;
    }

    public void setGasInTank(int tank, GasStack resource) {
        if (tank != 0) {
            return;
        }

        gas = resource.copyWithAmount(CAPACITY);
        save();
    }
}
