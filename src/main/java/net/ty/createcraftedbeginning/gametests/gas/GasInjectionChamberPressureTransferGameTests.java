package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer.InjectionMode;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberCanisterTransfer;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasInjectionChamberPressureTransferGameTests {

    private GasInjectionChamberPressureTransferGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void chamberToCanisterPlanningFillsToFixedSourcePressure(GameTestHelper helper) {
        GasTank chamber = tank(10000, 1000000, 10000);
        TestCanister canister = new TestCanister(2000, 1000000, 0, InjectionMode.ALLOW);
        GasStack expectedGas = chamber.getGasStack();

        long transferable = GasInjectionChamberCanisterTransfer.getTransferableAmount(chamber, canister, expectedGas, Long.MAX_VALUE);
        helper.assertValueEqual(transferable, 2000L, "chamber fixed-pressure canister planning amount");
        helper.assertValueEqual(chamber.getStoredAmount(), 10000L, "chamber amount after planning");
        helper.assertValueEqual(canister.getGasInTank(0).getAmount(), 0L, "canister amount after planning");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void transferExactlyFillsCanisterToStartingSourcePressure(GameTestHelper helper) {
        GasTank chamber = tank(10000, 1000000, 10000);
        TestCanister canister = new TestCanister(2000, 1000000, 0, InjectionMode.ALLOW);
        GasStack expectedGas = chamber.getGasStack();
        long startingPressurePa = chamber.getPressurePa();

        boolean transferred = GasInjectionChamberCanisterTransfer.transferExactly(chamber, canister, expectedGas, 2000);

        helper.assertTrue(transferred, "Fixed-pressure chamber transfer was rejected");
        helper.assertValueEqual(chamber.getStoredAmount(), 8000L, "chamber amount after fixed-pressure transfer");
        helper.assertValueEqual(canister.getGasInTank(0).getAmount(), 2000L, "canister amount after fixed-pressure transfer");
        helper.assertValueEqual(canister.getTankPressurePa(0), startingPressurePa, "canister pressure after fixed-pressure transfer");
        helper.assertTrue(canister.getTankPressurePa(0) > chamber.getPressurePa(), "Canister did not retain the injection chamber's starting pressure");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void transferExactlyCompletesFinalGuAtSourcePressure(GameTestHelper helper) {
        GasTank chamber = tank(10000, 1000000, 10000);
        TestCanister canister = new TestCanister(2000, 1000000, 1999, InjectionMode.ALLOW);
        GasStack expectedGas = chamber.getGasStack();

        long transferable = GasInjectionChamberCanisterTransfer.getTransferableAmount(chamber, canister, expectedGas, Long.MAX_VALUE);
        boolean transferred = GasInjectionChamberCanisterTransfer.transferExactly(chamber, canister, expectedGas, 1);

        helper.assertValueEqual(transferable, 1L, "final fixed-pressure canister planning amount");
        helper.assertTrue(transferred, "Final GU at source pressure was rejected");
        helper.assertValueEqual(chamber.getStoredAmount(), 9999L, "chamber amount after final GU transfer");
        helper.assertValueEqual(canister.getGasInTank(0).getAmount(), 2000L, "canister amount after final GU transfer");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void transferExactlyRejectsAmountBeyondFixedPressureWithoutMutation(GameTestHelper helper) {
        GasTank chamber = tank(10000, 1000000, 10000);
        TestCanister canister = new TestCanister(2000, 1000000, 0, InjectionMode.ALLOW);
        GasStack expectedGas = chamber.getGasStack();

        boolean transferred = GasInjectionChamberCanisterTransfer.transferExactly(chamber, canister, expectedGas, 2001);

        helper.assertTrue(!transferred, "Over-pressure chamber transfer was accepted");
        helper.assertValueEqual(chamber.getStoredAmount(), 10000L, "chamber amount after rejected transfer");
        helper.assertValueEqual(canister.getGasInTank(0).getAmount(), 0L, "canister amount after rejected transfer");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void higherPressureCanisterRejectsLowerPressureChamber(GameTestHelper helper) {
        GasTank chamber = tank(10000, 1000000, 10000);
        TestCanister canister = new TestCanister(2000, 1000000, 3000, InjectionMode.ALLOW);
        GasStack expectedGas = chamber.getGasStack();

        long transferable = GasInjectionChamberCanisterTransfer.getTransferableAmount(chamber, canister, expectedGas, 10000);

        helper.assertValueEqual(transferable, 0L, "uphill chamber to canister amount");
        helper.assertValueEqual(chamber.getStoredAmount(), 10000L, "chamber amount after uphill planning");
        helper.assertValueEqual(canister.getGasInTank(0).getAmount(), 3000L, "canister amount after uphill planning");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void deniedCanisterCannotReceiveChamberGas(GameTestHelper helper) {
        GasTank chamber = tank(10000, 1000000, 10000);
        TestCanister canister = new TestCanister(2000, 1000000, 0, InjectionMode.DENY);
        GasStack expectedGas = chamber.getGasStack();

        long transferable = GasInjectionChamberCanisterTransfer.getTransferableAmount(chamber, canister, expectedGas, 10000);
        boolean transferred = GasInjectionChamberCanisterTransfer.transferExactly(chamber, canister, expectedGas, 1);

        helper.assertValueEqual(transferable, 0L, "denied canister planning amount");
        helper.assertTrue(!transferred, "denied canister accepted chamber gas");
        helper.assertValueEqual(chamber.getStoredAmount(), 10000L, "chamber amount after denied canister transfer");
        helper.assertValueEqual(canister.getGasInTank(0).getAmount(), 0L, "denied canister amount");
        helper.succeed();
    }

    private static GasTank tank(long volume, long maxPressurePa, long amount) {
        GasTank tank = new GasTank(volume, maxPressurePa);
        if (amount > 0) {
            long filled = tank.fill(new GasStack(CCBGases.NATURAL_AIR.get(), amount), GasAction.EXECUTE);
            if (filled != amount) {
                throw new IllegalStateException("Failed to initialize test gas storage: expected " + amount + " GU, filled " + filled + " GU.");
            }
        }
        return tank;
    }

    private static final class TestCanister implements GasCanisterContainer {
        private final GasTank tank;
        private final InjectionMode injectionMode;

        private TestCanister(long volume, long maxPressurePa, long amount, InjectionMode injectionMode) {
            tank = tank(volume, maxPressurePa, amount);
            this.injectionMode = injectionMode;
        }

        @Override
        public boolean isEmpty() {
            return tank.isEmpty();
        }

        @Override
        public boolean isFull() {
            return tank.getStoredAmount() >= tank.getMaxAmount();
        }

        @Override
        public boolean isGasValid(int tankIndex, GasStack stack) {
            return tankIndex == 0 && tank.isGasValid(stack);
        }

        @Override
        public GasStack drain(int tankIndex, GasStack resource, GasAction action) {
            if (tankIndex == 0) {
                return tank.drain(resource, action);
            }

            return GasStack.EMPTY;
        }

        @Override
        public GasStack drain(int tankIndex, long maxDrain, GasAction action) {
            if (tankIndex == 0) {
                return tank.drain(maxDrain, action);
            }

            return GasStack.EMPTY;
        }

        @Override
        public GasStack getGasInTank(int tankIndex) {
            if (tankIndex != 0) {
                return GasStack.EMPTY;
            }

            return tank.getGasStack();
        }

        @Override
        public int getPriority() {
            if (isEmpty()) {
                return EMPTY_CANISTER;
            }

            return NON_EMPTY_CANISTER;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public ItemStack getContainer() {
            return ItemStack.EMPTY;
        }

        @Override
        public List<ItemStack> createVirtualItems() {
            return List.of();
        }

        @Override
        public InjectionMode getInjectionMode() {
            return injectionMode;
        }

        @Override
        public long fill(int tankIndex, GasStack resource, GasAction action) {
            if (tankIndex != 0) {
                return 0;
            }

            return tank.fill(resource, action);
        }

        @Override
        public long getTankVolume(int tankIndex) {
            if (tankIndex != 0) {
                return 0;
            }

            return tank.getVolume();
        }

        @Override
        public long getTankMaxPressurePa(int tankIndex) {
            if (tankIndex != 0) {
                return 0;
            }

            return tank.getMaxPressurePa();
        }

        @Override
        public void save() {
        }
    }
}
