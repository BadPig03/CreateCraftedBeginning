package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.gas.storage.GasTankLimits;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasRetiredConnectionRecoveryGameTest {
    private static final String RETIRED_CONNECTIONS_KEY = "RetiredGasConnections";
    private static final String PENDING_TRANSFER_KEY = "PendingTransfer";
    private static final String SIDE_KEY = "Side";

    private static final long INITIAL_SOURCE_AMOUNT = 8000;
    private static final long PENDING_TRANSFER_AMOUNT = 2500;
    private static final int RECOVERY_DEADLINE_TICKS = 25;
    private static final int POST_RECOVERY_STABILITY_TICKS = 25;
    private static final int TEMPORARY_BLOCK_TICKS = 5;
    private static final int RESTORED_RECOVERY_DEADLINE_TICKS = 30;

    private static final BlockPos TANK_POS = new BlockPos(0, 1, 1);
    private static final BlockPos PIPE_POS = new BlockPos(1, 1, 1);

    private GasRetiredConnectionRecoveryGameTest() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 70)
    public static void retiredPendingTransferRecoversExactlyOnceAfterFaceClosure(GameTestHelper helper) {
        AirtightTankBlockEntity sourceTank = placeTank(helper);
        helper.setBlock(PIPE_POS, westOpenEncasedPipeState());
        GasTank tank = sourceTank.getTankInventory();
        GasHandler sourceHandler = sourceHandler(helper);
        GasTransportBehaviour transport = transport(helper);

        long insertedAmount = sourceHandler.fill(new GasStack(CCBGases.NATURAL_AIR.get(), INITIAL_SOURCE_AMOUNT), GasAction.EXECUTE);
        helper.assertValueEqual(insertedAmount, INITIAL_SOURCE_AMOUNT, "initial Natural Air inserted into the airtight tank");

        GasPipeConnection westConnection = transport.getConnection(Direction.WEST);
        helper.assertTrue(westConnection != null, "Airtight encased pipe did not create its west connection to the airtight tank capability");
        if (westConnection == null) {
            throw new NullPointerException("Airtight encased pipe did not create its west connection to the airtight tank capability.");
        }

        GasStack drainedForCustody = sourceHandler.drain(PENDING_TRANSFER_AMOUNT, GasAction.EXECUTE);
        helper.assertValueEqual(drainedForCustody.getAmount(), PENDING_TRANSFER_AMOUNT, "Natural Air amount moved into pending custody");
        helper.assertTrue(westConnection.retainPendingTransfer(drainedForCustody), "Gas pipe connection rejected the pending Natural Air custody transfer");
        long sourceAfterDrain = INITIAL_SOURCE_AMOUNT - PENDING_TRANSFER_AMOUNT;
        helper.assertValueEqual(tank.getStoredAmount(), sourceAfterDrain, "airtight tank amount after pending custody was established");

        closeWestFaceWithWrench(helper);
        helper.assertTrue(helper.getLevel().getCapability(GasCapabilities.BLOCK, helper.absolutePos(TANK_POS), Direction.EAST) != null, "Airtight tank recovery endpoint disappeared after the encased-pipe face was closed");

        int[] phase = new int[1];
        int[] phaseTicks = new int[1];
        helper.onEachTick(() -> {
            phaseTicks[0]++;
            assertWestFaceClosed(helper);
            helper.assertTrue(transport.getConnection(Direction.WEST) == null, "Closed west face unexpectedly rebuilt an active connection during retired recovery");
            long sourceAmount = tank.getStoredAmount();
            helper.assertTrue(sourceAmount >= sourceAfterDrain && sourceAmount <= INITIAL_SOURCE_AMOUNT, "Retired pending custody violated source conservation");

            if (phase[0] == 0) {
                if (sourceAmount == INITIAL_SOURCE_AMOUNT) {
                    phase[0] = 1;
                    phaseTicks[0] = 0;
                    return;
                }

                if (phaseTicks[0] > RECOVERY_DEADLINE_TICKS) {
                    helper.fail("Retired pending transfer did not return to the still-available airtight tank endpoint");
                }
                return;
            }

            helper.assertValueEqual(sourceAmount, INITIAL_SOURCE_AMOUNT, "exactly-once source amount after retired connection recovery");
            if (phaseTicks[0] < POST_RECOVERY_STABILITY_TICKS) {
                return;
            }

            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 90)
    public static void retiredPendingTransferWaitsForCapacityThenRecoversExactlyOnce(GameTestHelper helper) {
        AirtightTankBlockEntity sourceTank = placeTank(helper);
        helper.setBlock(PIPE_POS, westOpenEncasedPipeState());
        GasTank tank = sourceTank.getTankInventory();
        GasTankLimits originalLimits = tank.getLimits();
        GasHandler sourceHandler = sourceHandler(helper);
        GasTransportBehaviour transport = transport(helper);

        long insertedAmount = sourceHandler.fill(new GasStack(CCBGases.NATURAL_AIR.get(), INITIAL_SOURCE_AMOUNT), GasAction.EXECUTE);
        helper.assertValueEqual(insertedAmount, INITIAL_SOURCE_AMOUNT, "initial Natural Air inserted before capacity-change recovery");
        GasPipeConnection westConnection = requireWestConnection(helper, transport);

        GasStack drainedForCustody = sourceHandler.drain(PENDING_TRANSFER_AMOUNT, GasAction.EXECUTE);
        helper.assertValueEqual(drainedForCustody.getAmount(), PENDING_TRANSFER_AMOUNT, "Natural Air moved into pending custody before capacity change");
        helper.assertTrue(westConnection.retainPendingTransfer(drainedForCustody), "Gas pipe connection rejected capacity-change pending custody");
        long sourceAfterDrain = INITIAL_SOURCE_AMOUNT - PENDING_TRANSFER_AMOUNT;
        helper.assertValueEqual(tank.getStoredAmount(), sourceAfterDrain, "airtight tank amount after capacity-change custody was established");

        tank.tryReconfigure(GasTankLimits.atReferencePressure(sourceAfterDrain)).requireAccepted();
        helper.assertValueEqual(tank.getMaxAmount(), sourceAfterDrain, "temporary airtight tank capacity while pending recovery is blocked");
        helper.assertValueEqual(tank.getRemainingAmount(), 0L, "temporary airtight tank remaining capacity while pending recovery is blocked");
        closeWestFaceWithWrench(helper);

        int[] phase = new int[1];
        int[] phaseTicks = new int[1];
        helper.onEachTick(() -> {
            phaseTicks[0]++;
            assertWestFaceClosed(helper);
            helper.assertTrue(transport.getConnection(Direction.WEST) == null, "Closed west face unexpectedly rebuilt while capacity blocked retired recovery");

            if (phase[0] == 0) {
                helper.assertValueEqual(tank.getStoredAmount(), sourceAfterDrain, "source amount while retired pending recovery is blocked by capacity");
                assertRetiredPendingAmount(helper, transport, PENDING_TRANSFER_AMOUNT, "retired pending amount while source capacity is unavailable");
                if (phaseTicks[0] < TEMPORARY_BLOCK_TICKS) {
                    return;
                }

                tank.tryReconfigure(originalLimits).requireAccepted();
                helper.assertTrue(tank.getRemainingAmount() >= PENDING_TRANSFER_AMOUNT, "Restored airtight tank limits did not provide enough recovery capacity");
                phase[0] = 1;
                phaseTicks[0] = 0;
                return;
            }

            if (phase[0] == 1) {
                long sourceAmount = tank.getStoredAmount();
                helper.assertTrue(sourceAmount >= sourceAfterDrain && sourceAmount <= INITIAL_SOURCE_AMOUNT, "Capacity-restored retired recovery violated source conservation");
                if (sourceAmount == INITIAL_SOURCE_AMOUNT) {
                    assertRetiredPendingAmount(helper, transport, 0, "retired pending amount after capacity-restored recovery");
                    phase[0] = 2;
                    phaseTicks[0] = 0;
                    return;
                }

                assertRetiredPendingAmount(helper, transport, PENDING_TRANSFER_AMOUNT, "retired pending amount before capacity-restored recovery completes");
                if (phaseTicks[0] > RESTORED_RECOVERY_DEADLINE_TICKS) {
                    helper.fail("Retired pending transfer did not recover after source capacity became available again");
                }
                return;
            }

            helper.assertValueEqual(tank.getStoredAmount(), INITIAL_SOURCE_AMOUNT, "exactly-once source amount after capacity-restored retired recovery");
            assertRetiredPendingAmount(helper, transport, 0, "retired pending amount during post-capacity recovery stability");
            if (phaseTicks[0] >= POST_RECOVERY_STABILITY_TICKS) {
                helper.succeed();
            }
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 90)
    public static void retiredPendingTransferSurvivesTemporaryCapabilityLossAndRecoversWhenCapabilityReturns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeTank(helper);
        helper.setBlock(PIPE_POS, westOpenEncasedPipeState());
        GasHandler sourceHandler = sourceHandler(helper);
        GasTransportBehaviour transport = transport(helper);

        long insertedAmount = sourceHandler.fill(new GasStack(CCBGases.NATURAL_AIR.get(), PENDING_TRANSFER_AMOUNT), GasAction.EXECUTE);
        helper.assertValueEqual(insertedAmount, PENDING_TRANSFER_AMOUNT, "Natural Air inserted before temporary capability loss");
        GasPipeConnection westConnection = requireWestConnection(helper, transport);
        GasStack drainedForCustody = sourceHandler.drain(PENDING_TRANSFER_AMOUNT, GasAction.EXECUTE);
        helper.assertValueEqual(drainedForCustody.getAmount(), PENDING_TRANSFER_AMOUNT, "Natural Air moved into pending custody before capability loss");
        helper.assertTrue(westConnection.retainPendingTransfer(drainedForCustody), "Gas pipe connection rejected capability-loss pending custody");

        boolean sourceDestroyed = level.destroyBlock(helper.absolutePos(TANK_POS), false);
        helper.assertTrue(sourceDestroyed, "Empty source tank could not be destroyed for temporary capability-loss recovery");
        helper.assertTrue(level.getCapability(GasCapabilities.BLOCK, helper.absolutePos(TANK_POS), Direction.EAST) == null, "Source gas capability remained available after its block was destroyed");
        closeWestFaceWithWrench(helper);

        AirtightTankBlockEntity[] replacementTank = new AirtightTankBlockEntity[1];
        int[] phase = new int[1];
        int[] phaseTicks = new int[1];
        helper.onEachTick(() -> {
            phaseTicks[0]++;
            assertWestFaceClosed(helper);
            helper.assertTrue(transport.getConnection(Direction.WEST) == null, "Closed west face unexpectedly rebuilt during temporary capability-loss recovery");

            if (phase[0] == 0) {
                helper.assertTrue(level.getCapability(GasCapabilities.BLOCK, helper.absolutePos(TANK_POS), Direction.EAST) == null, "Source gas capability unexpectedly reappeared before replacement");
                assertRetiredPendingAmount(helper, transport, PENDING_TRANSFER_AMOUNT, "retired pending amount while source capability is absent");
                if (phaseTicks[0] < TEMPORARY_BLOCK_TICKS) {
                    return;
                }

                replacementTank[0] = placeTank(helper);
                helper.assertTrue(level.getCapability(GasCapabilities.BLOCK, helper.absolutePos(TANK_POS), Direction.EAST) != null, "Replacement source did not restore its gas capability");
                phase[0] = 1;
                phaseTicks[0] = 0;
                return;
            }

            GasTank replacementInventory = replacementTank[0].getTankInventory();
            if (phase[0] == 1) {
                long replacementAmount = replacementInventory.getStoredAmount();
                helper.assertTrue(replacementAmount >= 0 && replacementAmount <= PENDING_TRANSFER_AMOUNT, "Capability-restored retired recovery duplicated pending gas");
                if (replacementAmount == PENDING_TRANSFER_AMOUNT) {
                    assertRetiredPendingAmount(helper, transport, 0, "retired pending amount after capability-restored recovery");
                    phase[0] = 2;
                    phaseTicks[0] = 0;
                    return;
                }

                assertRetiredPendingAmount(helper, transport, PENDING_TRANSFER_AMOUNT, "retired pending amount before capability-restored recovery completes");
                if (phaseTicks[0] > RESTORED_RECOVERY_DEADLINE_TICKS) {
                    helper.fail("Retired pending transfer did not recover after the source capability returned");
                }
                return;
            }

            helper.assertValueEqual(replacementInventory.getStoredAmount(), PENDING_TRANSFER_AMOUNT, "exactly-once replacement source amount after capability-restored recovery");
            assertRetiredPendingAmount(helper, transport, 0, "retired pending amount during post-capability recovery stability");
            if (phaseTicks[0] >= POST_RECOVERY_STABILITY_TICKS) {
                helper.succeed();
            }
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

    private static BlockState westOpenEncasedPipeState() {
        return CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().defaultBlockState().setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST), true);
    }

    private static GasPipeConnection requireWestConnection(GameTestHelper helper, GasTransportBehaviour transport) {
        GasPipeConnection connection = transport.getConnection(Direction.WEST);
        helper.assertTrue(connection != null, "Gas transport did not create its west connection to the airtight tank capability");
        if (connection == null) {
            throw new NullPointerException("Gas transport did not create its west connection to the airtight tank capability.");
        }

        return connection;
    }

    private static void assertRetiredPendingAmount(GameTestHelper helper, GasTransportBehaviour transport, long expectedAmount, String description) {
        ServerLevel level = helper.getLevel();
        CompoundTag serialized = new CompoundTag();
        transport.write(serialized, level.registryAccess(), false);
        ListTag retiredConnections = serialized.getList(RETIRED_CONNECTIONS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < retiredConnections.size(); index++) {
            CompoundTag retiredConnection = retiredConnections.getCompound(index);
            if (retiredConnection.getInt(SIDE_KEY) != Direction.WEST.get3DDataValue()) {
                continue;
            }

            GasStack pending = GasStack.parseOptional(level.registryAccess(), NbtValues.getCompoundOrEmpty(retiredConnection, PENDING_TRANSFER_KEY));
            helper.assertValueEqual(pending.getAmount(), expectedAmount, description);
            return;
        }

        helper.assertValueEqual(expectedAmount, 0L, description + " (retired connection was absent)");
    }

    private static void closeWestFaceWithWrench(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        BlockPos absolutePos = helper.absolutePos(PIPE_POS);
        BlockState state = helper.getLevel().getBlockState(absolutePos);
        helper.assertTrue(state.is(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get()), "Airtight encased pipe was missing before its west face was closed");
        helper.assertTrue(state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST)), "Airtight encased pipe west face was already closed before the retirement transition");
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(absolutePos).add(-0.5, 0, 0), Direction.WEST, absolutePos, false);
        InteractionResult result = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().onWrenched(state, new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
        helper.assertTrue(result == InteractionResult.SUCCESS, "Wrenching the airtight encased pipe west face closed did not succeed");
        assertWestFaceClosed(helper);
    }

    private static void assertWestFaceClosed(GameTestHelper helper) {
        BlockState state = helper.getLevel().getBlockState(helper.absolutePos(PIPE_POS));
        helper.assertTrue(state.is(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get()), "Airtight encased pipe disappeared during retired connection recovery");
        helper.assertTrue(!state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST)), "Airtight encased pipe west face reopened during retired connection recovery");
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
        helper.assertTrue(transport != null, "GasTransportBehaviour was not initialized on the airtight encased pipe");
        if (transport == null) {
            throw new NullPointerException("GasTransportBehaviour was not initialized on the airtight encased pipe.");
        }

        return transport;
    }
}
