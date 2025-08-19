package net.ty.createcraftedbeginning.content.airtighttank;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
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
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class AirtightTankItem extends BlockItem {
    public AirtightTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull InteractionResult place(@NotNull BlockPlaceContext ctx) {
        InteractionResult initialResult = super.place(ctx);
        if (!initialResult.consumesAction()) {
            return initialResult;
        }
        tryMultiPlace(ctx);
        return initialResult;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(@NotNull BlockPos blockPos, Level level, Player player, @NotNull ItemStack itemStack, @NotNull BlockState blockState) {
        MinecraftServer minecraftServer = level.getServer();
        if (minecraftServer == null) {
            return false;
        }
        CustomData blockEntityData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            CompoundTag nbt = blockEntityData.copyTag();
            nbt.remove("Size");
            nbt.remove("Height");
            nbt.remove("Controller");
            nbt.remove("LastKnownPos");
            itemStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(nbt));
        }
        return super.updateCustomBlockEntityTag(blockPos, level, player, itemStack, blockState);
    }

    private void tryMultiPlace(BlockPlaceContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || player.isShiftKeyDown()) {
            return;
        }
        Direction face = ctx.getClickedFace();
        if (!face.getAxis().isVertical()) {
            return;
        }
        ItemStack stack = ctx.getItemInHand();
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockPos placedOnPos = pos.relative(face.getOpposite());
        BlockState placedOnState = world.getBlockState(placedOnPos);

        if (placedOnState.getBlock() != getBlock()) {
            return;
        }
        AirtightTankBlockEntity tankAt = ConnectivityHandler.partAt(CCBBlockEntities.AIRTIGHT_TANK.get(), world, placedOnPos);
        if (tankAt == null) {
            return;
        }
        AirtightTankBlockEntity controllerBE = tankAt.getControllerBE();
        if (controllerBE == null) {
            return;
        }
        int width = controllerBE.width;
        if (width == 1) {
            return;
        }

        int tanksToPlace = 0;
        BlockPos startPos = face == Direction.DOWN ? controllerBE.getBlockPos().below() : controllerBE.getBlockPos().above(controllerBE.height);
        if (startPos.getY() != pos.getY()) {
            return;
        }
        for (int xOffset = 0; xOffset < width; xOffset++) {
            for (int zOffset = 0; zOffset < width; zOffset++) {
                BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
                BlockState blockState = world.getBlockState(offsetPos);
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
                BlockState blockState = world.getBlockState(offsetPos);
                if (blockState.getBlock() == getBlock()) {
                    continue;
                }
                BlockPlaceContext context = BlockPlaceContext.at(ctx, offsetPos, face);
                player.getPersistentData().putBoolean("SilenceTankSound", true);
                super.place(context);
                player.getPersistentData().remove("SilenceTankSound");
            }
        }
    }
}
