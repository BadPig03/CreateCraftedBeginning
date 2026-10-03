package net.ty.createcraftedbeginning.gametests.content.opticalpower;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laser.LaserBehaviour;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RayProjection;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.Service;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LaserProjectedChunkGameTests {
    private static final int MAX_BOUNDARY_SEARCH_CHUNKS = 64;
    private static final int FIXTURE_HEIGHT_MARGIN = 8;
    private static final double BOUNDARY_GAP = 0.5;
    private static final double LENGTH_TOLERANCE = 1.0E-5;

    private LaserProjectedChunkGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", batch = "laser_projected_chunk")
    public static void southeastRayStopsAtMissingCornerChunk(GameTestHelper helper) throws ReflectiveOperationException {
        assertCornerStopsAndResumes(helper, Direction.EAST, Direction.SOUTH, 0.6);
    }

    @GameTest(template = "gametest/empty_3x3", batch = "laser_projected_chunk")
    public static void southwestRayStopsAtMissingCornerChunk(GameTestHelper helper) throws ReflectiveOperationException {
        assertCornerStopsAndResumes(helper, Direction.WEST, Direction.SOUTH, 0.6);
    }

    @GameTest(template = "gametest/empty_3x3", batch = "laser_projected_chunk")
    public static void northeastRayStopsAtMissingCornerChunk(GameTestHelper helper) throws ReflectiveOperationException {
        assertCornerStopsAndResumes(helper, Direction.EAST, Direction.NORTH, 0.6);
    }

    @GameTest(template = "gametest/empty_3x3", batch = "laser_projected_chunk")
    public static void northwestRayStopsAtMissingCornerChunk(GameTestHelper helper) throws ReflectiveOperationException {
        assertCornerStopsAndResumes(helper, Direction.WEST, Direction.NORTH, 0.6);
    }

    @GameTest(template = "gametest/empty_3x3", batch = "laser_projected_chunk")
    public static void southFirstRayStopsAtMissingCornerChunk(GameTestHelper helper) throws ReflectiveOperationException {
        assertCornerStopsAndResumes(helper, Direction.SOUTH, Direction.EAST, 0.6);
    }

    @GameTest(template = "gametest/empty_3x3", batch = "laser_projected_chunk")
    public static void northFirstRayStopsAtMissingCornerChunk(GameTestHelper helper) throws ReflectiveOperationException {
        assertCornerStopsAndResumes(helper, Direction.NORTH, Direction.WEST, 0.6);
    }

    @GameTest(template = "gametest/empty_3x3", batch = "laser_projected_chunk")
    public static void exactCornerRayChecksTouchedChunk(GameTestHelper helper) throws ReflectiveOperationException {
        assertCornerStopsAndResumes(helper, Direction.SOUTH, Direction.EAST, BOUNDARY_GAP);
    }

    @GameTest(template = "gametest/empty_3x3", batch = "laser_projected_chunk")
    public static void verticalRaysRetainFullRange(GameTestHelper helper) throws ReflectiveOperationException {
        ServerLevel level = helper.getLevel();
        BlockPos emitterPos = helper.absolutePos(BlockPos.ZERO).atY(level.getMaxBuildHeight() - FIXTURE_HEIGHT_MARGIN);
        Vec3 rayStart = Vec3.atCenterOf(emitterPos);
        LaserEmitterBlockEntity emitter = new LaserEmitterBlockEntity(CCBBlockEntities.LASER_EMITTER.get(), emitterPos, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState());
        emitter.setLevel(level);
        LaserBehaviour laser = new LaserBehaviour(emitter, emitter::getLaserDirection, () -> true, emitter::getLaserRange);
        Service previous = getBridgeService();
        try {
            for (int sign : new int[]{-1, 1}) {
                SubLevelBridge.install(new ProjectedRayService(rayStart, new Vec3(0, sign, 0)));
                laser.tick();
                helper.assertTrue(laser.getBeamLength() == LaserBehaviour.MAX_RANGE && laser.getHitResult() == null, "Vertical projected ray did not retain its unobstructed maximum range.");
            }
        }
        finally {
            SubLevelBridge.install(previous);
        }
        helper.succeed();
    }

    private static void assertCornerStopsAndResumes(GameTestHelper helper, Direction boundaryDirection, Direction tangentDirection, double tangentGap) throws ReflectiveOperationException {
        ServerLevel level = helper.getLevel();
        ChunkPos origin = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        boolean exactCorner = tangentGap == BOUNDARY_GAP;
        if (exactCorner) {
            origin = new ChunkPos(origin.x, origin.x);
        }

        int stepX = boundaryDirection.getStepX() + tangentDirection.getStepX();
        int stepZ = boundaryDirection.getStepZ() + tangentDirection.getStepZ();
        int localX = stepX > 0 ? 15 : 0;
        int localZ = stepZ > 0 ? 15 : 0;
        BlockPos emitterPos = new BlockPos((origin.x << 4) + localX, level.getMaxBuildHeight() - FIXTURE_HEIGHT_MARGIN, (origin.z << 4) + localZ);
        for (int step = 0; step < MAX_BOUNDARY_SEARCH_CHUNKS && level.isLoaded(emitterPos.relative(boundaryDirection)); step++) {
            emitterPos = exactCorner ? emitterPos.offset(16, 0, 16) : emitterPos.relative(boundaryDirection, 16);
        }
        if (exactCorner) {
            level.getChunkAt(emitterPos);
        }

        BlockPos skippedPos = emitterPos.relative(boundaryDirection);
        helper.assertTrue(level.isLoaded(emitterPos) && !level.isLoaded(skippedPos), "No loaded/unloaded corner fixture for " + boundaryDirection + '.');
        level.getChunkAt(skippedPos.relative(tangentDirection));
        helper.assertTrue(!level.isLoaded(skippedPos), "Diagonal fixture loaded the side chunk before tracing toward " + boundaryDirection + '.');
        Vec3 boundaryOffset = Vec3.atLowerCornerOf(boundaryDirection.getNormal()).scale(0.5 - BOUNDARY_GAP);
        Vec3 tangentOffset = Vec3.atLowerCornerOf(tangentDirection.getNormal()).scale(0.5 - tangentGap);
        Vec3 rayStart = Vec3.atCenterOf(emitterPos).add(boundaryOffset).add(tangentOffset);
        Vec3 rayDirection = new Vec3(stepX, 0, stepZ);
        BlockPos targetPos = BlockPos.containing(rayStart.add(rayDirection.normalize().scale(3)));
        LevelChunk targetChunk = level.getChunkAt(targetPos);
        LevelChunkSection section = targetChunk.getSection(targetChunk.getSectionIndex(targetPos.getY()));
        int targetX = targetPos.getX() & 15;
        int targetY = targetPos.getY() & 15;
        int targetZ = targetPos.getZ() & 15;
        BlockState originalTarget = section.getBlockState(targetX, targetY, targetZ);
        helper.assertTrue(originalTarget.isAir(), "Projected laser target requires unused air at " + targetPos + '.');
        LaserEmitterBlockEntity emitter = new LaserEmitterBlockEntity(CCBBlockEntities.LASER_EMITTER.get(), emitterPos, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, boundaryDirection));
        emitter.setLevel(level);
        LaserBehaviour laser = new LaserBehaviour(emitter, emitter::getLaserDirection, () -> true, emitter::getLaserRange);
        Service previous = getBridgeService();
        try {
            section.setBlockState(targetX, targetY, targetZ, Blocks.STONE.defaultBlockState());
            SubLevelBridge.install(new ProjectedRayService(rayStart, rayDirection));
            laser.tick();
            helper.assertTrue(!level.isLoaded(skippedPos), "Projected laser toward " + boundaryDirection + " loaded the skipped side chunk; beam length was " + laser.getBeamLength() + '.');
            double boundaryDistance = BOUNDARY_GAP * 1.4142135623730951;
            helper.assertTrue(Math.abs(laser.getBeamLength() - boundaryDistance) < LENGTH_TOLERANCE && laser.getHitResult() == null, "Projected laser did not stop at the first unloaded boundary toward " + boundaryDirection + "; beam length was " + laser.getBeamLength() + " with hit " + laser.getHitResult() + '.');
            level.getChunkAt(skippedPos);
            laser.tick();
            BlockHitResult restoredHit = laser.getHitResult();
            if (restoredHit == null) {
                throw new NullPointerException("Missing projected laser target after loading the side chunk at " + targetPos + '.');
            }

            helper.assertTrue(restoredHit.getBlockPos().equals(targetPos) && laser.getBeamLength() > boundaryDistance, "Projected laser did not resume at its target after the skipped chunk loaded.");
            ScrollValueBehaviour range = emitter.getBehaviour(ScrollValueBehaviour.TYPE);
            if (range == null) {
                throw new NullPointerException("Missing projected laser range control at " + emitterPos + '.');
            }

            range.setValue(1);
            laser.tick();
            helper.assertTrue(laser.getHitResult() == null && Math.abs(laser.getBeamLength() - 1) < LENGTH_TOLERANCE, "Projected laser exceeded its shortened range or retained the previous hit.");
        }
        finally {
            SubLevelBridge.install(previous);
            section.setBlockState(targetX, targetY, targetZ, originalTarget);
        }
        helper.succeed();
    }

    private static Service getBridgeService() throws ReflectiveOperationException {
        Field serviceField = SubLevelBridge.class.getDeclaredField("service");
        serviceField.setAccessible(true);
        Service service = (Service) serviceField.get(null);
        if (service == null) {
            throw new NullPointerException("Missing sublevel bridge service before projected laser test.");
        }

        return service;
    }

    private static final class ProjectedRayService implements Service {
        private final Vec3 rayStart;
        private final Vec3 rayEnd;

        private ProjectedRayService(Vec3 rayStart, Vec3 rayDirection) {
            this.rayStart = rayStart;
            rayEnd = rayStart.add(rayDirection.scale(LaserBehaviour.MAX_RANGE));
        }

        @Override
        public RayProjection projectRay(Level level, Position start, Position end) {
            return new RayProjection(rayStart, rayEnd, true);
        }
    }
}
