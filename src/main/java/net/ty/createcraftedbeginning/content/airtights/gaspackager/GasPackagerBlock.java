package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceBlock;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasPackagerBlock extends PackagerBlock {
    public GasPackagerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    private static Direction findConnectedGasDirection(BlockPlaceContext context, Level level, BlockPos clickedPos) {
        for (Direction direction : context.getNearestLookingDirections()) {
            BlockPos targetPos = clickedPos.relative(direction);
            BlockEntity target = level.getBlockEntity(targetPos);
            if (target instanceof GasPackagerBlockEntity) {
                continue;
            }

            Direction targetSide = direction.getOpposite();
            if (target == null || level.getCapability(GasHandler.BLOCK, targetPos, targetSide) == null) {
                continue;
            }

            return targetSide;
        }
        return null;
    }

    private static void handleInteraction(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand, PackagerBlockEntity blockEntity) {
        if (blockEntity.animationTicks > 0) {
            return;
        }

        if (!blockEntity.heldBox.isEmpty()) {
            if (!level.isClientSide()) {
                player.getInventory().placeItemBackInInventory(blockEntity.heldBox.copy());
                AllSoundEvents.playItemPickup(player);
                blockEntity.heldBox = ItemStack.EMPTY;
                blockEntity.notifyUpdate();
            }
            return;
        }

        if (!BalloonUtils.isBalloon(stack) || level.isClientSide()) {
            return;
        }

        ItemStack inserted = stack.copyWithCount(1);
        if (!blockEntity.unwrapBox(inserted, false)) {
            return;
        }

        stack.shrink(1);
        AllSoundEvents.DEPOT_PLOP.playOnServer(level, pos);
        if (!stack.isEmpty()) {
            return;
        }

        player.setItemInHand(hand, ItemStack.EMPTY);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction preferred = findConnectedGasDirection(context, level, clickedPos);
        Player player = context.getPlayer();
        if (preferred == null) {
            Direction direction = context.getNearestLookingDirection();
            preferred = player != null && player.isShiftKeyDown() ? direction : direction.getOpposite();
        }

        BlockPos targetPos = clickedPos.relative(preferred.getOpposite());
        if (player != null && !(player instanceof FakePlayer) && level.getBlockState(targetPos).getBlock() instanceof PortableGasInterfaceBlock) {
            CCBLang.translate("gui.warnings.no_gas_portable_interface").sendStatus(player);
            return null;
        }
        return state.setValue(POWERED, level.hasNeighborSignal(clickedPos)).setValue(FACING, preferred);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (AllItems.WRENCH.isIn(stack) || AllBlocks.FACTORY_GAUGE.isIn(stack) || CCBBlocks.GAS_FACTORY_GAUGE_BLOCK.isIn(stack) || AllBlocks.STOCK_LINK.isIn(stack) && !(state.hasProperty(LINKED) && state.getValue(LINKED)) || AllBlocks.PACKAGE_FROGPORT.isIn(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        onBlockEntityUseItemOn(level, pos, blockEntity -> {
            handleInteraction(stack, level, pos, player, hand, blockEntity);
            return ItemInteractionResult.SUCCESS;
        });
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public BlockEntityType<? extends PackagerBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.GAS_PACKAGER.get();
    }
}
