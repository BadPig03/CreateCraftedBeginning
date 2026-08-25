package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.platform.client.ClientRenderBridge;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressBlockItem extends BlockItem {
    public AirtightForgingPressBlockItem(Block block, Properties properties) {
        super(block, properties.rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult placementResult = super.place(context);
        if (placementResult != InteractionResult.FAIL || !(getBlock() instanceof AirtightForgingPressBlock)) {
            return placementResult;
        }

        Direction clickedFace = context.getClickedFace();
        placementResult = super.place(BlockPlaceContext.at(context, context.getClickedPos().relative(clickedFace), clickedFace));
        if (placementResult != InteractionResult.FAIL || !context.getLevel().isClientSide()) {
            return placementResult;
        }

        BlockPos placementPos = context.getClickedPos().relative(clickedFace);
        ClientRenderBridge.showPlacementBounds(context, "airtight_forging_press", placementPos, new AABB(placementPos).inflate(1));
        return placementResult;
    }

}
