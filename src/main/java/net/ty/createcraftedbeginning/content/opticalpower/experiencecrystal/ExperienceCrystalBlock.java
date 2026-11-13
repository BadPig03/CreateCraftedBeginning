package net.ty.createcraftedbeginning.content.opticalpower.experiencecrystal;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ExperienceCrystalBlock extends AmethystClusterBlock {
    public static final int SMALL_STAGE = 0;
    public static final int MEDIUM_STAGE = 1;
    public static final int MATURE_STAGE = 2;

    private final int stage;

    public ExperienceCrystalBlock(int stage, Properties properties) {
        super(switch (stage) {
            case SMALL_STAGE -> 7;
            case MEDIUM_STAGE -> 11;
            case MATURE_STAGE -> 15;
            default -> throw new IllegalArgumentException("Experience crystal stage must be " + SMALL_STAGE + ", " + MEDIUM_STAGE + ", or " + MATURE_STAGE + "; got " + stage + '.');
        }, stage == MATURE_STAGE ? 2 : 3, properties);
        this.stage = stage;
    }

    @Internal
    public static boolean canSeedIn(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER) && state.getFluidState().isSourceOfType(Fluids.WATER);
    }

    @Internal
    public static boolean tryGrow(ServerLevel level, BlockPos pos, Direction facing) {
        BlockPos supportPos = pos.relative(facing.getOpposite());
        if (!level.isLoaded(pos) || !level.isLoaded(supportPos) || !level.getBlockState(supportPos).is(Blocks.SCULK)) {
            return false;
        }

        BlockState current = level.getBlockState(pos);
        int nextStage = SMALL_STAGE;
        boolean waterlogged;
        if (current.getBlock() instanceof ExperienceCrystalBlock crystal) {
            if (current.getValue(FACING) != facing || crystal.isMature()) {
                return false;
            }

            nextStage = crystal.stage + 1;
            waterlogged = current.getValue(WATERLOGGED);
        }
        else {
            if (!canSeedIn(current)) {
                return false;
            }

            waterlogged = current.is(Blocks.WATER);
        }

        BlockState next = switch (nextStage) {
            case SMALL_STAGE -> CCBBlocks.SMALL_EXPERIENCE_BUD.getDefaultState();
            case MEDIUM_STAGE -> CCBBlocks.MEDIUM_EXPERIENCE_BUD.getDefaultState();
            default -> CCBBlocks.EXPERIENCE_CLUSTER.getDefaultState();
        };
        return level.setBlockAndUpdate(pos, next.setValue(FACING, facing).setValue(WATERLOGGED, waterlogged));
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer) || player instanceof FakePlayer || !isMature()) {
            return;
        }

        BlockPos support = pos.relative(state.getValue(FACING).getOpposite());
        if (!serverLevel.isLoaded(support) || !serverLevel.getBlockState(support).is(Blocks.SCULK)) {
            return;
        }

        CCBAdvancements.SCULK_HARVEST.awardTo(serverPlayer);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING).getOpposite())).is(Blocks.SCULK);
    }

    public boolean isMature() {
        return stage == MATURE_STAGE;
    }
}
