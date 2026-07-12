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
import net.ty.createcraftedbeginning.registry.CCBBlocks;

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
        BlockState state = level.getBlockState(pos);
        InteractionResult result = super.place(context);
        if (!result.consumesAction()) {
            return result;
        }

        if (level.isClientSide || !(state.getBlock() instanceof AirtightPipeBlock)) {
            return result;
        }

        Player player = context.getPlayer();
        ItemStack stack = new ItemStack(CCBBlocks.AIRTIGHT_PIPE_BLOCK.asItem());
        if (player == null) {
            Block.popResource(level, pos, stack);
            return result;
        }

        if (player.isCreative()) {
            return result;
        }

        ItemHandlerHelper.giveItemToPlayer(player, stack);
        return result;
    }
}
