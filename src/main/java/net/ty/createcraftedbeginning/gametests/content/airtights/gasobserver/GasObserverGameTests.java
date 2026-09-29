package net.ty.createcraftedbeginning.gametests.content.airtights.gasobserver;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilters.GasFilterData;
import net.ty.createcraftedbeginning.content.airtights.gasobserver.GasObserverBehaviour;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.NoSuchElementException;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasObserverGameTests {
    private GasObserverGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void pipeDetectionRequiresInboundFlowAndRefreshesFilter(GameTestHelper helper) {
        SmartObserverBlockEntity observer = observer(helper);
        GasObserverBehaviour detection = (GasObserverBehaviour) observer.getAllBehaviours().stream().filter(GasObserverBehaviour.class::isInstance).findFirst().orElseThrow(() -> new NoSuchElementException("Expected the gas observer behaviour in test 'pipeDetectionRequiresInboundFlowAndRefreshesFilter'."));
        FilteringBehaviour filtering = observer.getBehaviour(FilteringBehaviour.TYPE);
        BlockPos target = new BlockPos(2, 1, 1);
        helper.setBlock(target, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        GasTransportBehaviour transport = BlockEntityBehaviour.get(helper.getBlockEntity(target), GasTransportBehaviour.TYPE);
        GasPipeConnection connection = transport.getConnection(Direction.EAST);
        helper.assertTrue(connection != null, "Pipe connection missing");
        if (connection == null) {
            throw new NullPointerException("Pipe connection missing.");
        }

        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 100);
        connection.setFlowState(gas, false, 10);
        helper.assertTrue(!detection.hasMatchingGas(filtering), "Outbound flow triggered observer");
        connection.setFlowState(gas, true, 10);
        helper.assertTrue(detection.hasMatchingGas(filtering), "Inbound flow was ignored");
        filtering.setFilter(filter(new GasStack(CCBGases.ETHEREAL_AIR.get(), 1)));
        helper.assertTrue(!detection.hasMatchingGas(filtering), "Changed filter reused previous match");
        filtering.setFilter(filter(gas));
        helper.assertTrue(detection.hasMatchingGas(filtering), "Matching gas filter rejected flow");
        filtering.setFilter(ItemStack.EMPTY);
        helper.assertTrue(detection.hasMatchingGas(filtering), "Clearing filter did not restore detection");
        connection.clearFlowState();
        helper.assertTrue(!detection.hasMatchingGas(filtering), "Stopped flow still triggered observer");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void realObserverTickDetectsTankWithoutDrainingIt(GameTestHelper helper) {
        SmartObserverBlockEntity observer = observer(helper);
        BlockPos target = new BlockPos(2, 1, 1);
        helper.setBlock(target, CCBBlocks.AIRTIGHT_TANK_BLOCK.get());
        AirtightTankBlockEntity tank = helper.getBlockEntity(target);
        GasTank handler = tank.getTankInventory();
        handler.fill(new GasStack(CCBGases.NATURAL_AIR.get(), 100), GasAction.EXECUTE);
        GasObserverBehaviour detection = (GasObserverBehaviour) observer.getAllBehaviours().stream().filter(GasObserverBehaviour.class::isInstance).findFirst().orElseThrow(() -> new NoSuchElementException("Expected the gas observer behaviour in test 'realObserverTickDetectsTankWithoutDrainingIt'."));
        detection.findNewCapability();
        FilteringBehaviour filtering = observer.getBehaviour(FilteringBehaviour.TYPE);
        filtering.setFilter(filter(new GasStack(CCBGases.ETHEREAL_AIR.get(), 1)));
        helper.assertTrue(!detection.hasMatchingGas(filtering), "Tank bypassed gas filter");
        filtering.setFilter(ItemStack.EMPTY);
        observer.tick();
        helper.assertValueEqual(observer.turnOffTicks, 6, "Real observer activation duration");
        helper.assertValueEqual(handler.getGasInTank(0).getAmount(), 100L, "Gas remaining after observation");
        helper.succeed();
    }

    private static ItemStack filter(GasStack gas) {
        ItemStack item = new ItemStack(CCBItems.GAS_FILTER.get());
        item.set(CCBDataComponents.GAS_FILTER_DATA, new GasFilterData(false, true, List.of(gas)));
        return item;
    }

    private static SmartObserverBlockEntity observer(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, AllBlocks.SMART_OBSERVER.get().defaultBlockState().setValue(DirectedDirectionalBlock.FACING, Direction.EAST));
        return helper.getBlockEntity(pos);
    }
}
