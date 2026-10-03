package net.ty.createcraftedbeginning.gametests.compat;

import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructural;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructural;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
public final class MultiblockSubLevelGameTests {
    private static final List<BlockPos> CUBE_BREAK_OFFSETS = List.of(new BlockPos(1, 1, 1), new BlockPos(1, 1, 0), new BlockPos(0, 1, 0), BlockPos.ZERO);

    @GameTestGenerator
    public static Collection<TestFunction> subLevelDestruction() {
        if (!CCBCompatMods.SIMULATED.isLoaded() || !CCBCompatMods.SABLE.isLoaded()) {
            return List.of();
        }

        List<TestFunction> tests = new ArrayList<>();
        tests.add(createTest("reactor", CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.getDefaultState(), CUBE_BREAK_OFFSETS, 27));
        tests.add(createTest("press", CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK.getDefaultState(), CUBE_BREAK_OFFSETS, 27));
        BlockState turbineState = CCBBlocks.TESLA_TURBINE_BLOCK.getDefaultState();
        for (Axis axis : Axis.values()) {
            List<BlockPos> offsets = switch (axis) {
                case X -> List.of(new BlockPos(0, 1, 1), new BlockPos(0, 1, 0), BlockPos.ZERO);
                case Y -> List.of(new BlockPos(1, 0, 1), new BlockPos(1, 0, 0), BlockPos.ZERO);
                case Z -> List.of(new BlockPos(1, 1, 0), new BlockPos(1, 0, 0), BlockPos.ZERO);
            };
            tests.add(createTest("turbine_" + axis.getName() + "_empty", turbineState.setValue(TeslaTurbineBlock.AXIS, axis), offsets, 9));
            tests.add(createTest("turbine_" + axis.getName() + "_rotors", turbineState.setValue(TeslaTurbineBlock.AXIS, axis).setValue(TeslaTurbineBlock.ROTOR, 3), offsets, 9));
        }
        return tests;
    }

    private static TestFunction createTest(String name, BlockState state, List<BlockPos> breakOffsets, int blockCount) {
        String batch = "physical_destruction_" + name;
        return new TestFunction(batch, batch + ".fragments", CCBAPI.MOD_ID + ":gametest/empty_20x12x20", 200, 0, true, helper -> breakAssembledMachine(helper, state, breakOffsets, blockCount, 0));
    }

