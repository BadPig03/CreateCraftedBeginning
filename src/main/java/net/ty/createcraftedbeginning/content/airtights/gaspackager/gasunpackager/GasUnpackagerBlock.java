package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasunpackager;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerPlacement;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasUnpackagerBlock extends WrenchableDirectionalBlock implements IBE<GasUnpackagerBlockEntity> {
    public GasUnpackagerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return GasPackagerPlacement.withGasFacing(context, super.getStateForPlacement(context));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moving);
        if (level.isClientSide()) {
            return;
        }

        withBlockEntityDo(level, pos, unpackager -> unpackager.targetChanged(neighborPos));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (AllItems.WRENCH.isIn(stack) || AllBlocks.PACKAGE_FROGPORT.isIn(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (AllBlocks.STOCK_LINK.isIn(stack) || AllBlocks.FACTORY_GAUGE.isIn(stack) || CCBBlocks.GAS_FACTORY_GAUGE_BLOCK.isIn(stack)) {
            return ItemInteractionResult.FAIL;
        }

        return onBlockEntityUseItemOn(level, pos, blockEntity -> {
            if (blockEntity.animationTicks > 0) {
                return ItemInteractionResult.SUCCESS;
            }

            if (!blockEntity.heldBox.isEmpty()) {
                if (!level.isClientSide()) {
                    player.getInventory().placeItemBackInInventory(blockEntity.heldBox.copy());
                    AllSoundEvents.playItemPickup(player);
                    blockEntity.heldBox = ItemStack.EMPTY;
                    blockEntity.notifyUpdate();
                }
                return ItemInteractionResult.SUCCESS;
            }

            if (!BalloonItem.isBalloon(stack)) {
                return ItemInteractionResult.SUCCESS;
            }

            if (!level.isClientSide() && blockEntity.unwrapBox(stack.copyWithCount(1), false)) {
                stack.shrink(1);
                AllSoundEvents.DEPOT_PLOP.playOnServer(level, pos);
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }
            }
            return ItemInteractionResult.SUCCESS;
        });
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos).map(unpackager -> {
            if (unpackager.animationTicks == 0 && unpackager.inventory.getStackInSlot(0).isEmpty()) {
                return 0;
            }

            return 15;
        }).orElse(0);
    }

    @Override
    public Class<GasUnpackagerBlockEntity> getBlockEntityClass() {
        return GasUnpackagerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GasUnpackagerBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.GAS_UNPACKAGER.get();
    }
}
