package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.end.endcasing.EndMechanicalBlock;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EndIncinerationBlowerBlock extends EndMechanicalBlock implements IBE<EndIncinerationBlowerBlockEntity> {
    public EndIncinerationBlowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
        withBlockEntityDo(level, pos, EndIncinerationBlowerBlockEntity::updateStructural);
        if (!(entity instanceof ServerPlayer player)) {
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