    private static void breakAssembledMachine(GameTestHelper helper, BlockState state, List<BlockPos> breakOffsets, int blockCount, int step) {
        BlockPos relativeCore = new BlockPos(3, 3, 3);
        helper.setBlock(relativeCore, state);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            BlockPos core = helper.absolutePos(relativeCore);
            Set<UUID> existingSubLevels = new HashSet<>();
            for (Object existing : activeSubLevels(level)) {
                existingSubLevels.add(((SubLevelAccess) existing).getUniqueId());
            }
            Set<BlockPos> parts = MultiblockAssemblyGameTests.gather(level, core);
            BlockPos movedCore;
            try {
                BoundingBox3i bounds = BoundingBox3i.from(parts);
                if (bounds == null) {
                    throw new NullPointerException("Expected assembly bounds at " + core + '.');
                }

                Class<?> mover = Class.forName("dev.ryanhcode.sable.api.SubLevelAssemblyHelper");
                Object subLevel = mover.getMethod("assembleBlocks", ServerLevel.class, BlockPos.class, Iterable.class, BoundingBox3ic.class).invoke(null, level, core, parts, bounds);
                if (subLevel == null) {
                    throw new NullPointerException("Expected an assembled machine sub-level at " + core + '.');
                }

                Object plot = subLevel.getClass().getMethod("getPlot").invoke(subLevel);
                if (plot == null) {
                    throw new NullPointerException("Expected a machine sub-level plot at " + core + '.');
                }

                movedCore = (BlockPos) plot.getClass().getMethod("getCenterBlock").invoke(plot);
                if (movedCore == null) {
                    throw new NullPointerException("Expected a machine sub-level center at " + core + '.');
                }
            }
            catch (ReflectiveOperationException exception) {
                Throwable cause = exception instanceof InvocationTargetException invocation ? invocation.getCause() : exception;
                throw new IllegalStateException("Failed to assemble a machine sub-level at " + core + '.', cause);
            }

            helper.runAfterDelay(10, () -> {
                Set<BlockPos> movedParts = MultiblockAssemblyGameTests.gather(level, movedCore);
                helper.assertValueEqual(movedParts.size(), blockCount, "assembled machine block count");
                level.destroyBlock(movedCore.offset(breakOffsets.get(step)), true);
                for (BlockPos part : movedParts) {
                    helper.assertTrue(level.getBlockState(part).isAir(), "Machine structure remained after destruction at " + part);
                }
                helper.runAfterDelay(20, () -> {
                    helper.assertValueEqual(countMachineFragments(level, existingSubLevels), 0, "remaining machine fragments across sub-levels");
                    if (step + 1 < breakOffsets.size()) {
                        breakAssembledMachine(helper, state, breakOffsets, blockCount, step + 1);
                        return;
                    }

                    helper.succeed();
                });
            });
        });
    }

    private static List<?> activeSubLevels(ServerLevel level) {
        Object container;
        try {
            Class<?> containers = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            container = MethodHandles.publicLookup().findStatic(containers, "getContainer", MethodType.methodType(Class.forName("dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer"), ServerLevel.class)).invoke(level);
        }
        catch (Throwable exception) {
            throw new IllegalStateException("Failed to access the server sub-level container for the multiblock destruction test.", exception);
        }
        if (container == null) {
            throw new NullPointerException("Expected a sub-level container for the multiblock destruction test.");
        }

        List<?> subLevels;
        try {
            subLevels = (List<?>) container.getClass().getMethod("getAllSubLevels").invoke(container);
        }
        catch (ReflectiveOperationException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation ? invocation.getCause() : exception;
            throw new IllegalStateException("Failed to list active sub-levels for the multiblock destruction test.", cause);
        }
        if (subLevels == null) {
            throw new NullPointerException("Expected active sub-levels for the multiblock destruction test.");
        }

        return subLevels;
    }

    private static int countMachineFragments(ServerLevel level, Set<UUID> existingSubLevels) {
        int remaining = 0;
        for (Object active : activeSubLevels(level)) {
            if (existingSubLevels.contains(((SubLevelAccess) active).getUniqueId())) {
                continue;
            }

            BoundingBox3ic bounds;
            try {
                Object plot = active.getClass().getMethod("getPlot").invoke(active);
                if (plot == null) {
                    throw new NullPointerException("Expected an active sub-level plot after multiblock destruction.");
                }

                bounds = (BoundingBox3ic) plot.getClass().getMethod("getBoundingBox").invoke(plot);
            }
            catch (ReflectiveOperationException exception) {
                Throwable cause = exception instanceof InvocationTargetException invocation ? invocation.getCause() : exception;
                throw new IllegalStateException("Failed to inspect a sub-level plot after multiblock destruction.", cause);
            }
            if (bounds == null) {
                throw new NullPointerException("Expected active sub-level bounds after multiblock destruction.");
            }

            if (bounds.minX() > bounds.maxX() || bounds.minY() > bounds.maxY() || bounds.minZ() > bounds.maxZ()) {
                continue;
            }

            for (BlockPos part : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
                BlockState state = level.getBlockState(part);
                Block block = state.getBlock();
                if (!(block instanceof AirtightReactorKettleStructural) && !(block instanceof AirtightReactorKettleBlock) && !(block instanceof AirtightForgingPressStructural) && !(block instanceof AirtightForgingPressBlock) && !(block instanceof TeslaTurbineStructuralBlock) && !(block instanceof TeslaTurbineBlock)) {
                    continue;
                }

                remaining++;
            }
        }
        return remaining;
    }
}
