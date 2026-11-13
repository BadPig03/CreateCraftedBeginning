package net.ty.createcraftedbeginning.content.opticalpower;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpticalPlatePlacement {
    private OpticalPlatePlacement() {
    }

    public static Direction getNormal(BlockPlaceContext context) {
        Direction normal = context.getHorizontalDirection().getOpposite();
        if (normal.getAxis() != context.getClickedFace().getAxis()) {
            return normal;
        }

        Player player = context.getPlayer();
        if (player != null && player.getXRot() < 0) {
            return Direction.DOWN;
        }

        return Direction.UP;
    }
}
