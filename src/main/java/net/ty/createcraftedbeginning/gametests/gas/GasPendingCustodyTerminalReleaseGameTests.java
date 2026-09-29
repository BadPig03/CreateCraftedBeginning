package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.RotatedPillarBlock;
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
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasPendingCustodyTerminalReleaseGameTests {
    private static final String RETIRED_CONNECTIONS_KEY = "RetiredGasConnections";
    private static final String PENDING_TRANSFER_KEY = "PendingTransfer";
    private static final String SIDE_KEY = "Side";

    private static final long TERMINAL_RELEASE_AMOUNT = GasReleaseService.EFFECT_INTERVAL + 500;
    private static final long PARTIAL_PENDING_AMOUNT = GasReleaseService.EFFECT_INTERVAL + 500;
    private static final long PARTIAL_RECOVERY_CAPACITY = 501;
    private static final long PARTIAL_RELEASE_REMAINDER = PARTIAL_PENDING_AMOUNT - PARTIAL_RECOVERY_CAPACITY;
    private static final int RETIRED_STABILITY_TICKS = 25;

    private static final BlockPos TANK_POS = new BlockPos(2, 1, 2);
    private static final BlockPos PIPE_POS = new BlockPos(3, 1, 2);
    private static final BlockPos EFFECT_POS = new BlockPos(3, 1, 3);

    private GasPendingCustodyTerminalReleaseGameTests() {
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 20)
    public static void unavailableSourceReleasesPendingCustodyWhenOwnerIsDestroyed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeTank(helper);
        helper.setBlock(PIPE_POS, axisPipeState());

        GasHandler sourceHandler = sourceHandler(helper);
        GasPipeConnection westConnection = westConnection(helper);
        long insertedAmount = sourceHandler.fill(new GasStack(CCBGases.NATURAL_AIR.get(), TERMINAL_RELEASE_AMOUNT), GasAction.EXECUTE);
        helper.assertValueEqual(insertedAmount, TERMINAL_RELEASE_AMOUNT, "Natural Air inserted before terminal custody release");

        GasStack pending = sourceHandler.drain(TERMINAL_RELEASE_AMOUNT, GasAction.EXECUTE);
        helper.assertValueEqual(pending.getAmount(), TERMINAL_RELEASE_AMOUNT, "Natural Air moved into terminal pending custody");
        helper.assertTrue(westConnection.retainPendingTransfer(pending), "Gas pipe connection rejected terminal pending custody");

        Piglin piglin = helper.spawn(EntityType.PIGLIN, EFFECT_POS);
        helper.assertTrue(!piglin.hasEffect(CCBMobEffects.ZOMBIFICATION), "Terminal release piglin started with the zombification effect");

        boolean tankDestroyed = level.destroyBlock(helper.absolutePos(TANK_POS), false);
        helper.assertTrue(tankDestroyed, "Empty source tank could not be destroyed before terminal custody fallback");
        helper.assertTrue(!piglin.hasEffect(CCBMobEffects.ZOMBIFICATION), "Destroying the empty source tank unexpectedly released Natural Air");

        boolean pipeDestroyed = level.destroyBlock(helper.absolutePos(PIPE_POS), false);
        helper.assertTrue(pipeDestroyed, "Pending-custody pipe could not be destroyed");
        helper.assertTrue(level.getBlockEntity(helper.absolutePos(PIPE_POS)) == null, "Pending-custody pipe block entity remained after destruction");
        helper.assertTrue(piglin.hasEffect(CCBMobEffects.ZOMBIFICATION), "Unrecoverable pending custody was not released radially when its owner was destroyed");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 20)
    public static void terminalFinalizationReturnsOnlyUnrecoveredRemainderExactlyOnce(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AirtightTankBlockEntity sourceTank = placeTank(helper);
        helper.setBlock(PIPE_POS, axisPipeState());

        GasTank tank = sourceTank.getTankInventory();
        long tankCapacity = tank.getMaxAmount();
        helper.assertTrue(tankCapacity >= PARTIAL_PENDING_AMOUNT, "Airtight tank capacity is too small for partial terminal recovery test");
        tank.tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), tankCapacity)).requireAccepted();

        GasHandler sourceHandler = sourceHandler(helper);
        GasPipeConnection westConnection = westConnection(helper);
        GasStack pending = sourceHandler.drain(PARTIAL_PENDING_AMOUNT, GasAction.EXECUTE);
        helper.assertValueEqual(pending.getAmount(), PARTIAL_PENDING_AMOUNT, "Natural Air moved into partial terminal pending custody");
        helper.assertTrue(westConnection.retainPendingTransfer(pending), "Gas pipe connection rejected partial terminal pending custody");

        long refillBeforeRemoval = PARTIAL_PENDING_AMOUNT - PARTIAL_RECOVERY_CAPACITY;
        long refilledAmount = sourceHandler.fill(new GasStack(CCBGases.NATURAL_AIR.get(), refillBeforeRemoval), GasAction.EXECUTE);
        helper.assertValueEqual(refilledAmount, refillBeforeRemoval, "Natural Air refilled before partial terminal recovery");
        helper.assertValueEqual(tank.getStoredAmount(), tankCapacity - PARTIAL_RECOVERY_CAPACITY, "airtight tank amount before partial terminal recovery");

        GasStack unrecovered = westConnection.finalizePendingTransferForOwnerRemoval(level, helper.absolutePos(PIPE_POS));
        helper.assertTrue(unrecovered.is(CCBGases.NATURAL_AIR.get()), "Partial terminal finalization returned the wrong gas");
        helper.assertValueEqual(unrecovered.getAmount(), PARTIAL_RELEASE_REMAINDER, "unrecovered terminal remainder after partial source recovery");
        helper.assertValueEqual(tank.getStoredAmount(), tankCapacity, "airtight tank amount after partial terminal source recovery");
        helper.assertTrue(!westConnection.hasPendingTransfer(), "Connection retained custody after terminal finalization handed off its remainder");

        GasStack secondFinalization = westConnection.finalizePendingTransferForOwnerRemoval(level, helper.absolutePos(PIPE_POS));
        helper.assertTrue(secondFinalization.isEmpty(), "Repeated terminal finalization handed off custody more than once");
        helper.assertValueEqual(tank.getStoredAmount(), tankCapacity, "airtight tank amount after repeated terminal finalization");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 80)
    public static void detachedCustodyWaitsWhileOwnerLivesThenReleasesOnOwnerDestruction(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeTank(helper);
        helper.setBlock(PIPE_POS, CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().defaultBlockState().setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST), true));

        GasHandler sourceHandler = sourceHandler(helper);
        GasTransportBehaviour transport = transport(helper);
        GasPipeConnection westConnection = requireWestConnection(helper, transport);
        long insertedAmount = sourceHandler.fill(new GasStack(CCBGases.NATURAL_AIR.get(), TERMINAL_RELEASE_AMOUNT), GasAction.EXECUTE);
        helper.assertValueEqual(insertedAmount, TERMINAL_RELEASE_AMOUNT, "Natural Air inserted before detached custody test");

        GasStack pending = sourceHandler.drain(TERMINAL_RELEASE_AMOUNT, GasAction.EXECUTE);
        helper.assertValueEqual(pending.getAmount(), TERMINAL_RELEASE_AMOUNT, "Natural Air moved into detached pending custody");
        helper.assertTrue(westConnection.retainPendingTransfer(pending), "Gas pipe connection rejected detached pending custody");

        Piglin piglin = helper.spawn(EntityType.PIGLIN, EFFECT_POS);
        piglin.setNoAi(true);
        helper.assertTrue(!piglin.hasEffect(CCBMobEffects.ZOMBIFICATION), "Detached custody piglin started with the zombification effect");

        boolean tankDestroyed = level.destroyBlock(helper.absolutePos(TANK_POS), false);
        helper.assertTrue(tankDestroyed, "Empty source tank could not be destroyed before connection retirement");
        helper.assertTrue(!piglin.hasEffect(CCBMobEffects.ZOMBIFICATION), "Destroying the empty source tank unexpectedly released detached custody");

        closeWestFaceWithWrench(helper);

        int[] phase = new int[1];
        int[] phaseTicks = new int[1];
        helper.onEachTick(() -> {
            phaseTicks[0]++;
            helper.assertTrue(!piglin.hasEffect(CCBMobEffects.ZOMBIFICATION), "Detached pending custody leaked while its owner block was still alive");

            if (phase[0] == 0) {
                if (transport.getConnection(Direction.WEST) != null) {
                    if (phaseTicks[0] > 10) {
                        helper.fail("Closed west connection did not retire while holding unrecoverable pending custody");
                    }
                    return;
                }

                GasStack retiredPending = retiredPendingTransfer(helper, transport);
                helper.assertTrue(retiredPending.is(CCBGases.NATURAL_AIR.get()), "Retired connection serialized the wrong pending gas");
                helper.assertValueEqual(retiredPending.getAmount(), TERMINAL_RELEASE_AMOUNT, "retired pending custody amount while owner remains alive");
                phase[0] = 1;
                phaseTicks[0] = 0;
                return;
            }

            if (phaseTicks[0] < RETIRED_STABILITY_TICKS) {
                GasStack retiredPending = retiredPendingTransfer(helper, transport);
                helper.assertValueEqual(retiredPending.getAmount(), TERMINAL_RELEASE_AMOUNT, "detached custody amount during owner-alive recovery backoff");
                return;
            }

            boolean pipeDestroyed = level.destroyBlock(helper.absolutePos(PIPE_POS), false);
            helper.assertTrue(pipeDestroyed, "Owner pipe could not be destroyed after detached custody stability period");
            helper.assertTrue(piglin.hasEffect(CCBMobEffects.ZOMBIFICATION), "Detached pending custody was not released when its owner block was destroyed");
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

    private static BlockState axisPipeState() {
        return CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X);
    }

    private static GasHandler sourceHandler(GameTestHelper helper) {
        GasHandler handler = helper.getLevel().getCapability(GasCapabilities.BLOCK, helper.absolutePos(TANK_POS), Direction.EAST);
        helper.assertTrue(handler != null, "Airtight tank did not expose its east-side gas capability");
        if (handler == null) {
            throw new NullPointerException("Airtight tank did not expose its east-side gas capability.");
        }

        return handler;
    }

    private static GasPipeConnection westConnection(GameTestHelper helper) {
        return requireWestConnection(helper, transport(helper));
    }

    private static GasPipeConnection requireWestConnection(GameTestHelper helper, GasTransportBehaviour transport) {
        GasPipeConnection connection = transport.getConnection(Direction.WEST);
        helper.assertTrue(connection != null, "Gas transport did not create its west connection to the airtight tank");
        if (connection == null) {
            throw new NullPointerException("Gas transport did not create its west connection to the airtight tank.");
        }

        return connection;
    }

    private static GasTransportBehaviour transport(GameTestHelper helper) {
        GasTransportBehaviour transport = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(PIPE_POS), GasTransportBehaviour.TYPE);
        helper.assertTrue(transport != null, "GasTransportBehaviour was not initialized on the pipe");
        if (transport == null) {
            throw new NullPointerException("GasTransportBehaviour was not initialized on the pipe.");
        }

        return transport;
    }

    private static void closeWestFaceWithWrench(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        BlockPos absolutePos = helper.absolutePos(PIPE_POS);
        BlockState state = helper.getLevel().getBlockState(absolutePos);
        helper.assertTrue(state.is(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get()), "Airtight encased pipe was missing before its west face was closed");
        helper.assertTrue(state.getValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST)), "Airtight encased pipe west face was already closed");
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(absolutePos).add(-0.5, 0, 0), Direction.WEST, absolutePos, false);
        InteractionResult result = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().onWrenched(state, new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
        helper.assertTrue(result == InteractionResult.SUCCESS, "Wrenching the airtight encased pipe west face closed did not succeed");
    }

    private static GasStack retiredPendingTransfer(GameTestHelper helper, GasTransportBehaviour transport) {
        ServerLevel level = helper.getLevel();
        CompoundTag serialized = new CompoundTag();
        transport.write(serialized, level.registryAccess(), false);
        ListTag retiredConnections = serialized.getList(RETIRED_CONNECTIONS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < retiredConnections.size(); index++) {
            CompoundTag retiredConnection = retiredConnections.getCompound(index);
            if (retiredConnection.getInt(SIDE_KEY) != Direction.WEST.get3DDataValue()) {
                continue;
            }

            return GasStack.parseOptional(level.registryAccess(), NbtValues.getCompoundOrEmpty(retiredConnection, PENDING_TRANSFER_KEY));
        }
        helper.fail("No retired pending custody was serialized for " + Direction.WEST.getName());
        return GasStack.EMPTY;
    }
}
