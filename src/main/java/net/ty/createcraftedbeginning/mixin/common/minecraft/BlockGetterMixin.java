package net.ty.createcraftedbeginning.mixin.common.minecraft;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorRotationRay;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserBlockClip;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserCollisionContext;
import net.ty.createcraftedbeginning.mixin.common.accessor.ClipContextAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(BlockGetter.class)
public interface BlockGetterMixin {
    @WrapMethod(method = "lambda$clip$2")
    private @Nullable BlockHitResult ccb$lambda$clip$2(ClipContext context, BlockPos pos, Operation<BlockHitResult> original) {
        BlockGetter world = (BlockGetter) this;
        CollisionContext collision = ((ClipContextAccessor) context).ccb$getCollisionContext();
        if (collision instanceof AmethystMirrorRotationRay ray) {
            BlockHitResult unavailable = ray.stopAtUnloadedCell(world, context, pos);
            if (unavailable != null) {
                return unavailable;
            }
        }
        if (!(collision instanceof LaserCollisionContext)) {
            return original.call(context, pos);
        }

        return LaserBlockClip.clipCell(world, context, pos);
    }
}
