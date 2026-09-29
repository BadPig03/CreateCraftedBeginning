package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive.Linearization;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive.OutletPressureTarget;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasTransportPressureDriveGameTests {
    private static final double CONDUCTANCE = 0.01;

    private GasTransportPressureDriveGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void pressureBoostRetainsDifferentialDrive(GameTestHelper helper) {
        long inletPressurePa = GasPressure.pascals(2);
        long pressureBoostPa = GasPressure.pascals(3);
        GasTransportPressureDrive drive = GasTransportPressureDrive.pressureBoost(pressureBoostPa);
        Linearization linearization = drive.linearize(inletPressurePa, CONDUCTANCE);

        helper.assertTrue(closeTo(drive.drivenOutletPressurePa(inletPressurePa), GasPressure.pascals(5)), "Pressure boost drive did not add its configured pressure rise");
        helper.assertTrue(closeTo(linearization.fromPressureConductance(), CONDUCTANCE), "Pressure boost inlet conductance changed");
        helper.assertTrue(closeTo(linearization.toPressureConductance(), CONDUCTANCE), "Pressure boost outlet conductance changed");
        helper.assertTrue(closeTo(linearization.constantFlowRate(), CONDUCTANCE * pressureBoostPa), "Pressure boost linearization offset changed");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void stagedPressureBoostsCanReachHighPressureGameplayProfile(GameTestHelper helper) {
        GasTransportPressureDrive stage = GasTransportPressureDrive.pressureBoost(GasPressure.pascals(4));
        long pressurePa = GasPressure.REFERENCE_PRESSURE_PA;
        long highPressureThresholdPa = GameplayPressureProfiles.HIGH_PRESSURE.minimumPressurePa();

        pressurePa = Math.round(stage.drivenOutletPressurePa(pressurePa));
        helper.assertTrue(pressurePa < highPressureThresholdPa, "A single staged pressure boost unexpectedly crossed the high-pressure gameplay threshold");
        pressurePa = Math.round(stage.drivenOutletPressurePa(pressurePa));
        helper.assertTrue(pressurePa < highPressureThresholdPa, "Two staged pressure boosts unexpectedly crossed the high-pressure gameplay threshold");
        pressurePa = Math.round(stage.drivenOutletPressurePa(pressurePa));
        helper.assertTrue(GameplayPressureProfiles.isAtLeast(pressurePa, GameplayPressureProfiles.HIGH_PRESSURE), "Three staged pressure boosts could not reach the high-pressure gameplay threshold after Air Compressor removal");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void pressureBoostCannotDriveBeyondHardPressureLimit(GameTestHelper helper) {
        GasTransportPressureDrive drive = GasTransportPressureDrive.pressureBoost(GasPressure.pascals(4));
        long inletPressurePa = GasPressure.pascals(23);
        Linearization linearization = drive.linearize(inletPressurePa, CONDUCTANCE);

        helper.assertTrue(closeTo(drive.drivenOutletPressurePa(inletPressurePa), GasPressureLimits.HARD_PRESSURE_PA), "Pressure boost drove the outlet above the global hard pressure limit");
        helper.assertTrue(closeTo(linearization.fromPressureConductance(), 0), "Hard-limited pressure boost retained inlet-pressure dependence");
        helper.assertTrue(closeTo(linearization.toPressureConductance(), CONDUCTANCE), "Hard-limited pressure boost lost outlet-pressure dependence");
        helper.assertTrue(closeTo(linearization.constantFlowRate(), CONDUCTANCE * GasPressureLimits.HARD_PRESSURE_PA), "Hard-limited pressure boost linearization did not target the hard ceiling");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void outletTargetCannotBeConfiguredBeyondHardPressureLimit(GameTestHelper helper) {
        OutletPressureTarget drive = GasTransportPressureDrive.outletPressureTarget(GasPressure.pascals(100), GasPressure.pascals(100));

        helper.assertTrue(drive.targetPressurePa() == GasPressureLimits.HARD_PRESSURE_PA, "Outlet pressure target retained a target above the hard pressure limit");
        helper.assertTrue(drive.maxPressureBoostPa() == GasPressureLimits.HARD_PRESSURE_PA, "Outlet pressure target retained a pressure rise above the hard pressure limit");
        helper.assertTrue(closeTo(drive.drivenOutletPressurePa(GasPressure.pascals(1)), GasPressureLimits.HARD_PRESSURE_PA), "Clamped outlet target did not stop at the hard pressure limit");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void outletTargetUsesAbsolutePressureWhenReachable(GameTestHelper helper) {
        long inletPressurePa = GasPressure.pascals(1);
        long targetPressurePa = GasPressure.pascals(8);
        OutletPressureTarget drive = GasTransportPressureDrive.outletPressureTarget(targetPressurePa, GasPressure.pascals(16));
        Linearization linearization = drive.linearize(inletPressurePa, CONDUCTANCE);

        helper.assertTrue(drive.targetLimited(inletPressurePa), "Reachable outlet target was not target-limited");
        helper.assertTrue(closeTo(drive.drivenOutletPressurePa(inletPressurePa), targetPressurePa), "Reachable outlet target did not drive toward its absolute target pressure");
        helper.assertTrue(closeTo(drive.drivePressurePa(inletPressurePa, targetPressurePa), 0), "Outlet target retained drive at the target pressure");
        helper.assertTrue(drive.drivePressurePa(inletPressurePa, GasPressure.pascals(9)) < 0, "Outlet target requested forward drive above the target pressure");
        helper.assertTrue(closeTo(linearization.fromPressureConductance(), 0), "Target-limited drive retained inlet-pressure dependence");
        helper.assertTrue(closeTo(linearization.toPressureConductance(), CONDUCTANCE), "Target-limited drive lost outlet-pressure dependence");
        helper.assertTrue(closeTo(linearization.constantFlowRate(), CONDUCTANCE * targetPressurePa), "Target-limited drive linearization offset was incorrect");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void outletTargetRespectsMaximumPressureRise(GameTestHelper helper) {
        long inletPressurePa = GasPressure.pascals(1);
        long maxPressureBoostPa = GasPressure.pascals(4);
        OutletPressureTarget drive = GasTransportPressureDrive.outletPressureTarget(GasPressure.pascals(16), maxPressureBoostPa);
        Linearization linearization = drive.linearize(inletPressurePa, CONDUCTANCE);

        helper.assertTrue(!drive.targetLimited(inletPressurePa), "Unreachable outlet target incorrectly ignored the pressure-rise limit");
        helper.assertTrue(closeTo(drive.drivenOutletPressurePa(inletPressurePa), GasPressure.pascals(5)), "Outlet target exceeded its maximum pressure rise");
        helper.assertTrue(closeTo(linearization.fromPressureConductance(), CONDUCTANCE), "Boost-limited outlet target lost inlet-pressure dependence");
        helper.assertTrue(closeTo(linearization.toPressureConductance(), CONDUCTANCE), "Boost-limited outlet target lost outlet-pressure dependence");
        helper.assertTrue(closeTo(linearization.constantFlowRate(), CONDUCTANCE * maxPressureBoostPa), "Boost-limited outlet target linearization offset was incorrect");
        helper.succeed();
    }

    private static boolean closeTo(double actual, double expected) {
        return Math.abs(actual - expected) <= Math.max(1.0E-9, Math.abs(expected) * 1.0E-12);
    }
}
