package net.ty.createcraftedbeginning.content.opticalpower.amethystloader;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management.AmethystLoaderMenu;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystLoaderBlock extends Block implements IBE<AmethystLoaderBlockEntity>, IWrenchable {
    public AmethystLoaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AmethystLoaderBlockEntity loader) || AmethystLoaderNetwork.antenna(loader) == null) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            AmethystLoaderMenu.open(loader, serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            AmethystLoaderTickets.setForced(serverLevel, pos, false);
            LoadingNetworkSavedData.get(serverLevel).removeLoader(GlobalPos.of(level.dimension(), pos));
        }
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            withBlockEntityDo(level, context.getClickedPos(), AmethystLoaderBlockEntity::toggleShowOutline);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Class<AmethystLoaderBlockEntity> getBlockEntityClass() {
        return AmethystLoaderBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AmethystLoaderBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AMETHYST_LOADER.get();
    }
}
