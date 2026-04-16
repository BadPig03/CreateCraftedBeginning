package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.Tags.Items;
import net.ty.createcraftedbeginning.content.end.endcasing.EndMechanicalBlock;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class EndIncinerationBlowerBlock extends EndMechanicalBlock implements IBE<EndIncinerationBlowerBlockEntity> {
    public EndIncinerationBlowerBlock(Properties properties) {
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

        withBlockEntityDo(level, pos, EndIncinerationBlowerBlockEntity::toggleShowOutline);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState blockState, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, blockState, placer, stack);
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
    public Class<EndIncinerationBlowerBlockEntity> getBlockEntityClass() {
        return EndIncinerationBlowerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EndIncinerationBlowerBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.END_INCINERATION_BLOWER.get();
    }
}