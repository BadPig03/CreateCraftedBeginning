package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasConnectivityHandler;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightTankItem extends BlockItem {
    private static final String COMPOUND_KEY_CORE = "Core";
    private static final String COMPOUND_KEY_WIDTH = "Width";
    private static final String COMPOUND_KEY_HEIGHT = "Height";
    private static final String COMPOUND_KEY_LAST_KNOWN_POS = "LastKnownPos";
    private static final String COMPOUND_KEY_CONTROLLER_POS = "Controller";
    private static final String COMPOUND_KEY_TANK_CONTENT = "TankContent";
    private static final int INVALID_PLACEMENT = -1;

    public AirtightTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult placementResult = placeSingleBlock(context);
        if (!placementResult.consumesAction()) {
            return placementResult;
        }

        tryMultiPlace(context);
        return placementResult;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos blockPos, Level level, @Nullable Player player, ItemStack itemStack, BlockState blockState) {
        if (level.getServer() == null) {
            return false;
        }

        CustomData blockEntityData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData == null) {
            return super.updateCustomBlockEntityTag(blockPos, level, player, itemStack, blockState);
        }

        CompoundTag blockEntityTag = blockEntityData.copyTag();
        CCBNbtUtils.remove(blockEntityTag, COMPOUND_KEY_WIDTH);
        CCBNbtUtils.remove(blockEntityTag, COMPOUND_KEY_HEIGHT);
        CCBNbtUtils.remove(blockEntityTag, COMPOUND_KEY_CONTROLLER_POS);
        CCBNbtUtils.remove(blockEntityTag, COMPOUND_KEY_LAST_KNOWN_POS);
        CCBNbtUtils.remove(blockEntityTag, COMPOUND_KEY_CORE);
        CCBNbtUtils.remove(blockEntityTag, COMPOUND_KEY_TANK_CONTENT);
        itemStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
        return super.updateCustomBlockEntityTag(blockPos, level, player, itemStack, blockState);
    }

    InteractionResult placeSingleBlock(BlockPlaceContext context) {
        return super.place(context);
    }

    void tryMultiPlace(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) {
            return;
        }

        Direction clickedFace = context.getClickedFace();
        if (!clickedFace.getAxis().isVertical()) {
            return;
        }

        ItemStack tankStack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos placementPos = context.getClickedPos();
        BlockPos placedOnPos = placementPos.relative(clickedFace.getOpposite());
        if (level.getBlockState(placedOnPos).getBlock() != getBlock()) {
            return;
        }

        AirtightTankBlockEntity placedOnTank = GasConnectivityHandler.partAt(CCBBlockEntities.AIRTIGHT_TANK.get(), level, placedOnPos);
        if (placedOnTank == null) {
            return;
        }

        AirtightTankBlockEntity controller = placedOnTank.getControllerBE();
        if (controller == null) {
            return;
        }

        int width = controller.getWidth();
        if (width == 1 || controller.getHeight() >= controller.getMaxLength(controller.getMainConnectionAxis(), width)) {
            return;
        }

        BlockPos controllerPos = controller.getBlockPos();
        BlockPos layerStartPos = clickedFace == Direction.DOWN ? controllerPos.below() : controllerPos.above(controller.getHeight());
        if (layerStartPos.getY() != placementPos.getY()) {
            return;
        }

        int tanksToPlace = countTanksToPlace(level, layerStartPos, width);
        if (tanksToPlace == INVALID_PLACEMENT || !player.isCreative() && tankStack.getCount() < tanksToPlace) {
            return;
        }

        placeTankLayer(context, level, layerStartPos, clickedFace, width);
    }

    private int countTanksToPlace(Level level, BlockPos startPos, int width) {
        int tanksToPlace = 0;
        for (int xOffset = 0; xOffset < width; xOffset++) {
            for (int zOffset = 0; zOffset < width; zOffset++) {
                BlockPos targetPos = startPos.offset(xOffset, 0, zOffset);
                BlockState targetState = level.getBlockState(targetPos);
                if (targetState.getBlock() == getBlock()) {
                    continue;
                }

                if (!targetState.canBeReplaced()) {
                    return INVALID_PLACEMENT;
                }

                tanksToPlace++;
            }
        }
        return tanksToPlace;
    }

    private void placeTankLayer(BlockPlaceContext context, Level level, BlockPos startPos, Direction face, int width) {
        for (int xOffset = 0; xOffset < width; xOffset++) {
            for (int zOffset = 0; zOffset < width; zOffset++) {
                BlockPos targetPos = startPos.offset(xOffset, 0, zOffset);
                BlockState targetState = level.getBlockState(targetPos);
                if (targetState.getBlock() == getBlock()) {
                    continue;
                }

                super.place(BlockPlaceContext.at(context, targetPos, face));
            }
        }
    }
}
