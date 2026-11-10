package net.ty.createcraftedbeginning.compat.sable;

import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import it.unimi.dsi.fastutil.Function;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.compat.sable.SableRenderBounds.Region;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public final class SableRenderRaycast {
    private static final ThreadLocal<Float> PARTIAL_TICK = new ThreadLocal<>();
    private static final Class<?> PROVIDER;
    private static final MethodHandle PUSH;
    private static final MethodHandle POP;

    static {
        try {
            PROVIDER = Class.forName("dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension");
            PUSH = MethodHandles.publicLookup().findVirtual(PROVIDER, "sable$pushPoseSupplier", MethodType.methodType(void.class, Function.class)).asType(MethodType.methodType(void.class, Object.class, Function.class));
            POP = MethodHandles.publicLookup().findVirtual(PROVIDER, "sable$popPoseSupplier", MethodType.methodType(void.class)).asType(MethodType.methodType(void.class, Object.class));
        }
        catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to resolve Sable render-pose raycast support.", exception);
        }
    }

    private SableRenderRaycast() {
    }

    public static BlockHitResult withPose(Object provider, float partialTick, Supplier<BlockHitResult> query) {
        if (!PROVIDER.isInstance(provider)) {
            return query.get();
        }

        Function<SubLevelAccess, Pose3dc> poses = value -> {
            SubLevelAccess subLevel = (SubLevelAccess) value;
            if (subLevel instanceof ClientSubLevelAccess client) {
                return client.renderPose(partialTick);
            }

            return subLevel.logicalPose();
        };
        try {
            PUSH.invokeExact(provider, poses);
            Float previous = PARTIAL_TICK.get();
            PARTIAL_TICK.set(partialTick);
            try {
                return query.get();
            }
            finally {
                if (previous == null) {
                    PARTIAL_TICK.remove();
                }
                else {
                    PARTIAL_TICK.set(previous);
                }
                POP.invokeExact(provider);
            }
        }
        catch (Throwable exception) {
            throw new IllegalStateException("Failed to trace a laser using the Sable render pose.", exception);
        }
    }

    public static Iterable<? extends SubLevelAccess> candidates(Level level, BoundingBox3dc bounds, Supplier<Iterable<? extends SubLevelAccess>> original) {
        Float partial = PARTIAL_TICK.get();
        if (!level.isClientSide || partial == null) {
            return original.get();
        }

        return SableRenderBounds.query(level, List.of(new BoundingBox3d(bounds).toMojang()), partial).stream().map(Region::owner).toList();
    }

    static BlockHitResult clip(Level level, ClipContext context, float partialTick) {
        if (!level.isClientSide) {
            return level.clip(context);
        }

        return withPose(level, partialTick, () -> level.clip(context));
    }
}
