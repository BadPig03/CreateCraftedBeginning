package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.GasFilteringBehaviour;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowDirection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowState;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasSmartAirtightPipeFilterGameTest {
    private static final long SOURCE_PRESSURE_PA = GasPressure.pascals(2);

    private static final BlockPos MATCHING_SOURCE_POS = new BlockPos(0, 1, 1);
    private static final BlockPos MATCHING_PIPE_POS = new BlockPos(1, 1, 1);
    private static final BlockPos BLOCKED_SOURCE_POS = new BlockPos(0, 1, 4);
    private static final BlockPos BLOCKED_PIPE_POS = new BlockPos(1, 1, 4);

    private static final String FILTER_NBT_KEY = "Filter";

    private GasSmartAirtightPipeFilterGameTest() {
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 120)
    public static void smartAirtightPipeOnlyPassesMatchingGas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AirtightTankBlockEntity matchingSource = placeFiniteSource(helper, MATCHING_SOURCE_POS, CCBGases.NATURAL_AIR.get());
        AirtightTankBlockEntity blockedSource = placeFiniteSource(helper, BLOCKED_SOURCE_POS, CCBGases.ULTRAWARM_AIR.get());
        long matchingInitialAmount = matchingSource.getTankInventory().getStoredAmount();
        long blockedInitialAmount = blockedSource.getTankInventory().getStoredAmount();

        helper.setBlock(MATCHING_PIPE_POS, smartPipeState());
        helper.setBlock(BLOCKED_PIPE_POS, smartPipeState());

        GasFilteringBehaviour matchingFilter = configureNaturalAirCanisterFilter(helper, MATCHING_PIPE_POS);
        GasFilteringBehaviour blockedFilter = configureNaturalAirCanisterFilter(helper, BLOCKED_PIPE_POS);

        helper.assertTrue(matchingFilter.test(new GasStack(CCBGases.NATURAL_AIR.get(), 1)), "Matching smart airtight pipe filter rejected Natural Air");
        helper.assertTrue(!matchingFilter.test(new GasStack(CCBGases.ULTRAWARM_AIR.get(), 1)), "Matching smart airtight pipe filter accepted Ultrawarm Air");
        helper.assertTrue(blockedFilter.test(new GasStack(CCBGases.NATURAL_AIR.get(), 1)), "Blocked smart airtight pipe filter rejected Natural Air");
        helper.assertTrue(!blockedFilter.test(new GasStack(CCBGases.ULTRAWARM_AIR.get(), 1)), "Blocked smart airtight pipe filter accepted Ultrawarm Air");

        helper.succeedWhen(() -> {
            GasTransportBehaviour matchingTransport = BlockEntityBehaviour.get(level, helper.absolutePos(MATCHING_PIPE_POS), GasTransportBehaviour.TYPE);
            GasTransportBehaviour blockedTransport = BlockEntityBehaviour.get(level, helper.absolutePos(BLOCKED_PIPE_POS), GasTransportBehaviour.TYPE);
            helper.assertTrue(matchingTransport != null, "Matching smart airtight pipe transport behaviour was not initialized");
            if (matchingTransport == null) {
                throw new NullPointerException("Matching smart airtight pipe transport behaviour was not initialized.");
            }

            helper.assertTrue(blockedTransport != null, "Blocked smart airtight pipe transport behaviour was not initialized");
            if (blockedTransport == null) {
                throw new NullPointerException("Blocked smart airtight pipe transport behaviour was not initialized.");
            }

            FlowState matchingInlet = matchingTransport.getFlowState(Direction.WEST);
            FlowState matchingOutlet = matchingTransport.getFlowState(Direction.EAST);
            helper.assertTrue(matchingInlet != null, "Natural Air did not enter the matching smart airtight pipe");
            if (matchingInlet == null) {
                throw new NullPointerException("Natural Air did not enter the matching smart airtight pipe.");
            }

            helper.assertTrue(matchingOutlet != null, "Natural Air did not leave the matching smart airtight pipe toward atmosphere");
            if (matchingOutlet == null) {
                throw new NullPointerException("Natural Air did not leave the matching smart airtight pipe toward atmosphere.");
            }

            helper.assertTrue(matchingInlet.direction() == FlowDirection.INBOUND, "Matching smart airtight pipe west face was not inbound");
            helper.assertTrue(matchingOutlet.direction() == FlowDirection.OUTBOUND, "Matching smart airtight pipe east face was not outbound");
            helper.assertTrue(matchingInlet.flowRate() > 0, "Matching smart airtight pipe flow rate was not positive");
            helper.assertValueEqual(matchingOutlet.flowRate(), matchingInlet.flowRate(), "matching smart airtight pipe face flow rate");
            helper.assertValueEqual(matchingTransport.getThroughputFlowRate(), matchingInlet.flowRate(), "matching smart airtight pipe throughput");
            helper.assertTrue(matchingInlet.gas().is(CCBGases.NATURAL_AIR.get()), "Matching smart airtight pipe inlet gas was not Natural Air");
            helper.assertTrue(matchingOutlet.gas().is(CCBGases.NATURAL_AIR.get()), "Matching smart airtight pipe outlet gas was not Natural Air");
            helper.assertTrue(matchingSource.getTankInventory().getStoredAmount() < matchingInitialAmount, "Matching Natural Air source did not lose gas through the smart airtight pipe");

            helper.assertTrue(blockedTransport.getFlowState(Direction.WEST) == null, "Ultrawarm Air entered a smart airtight pipe filtered for Natural Air");
            helper.assertTrue(blockedTransport.getFlowState(Direction.EAST) == null, "Filtered Ultrawarm Air produced outlet flow through the smart airtight pipe");
            helper.assertValueEqual(blockedTransport.getThroughputFlowRate(), 0L, "blocked smart airtight pipe throughput");
            helper.assertValueEqual(blockedSource.getTankInventory().getStoredAmount(), blockedInitialAmount, "blocked Ultrawarm Air source amount");
            helper.assertTrue(blockedSource.getTankInventory().getGasStack().is(CCBGases.ULTRAWARM_AIR.get()), "Blocked source gas changed while rejected by the smart airtight pipe");
            helper.assertTrue(level.getBlockState(helper.absolutePos(BLOCKED_PIPE_POS)).is(CCBBlocks.SMART_AIRTIGHT_PIPE_BLOCK.get()), "Filtering destroyed the blocked smart airtight pipe instead of rejecting the gas");
        });
    }

    private static AirtightTankBlockEntity placeFiniteSource(GameTestHelper helper, BlockPos pos, Gas gas) {
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof AirtightTankBlockEntity, "Airtight tank block entity was not initialized at " + pos);
        if (!(blockEntity instanceof AirtightTankBlockEntity tank)) {
            throw new IllegalStateException("Airtight tank block entity missing at " + pos + '.');
        }

        long volume = tank.getTankInventory().getVolume();
        helper.assertTrue(volume > 0, "Smart pipe source tank reported zero physical volume at " + pos);
        helper.assertTrue(tank.getTankInventory().getMaxPressurePa() >= SOURCE_PRESSURE_PA, "Smart pipe source tank pressure rating was below the 2 atm test pressure at " + pos);

        long initialAmount = GasPressure.amount(volume, SOURCE_PRESSURE_PA);
        tank.getTankInventory().tryReplaceContents(new GasStack(gas, initialAmount)).requireAccepted();
        helper.assertValueEqual(tank.getTankInventory().getStoredAmount(), initialAmount, "smart pipe source amount at " + pos);
        helper.assertValueEqual(tank.getTankInventory().getPressurePa(), SOURCE_PRESSURE_PA, "smart pipe source pressure at " + pos);
        return tank;
    }

    private static GasFilteringBehaviour configureNaturalAirCanisterFilter(GameTestHelper helper, BlockPos pipePos) {
        ServerLevel level = helper.getLevel();
        GasFilteringBehaviour filter = BlockEntityBehaviour.get(level, helper.absolutePos(pipePos), GasFilteringBehaviour.TYPE);
        helper.assertTrue(filter != null, "Smart airtight pipe filtering behaviour was not initialized at " + pipePos);
        if (filter == null) {
            throw new NullPointerException("Smart airtight pipe filtering behaviour was not initialized at " + pipePos + '.');
        }

        ItemStack filterCanister = new ItemStack(CCBItems.GAS_CANISTER.asItem());
        filterCanister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.NATURAL_AIR.get(), 1));

        CompoundTag filterTag = new CompoundTag();
        filterTag.put(FILTER_NBT_KEY, filterCanister.saveOptional(level.registryAccess()));
        filter.read(filterTag, level.registryAccess(), false);

        helper.assertTrue(ItemStack.isSameItemSameComponents(filter.getFilter(), filterCanister), "Smart airtight pipe did not retain the Natural Air canister filter at " + pipePos);
        return filter;
    }

    private static BlockState smartPipeState() {
        return CCBBlocks.SMART_AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X);
    }
}
