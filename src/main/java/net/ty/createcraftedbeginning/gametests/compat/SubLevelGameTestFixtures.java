package net.ty.createcraftedbeginning.gametests.compat;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3d;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class SubLevelGameTestFixtures {
    private SubLevelGameTestFixtures() {
    }

    static Fixture assemble(ServerLevel level, BlockPos origin, Set<BlockPos> parts) {
        Set<BlockPos> assembledParts = new HashSet<>(parts);
        BlockPos anchor = origin.north();
        if (!assembledParts.contains(anchor)) {
            level.setBlockAndUpdate(anchor, Blocks.STONE.defaultBlockState());
            assembledParts.add(anchor);
        }
        try {
            BoundingBox3i bounds = BoundingBox3i.from(assembledParts);
            if (bounds == null) {
                throw new NullPointerException("Expected assembly bounds at " + origin + '.');
            }

            Class<?> mover = Class.forName("dev.ryanhcode.sable.api.SubLevelAssemblyHelper");
            Object assembled = mover.getMethod("assembleBlocks", ServerLevel.class, BlockPos.class, Iterable.class, BoundingBox3ic.class).invoke(null, level, origin, assembledParts, bounds);
            if (assembled == null) {
                throw new NullPointerException("Expected an assembled physical fixture at " + origin + '.');
            }

            Object plot = assembled.getClass().getMethod("getPlot").invoke(assembled);
            if (plot == null) {
                throw new NullPointerException("Expected a physical fixture plot at " + origin + '.');
            }

            BlockPos center = (BlockPos) plot.getClass().getMethod("getCenterBlock").invoke(plot);
            if (center == null) {
                throw new NullPointerException("Expected a physical fixture center at " + origin + '.');
            }

            Object container = Class.forName("dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder").getMethod("sable$getPlotContainer").invoke(level);
            if (container == null) {
                throw new NullPointerException("Expected a sub-level container for the physical fixture at " + origin + '.');
            }

            Object physics = container.getClass().getMethod("physicsSystem").invoke(container);
            if (physics == null) {
                throw new NullPointerException("Expected a physics system for the physical fixture at " + origin + '.');
            }

            Object pipeline = physics.getClass().getMethod("getPipeline").invoke(physics);
            if (pipeline == null) {
                throw new NullPointerException("Expected a physics pipeline for the physical fixture at " + origin + '.');
            }

            Fixture fixture = new Fixture((SubLevelAccess) assembled, center, pipeline);
            move(fixture, Vec3.atCenterOf(origin), new Quaterniond());
            return fixture;
        }
        catch (ReflectiveOperationException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation ? invocation.getCause() : exception;
            throw new IllegalStateException("Failed to assemble a physical fixture at " + origin + '.', cause);
        }
    }

    static void move(Fixture fixture, Vec3 target, Quaterniond orientation) {
        SubLevelAccess subLevel = fixture.subLevel();
        Pose3d pose = new Pose3d(subLevel.logicalPose());
        pose.orientation().set(orientation);
        Vec3 current = pose.transformPosition(Vec3.atCenterOf(fixture.center()));
        Vec3 delta = target.subtract(current);
        pose.position().add(delta.x, delta.y, delta.z);
        try {
            Class<?> pipeline = Class.forName("dev.ryanhcode.sable.api.physics.PhysicsPipeline");
            Class<?> body = Class.forName("dev.ryanhcode.sable.api.physics.PhysicsPipelineBody");
            pipeline.getMethod("teleport", body, Vector3dc.class, Quaterniondc.class).invoke(fixture.pipeline(), subLevel, pose.position(), pose.orientation());
            subLevel.getClass().getMethod("updateBoundingBox").invoke(subLevel);
        }
        catch (ReflectiveOperationException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation ? invocation.getCause() : exception;
            throw new IllegalStateException("Failed to update physical fixture bounds at " + target + '.', cause);
        }
        Vec3 actual = subLevel.logicalPose().transformPosition(Vec3.atCenterOf(fixture.center()));
        if (actual.distanceToSqr(target) > 1.0E-8) {
            throw new IllegalStateException("Physical fixture center mismatch: expected " + target + ", found " + actual + '.');
        }
    }

    static void clear(ServerLevel level, Fixture fixture) {
        if (SableCompanion.INSTANCE.getContaining(level, fixture.center()) != fixture.subLevel()) {
            return;
        }

        Object container;
        Object plot;
        Object removed;
        MethodHandle remove;
        try {
            Class<?> containers = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            Class<?> subLevel = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
            Class<?> reason = Class.forName("dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason");
            container = Class.forName("dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder").getMethod("sable$getPlotContainer").invoke(level);
            plot = fixture.subLevel().getClass().getMethod("getPlot").invoke(fixture.subLevel());
            removed = reason.getField("REMOVED").get(null);
            remove = MethodHandles.publicLookup().findVirtual(containers, "removeSubLevel", MethodType.methodType(void.class, subLevel, reason));
        }
        catch (ReflectiveOperationException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation ? invocation.getCause() : exception;
            throw new IllegalStateException("Failed to prepare physical fixture removal at " + fixture.center() + '.', cause);
        }
        if (container == null) {
            throw new NullPointerException("Expected the physical fixture sub-level container at " + fixture.center() + '.');
        }

        if (removed == null) {
            throw new NullPointerException("Expected the physical fixture removal reason at " + fixture.center() + '.');
        }

        if (plot == null) {
            throw new NullPointerException("Expected the physical fixture plot during removal at " + fixture.center() + '.');
        }

        try {
            plot.getClass().getMethod("destroyAllBlocks").invoke(plot);
            remove.invoke(container, fixture.subLevel(), removed);
        }
        catch (Throwable exception) {
            throw new IllegalStateException("Failed to remove physical fixture at " + fixture.center() + '.', exception);
        }
    }

    record Fixture(SubLevelAccess subLevel, BlockPos center, Object pipeline) {}
}
