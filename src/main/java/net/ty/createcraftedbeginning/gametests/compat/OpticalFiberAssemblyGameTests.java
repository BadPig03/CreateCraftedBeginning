package net.ty.createcraftedbeginning.gametests.compat;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetworkManager;
import net.ty.createcraftedbeginning.content.opticalpower.opticalfiber.OpticalFiberBlock;
import net.ty.createcraftedbeginning.gametests.compat.SubLevelGameTestFixtures.Fixture;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
public final class OpticalFiberAssemblyGameTests {
    private static final String COMPOUND_KEY_OPTICAL_POWER_LP = "OpticalPowerLp";
    private static final String BATCH = "optical_fiber_assembly";
    private static final String HORIZONTAL_EMITTER_BATCH = BATCH + ".horizontal_emitter_rotation";
    private static final String TEMPLATE = CCBAPI.MOD_ID + ":gametest/empty_20x12x20";
    private static final int CACHE_SETTLE_STEPS = 20;

    private OpticalFiberAssemblyGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> physicalAssembly() {
        if (!CCBCompatMods.SABLE.isLoaded() || !CCBCompatMods.SIMULATED.isLoaded()) {
            return List.of();
        }

        List<TestFunction> tests = new ArrayList<>();
        tests.add(new TestFunction(BATCH, BATCH + ".complete", TEMPLATE, 60, 0, true, helper -> verifyPhysicalAssembly(helper, true)));
        tests.add(new TestFunction(BATCH, BATCH + ".separated_boundary", TEMPLATE, 60, 0, true, helper -> verifyPhysicalAssembly(helper, false)));
        tests.add(new TestFunction(BATCH, BATCH + ".source_only", TEMPLATE, 60, 0, true, OpticalFiberAssemblyGameTests::verifySourceOnlyAssembly));
        for (Rotation rotation : Rotation.values()) {
            tests.add(new TestFunction(BATCH, BATCH + ".rotation_" + rotation.ordinal(), TEMPLATE, 60, 0, true, helper -> verifyRotatedMove(helper, rotation)));
            tests.add(new TestFunction(HORIZONTAL_EMITTER_BATCH, HORIZONTAL_EMITTER_BATCH + '_' + rotation.ordinal(), TEMPLATE, 60, 0, true, helper -> verifyHorizontalEmitterRotation(helper, rotation)));
        }
        return tests;
    }

