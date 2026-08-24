package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
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
public class AirtightReactorKettleBlockItem extends BlockItem {
    public AirtightReactorKettleBlockItem(Block block, Properties properties) {
        super(block, properties.rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult placementResult = super.place(context);
        if (placementResult != InteractionResult.FAIL || !(getBlock() instanceof AirtightReactorKettleBlock)) {
            return placementResult;
        }

        Direction clickedFace = context.getClickedFace();
        BlockPos adjacentPos = context.getClickedPos().relative(clickedFace);
        placementResult = super.place(BlockPlaceContext.at(context, adjacentPos, clickedFace));
        if (placementResult != InteractionResult.FAIL || !context.getLevel().isClientSide()) {
            return placementResult;
        }

        CCBClientBridge.showAirtightReactorKettlePlacementBounds(context);
        return placementResult;
    }

}
