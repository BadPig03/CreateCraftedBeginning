package net.ty.createcraftedbeginning.gametests.gas;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunk.EntityCreationType;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver.AdjacentConnection;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasUnloadedChunkGameTests {
    private GasUnloadedChunkGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void unloadedTopologyStartAndConnectionStayUnloaded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pipePos = loadedEdge(helper);
        BlockPos unloadedPos = pipePos.east();
        helper.assertTrue(GasConnectionResolver.getTransportBehaviour(level, unloadedPos) == null, "Unloaded position exposed a transport");
        AdjacentConnection adjacent = GasConnectionResolver.resolveAdjacentConnection(level, pipePos, Direction.EAST);
        helper.assertTrue(adjacent.behaviour() == null && !adjacent.hasGasHandler() && !adjacent.hasTransportConnection(), "Unloaded neighbor exposed a connection");
        helper.assertTrue(!adjacent.isAtmospheric(), "Unloaded neighbor became an atmospheric outlet");
        helper.assertTrue(GasNetworkTopology.get(level, unloadedPos).pipePositions().isEmpty(), "Unloaded start entered the topology");
        helper.assertTrue(!level.isLoaded(unloadedPos), "Connection resolution or topology scanning loaded a chunk");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void pumpScanStopsAtUnloadedChunkAndRefreshesWhenItLoads(GameTestHelper helper) {
        assertBoundaryRefresh(helper, CCBBlocks.AIRTIGHT_PUMP_BLOCK.get().defaultBlockState().setValue(AirtightPumpBlock.FACING, Direction.EAST));
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void pipeConnectionReopensWhenNeighborChunkLoads(GameTestHelper helper) {
        assertBoundaryRefresh(helper, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
    }

    private static void assertBoundaryRefresh(GameTestHelper helper, BlockState pipeState) {
        ServerLevel level = helper.getLevel();
        BlockPos pipePos = loadedEdge(helper);
        BlockPos adjacentPos = pipePos.east();
        LevelChunk chunk = level.getChunkAt(pipePos);
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(pipePos.getY()));
        int x = pipePos.getX() & 15;
        int y = pipePos.getY() & 15;
        int z = pipePos.getZ() & 15;
        BlockState original = section.getBlockState(x, y, z);
        helper.assertTrue(original.isAir() && chunk.getBlockEntity(pipePos, EntityCreationType.CHECK) == null, "Expected an unused air position for the boundary fixture");
        BlockEntity pipe = ((EntityBlock) pipeState.getBlock()).newBlockEntity(pipePos, pipeState);
        helper.assertTrue(pipe != null, "Boundary fixture could not create its block entity");
        if (pipe == null) {
            throw new NullPointerException("Boundary fixture could not create its block entity.");
        }

        try {
            section.setBlockState(x, y, z, pipeState);
            chunk.setBlockEntity(pipe);
            helper.assertTrue(!level.isLoaded(adjacentPos), "Test setup loaded the neighbor chunk");
            Snapshot before = GasNetworkTopology.get(level, pipePos);
            BlockFace outlet = new BlockFace(pipePos, Direction.EAST);
            helper.assertTrue(before.pipePositions().contains(pipePos), "Boundary pipe was not scanned");
            helper.assertTrue(!before.atmosphericFaces().contains(outlet) && !before.endpointFaces().contains(outlet), "Unloaded boundary was exposed as an endpoint");
            helper.assertTrue(!level.isLoaded(adjacentPos), "Topology scan synchronously loaded a neighbor chunk");
            helper.assertTrue(GasNetworkTopology.get(level, pipePos) == before, "Unchanged loaded chunks did not reuse the cache");

            level.getChunkAt(adjacentPos);
            helper.assertTrue(level.getBlockState(adjacentPos).isAir(), "Expected an air outlet at the top of the test world");
            Snapshot after = GasNetworkTopology.get(level, pipePos);
            helper.assertTrue(after != before, "Chunk loading left the cached boundary unchanged");
            helper.assertTrue(after.atmosphericFaces().contains(outlet), "Loaded air outlet did not reopen immediately");
            helper.assertTrue(GasNetworkTopology.get(level, pipePos) == after, "Refreshed topology was not cached");
        }
        finally {
            try {
                chunk.removeBlockEntity(pipePos);
            }
            finally {
                section.setBlockState(x, y, z, original);
                GasNetworkTopology.invalidate(level, pipePos);
            }
        }
        helper.succeed();
    }

    private static BlockPos loadedEdge(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ChunkPos origin = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        int x = origin.x;
        while (level.hasChunk(x + 1, origin.z) && x - origin.x < 512) {
            x++;
        }
        helper.assertTrue(!level.hasChunk(x + 1, origin.z), "Could not locate an unloaded neighbor chunk");
        return new BlockPos((x << 4) + 15, level.getMaxBuildHeight() - 8, (origin.z << 4) + 8);
    }
}
