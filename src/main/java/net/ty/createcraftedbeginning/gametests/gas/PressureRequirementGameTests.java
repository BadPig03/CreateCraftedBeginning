package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.recipe.pressure.CCBPressureRequirements;
import net.ty.createcraftedbeginning.recipe.pressure.PressureRequirement;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PressureRequirementGameTests {
    private PressureRequirementGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void optionalPressureBoundsMatchInclusiveRange(GameTestHelper helper) {
        long low = GasPressure.pascals(2);
        long high = GasPressure.pascals(5);

        helper.assertTrue(PressureRequirement.NONE.allowsPressure(GasPressure.VACUUM_PA), "Unbounded pressure requirement rejected vacuum");
        helper.assertTrue(PressureRequirement.atLeast(low).allowsPressure(low), "Minimum pressure boundary was not inclusive");
        helper.assertTrue(!PressureRequirement.atLeast(low).allowsPressure(low - 1), "Minimum pressure requirement accepted pressure below its boundary");
        helper.assertTrue(PressureRequirement.atMost(high).allowsPressure(high), "Maximum pressure boundary was not inclusive");
        helper.assertTrue(!PressureRequirement.atMost(high).allowsPressure(high + 1), "Maximum pressure requirement accepted pressure above its boundary");

        PressureRequirement bounded = PressureRequirement.between(low, high);
        helper.assertTrue(bounded.allowsPressure(low) && bounded.allowsPressure(high), "Bounded pressure requirement rejected an inclusive boundary");
        helper.assertTrue(!bounded.allowsPressure(low - 1) && !bounded.allowsPressure(high + 1), "Bounded pressure requirement accepted pressure outside its range");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void invalidPressureRangesAreRejected(GameTestHelper helper) {
        assertIllegalArgument(() -> PressureRequirement.atLeast(-1), "Negative minimum pressure was accepted.");
        assertIllegalArgument(() -> PressureRequirement.atMost(-1), "Negative maximum pressure was accepted.");
        assertIllegalArgument(() -> PressureRequirement.between(GasPressure.pascals(5), GasPressure.pascals(2)), "Reversed pressure range was accepted.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void pressureFloorAmountRoundsUp(GameTestHelper helper) {
        long volume = 100001;
        long minimumPressure = 1;
        long reserve = GasPressure.minimumAmountForPressure(volume, minimumPressure);

        helper.assertTrue(reserve == 2, "Pressure floor reserve did not round upward");
        helper.assertTrue(GasPressure.pressure(reserve, volume) >= minimumPressure, "Rounded pressure floor reserve still fell below the requested pressure");
        helper.assertTrue(GasPressure.pressure(reserve - 1, volume) < minimumPressure, "Pressure floor reserve was larger than necessary");
        helper.assertTrue(GasPressure.amountAbovePressureFloor(reserve + 25, volume, minimumPressure) == 25, "Drainable amount above the pressure floor was incorrect");
        helper.assertTrue(GasPressure.amountAbovePressureFloor(reserve, volume, minimumPressure) == 0, "Pressure floor allowed draining its reserved amount");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void pressurizedRecipeRequirementRemainsAnExactPhysicalRule(GameTestHelper helper) {
        long threshold = GasPressure.pascals(10);
        PressureRequirement requirement = CCBPressureRequirements.PRESSURIZED;

        helper.assertValueEqual(CCBPressureRequirements.PRESSURIZED_MINIMUM_PRESSURE_PA, threshold, "Pressurized recipe minimum pressure");
        helper.assertValueEqual(requirement.minimumPressurePaOrVacuum(), threshold, "Pressurized recipe requirement minimum");
        helper.assertTrue(!requirement.allowsPressure(threshold - 1), "Pressurized recipe requirement accepted pressure below 10 atm");
        helper.assertTrue(requirement.allowsPressure(threshold), "Pressurized recipe requirement rejected its inclusive 10 atm boundary");
        helper.assertTrue(requirement.allowsPressure(GasPressure.pascals(25)), "Pressurized recipe requirement unexpectedly imposed a gameplay-profile ceiling");
        helper.succeed();
    }

    private static void assertIllegalArgument(Runnable action, String message) {
        try {
            action.run();
        }
        catch (IllegalArgumentException ignored) {
            return;
        }
        throw new AssertionError(message);
    }
}
