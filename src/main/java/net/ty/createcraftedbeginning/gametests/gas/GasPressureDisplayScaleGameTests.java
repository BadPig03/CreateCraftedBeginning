package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.gas.visual.GasPressureDisplayScale;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasPressureDisplayScaleGameTests {
    private static final double EPSILON = 1.0E-6;

    private GasPressureDisplayScaleGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void fiveColorGaugeUsesRatedAndOverpressureBands(GameTestHelper helper) {
        assertClose(helper, GasPressureDisplayScale.fractionForPressure(GasPressure.VACUUM_PA), 0.0, "vacuum");
        assertClose(helper, GasPressureDisplayScale.fractionForPressure(GasPressure.pascals(6)), 0.2, "blue band end");
        assertClose(helper, GasPressureDisplayScale.fractionForPressure(GasPressure.pascals(10)), 0.4, "green band end");
        assertClose(helper, GasPressureDisplayScale.fractionForPressure(GasPressure.pascals(14)), 0.6, "yellow band end");
        assertClose(helper, GasPressureDisplayScale.fractionForPressure(GasPressureLimits.SAFE_PRESSURE_PA), 0.8, "orange safe-pressure boundary");
        assertClose(helper, GasPressureDisplayScale.fractionForPressure(GasPressureLimits.HARD_PRESSURE_PA), 1.0, "red hard-pressure boundary");
        assertClose(helper, GasPressureDisplayScale.fractionForPressure(GasPressure.pascals(30)), 1.0, "pressure above hard limit");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void ambientBreakpointIsMergedIntoBlueGaugeBand(GameTestHelper helper) {
        assertClose(helper, GasPressureDisplayScale.fractionForPressure(GasPressure.pascals(1)), 0.03333333333333333, "1 atm merged blue-band position");
        helper.assertTrue(!GasPressureLimits.isOverpressure(GasPressureLimits.SAFE_PRESSURE_PA), "Exact 16 atm must remain rated-safe");
        helper.assertTrue(GasPressureLimits.isOverpressure(GasPressureLimits.SAFE_PRESSURE_PA + 1), "Pressure above 16 atm must enter overpressure state");
        helper.succeed();
    }

    private static void assertClose(GameTestHelper helper, double actual, double expected, String label) {
        helper.assertTrue(Math.abs(actual - expected) <= EPSILON, label + ": expected " + expected + ", got " + actual);
    }
}
