package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineGeometry;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlock;
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
public final class GasTeslaTurbineDualNozzleUnderfillGameTest {
    private static final BlockPos SOURCE_TANK_POS = new BlockPos(3, 1, 0);
    private static final BlockPos TURBINE_POS = new BlockPos(3, 1, 3);
    private static final BlockPos CLOCKWISE_NOZZLE_POS = new BlockPos(5, 1, 4);
    private static final BlockPos COUNTER_CLOCKWISE_NOZZLE_POS = new BlockPos(1, 1, 4);
    private static final long PER_NOZZLE_AMOUNT = 2500;
    private static final long TOTAL_TRANSFER_AMOUNT = PER_NOZZLE_AMOUNT * 2;
    private static final int STRUCTURE_DEADLINE_TICKS = 10;

    private GasTeslaTurbineDualNozzleUnderfillGameTest() {
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 40)
    public static void mixedGasAcrossClockwiseAndCounterClockwiseNozzlesDoesNotUnderfillAfterSimulation(GameTestHelper helper) {
        helper.setBlock(SOURCE_TANK_POS, CCBBlocks.AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        helper.setBlock(TURBINE_POS, CCBBlocks.TESLA_TURBINE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.Y));

        AirtightTankBlockEntity sourceTank = sourceTank(helper);
        GasPressureCompartment sourceCompartment = sourceTank.getTankInventory();
        sourceTank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.ULTRAWARM_AIR.get(), TOTAL_TRANSFER_AMOUNT)).requireAccepted();
        helper.assertValueEqual(sourceCompartment.getStoredAmount(), TOTAL_TRANSFER_AMOUNT, "initial Ultrawarm Air source amount");

        int[] elapsedTicks = new int[1];
        helper.onEachTick(() -> {
            if (!turbineStructureReady(helper)) {
                elapsedTicks[0]++;
                if (elapsedTicks[0] > STRUCTURE_DEADLINE_TICKS) {
                    helper.fail("Tesla Turbine did not form its structural ring before the dual-nozzle underfill test");
                }
                return;
            }

            placeNozzles(helper);
            GasHandler clockwise = nozzleHandler(helper, CLOCKWISE_NOZZLE_POS, Direction.EAST, "clockwise");
            GasHandler counterClockwise = nozzleHandler(helper, COUNTER_CLOCKWISE_NOZZLE_POS, Direction.WEST, "counter-clockwise");
            helper.assertTrue(clockwise != counterClockwise, "Clockwise and counter-clockwise Tesla nozzles unexpectedly exposed the same handler object");

            GasStack initialGas = new GasStack(CCBGases.NATURAL_AIR.get(), 1);
            helper.assertValueEqual(clockwise.fill(initialGas, GasAction.EXECUTE), 1L, "Natural Air seed accepted by the Tesla Turbine");

            GasStack mixedGas = new GasStack(CCBGases.ULTRAWARM_AIR.get(), PER_NOZZLE_AMOUNT);
            helper.assertValueEqual(GasTransferExecutor.simulateFillAmount(clockwise, mixedGas, PER_NOZZLE_AMOUNT), PER_NOZZLE_AMOUNT, "clockwise Tesla nozzle simulated mixed-gas fill");
            helper.assertValueEqual(GasTransferExecutor.simulateFillAmount(counterClockwise, mixedGas, PER_NOZZLE_AMOUNT), PER_NOZZLE_AMOUNT, "counter-clockwise Tesla nozzle simulated mixed-gas fill");

            GasNetworkPressureEndpoint sourceEndpoint = sourceEndpoint(sourceCompartment);
            GasTransferPlan plan = new GasTransferPlan(List.of(new PlannedDrain(sourceEndpoint, TOTAL_TRANSFER_AMOUNT)), List.of(new PlannedFill(fillEndpoint(clockwise), PER_NOZZLE_AMOUNT, -1), new PlannedFill(fillEndpoint(counterClockwise), PER_NOZZLE_AMOUNT, -1)), TOTAL_TRANSFER_AMOUNT);

            PooledTransferExecutionResult execution = GasTransferExecutor.executePooledTransfer(new GasStack(CCBGases.ULTRAWARM_AIR.get(), 1), plan.executorDrains(), plan.executorFills());
            helper.assertValueEqual(execution.drainedAmounts().length, 1, "Tesla dual-nozzle executed drain result count");
            helper.assertValueEqual(execution.filledAmounts().length, 2, "Tesla dual-nozzle executed fill result count");
            helper.assertValueEqual(execution.drainedAmounts()[0], TOTAL_TRANSFER_AMOUNT, "Tesla dual-nozzle executed source drain amount");

            long remainderBeforeRecovery = execution.remainingGas().getAmount();
            plan.restoreRemainder(helper.getLevel(), new GasStack(CCBGases.ULTRAWARM_AIR.get(), 1), execution);

            long executedFillTotal = execution.filledAmounts()[0] + execution.filledAmounts()[1];
            helper.assertValueEqual(sourceCompartment.getStoredAmount() + executedFillTotal, TOTAL_TRANSFER_AMOUNT, "Tesla dual-nozzle source plus executed fills after remainder recovery");
            helper.assertValueEqual(remainderBeforeRecovery, 0L, "Tesla dual-nozzle mixed-gas transfer remainder after both nozzles simulated full acceptance");
            helper.assertValueEqual(execution.filledAmounts()[0], PER_NOZZLE_AMOUNT, "clockwise Tesla nozzle execute fill amount");
            helper.assertValueEqual(execution.filledAmounts()[1], PER_NOZZLE_AMOUNT, "counter-clockwise Tesla nozzle execute fill amount");
            helper.assertValueEqual(sourceCompartment.getStoredAmount(), 0L, "Ultrawarm Air source amount after both simulated Tesla fills executed fully");
            helper.succeed();
        });
    }

    private static AirtightTankBlockEntity sourceTank(GameTestHelper helper) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(SOURCE_TANK_POS));
        helper.assertTrue(blockEntity instanceof AirtightTankBlockEntity, "Airtight source tank block entity was not initialized for Tesla underfill regression");
        if (!(blockEntity instanceof AirtightTankBlockEntity tank)) {
            throw new IllegalStateException("Missing airtight source tank block entity at " + SOURCE_TANK_POS + '.');
        }

        return tank;
    }

    private static boolean turbineStructureReady(GameTestHelper helper) {
        BlockPos absoluteTurbinePos = helper.absolutePos(TURBINE_POS);
        for (int u = -1; u <= 1; u++) {
            for (int v = -1; v <= 1; v++) {
                if (u == 0 && v == 0) {
                    continue;
                }

                BlockPos structuralPos = TeslaTurbineGeometry.calculateStructurePos(absoluteTurbinePos, Axis.Y, u, v);
                if (!(helper.getLevel().getBlockState(structuralPos).getBlock() instanceof TeslaTurbineStructuralBlock)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static void placeNozzles(GameTestHelper helper) {
        helper.setBlock(CLOCKWISE_NOZZLE_POS, CCBBlocks.TESLA_TURBINE_NOZZLE_BLOCK.get().defaultBlockState().setValue(TeslaTurbineNozzleBlock.FACING, Direction.EAST).setValue(TeslaTurbineNozzleBlock.CLOCKWISE, true));
        helper.setBlock(COUNTER_CLOCKWISE_NOZZLE_POS, CCBBlocks.TESLA_TURBINE_NOZZLE_BLOCK.get().defaultBlockState().setValue(TeslaTurbineNozzleBlock.FACING, Direction.WEST).setValue(TeslaTurbineNozzleBlock.CLOCKWISE, false));
    }

    private static GasHandler nozzleHandler(GameTestHelper helper, BlockPos nozzlePos, Direction accessDirection, String description) {
        GasHandler handler = helper.getLevel().getCapability(GasCapabilities.BLOCK, helper.absolutePos(nozzlePos), accessDirection);
        helper.assertTrue(handler != null, "Tesla Turbine " + description + " nozzle did not expose its production gas capability");
        if (handler == null) {
            throw new NullPointerException("Tesla Turbine " + description + " nozzle did not expose its production gas capability" + '.');
        }

        return handler;
    }

    private static GasNetworkPressureEndpoint sourceEndpoint(GasPressureCompartment source) {
        return new GasNetworkPressureEndpoint(
            new TransferAccess(source, null, List.of(), List.of()),
            new PressureState(source.getPressurePa(), false, source.getStoredAmount(), source.getVolume(), source.getMaxPressurePa(), source.getMaxAmount()),
            new TransferLimits(TOTAL_TRANSFER_AMOUNT, 0),
            new Recovery(List.of(source), null, null, false));
    }

    private static GasNetworkPressureEndpoint fillEndpoint(GasHandler handler) {
        return new GasNetworkPressureEndpoint(
            new TransferAccess(null, handler, List.of(), List.of()),
            new PressureState(GasPressure.REFERENCE_PRESSURE_PA, true, 0, 1, GasPressure.REFERENCE_PRESSURE_PA, Long.MAX_VALUE),
            new TransferLimits(0, PER_NOZZLE_AMOUNT),
            Recovery.NONE);
    }
}
