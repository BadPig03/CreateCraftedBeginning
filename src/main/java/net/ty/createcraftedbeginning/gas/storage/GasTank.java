package net.ty.createcraftedbeginning.gas.storage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureFillService;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferEndpoint;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasTank implements GasStorageHandler, GasPressureCompartment {
    private static final String COMPOUND_KEY_GAS = "Gas";

    private final Predicate<GasStack> validator;
    private GasTankLimits limits;
    private GasStack gas = GasStack.EMPTY;

    public GasTank(long volume) {
        this(volume, GasPressure.REFERENCE_PRESSURE_PA, gasStack -> true);
    }

    public GasTank(long volume, Predicate<GasStack> validator) {
        this(volume, GasPressure.REFERENCE_PRESSURE_PA, validator);
    }

    public GasTank(long volume, long maxPressurePa) {
        this(volume, maxPressurePa, gasStack -> true);
    }

    public GasTank(long volume, long maxPressurePa, Predicate<GasStack> validator) {
        this(new GasTankLimits(volume, maxPressurePa), validator);
    }

    public GasTank(GasTankLimits limits) {
        this(limits, gasStack -> true);
    }

    public GasTank(GasTankLimits limits, Predicate<GasStack> validator) {
        this.limits = limits;
        this.validator = validator;
    }

    @Override
    public PredictedTransferLimits predictTransferLimits(GasStack gasType, long storedAmount) {
        if (gasType.isEmpty() || !gas.isEmpty() && !GasStack.isSameGasSameComponents(gas, gasType)) {
            return new PredictedTransferLimits(0, 0);
        }

        long amount = Math.max(0, Math.min(getMaxAmount(), storedAmount));
        if (!isGasValid(gasType)) {
            return new PredictedTransferLimits(amount, 0);
        }

        return new PredictedTransferLimits(amount, getMaxAmount() - amount);
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
    public boolean isGasValid(GasStack stack) {
        return validator.test(stack);
    }

    @Override
    public GasStack getGasStack() {
        return gas.copy();
    }

    @Override
    public boolean supportsExactDrainRecovery() {
        return true;
    }

    @Override
    public long getVolume() {
        return limits.volumeLiters();
    }

    @Override
    public long getPressurePa() {
        return GasPressure.pressure(getStoredAmount(), getVolume());
    }

    @Override
    public long getMaxPressurePa() {
        return limits.maxPressurePa();
    }

    @Override
    public long getMaxAmount() {
        return limits.maxAmount();
    }

    @Override
    public long getStoredAmount() {
        return gas.getAmount();
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        if (resource.isEmpty() || !GasStack.isSameGasSameComponents(resource, gas)) {
            return GasStack.EMPTY;
        }

        return drain(resource.getAmount(), action);
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        if (maxDrain <= 0 || gas.isEmpty()) {
            return GasStack.EMPTY;
        }

        long drained = Math.min(maxDrain, gas.getAmount());
        GasStack stack = gas.copyWithAmount(drained);
        if (!action.execute() || drained <= 0) {
            return stack;
        }

        gas.shrink(drained);
        onStateChanged();
        return stack;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        if (resource.isEmpty() || !isGasValid(resource)) {
            return 0;
        }

        if (action.simulate()) {
            return getFillableAmount(resource);
        }

        if (gas.isEmpty()) {
            return fillEmptyTank(resource);
        }

        if (!GasStack.isSameGasSameComponents(gas, resource)) {
            return 0;
        }

        long remainingSpace = getRemainingAmount();
        long amountToTransfer = Math.min(remainingSpace, resource.getAmount());
        gas.grow(amountToTransfer);
        if (amountToTransfer <= 0) {
            return amountToTransfer;
        }

        onStateChanged();
        return amountToTransfer;
    }

    @Override
    public AtomicFillResult tryFillAtomicallyFromPressure(List<GasStack> resources, long sourcePressurePa, GasAction action) {
        GasStack combinedResource = GasStack.EMPTY;
        for (GasStack resource : resources) {
            if (resource == null || resource.isEmpty()) {
                continue;
            }

            if (combinedResource.isEmpty()) {
                combinedResource = resource.copy();
                continue;
            }

            if (!GasStack.isSameGasSameComponents(combinedResource, resource)) {
                return AtomicFillResult.REJECTED;
            }

            combinedResource.grow(resource.getAmount());
        }
        if (combinedResource.isEmpty()) {
            return AtomicFillResult.SUCCESS;
        }

        GasPressureTransferEndpoint target = GasPressureTransferEndpoint.compartment(this);
        long transferableAmount = GasPressureFillService.getTransferableAmount(target, combinedResource, combinedResource.getAmount(), sourcePressurePa);
        if (transferableAmount != combinedResource.getAmount()) {
            return AtomicFillResult.REJECTED;
        }

        if (!action.execute()) {
            return AtomicFillResult.SUCCESS;
        }

        GasTankState stateSnapshot = snapshot();
        long filledAmount = target.executeFill(combinedResource, combinedResource.getAmount(), sourcePressurePa);
        if (filledAmount == combinedResource.getAmount()) {
            return AtomicFillResult.SUCCESS;
        }

        tryApplyState(stateSnapshot).requireAccepted();
        return AtomicFillResult.REJECTED;
    }

    public GasTankLimits getLimits() {
        return limits;
    }

    public GasTankState snapshot() {
        return new GasTankState(limits, gas);
    }

    public boolean canContain(GasTankLimits candidateLimits, GasStack contents) {
        return isCandidateGasValid(contents) && getExcessAmount(candidateLimits, contents) == 0;
    }

    public GasTankMutationResult tryReplaceContents(GasStack contents) {
        return applyCandidate(limits, contents, true);
    }

    public GasTankMutationResult tryReconfigure(GasTankLimits candidateLimits) {
        return applyCandidate(candidateLimits, gas, true);
    }

    public GasTankMutationResult tryApplyState(GasTankState state) {
        return applyCandidate(state.limits(), state.contents(), true);
    }

    public GasTankMutationResult read(Provider provider, CompoundTag compoundTag) {
        return applyCandidate(limits, readGas(provider, compoundTag), false);
    }

    public GasStack readClampedToLimits(Provider provider, CompoundTag compoundTag) {
        GasStack loadedGas = readGas(provider, compoundTag);
        if (!isCandidateGasValid(loadedGas)) {
            applyCandidate(limits, GasStack.EMPTY, false).requireAccepted();
            return GasStack.EMPTY;
        }

        long retainedAmount = Math.min(loadedGas.getAmount(), getMaxAmount());
        GasStack retainedGas = retainedAmount <= 0 ? GasStack.EMPTY : loadedGas.copyWithAmount(retainedAmount);
        applyCandidate(limits, retainedGas, false).requireAccepted();
        long excessAmount = loadedGas.getAmount() - retainedAmount;
        if (excessAmount <= 0) {
            return GasStack.EMPTY;
        }

        return loadedGas.copyWithAmount(excessAmount);
    }

    public CompoundTag write(Provider provider, CompoundTag compoundTag) {
        compoundTag.put(COMPOUND_KEY_GAS, gas.saveOptional(provider));
        return compoundTag;
    }

    public boolean isEmpty() {
        return gas.isEmpty();
    }

    public long getRemainingAmount() {
        return Math.max(0, BoundedMath.saturatedSubtract(getMaxAmount(), gas.getAmount()));
    }

    protected void onStateChanged() {
    }

    private static GasStack readGas(Provider provider, CompoundTag compoundTag) {
        if (!compoundTag.contains(COMPOUND_KEY_GAS)) {
            return GasStack.EMPTY;
        }

        return GasStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_GAS));
    }

    private static long getExcessAmount(GasTankLimits candidateLimits, GasStack contents) {
        return Math.max(0, contents.getAmount() - candidateLimits.maxAmount());
    }

    private GasTankMutationResult applyCandidate(GasTankLimits candidateLimits, GasStack contents, boolean notifyChange) {
        GasStack candidateGas = contents.copy();
        if (!isCandidateGasValid(candidateGas)) {
            return GasTankMutationResult.invalidGas();
        }

        long excessAmount = getExcessAmount(candidateLimits, candidateGas);
        if (excessAmount > 0) {
            return GasTankMutationResult.exceedsPressureLimit(excessAmount);
        }

        if (limits.equals(candidateLimits) && GasStack.matches(gas, candidateGas)) {
            return GasTankMutationResult.unchanged();
        }

        limits = candidateLimits;
        gas = candidateGas;
        if (notifyChange) {
            onStateChanged();
        }
        return GasTankMutationResult.applied();
    }

    private boolean isCandidateGasValid(GasStack contents) {
        return contents.isEmpty() || isGasValid(contents);
    }

    private long getFillableAmount(GasStack resource) {
        if (gas.isEmpty()) {
            return Math.min(getMaxAmount(), resource.getAmount());
        }

        if (!GasStack.isSameGasSameComponents(gas, resource)) {
            return 0;
        }

        return Math.min(getRemainingAmount(), resource.getAmount());
    }

    private long fillEmptyTank(GasStack resource) {
        long amount = Math.min(getMaxAmount(), resource.getAmount());
        if (amount <= 0) {
            return 0;
        }

        gas = resource.copyWithAmount(amount);
        onStateChanged();
        return amount;
    }
}
