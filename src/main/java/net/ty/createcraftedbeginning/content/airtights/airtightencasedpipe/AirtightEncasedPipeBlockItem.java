package net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightEncasedPipeBlockItem extends BlockItem {
    public AirtightEncasedPipeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState replacedState = level.getBlockState(pos);
        InteractionResult placementResult = super.place(context);
        if (!placementResult.consumesAction()) {
            return placementResult;
        }

        if (level.isClientSide || !(replacedState.getBlock() instanceof AirtightPipeBlock)) {
            return placementResult;
        }

        Player player = context.getPlayer();
        ItemStack pipeStack = new ItemStack(replacedState.getBlock().asItem());
        if (player == null) {
            Block.popResource(level, pos, pipeStack);
            return placementResult;
        }

        if (player.isCreative()) {
            return placementResult;
        }

        ItemHandlerHelper.giveItemToPlayer(player, pipeStack);
        return placementResult;
    }
}
