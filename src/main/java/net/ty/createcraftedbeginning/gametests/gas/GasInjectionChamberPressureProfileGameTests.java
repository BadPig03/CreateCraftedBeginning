package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberTransactions;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasInjectionChamberPressureProfileGameTests {
    private GasInjectionChamberPressureProfileGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void transactionValidationKeepsPlannedGameplayPressureProfile(GameTestHelper helper) {
        long threshold = GameplayPressureProfiles.HIGH_PRESSURE.minimumPressurePa();

        helper.assertTrue(GasInjectionChamberTransactions.pressureProfileMatches(GasPressure.REFERENCE_PRESSURE_PA, threshold - 1), "Injection transaction rejected two pressures in the normal gameplay profile");
        helper.assertTrue(!GasInjectionChamberTransactions.pressureProfileMatches(threshold - 1, threshold), "Injection transaction allowed a plan to cross the gameplay pressure profile boundary");
        helper.assertTrue(GasInjectionChamberTransactions.pressureProfileMatches(threshold, GasPressure.pascals(25)), "Injection transaction rejected two pressures in the high-pressure gameplay profile");
        helper.assertTrue(GasInjectionChamberTransactions.pressureProfileMatches(GasPressure.VACUUM_PA, -1), "Injection transaction wildcard source pressure stopped matching");
        helper.succeed();
    }
}
