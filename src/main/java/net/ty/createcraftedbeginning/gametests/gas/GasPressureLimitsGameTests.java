package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureTier;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.gas.storage.GasTankLimits;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasPressureLimitsGameTests {
    private static final long VOLUME = 1000;

    private GasPressureLimitsGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void globalPressureBoundariesRemainDistinctFromRecipeTiers(GameTestHelper helper) {
        helper.assertTrue(GasPressureLimits.SAFE_PRESSURE_PA == GasPressure.pascals(16), "Safe pressure boundary changed from 16 atm");
        helper.assertTrue(GasPressureLimits.HARD_PRESSURE_PA == GasPressure.pascals(24), "Hard pressure boundary changed from 24 atm");
        helper.assertTrue(GasPressureTier.MAX_DEFINED_PRESSURE_PA == GasPressureLimits.SAFE_PRESSURE_PA, "Recipe pressure tiers no longer end at the 16 atm rated pressure");
        helper.assertTrue(GasPressureTier.resolve(GasPressureLimits.HARD_PRESSURE_PA) == GasPressureTier.EXTREME, "Overpressure range unexpectedly created a new recipe pressure tier");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void pressureNormalizationClampsToHardCeiling(GameTestHelper helper) {
        helper.assertTrue(GasPressureLimits.clampToHardLimit(-1) == GasPressure.VACUUM_PA, "Negative pressure did not clamp to vacuum");
        helper.assertTrue(GasPressureLimits.clampToHardLimit(GasPressureLimits.SAFE_PRESSURE_PA) == GasPressureLimits.SAFE_PRESSURE_PA, "Safe pressure was changed by hard-limit normalization");
        helper.assertTrue(GasPressureLimits.clampToHardLimit(GasPressureLimits.HARD_PRESSURE_PA) == GasPressureLimits.HARD_PRESSURE_PA, "Hard pressure did not remain representable");
        helper.assertTrue(GasPressureLimits.clampToHardLimit(GasPressure.pascals(25)) == GasPressureLimits.HARD_PRESSURE_PA, "Pressure above the hard ceiling was not clamped");
        helper.assertTrue(GasPressureLimits.clampToHardLimit(Double.POSITIVE_INFINITY) == GasPressureLimits.HARD_PRESSURE_PA, "Infinite pressure was not clamped to the hard ceiling");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void gasTankLimitsRejectRatingsAboveHardCeiling(GameTestHelper helper) {
        GasTankLimits accepted = new GasTankLimits(VOLUME, GasPressureLimits.HARD_PRESSURE_PA);
        helper.assertTrue(accepted.maxPressurePa() == GasPressureLimits.HARD_PRESSURE_PA, "Tank could not use the exact hard pressure ceiling");

        boolean rejected = false;
        try {
            new GasTankLimits(VOLUME, GasPressureLimits.HARD_PRESSURE_PA + 1);
        }
        catch (IllegalArgumentException ignored) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Tank accepted a maximum pressure above the global hard ceiling");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void gasTankCannotStoreBeyondHardCeiling(GameTestHelper helper) {
        GasTank tank = new GasTank(VOLUME, GasPressureLimits.HARD_PRESSURE_PA);
        GasStack oversizedFill = new GasStack(CCBGases.NATURAL_AIR.get(), GasPressure.amount(VOLUME, GasPressure.pascals(30)));
        long filled = tank.fill(oversizedFill, GasAction.EXECUTE);

        helper.assertTrue(filled == GasPressure.amount(VOLUME, GasPressureLimits.HARD_PRESSURE_PA), "Tank accepted an amount above the hard pressure ceiling");
        helper.assertTrue(tank.getPressurePa() == GasPressureLimits.HARD_PRESSURE_PA, "Filled tank pressure did not stop at the hard ceiling");
        helper.succeed();
    }
}
