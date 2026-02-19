package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.simibubi.create.content.kinetics.base.KineticBlock;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.Tags.Items;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public class EndIncinerationBlowerBlock extends KineticBlock implements IBE<EndIncinerationBlowerBlockEntity> {
    public EndIncinerationBlowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getLightEmission(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return 15;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (!stack.is(Items.TOOLS_WRENCH)) {
            return ItemInteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        withBlockEntityDo(level, pos, EndIncinerationBlowerBlockEntity::toggleShowOutline);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState blockState, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, blockState, placer, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, placer);
        withBlockEntityDo(level, pos, EndIncinerationBlowerBlockEntity::updateStructural);
        if (!(placer instanceof ServerPlayer player)) {
            return;
        }

        withBlockEntityDo(level, pos, be -> be.setOwner(player.getUUID()));
    }

    @Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!isMoving && !state.is(newState.getBlock())) {
            withBlockEntityDo(level, pos, EndIncinerationBlowerBlockEntity::discardPlayer);
        }
		super.onRemove(state, level, pos, newState, isMoving);
	}

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        Level level = context.getLevel();
        Direction direction = context.getClickedFace();
        BlockPos placePos = context.getClickedPos().relative(direction);
        if (!level.getBlockState(placePos).is(CCBBlocks.END_CASING_BLOCK)) {
            return null;
        }

        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return null;
        }

        return state;
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction direction) {
        return direction == Direction.DOWN;
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.Y;
    }

    @Override
    public Class<EndIncinerationBlowerBlockEntity> getBlockEntityClass() {
        return EndIncinerationBlowerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EndIncinerationBlowerBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.END_INCINERATION_BLOWER.get();
    }
}