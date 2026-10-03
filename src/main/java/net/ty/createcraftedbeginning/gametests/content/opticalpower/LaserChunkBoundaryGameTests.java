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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laser.LaserBehaviour;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.atomic.AtomicReference;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LaserChunkBoundaryGameTests {
    private static final int MAX_BOUNDARY_SEARCH_CHUNKS = 64;
    private static final int FIXTURE_HEIGHT_MARGIN = 8;

    private LaserChunkBoundaryGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", batch = "laser_chunk_boundary")
    public static void eastFacingLaserStopsAtUnloadedStart(GameTestHelper helper) {
        assertLaserStopsAtUnloadedStart(helper, Direction.EAST);
    }

    @GameTest(template = "gametest/empty_3x3", batch = "laser_chunk_boundary")
    public static void southFacingLaserStopsAtUnloadedStart(GameTestHelper helper) {
        assertLaserStopsAtUnloadedStart(helper, Direction.SOUTH);
    }

    @GameTest(template = "gametest/empty_3x3", batch = "laser_chunk_boundary")
    public static void westFacingLaserStopsAtUnloadedStart(GameTestHelper helper) {
        assertLaserStopsAtUnloadedStart(helper, Direction.WEST);
    }

    @GameTest(template = "gametest/empty_3x3", batch = "laser_chunk_boundary")
    public static void northFacingLaserStopsAtUnloadedStart(GameTestHelper helper) {
        assertLaserStopsAtUnloadedStart(helper, Direction.NORTH);
    }

    private static void assertLaserStopsAtUnloadedStart(GameTestHelper helper, Direction direction) {
        ServerLevel level = helper.getLevel();
        ChunkPos origin = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        int localX = switch (direction) {
            case EAST -> 15;
            case WEST -> 0;
            default -> 8;
        };
        int localZ = switch (direction) {
            case SOUTH -> 15;
            case NORTH -> 0;
            default -> 8;
        };
        BlockPos emitterPos = new BlockPos((origin.x << 4) + localX, level.getMaxBuildHeight() - FIXTURE_HEIGHT_MARGIN, (origin.z << 4) + localZ);
        for (int step = 0; step < MAX_BOUNDARY_SEARCH_CHUNKS && level.isLoaded(emitterPos.relative(direction)); step++) {
            emitterPos = emitterPos.relative(direction, 16);
        }

        BlockPos unloadedPos = emitterPos.relative(direction);
        helper.assertTrue(level.isLoaded(emitterPos) && !level.isLoaded(unloadedPos), "Could not locate a loaded laser position beside an unloaded chunk for " + direction + '.');
        BlockPos targetPos = emitterPos.relative(direction.getOpposite(), 3);
        LevelChunk chunk = level.getChunkAt(targetPos);
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(targetPos.getY()));
        int targetX = targetPos.getX() & 15;
        int targetY = targetPos.getY() & 15;
        int targetZ = targetPos.getZ() & 15;
        BlockState originalTarget = section.getBlockState(targetX, targetY, targetZ);
        helper.assertTrue(originalTarget.isAir(), "Laser boundary target requires unused air at " + targetPos + '.');
        BlockState emitterState = CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, direction.getOpposite());
        LaserEmitterBlockEntity emitter = new LaserEmitterBlockEntity(CCBBlockEntities.LASER_EMITTER.get(), emitterPos, emitterState);
        emitter.setLevel(level);
        AtomicReference<Direction> laserDirection = new AtomicReference<>(emitter.getLaserDirection());
        LaserBehaviour laser = new LaserBehaviour(emitter, laserDirection::get, () -> true, emitter::getLaserRange);
        try {
            section.setBlockState(targetX, targetY, targetZ, Blocks.STONE.defaultBlockState());
            laser.tick();
            BlockHitResult initialHit = laser.getHitResult();
            if (initialHit == null) {
                throw new NullPointerException("Missing initial loaded laser target at " + targetPos + '.');
            }

            helper.assertTrue(initialHit.getBlockPos().equals(targetPos) && laser.getBeamLength() > 0, "Laser failed to hit its loaded target before turning toward " + direction + '.');
            laserDirection.set(direction);
            laser.tick();
            helper.assertTrue(!level.isLoaded(unloadedPos), "Laser facing " + direction + " loaded its missing start chunk; beam length was " + laser.getBeamLength() + '.');
            helper.assertTrue(laser.getBeamLength() == 0 && laser.getHitResult() == null, "Laser facing " + direction + " retained its previous hit or beam across an unloaded boundary.");
            laserDirection.set(direction.getOpposite());
            laser.tick();
            BlockHitResult restoredHit = laser.getHitResult();
            if (restoredHit == null) {
                throw new NullPointerException("Missing restored loaded laser target at " + targetPos + '.');
            }

            helper.assertTrue(restoredHit.getBlockPos().equals(targetPos) && laser.getBeamLength() > 0, "Laser did not resume tracing after turning back into the loaded chunk.");
        }
        finally {
            section.setBlockState(targetX, targetY, targetZ, originalTarget);
        }
        helper.succeed();
    }
}
