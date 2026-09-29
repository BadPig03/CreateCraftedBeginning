package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasPendingTransferPipeRemovalGameTest {
    private static final String PENDING_TRANSFER_KEY = "PendingTransfer";
    private static final String PENDING_TRANSFER_ORIGIN_KEY = "PendingTransferOrigin";
    private static final String EXTERNAL_HANDLER_ORIGIN = "external_handler";

    private static final long INITIAL_SOURCE_AMOUNT = 8000;
    private static final long PENDING_TRANSFER_AMOUNT = 2500;
    private static final int REMOVAL_RECOVERY_DEADLINE_TICKS = 5;
    private static final int POST_RECOVERY_STABILITY_TICKS = 25;

    private static final BlockPos TANK_POS = new BlockPos(0, 1, 1);
    private static final BlockPos PIPE_POS = new BlockPos(1, 1, 1);

    private GasPendingTransferPipeRemovalGameTest() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 60)
    public static void destroyingPipeWithPendingTransferReturnsCustodyExactlyOnce(GameTestHelper helper) {
        AirtightTankBlockEntity sourceTank = placeTank(helper);
        helper.setBlock(PIPE_POS, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));

        GasTank tank = sourceTank.getTankInventory();
        GasHandler sourceHandler = sourceHandler(helper);
        long insertedAmount = sourceHandler.fill(new GasStack(CCBGases.NATURAL_AIR.get(), INITIAL_SOURCE_AMOUNT), GasAction.EXECUTE);
        helper.assertValueEqual(insertedAmount, INITIAL_SOURCE_AMOUNT, "initial Natural Air inserted into the airtight tank");
        assertTankAmount(helper, tank, INITIAL_SOURCE_AMOUNT, "initial airtight tank");

        GasTransportBehaviour transport = transport(helper);
        GasPipeConnection westConnection = transport.getConnection(Direction.WEST);
        helper.assertTrue(westConnection != null, "Airtight pipe did not create its west connection to the airtight tank capability");
        if (westConnection == null) {
            throw new NullPointerException("Airtight pipe did not create its west connection to the airtight tank capability.");
        }

        GasStack drainedForCustody = sourceHandler.drain(PENDING_TRANSFER_AMOUNT, GasAction.EXECUTE);
        helper.assertTrue(drainedForCustody.is(CCBGases.NATURAL_AIR.get()), "Custody checkpoint drained the wrong gas from the airtight tank");
        helper.assertValueEqual(drainedForCustody.getAmount(), PENDING_TRANSFER_AMOUNT, "Natural Air amount moved into pending custody");
        helper.assertTrue(westConnection.retainPendingTransfer(drainedForCustody), "Gas pipe connection rejected the pending Natural Air custody transfer");
        helper.assertTrue(westConnection.hasPendingTransfer(), "Gas pipe connection did not retain the pending Natural Air custody transfer");

        long sourceAfterDrain = INITIAL_SOURCE_AMOUNT - PENDING_TRANSFER_AMOUNT;
        assertTankAmount(helper, tank, sourceAfterDrain, "airtight tank after pending custody was established");
        GasStack pendingBeforeRemoval = pendingTransfer(helper, westConnection);
        helper.assertTrue(pendingBeforeRemoval.is(CCBGases.NATURAL_AIR.get()), "Serialized active pending custody did not contain Natural Air");
        helper.assertValueEqual(pendingBeforeRemoval.getAmount(), PENDING_TRANSFER_AMOUNT, "serialized active pending custody amount before pipe destruction");
        helper.assertValueEqual(sourceAfterDrain + pendingBeforeRemoval.getAmount(), INITIAL_SOURCE_AMOUNT, "source plus active pending custody conservation checkpoint before pipe destruction");
        helper.assertTrue(helper.getLevel().getCapability(GasCapabilities.BLOCK, helper.absolutePos(TANK_POS), Direction.EAST) != null, "Airtight tank recovery endpoint was unavailable before the pipe was destroyed");

        BlockPos absolutePipePos = helper.absolutePos(PIPE_POS);
        boolean destroyed = helper.getLevel().destroyBlock(absolutePipePos, false);
        helper.assertTrue(destroyed, "Runtime destruction did not remove the airtight pipe holding pending custody");
        helper.assertTrue(helper.getLevel().getBlockState(absolutePipePos).isAir(), "Destroyed airtight pipe position was not air");
        helper.assertTrue(helper.getLevel().getBlockEntity(absolutePipePos) == null, "Destroyed airtight pipe block entity remained in the level");

        int[] phase = new int[1];
        int[] phaseTicks = new int[1];
        helper.onEachTick(() -> {
            phaseTicks[0]++;
            helper.assertTrue(helper.getLevel().getBlockState(absolutePipePos).isAir(), "Destroyed airtight pipe unexpectedly reappeared during pending-transfer recovery");
            helper.assertTrue(helper.getLevel().getBlockEntity(absolutePipePos) == null, "Destroyed airtight pipe block entity unexpectedly reappeared during pending-transfer recovery");
            helper.assertTrue(helper.getLevel().getCapability(GasCapabilities.BLOCK, helper.absolutePos(TANK_POS), Direction.EAST) != null, "Airtight tank recovery endpoint disappeared after the pipe was destroyed");

            long sourceAmount = tank.getStoredAmount();
            helper.assertTrue(sourceAmount <= INITIAL_SOURCE_AMOUNT, "Destroying the pipe duplicated pending gas into the airtight tank");
            if (phase[0] == 0) {
                if (sourceAmount == INITIAL_SOURCE_AMOUNT) {
                    phase[0] = 1;
                    phaseTicks[0] = 0;
                    return;
                }

                helper.assertValueEqual(sourceAmount, sourceAfterDrain, "airtight tank amount while waiting for pipe-removal pending recovery");
                if (phaseTicks[0] > REMOVAL_RECOVERY_DEADLINE_TICKS) {
                    helper.fail("Destroying an airtight pipe discarded its active pending transfer instead of returning custody to the still-available source endpoint");
                }
                return;
            }

            helper.assertValueEqual(sourceAmount, INITIAL_SOURCE_AMOUNT, "exactly-once source amount after pipe-removal pending recovery");
            if (phaseTicks[0] < POST_RECOVERY_STABILITY_TICKS) {
                return;
            }

            helper.succeed();
        });
    }

    private static AirtightTankBlockEntity placeTank(GameTestHelper helper) {
        helper.setBlock(TANK_POS, CCBBlocks.AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TANK_POS));
        helper.assertTrue(blockEntity instanceof AirtightTankBlockEntity, "Airtight tank was not initialized");
        if (!(blockEntity instanceof AirtightTankBlockEntity tank)) {
            throw new IllegalStateException("Airtight tank was not initialized at " + TANK_POS + '.');
        }

        return tank;
    }

    private static GasHandler sourceHandler(GameTestHelper helper) {
        GasHandler handler = helper.getLevel().getCapability(GasCapabilities.BLOCK, helper.absolutePos(TANK_POS), Direction.EAST);
        helper.assertTrue(handler != null, "Airtight tank did not expose the expected gas handler on " + Direction.EAST.getName());
        if (handler == null) {
            throw new NullPointerException("Airtight tank did not expose the expected gas handler on " + Direction.EAST.getName() + '.');
        }

        return handler;
    }

    private static GasTransportBehaviour transport(GameTestHelper helper) {
        GasTransportBehaviour transport = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(PIPE_POS), GasTransportBehaviour.TYPE);
        helper.assertTrue(transport != null, "GasTransportBehaviour was not initialized on the airtight pipe");
        if (transport == null) {
            throw new NullPointerException("GasTransportBehaviour was not initialized on the airtight pipe.");
        }

        return transport;
    }

    private static void assertTankAmount(GameTestHelper helper, GasTank tank, long expectedAmount, String description) {
        GasStack contents = tank.getGasStack();
        if (expectedAmount == 0) {
            helper.assertTrue(contents.isEmpty(), description + " was not empty");
            helper.assertValueEqual(tank.getStoredAmount(), 0L, description + " stored amount");
            return;
        }

        helper.assertTrue(contents.is(CCBGases.NATURAL_AIR.get()), description + " did not contain Natural Air");
        helper.assertValueEqual(contents.getAmount(), expectedAmount, description + " Natural Air amount");
    }

    private static GasStack pendingTransfer(GameTestHelper helper, GasPipeConnection connection) {
        ServerLevel level = helper.getLevel();
        CompoundTag serialized = new CompoundTag();
        connection.write(serialized, level.registryAccess(), false);
        CompoundTag westData = NbtValues.getCompoundOrEmpty(serialized, Direction.WEST.getName());
        helper.assertTrue(westData.contains(PENDING_TRANSFER_KEY, Tag.TAG_COMPOUND), "Active west connection did not serialize its pending transfer before pipe destruction");
        helper.assertTrue(EXTERNAL_HANDLER_ORIGIN.equals(NbtValues.getStringOrDefault(westData, PENDING_TRANSFER_ORIGIN_KEY, "")), "Active pending transfer did not preserve its external-handler recovery origin before pipe destruction");
        return GasStack.parseOptional(level.registryAccess(), NbtValues.getCompoundOrEmpty(westData, PENDING_TRANSFER_KEY));
    }
}
