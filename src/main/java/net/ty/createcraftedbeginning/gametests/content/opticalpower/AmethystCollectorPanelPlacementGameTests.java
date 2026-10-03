package net.ty.createcraftedbeginning.gametests.content.opticalpower;

import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel.AmethystCollectorPanelPlacementHelper;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AmethystCollectorPanelPlacementGameTests {
    private AmethystCollectorPanelPlacementGameTests() {
    }

    @GameTest(template = "gametest/empty_20x12x20")
    public static void incompleteExpansionIgnoresRoofAndRejectsBlockedCompletion(GameTestHelper helper) {
        BlockState panel = CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.getDefaultState();
        for (int x = 4; x <= 5; x++) {
            for (int z = 4; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 2, z), panel);
            }
        }
        Level level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(5, 2, 4));
        BlockPos target = origin.east();
        helper.setBlock(new BlockPos(6, 3, 4), Blocks.STONE);
        helper.assertTrue(AmethystCollectorPanelPlacementHelper.canExtendArray(level, origin, target), "Roof or incomplete new row blocked collector expansion.");
        helper.assertTrue(!AmethystCollectorPanelPlacementHelper.canExtendArray(level, origin, origin.above()), "Collector placement suggested another height.");
        helper.setBlock(new BlockPos(6, 2, 5), Blocks.STONE);
        helper.assertTrue(!AmethystCollectorPanelPlacementHelper.canExtendArray(level, origin, target), "Collector suggested a rectangle whose missing corner is blocked.");
        helper.setBlock(new BlockPos(6, 2, 5), Blocks.AIR);
        helper.setBlock(new BlockPos(6, 2, 4), Blocks.STONE);
        helper.assertTrue(!AmethystCollectorPanelPlacementHelper.canExtendArray(level, origin, target), "Collector suggested an occupied position.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_20x12x20")
    public static void joinedArraysRespectFiveBlockLimit(GameTestHelper helper) {
        BlockState panel = CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.getDefaultState();
        for (int x : new int[]{2, 3, 5, 6, 7}) {
            helper.setBlock(new BlockPos(x, 2, 4), panel);
        }
        Level level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(3, 2, 4));
        helper.assertTrue(!AmethystCollectorPanelPlacementHelper.canExtendArray(level, origin, origin.east()), "Bridging two arrays exceeded the five-block width limit.");
        helper.setBlock(new BlockPos(7, 2, 4), Blocks.AIR);
        helper.assertTrue(AmethystCollectorPanelPlacementHelper.canExtendArray(level, origin, origin.east()), "Valid five-block merged array was rejected.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_20x12x20")
    public static void previewFollowsNearestHorizontalEdgeAndSneakingDisablesIt(GameTestHelper helper) {
        BlockPos relative = new BlockPos(4, 2, 4);
        BlockState panel = CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.getDefaultState();
        helper.setBlock(relative, panel);
        BlockPos origin = helper.absolutePos(relative);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        AmethystCollectorPanelPlacementHelper placement = new AmethystCollectorPanelPlacementHelper();
        BlockHitResult ray = new BlockHitResult(Vec3.atLowerCornerOf(origin).add(0.99, 0.6, 0.5), Direction.UP, origin, false);
        PlacementOffset offset = placement.getOffset(player, helper.getLevel(), panel, origin, ray);
        helper.assertTrue(offset.isSuccessful() && offset.getBlockPos().equals(origin.east()), "Collector preview did not follow the nearest horizontal edge.");
        player.setShiftKeyDown(true);
        helper.assertTrue(!placement.getOffset(player, helper.getLevel(), panel, origin, ray).isSuccessful(), "Sneaking retained the collector placement preview.");
        helper.succeed();
    }
}
