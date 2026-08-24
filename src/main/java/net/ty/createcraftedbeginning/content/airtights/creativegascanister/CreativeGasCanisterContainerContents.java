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
    public InjectionMode getInjectionMode() {
        return InjectionMode.DENY;
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
        return gas.copyWithAmount(CAPACITY);
    }

    @Override
    public long fill(int tankIndex, GasStack gas, GasAction action) {
        return 0;
    }

    @Override
    public long getTankCapacity(int tankIndex) {
        return isInvalidTank(tankIndex) ? 0 : CAPACITY;
    }

    @Override
    public HatchCanisterType getAirtightHatchType() {
        return HatchCanisterType.CREATIVE;
    }

    public void setGasInTank(int tankIndex, GasStack newGas) {
        if (tankIndex != 0) {
            return;
        }

        gas = newGas.copyWithAmount(CAPACITY);
        save();
    }
}
