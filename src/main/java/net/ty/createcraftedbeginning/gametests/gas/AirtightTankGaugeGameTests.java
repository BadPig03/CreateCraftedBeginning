package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.AirtightTelemetryVisibility;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.multiblock.GasTankMultiblockConnectivity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirtightTankGaugeGameTests {
    private static final BlockPos CONTROLLER_POS = new BlockPos(2, 1, 2);
    private static final BlockPos EAST_POS = new BlockPos(3, 1, 2);
    private static final BlockPos SOUTH_POS = new BlockPos(2, 1, 3);
    private static final BlockPos SOUTH_EAST_POS = new BlockPos(3, 1, 3);

    private AirtightTankGaugeGameTests() {
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 20)
    public static void gaugeCanBeInstalledAndRemovedFromAnyMultiblockPart(GameTestHelper helper) {
        AirtightTankBlockEntity controllerCandidate = placeTank(helper, CONTROLLER_POS);
        placeTank(helper, EAST_POS);
        placeTank(helper, SOUTH_POS);
        placeTank(helper, SOUTH_EAST_POS);
        GasTankMultiblockConnectivity.formMultiblock(controllerCandidate, helper.getLevel());

        AirtightTankBlockEntity controller = requireTank(helper, CONTROLLER_POS);
        AirtightTankBlockEntity east = requireTank(helper, EAST_POS);
        AirtightTankBlockEntity south = requireTank(helper, SOUTH_POS);
        AirtightTankBlockEntity southEast = requireTank(helper, SOUTH_EAST_POS);
        helper.assertTrue(controller.isController(), "Expected airtight tank controller was not selected after multiblock formation");

        assertGaugeState(helper, false, controller, east, south, southEast);
        helper.assertTrue(southEast.installTankGauge(), "Installing a tank gauge through a non-controller part failed");
        assertGaugeState(helper, true, controller, east, south, southEast);

        helper.assertTrue(east.removeTankGauge(), "Removing a tank gauge through a non-controller part failed");
        assertGaugeState(helper, false, controller, east, south, southEast);
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 20)
    public static void creativeTankAndCanistersHaveDistinctMeasurementPermissions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos creativePos = new BlockPos(1, 1, 1);
        helper.setBlock(creativePos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity creativeTank = level.getBlockEntity(helper.absolutePos(creativePos));
        helper.assertTrue(creativeTank != null, "Creative airtight tank block entity was not initialized");
        if (creativeTank == null) {
            throw new NullPointerException("Creative airtight tank block entity was not initialized.");
        }

        helper.assertTrue(AirtightTelemetryVisibility.canReadStoragePressure(creativeTank), "Creative tank pressure must be visible");
        helper.assertTrue(!AirtightTelemetryVisibility.canReadStorageCapacity(creativeTank), "Creative tank leaked capacity");

        BlockPos horizontalPos = new BlockPos(2, 1, 3);
        helper.setBlock(horizontalPos, CCBBlocks.HORIZONTAL_AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity horizontal = level.getBlockEntity(helper.absolutePos(horizontalPos));
        helper.assertTrue(horizontal instanceof AirtightTankBlockEntity, "Horizontal airtight tank block entity was not initialized");
        if (!(horizontal instanceof AirtightTankBlockEntity horizontalTank)) {
            throw new IllegalStateException("Horizontal airtight tank block entity missing at " + horizontalPos + '.');
        }

        helper.assertTrue(!AirtightTelemetryVisibility.canReadStoragePressure(horizontalTank), "Ungauged horizontal tank leaked pressure");
        helper.assertTrue(horizontalTank.installTankGauge(), "Horizontal tank gauge could not be installed");
        helper.assertTrue(!AirtightTelemetryVisibility.canReadStorageCapacity(horizontalTank), "Gauged horizontal tank leaked capacity");
        helper.assertTrue(horizontalTank.removeTankGauge(), "Horizontal tank gauge could not be removed");
        helper.assertTrue(!AirtightTelemetryVisibility.canReadStoragePressure(horizontalTank), "Horizontal tank exposed pressure after removing gauge");

        BlockPos canisterPos = new BlockPos(3, 1, 1);
        helper.setBlock(canisterPos, CCBBlocks.GAS_CANISTER_BLOCK.get().defaultBlockState());
        BlockEntity canister = level.getBlockEntity(helper.absolutePos(canisterPos));
        helper.assertTrue(canister != null, "Gas canister block entity was not initialized");
        if (canister == null) {
            throw new NullPointerException("Gas canister block entity was not initialized.");
        }

        helper.assertTrue(!AirtightTelemetryVisibility.canReadStoragePressure(canister), "Gas canister leaked pressure");
        helper.assertTrue(!AirtightTelemetryVisibility.canReadStorageCapacity(canister), "Gas canister leaked capacity");

        BlockPos creativeCanisterPos = new BlockPos(4, 1, 1);
        helper.setBlock(creativeCanisterPos, CCBBlocks.CREATIVE_GAS_CANISTER_BLOCK.get().defaultBlockState());
        BlockEntity creativeCanister = level.getBlockEntity(helper.absolutePos(creativeCanisterPos));
        helper.assertTrue(creativeCanister != null, "Creative gas canister block entity was not initialized");
        if (creativeCanister == null) {
            throw new NullPointerException("Creative gas canister block entity was not initialized.");
        }

        helper.assertTrue(!AirtightTelemetryVisibility.canReadStoragePressure(creativeCanister), "Creative gas canister leaked pressure");
        helper.assertTrue(!AirtightTelemetryVisibility.canReadStorageCapacity(creativeCanister), "Creative gas canister leaked capacity");
        helper.succeed();
    }

    private static void assertGaugeState(GameTestHelper helper, boolean expected, AirtightTankBlockEntity... tanks) {
        for (AirtightTankBlockEntity tank : tanks) {
            helper.assertTrue(tank.hasTankGauge() == expected, "Resolved tank gauge state did not match expected=" + expected + " at " + tank.getBlockPos());
            helper.assertTrue(Boolean.valueOf(expected).equals(tank.getExtraData()), "Local tank gauge attachment was not propagated at " + tank.getBlockPos());
            helper.assertTrue(AirtightTelemetryVisibility.canReadStoragePressure(tank) == expected, "Pressure permission diverged at " + tank.getBlockPos());
            helper.assertTrue(!AirtightTelemetryVisibility.canReadStorageCapacity(tank), "Tank leaked capacity at " + tank.getBlockPos());
        }
    }

    private static AirtightTankBlockEntity placeTank(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        return requireTank(helper, pos);
    }

    private static AirtightTankBlockEntity requireTank(GameTestHelper helper, BlockPos pos) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof AirtightTankBlockEntity, "Airtight tank block entity was not initialized at " + pos);
        if (!(blockEntity instanceof AirtightTankBlockEntity tank)) {
            throw new IllegalStateException("Airtight tank block entity missing at " + pos + '.');
        }

        return tank;
    }
}
