package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchController;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirtightHatchPressureTransferGameTests {

    private AirtightHatchPressureTransferGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void normalOutputStopsAtPassiveEquilibrium(GameTestHelper helper) {
        GasTank hatch = tank(2000000, 10000);
        GasTank target = tank(2000000, 0);

        long transferred = AirtightHatchController.outputOnlyPressureSafe(hatch, target, Long.MAX_VALUE, false);

        helper.assertValueEqual(transferred, 5000L, "hatch passive output amount");
        helper.assertValueEqual(hatch.getStoredAmount(), 5000L, "hatch amount after passive output");
        helper.assertValueEqual(target.getStoredAmount(), 5000L, "target amount after passive output");
        helper.assertValueEqual(hatch.getPressurePa(), 500000L, "hatch pressure after passive output");
        helper.assertValueEqual(target.getPressurePa(), 500000L, "target pressure after passive output");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void normalInputCannotFlowUphill(GameTestHelper helper) {
        GasTank hatch = tank(2000000, 8000);
        GasTank source = tank(2000000, 5000);

        long transferred = AirtightHatchController.inputOnlyPressureSafe(hatch, source, Long.MAX_VALUE, false);

        helper.assertValueEqual(transferred, 0L, "uphill hatch input amount");
        helper.assertValueEqual(hatch.getStoredAmount(), 8000L, "hatch amount after uphill input");
        helper.assertValueEqual(source.getStoredAmount(), 5000L, "source amount after uphill input");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void pressurelessTargetFailsClosed(GameTestHelper helper) {
        GasTank hatch = tank(2000000, 10000);
        PlainGasHandler target = new PlainGasHandler(0);

        long transferred = AirtightHatchController.outputOnlyPressureSafe(hatch, target, 4000, false);

        helper.assertValueEqual(transferred, 0L, "pressureless hatch output amount");
        helper.assertValueEqual(hatch.getStoredAmount(), 10000L, "hatch amount after pressureless output");
        helper.assertValueEqual(target.getStoredAmount(), 0L, "pressureless target amount");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void targetPressureInputStopsAtSetpoint(GameTestHelper helper) {
        GasTank hatch = tank(1000000, 1000);
        GasTank source = tank(2000000, 12000);

        long transferred = AirtightHatchController.targetPressureSafe(hatch, source, Long.MAX_VALUE, 500000);

        helper.assertValueEqual(transferred, 4000L, "target-pressure hatch input amount");
        helper.assertValueEqual(hatch.getStoredAmount(), 5000L, "hatch amount at target pressure");
        helper.assertValueEqual(hatch.getPressurePa(), 500000L, "hatch target pressure");
        helper.assertValueEqual(source.getStoredAmount(), 8000L, "source amount after target-pressure input");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void targetPressureOutputStopsAtSetpoint(GameTestHelper helper) {
        GasTank hatch = tank(1000000, 9000);
        GasTank target = tank(2000000, 1000);

        long transferred = AirtightHatchController.targetPressureSafe(hatch, target, Long.MAX_VALUE, 500000);

        helper.assertValueEqual(transferred, 4000L, "target-pressure hatch output amount");
        helper.assertValueEqual(hatch.getStoredAmount(), 5000L, "hatch amount after target-pressure output");
        helper.assertValueEqual(hatch.getPressurePa(), 500000L, "hatch pressure after target-pressure output");
        helper.assertValueEqual(target.getStoredAmount(), 5000L, "target amount after target-pressure output");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void targetPressureCannotCreatePressure(GameTestHelper helper) {
        GasTank hatch = tank(1000000, 0);
        GasTank source = tank(1000000, 3000);

        long transferred = AirtightHatchController.targetPressureSafe(hatch, source, Long.MAX_VALUE, 500000);

        helper.assertValueEqual(transferred, 1500L, "passive target-pressure input amount");
        helper.assertValueEqual(hatch.getPressurePa(), 150000L, "passive hatch equilibrium pressure");
        helper.assertValueEqual(source.getPressurePa(), 150000L, "passive source equilibrium pressure");
        helper.assertTrue(hatch.getPressurePa() < 500000, "target pressure must not act as a compressor");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void creativeOutputIsFixedPressureInfiniteSource(GameTestHelper helper) {
        GasTank hatch = tank(1000000, 10000);
        GasTank target = tank(2000000, 0);

        long transferred = AirtightHatchController.outputOnlyPressureSafe(hatch, target, Long.MAX_VALUE, true);

        helper.assertValueEqual(transferred, 10000L, "creative hatch output amount");
        helper.assertValueEqual(hatch.getStoredAmount(), 10000L, "creative hatch source amount");
        helper.assertValueEqual(target.getStoredAmount(), 10000L, "creative hatch target amount");
        helper.assertValueEqual(target.getPressurePa(), 1000000L, "creative hatch target pressure");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void creativeInputDiscardsWithoutCompressingHatchStorage(GameTestHelper helper) {
        GasTank hatch = tank(1000000, 10000);
        GasTank source = tank(2000000, 5000);

        long transferred = AirtightHatchController.inputOnlyPressureSafe(hatch, source, 2000, true);

        helper.assertValueEqual(transferred, 2000L, "creative hatch discard amount");
        helper.assertValueEqual(source.getStoredAmount(), 3000L, "creative hatch discard source amount");
        helper.assertValueEqual(hatch.getStoredAmount(), 10000L, "creative hatch storage after discard");
        helper.assertValueEqual(hatch.getPressurePa(), 1000000L, "creative hatch pressure after discard");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void creativeInputFailsClosedForPressurelessSource(GameTestHelper helper) {
        GasTank hatch = tank(1000000, 10000);
        PlainGasHandler source = new PlainGasHandler(2000);

        long transferred = AirtightHatchController.inputOnlyPressureSafe(hatch, source, 2000, true);

        helper.assertValueEqual(transferred, 0L, "pressureless creative hatch input amount");
        helper.assertValueEqual(source.getStoredAmount(), 2000L, "pressureless creative hatch source amount");
        helper.assertValueEqual(hatch.getStoredAmount(), 10000L, "creative hatch amount after pressureless input");
        helper.succeed();
    }

    private static GasTank tank(long maxPressurePa, long amount) {
        GasTank tank = new GasTank(1000, maxPressurePa);
        if (amount > 0) {
            long filled = tank.fill(new GasStack(CCBGases.NATURAL_AIR.get(), amount), GasAction.EXECUTE);
            if (filled != amount) {
                throw new IllegalStateException("Failed to initialize test gas storage: expected " + amount + " GU, filled " + filled + " GU.");
            }
        }
        return tank;
    }

    private static final class PlainGasHandler implements GasHandler {
        private final GasTank tank = new GasTank(1000, 2000000);

        private PlainGasHandler(long amount) {
            if (amount <= 0) {
                return;
            }

            long filled = tank.fill(new GasStack(CCBGases.NATURAL_AIR.get(), amount), GasAction.EXECUTE);
            if (filled != amount) {
                throw new IllegalStateException("Failed to initialize test gas storage: expected " + amount + " GU, filled " + filled + " GU.");
            }
        }

        @Override
        public boolean isGasValid(int tankIndex, GasStack stack) {
            return tankIndex == 0 && tank.isGasValid(stack);
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return tank.drain(resource, action);
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return tank.drain(maxDrain, action);
        }

        @Override
        public GasStack getGasInTank(int tankIndex) {
            if (tankIndex != 0) {
                return GasStack.EMPTY;
            }

            return tank.getGasStack();
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            return tank.fill(resource, action);
        }

        private long getStoredAmount() {
            return tank.getStoredAmount();
        }
    }
}
