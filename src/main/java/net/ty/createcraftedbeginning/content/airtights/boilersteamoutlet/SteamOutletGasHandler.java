package net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class SteamOutletGasHandler implements GasStorageHandler, GasPressureCompartment {
    private static final String COMPOUND_KEY_STORED_STEAM = "StoredSteam";

    private final BoilerSteamOutletBlockEntity outlet;
    private long storedSteam;

    SteamOutletGasHandler(BoilerSteamOutletBlockEntity outlet) {
        this.outlet = outlet;
    }

    @Override
    public GasPressureCompartment getPressureCompartment(int tank) {
        if (tank != 0) {
            throw new IndexOutOfBoundsException("Tank index must be in [0, 1); got " + tank + '.');
        }

        return this;
    }

    @Override
    public Object getCompartmentIdentity() {
        return this;
    }

    @Override
    public boolean isGasValid(int tankIndex, GasStack stack) {
        return tankIndex == 0 && isGasValid(stack);
    }

    @Override
    public boolean isGasValid(GasStack stack) {
        return !stack.isEmpty() && stack.is(CCBGases.STEAM);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        outlet.ensureCurrentTick();
        GasStack availableSteam = getBufferedSteamStack();
        if (resource.isEmpty() || availableSteam.isEmpty() || !GasStack.isSameGasSameComponents(resource, availableSteam)) {
            return GasStack.EMPTY;
        }

        GasStack drainedSteam = drainBuffered(resource.getAmount(), action);
        outlet.recordExtraction(drainedSteam, action);
        return drainedSteam;
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        outlet.ensureCurrentTick();
        GasStack drainedSteam = drainBuffered(maxDrain, action);
        outlet.recordExtraction(drainedSteam, action);
        return drainedSteam;
    }

    @Override
    public GasStack getGasInTank(int tankIndex) {
        if (tankIndex != 0) {
            return GasStack.EMPTY;
        }

        return getGasStack();
    }

    @Override
    public GasStack getGasStack() {
        outlet.ensureCurrentTick();
        return getBufferedSteamStack();
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        return 0;
    }

    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        for (GasStack resource : resources) {
            if (resource == null || resource.isEmpty()) {
                continue;
            }

            return AtomicFillResult.REJECTED;
        }
        return AtomicFillResult.SUCCESS;
    }

    @Override
    public boolean supportsExactDrainRecovery() {
        return true;
    }

    @Override
    public long restoreDrainedGas(GasStack resource, GasAction action) {
        if (!isGasValid(resource)) {
            return 0;
        }

        normalizeStoredSteam();
        long recoveryCapacity = Math.max(0, getMaxAmount() - storedSteam);
        long restoredAmount = Math.min(resource.getAmount(), recoveryCapacity);
        if (restoredAmount <= 0 || action.simulate()) {
            return restoredAmount;
        }

        storedSteam += restoredAmount;
        outlet.restoreExtraction(resource.copyWithAmount(restoredAmount), action);
        outlet.setChanged();
        return restoredAmount;
    }

    @Override
    public long getVolume() {
        return BoilerSteamOutletProduction.getOutputVolume();
    }

    @Override
    public long getPressurePa() {
        outlet.ensureCurrentTick();
        normalizeStoredSteam();
        long volume = getVolume();
        if (volume <= 0) {
            return GasPressure.VACUUM_PA;
        }

        return GasPressure.pressure(storedSteam, volume);
    }

    @Override
    public long getMaxPressurePa() {
        return BoilerSteamOutletProduction.getMaximumOutputPressurePa();
    }

    @Override
    public long getMaxAmount() {
        return outlet.getMaximumOutputAmount();
    }

    @Override
    public long getStoredAmount() {
        outlet.ensureCurrentTick();
        normalizeStoredSteam();
        return storedSteam;
    }

    @Override
    public PressureModel getPressureModel() {
        return PressureModel.FIXED;
    }

    void addProducedSteam(long producedSteam) {
        if (producedSteam <= 0) {
            return;
        }

        normalizeStoredSteam();
        long remainingCapacity = Math.max(0, getMaxAmount() - storedSteam);
        long accepted = Math.min(producedSteam, remainingCapacity);
        if (accepted <= 0) {
            return;
        }

        storedSteam += accepted;
        outlet.setChanged();
    }

    void clearBufferedSteam() {
        if (storedSteam <= 0) {
            return;
        }

        storedSteam = 0;
        outlet.setChanged();
    }

    void write(CompoundTag compoundTag, boolean clientPacket) {
        if (clientPacket) {
            return;
        }

        compoundTag.putLong(COMPOUND_KEY_STORED_STEAM, storedSteam);
    }

    void read(CompoundTag compoundTag, boolean clientPacket) {
        if (clientPacket) {
            return;
        }

        storedSteam = Math.max(0, compoundTag.getLong(COMPOUND_KEY_STORED_STEAM));
        normalizeStoredSteam();
    }

    private GasStack drainBuffered(long maxDrain, GasAction action) {
        normalizeStoredSteam();
        long drainedAmount = Mth.clamp(maxDrain, 0L, storedSteam);
        if (drainedAmount <= 0) {
            return GasStack.EMPTY;
        }

        GasStack drainedSteam = new GasStack(CCBGases.STEAM.get(), drainedAmount);
        if (action.execute()) {
            storedSteam -= drainedAmount;
            outlet.setChanged();
        }
        return drainedSteam;
    }

    private GasStack getBufferedSteamStack() {
        normalizeStoredSteam();
        if (storedSteam <= 0) {
            return GasStack.EMPTY;
        }

        return new GasStack(CCBGases.STEAM.get(), storedSteam);
    }

    private void normalizeStoredSteam() {
        storedSteam = Mth.clamp(storedSteam, 0L, getMaxAmount());
    }
}
