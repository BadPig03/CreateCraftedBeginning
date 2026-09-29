package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionFanCost;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasInjectionChamberGameplayPressureGameTests {
    private GasInjectionChamberGameplayPressureGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void fanProcessingPressureEfficiencyUsesGameplayProfileBoundary(GameTestHelper helper) {
        long threshold = GameplayPressureProfiles.HIGH_PRESSURE.minimumPressurePa();
        helper.assertValueEqual(GasInjectionFanCost.getFanProcessingPressureEfficiencyDivisor(threshold - 1), 1, "Normal-profile fan processing pressure divisor");
        helper.assertValueEqual(GasInjectionFanCost.getFanProcessingPressureEfficiencyDivisor(threshold), 2, "High-pressure-profile fan processing pressure divisor");
        helper.succeed();
    }
}
