package net.ty.createcraftedbeginning.content.airtights.creativegascanister;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeGasCanisterContainerContents extends GasCanisterContainerContents {
    public static final List<GasStack> DEFAULT_CONTENT = List.of(GasStack.EMPTY);

    public CreativeGasCanisterContainerContents(ItemStack canister) {
        super(canister);
        gas = canister.getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, DEFAULT_CONTENT).getFirst();
        capacity = getDefaultCapacity();
    }

    public static long getDefaultCapacity() {
        return Integer.MAX_VALUE * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
    }

    @Override
    public MachineFillingStrategy getMachineFillingStrategy() {
        return MachineFillingStrategy.DENY;
    }

    @Override
    public boolean isFull() {
        return !getGasInTank(0).isEmpty();
    }

    @Override
    public GasStack drain(int tank, long maxDrain, GasAction action) {
        GasStack storedGas = getGasInTank(0);
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

        return gas.copyWithAmount(capacity);
    }

    @Override
    public long fill(int tank, GasStack resource, GasAction action) {
        return 0;
    }

    @Override
    public long getTankCapacity(int tank) {
        return capacity;
    }

    @Override
    public void setCapacity(int tank, long capacity) {
    }

    public void setGasInTank(int tank, GasStack resource) {
        if (tank != 0) {
            return;
        }

        gas = resource.copyWithAmount(capacity);
        save();
    }
}
