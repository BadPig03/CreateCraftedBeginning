package net.ty.createcraftedbeginning.mixin.compat.sable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.ty.createcraftedbeginning.compat.sable.SableRenderRaycast;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorRotationRay;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserBlockClip;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserCollisionContext;
import net.ty.createcraftedbeginning.mixin.common.accessor.ClipContextAccessor;
import net.ty.createcraftedbeginning.platform.SkyLightCollisionContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BiFunction;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = BlockGetter.class, priority = 1200)
public interface BlockGetterMixin {
    @SuppressWarnings("ConstantValue")
    @WrapMethod(method = "originalClip", remap = false)
    private static BlockHitResult ccb$originalClip$specialContexts(BlockGetter level, ClipContext context, Operation<BlockHitResult> original) {
        CollisionContext collision = ((ClipContextAccessor) context).ccb$getCollisionContext();
        if (collision instanceof SkyLightCollisionContext skyLight) {
            return skyLight.clip(level, context);
        }

        if (!(collision instanceof LaserCollisionContext)) {
            return original.call(level, context);
        }

        return BlockGetter.traverseBlocks(context.getFrom(), context.getTo(), context, (clip, pos) -> LaserBlockClip.clipCell(level, clip, pos), clip -> {
            Vec3 end = clip.getTo();
            Vec3 delta = clip.getFrom().subtract(end);
            return BlockHitResult.miss(end, Direction.getNearest(delta.x, delta.y, delta.z), BlockPos.containing(end));
        });
    }

    @WrapOperation(method = "originalClip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockGetter;traverseBlocks(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Ljava/lang/Object;Ljava/util/function/BiFunction;Ljava/util/function/Function;)Ljava/lang/Object;", remap = false), remap = false)
    private static Object ccb$originalClip$traverseBlocks(Vec3 start, Vec3 end, Object context, BiFunction<ClipContext, BlockPos, @Nullable BlockHitResult> tester, Function<ClipContext, BlockHitResult> miss, Operation<Object> original, @Local(argsOnly = true) BlockGetter world) {
        ClipContext clip = (ClipContext) context;
        if (!(((ClipContextAccessor) clip).ccb$getCollisionContext() instanceof AmethystMirrorRotationRay ray)) {
            return original.call(start, end, context, tester, miss);
        }

        BiFunction<ClipContext, BlockPos, @Nullable BlockHitResult> guarded = (active, pos) -> {
            BlockHitResult unavailable = ray.stopAtUnloadedCell(world, active, pos);
            if (unavailable != null) {
                return unavailable;
            }

            return tester.apply(active, pos);
        };
        return original.call(start, end, context, guarded, miss);
    }

    @SuppressWarnings("MethodMayBeStatic")
    @WrapOperation(method = "clip", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/ActiveSableCompanion;getAllIntersecting(Lnet/minecraft/world/level/Level;Ldev/ryanhcode/sable/companion/math/BoundingBox3dc;)Ljava/lang/Iterable;", remap = false))
    private Iterable<? extends SubLevelAccess> ccb$clip(@Coerce Object companion, Level level, BoundingBox3dc bounds, Operation<Iterable<? extends SubLevelAccess>> original, @Local(argsOnly = true) ClipContext context) {
        if (!(((ClipContextAccessor) context).ccb$getCollisionContext() instanceof LaserCollisionContext)) {
            return original.call(companion, level, bounds);
        }

        BoundingBox3dc expanded = new BoundingBox3d(bounds).expand(LaserBlockClip.NEIGHBOR_REACH);
        return SableRenderRaycast.candidates(level, expanded, () -> original.call(companion, level, expanded));
    }
}
