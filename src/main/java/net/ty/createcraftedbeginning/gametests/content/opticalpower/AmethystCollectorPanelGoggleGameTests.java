package net.ty.createcraftedbeginning.gametests.content.opticalpower;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel.AmethystCollectorPanelBlock;
import net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel.AmethystCollectorPanelBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel.AmethystCollectorPanelOutput;
import net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel.AmethystCollectorPanelOutput.Limitation;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AmethystCollectorPanelGoggleGameTests {
    private static final long DAY_TIME = 6000;
    private static final long NIGHT_TIME = 18000;

    private AmethystCollectorPanelGoggleGameTests() {
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 40)
    public static void allArrayMembersShowTotalOutputAndWeatherReason(GameTestHelper helper) {
        helper.setBiome(Biomes.PLAINS);
        BlockState panel = CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.getDefaultState();
        for (int x = 4; x <= 6; x++) {
            for (int z = 4; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 2, z), panel);
            }
        }
        BlockPos extraPanel = new BlockPos(7, 2, 4);
        helper.setBlock(extraPanel, panel);
        helper.runAfterDelay(5, () -> {
            ServerLevel level = helper.getLevel();
            long previousTime = level.getDayTime();
            float previousRain = level.getRainLevel(1);
            float previousThunder = level.getThunderLevel(1);
            double previousRainMultiplier = CCBConfig.server().opticalPower.amethystCollectorPanel.rainOutputMultiplier.get();
            try {
                level.setDayTime(DAY_TIME);
                level.setRainLevel(0);
                level.setThunderLevel(0);
                level.updateSkyBrightness();
                for (int x = 4; x <= 6; x++) {
                    for (int z = 4; z <= 6; z++) {
                        assertGoggleOutput(helper, new BlockPos(x, 2, z), 4, Limitation.NONE);
                    }
                }
                assertGoggleOutput(helper, extraPanel, 4, Limitation.NONE);
                level.setDayTime(NIGHT_TIME);
                level.updateSkyBrightness();
                assertGoggleOutput(helper, extraPanel, 0, Limitation.NIGHT);
                level.setDayTime(DAY_TIME);
                level.setRainLevel(1);
                level.updateSkyBrightness();
                BlockPos rainSample = helper.absolutePos(new BlockPos(4, 3, 4));
                BlockPos rainColumnTop = level.getHeightmapPos(Types.MOTION_BLOCKING, rainSample);
                for (BlockPos pos : BlockPos.betweenClosed(rainSample, rainColumnTop)) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
                helper.assertTrue(level.isRainingAt(rainSample), "Test structure roof still blocked rainfall at the collector anchor.");
                CCBConfig.server().opticalPower.amethystCollectorPanel.rainOutputMultiplier.set(1.0);
                assertGoggleOutput(helper, extraPanel, 2, Limitation.RAIN);
                CCBConfig.server().opticalPower.amethystCollectorPanel.rainOutputMultiplier.set(2.0);
                assertGoggleOutput(helper, extraPanel, 4, Limitation.NONE);
                CCBConfig.server().opticalPower.amethystCollectorPanel.rainOutputMultiplier.set(0.0);
                assertGoggleOutput(helper, extraPanel, 0, Limitation.RAIN);
            }
            finally {
                CCBConfig.server().opticalPower.amethystCollectorPanel.rainOutputMultiplier.set(previousRainMultiplier);
                level.setDayTime(previousTime);
                level.setRainLevel(previousRain);
                level.setThunderLevel(previousThunder);
                level.updateSkyBrightness();
            }
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 40)
    public static void obstructionAndOversizedArrayExplainZeroOutput(GameTestHelper helper) {
        BlockState panel = CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.getDefaultState();
        BlockPos covered = new BlockPos(3, 2, 3);
        helper.setBlock(covered, panel);
        helper.setBlock(covered.above(2), Blocks.STONE);
        for (int x = 6; x <= 11; x++) {
            helper.setBlock(new BlockPos(x, 2, 6), panel);
        }
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(!helper.getLevel().canSeeSky(helper.absolutePos(covered.above())), "Waiting for the collector obstruction to update sky light.")).thenExecute(() -> {
            ServerLevel level = helper.getLevel();
            long previousTime = level.getDayTime();
            float previousRain = level.getRainLevel(1);
            float previousThunder = level.getThunderLevel(1);
            try {
                level.setDayTime(DAY_TIME);
                level.setRainLevel(0);
                level.setThunderLevel(0);
                level.updateSkyBrightness();
                assertGoggleOutput(helper, covered, 0, Limitation.OBSTRUCTED);
                assertGoggleOutput(helper, new BlockPos(6, 2, 6), 0, Limitation.OVERSIZED);
                assertGoggleOutput(helper, new BlockPos(11, 2, 6), 0, Limitation.OVERSIZED);
            }
            finally {
                level.setDayTime(previousTime);
                level.setRainLevel(previousRain);
                level.setThunderLevel(previousThunder);
                level.updateSkyBrightness();
            }
        }).thenSucceed();
    }

    private static void assertGoggleOutput(GameTestHelper helper, BlockPos relativePos, int expectedPowerLp, Limitation limitation) {
        AmethystCollectorPanelBlockEntity panel = helper.getBlockEntity(relativePos);
        AmethystCollectorPanelOutput output = panel.getOutput();
        if (output == null) {
            throw new NullPointerException("Missing collector goggle output at " + relativePos + '.');
        }

        helper.assertTrue(output.powerLp() == expectedPowerLp, "Collector goggles expected " + expectedPowerLp + " LP (" + limitation + ") at " + relativePos + ", got " + output + '.');
        helper.assertTrue(output.limitation() == limitation, "Collector goggles reported the wrong limitation at " + relativePos + ": " + output.limitation() + '.');
        AmethystCollectorPanelBlock block = CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.get();
        int actualPowerLp = block.getOpticalPowerSource(helper.getLevel(), helper.absolutePos(relativePos), panel.getBlockState()).powerLp();
        helper.assertTrue(actualPowerLp == expectedPowerLp, "Goggle output disagreed with the optical network source.");
    }
}
