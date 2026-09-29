package net.ty.createcraftedbeginning.gametests.content.airtights.gaspackager;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestAmounts;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasRequestAmountsGameTests {
    private GasRequestAmountsGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void scrollingKeepsFirstStepAndControlRules(GameTestHelper helper) {
        helper.assertValueEqual(GasRequestAmounts.scroll(1, 10, 1, false, 100), 10, "First step");
        helper.assertValueEqual(GasRequestAmounts.scroll(1, 10, 1, true, 100), 11, "Control first step");
        helper.assertValueEqual(GasRequestAmounts.scroll(15, 10, -1, false, 100), 5, "Decrease");
        helper.assertValueEqual(GasRequestAmounts.scroll(15, 10, 0, false, 100), 15, "Zero scroll");
        helper.assertValueEqual(GasRequestAmounts.scroll(15, 10, 0.25, false, 100), 25, "Fractional positive scroll keeps one configured step");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void requestAndFactoryBoundsDoNotOverflow(GameTestHelper helper) {
        helper.assertValueEqual(GasRequestAmounts.scroll(Integer.MAX_VALUE - 1, 100, 1, false, Integer.MAX_VALUE), Integer.MAX_VALUE, "Requester upper bound");
        helper.assertValueEqual(GasRequestAmounts.scroll(1, Integer.MAX_VALUE, -1, false, Integer.MAX_VALUE), 1, "Requester lower bound");
        int maximum = GasFactoryGaugeBehaviour.MAX_TARGET_AMOUNT;
        helper.assertValueEqual(GasRequestAmounts.scroll(maximum - 1, Integer.MAX_VALUE, 1, false, maximum), maximum, "Factory upper bound");
        helper.assertValueEqual(GasRequestAmounts.scroll(1, 100, -1, false, maximum), 1, "Factory minimum remains one");
        helper.succeed();
    }
}
