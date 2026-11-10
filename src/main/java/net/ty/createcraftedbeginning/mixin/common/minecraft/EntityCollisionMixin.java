package net.ty.createcraftedbeginning.mixin.common.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorCollisionShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Entity.class)
public class EntityCollisionMixin {
    @SuppressWarnings("MethodMayBeStatic")
    @WrapOperation(method = "collide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collectCandidateStepUpHeights(Lnet/minecraft/world/phys/AABB;Ljava/util/List;FF)[F"))
    private float[] ccb$collide(AABB box, List<VoxelShape> colliders, float maxStep, float previousY, Operation<float[]> original, @Local(argsOnly = true) Vec3 movement) {
        float[] heights = original.call(box, colliders, maxStep, previousY);
        FloatSet adjusted = null;
        for (VoxelShape collider : colliders) {
            if (!(collider instanceof AmethystMirrorCollisionShape mirror)) {
                continue;
            }

            AABB raised = box.move(movement.x, maxStep, movement.z);
            if (mirror.intersects(raised)) {
                continue;
            }

            double downward = mirror.collide(Axis.Y, raised, -maxStep);
            if (downward == -maxStep) {
                continue;
            }

            float height = Math.nextUp((float) (maxStep + downward));
            if (height <= 0 || height > maxStep || height == previousY) {
                continue;
            }

            if (adjusted == null) {
                adjusted = new FloatArraySet(heights);
            }
            adjusted.add(height);
        }
        if (adjusted == null) {
            return heights;
        }

        float[] result = adjusted.toFloatArray();
        FloatArrays.unstableSort(result);
        return result;
    }
}
