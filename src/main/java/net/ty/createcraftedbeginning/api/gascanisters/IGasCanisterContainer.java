package net.ty.createcraftedbeginning.api.gascanisters;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IGasCanisterContainer {
    int NON_EMPTY_PACK = 1;
    int NON_EMPTY_CANISTER = 0;
    int EMPTY_PACK = -1;
    int EMPTY_CANISTER = -2;

    boolean isEmpty();

    boolean isFull();

    boolean isGasValid(int tank, GasStack stack);

    GasStack drain(int tank, GasStack resource, GasAction action);

    GasStack drain(int tank, long maxDrain, GasAction action);

    GasStack getGasInTank(int tank);

    int getPriority();

    int getTanks();

    ItemStack getContainer();

    List<ItemStack> getVirtualItems();

    long fill(int tank, GasStack resource, GasAction action);

    long getTankCapacity(int tank);

    void save();

    void setCapacity(int tank, long capacity);
}
