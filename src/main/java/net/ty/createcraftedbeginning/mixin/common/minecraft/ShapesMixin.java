package net.ty.createcraftedbeginning.mixin.common.minecraft;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorCollisionShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Shapes.class)
public class ShapesMixin {
    @Inject(method = "joinIsNotEmpty(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/BooleanOp;)Z", at = @At("HEAD"), cancellable = true)
    private static void ccb$joinIsNotEmpty(VoxelShape first, VoxelShape second, BooleanOp operator, CallbackInfoReturnable<Boolean> callback) {
        if (operator != BooleanOp.AND) {
            return;
        }

        if (first instanceof AmethystMirrorCollisionShape mirror) {
            callback.setReturnValue(mirror.intersects(second));
            return;
        }

        if (!(second instanceof AmethystMirrorCollisionShape mirror)) {
            return;
        }

        callback.setReturnValue(mirror.intersects(first));
    }
}
