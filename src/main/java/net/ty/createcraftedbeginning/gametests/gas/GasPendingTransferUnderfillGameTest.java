package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.PressureState;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.Recovery;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.TransferAccess;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.TransferLimits;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor.PooledTransferExecutionResult;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan.PlannedDrain;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan.PlannedFill;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasPendingTransferUnderfillGameTest {
    private static final BlockPos RECOVERY_PIPE_POS = new BlockPos(1, 1, 1);
    private static final long INITIAL_SOURCE_AMOUNT = 5000;
    private static final long TRANSFER_AMOUNT = 2500;
    private static final String PENDING_TRANSFER_KEY = "PendingTransfer";
    private static final String PENDING_TRANSFER_ORIGIN_KEY = "PendingTransferOrigin";
    private static final String EXTERNAL_HANDLER_ORIGIN = "external_handler";

    private GasPendingTransferUnderfillGameTest() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void executeUnderfillUsesExactPressureBoundaryRecovery(GameTestHelper helper) {
        GasStack naturalAir = new GasStack(CCBGases.NATURAL_AIR.get(), 1);
        ExactRecoveryBoundarySource source = new ExactRecoveryBoundarySource(INITIAL_SOURCE_AMOUNT);
        SimulateAcceptExecuteRejectSinkHandler sink = new SimulateAcceptExecuteRejectSinkHandler();
        GasNetworkPressureEndpoint sourceEndpoint = new GasNetworkPressureEndpoint(
            new TransferAccess(source, null, List.of(), List.of()),
            new PressureState(GasPressure.pascals(2), true, 0, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE),
            new TransferLimits(TRANSFER_AMOUNT, 0),
            Recovery.NONE);
        GasTransferPlan plan = new GasTransferPlan(
            List.of(new PlannedDrain(sourceEndpoint, TRANSFER_AMOUNT)),
            List.of(new PlannedFill(sinkEndpoint(sink), TRANSFER_AMOUNT, -1)),
            TRANSFER_AMOUNT);

        PooledTransferExecutionResult execution = GasTransferExecutor.executePooledTransfer(naturalAir, plan.executorDrains(), plan.executorFills());
        helper.assertValueEqual(source.storedAmount(), INITIAL_SOURCE_AMOUNT - TRANSFER_AMOUNT, "pressure-boundary source amount after execute drain");
        helper.assertValueEqual(execution.remainingGas().getAmount(), TRANSFER_AMOUNT, "pressure-boundary execute underfill remainder amount");

        plan.restoreRemainder(helper.getLevel(), naturalAir, execution);

        helper.assertValueEqual(source.restoreExecuteCalls(), 1, "pressure-boundary exact recovery call count");
        helper.assertValueEqual(source.storedAmount(), INITIAL_SOURCE_AMOUNT, "pressure-boundary source amount after exact remainder recovery");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 40)
    public static void executeUnderfillAutomaticallyRetainsPendingTransferAndConservesGas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(RECOVERY_PIPE_POS, CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().defaultBlockState());
        BlockPos absoluteRecoveryPos = helper.absolutePos(RECOVERY_PIPE_POS);
        BlockEntity recoveryBlockEntity = level.getBlockEntity(absoluteRecoveryPos);
        helper.assertTrue(recoveryBlockEntity != null, "Airtight encased pipe block entity was not initialized for pending-transfer custody");
        if (recoveryBlockEntity == null) {
            throw new NullPointerException("Airtight encased pipe block entity was not initialized for pending-transfer custody.");
        }

        GasStack naturalAir = new GasStack(CCBGases.NATURAL_AIR.get(), 1);
        RejectingRestoreSourceHandler source = new RejectingRestoreSourceHandler(INITIAL_SOURCE_AMOUNT);
        SimulateAcceptExecuteRejectSinkHandler sink = new SimulateAcceptExecuteRejectSinkHandler();
        GasPipeConnection recoveryConnection = new GasPipeConnection(Direction.WEST);

        helper.assertValueEqual(GasTransferExecutor.simulateDrainAmount(source, naturalAir, TRANSFER_AMOUNT), TRANSFER_AMOUNT, "fault source simulated drain amount");
        helper.assertValueEqual(GasTransferExecutor.simulateFillAmount(sink, naturalAir, TRANSFER_AMOUNT), TRANSFER_AMOUNT, "fault sink simulated fill amount");
        helper.assertValueEqual(source.storedAmount(), INITIAL_SOURCE_AMOUNT, "source amount after drain simulation");
        helper.assertValueEqual(SimulateAcceptExecuteRejectSinkHandler.storedAmount(), 0L, "sink amount after fill simulation");

        GasNetworkPressureEndpoint sourceEndpoint = sourceEndpoint(source, recoveryConnection, absoluteRecoveryPos);
        GasNetworkPressureEndpoint sinkEndpoint = sinkEndpoint(sink);
        GasTransferPlan plan = new GasTransferPlan(List.of(new PlannedDrain(sourceEndpoint, TRANSFER_AMOUNT)), List.of(new PlannedFill(sinkEndpoint, TRANSFER_AMOUNT, -1)), TRANSFER_AMOUNT);
        helper.assertTrue(!plan.isEmpty(), "Fault-injection transfer plan was unexpectedly empty");

        PooledTransferExecutionResult execution = GasTransferExecutor.executePooledTransfer(naturalAir, plan.executorDrains(), plan.executorFills());
        helper.assertValueEqual(execution.drainedAmounts().length, 1, "executed drain result count");
        helper.assertValueEqual(execution.filledAmounts().length, 1, "executed fill result count");
        helper.assertValueEqual(execution.drainedAmounts()[0], TRANSFER_AMOUNT, "executed source drain amount");
        helper.assertValueEqual(execution.filledAmounts()[0], 0L, "underfilled sink execute amount");
        helper.assertTrue(execution.remainingGas().is(CCBGases.NATURAL_AIR.get()), "execute underfill remainder was not Natural Air");
        helper.assertValueEqual(execution.remainingGas().getAmount(), TRANSFER_AMOUNT, "execute underfill remainder amount");
        helper.assertValueEqual(source.storedAmount(), INITIAL_SOURCE_AMOUNT - TRANSFER_AMOUNT, "source amount after successful execute drain");
        helper.assertValueEqual(SimulateAcceptExecuteRejectSinkHandler.storedAmount(), 0L, "sink amount after rejected execute fill");
        helper.assertValueEqual(source.storedAmount() + SimulateAcceptExecuteRejectSinkHandler.storedAmount() + execution.remainingGas().getAmount(), INITIAL_SOURCE_AMOUNT, "gas conservation while execute remainder is in transaction custody");
        helper.assertValueEqual(source.executeDrainCalls(), 1, "source execute drain call count");
        helper.assertValueEqual(sink.executeFillCalls(), 1, "sink execute fill call count");

        plan.restoreRemainder(level, naturalAir, execution);

        helper.assertValueEqual(source.restoreExecuteCalls(), 1, "source execute restore rejection call count");
        helper.assertValueEqual(source.storedAmount(), INITIAL_SOURCE_AMOUNT - TRANSFER_AMOUNT, "source amount after rejected direct remainder restore");
        helper.assertValueEqual(SimulateAcceptExecuteRejectSinkHandler.storedAmount(), 0L, "sink amount after remainder recovery");
        helper.assertTrue(recoveryConnection.hasPendingTransfer(), "Execute underfill remainder was not automatically retained by the recovery connection");

        GasStack pending = pendingTransfer(helper, recoveryConnection);
        helper.assertTrue(pending.is(CCBGases.NATURAL_AIR.get()), "Pending custody gas was not Natural Air");
        helper.assertValueEqual(pending.getAmount(), TRANSFER_AMOUNT, "pending custody amount after execute underfill");
        helper.assertValueEqual(source.storedAmount() + SimulateAcceptExecuteRejectSinkHandler.storedAmount() + pending.getAmount(), INITIAL_SOURCE_AMOUNT, "gas conservation after remainder moved into pending connection custody");
        helper.succeed();
    }

    private static GasNetworkPressureEndpoint sourceEndpoint(GasHandler source, GasPipeConnection recoveryConnection, BlockPos recoveryPipePos) {
        return new GasNetworkPressureEndpoint(
            new TransferAccess(source, null, List.of(), List.of()),
            new PressureState(GasPressure.pascals(2), false, INITIAL_SOURCE_AMOUNT, INITIAL_SOURCE_AMOUNT, GasPressure.pascals(3), INITIAL_SOURCE_AMOUNT),
            new TransferLimits(TRANSFER_AMOUNT, 0),
            new Recovery(List.of(), recoveryConnection, recoveryPipePos, false));
    }

    private static GasNetworkPressureEndpoint sinkEndpoint(GasHandler sink) {
        return new GasNetworkPressureEndpoint(
            new TransferAccess(null, sink, List.of(), List.of()),
            new PressureState(GasPressure.REFERENCE_PRESSURE_PA, false, 0, INITIAL_SOURCE_AMOUNT, GasPressure.pascals(3), INITIAL_SOURCE_AMOUNT),
            new TransferLimits(0, TRANSFER_AMOUNT),
            Recovery.NONE);
    }

    private static GasStack pendingTransfer(GameTestHelper helper, GasPipeConnection connection) {
        ServerLevel level = helper.getLevel();
        CompoundTag serialized = new CompoundTag();
        connection.write(serialized, level.registryAccess(), false);
        CompoundTag westData = NbtValues.getCompoundOrEmpty(serialized, Direction.WEST.getName());
        helper.assertTrue(westData.contains(PENDING_TRANSFER_KEY, Tag.TAG_COMPOUND), "Pending connection did not serialize its retained transfer");
        helper.assertTrue(EXTERNAL_HANDLER_ORIGIN.equals(NbtValues.getStringOrDefault(westData, PENDING_TRANSFER_ORIGIN_KEY, "")), "Automatically retained pending transfer did not use external-handler recovery origin");
        return GasStack.parseOptional(level.registryAccess(), NbtValues.getCompoundOrEmpty(westData, PENDING_TRANSFER_KEY));
    }

    private static final class ExactRecoveryBoundarySource implements GasPressureBoundary {
        private long storedAmount;
        private int restoreExecuteCalls;

        private ExactRecoveryBoundarySource(long storedAmount) {
            this.storedAmount = storedAmount;
        }

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return tank == 0 && stack.is(CCBGases.NATURAL_AIR.get());
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            if (resource.isEmpty() || !resource.is(CCBGases.NATURAL_AIR.get())) {
                return GasStack.EMPTY;
            }

            return drain(resource.getAmount(), action);
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            long drainedAmount = Mth.clamp(maxDrain, 0, storedAmount);
            if (drainedAmount <= 0) {
                return GasStack.EMPTY;
            }

            if (action.execute()) {
                storedAmount -= drainedAmount;
            }
            return new GasStack(CCBGases.NATURAL_AIR.get(), drainedAmount);
        }

        @Override
        public GasStack getGasInTank(int tank) {
            if (tank != 0 || storedAmount <= 0) {
                return GasStack.EMPTY;
            }

            return new GasStack(CCBGases.NATURAL_AIR.get(), storedAmount);
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
        public boolean supportsExactDrainRecovery(int tank) {
            return tank == 0;
        }

        @Override
        public long restoreDrainedGas(int tank, GasStack resource, GasAction action) {
            if (tank != 0 || resource.isEmpty() || !resource.is(CCBGases.NATURAL_AIR.get())) {
                return 0;
            }

            long restoredAmount = resource.getAmount();
            if (action.execute()) {
                storedAmount = BoundedMath.saturatedAdd(storedAmount, restoredAmount);
                restoreExecuteCalls++;
            }
            return restoredAmount;
        }

        private long storedAmount() {
            return storedAmount;
        }

        private int restoreExecuteCalls() {
            return restoreExecuteCalls;
        }
    }

    private static final class RejectingRestoreSourceHandler implements GasHandler {
        private long storedAmount;
        private int executeDrainCalls;
        private int restoreExecuteCalls;

        private RejectingRestoreSourceHandler(long storedAmount) {
            this.storedAmount = storedAmount;
        }

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return tank == 0 && stack.is(CCBGases.NATURAL_AIR.get());
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            if (resource.isEmpty() || !resource.is(CCBGases.NATURAL_AIR.get())) {
                return GasStack.EMPTY;
            }

            return drainNaturalAir(resource.getAmount(), action);
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return drainNaturalAir(maxDrain, action);
        }

        @Override
        public GasStack getGasInTank(int tank) {
            if (tank != 0 || storedAmount <= 0) {
                return GasStack.EMPTY;
            }

            return new GasStack(CCBGases.NATURAL_AIR.get(), storedAmount);
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            if (action.execute() && !resource.isEmpty()) {
                restoreExecuteCalls++;
            }
            return 0;
        }

        private GasStack drainNaturalAir(long requestedAmount, GasAction action) {
            long drained = Mth.clamp(requestedAmount, 0, storedAmount);
            if (drained <= 0) {
                return GasStack.EMPTY;
            }

            if (action.execute()) {
                storedAmount -= drained;
                executeDrainCalls++;
            }
            return new GasStack(CCBGases.NATURAL_AIR.get(), drained);
        }

        private long storedAmount() {
            return storedAmount;
        }

        private int executeDrainCalls() {
            return executeDrainCalls;
        }

        private int restoreExecuteCalls() {
            return restoreExecuteCalls;
        }
    }

    private static final class SimulateAcceptExecuteRejectSinkHandler implements GasHandler {
        private int executeFillCalls;

        private static long storedAmount() {
            return 0;
        }

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return tank == 0 && stack.is(CCBGases.NATURAL_AIR.get());
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
            if (resource.isEmpty() || !resource.is(CCBGases.NATURAL_AIR.get())) {
                return 0;
            }

            if (action.simulate()) {
                return resource.getAmount();
            }

            executeFillCalls++;
            return 0;
        }

        private int executeFillCalls() {
            return executeFillCalls;
        }
    }
}
