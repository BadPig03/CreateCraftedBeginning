package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
enum AmethystSoundSuppressorPort implements OpticalGeometry {
    INSTANCE;

    private static final double HIT_TOLERANCE = 1.0E-5;
    private static final VoxelShape CRYSTAL = Shapes.or(Block.box(6, 9, 6, 10, 11, 10), Block.box(5, 11, 5, 11, 16, 11), Block.box(5, 16, 5, 11, 17, 11), Block.box(6, 17, 6, 10, 18, 10));
    private static final VoxelShape LASER_SHAPE = Shapes.or(CCBShapes.AMETHYST_SOUND_SUPPRESSOR_COLLISION_SHAPE, CRYSTAL);
    private static final List<AABB> HIT_BOUNDS = CRYSTAL.toAabbs().stream().map(bounds -> bounds.inflate(HIT_TOLERANCE)).toList();

    static boolean isHit(BlockHitResult hit) {
        Vec3 localHit = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()));
        if (localHit.y <= 9.0 / 16 + HIT_TOLERANCE) {
            return false;
        }

        for (AABB bounds : HIT_BOUNDS) {
            if (bounds.contains(localHit)) {
                return true;
            }
        }
        return false;
    }

    static VoxelShape getReceivingShape() {
        return CRYSTAL;
    }

    @Override
    public VoxelShape getCollisionShape() {
        return LASER_SHAPE;
    }

    @Override
    public List<Double> displayKey() {
        return List.of();
    }

    @Override
    public OpticalSurface getSurface(BlockHitResult hit, Vec3 localDirection) {
        return OpticalSurface.opaque(hit);
    }

    @Override
    public boolean hasOpaqueBoxSurfaces() {
        return true;
    }
}
