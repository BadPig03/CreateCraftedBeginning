package net.ty.createcraftedbeginning.api.canister;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasCanisterContainer {
    int NON_EMPTY_PACK = 1;
    int NON_EMPTY_CANISTER = 0;
    int EMPTY_PACK = -1;
    int EMPTY_CANISTER = -2;

    boolean isEmpty();

    @SuppressWarnings("unused")
    boolean isFull();

    boolean isGasValid(int tank, GasStack stack);

    GasStack drain(int tank, GasStack resource, GasAction action);

    GasStack drain(int tank, long maxDrain, GasAction action);

    GasStack getGasInTank(int tank);

    int getPriority();

    int getTanks();

    ItemStack getContainer();

    List<ItemStack> createVirtualItems();

    default InjectionMode getInjectionMode() {
        return InjectionMode.ALLOW;
    }

    long fill(int tank, GasStack resource, GasAction action);

    default boolean supportsExactDrainRecovery(int tank) {
        return false;
    }

    default long restoreDrainedGas(int tank, GasStack resource, GasAction action) {
        return fill(tank, resource, action);
    }

    long getTankVolume(int tank);

    long getTankMaxPressurePa(int tank);

    default long getTankPressurePa(int tank) {
        return GasPressure.pressure(getGasInTank(tank).getAmount(), getTankVolume(tank));
    }

    default PressureModel getTankPressureModel(int tank) {
        return PressureModel.VARIABLE;
    }

    default long getTankMaxAmount(int tank) {
        return GasPressure.amount(getTankVolume(tank), getTankMaxPressurePa(tank));
    }

    void save();

    enum InjectionMode {
        ALLOW,
        DENY
    }
}
