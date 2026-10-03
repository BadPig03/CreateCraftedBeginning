package net.ty.createcraftedbeginning.gametests.content.opticalpower;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetwork;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetworkManager;
import net.ty.createcraftedbeginning.content.opticalpower.opticalfiber.OpticalFiberBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OpticalPowerNetworkGameTests {
    private static final int CACHE_SETTLE_STEPS = 20;

    private OpticalPowerNetworkGameTests() {
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 40)
    public static void networkAggregatesMoreThanEightIndependentCollectors(GameTestHelper helper) {
        BlockState fiber = CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState().setValue(OpticalFiberBlock.NORTH, true).setValue(OpticalFiberBlock.SOUTH, true).setValue(OpticalFiberBlock.EAST, true).setValue(OpticalFiberBlock.WEST, true).setValue(OpticalFiberBlock.UP, true);
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 1, z), fiber);
                if (x % 2 != 0 || z % 2 != 0) {
                    continue;
                }

                helper.setBlock(new BlockPos(x, 2, z), CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.getDefaultState());
            }
        }
        BlockPos emitterPos = new BlockPos(1, 1, 2);
        helper.setBlock(emitterPos, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.WEST));
        helper.runAfterDelay(2, () -> {
            ServerLevel level = helper.getLevel();
            long previousTime = level.getDayTime();
            float previousRain = level.getRainLevel(1);
            float previousThunder = level.getThunderLevel(1);
            try {
                level.setDayTime(6000);
                level.setRainLevel(0);
                level.setThunderLevel(0);
                level.updateSkyBrightness();
                BlockPos absoluteEmitter = helper.absolutePos(emitterPos);
                OpticalPowerNetwork network = OpticalPowerNetwork.scan(level, absoluteEmitter);
                helper.assertTrue(network.getAllocatedPowerLp(absoluteEmitter) == 9, "Optical network failed to aggregate nine independent 1 LP collectors.");
            }
            finally {
                level.setDayTime(previousTime);
                level.setRainLevel(previousRain);
                level.setThunderLevel(previousThunder);
                level.updateSkyBrightness();
            }
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 60)
    public static void oversizedCollectorDependenciesRemainStableAndRecover(GameTestHelper helper) {
        for (int x = 2; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 3, 4), CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.getDefaultState());
            helper.setBlock(new BlockPos(x, 4, 4), Blocks.STONE);
        }
        BlockPos firstEmitter = new BlockPos(2, 1, 4);
        BlockPos secondEmitter = new BlockPos(9, 1, 4);
        for (BlockPos emitter : List.of(firstEmitter, secondEmitter)) {
            helper.setBlock(emitter.above(), CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState().setValue(OpticalFiberBlock.UP, true).setValue(OpticalFiberBlock.DOWN, true));
            helper.setBlock(emitter.above().north(), Blocks.GLOWSTONE);
            helper.setBlock(emitter, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.DOWN));
        }
        helper.runAfterDelay(2, () -> {
            ServerLevel level = helper.getLevel();
            rebuildNetworks(helper, firstEmitter, secondEmitter);
            OpticalPowerNetwork first = requireCachedNetwork(helper, firstEmitter);
            OpticalPowerNetwork second = requireCachedNetwork(helper, secondEmitter);
            helper.assertTrue(first != second, "Invalid collector geometry bridged independent optical networks.");
            helper.assertTrue(first.getSourceDependencies().contains(helper.absolutePos(new BlockPos(5, 3, 4))) && second.getSourceDependencies().contains(helper.absolutePos(new BlockPos(5, 3, 4))), "Oversized collector fixture did not create overlapping observed dependencies.");
            helper.assertTrue(first.getAllocatedPowerLp(helper.absolutePos(firstEmitter)) == 1 && second.getAllocatedPowerLp(helper.absolutePos(secondEmitter)) == 1, "Invalid collectors changed independent ordinary-source allocations.");
            assertStableNetworks(helper, firstEmitter, secondEmitter);

            helper.setBlock(new BlockPos(5, 3, 4), Blocks.AIR);
            helper.assertTrue(OpticalPowerNetworkManager.findCachedNetwork(level, helper.absolutePos(firstEmitter)) == null && OpticalPowerNetworkManager.findCachedNetwork(level, helper.absolutePos(secondEmitter)) == null, "Changing a shared dependency did not invalidate every observing network.");
            rebuildNetworks(helper, firstEmitter, secondEmitter);
            helper.assertTrue(requireCachedNetwork(helper, firstEmitter) != first && requireCachedNetwork(helper, secondEmitter) != second, "Repaired collector geometry reused obsolete network snapshots.");
            assertStableNetworks(helper, firstEmitter, secondEmitter);
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 60)
    public static void truncatedNetworkSnapshotsRemainStableAndRecoverAfterSplit(GameTestHelper helper) {
        BlockState fiber = CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState().setValue(OpticalFiberBlock.NORTH, true).setValue(OpticalFiberBlock.SOUTH, true).setValue(OpticalFiberBlock.EAST, true).setValue(OpticalFiberBlock.WEST, true).setValue(OpticalFiberBlock.UP, true).setValue(OpticalFiberBlock.DOWN, true);
        for (BlockPos pos : BlockPos.betweenClosed(0, 1, 0, 19, 11, 19)) {
            helper.setBlock(pos, fiber);
        }
        BlockPos firstEmitter = new BlockPos(0, 1, 0);
        BlockPos secondEmitter = new BlockPos(19, 11, 19);
        helper.setBlock(firstEmitter, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.WEST));
        helper.setBlock(secondEmitter, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.GLOWSTONE);
        helper.setBlock(new BlockPos(18, 10, 18), Blocks.GLOWSTONE);
        helper.runAfterDelay(2, () -> {
            ServerLevel level = helper.getLevel();
            rebuildNetworks(helper, firstEmitter, secondEmitter);
            OpticalPowerNetwork first = requireCachedNetwork(helper, firstEmitter);
            OpticalPowerNetwork second = requireCachedNetwork(helper, secondEmitter);
            helper.assertTrue(first != second && !first.getConsumers().contains(helper.absolutePos(secondEmitter)) && !second.getConsumers().contains(helper.absolutePos(firstEmitter)), "Oversized network fixture did not truncate before discovering the opposite consumer.");
            BlockPos sharedNode = helper.absolutePos(new BlockPos(10, 6, 10));
            helper.assertTrue(first.getNodes().contains(sharedNode) && second.getNodes().contains(sharedNode), "Truncated network fixture did not create overlapping observed nodes.");
            helper.assertTrue(first.getAllocatedPowerLp(helper.absolutePos(firstEmitter)) == 0 && second.getAllocatedPowerLp(helper.absolutePos(secondEmitter)) == 0, "Truncated optical networks distributed power above the scan limit.");
            assertStableNetworks(helper, firstEmitter, secondEmitter);

            helper.setBlock(new BlockPos(10, 6, 10), Blocks.AIR);
            helper.assertTrue(OpticalPowerNetworkManager.findCachedNetwork(level, helper.absolutePos(firstEmitter)) == null && OpticalPowerNetworkManager.findCachedNetwork(level, helper.absolutePos(secondEmitter)) == null, "Changing a shared node did not invalidate every truncated snapshot.");
            for (BlockPos pos : BlockPos.betweenClosed(10, 1, 0, 10, 11, 19)) {
                helper.setBlock(pos, Blocks.AIR);
            }
            rebuildNetworks(helper, firstEmitter, secondEmitter);
            first = requireCachedNetwork(helper, firstEmitter);
            second = requireCachedNetwork(helper, secondEmitter);
            helper.assertTrue(first != second && first.getAllocatedPowerLp(helper.absolutePos(firstEmitter)) == 1 && second.getAllocatedPowerLp(helper.absolutePos(secondEmitter)) == 1, "Splitting an oversized network did not restore independent optical power.");
            assertStableNetworks(helper, firstEmitter, secondEmitter);
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 60)
    public static void validCollectorPortsShareOneNetworkAcrossSplitAndMerge(GameTestHelper helper) {
        for (int x = 2; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 3, 4), CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.getDefaultState());
            helper.setBlock(new BlockPos(x, 4, 4), Blocks.STONE);
        }
        BlockPos firstEmitter = new BlockPos(2, 1, 4);
        BlockPos secondEmitter = new BlockPos(6, 1, 4);
        for (BlockPos emitter : List.of(firstEmitter, secondEmitter)) {
            helper.setBlock(emitter.above(), CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState().setValue(OpticalFiberBlock.UP, true).setValue(OpticalFiberBlock.DOWN, true));
            helper.setBlock(emitter, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.DOWN));
        }
        helper.setBlock(firstEmitter.above().north(), Blocks.GLOWSTONE);
        helper.runAfterDelay(2, () -> {
            ServerLevel level = helper.getLevel();
            long previousTime = level.getDayTime();
            try {
                level.setDayTime(18000);
                level.updateSkyBrightness();
                OpticalPowerNetworkManager.invalidateAt(level, helper.absolutePos(firstEmitter));
                OpticalPowerNetworkManager.invalidateAt(level, helper.absolutePos(secondEmitter));
                rebuildNetworks(helper, firstEmitter, secondEmitter);
                OpticalPowerNetwork merged = requireCachedNetwork(helper, firstEmitter);
                helper.assertTrue(merged == requireCachedNetwork(helper, secondEmitter), "Valid collector ports did not share one cached optical network.");
                helper.assertTrue(merged.getAllocatedPowerLp(helper.absolutePos(firstEmitter)) + merged.getAllocatedPowerLp(helper.absolutePos(secondEmitter)) == 1, "Shared collector network duplicated ordinary-source power.");
                assertStableNetworks(helper, firstEmitter, secondEmitter);

                helper.setBlock(new BlockPos(4, 3, 4), Blocks.AIR);
                rebuildNetworks(helper, firstEmitter, secondEmitter);
                OpticalPowerNetwork first = requireCachedNetwork(helper, firstEmitter);
                OpticalPowerNetwork second = requireCachedNetwork(helper, secondEmitter);
                helper.assertTrue(first != second && first.getAllocatedPowerLp(helper.absolutePos(firstEmitter)) == 1 && second.getAllocatedPowerLp(helper.absolutePos(secondEmitter)) == 0, "Splitting a valid collector left obsolete network membership or power.");
                assertStableNetworks(helper, firstEmitter, secondEmitter);

                helper.setBlock(new BlockPos(4, 3, 4), CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.getDefaultState());
                rebuildNetworks(helper, firstEmitter, secondEmitter);
                merged = requireCachedNetwork(helper, firstEmitter);
                helper.assertTrue(merged == requireCachedNetwork(helper, secondEmitter) && merged != first && merged != second, "Reconnecting collector ports did not merge their cached networks.");
                assertStableNetworks(helper, firstEmitter, secondEmitter);
            }
            finally {
                level.setDayTime(previousTime);
                level.updateSkyBrightness();
            }
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 60)
    public static void disconnectedEmittersStayUnpoweredWhileRebuildsAreQueued(GameTestHelper helper) {
        BlockState fiber = CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState().setValue(OpticalFiberBlock.UP, true).setValue(OpticalFiberBlock.EAST, true);
        List<BlockPos> consumers = new ArrayList<>();
        List<LaserEmitterBlockEntity> emitters = new ArrayList<>();
        for (int x = 2; x <= 11; x += 3) {
            for (int z = 2; z <= 10; z += 4) {
                BlockPos consumer = new BlockPos(x, 2, z);
                helper.setBlock(consumer.below(), fiber);
                helper.setBlock(consumer.below().east(), Blocks.GLOWSTONE);
                helper.setBlock(consumer, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.UP));
                LaserEmitterBlockEntity emitter = helper.getBlockEntity(consumer);
                consumers.add(consumer);
                emitters.add(emitter);
            }
        }
        BlockPos[] consumerPositions = consumers.toArray(BlockPos[]::new);
        helper.runAfterDelay(2, () -> {
            ServerLevel level = helper.getLevel();
            rebuildNetworks(helper, consumerPositions);
            for (int i = 0; i < consumers.size(); i++) {
                LaserEmitterBlockEntity emitter = emitters.get(i);
                helper.assertTrue(emitter.isLaserActive(), "Queued rebuild fixture did not power emitter " + consumers.get(i) + '.');
                helper.setBlock(consumers.get(i).below(), Blocks.AIR);
                helper.assertTrue(!emitter.isLaserActive(), "Disconnected emitter retained its old allocation before network rebuilding at " + consumers.get(i) + '.');
            }

            OpticalPowerNetworkManager.tick(level);
            int pendingNetworks = 0;
            for (int i = 0; i < consumers.size(); i++) {
                helper.assertTrue(!emitters.get(i).isLaserActive(), "Disconnected emitter resumed power while topology rebuilds were queued.");
                if (OpticalPowerNetworkManager.findCachedNetwork(level, helper.absolutePos(consumers.get(i))) != null) {
                    continue;
                }

                pendingNetworks++;
            }
            helper.assertTrue(pendingNetworks >= 4, "Twelve independent networks did not exceed the eight-rebuild scheduling limit.");

            for (int i = 0; i < consumers.size(); i++) {
                helper.setBlock(consumers.get(i).below(), fiber);
                helper.assertTrue(!emitters.get(i).isLaserActive(), "Reconnected emitter reused an allocation before its network was rebuilt.");
            }
            OpticalPowerNetworkManager.tick(level);
            int activeEmitters = 0;
            for (LaserEmitterBlockEntity emitter : emitters) {
                if (!emitter.isLaserActive()) {
                    continue;
                }

                activeEmitters++;
            }
            helper.assertTrue(activeEmitters <= 8, "Queued emitters received power without waiting for their network rebuilds.");
            rebuildNetworks(helper, consumerPositions);
            for (LaserEmitterBlockEntity emitter : emitters) {
                helper.assertTrue(emitter.isLaserActive(), "Reconnected emitter did not regain power after its network was rebuilt.");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 60)
    public static void sourceAndChunkInvalidationImmediatelyRevokeAllocations(GameTestHelper helper) {
        BlockPos consumer = new BlockPos(4, 2, 4);
        BlockPos fiberPos = consumer.below();
        BlockPos sourcePos = fiberPos.east();
        helper.setBlock(fiberPos, CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState().setValue(OpticalFiberBlock.UP, true).setValue(OpticalFiberBlock.EAST, true));
        helper.setBlock(sourcePos, Blocks.GLOWSTONE);
        helper.setBlock(consumer, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.UP));
        LaserEmitterBlockEntity emitter = helper.getBlockEntity(consumer);
        helper.runAfterDelay(2, () -> {
            ServerLevel level = helper.getLevel();
            rebuildNetworks(helper, consumer);
            helper.assertTrue(emitter.isLaserActive(), "Source invalidation fixture did not power its emitter.");
            helper.setBlock(sourcePos, Blocks.AIR);
            helper.assertTrue(!emitter.isLaserActive(), "Removing a light source retained the emitter allocation until rebuilding.");

            helper.setBlock(sourcePos, Blocks.GLOWSTONE);
            rebuildNetworks(helper, consumer);
            helper.assertTrue(emitter.isLaserActive(), "Restoring the light source did not restore emitter power.");
            OpticalPowerNetworkManager.onChunkAccessibilityChanged(level, new ChunkPos(helper.absolutePos(fiberPos)));
            helper.assertTrue(OpticalPowerNetworkManager.findCachedNetwork(level, helper.absolutePos(consumer)) == null && !emitter.isLaserActive(), "Chunk accessibility invalidation retained an emitter allocation while rebuilding was pending.");
            rebuildNetworks(helper, consumer);
            helper.assertTrue(emitter.isLaserActive(), "Loaded network did not recover power after chunk accessibility invalidation.");
            helper.succeed();
        });
    }

    private static void rebuildNetworks(GameTestHelper helper, BlockPos... consumers) {
        ServerLevel level = helper.getLevel();
        for (BlockPos consumer : consumers) {
            OpticalPowerNetworkManager.registerConsumer(level, helper.absolutePos(consumer));
        }
        for (int step = 0; step < CACHE_SETTLE_STEPS; step++) {
            OpticalPowerNetworkManager.tick(level);
        }
    }

    private static void assertStableNetworks(GameTestHelper helper, BlockPos... consumers) {
        ServerLevel level = helper.getLevel();
        List<OpticalPowerNetwork> snapshots = new ArrayList<>();
        for (BlockPos consumer : consumers) {
            snapshots.add(requireCachedNetwork(helper, consumer));
        }
        for (int step = 0; step < CACHE_SETTLE_STEPS; step++) {
            for (BlockPos consumer : consumers) {
                OpticalPowerNetworkManager.ensureConsumer(level, helper.absolutePos(consumer));
            }
            OpticalPowerNetworkManager.tick(level);
            for (int i = 0; i < consumers.length; i++) {
                helper.assertTrue(OpticalPowerNetworkManager.findCachedNetwork(level, helper.absolutePos(consumers[i])) == snapshots.get(i), "Unchanged optical network was invalidated or rebuilt repeatedly at " + consumers[i] + '.');
            }
        }
    }

    private static OpticalPowerNetwork requireCachedNetwork(GameTestHelper helper, BlockPos consumer) {
        OpticalPowerNetwork network = OpticalPowerNetworkManager.findCachedNetwork(helper.getLevel(), helper.absolutePos(consumer));
        if (network == null) {
            throw new NullPointerException("Missing cached optical network for test consumer at " + consumer + '.');
        }

        return network;
    }
}
