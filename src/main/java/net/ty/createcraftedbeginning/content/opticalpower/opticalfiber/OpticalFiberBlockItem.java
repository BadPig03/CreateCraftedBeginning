package net.ty.createcraftedbeginning.content.opticalpower.opticalfiber;

import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.content.opticalpower.opticalfiber.OpticalFiberPlacementHelper.Placement;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpticalFiberBlockItem extends BlockItem {
    private static final OpticalFiberPlacementHelper PLACEMENT_HELPER = new OpticalFiberPlacementHelper();

    static {
        PlacementHelpers.register(PLACEMENT_HELPER);
    }

    public OpticalFiberBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack heldItem = context.getItemInHand();
        if (!PLACEMENT_HELPER.canAssist(player, level, pos, heldItem)) {
            return super.useOn(context);
        }

        BlockState state = level.getBlockState(pos);
        BlockHitResult ray = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), pos, context.isInside());
        Placement placement = PLACEMENT_HELPER.planPlacement(player, level, state, pos, ray, context.getHand(), heldItem);
        if (placement == null) {
            return super.useOn(context);
        }

        return place(placement.context());
    }
}
