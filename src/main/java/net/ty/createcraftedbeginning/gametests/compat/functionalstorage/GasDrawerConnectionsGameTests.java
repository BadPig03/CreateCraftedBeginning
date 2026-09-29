package net.ty.createcraftedbeginning.gametests.compat.functionalstorage;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.util.ConnectedDrawers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerBlockEntity;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerConnections;
import net.ty.createcraftedbeginning.compat.functionalstorage.access.GasConnectedDrawersAccess;
import net.ty.createcraftedbeginning.compat.functionalstorage.access.GasControllerAccess;
import net.ty.createcraftedbeginning.compat.functionalstorage.registry.CCBFunctionalStorageBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@PrefixGameTestTemplate(false)
public final class GasDrawerConnectionsGameTests {
    private GasDrawerConnectionsGameTests() {
    }

    @GameTest(templateNamespace = CCBAPI.MOD_ID, template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void rebuildRefreshesControllerAndRemovesDeletedDrawers(GameTestHelper helper) {
        StorageControllerTile<?> controller = controller(helper);
        BlockPos drawerPos = new BlockPos(2, 1, 1);
        helper.setBlock(drawerPos, CCBFunctionalStorageBlocks.GAS_DRAWER_2_BLOCK.get());
        GasDrawerBlockEntity drawer = helper.getBlockEntity(drawerPos);
        ConnectedDrawers connected = controller.getConnectedDrawers();
        connected.setLevel(helper.getLevel());
        connected.getConnectedDrawers().add(drawer.getBlockPos().asLong());
        connected.rebuild();
        GasConnectedDrawersAccess access = (GasConnectedDrawersAccess) connected;
        helper.assertTrue(access.ccb$getGasHandlers().equals(List.of(drawer.getGasHandler())), "Real rebuild did not collect gas drawer");
        GasHandler handler = ((GasControllerAccess) controller).ccb$getGasHandler();
        if (handler == null) {
            throw new NullPointerException("Gas controller handler is missing after rebuilding drawer connections.");
        }

        helper.assertValueEqual(handler.getTanks(), 2, "Controller tanks after rebuild");
        helper.setBlock(drawerPos, Blocks.AIR);
        connected.rebuild();
        helper.assertTrue(access.ccb$getGasHandlers().isEmpty(), "Removed drawer remained connected");
        helper.assertValueEqual(handler.getTanks(), 0, "Controller tanks after removal");
        helper.succeed();
    }

    @GameTest(templateNamespace = CCBAPI.MOD_ID, template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void inclusionKeepsExistingOrderAndDoesNotDuplicateEntries(GameTestHelper helper) {
        StorageControllerTile<?> controller = controller(helper);
        BlockPos first = new BlockPos(2, 1, 1);
        BlockPos second = new BlockPos(3, 1, 1);
        helper.setBlock(first, CCBFunctionalStorageBlocks.GAS_DRAWER_2_BLOCK.get());
        helper.setBlock(second, CCBFunctionalStorageBlocks.GAS_DRAWER_2_BLOCK.get());
        long firstPos = helper.absolutePos(first).asLong();
        long secondPos = helper.absolutePos(second).asLong();
        List<Long> valid = new ArrayList<>(List.of(secondPos));
        List<Long> connected = List.of(firstPos, secondPos, firstPos);
        GasDrawerConnections connections = new GasDrawerConnections();
        connections.beginRebuild(connected);
        connections.include(helper.getLevel(), controller, valid);
        helper.assertTrue(valid.equals(List.of(secondPos, firstPos)), "Inclusion reordered or duplicated positions before the original sort");
        helper.succeed();
    }

    @GameTest(templateNamespace = CCBAPI.MOD_ID, template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void unloadedAndAbsentWorldDoNotResolveGasHandlers(GameTestHelper helper) {
        StorageControllerTile<?> controller = controller(helper);
        ServerLevel level = helper.getLevel();
        BlockPos unloaded = controller.getBlockPos().offset(1000000, 0, 1000000);
        helper.assertTrue(!level.isLoaded(unloaded), "Test target chunk was already loaded");
        List<Long> connected = List.of(unloaded.asLong());
        List<Long> valid = new ArrayList<>();
        GasDrawerConnections connections = new GasDrawerConnections();
        connections.beginRebuild(connected);
        connections.include(level, controller, valid);
        helper.assertTrue(valid.isEmpty(), "Out-of-range position was included");
        connections.finishRebuild(level, controller, connected);
        helper.assertTrue(connections.getHandlers().isEmpty(), "Unloaded chunk produced a handler");
        helper.assertTrue(!level.isLoaded(unloaded), "Collection loaded an absent chunk");
        connections.beginRebuild(connected);
        connections.include(null, controller, valid);
        connections.finishRebuild(null, controller, connected);
        helper.assertTrue(valid.isEmpty() && connections.getHandlers().isEmpty(), "Absent world produced a handler");
        helper.succeed();
    }

    private static StorageControllerTile<?> controller(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, FunctionalStorage.DRAWER_CONTROLLER.block().get());
        return helper.getBlockEntity(pos);
    }
}
