package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamPathSignature.Input;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderSnapshot;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserDisplaySnapshot;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender.LaserEmitterRenderer.CachedMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender.LaserEmitterRenderer.RenderState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamRetainedMesh {
    private final CachedMesh base;
    private final Input anchor;
    private final LaserRenderGeometry dependencies;
    private final int range;

    private LaserBeamRetainedMesh(CachedMesh base, LaserRenderGeometry dependencies, int range, Input anchor) {
        this.base = base;
        this.anchor = anchor;
        this.dependencies = dependencies;
        this.range = range;
    }

    static @Nullable LaserBeamRetainedMesh capture(LaserEmitterBlockEntity owner, RenderState state, CachedMesh mesh) {
        if (mesh.snapshot == null || mesh.geometrySnapshot != mesh.snapshot) {
            return null;
        }

        LaserRenderGeometry dependencies = state.traceCache.captureGeometry(mesh.snapshot);
        if (dependencies == null) {
            return null;
        }

        return new LaserBeamRetainedMesh(mesh, dependencies, owner.getLaserRange(), Input.from(mesh.snapshot));
    }

    static @Nullable LaserBeamRetainedMesh captureSynchronized(LaserEmitterBlockEntity owner, CachedMesh mesh, LaserDisplaySnapshot snapshot) {
        if (snapshot.rigid() != null) {
            return null;
        }

        Level level = Objects.requireNonNull(owner.getLevel(), "Retaining a synchronized laser mesh requires an emitter level.");
        return new LaserBeamRetainedMesh(mesh, new LaserRenderGeometry(level, snapshot.dependencies()), snapshot.range(), snapshot.clipping());
    }

    @Nullable CachedMesh restore(LaserEmitterBlockEntity owner, LaserRenderSnapshot snapshot, double radius, boolean restoreContentMatches) {
        Level level = owner.getLevel();
        LaserBeamContext context = anchor.context();
        if (level == null || radius != base.radius || owner.getLaserRange() != range || !anchor.source().equals(LaserBeamContext.state(snapshot.propagation().source())) || !context.localOrigin().equals(snapshot.localOrigin()) || !context.worldOrigin().equals(snapshot.worldOrigin()) || !context.xAxis().equals(snapshot.xAxis()) || !context.yAxis().equals(snapshot.yAxis()) || !context.zAxis().equals(snapshot.zAxis())) {
            return null;
        }

        if (!restoreContentMatches && !dependencies.matches(level)) {
            return null;
        }

        return new CachedMesh(snapshot, base.inputSections, base.sections, base.contacts, base.optics, radius, base.shells, base.particles, base.vertices, base.displayStatus, "Restored retained shell geometry.", true, base.view, base.snapshot, base.geometryCache);
    }
}
