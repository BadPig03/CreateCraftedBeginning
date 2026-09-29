package net.ty.createcraftedbeginning.gametests.content.opticalpower;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AmethystCollectorPanelShapeGameTests {
    private AmethystCollectorPanelShapeGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void missingOriginUsesSingleCollectorShape(GameTestHelper helper) {
        BlockState state = CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.getDefaultState();
        BlockGetter view = new SparseBlockGetter(Map.of());
        helper.assertTrue(!Shapes.joinIsNotEmpty(state.getShape(view, BlockPos.ZERO), CCBShapes.AMETHYST_COLLECTOR_PANEL_SHAPE, BooleanOp.NOT_SAME), "Missing origin did not use the single collector outline");
        helper.assertTrue(!Shapes.joinIsNotEmpty(state.getCollisionShape(view, BlockPos.ZERO), CCBShapes.AMETHYST_COLLECTOR_PANEL_SHAPE, BooleanOp.NOT_SAME), "Missing origin did not use the single collector collision shape");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void missingNeighborsUseSingleCollectorShape(GameTestHelper helper) {
        BlockState state = CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.getDefaultState();
        BlockGetter view = new SparseBlockGetter(Map.of(BlockPos.ZERO, state));
        helper.assertTrue(!Shapes.joinIsNotEmpty(state.getShape(view, BlockPos.ZERO), CCBShapes.AMETHYST_COLLECTOR_PANEL_SHAPE, BooleanOp.NOT_SAME), "Missing neighbors changed the single collector outline");
        helper.assertTrue(!Shapes.joinIsNotEmpty(state.getCollisionShape(view, BlockPos.ZERO), CCBShapes.AMETHYST_COLLECTOR_PANEL_SHAPE, BooleanOp.NOT_SAME), "Missing neighbors changed the single collector collision shape");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void availableArrayKeepsItsDynamicShape(GameTestHelper helper) {
        BlockState state = CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.getDefaultState();
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                states.put(new BlockPos(x, 0, z), state);
            }
        }
        BlockGetter view = new SparseBlockGetter(states);
        helper.assertTrue(!Shapes.joinIsNotEmpty(state.getShape(view, BlockPos.ZERO), Block.box(0, 8, 0, 16, 10, 16), BooleanOp.NOT_SAME), "Array center retained an internal frame in its outline");
        helper.assertTrue(!Shapes.joinIsNotEmpty(state.getCollisionShape(view, BlockPos.ZERO), Block.box(0, 8, 0, 16, 10, 16), BooleanOp.NOT_SAME), "Array center retained an internal frame in its collision shape");
        helper.succeed();
    }

    private record SparseBlockGetter(Map<BlockPos, BlockState> states) implements BlockGetter {
        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public @Nullable BlockState getBlockState(BlockPos pos) {
            return states.get(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return Fluids.EMPTY.defaultFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinBuildHeight() {
            return -64;
        }
    }
}
