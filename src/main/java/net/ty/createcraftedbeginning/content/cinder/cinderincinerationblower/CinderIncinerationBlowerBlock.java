package net.ty.createcraftedbeginning.content.cinder.cinderincinerationblower;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.Tags.Items;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class CinderIncinerationBlowerBlock extends HorizontalKineticBlock implements IBE<CinderIncinerationBlowerBlockEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CinderIncinerationBlowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (!stack.is(Items.TOOLS_WRENCH)) {
            return ItemInteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        withBlockEntityDo(level, pos, CinderIncinerationBlowerBlockEntity::toggleShowOutline);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(placer instanceof ServerPlayer player)) {
            return;
        }

        withBlockEntityDo(level, pos, be -> be.setOwner(player.getUUID()));
    }

    @Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!isMoving && !state.is(newState.getBlock())) {
            withBlockEntityDo(level, pos, CinderIncinerationBlowerBlockEntity::discardPlayer);
        }
		super.onRemove(state, level, pos, newState, isMoving);
	}

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction direction) {
        return direction == Direction.DOWN;
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.Y;
    }

    @Override
    public Class<CinderIncinerationBlowerBlockEntity> getBlockEntityClass() {
        return CinderIncinerationBlowerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CinderIncinerationBlowerBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.CINDER_INCINERATION_BLOWER.get();
    }
}
