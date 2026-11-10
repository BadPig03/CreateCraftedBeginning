package net.ty.createcraftedbeginning.mixin.compat.sable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.compat.sable.SableOrientedBoxAccess;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorCollisionShape;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorSectionIndex;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision", remap = false)
public abstract class SubLevelEntityCollisionMixin {
    @WrapOperation(method = "collide", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;betweenClosed(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Ljava/lang/Iterable;"))
    private static Iterable<BlockPos> ccb$collide$betweenClosed(BlockPos min, BlockPos max, Operation<Iterable<BlockPos>> original, @Local(argsOnly = true) Entity entity) {
        if (!AmethystMirrorSectionIndex.requiresExpansion(entity.level(), min, max)) {
            return original.call(min, max);
        }

        return original.call(min.offset(-1, 0, -1), max.offset(1, 1, 1));
    }

    @WrapOperation(method = {"collide", "hasCollision"}, at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/api/math/OrientedBoundingBox3d;sat(Ldev/ryanhcode/sable/api/math/OrientedBoundingBox3d;Ldev/ryanhcode/sable/api/math/OrientedBoundingBox3d;Lorg/joml/Vector3d;)Lorg/joml/Vector3d;"))
    private static Vector3d ccb$collide$hasCollision$sat(@Coerce Object entity, @Coerce Object block, Vector3d result, Operation<Vector3d> original, @Local(ordinal = 0) VoxelShape shape) {
        if (!(shape instanceof AmethystMirrorCollisionShape mirror)) {
            return original.call(entity, block, result);
        }

        SableOrientedBoxAccess box = (SableOrientedBoxAccess) block;
        Quaterniond orientation = box.ccb$getOrientation();
        Vector3d dimensions = box.ccb$getDimensions();
        double rotationX = orientation.x;
        double rotationY = orientation.y;
        double rotationZ = orientation.z;
        double rotationW = orientation.w;
        double sizeX = dimensions.x;
        double sizeY = dimensions.y;
        double sizeZ = dimensions.z;
        mirror.applyRotation(orientation);
        dimensions.set(AmethystMirrorGeometry.FRAME_HALF_SIZE * 2, AmethystMirrorGeometry.FRAME_HALF_SIZE * 2, AmethystMirrorGeometry.HALF_THICKNESS * 2);
        try {
            return original.call(entity, block, result);
        }
        finally {
            orientation.set(rotationX, rotationY, rotationZ, rotationW);
            dimensions.set(sizeX, sizeY, sizeZ);
        }
    }
}