    private static void verifyPhysicalAssembly(GameTestHelper helper, boolean moveAll) {
        ServerLevel level = helper.getLevel();
        BlockPos fiberPos = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockPos externalPos = fiberPos.west();
        BlockPos sourcePos = externalPos.west();
        BlockPos emitterPos = fiberPos.east(2);
        BlockState fiber = CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState().setValue(OpticalFiberBlock.WEST, true).setValue(OpticalFiberBlock.EAST, true);
        level.setBlockAndUpdate(fiberPos, fiber);
        level.setBlockAndUpdate(fiberPos.east(), fiber);
        level.setBlockAndUpdate(externalPos, fiber);
        level.setBlockAndUpdate(sourcePos, Blocks.GLOWSTONE.defaultBlockState());
        level.setBlockAndUpdate(emitterPos, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.EAST));
        helper.runAfterDelay(2, () -> {
            settleNetworks(level);
            assertEmitterPower(helper, emitterPos, 1);
            Set<BlockPos> parts = new HashSet<>(List.of(fiberPos, fiberPos.east(), emitterPos));
            if (moveAll) {
                parts.add(externalPos);
                parts.add(sourcePos);
            }
            Fixture fixture = SubLevelGameTestFixtures.assemble(level, fiberPos, parts);
            BlockPos moved = fixture.center();
            Direction[] centerConnections = moveAll ? new Direction[]{Direction.WEST, Direction.EAST} : new Direction[]{Direction.EAST};
            try {
                assertConnections(helper, moved, centerConnections);
                assertConnections(helper, moved.east(), Direction.WEST, Direction.EAST);
                helper.assertTrue(OpticalPowerNetworkManager.findCachedNetwork(level, emitterPos) == null, "Assembly retained the optical cache at the old emitter position.");
                if (!moveAll) {
                    assertConnections(helper, externalPos, Direction.WEST);
                    helper.assertTrue(level.getBlockState(moved.west()).isAir(), "Separated physical fiber still had a local neighbor across the assembly boundary.");
                }
            }
            catch (RuntimeException | Error exception) {
                SubLevelGameTestFixtures.clear(level, fixture);
                throw exception;
            }
            helper.runAfterDelay(5, () -> {
                try {
                    assertConnections(helper, moved, centerConnections);
                    assertConnections(helper, moved.east(), Direction.WEST, Direction.EAST);
                    settleNetworks(level);
                    assertEmitterPower(helper, moved.east(2), moveAll ? 1 : 0);
                    if (moveAll) {
                        assertConnections(helper, moved.west(), Direction.WEST, Direction.EAST);
                        helper.succeed();
                        return;
                    }

                    assertConnections(helper, externalPos, Direction.WEST);
                    helper.assertTrue(level.getBlockState(sourcePos).is(Blocks.GLOWSTONE), "Partial fiber assembly removed its stationary source.");
                    helper.succeed();
                }
                finally {
                    SubLevelGameTestFixtures.clear(level, fixture);
                }
            });
        });
    }

    private static void verifySourceOnlyAssembly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fiberPos = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockPos sourcePos = fiberPos.west();
        BlockPos emitterPos = fiberPos.east();
        level.setBlockAndUpdate(fiberPos, CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState());
        level.setBlockAndUpdate(sourcePos, Blocks.GLOWSTONE.defaultBlockState());
        level.setBlockAndUpdate(emitterPos, CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.EAST));
        helper.runAfterDelay(2, () -> {
            settleNetworks(level);
            assertEmitterPower(helper, emitterPos, 1);
            Fixture fixture = SubLevelGameTestFixtures.assemble(level, sourcePos, Set.of(sourcePos));
            try {
                assertConnections(helper, fiberPos, Direction.EAST);
                assertEmitterPower(helper, emitterPos, 0);
                helper.assertTrue(OpticalPowerNetworkManager.findCachedNetwork(level, emitterPos) == null, "Moving only the source retained the stationary optical network cache.");
                settleNetworks(level);
                assertEmitterPower(helper, emitterPos, 0);
                helper.assertTrue(OpticalFiberBlock.getDeviceConnections(level, fiberPos, level.getBlockState(fiberPos)) == 1 << Direction.EAST.get3DDataValue(), "Moving the source retained its optical fiber device port.");
                helper.succeed();
            }
            finally {
                SubLevelGameTestFixtures.clear(level, fixture);
            }
        });
    }

    private static void verifyRotatedMove(GameTestHelper helper, Rotation rotation) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockPos destination = helper.absolutePos(new BlockPos(12, 3, 12));
        BlockPos stationaryOld = origin.south();
        BlockPos stationaryNew = destination.relative(rotation.rotate(Direction.NORTH));
        BlockState fiber = CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState();
        level.setBlockAndUpdate(origin, fiber);
        level.setBlockAndUpdate(origin.east(), fiber.setValue(OpticalFiberBlock.WEST, true));
        level.setBlockAndUpdate(origin.west(), Blocks.GLOWSTONE.defaultBlockState());
        level.setBlockAndUpdate(origin.above(), CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.UP));
        level.setBlockAndUpdate(stationaryOld, fiber.setValue(OpticalFiberBlock.NORTH, true));
        level.setBlockAndUpdate(stationaryNew, fiber);
        helper.runAfterDelay(2, () -> {
            settleNetworks(level);
            assertEmitterPower(helper, origin.above(), 1);
            MultiblockAssemblyGameTests.move(level, origin, destination, rotation, List.of(origin, origin.east(), origin.west(), origin.above()));
            assertConnections(helper, stationaryOld);
            assertConnections(helper, destination, rotation.rotate(Direction.EAST), rotation.rotate(Direction.WEST), rotation.rotate(Direction.NORTH), Direction.UP);
            assertConnections(helper, destination.relative(rotation.rotate(Direction.EAST)), rotation.rotate(Direction.WEST));
            assertConnections(helper, stationaryNew, rotation.rotate(Direction.SOUTH));
            helper.assertTrue(OpticalPowerNetworkManager.findCachedNetwork(level, origin.above()) == null, "Rotated relocation retained the old emitter network cache.");
            for (BlockPos old : List.of(origin, origin.east(), origin.west(), origin.above())) {
                helper.assertTrue(level.getBlockState(old).isAir(), "Rotated optical relocation retained a moved block at " + old + '.');
            }
            helper.runAfterDelay(2, () -> {
                settleNetworks(level);
                assertEmitterPower(helper, destination.above(), 1);
                assertConnections(helper, stationaryOld);
                assertConnections(helper, destination, rotation.rotate(Direction.EAST), rotation.rotate(Direction.WEST), rotation.rotate(Direction.NORTH), Direction.UP);
                helper.succeed();
            });
        });
    }

    private static void verifyHorizontalEmitterRotation(GameTestHelper helper, Rotation rotation) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockPos destination = helper.absolutePos(new BlockPos(12, 3, 12));
        Direction expectedFacing = rotation.rotate(Direction.EAST);
        BlockPos emitterPos = destination.relative(expectedFacing);
        level.setBlockAndUpdate(origin, CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState());
        level.setBlockAndUpdate(origin.west(), Blocks.GLOWSTONE.defaultBlockState());
        level.setBlockAndUpdate(origin.east(), CCBBlocks.LASER_EMITTER_BLOCK.getDefaultState().setValue(DirectionalBlock.FACING, Direction.EAST));
        LaserEmitterBlockEntity originalEmitter = CCBBlocks.LASER_EMITTER_BLOCK.get().getBlockEntity(level, origin.east());
        if (originalEmitter == null) {
            throw new NullPointerException("Missing horizontal laser emitter before range preservation test at " + origin.east() + '.');
        }

        ScrollValueBehaviour range = originalEmitter.getBehaviour(ScrollValueBehaviour.TYPE);
        if (range == null) {
            throw new NullPointerException("Missing laser range control before physical assembly at " + origin.east() + '.');
        }

        range.setValue(7);
        helper.runAfterDelay(2, () -> {
            settleNetworks(level);
            assertEmitterPower(helper, origin.east(), 1);
            Fixture fixture = SubLevelGameTestFixtures.assemble(level, origin, Set.of(origin, origin.west(), origin.east()));
            BlockPos center = fixture.center();
            try {
                MultiblockAssemblyGameTests.move(level, center, destination, rotation, List.of(center, center.west(), center.east(), center.north()));
                BlockState emitterState = level.getBlockState(emitterPos);
                helper.assertTrue(emitterState.is(CCBBlocks.LASER_EMITTER_BLOCK.get()), "Missing horizontal laser emitter after rotated physical relocation at " + emitterPos + '.');
                Direction actualFacing = emitterState.getValue(DirectionalBlock.FACING);
                helper.assertTrue(actualFacing == expectedFacing, "Expected horizontal laser emitter facing " + expectedFacing + " after " + rotation + ", found " + actualFacing + '.');
                LaserEmitterBlockEntity movedEmitter = CCBBlocks.LASER_EMITTER_BLOCK.get().getBlockEntity(level, emitterPos);
                if (movedEmitter == null) {
                    throw new NullPointerException("Missing relocated laser emitter for range preservation at " + emitterPos + '.');
                }

                helper.assertTrue(movedEmitter.getLaserRange() == 7, "Physical assembly and rotation lost the selected laser range.");
                assertConnections(helper, destination, expectedFacing, expectedFacing.getOpposite());
            }
            finally {
                SubLevelGameTestFixtures.clear(level, fixture);
            }
            helper.runAfterDelay(2, () -> {
                settleNetworks(level);
                assertEmitterPower(helper, emitterPos, 1);
                assertConnections(helper, destination, expectedFacing, expectedFacing.getOpposite());
                helper.succeed();
            });
        });
    }

    private static void assertConnections(GameTestHelper helper, BlockPos pos, Direction... connections) {
        ServerLevel level = helper.getLevel();
        BlockState actual = level.getBlockState(pos);
        helper.assertTrue(actual.getBlock() instanceof OpticalFiberBlock, "Missing assembled optical fiber at " + pos + '.');
        List<Direction> expectedConnections = List.of(connections);
        for (Direction direction : Iterate.directions) {
            boolean connected = expectedConnections.contains(direction);
            helper.assertTrue(OpticalFiberBlock.isConnected(actual, direction) == connected, "Unexpected assembled fiber connection toward " + direction + " at " + pos + '.');
        }
    }

    private static void assertEmitterPower(GameTestHelper helper, BlockPos emitterPos, int expectedPowerLp) {
        ServerLevel level = helper.getLevel();
        LaserEmitterBlockEntity emitter = CCBBlocks.LASER_EMITTER_BLOCK.get().getBlockEntity(level, emitterPos);
        if (emitter == null) {
            throw new NullPointerException("Missing assembled laser emitter at " + emitterPos + '.');
        }

        int powerLp = emitter.getUpdateTag(level.registryAccess()).getInt(COMPOUND_KEY_OPTICAL_POWER_LP);
        helper.assertTrue(powerLp == expectedPowerLp, "Expected " + expectedPowerLp + " LP after optical assembly at " + emitterPos + ", found " + powerLp + " LP.");
    }

    private static void settleNetworks(ServerLevel level) {
        for (int step = 0; step < CACHE_SETTLE_STEPS; step++) {
            OpticalPowerNetworkManager.tick(level);
        }
    }
}
