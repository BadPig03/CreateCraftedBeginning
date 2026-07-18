package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

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
import net.ty.createcraftedbeginning.api.gas.gases.GasConnectivityHandler;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeAirtightTankItem extends BlockItem {
    private static final String COMPOUND_KEY_WIDTH = "Width";
    private static final String COMPOUND_KEY_HEIGHT = "Height";
    private static final String COMPOUND_KEY_LAST_KNOWN_POS = "LastKnownPos";
    private static final String COMPOUND_KEY_CONTROLLER_POS = "Controller";
    private static final int INVALID_PLACEMENT = -1;

    public CreativeAirtightTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (!result.consumesAction()) {
            return result;
        }

        tryMultiPlace(context);
        return result;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos blockPos, Level level, @Nullable Player player, ItemStack itemStack, BlockState blockState) {
        if (level.getServer() == null) {
            return false;
        }

        CustomData data = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data != null) {
            CompoundTag tag = data.copyTag();
            tag.remove(COMPOUND_KEY_WIDTH);
            tag.remove(COMPOUND_KEY_HEIGHT);
            tag.remove(COMPOUND_KEY_CONTROLLER_POS);
            tag.remove(COMPOUND_KEY_LAST_KNOWN_POS);
            itemStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        }
        return super.updateCustomBlockEntityTag(blockPos, level, player, itemStack, blockState);
    }

    private void tryMultiPlace(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) {
            return;
        }

        Direction face = context.getClickedFace();
        if (!face.getAxis().isVertical()) {
            return;
        }

        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockPos placedOnPos = pos.relative(face.getOpposite());
        BlockState placedOnState = level.getBlockState(placedOnPos);
        if (placedOnState.getBlock() != getBlock()) {
            return;
        }

        CreativeAirtightTankBlockEntity tank = GasConnectivityHandler.partAt(CCBBlockEntities.CREATIVE_AIRTIGHT_TANK.get(), level, placedOnPos);
        if (tank == null) {
            return;
        }

        CreativeAirtightTankBlockEntity controller = tank.getControllerBE();
        if (controller == null) {
            return;
        }

        int width = controller.width;
        if (width == 1) {
            return;
        }

        BlockPos controllerPos = controller.getBlockPos();
        BlockPos startPos = face == Direction.DOWN ? controllerPos.below() : controllerPos.above(controller.height);
        if (startPos.getY() != pos.getY()) {
            return;
        }

        int tanksToPlace = countTanksToPlace(level, startPos, width);
        if (tanksToPlace == INVALID_PLACEMENT || !player.isCreative() && stack.getCount() < tanksToPlace) {
            return;
        }

        placeTankLayer(context, level, startPos, face, width);
    }

    private int countTanksToPlace(Level level, BlockPos startPos, int width) {
        int count = 0;
        for (int xOffset = 0; xOffset < width; xOffset++) {
            for (int zOffset = 0; zOffset < width; zOffset++) {
                BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
                BlockState state = level.getBlockState(offsetPos);
                if (state.getBlock() == getBlock()) {
                    continue;
                }
                if (!state.canBeReplaced()) {
                    return INVALID_PLACEMENT;
                }

                count++;
            }
        }
        return count;
    }

    private void placeTankLayer(BlockPlaceContext context, Level level, BlockPos startPos, Direction face, int width) {
        for (int xOffset = 0; xOffset < width; xOffset++) {
            for (int zOffset = 0; zOffset < width; zOffset++) {
                BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
                BlockState state = level.getBlockState(offsetPos);
                if (state.getBlock() == getBlock()) {
                    continue;
                }

                super.place(BlockPlaceContext.at(context, offsetPos, face));
            }
        }
    }
}