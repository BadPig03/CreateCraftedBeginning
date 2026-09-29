package net.ty.createcraftedbeginning.gametests.compat.functionalstorage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PredictedTransferLimits;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerBlock;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerBlockEntity;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerHandler;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerTransfer;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerTransfer.VoidTarget;
import net.ty.createcraftedbeginning.compat.functionalstorage.registry.CCBFunctionalStorageBlocks;
import net.ty.createcraftedbeginning.gas.atmosphere.AtmosphereStateResolver;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkSimulator;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.gas.storage.handler.CombinedGasStorageHandler;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@PrefixGameTestTemplate(false)
public final class GasDrawerPressureTransferGameTests {
    private static final long ATM = GasPressure.REFERENCE_PRESSURE_PA;
    private static final int STABLE_TICKS = 12;

    private GasDrawerPressureTransferGameTests() {
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 140)
    public static void oneByOneDrawerReachesExactAtmosphericBalance(GameTestHelper helper) {
        assertAtmosphericBalance(helper, CCBFunctionalStorageBlocks.GAS_DRAWER_1_BLOCK.get());
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 140)
    public static void oneByTwoDrawerReachesExactAtmosphericBalance(GameTestHelper helper) {
        assertAtmosphericBalance(helper, CCBFunctionalStorageBlocks.GAS_DRAWER_2_BLOCK.get());
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 140)
    public static void twoByTwoDrawerReachesExactAtmosphericBalance(GameTestHelper helper) {
        assertAtmosphericBalance(helper, CCBFunctionalStorageBlocks.GAS_DRAWER_4_BLOCK.get());
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void drawerPredictionPreservesSlotRoutingWithoutChangingStorage(GameTestHelper helper) {
        GasDrawerBlockEntity drawer = placeDrawer(helper, new BlockPos(1, 1, 1), CCBFunctionalStorageBlocks.GAS_DRAWER_2_BLOCK.get());
        GasDrawerHandler handler = drawer.getGasHandler();
        GasTank firstTank = handler.getInternalTank(0);
        GasTank secondTank = handler.getInternalTank(1);
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 100);
        firstTank.tryReplaceContents(gas).requireAccepted();
        GasPressureCompartment first = handler.getPressureCompartment(0);
        GasPressureCompartment second = handler.getPressureCompartment(1);
        long capacity = first.getMaxAmount();
        PredictedTransferLimits occupied = first.predictTransferLimits(gas, 101);
        if (occupied == null) {
            throw new NullPointerException("Missing drawer transfer prediction for slot 0 at 101 GU.");
        }

        helper.assertValueEqual(occupied.drainLimit(), 101L, "predicted drawer drain amount");
        helper.assertValueEqual(occupied.fillLimit(), capacity - 101, "predicted drawer fill headroom");
        PredictedTransferLimits full = first.predictTransferLimits(gas, capacity);
        if (full == null) {
            throw new NullPointerException("Missing drawer transfer prediction for slot 0 at capacity.");
        }

        helper.assertValueEqual(full.fillLimit(), 0L, "predicted full drawer fill limit");
        PredictedTransferLimits empty = first.predictTransferLimits(gas, 0);
        if (empty == null) {
            throw new NullPointerException("Missing drawer transfer prediction for empty slot 0.");
        }

        helper.assertValueEqual(empty.drainLimit(), 0L, "predicted empty drawer drain limit");
        helper.assertValueEqual(empty.fillLimit(), capacity, "predicted empty drawer fill limit");
        PredictedTransferLimits sibling = second.predictTransferLimits(gas, 0);
        if (sibling == null) {
            throw new NullPointerException("Missing drawer transfer prediction for blocked sibling slot 1.");
        }

        helper.assertValueEqual(sibling.fillLimit(), 0L, "Prediction invented access to a blocked sibling slot");
        helper.assertValueEqual(firstTank.getStoredAmount(), 100L, "Prediction changed live drawer contents");
        helper.assertValueEqual(secondTank.getStoredAmount(), 0L, "Prediction changed live sibling contents");

        secondTank.tryReplaceContents(gas.copyWithAmount(50)).requireAccepted();
        PredictedTransferLimits emptiedFirst = first.predictTransferLimits(gas, 0);
        helper.assertTrue(emptiedFirst == null, "Prediction hid the sibling endpoint activated by emptying the selected slot");
        PredictedTransferLimits blockedDrain = second.predictTransferLimits(gas, 51);
        if (blockedDrain == null) {
            throw new NullPointerException("Missing drawer transfer prediction for slot 1 at 51 GU with slot 0 occupied.");
        }

        helper.assertValueEqual(blockedDrain.drainLimit(), 0L, "Prediction bypassed the first occupied drain slot");
        helper.assertValueEqual(firstTank.getStoredAmount(), 100L, "Prediction changed live first duplicate contents");
        helper.assertValueEqual(secondTank.getStoredAmount(), 50L, "Prediction changed live second duplicate contents");

        firstTank.tryReplaceContents(GasStack.EMPTY).requireAccepted();
        helper.assertTrue(second.predictTransferLimits(gas, 0) == null, "Prediction hid routing back to an earlier empty slot");
        helper.assertValueEqual(firstTank.getStoredAmount(), 0L, "Prediction changed live empty first slot contents");
        helper.assertValueEqual(secondTank.getStoredAmount(), 50L, "Prediction changed live remaining sibling contents");

        secondTank.tryReplaceContents(GasStack.EMPTY).requireAccepted();
        PredictedTransferLimits firstEmpty = first.predictTransferLimits(gas, 0);
        if (firstEmpty == null) {
            throw new NullPointerException("Missing drawer transfer prediction for slot 0 with both slots empty.");
        }

        helper.assertValueEqual(firstEmpty.fillLimit(), capacity, "First empty slot lost its predicted fill access");
        PredictedTransferLimits secondEmpty = second.predictTransferLimits(gas, 0);
        if (secondEmpty == null) {
            throw new NullPointerException("Missing drawer transfer prediction for slot 1 with both slots empty.");
        }

        helper.assertValueEqual(secondEmpty.fillLimit(), 0L, "Prediction opened multiple empty slots for the same gas");
        helper.assertValueEqual(firstTank.getStoredAmount(), 0L, "Prediction filled the live empty first slot");
        helper.assertValueEqual(secondTank.getStoredAmount(), 0L, "Prediction filled the live empty sibling slot");
        helper.succeed();
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void oneByTwoPressureViewDoesNotRefillEmptySiblingWithStoredGas(GameTestHelper helper) {
        GasDrawerBlockEntity drawer = placeDrawer(helper, new BlockPos(1, 1, 1), CCBFunctionalStorageBlocks.GAS_DRAWER_2_BLOCK.get());
        GasDrawerHandler handler = drawer.getGasHandler();
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 1000);

        long initialized = handler.getInternalTank(0).fill(gas, GasAction.EXECUTE);
        helper.assertValueEqual(initialized, 1000L, "1x2 initialized gas amount");

        GasPressureCompartment occupied = handler.getPressureCompartment(0);
        GasPressureCompartment emptySibling = handler.getPressureCompartment(1);
        helper.assertTrue(occupied.fill(gas.copyWithAmount(1), GasAction.SIMULATE) > 0, "Occupied 1x2 slot stopped accepting its own gas");
        helper.assertValueEqual(emptySibling.fill(gas.copyWithAmount(1), GasAction.SIMULATE), 0L, "Empty 1x2 sibling remained a Solver fill endpoint for gas stored in another slot");
        helper.assertTrue(!emptySibling.isGasValid(gas), "Empty 1x2 sibling still advertised stored gas as pressure-valid");
        helper.succeed();
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void twoByTwoPressureViewChoosesOnlyOneEmptySlotForNewGas(GameTestHelper helper) {
        GasDrawerBlockEntity drawer = placeDrawer(helper, new BlockPos(1, 1, 1), CCBFunctionalStorageBlocks.GAS_DRAWER_4_BLOCK.get());
        GasDrawerHandler handler = drawer.getGasHandler();
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 1);

        helper.assertTrue(handler.getPressureCompartment(0).fill(gas, GasAction.SIMULATE) > 0, "First 2x2 slot did not accept new gas through pressure view");
        for (int tank = 1; tank < handler.getTanks(); tank++) {
            helper.assertValueEqual(handler.getPressureCompartment(tank).fill(gas, GasAction.SIMULATE), 0L, "Additional empty 2x2 slot was exposed as a duplicate Solver fill endpoint");
        }

        helper.succeed();
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void regularDrawerFillStillOverflowsFullGasIntoNextSlot(GameTestHelper helper) {
        GasDrawerBlockEntity drawer = placeDrawer(helper, new BlockPos(1, 1, 1), CCBFunctionalStorageBlocks.GAS_DRAWER_2_BLOCK.get());
        GasDrawerHandler handler = drawer.getGasHandler();
        long firstCapacity = handler.getTankMaxAmount(0);
        GasStack fullFirstSlot = new GasStack(CCBGases.NATURAL_AIR.get(), firstCapacity);

        long initialized = handler.getInternalTank(0).fill(fullFirstSlot, GasAction.EXECUTE);
        helper.assertValueEqual(initialized, firstCapacity, "1x2 first slot initialization");

        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 100);
        long directFill = handler.fill(gas, GasAction.EXECUTE);
        helper.assertValueEqual(directFill, 100L, "Normal Drawer handler no longer overflowed into the next slot");
        helper.assertValueEqual(handler.getInternalTank(1).getStoredAmount(), 100L, "Normal Drawer overflow target amount");

        GasPressureCompartment first = handler.getPressureCompartment(0);
        GasPressureCompartment second = handler.getPressureCompartment(1);
        helper.assertValueEqual(first.drain(gas.copyWithAmount(1), GasAction.SIMULATE).getAmount(), 1L, "First duplicate slot was not selected for pressure drain");
        helper.assertTrue(second.drain(gas.copyWithAmount(1), GasAction.SIMULATE).isEmpty(), "Second duplicate slot remained a parallel Solver drain endpoint");

        GasStack drainedFirst = first.drain(firstCapacity, GasAction.EXECUTE);
        helper.assertValueEqual(drainedFirst.getAmount(), firstCapacity, "First duplicate slot drain amount");
        helper.assertValueEqual(second.drain(gas.copyWithAmount(1), GasAction.SIMULATE).getAmount(), 1L, "Second duplicate slot did not become the pressure drain after the first emptied");
        helper.succeed();
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void drawerToCanisterStopsAtPassiveEquilibrium(GameTestHelper helper) {
        GasTank drawer = tank(64000, ATM, 64000);
        TestCanister canister = new TestCanister(2000, 10 * ATM, 0);

        long transferred = GasDrawerTransfer.transferDrawerToCanister(drawer, canister, 0, Long.MAX_VALUE);

        helper.assertValueEqual(transferred, 1939L, "drawer-to-canister passive amount");
        helper.assertValueEqual(drawer.getStoredAmount(), 62061L, "drawer amount after canister fill");
        helper.assertValueEqual(canister.getGasInTank(0).getAmount(), 1939L, "canister amount after drawer fill");
        long canisterPressure = canister.getTankPressurePa(0);
        helper.assertTrue(canisterPressure <= ATM, "One-atmosphere Drawer compressed a Canister above its source pressure");
        helper.assertTrue(drawer.getPressurePa() >= canisterPressure, "Drawer-to-Canister transfer crossed passive equilibrium");
        helper.succeed();
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void canisterCannotFlowUphillIntoDrawer(GameTestHelper helper) {
        TestCanister canister = new TestCanister(2000, 10 * ATM, 1000);
        GasTank drawer = tank(64000, ATM, 64000);

        long transferred = GasDrawerTransfer.transferCanisterToDrawer(canister, 0, drawer, null, Long.MAX_VALUE);

        helper.assertValueEqual(transferred, 0L, "uphill canister-to-drawer amount");
        helper.assertValueEqual(canister.getGasInTank(0).getAmount(), 1000L, "uphill canister source amount");
        helper.assertValueEqual(drawer.getStoredAmount(), 64000L, "uphill drawer target amount");
        helper.succeed();
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void automaticPushUsesPressureSafeTargetCompartments(GameTestHelper helper) {
        GasTank drawer = tank(64000, ATM, 64000);
        GasTank target = tank(2000, 10 * ATM, 0);

        long transferred = GasDrawerTransfer.pushPressureSafe(drawer, target, 4000);

        helper.assertValueEqual(transferred, 1939L, "automatic push passive amount");
        helper.assertValueEqual(target.getStoredAmount(), 1939L, "automatic push target amount");
        helper.assertTrue(target.getPressurePa() <= ATM, "Automatic Drawer push created pressure above the Drawer source");
        helper.succeed();
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void automaticPullDoesNotDrainLowPressureSourceIntoHigherPressureDrawer(GameTestHelper helper) {
        GasTank source = tank(2000, 10 * ATM, 1000);
        GasTank drawer = tank(64000, ATM, 64000);

        long transferred = GasDrawerTransfer.pullPressureSafe(source, drawer, 4000);

        helper.assertValueEqual(transferred, 0L, "automatic uphill pull amount");
        helper.assertValueEqual(source.getStoredAmount(), 1000L, "automatic uphill pull source amount");
        helper.assertValueEqual(drawer.getStoredAmount(), 64000L, "automatic uphill pull drawer amount");
        helper.succeed();
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void automaticPushFailsClosedForPressurelessTarget(GameTestHelper helper) {
        GasTank drawer = tank(64000, ATM, 64000);
        PlainGasHandler target = new PlainGasHandler(0);

        long transferred = GasDrawerTransfer.pushPressureSafe(drawer, target, 4000);

        helper.assertValueEqual(transferred, 0L, "pressureless push amount");
        helper.assertValueEqual(drawer.getStoredAmount(), 64000L, "pressureless push drawer amount");
        helper.assertValueEqual(target.getStoredAmount(), 0L, "pressureless push target amount");
        helper.succeed();
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void automaticPullFailsClosedForPressurelessSource(GameTestHelper helper) {
        PlainGasHandler source = new PlainGasHandler(4000);
        GasTank drawer = tank(64000, ATM, 0);

        long transferred = GasDrawerTransfer.pullPressureSafe(source, drawer, 4000);

        helper.assertValueEqual(transferred, 0L, "pressureless pull amount");
        helper.assertValueEqual(source.getStoredAmount(), 4000L, "pressureless pull source amount");
        helper.assertValueEqual(drawer.getStoredAmount(), 0L, "pressureless pull drawer amount");
        helper.succeed();
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void voidTargetStoresOnlyPressureSafeShareThenDiscardsRemainder(GameTestHelper helper) {
        TestCanister canister = new TestCanister(1000, 10 * ATM, 10000);
        VoidTestTank drawer = new VoidTestTank(1000, ATM, 0);

        long transferred = GasDrawerTransfer.transferCanisterToDrawer(canister, 0, drawer, drawer, 4000);

        helper.assertValueEqual(transferred, 4000L, "void target handled amount");
        helper.assertValueEqual(drawer.getStoredAmount(), 1000L, "void target physically stored amount");
        long drawerPressure = drawer.getPressurePa();
        helper.assertValueEqual(drawerPressure, ATM, "void target stored pressure");
        helper.assertValueEqual(canister.getGasInTank(0).getAmount(), 6000L, "void target source amount");
        helper.assertTrue(drawerPressure <= drawer.getMaxPressurePa(), "Void handling overpressurized the Drawer storage");
        helper.succeed();
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void voidTargetCanDiscardUphillWithoutCompressingStorage(GameTestHelper helper) {
        TestCanister canister = new TestCanister(1000, 10 * ATM, 500);
        VoidTestTank drawer = new VoidTestTank(1000, ATM, 1000);

        long transferred = GasDrawerTransfer.transferCanisterToDrawer(canister, 0, drawer, drawer, 500);

        helper.assertValueEqual(transferred, 500L, "void uphill discard amount");
        helper.assertValueEqual(canister.getGasInTank(0).getAmount(), 0L, "void uphill source amount");
        helper.assertValueEqual(drawer.getStoredAmount(), 1000L, "void uphill stored amount");
        helper.assertValueEqual(drawer.getPressurePa(), ATM, "void uphill stored pressure");
        helper.succeed();
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void controllerStyleWrapperPreservesVoidDiscardSemantics(GameTestHelper helper) {
        GasTank source = tank(1000, 10 * ATM, 10000);
        VoidTestTank drawer = new VoidTestTank(1000, ATM, 0);
        CombinedGasStorageHandler controllerView = new CombinedGasStorageHandler(drawer);

        long transferred = GasDrawerTransfer.pushPressureSafe(source, controllerView, 4000);

        helper.assertValueEqual(transferred, 4000L, "wrapped void handled amount");
        helper.assertValueEqual(source.getStoredAmount(), 6000L, "wrapped void source amount");
        helper.assertValueEqual(drawer.getStoredAmount(), 1000L, "wrapped void stored amount");
        helper.assertValueEqual(drawer.getPressurePa(), ATM, "wrapped void stored pressure");
        helper.succeed();
    }

    @GameTest(templateNamespace = CreateCraftedBeginning.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void voidTargetCannotDiscardItsOwnCompartment(GameTestHelper helper) {
        VoidTestTank drawer = new VoidTestTank(1000, ATM, 1000);

        long transferred = GasDrawerTransfer.pushPressureSafe(drawer, drawer, 4000);

        helper.assertValueEqual(transferred, 0L, "self-void amount");
        helper.assertValueEqual(drawer.getStoredAmount(), 1000L, "self-void stored amount");
        helper.succeed();
    }

    private static void assertAtmosphericBalance(GameTestHelper helper, GasDrawerBlock block) {
        GasDrawerBlockEntity drawer = placeDrawer(helper, new BlockPos(0, 1, 1), block);
        GasDrawerHandler handler = drawer.getGasHandler();
        GasTank firstTank = handler.getInternalTank(0);
        ServerLevel level = helper.getLevel();
        BlockPos pipe = new BlockPos(1, 1, 1);
        BlockPos absolutePipe = helper.absolutePos(pipe);
        helper.setBlock(pipe, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        long pressure = AtmosphereStateResolver.resolve(level, helper.absolutePos(new BlockPos(2, 1, 1))).pressurePa();
        helper.assertValueEqual(pressure, ATM, "drawer fixture atmospheric pressure");
        long target = handler.getTankVolume(0);
        firstTank.tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), target - 64)).requireAccepted();
        int[] stableTicks = {0};
        helper.onEachTick(() -> {
            GasNetworkSimulator.simulateTick(level, absolutePipe);
            long amount = firstTank.getStoredAmount();
            helper.assertTrue(amount <= target, "Atmospheric fill exceeded equilibrium");
            for (int slot = 1; slot < handler.getTanks(); slot++) {
                helper.assertValueEqual(handler.getInternalTank(slot).getStoredAmount(), 0L, "Atmospheric fill leaked into a sibling slot");
            }

            if (amount != target) {
                stableTicks[0] = 0;
                return;
            }

            if (++stableTicks[0] < STABLE_TICKS) {
                return;
            }

            helper.succeed();
        });
    }

    private static GasDrawerBlockEntity placeDrawer(GameTestHelper helper, BlockPos pos, GasDrawerBlock block) {
        helper.setBlock(pos, block.defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        if (blockEntity == null) {
            throw new NullPointerException("Gas drawer block entity was not created at " + pos + '.');
        }

        if (!(blockEntity instanceof GasDrawerBlockEntity drawer)) {
            throw new IllegalStateException("Gas drawer block entity was not created at " + pos + '.');
        }

        return drawer;
    }

    private static GasTank tank(long volume, long maxPressurePa, long amount) {
        GasTank tank = new GasTank(volume, maxPressurePa);
        if (amount <= 0) {
            return tank;
        }

        long filled = tank.fill(new GasStack(CCBGases.NATURAL_AIR.get(), amount), GasAction.EXECUTE);
        if (filled != amount) {
            throw new IllegalStateException("Failed to initialize test gas storage: expected " + amount + " GU, filled " + filled + " GU.");
        }

        return tank;
    }

    private static final class TestCanister implements GasCanisterContainer {
        private final GasTank tank;

        private TestCanister(long volume, long maxPressurePa, long amount) {
            tank = tank(volume, maxPressurePa, amount);
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
            if (tankIndex != 0) {
                return GasStack.EMPTY;
            }

            return tank.drain(resource, action);
        }

        @Override
        public GasStack drain(int tankIndex, long maxDrain, GasAction action) {
            if (tankIndex != 0) {
                return GasStack.EMPTY;
            }

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
        public long fill(int tankIndex, GasStack resource, GasAction action) {
            if (tankIndex != 0) {
                return 0;
            }

            return tank.fill(resource, action);
        }

        @Override
        public boolean supportsExactDrainRecovery(int tankIndex) {
            return tankIndex == 0;
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

    private static final class VoidTestTank extends GasTank implements VoidTarget {
        private VoidTestTank(long volume, long maxPressurePa, long amount) {
            super(volume, maxPressurePa);
            if (amount <= 0) {
                return;
            }

            long filled = fill(new GasStack(CCBGases.NATURAL_AIR.get(), amount), GasAction.EXECUTE);
            if (filled == amount) {
                return;
            }

            throw new IllegalStateException("Failed to initialize test gas storage: expected " + amount + " GU, filled " + filled + " GU.");
        }

        @Override
        public boolean canVoid(GasStack gas) {
            GasStack storedGas = getGasStack();
            return !gas.isEmpty() && (storedGas.isEmpty() || GasStack.isSameGasSameComponents(storedGas, gas));
        }
    }

    private static final class PlainGasHandler implements GasHandler {
        private final GasTank tank = new GasTank(64000, 10 * ATM);

        private PlainGasHandler(long amount) {
            if (amount <= 0) {
                return;
            }

            long filled = tank.fill(new GasStack(CCBGases.NATURAL_AIR.get(), amount), GasAction.EXECUTE);
            if (filled == amount) {
                return;
            }

            throw new IllegalStateException("Failed to initialize test gas storage: expected " + amount + " GU, filled " + filled + " GU.");
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
