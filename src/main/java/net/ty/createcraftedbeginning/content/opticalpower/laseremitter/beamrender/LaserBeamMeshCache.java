package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Boundary;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Termination;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalInteractor;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayMesh.Cache;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayMesh.Result;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamShells.ShellMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamWork;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamTerminal;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderSnapshot;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserDisplaySnapshot;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender.LaserEmitterRenderer.CachedMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender.LaserEmitterRenderer.RenderState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamMeshCache {
    private static final Map<LaserEmitterBlockEntity, Source> SOURCES = new LinkedHashMap<>();
    private static @Nullable ClientLevel world;

    private LaserBeamMeshCache() {}

    static void beginFrame(@Nullable ClientLevel nextWorld) {
        if (world != nextWorld) {
            clear();
            world = nextWorld;
        }

        Iterator<Entry<LaserEmitterBlockEntity, Source>> iterator = SOURCES.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<LaserEmitterBlockEntity, Source> entry = iterator.next();
            LaserEmitterBlockEntity owner = entry.getKey();
            if (!owner.isRemoved() && owner.getLevel() == world && owner.isLaserActive()) {
                continue;
            }

            entry.getValue().invalidate();
            iterator.remove();
        }
    }

    static void clear() {
        for (Source source : SOURCES.values()) {
            source.invalidate();
        }

        SOURCES.clear();
        world = null;
    }

    static boolean supports(LaserRenderSnapshot snapshot) {
        for (Segment segment : snapshot.propagation().segments()) {
            OpticalHit hit = segment.hit();
            if (hit == null) {
                continue;
            }

            OpticalInteractor interactor = hit.surface().interactor();
            if (interactor != OpticalInteractor.ABSORBING && !interactor.supportsDisplayMeshReuse()) {
                return false;
            }
        }

        return true;
    }

    static CachedMesh request(LaserEmitterBlockEntity owner, RenderState renderState, LaserRenderSnapshot snapshot, Vec3 blockOrigin, double radius) {
        Source source = SOURCES.computeIfAbsent(owner, key -> new Source(key, renderState));
        source.renderState = renderState;
        if (source.lastSnapshot == snapshot && source.lastRadius == radius && renderState.mesh != null) {
            return renderState.mesh;
        }

        Content content = Content.from(snapshot, radius);
        source.lastSnapshot = snapshot;
        source.lastRadius = radius;
        if (content.equals(source.content) && renderState.mesh != null) {
            return renderState.mesh;
        }

        CachedMesh candidate = renderState.mesh;
        LaserBeamRetainedMesh retained = source.retained;
        if (retained != null) {
            CachedMesh restored = retained.restore(owner, snapshot, radius, content.equals(source.baseContent));
            if (restored != null) {
                source.invalidate();
                source.content = content;
                renderState.mesh = restored;
                return restored;
            }

            candidate = null;
            source.retained = null;
            source.baseContent = null;
        }
        if (candidate != null && (candidate.snapshot == null || candidate.geometrySnapshot != candidate.snapshot)) {
            candidate = null;
        }

        source.invalidate();
        source.content = content;
        CachedMesh mesh = build(snapshot, blockOrigin, radius, candidate, source.completedCache);
        source.publish(mesh);
        return mesh;
    }

    static CachedMesh requestSynchronized(LaserEmitterBlockEntity owner, RenderState renderState, LaserDisplaySnapshot snapshot, Vec3 blockOrigin) {
        Source source = SOURCES.computeIfAbsent(owner, key -> new Source(key, renderState));
        source.renderState = renderState;
        if (source.synchronizedInput == snapshot && renderState.mesh != null) {
            return renderState.mesh;
        }

        LaserBeamDisplayInput input = snapshot.input();
        CachedMesh candidate = renderState.mesh;
        source.synchronizedInput = snapshot;
        source.lastSnapshot = null;
        source.content = null;
        if (candidate != null && candidate.geometryCache != null && input.equals(candidate.geometryCache.input())) {
            source.retained = LaserBeamRetainedMesh.captureSynchronized(owner, candidate, snapshot);
            source.baseContent = null;
            source.completedCache = candidate.geometryCache;
            return candidate;
        }

        Result display = complete(LaserBeamDisplayMesh.begin(input, source.completedCache));
        LaserBeamPreparedBuffers.Result buffers = complete(LaserBeamPreparedBuffers.begin(input.context(), display.sections(), display.shells(), blockOrigin));
        CachedMesh mesh = new CachedMesh(input, display, buffers.view(), buffers.particles(), buffers.vertices());
        source.publish(mesh);
        return mesh;
    }

    static void local(LaserEmitterBlockEntity owner) {
        Source source = SOURCES.get(owner);
        if (source == null || source.synchronizedInput == null) {
            return;
        }

        source.synchronizedInput = null;
        source.lastSnapshot = null;
        source.content = null;
    }

    static void detach(LaserEmitterBlockEntity owner) {
        SOURCES.remove(owner);
    }

    static void remove(LaserEmitterBlockEntity owner) {
        Source source = SOURCES.remove(owner);
        if (source == null) {
            LaserBeamCoverageRenderer.invalidate(owner);
            return;
        }

        source.invalidate();
    }

    private static CachedMesh build(LaserRenderSnapshot snapshot, Vec3 blockOrigin, double radius, @Nullable CachedMesh candidate, @Nullable Cache previous) {
        List<DrawSection> inputs = LaserBeamMesh.create(snapshot);
        BeamPropagation propagation = snapshot.propagation();
        List<Segment> segments = propagation.segments();
        List<OpticalPlane> contacts = segments.stream().map(Segment::hit).filter(Objects::nonNull).map(hit -> new OpticalPlane(hit.position(), hit.surface().normal())).toList();
        List<OpticalSurface> optics = segments.stream().map(Segment::hit).filter(Objects::nonNull).map(OpticalHit::surface).filter(surface -> surface.aperture() != null).toList();
        Result display;
        if (candidate != null && candidate.staticWorldPath && candidate.inputSections.equals(inputs) && candidate.contacts.equals(contacts) && candidate.optics.equals(optics) && candidate.radius == radius) {
            display = new Result(candidate.sections, candidate.shells, candidate.displayStatus, candidate.displayReason, candidate.geometryCache);
        }
        else {
            display = complete(LaserBeamDisplayMesh.begin(LaserBeamDisplayMesh.prepare(snapshot, inputs), previous));
        }

        List<DrawSection> sections = display.sections();
        ShellMesh shells = display.shells();
        LaserBeamPreparedBuffers.Result buffers = complete(LaserBeamPreparedBuffers.begin(LaserBeamContext.from(snapshot), sections, shells, blockOrigin));
        return new CachedMesh(snapshot, inputs, sections, contacts, optics, radius, shells, buffers.particles(), buffers.vertices(), display.status(), display.reason(), true, buffers.view(), snapshot, display.cache());
    }

    private static <T> T complete(LaserBeamWork<T> work) {
        while (!work.step()) {
        }
        return work.result();
    }

    private static final class Source {
        private final LaserEmitterBlockEntity owner;
        private RenderState renderState;
        private @Nullable Content content;
        private @Nullable LaserDisplaySnapshot synchronizedInput;
        private @Nullable Content baseContent;
        private @Nullable LaserBeamRetainedMesh retained;
        private @Nullable LaserRenderSnapshot lastSnapshot;
        private double lastRadius;
        private @Nullable Cache completedCache;

        private Source(LaserEmitterBlockEntity owner, RenderState renderState) {
            this.owner = owner;
            this.renderState = renderState;
        }

        private void publish(CachedMesh mesh) {
            renderState.mesh = mesh;
            completedCache = mesh.geometryCache;
            if (mesh.snapshot != null && mesh.geometrySnapshot == mesh.snapshot) {
                retained = LaserBeamRetainedMesh.capture(owner, renderState, mesh);
                baseContent = null;
                if (retained != null) {
                    baseContent = Content.from(mesh.snapshot, mesh.radius);
                }
            }
            if (synchronizedInput == null) {
                return;
            }

            retained = LaserBeamRetainedMesh.captureSynchronized(owner, mesh, synchronizedInput);
            baseContent = null;
        }

        private void invalidate() {
            renderState.mesh = null;
            LaserBeamCoverageRenderer.invalidate(owner);
        }
    }

    private record Content(BeamState source, List<SegmentContent> segments, List<Termination> terminations, Vec3 localOrigin, Vec3 worldOrigin, Vec3 xAxis, Vec3 yAxis, Vec3 zAxis, double radius) {
        private static Content from(LaserRenderSnapshot snapshot, double radius) {
            BeamPropagation input = snapshot.propagation();
            List<SegmentContent> segments = input.segments().stream().map(segment -> new SegmentContent(segment.parentIndex(), segment.start(), segment.incident(), segment.length(), segment.remainingRange(), HitContent.from(segment.hit()), segment.absorbedFluxLm(), segment.contact(), segment.boundary(), segment.terminal())).toList();
            return new Content(input.source(), segments, input.terminations(), snapshot.localOrigin(), snapshot.worldOrigin(), snapshot.xAxis(), snapshot.yAxis(), snapshot.zAxis(), radius);
        }
    }

    private record SegmentContent(int parentIndex, BeamState start, BeamState incident, double length, double remainingRange, @Nullable HitContent hit, double absorbedFluxLm, @Nullable BeamContact contact, @Nullable Boundary boundary, @Nullable BeamTerminal terminal) {}

    private record HitContent(OpticalTarget target, Vec3 location, Direction direction, BlockPos blockPos, boolean inside, Type type, Vec3 position, OpticalSurface surface, @Nullable Vec3 localPlaneNormal) {
        private static @Nullable HitContent from(@Nullable OpticalHit hit) {
            if (hit == null) {
                return null;
            }

            BlockHitResult local = hit.localHit();
            return new HitContent(hit.target(), local.getLocation(), local.getDirection(), local.getBlockPos(), local.isInside(), local.getType(), hit.position(), hit.surface(), hit.localPlaneNormal());
        }
    }

}
