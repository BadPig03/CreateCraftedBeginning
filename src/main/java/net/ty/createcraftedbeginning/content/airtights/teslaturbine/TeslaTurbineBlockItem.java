package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.platform.CCBClientBridge;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineBlockItem extends BlockItem {
    public TeslaTurbineBlockItem(Block block, Properties properties) {
        super(block, properties.rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (result != InteractionResult.FAIL || !(getBlock() instanceof TeslaTurbineBlock turbine)) {
            return result;
        }

        Axis axis = turbine.getAxisForPlacement(context);
        Direction direction = context.getClickedFace();
        if (direction.getAxis() != axis) {
            BlockPlaceContext offsetContext = BlockPlaceContext.at(context, context.getClickedPos().relative(direction), direction);
            result = super.place(offsetContext);
        }
        if (result != InteractionResult.FAIL || !context.getLevel().isClientSide()) {
            return result;
        }

        CCBClientBridge.showTeslaTurbinePlacementBounds(context);
        return result;
    }

}
