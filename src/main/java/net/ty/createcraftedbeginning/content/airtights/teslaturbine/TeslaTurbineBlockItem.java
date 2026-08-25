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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.platform.client.ClientRenderBridge;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineBlockItem extends BlockItem {
    public TeslaTurbineBlockItem(Block block, Properties properties) {
        super(block, properties.rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult placementResult = super.place(context);
        if (placementResult != InteractionResult.FAIL || !(getBlock() instanceof TeslaTurbineBlock turbineBlock)) {
            return placementResult;
        }

        Axis turbineAxis = turbineBlock.getAxisForPlacement(context);
        Direction clickedFace = context.getClickedFace();
        if (clickedFace.getAxis() != turbineAxis) {
            BlockPlaceContext offsetContext = BlockPlaceContext.at(context, context.getClickedPos().relative(clickedFace), clickedFace);
            placementResult = super.place(offsetContext);
        }
        if (placementResult != InteractionResult.FAIL || !context.getLevel().isClientSide()) {
            return placementResult;
        }

        if (turbineAxis != null) {
            BlockPos placementPos = context.getClickedPos();
            Vec3 axisDeflation = Vec3.atLowerCornerOf(Direction.get(AxisDirection.POSITIVE, turbineAxis).getNormal());
            AABB bounds = new AABB(placementPos).inflate(1).deflate(axisDeflation.x, axisDeflation.y, axisDeflation.z);
            ClientRenderBridge.showPlacementBounds(context, "tesla_turbine", placementPos, bounds);
        }
        return placementResult;
    }

}
