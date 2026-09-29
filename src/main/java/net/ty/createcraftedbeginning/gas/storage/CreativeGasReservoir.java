package net.ty.createcraftedbeginning.gas.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureFillService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CreativeGasReservoir implements GasStorageHandler, GasPressureCompartment {
    private static final String COMPOUND_KEY_GAS = "Gas";
    private static final long UNSET_FIXED_PRESSURE_PA = -1;

    public static final Codec<CreativeGasReservoir> CODEC = RecordCodecBuilder.create(instance -> instance.group(GasStack.OPTIONAL_CODEC.fieldOf("gas").forGetter(CreativeGasReservoir::getGasStack), Codec.LONG.fieldOf("volume").forGetter(CreativeGasReservoir::getVolume), Codec.LONG.optionalFieldOf("max_pressure", GasPressure.REFERENCE_PRESSURE_PA).forGetter(CreativeGasReservoir::getMaxPressurePa), Codec.LONG.optionalFieldOf("fixed_pressure", UNSET_FIXED_PRESSURE_PA).forGetter(CreativeGasReservoir::getFixedPressurePa)).apply(instance, (gas, volume, maxPressurePa, fixedPressurePa) -> {
        CreativeGasReservoir reservoir = new CreativeGasReservoir(new GasTankLimits(volume, maxPressurePa), fixedPressurePa < 0 ? maxPressurePa : fixedPressurePa, () -> {});
        reservoir.restoreContainedGas(gas);
        return reservoir;
    }));

    private final Runnable updateCallback;
    private GasTankLimits limits;
    private GasStack gas = GasStack.EMPTY;
    private long fixedPressurePa;

    public CreativeGasReservoir(long volume, long maxPressurePa, Runnable updateCallback) {
        this(new GasTankLimits(volume, maxPressurePa), maxPressurePa, updateCallback);
    }

    public CreativeGasReservoir(long volume, long maxPressurePa, long fixedPressurePa, Runnable updateCallback) {
        this(new GasTankLimits(volume, maxPressurePa), fixedPressurePa, updateCallback);
    }

    public CreativeGasReservoir(GasTankLimits limits, long fixedPressurePa, Runnable updateCallback) {
        this.limits = limits;
        this.updateCallback = updateCallback;
        this.fixedPressurePa = clampFixedPressure(fixedPressurePa);
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
        return true;
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
        return fixedPressurePa;
    }

    @Override
    public long getMaxPressurePa() {
        return limits.maxPressurePa();
    }

    @Override
    public long getStoredAmount() {
        if (gas.isEmpty()) {
            return 0;
        }

        return GasPressure.amount(getVolume(), fixedPressurePa);
    }

    @Override
    public PressureModel getPressureModel() {
        return PressureModel.FIXED;
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        if (resource.isEmpty() || gas.isEmpty() || !GasStack.isSameGasSameComponents(resource, gas)) {
            return GasStack.EMPTY;
        }

        return resource.copy();
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        if (maxDrain <= 0 || gas.isEmpty()) {
            return GasStack.EMPTY;
        }

        return gas.copyWithAmount(maxDrain);
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        return resource.getAmount();
    }

    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        for (GasStack resource : resources) {
            if (resource == null || resource.isEmpty() || fill(resource, GasAction.SIMULATE) == resource.getAmount()) {
                continue;
            }

            return AtomicFillResult.REJECTED;
        }
        return AtomicFillResult.SUCCESS;
    }

    @Override
    public AtomicFillResult tryFillAtomicallyFromPressure(List<GasStack> resources, long sourcePressurePa, GasAction action) {
        if (GasPressureFillService.fillAllFromFixedPressure(this, resources, sourcePressurePa)) {
            return AtomicFillResult.SUCCESS;
        }

        return AtomicFillResult.REJECTED;
    }

    public GasTankLimits getLimits() {
        return limits;
    }

    public void reconfigure(GasTankLimits newLimits) {
        if (limits.equals(newLimits)) {
            return;
        }

        limits = newLimits;
        fixedPressurePa = clampFixedPressure(fixedPressurePa);
        normalizeStoredGas();
        onStateChanged();
    }

    public long getFixedPressurePa() {
        return fixedPressurePa;
    }

    public void setFixedPressurePa(long newFixedPressurePa) {
        long clampedPressurePa = clampFixedPressure(newFixedPressurePa);
        if (fixedPressurePa == clampedPressurePa) {
            return;
        }

        fixedPressurePa = clampedPressurePa;
        normalizeStoredGas();
        onStateChanged();
    }

    public void restoreFixedPressurePa(long pressurePa) {
        fixedPressurePa = clampFixedPressure(pressurePa);
        normalizeStoredGas();
    }

    public void setContainedGas(GasStack gasStack) {
        GasStack normalizedGas = normalizedCopy(gasStack);
        if (GasStack.matches(gas, normalizedGas)) {
            return;
        }

        gas = normalizedGas;
        onStateChanged();
    }

    public void read(Provider provider, CompoundTag compoundTag) {
        gas = GasStack.EMPTY;
        if (!compoundTag.contains(COMPOUND_KEY_GAS)) {
            return;
        }

        restoreContainedGas(GasStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_GAS)));
    }

    public CompoundTag write(Provider provider, CompoundTag compoundTag) {
        compoundTag.put(COMPOUND_KEY_GAS, gas.saveOptional(provider));
        return compoundTag;
    }

    private void restoreContainedGas(GasStack gasStack) {
        gas = normalizedCopy(gasStack);
    }

    private void normalizeStoredGas() {
        gas = normalizedCopy(gas);
    }

    private GasStack normalizedCopy(GasStack stack) {
        if (stack.isEmpty()) {
            return GasStack.EMPTY;
        }

        long normalizedAmount = Math.max(1, GasPressure.amount(getVolume(), fixedPressurePa));
        return stack.copyWithAmount(normalizedAmount);
    }

    private long clampFixedPressure(long pressurePa) {
        return Mth.clamp(pressurePa, GasPressure.VACUUM_PA, getMaxPressurePa());
    }

    private void onStateChanged() {
        updateCallback.run();
    }
}
