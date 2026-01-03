package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
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
import org.jetbrains.annotations.NotNull;

public class CreativeAirtightTankItem extends BlockItem {
    private static final String COMPOUND_KEY_WIDTH = "Width";
    private static final String COMPOUND_KEY_HEIGHT = "Height";
    private static final String COMPOUND_KEY_LAST_KNOWN_POS = "LastKnownPos";
    private static final String COMPOUND_KEY_CONTROLLER_POS = "ControllerPos";

    public CreativeAirtightTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull InteractionResult place(@NotNull BlockPlaceContext context) {
        InteractionResult initialResult = super.place(context);
        if (!initialResult.consumesAction()) {
            return initialResult;
        }

        tryMultiPlace(context);
        return initialResult;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(@NotNull BlockPos blockPos, @NotNull Level level, Player player, @NotNull ItemStack itemStack, @NotNull BlockState blockState) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return false;
        }

        CustomData blockEntityData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            CompoundTag compoundTag = blockEntityData.copyTag();
            compoundTag.remove(COMPOUND_KEY_WIDTH);
            compoundTag.remove(COMPOUND_KEY_HEIGHT);
            compoundTag.remove(COMPOUND_KEY_CONTROLLER_POS);
            compoundTag.remove(COMPOUND_KEY_LAST_KNOWN_POS);
            itemStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(compoundTag));
        }
        return super.updateCustomBlockEntityTag(blockPos, level, player, itemStack, blockState);
    }

    private void tryMultiPlace(@NotNull BlockPlaceContext context) {
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

        CreativeAirtightTankBlockEntity tankAt = GasConnectivityHandler.partAt(CCBBlockEntities.CREATIVE_AIRTIGHT_TANK.get(), level, placedOnPos);
        if (tankAt == null) {
            return;
        }

        CreativeAirtightTankBlockEntity controller = tankAt.getControllerBE();
        if (controller == null) {
            return;
        }

        int width = controller.width;
        if (width == 1) {
            return;
        }

        int tanksToPlace = 0;
        BlockPos startPos = face == Direction.DOWN ? controller.getBlockPos().below() : controller.getBlockPos().above(controller.height);
        if (startPos.getY() != pos.getY()) {
            return;
        }

        for (int xOffset = 0; xOffset < width; xOffset++) {
            for (int zOffset = 0; zOffset < width; zOffset++) {
                BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
                BlockState blockState = level.getBlockState(offsetPos);
                if (blockState.getBlock() == getBlock()) {
                    continue;
                }

                if (!blockState.canBeReplaced()) {
                    return;
                }

                tanksToPlace++;
            }
        }

        if (!player.isCreative() && stack.getCount() < tanksToPlace) {
            return;
        }

        for (int xOffset = 0; xOffset < width; xOffset++) {
            for (int zOffset = 0; zOffset < width; zOffset++) {
                BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
                BlockState blockState = level.getBlockState(offsetPos);
                if (blockState.getBlock() == getBlock()) {
                    continue;
                }

                super.place(BlockPlaceContext.at(context, offsetPos, face));
            }
        }
    }
}