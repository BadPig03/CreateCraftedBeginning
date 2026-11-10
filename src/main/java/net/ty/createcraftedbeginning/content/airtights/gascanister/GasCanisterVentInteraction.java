package net.ty.createcraftedbeginning.content.airtights.gascanister;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags.Items;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasCanisterVentInteraction {
    static final double VALVE_HEIGHT = 0.8125;
    static final double VALVE_WIDTH = 0.25;
    private static final double HIT_TOLERANCE = 1.0E-4;

    private GasCanisterVentInteraction() {
    }

    static ItemInteractionResult use(Level level, BlockPos pos, Player player, ItemStack heldItem, BlockHitResult hitResult) {
        if (!canUse(player) || !heldItem.is(Items.TOOLS_WRENCH) || !isValveHit(hitResult)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!(level.getBlockEntity(pos) instanceof GasCanisterBlockEntity canister)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        canister.releaseGas();
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    static boolean canUse(Player player) {
        return player.mayBuild() && !player.isSpectator() && !player.isShiftKeyDown();
    }

    static boolean isValveHit(BlockHitResult hitResult) {
        if (hitResult.getDirection() != Direction.UP) {
            return false;
        }

        Vec3 localHit = hitResult.getLocation().subtract(Vec3.atLowerCornerOf(hitResult.getBlockPos()));
        double halfWidth = VALVE_WIDTH / 2;
        return Math.abs(localHit.y - VALVE_HEIGHT) <= HIT_TOLERANCE && Math.abs(localHit.x - 0.5) <= halfWidth && Math.abs(localHit.z - 0.5) <= halfWidth;
    }
}
