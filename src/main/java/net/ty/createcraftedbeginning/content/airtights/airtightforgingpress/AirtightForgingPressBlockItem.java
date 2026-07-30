package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.platform.CCBClientBridge;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressBlockItem extends BlockItem {
    public AirtightForgingPressBlockItem(Block block, Properties properties) {
        super(block, properties.rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (result != InteractionResult.FAIL || !(getBlock() instanceof AirtightForgingPressBlock)) {
            return result;
        }

        Direction direction = context.getClickedFace();
        result = super.place(BlockPlaceContext.at(context, context.getClickedPos().relative(direction), direction));
        if (result == InteractionResult.FAIL && context.getLevel().isClientSide()) {
            CCBClientBridge.showAirtightForgingPressPlacementBounds(context);
        }
        return result;
    }

}
