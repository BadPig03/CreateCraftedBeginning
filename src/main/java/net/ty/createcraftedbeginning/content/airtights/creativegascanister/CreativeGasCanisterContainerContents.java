package net.ty.createcraftedbeginning.content.airtights.creativegascanister;

import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("unused")
public class CreativeGasCanisterContainerContents extends GasCanisterContainerContents {
    public static final List<GasStack> DEFAULT_CONTENT = List.of(GasStack.EMPTY);

    public CreativeGasCanisterContainerContents(@NotNull ItemStack canister) {
        super(canister);
        gas = canister.getOrDefault(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, DEFAULT_CONTENT).getFirst();
        capacity = getDefaultCapacity();
    }

    public static long getDefaultCapacity() {
        return CCBConfig.server().airtights.maxCanisterCapacity.get() * 1000L;
    }

    @Override
    public long getTankCapacity(int tank) {
        return capacity;
    }

    @Override
    public void setCapacity(int tank, long capacity) {
    }

    @Override
    public GasStack getGasInTank(int tank) {
        return gas.copyWithAmount(capacity);
    }

    public void setGasInTank(int tank, @NotNull GasStack resource) {
        gas = resource.copyWithAmount(capacity);
        save();
    }

    @Override
    public long fill(int tank, @NotNull GasStack resource, GasAction action) {
        if (resource.isEmpty()) {
            return 0;
        }

        return resource.getAmount();
    }

    @Override
    public GasStack drain(int tank, long maxDrain, @NotNull GasAction action) {
        GasStack gas = getGasInTank(0);
        long drained = Math.min(maxDrain, gas.getAmount());
        return gas.copyWithAmount(drained);
    }

    @Override
    public boolean isFull() {
        return !getGasInTank(0).isEmpty();
    }
}
