package net.ty.createcraftedbeginning.gametests.compat;

import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;
import net.ty.createcraftedbeginning.gametests.compat.SubLevelGameTestFixtures.Fixture;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
public final class SubLevelFixtureLifecycleGameTests {
    private static final String BATCH = "physical_fixture_lifecycle";
    private static final String TEMPLATE = CCBAPI.MOD_ID + ":gametest/empty_20x12x20";

    private SubLevelFixtureLifecycleGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> fixtureCleanup() {
        if (!CCBCompatMods.SABLE.isLoaded() || !CCBCompatMods.SIMULATED.isLoaded()) {
            return List.of();
        }

        return List.of(new TestFunction(BATCH, BATCH + ".marked_removed", TEMPLATE, 40, 0, true, helper -> verifyRemovedFixtureCleanup(helper, false)), new TestFunction(BATCH, BATCH + ".empty_after_move", TEMPLATE, 40, 0, true, helper -> verifyRemovedFixtureCleanup(helper, true)), new TestFunction(BATCH, BATCH + ".reused_plot", TEMPLATE, 40, 0, true, SubLevelFixtureLifecycleGameTests::verifyReusedPlot));
    }

    private static void verifyRemovedFixtureCleanup(GameTestHelper helper, boolean moveAll) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockPos destination = helper.absolutePos(new BlockPos(12, 3, 12));
        level.setBlockAndUpdate(origin, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(origin.east(), Blocks.GLOWSTONE.defaultBlockState());
        Fixture fixture = SubLevelGameTestFixtures.assemble(level, origin, Set.of(origin, origin.east()));
        try {
            if (moveAll) {
                BlockPos center = fixture.center();
                MultiblockAssemblyGameTests.move(level, center, destination, Rotation.CLOCKWISE_90, List.of(center, center.east(), center.north()));
                fixture.subLevel().getClass().getMethod("updateBoundingBox").invoke(fixture.subLevel());
                helper.assertTrue(level.getBlockState(destination).is(Blocks.STONE), "Fixture target disappeared during physical relocation.");
            }
            else {
                fixture.subLevel().getClass().getMethod("markRemoved").invoke(fixture.subLevel());
            }

            helper.assertTrue((boolean) fixture.subLevel().getClass().getMethod("isRemoved").invoke(fixture.subLevel()), "Fixture did not reach the marked-removed lifecycle state.");
            helper.assertTrue(SableCompanion.INSTANCE.getContaining(level, fixture.center()) == fixture.subLevel(), "Marked fixture was removed from its container before cleanup could be tested.");
            SubLevelGameTestFixtures.clear(level, fixture);
            helper.assertTrue(SableCompanion.INSTANCE.getContaining(level, fixture.center()) == null, "Fixture cleanup left a marked-removed sub-level registered for another tick.");
        }
        catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to advance the physical fixture lifecycle at " + fixture.center() + '.', exception);
        }
        finally {
            flushRemovedFixtures(level);
        }
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(SableCompanion.INSTANCE.getContaining(level, fixture.center()) != fixture.subLevel(), "A removed fixture reappeared in the container after ticking.");
            helper.succeed();
        });
    }

    private static void verifyReusedPlot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(4, 3, 4));
        level.setBlockAndUpdate(origin, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(origin.east(2), Blocks.GOLD_BLOCK.defaultBlockState());
        Fixture first = SubLevelGameTestFixtures.assemble(level, origin, Set.of(origin, origin.east(2)));
        SubLevelGameTestFixtures.clear(level, first);
        helper.assertTrue(SableCompanion.INSTANCE.getContaining(level, first.center()) == null, "Initial fixture cleanup did not release its plot.");
        level.setBlockAndUpdate(origin, Blocks.STONE.defaultBlockState());
        Fixture replacement = SubLevelGameTestFixtures.assemble(level, origin, Set.of(origin));
        try {
            helper.assertTrue(first.center().equals(replacement.center()), "Replacement fixture did not reuse the released plot.");
            helper.assertTrue(level.getBlockState(replacement.center().east(2)).isAir(), "Replacement fixture inherited blocks from the previously removed plot.");
            SubLevelGameTestFixtures.clear(level, first);
            helper.assertTrue(SableCompanion.INSTANCE.getContaining(level, replacement.center()) == replacement.subLevel(), "Repeated cleanup removed the replacement fixture occupying the old plot.");
        }
        finally {
            SubLevelGameTestFixtures.clear(level, replacement);
            flushRemovedFixtures(level);
        }
        helper.succeed();
    }

    private static void flushRemovedFixtures(ServerLevel level) {
        Object container;
        try {
            container = Class.forName("dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder").getMethod("sable$getPlotContainer").invoke(level);
        }
        catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to access the sub-level container during fixture lifecycle cleanup.", exception);
        }
        if (container == null) {
            throw new NullPointerException("Missing sub-level container during fixture lifecycle cleanup.");
        }

        try {
            Class<?> containers = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            MethodHandles.publicLookup().findVirtual(containers, "processSubLevelRemovals", MethodType.methodType(void.class)).invoke(container);
        }
        catch (Throwable exception) {
            throw new IllegalStateException("Failed to flush removed sub-levels after fixture lifecycle validation.", exception);
        }
    }
}
