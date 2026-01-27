package net.ty.createcraftedbeginning.api.gas.cansiters;

import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import java.util.List;

public interface IGasCanisterContainer {
    int NON_EMPTY_PACK = 1;
    int NON_EMPTY_CANISTER = 0;
    int EMPTY_PACK = -1;
    int EMPTY_CANISTER = -2;

    int getTanks();

    GasStack getGasInTank(int tank);

    long getTankCapacity(int tank);

    void setCapacity(int tank, long capacity);

    boolean isGasValid(int tank, GasStack stack);

    long fill(int tank, GasStack resource, GasAction action);

    GasStack drain(int tank, GasStack resource, GasAction action);

    GasStack drain(int tank, long maxDrain, GasAction action);

    ItemStack getContainer();

    List<ItemStack> getVirtualItems();

    void save();

    boolean isEmpty();

    boolean isFull();

    int getPriority();
}
