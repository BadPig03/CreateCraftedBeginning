package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.OpticalPlatePlacement;
import org.joml.Quaternionf;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystMirrorBlockItem extends BlockItem {
    public AmethystMirrorBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (!super.placeBlock(context, state)) {
            return false;
        }

        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof AmethystMirrorBlockEntity mirror)) {
            return true;
        }

        Direction normal = OpticalPlatePlacement.getNormal(context);
        Quaternionf rotation = AmethystMirrorGeometry.faceDirection(mirror.getStoredRotation(), Vec3.atLowerCornerOf(normal.getNormal()));
        mirror.applyAim(rotation);
        return true;
    }
}
