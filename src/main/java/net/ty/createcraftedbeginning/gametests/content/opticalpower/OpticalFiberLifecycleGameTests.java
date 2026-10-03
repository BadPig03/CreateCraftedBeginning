package net.ty.createcraftedbeginning.gametests.content.opticalpower;

import com.simibubi.create.AllItems;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunk.EntityCreationType;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent.Load;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetwork;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetworkManager;
import net.ty.createcraftedbeginning.content.opticalpower.opticalfiber.OpticalFiberBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OpticalFiberLifecycleGameTests {
    private static final String COMPOUND_KEY_OPTICAL_POWER_LP = "OpticalPowerLp";
    private static final int CACHE_SETTLE_STEPS = 20;
    private static final int MAX_BOUNDARY_SEARCH_CHUNKS = 512;

    private OpticalFiberLifecycleGameTests() {
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 60)
    public static void wrenchPickupReturnsFiberAndImmediatelyDisconnectsPower(GameTestHelper helper) {
        BlockPos fiberPos = new BlockPos(4, 1, 4);
        BlockPos emitterPos = fiberPos.east();
        BlockState fiber = CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState().setValue(OpticalFiberBlock.WEST, true).setValue(OpticalFiberBlock.EAST, true);
        helper.setBlock(fiberPos.west(2), Blocks.GLOWSTONE);
        helper.setBlock(fiberPos.west(), fiber);
        helper.setBlock(fiberPos, fiber);
        helper.setBlock(emitterPos, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.EAST));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack wrench = new ItemStack(AllItems.WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        BlockPos absoluteFiber = helper.absolutePos(fiberPos);
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(absoluteFiber), Direction.UP, absoluteFiber, false);
        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult);
        helper.runAfterDelay(2, () -> {
            ServerLevel level = helper.getLevel();
            settleNetworks(level);
            assertEmitterPower(helper, emitterPos, 1, 1);
            helper.assertTrue(wrench.getItem().useOn(context).consumesAction(), "Regular wrench interaction did not recognize optical fiber.");
            settleNetworks(level);
            helper.assertTrue(level.getBlockState(absoluteFiber).is(CCBBlocks.OPTICAL_FIBER_BLOCK.get()), "Regular wrench interaction removed optical fiber without sneaking.");
            assertEmitterPower(helper, emitterPos, 1, 1);

            player.setShiftKeyDown(true);
            helper.assertTrue(wrench.getItem().useOn(context).consumesAction(), "Sneaking wrench interaction did not handle optical fiber pickup.");
            helper.assertTrue(level.getBlockState(absoluteFiber).isAir(), "Sneaking wrench interaction left optical fiber in the world.");
            helper.assertTrue(player.getInventory().countItem(CCBBlocks.OPTICAL_FIBER_BLOCK.get().asItem()) == 1, "Survival wrench pickup did not return exactly one optical fiber to inventory.");
            helper.assertTrue(!level.getBlockState(absoluteFiber.west()).getValue(OpticalFiberBlock.EAST), "Wrench pickup left the adjacent fiber connected to air.");
            helper.assertTrue(OpticalPowerNetworkManager.findCachedNetwork(level, helper.absolutePos(emitterPos)) == null, "Wrench pickup retained the disconnected network cache.");
            helper.assertTrue(!helper.<LaserEmitterBlockEntity>getBlockEntity(emitterPos).isLaserActive(), "Wrench pickup retained laser power until network rebuilding.");
            settleNetworks(level);
            assertEmitterPower(helper, emitterPos, 0, 1);

            helper.setBlock(fiberPos, fiber);
            settleNetworks(level);
            assertEmitterPower(helper, emitterPos, 1, 1);
            Player creativePlayer = helper.makeMockPlayer(GameType.CREATIVE);
            creativePlayer.setItemInHand(InteractionHand.MAIN_HAND, wrench.copy());
            creativePlayer.setShiftKeyDown(true);
            UseOnContext creativeContext = new UseOnContext(creativePlayer, InteractionHand.MAIN_HAND, hitResult);
            helper.assertTrue(wrench.getItem().useOn(creativeContext).consumesAction() && level.getBlockState(absoluteFiber).isAir(), "Creative wrench pickup failed to remove optical fiber.");
            helper.assertTrue(creativePlayer.getInventory().countItem(CCBBlocks.OPTICAL_FIBER_BLOCK.get().asItem()) == 0, "Creative wrench pickup unexpectedly added optical fiber to inventory.");
            helper.assertTrue(!helper.<LaserEmitterBlockEntity>getBlockEntity(emitterPos).isLaserActive(), "Creative wrench pickup retained the disconnected allocation.");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 60)
    public static void fiberLoopSplitsAndMergesWithoutDuplicatingPower(GameTestHelper helper) {
        BlockPos firstEmitter = new BlockPos(3, 2, 4);
        BlockPos secondEmitter = new BlockPos(7, 2, 4);
        BlockState fiber = CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState().setValue(OpticalFiberBlock.WEST, true).setValue(OpticalFiberBlock.EAST, true).setValue(OpticalFiberBlock.NORTH, true).setValue(OpticalFiberBlock.SOUTH, true).setValue(OpticalFiberBlock.UP, true);
        for (BlockPos emitter : List.of(firstEmitter, secondEmitter)) {
            helper.setBlock(emitter.below(), fiber);
            helper.setBlock(emitter.below().north(), Blocks.GLOWSTONE);
            helper.setBlock(emitter, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.UP));
        }
        helper.runAfterDelay(2, () -> {
            ServerLevel level = helper.getLevel();
            settleNetworks(level);
            assertEmitterPower(helper, firstEmitter, 1, 1);
            assertEmitterPower(helper, secondEmitter, 1, 1);
            for (int x = 4; x <= 6; x++) {
                helper.setBlock(new BlockPos(x, 1, 4), fiber);
            }
            settleNetworks(level);
            assertEmitterPower(helper, firstEmitter, 1, 2);
            assertEmitterPower(helper, secondEmitter, 0, 2);
            for (int x = 3; x <= 7; x++) {
                helper.setBlock(new BlockPos(x, 1, 5), fiber);
            }
            settleNetworks(level);
            assertEmitterPower(helper, firstEmitter, 1, 2);
            assertEmitterPower(helper, secondEmitter, 0, 2);

            helper.setBlock(new BlockPos(5, 1, 4), Blocks.AIR);
            settleNetworks(level);
            assertEmitterPower(helper, firstEmitter, 1, 2);
            assertEmitterPower(helper, secondEmitter, 0, 2);
            helper.setBlock(new BlockPos(5, 1, 5), Blocks.AIR);
            helper.assertTrue(!helper.<LaserEmitterBlockEntity>getBlockEntity(firstEmitter).isLaserActive() && !helper.<LaserEmitterBlockEntity>getBlockEntity(secondEmitter).isLaserActive(), "Splitting the last loop path retained old optical allocations.");
            settleNetworks(level);
            assertEmitterPower(helper, firstEmitter, 1, 1);
            assertEmitterPower(helper, secondEmitter, 1, 1);

            helper.setBlock(new BlockPos(5, 1, 4), fiber);
            helper.assertTrue(!helper.<LaserEmitterBlockEntity>getBlockEntity(firstEmitter).isLaserActive() && !helper.<LaserEmitterBlockEntity>getBlockEntity(secondEmitter).isLaserActive(), "Merging independent networks retained their old allocations.");
            settleNetworks(level);
            assertEmitterPower(helper, firstEmitter, 1, 2);
            assertEmitterPower(helper, secondEmitter, 0, 2);
            OpticalPowerNetwork merged = requireNetwork(helper, firstEmitter);
            helper.assertTrue(merged == requireNetwork(helper, secondEmitter), "Reconnected fiber consumers did not share the same network cache.");
            settleNetworks(level);
            helper.assertTrue(merged == requireNetwork(helper, firstEmitter) && merged == requireNetwork(helper, secondEmitter), "Unchanged fiber loop kept rebuilding its network cache.");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 60)
    public static void removingReplacingAndRotatingEmitterRedistributesPower(GameTestHelper helper) {
        BlockPos firstEmitter = new BlockPos(3, 2, 4);
        BlockPos secondEmitter = new BlockPos(7, 2, 4);
        BlockState fiber = CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState().setValue(OpticalFiberBlock.WEST, true).setValue(OpticalFiberBlock.EAST, true).setValue(OpticalFiberBlock.UP, true);
        for (int x = 3; x <= 7; x++) {
            helper.setBlock(new BlockPos(x, 1, 4), fiber);
        }
        BlockState emitter = CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.UP);
        helper.setBlock(firstEmitter.below().west(), Blocks.GLOWSTONE);
        helper.setBlock(firstEmitter, emitter);
        helper.setBlock(secondEmitter, emitter);
        helper.runAfterDelay(2, () -> {
            ServerLevel level = helper.getLevel();
            settleNetworks(level);
            assertEmitterPower(helper, firstEmitter, 1, 2);
            assertEmitterPower(helper, secondEmitter, 0, 2);
            helper.setBlock(firstEmitter, Blocks.AIR);
            settleNetworks(level);
            helper.assertTrue(OpticalPowerNetworkManager.findCachedNetwork(level, helper.absolutePos(firstEmitter)) == null, "Removed emitter retained ownership of an optical network.");
            assertEmitterPower(helper, secondEmitter, 1, 1);

            helper.setBlock(firstEmitter, emitter.setValue(DirectionalBlock.FACING, Direction.DOWN));
            helper.runAfterDelay(2, () -> {
                settleNetworks(level);
                assertEmitterPower(helper, firstEmitter, 0, 1);
                assertEmitterPower(helper, secondEmitter, 1, 1);
                helper.setBlock(firstEmitter, emitter);
                helper.assertTrue(!helper.<LaserEmitterBlockEntity>getBlockEntity(secondEmitter).isLaserActive(), "Rotating an emitter into the network retained the previous allocation.");
                settleNetworks(level);
                assertEmitterPower(helper, firstEmitter, 1, 2);
                assertEmitterPower(helper, secondEmitter, 0, 2);
                helper.assertTrue(requireNetwork(helper, firstEmitter) == requireNetwork(helper, secondEmitter), "Rotated emitter did not rejoin the existing network cache.");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 40)
    public static void unloadedBoundaryStaysUnloadedAndRefreshesOnChunkLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChunkPos origin = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        int chunkX = origin.x;
        while (level.hasChunk(chunkX + 1, origin.z) && chunkX - origin.x < MAX_BOUNDARY_SEARCH_CHUNKS) {
            chunkX++;
        }
        helper.assertTrue(!level.hasChunk(chunkX + 1, origin.z), "Could not locate an unloaded chunk for the optical boundary fixture.");
        BlockPos fiberPos = new BlockPos((chunkX << 4) + 15, level.getMaxBuildHeight() - 8, (origin.z << 4) + 8);
        BlockPos emitterPos = fiberPos.west();
        BlockPos unloadedPos = fiberPos.east();
        ChunkPos boundaryChunkPos = new ChunkPos(unloadedPos);
        BlockPos sourcePos = fiberPos.above();
        ServerChunkCache chunkSource = level.getChunkSource();
        ChunkMap chunkMap = chunkSource.chunkMap;
        DistanceManager distanceManager = chunkMap.getDistanceManager();
        int accessibleLevel = ChunkLevel.byStatus(ChunkStatus.FULL);
        LevelChunk chunk = level.getChunkAt(fiberPos);
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(fiberPos.getY()));
        BlockState originalFiber = section.getBlockState(fiberPos.getX() & 15, fiberPos.getY() & 15, fiberPos.getZ() & 15);
        BlockState originalEmitter = section.getBlockState(emitterPos.getX() & 15, emitterPos.getY() & 15, emitterPos.getZ() & 15);
        BlockState originalSource = section.getBlockState(sourcePos.getX() & 15, sourcePos.getY() & 15, sourcePos.getZ() & 15);
        helper.assertTrue(originalFiber.isAir() && originalEmitter.isAir() && originalSource.isAir(), "Optical boundary fixture requires unused air positions.");
        helper.assertTrue(chunk.getBlockEntity(fiberPos, EntityCreationType.CHECK) == null && chunk.getBlockEntity(emitterPos, EntityCreationType.CHECK) == null && chunk.getBlockEntity(sourcePos, EntityCreationType.CHECK) == null, "Optical boundary fixture would replace an existing block entity.");
        BlockState emitterState = CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.WEST);
        BlockState fiberState = CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState().setValue(OpticalFiberBlock.WEST, true).setValue(OpticalFiberBlock.EAST, true).setValue(OpticalFiberBlock.UP, true);
        BlockEntity emitter = ((EntityBlock) emitterState.getBlock()).newBlockEntity(emitterPos, emitterState);
        if (emitter == null) {
            throw new NullPointerException("Could not create the optical boundary emitter at " + emitterPos + '.');
        }

        AtomicInteger loadEvents = new AtomicInteger();
        Consumer<Load> loadListener = event -> {
            if (event.getLevel() != level || !event.getChunk().getPos().equals(boundaryChunkPos)) {
                return;
            }

            loadEvents.incrementAndGet();
        };
        NeoForge.EVENT_BUS.addListener(loadListener);
        try {
            section.setBlockState(fiberPos.getX() & 15, fiberPos.getY() & 15, fiberPos.getZ() & 15, fiberState);
            section.setBlockState(sourcePos.getX() & 15, sourcePos.getY() & 15, sourcePos.getZ() & 15, Blocks.GLOWSTONE.defaultBlockState());
            section.setBlockState(emitterPos.getX() & 15, emitterPos.getY() & 15, emitterPos.getZ() & 15, emitterState);
            chunk.setBlockEntity(emitter);
            helper.assertTrue(!level.isLoaded(unloadedPos), "Optical boundary fixture loaded its missing neighbor.");
            helper.assertTrue(OpticalPowerNetwork.scan(level, unloadedPos).getNodes().isEmpty(), "Optical scanning entered an unloaded starting position.");
            OpticalPowerNetworkManager.registerConsumer(level, unloadedPos);
            OpticalPowerNetworkManager.registerConsumer(level, emitterPos);
            settleNetworks(level);
            BlockPos relativeEmitter = emitterPos.subtract(helper.absolutePos(BlockPos.ZERO));
            assertEmitterPower(helper, relativeEmitter, 1, 1);
            OpticalPowerNetwork before = requireNetwork(helper, relativeEmitter);
            helper.assertTrue(before.getNodes().size() == 2 && !before.getNodes().contains(unloadedPos), "Optical scanning crossed the unloaded fiber boundary.");
            int ports = OpticalFiberBlock.getDeviceConnections(level, fiberPos, fiberState);
            helper.assertTrue((ports & 1 << Direction.EAST.get3DDataValue()) == 0, "Unloaded optical neighbor exposed a device port.");
            helper.assertTrue(!level.isLoaded(unloadedPos), "Optical scanning, scheduling or port lookup loaded the neighbor chunk.");
            settleNetworks(level);
            helper.assertTrue(before == requireNetwork(helper, relativeEmitter), "Unchanged unloaded boundary did not retain its network cache.");

            ChunkAccess loadedChunk = chunkSource.getChunkFuture(boundaryChunkPos.x, boundaryChunkPos.z, ChunkStatus.FULL, true).join().orElse(null);
            if (loadedChunk == null) {
                throw new NullPointerException("Missing loaded optical boundary chunk at " + boundaryChunkPos + '.');
            }

            helper.assertTrue(level.isLoaded(unloadedPos), "Requested optical boundary chunk did not become accessible.");
            settleNetworks(level);
            assertEmitterPower(helper, relativeEmitter, 1, 1);
            OpticalPowerNetwork accessible = requireNetwork(helper, relativeEmitter);
            helper.assertTrue(before != accessible, "Loaded boundary reused its obsolete optical network snapshot.");

            distanceManager.removeTicket(TicketType.UNKNOWN, boundaryChunkPos, accessibleLevel, boundaryChunkPos);
            distanceManager.runAllUpdates(chunkMap);
            helper.assertTrue(!level.isLoaded(unloadedPos), "Removing the boundary chunk ticket did not revoke optical access.");
            ChunkHolder retainedHolder = chunkMap.getVisibleChunkIfPresent(boundaryChunkPos.toLong());
            if (retainedHolder == null) {
                throw new NullPointerException("Missing retained optical boundary chunk holder at " + boundaryChunkPos + '.');
            }

            helper.assertTrue(retainedHolder.getChunkIfPresentUnchecked(ChunkStatus.FULL) == loadedChunk, "Inaccessible boundary fixture did not retain its loaded chunk.");
            settleNetworks(level);
            assertEmitterPower(helper, relativeEmitter, 1, 1);
            OpticalPowerNetwork inaccessible = requireNetwork(helper, relativeEmitter);
            helper.assertTrue(accessible != inaccessible, "Losing boundary chunk access did not refresh the optical cache.");
            int previousLoadEvents = loadEvents.get();
            ChunkAccess reactivatedChunk = chunkSource.getChunkFuture(boundaryChunkPos.x, boundaryChunkPos.z, ChunkStatus.FULL, true).join().orElse(null);
            if (reactivatedChunk == null) {
                throw new NullPointerException("Missing reactivated optical boundary chunk at " + boundaryChunkPos + '.');
            }

            helper.assertTrue(reactivatedChunk == loadedChunk && loadEvents.get() == previousLoadEvents, "Boundary reactivation unexpectedly reloaded its retained chunk.");
            helper.assertTrue(level.isLoaded(unloadedPos), "Retained optical boundary chunk did not become accessible again.");
            settleNetworks(level);
            assertEmitterPower(helper, relativeEmitter, 1, 1);
            helper.assertTrue(inaccessible != requireNetwork(helper, relativeEmitter), "Reactivated boundary reused its inaccessible optical snapshot.");
        }
        finally {
            NeoForge.EVENT_BUS.unregister(loadListener);
            try {
                chunk.removeBlockEntity(emitterPos);
            }
            finally {
                section.setBlockState(emitterPos.getX() & 15, emitterPos.getY() & 15, emitterPos.getZ() & 15, originalEmitter);
                section.setBlockState(fiberPos.getX() & 15, fiberPos.getY() & 15, fiberPos.getZ() & 15, originalFiber);
                section.setBlockState(sourcePos.getX() & 15, sourcePos.getY() & 15, sourcePos.getZ() & 15, originalSource);
                OpticalPowerNetworkManager.invalidateAround(level, emitterPos);
            }
        }
        helper.succeed();
    }

    private static void settleNetworks(ServerLevel level) {
        for (int step = 0; step < CACHE_SETTLE_STEPS; step++) {
            OpticalPowerNetworkManager.tick(level);
        }
    }

    private static void assertEmitterPower(GameTestHelper helper, BlockPos emitterPos, int expectedPowerLp, int expectedConsumers) {
        OpticalPowerNetwork network = requireNetwork(helper, emitterPos);
        helper.assertTrue(network.getConsumers().size() == expectedConsumers, "Unexpected optical consumer count at " + emitterPos + ": " + network.getConsumers().size() + '.');
        helper.assertTrue(network.getAllocatedPowerLp(helper.absolutePos(emitterPos)) == expectedPowerLp, "Unexpected cached optical allocation at " + emitterPos + '.');
        int allocatedPowerLp = helper.<LaserEmitterBlockEntity>getBlockEntity(emitterPos).getUpdateTag(helper.getLevel().registryAccess()).getInt(COMPOUND_KEY_OPTICAL_POWER_LP);
        helper.assertTrue(allocatedPowerLp == expectedPowerLp, "Expected " + expectedPowerLp + " LP at " + emitterPos + ", received " + allocatedPowerLp + " LP.");
    }

    private static OpticalPowerNetwork requireNetwork(GameTestHelper helper, BlockPos emitterPos) {
        OpticalPowerNetwork network = OpticalPowerNetworkManager.findCachedNetwork(helper.getLevel(), helper.absolutePos(emitterPos));
        if (network == null) {
            throw new NullPointerException("Missing optical lifecycle test network at " + emitterPos + '.');
        }

        return network;
    }
}
