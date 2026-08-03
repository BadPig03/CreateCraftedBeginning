package net.ty.createcraftedbeginning.content.end.endcasing;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class EndMechanicalStructuralBlock extends KineticBlock {
    public EndMechanicalStructuralBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return 15;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        if (!(level.getBlockState(pos).getBlock() instanceof EndMechanicalStructuralBlock)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(CCBBlocks.END_CASING_BLOCK);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block otherBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, otherBlock, neighborPos, isMoving);
        if (level.isClientSide || isMoving || !neighborPos.equals(pos.above())) {
            return;
        }

        level.scheduleTick(pos, this, 1);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        BlockState lootState = builder.getOptionalParameter(LootContextParams.BLOCK_STATE);
        if (lootState == null || !(lootState.getBlock() instanceof EndMechanicalStructuralBlock)) {
            return drops;
        }
        return List.of(new ItemStack(CCBBlocks.END_CASING_BLOCK));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof EndMechanicalStructuralBlockEntity<?> blockEntity)) {
            return;
        }

        blockEntity.verifyMaster();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction direction) {
        return direction.getAxis() == Axis.Y;
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.Y;
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;
    }
}
