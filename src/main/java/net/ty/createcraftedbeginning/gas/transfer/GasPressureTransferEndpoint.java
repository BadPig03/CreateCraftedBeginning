package net.ty.createcraftedbeginning.gas.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPressureTransferEndpoint {
    private final Access access;

    private GasPressureTransferEndpoint(Access access) {
        this.access = access;
    }

    public static GasPressureTransferEndpoint compartment(GasPressureCompartment compartment) {
        return new GasPressureTransferEndpoint(new CompartmentAccess(compartment));
    }

    public static GasPressureTransferEndpoint storage(GasStorageHandler storage, int tank) {
        if (tank < 0 || tank >= storage.getTanks()) {
            throw new IndexOutOfBoundsException("Tank index must be in [0, " + storage.getTanks() + "); got " + tank + '.');
        }

        return compartment(storage.getPressureCompartment(tank));
    }

    public static GasPressureTransferEndpoint canister(GasCanisterContainer canister, int tank) {
        if (tank < 0 || tank >= canister.getTanks()) {
            throw new IndexOutOfBoundsException("Tank index must be in [0, " + canister.getTanks() + "); got " + tank + '.');
        }

        return new GasPressureTransferEndpoint(new CanisterAccess(canister, tank));
    }

    public static GasPressureTransferEndpoint pressureBoundary(GasPressureBoundary boundary) {
        if (boundary.getTanks() != 1) {
            throw new IllegalArgumentException("Pressure boundary direct-transfer endpoints must expose exactly one tank; got " + boundary.getTanks() + '.');
        }

        return new GasPressureTransferEndpoint(new BoundaryAccess(boundary));
    }

    public static Optional<GasPressureTransferEndpoint> tryHandler(GasHandler handler, int tank) {
        if (tank < 0 || tank >= handler.getTanks()) {
            return Optional.empty();
        }

        return switch (handler) {
            case GasPressureCompartment compartment -> {
                if (tank == 0) {
                    yield Optional.of(compartment(compartment));
                }

                yield Optional.empty();
            }
            case GasStorageHandler storage -> Optional.of(storage(storage, tank));
            case GasPressureBoundary boundary when tank == 0 && boundary.getTanks() == 1 -> Optional.of(pressureBoundary(boundary));
            default -> Optional.empty();
        };
    }

    public boolean identifiesSameCompartment(GasPressureTransferEndpoint other) {
        return access.identityOwner() == other.access.identityOwner() && access.identityTank() == other.access.identityTank();
    }

    public GasStack getGasStack() {
        return access.getGasStack();
    }

    public PressureModel getPressureModel() {
        return access.getPressureModel();
    }

    public long getStoredAmount() {
        return Math.max(0, access.getStoredAmount());
    }

    public long getVolume() {
        return Math.max(0, access.getVolume());
    }

    public long getMaxPressurePa() {
        return GasPressureLimits.clampToHardLimit(access.getMaxPressurePa());
    }

    public long getMaxAmount() {
        return Math.max(0, access.getMaxAmount());
    }

    public boolean supportsExactDrainRecovery() {
        return access.supportsExactDrainRecovery();
    }

    public double getDrainPressurePa(GasStack gas) {
        return normalizePressure(access.getDrainPressurePa(gas));
    }

    public double getFillPressurePa(GasStack gas) {
        return normalizePressure(access.getFillPressurePa(gas));
    }

    public long simulateDrainAmount(GasStack gas, long maxAmount) {
        long requestedAmount = Math.max(0, maxAmount);
        if (gas.isEmpty() || requestedAmount <= 0) {
            return 0;
        }

        GasStack drained = access.drain(gas, requestedAmount, GasAction.SIMULATE);
        if (drained.isEmpty() || !GasStack.isSameGasSameComponents(drained, gas)) {
            return 0;
        }

        return Mth.clamp(drained.getAmount(), 0L, requestedAmount);
    }

    public GasStack executeDrain(GasStack gas, long maxAmount) {
        if (gas.isEmpty() || maxAmount <= 0) {
            return GasStack.EMPTY;
        }

        GasStack drained = access.drain(gas, maxAmount, GasAction.EXECUTE);
        if (drained.isEmpty() || !GasStack.isSameGasSameComponents(drained, gas)) {
            return GasStack.EMPTY;
        }

        long drainedAmount = Mth.clamp(drained.getAmount(), 0L, maxAmount);
        if (drainedAmount <= 0) {
            return GasStack.EMPTY;
        }

        return gas.copyWithAmount(drainedAmount);
    }

    public long simulateFillAmount(GasStack gas, long maxAmount, long sourcePressurePa) {
        if (gas.isEmpty() || maxAmount <= 0) {
            return 0;
        }

        long filled = access.fill(gas.copyWithAmount(maxAmount), GasPressureLimits.clampToHardLimit(sourcePressurePa), GasAction.SIMULATE);
        return Mth.clamp(filled, 0L, maxAmount);
    }

    public long executeFill(GasStack gas, long maxAmount, long sourcePressurePa) {
        if (gas.isEmpty() || maxAmount <= 0) {
            return 0;
        }

        long filled = access.fill(gas.copyWithAmount(maxAmount), GasPressureLimits.clampToHardLimit(sourcePressurePa), GasAction.EXECUTE);
        return Mth.clamp(filled, 0L, maxAmount);
    }

    public long restoreDrainedGas(GasStack gas) {
        if (gas.isEmpty()) {
            return 0;
        }

        long restored = access.restore(gas, GasAction.EXECUTE);
        return Mth.clamp(restored, 0L, gas.getAmount());
    }

    private static double normalizePressure(double pressurePa) {
        return GasPressureLimits.clampToHardLimit(pressurePa);
    }

    private interface Access {
        Object identityOwner();

        int identityTank();

        GasStack getGasStack();

        PressureModel getPressureModel();

        long getStoredAmount();

        long getVolume();

        long getMaxPressurePa();

        long getMaxAmount();

        boolean supportsExactDrainRecovery();

        double getDrainPressurePa(GasStack gas);

        double getFillPressurePa(GasStack gas);

        GasStack drain(GasStack gas, long maxAmount, GasAction action);

        long fill(GasStack gas, long sourcePressurePa, GasAction action);

        long restore(GasStack gas, GasAction action);
    }

    private record CompartmentAccess(GasPressureCompartment compartment) implements Access {
        @Override
        public Object identityOwner() {
            return compartment.getCompartmentIdentity();
        }

        @Override
        public int identityTank() {
            return 0;
        }

        @Override
        public GasStack getGasStack() {
            return compartment.getGasStack();
        }

        @Override
        public PressureModel getPressureModel() {
            return compartment.getPressureModel();
        }

        @Override
        public long getStoredAmount() {
            return compartment.getStoredAmount();
        }

        @Override
        public long getVolume() {
            return compartment.getVolume();
        }

        @Override
        public long getMaxPressurePa() {
            return compartment.getMaxPressurePa();
        }

        @Override
        public long getMaxAmount() {
            return compartment.getMaxAmount();
        }

        @Override
        public boolean supportsExactDrainRecovery() {
            return compartment.supportsExactDrainRecovery();
        }

        @Override
        public double getDrainPressurePa(GasStack ignoredGas) {
            return pressureExact(compartment);
        }

        @Override
        public double getFillPressurePa(GasStack ignoredGas) {
            return pressureExact(compartment);
        }

        @Override
        public GasStack drain(GasStack gas, long maxAmount, GasAction action) {
            return compartment.drain(gas.copyWithAmount(maxAmount), action);
        }

        @Override
        public long fill(GasStack gas, long ignoredSourcePressurePa, GasAction action) {
            return compartment.fill(gas, action);
        }

        @Override
        public long restore(GasStack gas, GasAction action) {
            return compartment.restoreDrainedGas(gas, action);
        }

        private static double pressureExact(GasPressureCompartment compartment) {
            if (compartment.getPressureModel() == PressureModel.FIXED) {
                return Math.max(GasPressure.VACUUM_PA, compartment.getPressurePa());
            }

            return GasPressure.pressureExact(compartment.getStoredAmount(), compartment.getVolume());
        }
    }

    private static final class CanisterAccess implements Access {
        private final GasCanisterContainer canister;
        private final Object identityOwner;
        private final int tank;

        private CanisterAccess(GasCanisterContainer canister, int tank) {
            this.canister = canister;
            ItemStack container = canister.getContainer();
            identityOwner = container.isEmpty() ? canister : container;
            this.tank = tank;
        }

        @Override
        public Object identityOwner() {
            return identityOwner;
        }

        @Override
        public int identityTank() {
            return tank;
        }

        @Override
        public GasStack getGasStack() {
            return canister.getGasInTank(tank);
        }

        @Override
        public PressureModel getPressureModel() {
            return canister.getTankPressureModel(tank);
        }

        @Override
        public long getStoredAmount() {
            return canister.getGasInTank(tank).getAmount();
        }

        @Override
        public long getVolume() {
            return canister.getTankVolume(tank);
        }

        @Override
        public long getMaxPressurePa() {
            return canister.getTankMaxPressurePa(tank);
        }

        @Override
        public long getMaxAmount() {
            return canister.getTankMaxAmount(tank);
        }

        @Override
        public boolean supportsExactDrainRecovery() {
            return canister.supportsExactDrainRecovery(tank);
        }

        @Override
        public double getDrainPressurePa(GasStack ignoredGas) {
            return pressureExact();
        }

        @Override
        public double getFillPressurePa(GasStack ignoredGas) {
            return pressureExact();
        }

        @Override
        public GasStack drain(GasStack gas, long maxAmount, GasAction action) {
            return canister.drain(tank, gas.copyWithAmount(maxAmount), action);
        }

        @Override
        public long fill(GasStack gas, long ignoredSourcePressurePa, GasAction action) {
            return canister.fill(tank, gas, action);
        }

        @Override
        public long restore(GasStack gas, GasAction action) {
            return canister.restoreDrainedGas(tank, gas, action);
        }

        private double pressureExact() {
            if (getPressureModel() == PressureModel.FIXED) {
                return Math.max(GasPressure.VACUUM_PA, canister.getTankPressurePa(tank));
            }

            return GasPressure.pressureExact(getStoredAmount(), getVolume());
        }
    }

    private record BoundaryAccess(GasPressureBoundary boundary) implements Access {
        @Override
        public Object identityOwner() {
            return boundary;
        }

        @Override
        public int identityTank() {
            return 0;
        }

        @Override
        public GasStack getGasStack() {
            return boundary.getGasInTank(0);
        }

        @Override
        public PressureModel getPressureModel() {
            return PressureModel.FIXED;
        }

        @Override
        public long getStoredAmount() {
            return boundary.getGasInTank(0).getAmount();
        }

        @Override
        public long getVolume() {
            return Long.MAX_VALUE;
        }

        @Override
        public long getMaxPressurePa() {
            return Long.MAX_VALUE;
        }

        @Override
        public long getMaxAmount() {
            return Long.MAX_VALUE;
        }

        @Override
        public boolean supportsExactDrainRecovery() {
            return boundary.supportsExactDrainRecovery(0);
        }

        @Override
        public double getDrainPressurePa(GasStack gas) {
            return Math.max(GasPressure.VACUUM_PA, boundary.getDrainPressurePa(0, gas));
        }

        @Override
        public double getFillPressurePa(GasStack gas) {
            return Math.max(GasPressure.VACUUM_PA, boundary.getFillPressurePa(0, gas));
        }

        @Override
        public GasStack drain(GasStack gas, long maxAmount, GasAction action) {
            GasStack request = gas.copyWithAmount(maxAmount);
            GasStack drained = boundary.drain(request, action);
            if (!drained.isEmpty()) {
                return drained;
            }

            GasStack genericPreview = boundary.drain(maxAmount, GasAction.SIMULATE);
            if (genericPreview.isEmpty() || !GasStack.isSameGasSameComponents(genericPreview, gas)) {
                return GasStack.EMPTY;
            }

            if (action.execute()) {
                return boundary.drain(maxAmount, GasAction.EXECUTE);
            }

            return genericPreview;
        }

        @Override
        public long fill(GasStack gas, long sourcePressurePa, GasAction action) {
            return boundary.fillFromPressure(gas, sourcePressurePa, action);
        }

        @Override
        public long restore(GasStack gas, GasAction action) {
            return boundary.restoreDrainedGas(0, gas, action);
        }
    }
}
