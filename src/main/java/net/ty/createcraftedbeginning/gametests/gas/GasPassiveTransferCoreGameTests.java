package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferEndpoint;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferResult;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferService;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasPassiveTransferCoreGameTests {
    private static final long ATM = GasPressure.REFERENCE_PRESSURE_PA;

    private GasPassiveTransferCoreGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void equalVolumeVariableTanksStopAtCommonEquilibrium(GameTestHelper helper) {
        GasTank source = tank(1000, 20, 10000);
        GasTank target = tank(1000, 20, 0);

        GasPressureTransferResult result = transfer(source, target, GasAction.EXECUTE);

        helper.assertValueEqual(result.transferableAmount(), 5000L, "equal-volume transferable amount");
        helper.assertValueEqual(result.filledAmount(), 5000L, "equal-volume filled amount");
        helper.assertValueEqual(source.getStoredAmount(), 5000L, "equal-volume source amount after transfer");
        helper.assertValueEqual(target.getStoredAmount(), 5000L, "equal-volume target amount after transfer");
        helper.assertValueEqual(source.getPressurePa(), 5 * ATM, "equal-volume source pressure after transfer");
        helper.assertValueEqual(target.getPressurePa(), 5 * ATM, "equal-volume target pressure after transfer");
        helper.assertTrue(result.custodyAccountedFor(), "Equal-volume passive transfer lost custody of gas");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void differentVolumesUseVolumeWeightedEquilibrium(GameTestHelper helper) {
        GasTank source = tank(1000, 20, 10000);
        GasTank target = tank(3000, 20, 0);

        GasPressureTransferResult result = transfer(source, target, GasAction.EXECUTE);

        helper.assertValueEqual(result.filledAmount(), 7500L, "different-volume filled amount");
        helper.assertValueEqual(source.getStoredAmount(), 2500L, "different-volume source amount after transfer");
        helper.assertValueEqual(target.getStoredAmount(), 7500L, "different-volume target amount after transfer");
        helper.assertValueEqual(source.getPressurePa(), 250000L, "different-volume source pressure after transfer");
        helper.assertValueEqual(target.getPressurePa(), 250000L, "different-volume target pressure after transfer");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void targetPressureRatingStopsTransferBeforeEquilibrium(GameTestHelper helper) {
        GasTank source = tank(1000, 20, 10000);
        GasTank target = tank(1000, 3, 0);

        GasPressureTransferResult result = transfer(source, target, GasAction.EXECUTE);

        helper.assertValueEqual(result.filledAmount(), 3000L, "pressure-rated target filled amount");
        helper.assertValueEqual(source.getStoredAmount(), 7000L, "pressure-rated source amount after transfer");
        helper.assertValueEqual(target.getStoredAmount(), 3000L, "pressure-rated target amount after transfer");
        helper.assertValueEqual(target.getPressurePa(), 3 * ATM, "pressure-rated target pressure");
        helper.assertTrue(source.getPressurePa() > target.getPressurePa(), "Target pressure rating did not stop transfer before equalization");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void passiveTransferNeverFlowsUphill(GameTestHelper helper) {
        GasTank source = tank(1000, 20, 5000);
        GasTank target = tank(1000, 20, 8000);

        GasPressureTransferResult result = transfer(source, target, GasAction.EXECUTE);

        helper.assertValueEqual(result.transferableAmount(), 0L, "uphill transferable amount");
        helper.assertValueEqual(source.getStoredAmount(), 5000L, "uphill source amount");
        helper.assertValueEqual(target.getStoredAmount(), 8000L, "uphill target amount");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void simulateReportsTransferableAmountWithoutMutatingEndpoints(GameTestHelper helper) {
        GasTank source = tank(1000, 20, 10000);
        GasTank target = tank(1000, 20, 0);

        GasPressureTransferResult result = transfer(source, target, GasAction.SIMULATE);

        helper.assertTrue(!result.executed(), "SIMULATE passive transfer was reported as executed");
        helper.assertValueEqual(result.transferableAmount(), 5000L, "simulated transferable amount");
        helper.assertValueEqual(result.drainedAmount(), 0L, "simulated drained amount");
        helper.assertValueEqual(result.filledAmount(), 0L, "simulated filled amount");
        helper.assertValueEqual(source.getStoredAmount(), 10000L, "source amount after SIMULATE");
        helper.assertValueEqual(target.getStoredAmount(), 0L, "target amount after SIMULATE");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void sourceWithoutExactRecoveryFailsClosedBeforeDrain(GameTestHelper helper) {
        UnsafeRecoveryCompartment source = new UnsafeRecoveryCompartment(1000, 20 * ATM, 10000);
        GasTank target = tank(1000, 20, 0);

        GasPressureTransferResult result = GasPressureTransferService.transferPassive(GasPressureTransferEndpoint.compartment(source), GasPressureTransferEndpoint.compartment(target), Long.MAX_VALUE, GasAction.EXECUTE);

        helper.assertTrue(!result.executed(), "Unsafe source passive transfer was reported as executed");
        helper.assertValueEqual(result.transferableAmount(), 0L, "unsafe-source transferable amount");
        helper.assertValueEqual(result.drainedAmount(), 0L, "unsafe-source drained amount");
        helper.assertValueEqual(source.getStoredAmount(), 10000L, "unsafe-source amount after rejected transfer");
        helper.assertValueEqual(target.getStoredAmount(), 0L, "unsafe-source target amount after rejected transfer");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void executionUnderfillRestoresRemainderToSource(GameTestHelper helper) {
        GasTank source = tank(1000, 20, 10000);
        ExecuteUnderfillCompartment target = new ExecuteUnderfillCompartment(1000, 20 * ATM, 3000);

        GasPressureTransferResult result = GasPressureTransferService.transferPassive(GasPressureTransferEndpoint.compartment(source), GasPressureTransferEndpoint.compartment(target), Long.MAX_VALUE, GasAction.EXECUTE);

        helper.assertValueEqual(result.transferableAmount(), 5000L, "underfill planned transferable amount");
        helper.assertValueEqual(result.drainedAmount(), 5000L, "underfill drained amount");
        helper.assertValueEqual(result.filledAmount(), 3000L, "underfill filled amount");
        helper.assertValueEqual(result.restoredAmount(), 2000L, "underfill restored amount");
        helper.assertValueEqual(result.unrecoveredAmount(), 0L, "underfill unrecovered amount");
        helper.assertValueEqual(source.getStoredAmount(), 7000L, "source amount after underfill recovery");
        helper.assertValueEqual(target.getStoredAmount(), 3000L, "target amount after underfill");
        helper.assertTrue(result.custodyAccountedFor(), "Underfill recovery did not account for every drained GU");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void fixedPressureSourceCannotCompressVariableTargetAboveBoundaryPressure(GameTestHelper helper) {
        FixedSourceBoundary boundary = new FixedSourceBoundary(new GasStack(CCBGases.NATURAL_AIR.get(), Long.MAX_VALUE), ATM);
        GasTank target = tank(1000, 10, 0);

        GasPressureTransferResult result = GasPressureTransferService.transferPassive(GasPressureTransferEndpoint.pressureBoundary(boundary), GasPressureTransferEndpoint.compartment(target), Long.MAX_VALUE, GasAction.EXECUTE);

        helper.assertValueEqual(result.filledAmount(), 1000L, "fixed-boundary filled amount");
        helper.assertValueEqual(target.getStoredAmount(), 1000L, "fixed-boundary target amount");
        helper.assertValueEqual(target.getPressurePa(), ATM, "fixed-boundary target pressure");
        helper.assertTrue(target.getPressurePa() < target.getMaxPressurePa(), "One-atmosphere boundary incorrectly filled target to its pressure rating");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void endpointAdaptersSupportCanistersAndRejectPressurelessHandlers(GameTestHelper helper) {
        GasTank source = tank(1000, 20, 10000);
        TestCanister target = new TestCanister(1000, 20 * ATM);

        GasPressureTransferResult result = GasPressureTransferService.transferPassive(GasPressureTransferEndpoint.storage(source, 0), GasPressureTransferEndpoint.canister(target, 0), Long.MAX_VALUE, GasAction.EXECUTE);

        helper.assertValueEqual(result.filledAmount(), 5000L, "canister-adapter filled amount");
        helper.assertValueEqual(target.getGasInTank(0).getAmount(), 5000L, "canister-adapter target amount");
        helper.assertTrue(GasPressureTransferEndpoint.tryHandler(new PlainGasHandler(), 0).isEmpty(), "Pressureless GasHandler unexpectedly produced a passive-transfer endpoint");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void fixedCanisterAdapterKeepsSourcePressureConstant(GameTestHelper helper) {
        FixedTestCanister source = new FixedTestCanister(1000, 10 * ATM, new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        GasTank target = tank(1000, 20, 0);

        GasPressureTransferResult result = GasPressureTransferService.transferPassive(GasPressureTransferEndpoint.canister(source, 0), GasPressureTransferEndpoint.compartment(target), Long.MAX_VALUE, GasAction.EXECUTE);

        helper.assertValueEqual(result.filledAmount(), 10000L, "fixed-canister filled amount");
        helper.assertValueEqual(target.getStoredAmount(), 10000L, "fixed-canister target amount");
        helper.assertValueEqual(target.getPressurePa(), 10 * ATM, "fixed-canister target pressure");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void sameCompartmentIdentityCannotTransferToItself(GameTestHelper helper) {
        GasTank tank = tank(1000, 20, 10000);
        GasPressureTransferEndpoint firstView = GasPressureTransferEndpoint.compartment(tank);
        GasPressureTransferEndpoint secondView = GasPressureTransferEndpoint.storage(tank, 0);

        GasPressureTransferResult result = GasPressureTransferService.transferPassive(firstView, secondView, Long.MAX_VALUE, GasAction.EXECUTE);

        helper.assertValueEqual(result.transferableAmount(), 0L, "same-compartment transferable amount");
        helper.assertValueEqual(tank.getStoredAmount(), 10000L, "same-compartment stored amount");
        helper.succeed();
    }

    private static GasPressureTransferResult transfer(GasTank source, GasTank target, GasAction action) {
        return GasPressureTransferService.transferPassive(GasPressureTransferEndpoint.compartment(source), GasPressureTransferEndpoint.compartment(target), Long.MAX_VALUE, action);
    }

    private static GasTank tank(long volume, long maxPressureAtm, long amount) {
        GasTank tank = new GasTank(volume, maxPressureAtm * ATM);
        if (amount > 0) {
            long filled = tank.fill(new GasStack(CCBGases.NATURAL_AIR.get(), amount), GasAction.EXECUTE);
            if (filled != amount) {
                throw new IllegalStateException("Failed to initialize test gas storage: expected " + amount + " GU, filled " + filled + " GU.");
            }
        }
        return tank;
    }

    private static final class UnsafeRecoveryCompartment implements GasPressureCompartment {
        private final GasTank delegate;

        private UnsafeRecoveryCompartment(long volume, long maxPressurePa, long amount) {
            delegate = new GasTank(volume, maxPressurePa);
            long filled = delegate.fill(new GasStack(CCBGases.NATURAL_AIR.get(), amount), GasAction.EXECUTE);
            if (filled != amount) {
                throw new IllegalStateException("Failed to initialize test gas storage: expected " + amount + " GU, filled " + filled + " GU.");
            }
        }

        @Override
        public Object getCompartmentIdentity() {
            return this;
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return delegate.drain(resource, action);
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return delegate.drain(maxDrain, action);
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            return delegate.fill(resource, action);
        }

        @Override
        public boolean isGasValid(GasStack stack) {
            return delegate.isGasValid(stack);
        }

        @Override
        public GasStack getGasStack() {
            return delegate.getGasStack();
        }

        @Override
        public long getVolume() {
            return delegate.getVolume();
        }

        @Override
        public long getPressurePa() {
            return delegate.getPressurePa();
        }

        @Override
        public long getMaxPressurePa() {
            return delegate.getMaxPressurePa();
        }

        @Override
        public long getMaxAmount() {
            return delegate.getMaxAmount();
        }

        @Override
        public long getStoredAmount() {
            return delegate.getStoredAmount();
        }
    }

    private static final class ExecuteUnderfillCompartment implements GasPressureCompartment {
        private final GasTank delegate;
        private final long executeFillLimit;

        private ExecuteUnderfillCompartment(long volume, long maxPressurePa, long executeFillLimit) {
            delegate = new GasTank(volume, maxPressurePa);
            this.executeFillLimit = executeFillLimit;
        }

        @Override
        public Object getCompartmentIdentity() {
            return this;
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return delegate.drain(resource, action);
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return delegate.drain(maxDrain, action);
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            if (action.simulate()) {
                return delegate.fill(resource, action);
            }

            return delegate.fill(resource.copyWithAmount(Math.min(resource.getAmount(), executeFillLimit)), action);
        }

        @Override
        public boolean isGasValid(GasStack stack) {
            return delegate.isGasValid(stack);
        }

        @Override
        public GasStack getGasStack() {
            return delegate.getGasStack();
        }

        @Override
        public long getVolume() {
            return delegate.getVolume();
        }

        @Override
        public long getPressurePa() {
            return delegate.getPressurePa();
        }

        @Override
        public long getMaxPressurePa() {
            return delegate.getMaxPressurePa();
        }

        @Override
        public long getMaxAmount() {
            return delegate.getMaxAmount();
        }

        @Override
        public long getStoredAmount() {
            return delegate.getStoredAmount();
        }
    }

    private record FixedSourceBoundary(GasStack gas, long pressurePa) implements GasPressureBoundary {
        private FixedSourceBoundary(GasStack gas, long pressurePa) {
            this.gas = gas.copy();
            this.pressurePa = pressurePa;
        }

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return tank == 0 && !stack.isEmpty();
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            if (resource.isEmpty() || !GasStack.isSameGasSameComponents(resource, gas)) {
                return GasStack.EMPTY;
            }

            return resource.copy();
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            if (maxDrain <= 0) {
                return GasStack.EMPTY;
            }

            return gas.copyWithAmount(maxDrain);
        }

        @Override
        public GasStack getGasInTank(int tank) {
            if (tank != 0) {
                return GasStack.EMPTY;
            }

            return gas.copy();
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            return resource.getAmount();
        }

        @Override
        public long getDrainPressurePa(int tank, GasStack gas) {
            return pressurePa;
        }

        @Override
        public boolean supportsExactDrainRecovery(int tank) {
            return tank == 0;
        }
    }

    private static final class TestCanister implements GasCanisterContainer {
        private final GasTank tank;

        private TestCanister(long volume, long maxPressurePa) {
            tank = new GasTank(volume, maxPressurePa);
        }

        @Override
        public boolean isEmpty() {
            return tank.isEmpty();
        }

        @Override
        public boolean isFull() {
            return tank.getStoredAmount() >= tank.getMaxAmount();
        }

        @Override
        public boolean isGasValid(int tankIndex, GasStack stack) {
            return tankIndex == 0 && tank.isGasValid(stack);
        }

        @Override
        public GasStack drain(int tankIndex, GasStack resource, GasAction action) {
            if (tankIndex != 0) {
                return GasStack.EMPTY;
            }

            return tank.drain(resource, action);
        }

        @Override
        public GasStack drain(int tankIndex, long maxDrain, GasAction action) {
            if (tankIndex != 0) {
                return GasStack.EMPTY;
            }

            return tank.drain(maxDrain, action);
        }

        @Override
        public GasStack getGasInTank(int tankIndex) {
            if (tankIndex != 0) {
                return GasStack.EMPTY;
            }

            return tank.getGasStack();
        }

        @Override
        public int getPriority() {
            if (isEmpty()) {
                return EMPTY_CANISTER;
            }

            return NON_EMPTY_CANISTER;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public ItemStack getContainer() {
            return ItemStack.EMPTY;
        }

        @Override
        public List<ItemStack> createVirtualItems() {
            return List.of();
        }

        @Override
        public long fill(int tankIndex, GasStack resource, GasAction action) {
            if (tankIndex != 0) {
                return 0;
            }

            return tank.fill(resource, action);
        }

        @Override
        public long getTankVolume(int tankIndex) {
            if (tankIndex != 0) {
                return 0;
            }

            return tank.getVolume();
        }

        @Override
        public long getTankMaxPressurePa(int tankIndex) {
            if (tankIndex != 0) {
                return 0;
            }

            return tank.getMaxPressurePa();
        }

        @Override
        public void save() {
        }
    }

    private record FixedTestCanister(long volume, long maxPressurePa, GasStack gas) implements GasCanisterContainer {
        private FixedTestCanister(long volume, long maxPressurePa, GasStack gas) {
            this.volume = volume;
            this.maxPressurePa = maxPressurePa;
            this.gas = gas.copy();
        }

        @Override
        public boolean isEmpty() {
            return gas.isEmpty();
        }

        @Override
        public boolean isFull() {
            return !gas.isEmpty();
        }

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return tank == 0 && !stack.isEmpty();
        }

        @Override
        public GasStack drain(int tank, GasStack resource, GasAction action) {
            if (tank != 0 || resource.isEmpty() || !GasStack.isSameGasSameComponents(resource, gas)) {
                return GasStack.EMPTY;
            }

            return resource.copy();
        }

        @Override
        public GasStack drain(int tank, long maxDrain, GasAction action) {
            if (tank != 0 || maxDrain <= 0 || gas.isEmpty()) {
                return GasStack.EMPTY;
            }

            return gas.copyWithAmount(maxDrain);
        }

        @Override
        public GasStack getGasInTank(int tank) {
            if (!(tank == 0 && !gas.isEmpty())) {
                return GasStack.EMPTY;
            }

            return gas.copyWithAmount(getTankMaxAmount(0));
        }

        @Override
        public int getPriority() {
            return NON_EMPTY_CANISTER;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public ItemStack getContainer() {
            return ItemStack.EMPTY;
        }

        @Override
        public List<ItemStack> createVirtualItems() {
            return List.of();
        }

        @Override
        public long fill(int tank, GasStack resource, GasAction action) {
            return 0;
        }

        @Override
        public boolean supportsExactDrainRecovery(int tank) {
            return tank == 0;
        }

        @Override
        public long restoreDrainedGas(int tank, GasStack resource, GasAction action) {
            if (tank != 0 || resource.isEmpty()) {
                return 0;
            }

            return resource.getAmount();
        }

        @Override
        public long getTankVolume(int tank) {
            if (tank != 0) {
                return 0;
            }

            return volume;
        }

        @Override
        public long getTankMaxPressurePa(int tank) {
            if (tank != 0) {
                return 0;
            }

            return maxPressurePa;
        }

        @Override
        public PressureModel getTankPressureModel(int tank) {
            if (tank == 0) {
                return PressureModel.FIXED;
            }

            return PressureModel.VARIABLE;
        }

        @Override
        public void save() {
        }
    }

    private static final class PlainGasHandler implements GasHandler {
        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return tank == 0;
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return GasStack.EMPTY;
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return GasStack.EMPTY;
        }

        @Override
        public GasStack getGasInTank(int tank) {
            return GasStack.EMPTY;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            return resource.getAmount();
        }
    }
}
