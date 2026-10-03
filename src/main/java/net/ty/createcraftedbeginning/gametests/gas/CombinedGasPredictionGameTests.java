package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PredictedTransferLimits;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.gas.storage.handler.CombinedGasStorageHandler;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CombinedGasPredictionGameTests {
    private CombinedGasPredictionGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void varietyPredictionAllowsEmptyingOnlyWithoutSiblingFillChanges(GameTestHelper helper) {
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 1);
        GasTank first = new GasTank(1000);
        GasTank second = new GasTank(1000);
        first.fill(gas, GasAction.EXECUTE);
        CombinedGasStorageHandler single = new CombinedGasStorageHandler(first);
        single.enforceVariety();
        PredictedTransferLimits empty = single.getPressureCompartment(0).predictTransferLimits(gas, 0);
        if (empty == null) {
            throw new NullPointerException("Expected a prediction when emptying the only gas compartment.");
        }

        helper.assertValueEqual(empty.drainLimit(), 0L, "empty single compartment drain limit");
        helper.assertValueEqual(empty.fillLimit(), 1000L, "empty single compartment fill limit");
        CombinedGasStorageHandler shared = new CombinedGasStorageHandler(second, first);
        shared.enforceVariety();
        helper.assertTrue(shared.getPressureCompartment(1).predictTransferLimits(gas, 0) == null, "Emptying a later slot must not hide newly available sibling filling.");
        CombinedGasStorageHandler output = new CombinedGasStorageHandler(first, second) {
            @Override
            protected boolean canFillPressureCompartment(int tank) {
                return false;
            }
        };
        output.enforceVariety();
        PredictedTransferLimits drainedOutput = output.getPressureCompartment(0).predictTransferLimits(gas, 0);
        if (drainedOutput == null) {
            throw new NullPointerException("Expected a prediction when emptying an extraction-only gas compartment.");
        }

        helper.assertValueEqual(drainedOutput.drainLimit(), 0L, "empty output drain limit");
        helper.assertValueEqual(drainedOutput.fillLimit(), 0L, "empty output fill limit");
        helper.assertValueEqual(first.getStoredAmount(), 1L, "stored gas after predictions");
        helper.assertValueEqual(second.getStoredAmount(), 0L, "sibling gas after predictions");
        first.drain(1, GasAction.EXECUTE);
        PredictedTransferLimits filled = single.getPressureCompartment(0).predictTransferLimits(gas, 1);
        if (filled == null) {
            throw new NullPointerException("Expected a prediction when filling the only gas compartment.");
        }

        helper.assertValueEqual(filled.drainLimit(), 1L, "predicted refilled single compartment drain limit");
        helper.assertValueEqual(filled.fillLimit(), 999L, "predicted refilled single compartment fill limit");
        helper.assertValueEqual(first.getStoredAmount(), 0L, "empty storage after refill prediction");
        helper.succeed();
    }
}
