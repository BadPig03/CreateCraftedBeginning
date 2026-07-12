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
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasPackagerBlock extends PackagerBlock {
    public GasPackagerBlock(Properties properties) {
        super(properties);
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

        Direction preferred = null;
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        for (Direction direction : context.getNearestLookingDirections()) {
            BlockPos targetPos = clickedPos.relative(direction);
            BlockEntity be = level.getBlockEntity(targetPos);
            if (be instanceof GasPackagerBlockEntity) {
                continue;
            }

            Direction targetSide = direction.getOpposite();
            if (be == null || level.getCapability(GasHandler.BLOCK, targetPos, targetSide) == null) {
                continue;
            }

            preferred = direction.getOpposite();
            break;
        }

        Player player = context.getPlayer();
        if (preferred == null) {
            Direction direction = context.getNearestLookingDirection();
            preferred = player != null && player.isShiftKeyDown() ? direction : direction.getOpposite();
        }

        if (player != null && !(player instanceof FakePlayer)) {
            if (level.getBlockState(clickedPos.relative(preferred.getOpposite())).getBlock() instanceof PortableGasInterfaceBlock) {
                CCBLang.translate("gui.warnings.no_gas_portable_interface").sendStatus(player);
                return null;
            }
        }

        return state.setValue(POWERED, level.hasNeighborSignal(clickedPos)).setValue(FACING, preferred);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (AllItems.WRENCH.isIn(stack) || AllBlocks.FACTORY_GAUGE.isIn(stack) || AllBlocks.STOCK_LINK.isIn(stack) && !(state.hasProperty(LINKED) && state.getValue(LINKED)) || AllBlocks.PACKAGE_FROGPORT.isIn(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        onBlockEntityUseItemOn(level, pos, be -> {
            if (be.heldBox.isEmpty()) {
                if (be.animationTicks > 0) {
                    return ItemInteractionResult.SUCCESS;
                }

                if (BalloonUtils.isBalloon(stack)) {
                    if (level.isClientSide()) {
                        return ItemInteractionResult.SUCCESS;
                    }

                    if (!be.unwrapBox(stack.copy(), true)) {
                        return ItemInteractionResult.SUCCESS;
                    }

                    be.unwrapBox(stack.copy(), false);
                    stack.shrink(1);
                    AllSoundEvents.DEPOT_PLOP.playOnServer(level, pos);
                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, ItemStack.EMPTY);
                    }
                    return ItemInteractionResult.SUCCESS;
                }
                return ItemInteractionResult.SUCCESS;
            }

            if (be.animationTicks > 0) {
                return ItemInteractionResult.SUCCESS;
            }

            if (!level.isClientSide()) {
                player.getInventory().placeItemBackInInventory(be.heldBox.copy());
                AllSoundEvents.playItemPickup(player);
                be.heldBox = ItemStack.EMPTY;
                be.notifyUpdate();
            }
            return ItemInteractionResult.SUCCESS;
        });

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public BlockEntityType<? extends PackagerBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.GAS_PACKAGER.get();
    }
}
