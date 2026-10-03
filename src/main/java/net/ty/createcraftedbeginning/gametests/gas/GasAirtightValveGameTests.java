package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightvalve.AirtightValveBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightvalve.AirtightValveBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowDirection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowState;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasAirtightValveGameTests {
    private static final BlockPos VALVE_POS = new BlockPos(1, 1, 1);
    private static final BlockPos WEST_POS = VALVE_POS.relative(Direction.WEST);
    private static final BlockPos EAST_POS = VALVE_POS.relative(Direction.EAST);
    private static final BlockPos MOTOR_POS = VALVE_POS.relative(Direction.SOUTH);

    private static final long HIGH_PRESSURE_PA = 2 * GasPressure.REFERENCE_PRESSURE_PA;
    private static final int TEST_MOTOR_SPEED = 32;
    private static final float SPEED_EPSILON = 0.01F;
    private static final int KINETIC_DEADLINE_TICKS = 30;

    private GasAirtightValveGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 80)
    public static void openValveAllowsWestToEastFlow(GameTestHelper helper) {
        placeSource(helper, WEST_POS);
        placeValve(helper, true);

        helper.succeedWhen(() -> {
            GasTransportBehaviour transport = transport(helper);
            BlockState state = valveState(helper);
            assertOpenDirectionality(helper, transport, state);
            assertFlow(helper, transport, Direction.WEST, Direction.EAST, "open west-to-east airtight valve");
            assertNoOffAxisFlow(helper, transport, "open west-to-east airtight valve");
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 80)
    public static void openValveAllowsEastToWestFlow(GameTestHelper helper) {
        placeSource(helper, EAST_POS);
        placeValve(helper, true);

        helper.succeedWhen(() -> {
            GasTransportBehaviour transport = transport(helper);
            BlockState state = valveState(helper);
            assertOpenDirectionality(helper, transport, state);
            assertFlow(helper, transport, Direction.EAST, Direction.WEST, "open east-to-west airtight valve");
            assertNoOffAxisFlow(helper, transport, "open east-to-west airtight valve");
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 80)
    public static void closedValveBlocksPressureDrivenFlow(GameTestHelper helper) {
        placeSource(helper, WEST_POS);
        placeValve(helper, false);
        GasTransportBehaviour transport = transport(helper);

        int[] ticks = new int[1];
        helper.onEachTick(() -> {
            ticks[0]++;
            if (ticks[0] < 20) {
                return;
            }

            BlockState state = valveState(helper);
            assertClosedDirectionality(helper, transport, state);
            helper.assertValueEqual(transport.getThroughputFlowRate(), 0L, "closed airtight valve throughput");
            helper.assertTrue(transport.getFlowState(Direction.WEST) == null, "Closed airtight valve reported west-face flow");
            helper.assertTrue(transport.getFlowState(Direction.EAST) == null, "Closed airtight valve reported east-face flow");
            assertNoOffAxisFlow(helper, transport, "closed airtight valve");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 140)
    public static void positiveKineticSpeedOpensValveAndEnablesFlow(GameTestHelper helper) {
        placeSource(helper, WEST_POS);
        AirtightValveBlockEntity valve = placeValve(helper, false);
        ValveMotorController motor = placeMotor(helper, valve);
        GasTransportBehaviour transport = transport(helper);

        int[] phase = new int[1];
        int[] phaseTicks = new int[1];
        helper.onEachTick(() -> {
            phaseTicks[0]++;

            if (phase[0] == 0) {
                helper.assertTrue(!valve.isOpen(), "Airtight valve opened before positive kinetic speed was requested");
                if (!motor.calibrate()) {
                    failAfterDeadline(helper, phaseTicks[0], "Airtight valve never received kinetic speed while calibrating its test motor");
                    return;
                }

                motor.drivePositive();
                phase[0] = 1;
                phaseTicks[0] = 0;
                return;
            }

            if (phase[0] == 1) {
                if (!motor.hasPositiveValveSpeed()) {
                    failAfterDeadline(helper, phaseTicks[0], "Airtight valve never received positive kinetic speed");
                    return;
                }

                assertTestSpeed(helper, valve, "opening airtight valve");
                if (!valve.isOpen()) {
                    helper.assertValueEqual(transport.getThroughputFlowRate(), 0L, "airtight valve throughput while opening but not yet open");
                    return;
                }

                phase[0] = 2;
                phaseTicks[0] = 0;
                return;
            }

            BlockState state = valveState(helper);
            assertOpenDirectionality(helper, transport, state);
            if (!hasPositiveFlow(transport)) {
                failAfterDeadline(helper, phaseTicks[0], "Airtight valve opened but never enabled west-to-east gas flow");
                return;
            }

            assertFlow(helper, transport, Direction.WEST, Direction.EAST, "kinetically opened airtight valve");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 140)
    public static void negativeKineticSpeedClosesValveAndClearsFlow(GameTestHelper helper) {
        placeSource(helper, WEST_POS);
        AirtightValveBlockEntity valve = placeValve(helper, true);
        ValveMotorController motor = placeMotor(helper, valve);
        GasTransportBehaviour transport = transport(helper);

        int[] phase = new int[1];
        int[] phaseTicks = new int[1];
        helper.onEachTick(() -> {
            phaseTicks[0]++;

            if (phase[0] == 0) {
                if (!hasPositiveFlow(transport)) {
                    failAfterDeadline(helper, phaseTicks[0], "Open airtight valve never established its initial west-to-east flow");
                    return;
                }

                phase[0] = 1;
                phaseTicks[0] = 0;
                return;
            }

            if (phase[0] == 1) {
                if (!motor.calibrate()) {
                    failAfterDeadline(helper, phaseTicks[0], "Airtight valve never received kinetic speed while calibrating its test motor");
                    return;
                }

                motor.driveNegative();
                phase[0] = 2;
                phaseTicks[0] = 0;
                return;
            }

            if (phase[0] == 2) {
                if (!motor.hasNegativeValveSpeed()) {
                    failAfterDeadline(helper, phaseTicks[0], "Airtight valve never received negative kinetic speed");
                    return;
                }

                assertTestSpeed(helper, valve, "closing airtight valve");
                if (valve.isOpen()) {
                    return;
                }

                phase[0] = 3;
                phaseTicks[0] = 0;
                return;
            }

            BlockState state = valveState(helper);
            assertClosedDirectionality(helper, transport, state);
            boolean flowCleared = transport.getFlowState(Direction.WEST) == null && transport.getFlowState(Direction.EAST) == null && transport.getThroughputFlowRate() == 0;
            if (!flowCleared) {
                failAfterDeadline(helper, phaseTicks[0], "Airtight valve closed but stale gas flow remained in its transport telemetry");
                return;
            }

            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 180)
    public static void stoppingMidOpeningPreservesPartialTravel(GameTestHelper helper) {
        AirtightValveBlockEntity valve = placeValve(helper, false);
        ValveMotorController motor = placeMotor(helper, valve);

        int[] phase = new int[1];
        int[] phaseTicks = new int[1];
        int[] movingTicks = new int[1];
        int[] stoppedTicks = new int[1];
        helper.onEachTick(() -> {
            phaseTicks[0]++;

            if (phase[0] == 0) {
                if (!motor.calibrate()) {
                    failAfterDeadline(helper, phaseTicks[0], "Airtight valve never received kinetic speed while calibrating its test motor");
                    return;
                }

                motor.driveNegative();
                phase[0] = 1;
                phaseTicks[0] = 0;
                return;
            }

            if (phase[0] == 1) {
                helper.assertTrue(!valve.isOpen(), "Airtight valve opened while normalizing toward its closed endpoint");
                if (!motor.hasNegativeValveSpeed()) {
                    failAfterDeadline(helper, phaseTicks[0], "Airtight valve never received negative speed while normalizing closed");
                    return;
                }

                assertTestSpeed(helper, valve, "closed-endpoint normalization");
                movingTicks[0]++;
                if (movingTicks[0] < 12) {
                    return;
                }

                motor.stop();
                phase[0] = 2;
                phaseTicks[0] = 0;
                movingTicks[0] = 0;
                return;
            }

            if (phase[0] == 2) {
                helper.assertTrue(!valve.isOpen(), "Airtight valve opened while stopped at its closed endpoint");
                if (!motor.isValveStopped()) {
                    failAfterDeadline(helper, phaseTicks[0], "Airtight valve did not stop before the partial-opening phase");
                    return;
                }

                motor.drivePositive();
                phase[0] = 3;
                phaseTicks[0] = 0;
                return;
            }

            if (phase[0] == 3) {
                if (!motor.hasPositiveValveSpeed()) {
                    failAfterDeadline(helper, phaseTicks[0], "Airtight valve never started its partial opening travel");
                    return;
                }

                assertTestSpeed(helper, valve, "partial opening travel");
                movingTicks[0]++;
                helper.assertTrue(!valve.isOpen(), "Airtight valve became logically open before its partial-opening stop point");
                if (movingTicks[0] < 3) {
                    return;
                }

                motor.stop();
                phase[0] = 4;
                phaseTicks[0] = 0;
                movingTicks[0] = 0;
                return;
            }

            if (phase[0] == 4) {
                helper.assertTrue(!valve.isOpen(), "Airtight valve became logically open while stopped mid-opening");
                if (!motor.isValveStopped()) {
                    failAfterDeadline(helper, phaseTicks[0], "Airtight valve did not stop during its partial opening travel");
                    return;
                }

                stoppedTicks[0]++;
                if (stoppedTicks[0] < 5) {
                    return;
                }

                motor.drivePositive();
                phase[0] = 5;
                phaseTicks[0] = 0;
                return;
            }

            if (!motor.hasPositiveValveSpeed()) {
                failAfterDeadline(helper, phaseTicks[0], "Airtight valve did not resume positive speed after a mid-opening stop");
                return;
            }

            assertTestSpeed(helper, valve, "resumed opening travel");
            movingTicks[0]++;
            if (valve.isOpen()) {
                helper.assertTrue(movingTicks[0] <= 8, "Airtight valve lost its partial travel position while stopped and required a full reopening stroke");
                helper.succeed();
                return;
            }

            if (movingTicks[0] > 8) {
                helper.fail("Airtight valve did not preserve enough partial opening travel to reach the open endpoint within the expected resumed stroke");
            }
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 160)
    public static void reversingMidClosingDoesNotPrematurelyCloseValve(GameTestHelper helper) {
        AirtightValveBlockEntity valve = placeValve(helper, true);
        ValveMotorController motor = placeMotor(helper, valve);

        int[] phase = new int[1];
        int[] phaseTicks = new int[1];
        int[] movingTicks = new int[1];
        helper.onEachTick(() -> {
            phaseTicks[0]++;

            if (phase[0] == 0) {
                if (!motor.calibrate()) {
                    failAfterDeadline(helper, phaseTicks[0], "Airtight valve never received kinetic speed while calibrating its test motor");
                    return;
                }

                motor.drivePositive();
                phase[0] = 1;
                phaseTicks[0] = 0;
                return;
            }

            if (phase[0] == 1) {
                helper.assertTrue(valve.isOpen(), "Airtight valve closed while normalizing toward its open endpoint");
                if (!motor.hasPositiveValveSpeed()) {
                    failAfterDeadline(helper, phaseTicks[0], "Airtight valve never received positive speed while normalizing open");
                    return;
                }

                assertTestSpeed(helper, valve, "open-endpoint normalization");
                movingTicks[0]++;
                if (movingTicks[0] < 12) {
                    return;
                }

                motor.driveNegative();
                phase[0] = 2;
                phaseTicks[0] = 0;
                movingTicks[0] = 0;
                return;
            }

            if (phase[0] == 2) {
                helper.assertTrue(valve.isOpen(), "Airtight valve became logically closed before reaching the closed endpoint");
                if (!motor.hasNegativeValveSpeed()) {
                    failAfterDeadline(helper, phaseTicks[0], "Airtight valve never started its partial closing travel");
                    return;
                }

                assertTestSpeed(helper, valve, "partial closing travel");
                movingTicks[0]++;
                if (movingTicks[0] < 2) {
                    return;
                }

                motor.drivePositive();
                phase[0] = 3;
                phaseTicks[0] = 0;
                movingTicks[0] = 0;
                return;
            }

            helper.assertTrue(valve.isOpen(), "Airtight valve toggled closed while a partial closing stroke was being reversed");
            if (!motor.hasPositiveValveSpeed()) {
                failAfterDeadline(helper, phaseTicks[0], "Airtight valve never reversed back to positive speed during its partial closing stroke");
                return;
            }

            assertTestSpeed(helper, valve, "reversed closing travel");
            movingTicks[0]++;
            if (movingTicks[0] < 5) {
                return;
            }

            helper.assertTrue(valve.isOpen(), "Airtight valve did not remain open after reversing a partial closing stroke");
            helper.succeed();
        });
    }

    private static AirtightValveBlockEntity placeValve(GameTestHelper helper, boolean open) {
        BlockState state = CCBBlocks.AIRTIGHT_VALVE_BLOCK.get().defaultBlockState().setValue(AirtightValveBlock.AXIS, Axis.X).setValue(AirtightValveBlock.OPEN, open);
        helper.setBlock(VALVE_POS, state);

        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(VALVE_POS));
        helper.assertTrue(blockEntity instanceof AirtightValveBlockEntity, "Airtight valve block entity was not initialized");
        if (!(blockEntity instanceof AirtightValveBlockEntity valve)) {
            throw new IllegalStateException("Airtight valve block entity was not initialized at " + VALVE_POS + '.');
        }

        return valve;
    }

    private static ValveMotorController placeMotor(GameTestHelper helper, AirtightValveBlockEntity valve) {
        helper.setBlock(MOTOR_POS, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.NORTH));
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(MOTOR_POS));
        helper.assertTrue(blockEntity instanceof CreativeMotorBlockEntity, "Creative motor block entity was not initialized");
        if (!(blockEntity instanceof CreativeMotorBlockEntity motor)) {
            throw new IllegalStateException("Creative motor block entity was not initialized at " + MOTOR_POS + '.');
        }

        return new ValveMotorController(motor, valve);
    }

    private static void placeSource(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof CreativeAirtightTankBlockEntity, "Creative airtight tank block entity was not initialized at " + pos);
        if (!(blockEntity instanceof CreativeAirtightTankBlockEntity sourceTank)) {
            throw new IllegalStateException("Creative airtight tank block entity was not initialized at " + pos + '.');
        }

        sourceTank.getTankInventory().setFixedPressurePa(HIGH_PRESSURE_PA);
        sourceTank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        helper.assertValueEqual(sourceTank.getTankInventory().getPressurePa(), HIGH_PRESSURE_PA, "airtight valve source pressure at " + pos);
    }

    private static BlockState valveState(GameTestHelper helper) {
        return helper.getLevel().getBlockState(helper.absolutePos(VALVE_POS));
    }

    private static GasTransportBehaviour transport(GameTestHelper helper) {
        GasTransportBehaviour transport = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(VALVE_POS), GasTransportBehaviour.TYPE);
        helper.assertTrue(transport != null, "Airtight valve transport behaviour was not initialized");
        if (transport == null) {
            throw new NullPointerException("Airtight valve transport behaviour was not initialized.");
        }

        return transport;
    }

    private static void assertOpenDirectionality(GameTestHelper helper, GasTransportBehaviour transport, BlockState state) {
        helper.assertTrue(state.getValue(AirtightValveBlock.OPEN), "Airtight valve state was not open");
        helper.assertTrue(transport.allowsInboundFlow(state, Direction.WEST), "Open airtight valve did not allow inbound gas on its west face");
        helper.assertTrue(transport.allowsOutboundFlow(state, Direction.WEST), "Open airtight valve did not allow outbound gas on its west face");
        helper.assertTrue(transport.allowsInboundFlow(state, Direction.EAST), "Open airtight valve did not allow inbound gas on its east face");
        helper.assertTrue(transport.allowsOutboundFlow(state, Direction.EAST), "Open airtight valve did not allow outbound gas on its east face");
    }

    private static void assertClosedDirectionality(GameTestHelper helper, GasTransportBehaviour transport, BlockState state) {
        helper.assertTrue(!state.getValue(AirtightValveBlock.OPEN), "Airtight valve state was not closed");
        helper.assertTrue(!transport.allowsInboundFlow(state, Direction.WEST), "Closed airtight valve allowed inbound gas on its west face");
        helper.assertTrue(!transport.allowsOutboundFlow(state, Direction.WEST), "Closed airtight valve allowed outbound gas on its west face");
        helper.assertTrue(!transport.allowsInboundFlow(state, Direction.EAST), "Closed airtight valve allowed inbound gas on its east face");
        helper.assertTrue(!transport.allowsOutboundFlow(state, Direction.EAST), "Closed airtight valve allowed outbound gas on its east face");
    }

    private static void assertFlow(GameTestHelper helper, GasTransportBehaviour transport, Direction inletFace, Direction outletFace, String stage) {
        FlowState inlet = transport.getFlowState(inletFace);
        FlowState outlet = transport.getFlowState(outletFace);
        helper.assertTrue(inlet != null, stage + " did not have inlet-face flow");
        if (inlet == null) {
            throw new NullPointerException("Required test object is missing: " + stage + " did not have inlet-face flow" + '.');
        }

        helper.assertTrue(outlet != null, stage + " did not have outlet-face flow");
        if (outlet == null) {
            throw new NullPointerException("Required test object is missing: " + stage + " did not have outlet-face flow" + '.');
        }

        helper.assertTrue(inlet.direction() == FlowDirection.INBOUND, stage + " inlet face was not inbound");
        helper.assertTrue(outlet.direction() == FlowDirection.OUTBOUND, stage + " outlet face was not outbound");
        helper.assertTrue(inlet.flowRate() > 0, stage + " flow rate was not positive");
        helper.assertValueEqual(outlet.flowRate(), inlet.flowRate(), stage + " face flow rate");
        helper.assertValueEqual(transport.getThroughputFlowRate(), inlet.flowRate(), stage + " throughput");
        helper.assertTrue(inlet.gas().is(CCBGases.NATURAL_AIR.get()), stage + " inlet gas was not Natural Air");
        helper.assertTrue(outlet.gas().is(CCBGases.NATURAL_AIR.get()), stage + " outlet gas was not Natural Air");
    }

    private static boolean hasPositiveFlow(GasTransportBehaviour transport) {
        return isPositiveFlow(transport.getFlowState(Direction.WEST), FlowDirection.INBOUND) && isPositiveFlow(transport.getFlowState(Direction.EAST), FlowDirection.OUTBOUND);
    }

    private static boolean isPositiveFlow(@Nullable FlowState flowState, FlowDirection expectedDirection) {
        return flowState != null && flowState.direction() == expectedDirection && flowState.flowRate() > 0;
    }

    private static void assertNoOffAxisFlow(GameTestHelper helper, GasTransportBehaviour transport, String stage) {
        helper.assertTrue(transport.getFlowState(Direction.NORTH) == null, stage + " produced ghost north flow");
        helper.assertTrue(transport.getFlowState(Direction.SOUTH) == null, stage + " produced ghost south flow");
        helper.assertTrue(transport.getFlowState(Direction.UP) == null, stage + " produced ghost up flow");
        helper.assertTrue(transport.getFlowState(Direction.DOWN) == null, stage + " produced ghost down flow");
    }

    private static void assertTestSpeed(GameTestHelper helper, AirtightValveBlockEntity valve, String stage) {
        float speedMagnitude = Mth.abs(valve.getSpeed());
        helper.assertTrue(Mth.abs(speedMagnitude - TEST_MOTOR_SPEED) <= SPEED_EPSILON, stage + " did not receive the expected direct-drive speed: actual=" + speedMagnitude + ", expected=" + TEST_MOTOR_SPEED);
    }

    private static void failAfterDeadline(GameTestHelper helper, int ticks, String message) {
        if (!(ticks > KINETIC_DEADLINE_TICKS)) {
            return;
        }

        helper.fail(message);
    }

    private static final class ValveMotorController {
        private final CreativeMotorBlockEntity motor;
        private final AirtightValveBlockEntity valve;
        private boolean calibrationStarted;
        private int motorToValveSign;

        private ValveMotorController(CreativeMotorBlockEntity motor, AirtightValveBlockEntity valve) {
            this.motor = motor;
            this.valve = valve;
            motor.generatedSpeed.setValue(0);
        }

        private boolean calibrate() {
            if (!calibrationStarted) {
                motor.generatedSpeed.setValue(TEST_MOTOR_SPEED);
                calibrationStarted = true;
                return false;
            }

            float motorSpeed = motor.generatedSpeed.getValue();
            float valveSpeed = valve.getSpeed();
            if (Mth.abs(motorSpeed) <= SPEED_EPSILON || Mth.abs(valveSpeed) <= SPEED_EPSILON) {
                return false;
            }

            motorToValveSign = motorSpeed > 0 == valveSpeed > 0 ? 1 : -1;
            return true;
        }

        private void drivePositive() {
            drive(1);
        }

        private void driveNegative() {
            drive(-1);
        }

        private void drive(int desiredValveSign) {
            if (motorToValveSign == 0) {
                throw new IllegalStateException("Valve motor controller was used before calibration.");
            }

            motor.generatedSpeed.setValue(TEST_MOTOR_SPEED * desiredValveSign * motorToValveSign);
        }

        private void stop() {
            motor.generatedSpeed.setValue(0);
        }

        private boolean hasPositiveValveSpeed() {
            return valve.getSpeed() > SPEED_EPSILON;
        }

        private boolean hasNegativeValveSpeed() {
            return valve.getSpeed() < -SPEED_EPSILON;
        }

        private boolean isValveStopped() {
            return Mth.abs(valve.getSpeed()) <= SPEED_EPSILON;
        }
    }
}
