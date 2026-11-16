package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
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
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayMesh.Cache;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayMesh.Result;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayMesh.Status;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamShells.ShellMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamShells.ShellReason;
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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamMeshCache {
    private static final Map<LaserEmitterBlockEntity, Source> SOURCES = new LinkedHashMap<>();
    private static final long FRAME_BUDGET_NS = 2000000;
    private static final int UNSEEN_FRAME_LIMIT = 60;
    private static final Deque<Source> BASE_QUEUE = new ArrayDeque<>();
    private static final Deque<Source> FULL_QUEUE = new ArrayDeque<>();
    private static @Nullable ClientLevel world;
    private static long frame;
    private static long remainingNs;

    private LaserBeamMeshCache() {}

    static void beginFrame(@Nullable ClientLevel nextWorld) {
        if (world != nextWorld) {
            clear();
            world = nextWorld;
        }

        frame++;
        remainingNs = FRAME_BUDGET_NS;
        Iterator<Entry<LaserEmitterBlockEntity, Source>> iterator = SOURCES.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<LaserEmitterBlockEntity, Source> entry = iterator.next();
            LaserEmitterBlockEntity owner = entry.getKey();
            Source source = entry.getValue();
            if (!owner.isRemoved() && owner.getLevel() == source.level && owner.isLaserActive() && frame - source.touchedFrame <= UNSEEN_FRAME_LIMIT) {
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
        BASE_QUEUE.clear();
        FULL_QUEUE.clear();
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

    static @Nullable CachedMesh request(LaserEmitterBlockEntity owner, RenderState renderState, LaserRenderSnapshot snapshot, Vec3 blockOrigin, double radius) {
        Source source = source(owner, renderState);
        if (source.lastSnapshot == snapshot && source.lastRadius == radius) {
            return renderState.mesh;
        }

        Content content = Content.from(snapshot, radius);
        source.lastSnapshot = snapshot;
        source.lastRadius = radius;
        if (supports(snapshot) && content.equals(source.content)) {
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
        if (candidate != null && (candidate.snapshot == null || candidate.geometrySnapshot != candidate.snapshot || candidate.displayStatus == Status.PREPARING)) {
            candidate = null;
        }

        source.invalidate();
        source.content = content;
        source.job = new Job(snapshot, null, blockOrigin, radius, renderState.staticWorldPath, candidate, source.completedCache);
        BASE_QUEUE.addLast(source);
        return null;
    }

    static @Nullable CachedMesh requestSynchronized(LaserEmitterBlockEntity owner, RenderState renderState, LaserDisplaySnapshot snapshot, Vec3 blockOrigin) {
        Source source = source(owner, renderState);
        if (source.synchronizedInput == snapshot) {
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

        source.invalidate();
        source.retained = null;
        source.baseContent = null;
        source.job = new Job(null, input, blockOrigin, input.context().sourceRadius(), true, null, source.completedCache);
        BASE_QUEUE.addLast(source);
        return null;
    }

    static void advance() {
        long start = System.nanoTime();
        while (System.nanoTime() - start < remainingNs) {
            Source source = poll(BASE_QUEUE);
            if (source == null) {
                source = poll(FULL_QUEUE);
            }
            if (source == null) {
                break;
            }

            Job job = source.job;
            if (job == null) {
                continue;
            }

            if (source.owner.isRemoved() || source.owner.getLevel() != source.level || !source.owner.isLaserActive() || source.owner.getLaserRange() != source.range || !source.owner.getDisplayInstance().equals(source.instance)) {
                source.invalidate();
                continue;
            }

            try {
                boolean complete = job.step();
                if (source.job != job) {
                    continue;
                }

                CachedMesh ready = job.ready;
                if (ready != null) {
                    source.publish(ready);
                    job.ready = null;
                }
                if (complete) {
                    source.job = null;
                    continue;
                }

                Deque<Source> next = job.baseReady ? FULL_QUEUE : BASE_QUEUE;
                next.addLast(source);
            }
            catch (RuntimeException failure) {
                source.renderState.fail(source.owner, failure);
            }
        }
        remainingNs = Math.max(0, remainingNs - (System.nanoTime() - start));
    }

    static void local(LaserEmitterBlockEntity owner) {
        Source source = SOURCES.get(owner);
        if (source == null || source.synchronizedInput == null) {
            return;
        }

        source.job = null;
        BASE_QUEUE.remove(source);
        FULL_QUEUE.remove(source);
        source.synchronizedInput = null;
        source.lastSnapshot = null;
        source.content = null;
    }

    static void remove(LaserEmitterBlockEntity owner) {
        Source source = SOURCES.remove(owner);
        if (source == null) {
            LaserBeamCoverageRenderer.invalidate(owner);
            return;
        }

        source.invalidate();
    }

    private static Source source(LaserEmitterBlockEntity owner, RenderState state) {
        Source source = SOURCES.get(owner);
        if (source == null || source.renderState != state || source.level != owner.getLevel() || source.range != owner.getLaserRange() || !source.instance.equals(owner.getDisplayInstance())) {
            if (source != null) {
                source.invalidate();
            }

            source = new Source(owner, state);
            SOURCES.put(owner, source);
        }

        source.touchedFrame = frame;
        return source;
    }

    private static @Nullable Source poll(Deque<Source> queue) {
        for (int remaining = queue.size(); remaining > 0; remaining--) {
            Source source = queue.removeFirst();
            if (source.job == null) {
                continue;
            }

            if (source.touchedFrame == frame) {
                return source;
            }

            queue.addLast(source);
        }
        return null;
    }

    private static final class Source {
        private final LaserEmitterBlockEntity owner;
        private final @Nullable Level level;
        private long touchedFrame;
        private @Nullable Job job;
        private final RenderState renderState;
        private final UUID instance;
        private final int range;
        private @Nullable Content content;
        private @Nullable LaserDisplaySnapshot synchronizedInput;
        private @Nullable Content baseContent;
        private @Nullable LaserBeamRetainedMesh retained;
        private @Nullable LaserRenderSnapshot lastSnapshot;
        private double lastRadius;
        private @Nullable Cache completedCache;

        private Source(LaserEmitterBlockEntity owner, RenderState renderState) {
            this.owner = owner;
            level = owner.getLevel();
            instance = owner.getDisplayInstance();
            range = owner.getLaserRange();
            this.renderState = renderState;
        }

        private void publish(CachedMesh mesh) {
            renderState.mesh = mesh;
            if (mesh.displayStatus == Status.PREPARING) {
                return;
            }

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
            job = null;
            BASE_QUEUE.remove(this);
            FULL_QUEUE.remove(this);
            renderState.mesh = null;
            LaserBeamCoverageRenderer.invalidate(owner);
        }
    }

    private static final class Job {
        private final @Nullable LaserRenderSnapshot snapshot;
        private final Vec3 blockOrigin;
        private final double radius;
        private final boolean staticWorldPath;
        private final @Nullable CachedMesh candidate;
        private final @Nullable Cache previous;
        private @Nullable LaserBeamDisplayInput input;
        private @Nullable LaserBeamWork<LaserBeamDisplayInput> preparation;
        private @Nullable LaserBeamWork<Result> shellWork;
        private @Nullable LaserBeamWork<LaserBeamPreparedBuffers.Result> bufferWork;
        private @Nullable Result display;
        private @Nullable CachedMesh ready;
        private List<OpticalPlane> contacts = List.of();
        private List<OpticalSurface> optics = List.of();
        private boolean baseReady;
        private int phase;

        private Job(@Nullable LaserRenderSnapshot snapshot, @Nullable LaserBeamDisplayInput input, Vec3 blockOrigin, double radius, boolean staticWorldPath, @Nullable CachedMesh candidate, @Nullable Cache previous) {
            this.snapshot = snapshot;
            this.input = input;
            this.blockOrigin = blockOrigin;
            this.radius = radius;
            this.staticWorldPath = staticWorldPath;
            this.candidate = candidate;
            this.previous = previous;
            if (snapshot != null) {
                preparation = LaserBeamDisplayMesh.beginPrepare(snapshot);
            }
        }

        private boolean step() {
            LaserBeamDisplayInput input = this.input;
            if (input == null) {
                LaserBeamWork<LaserBeamDisplayInput> work = Objects.requireNonNull(preparation, "Local display work requires preparation.");
                if (!work.step()) {
                    return false;
                }

                this.input = work.result();
                preparation = null;
                return false;
            }

            if (phase == 0) {
                if (snapshot != null) {
                    List<Segment> segments = snapshot.propagation().segments();
                    contacts = segments.stream().map(Segment::hit).filter(Objects::nonNull).map(hit -> new OpticalPlane(hit.position(), hit.surface().normal())).toList();
                    optics = segments.stream().map(Segment::hit).filter(Objects::nonNull).map(OpticalHit::surface).filter(surface -> surface.aperture() != null).toList();
                }

                if (candidate != null && candidate.staticWorldPath == staticWorldPath && candidate.inputSections.equals(input.base().sections()) && candidate.contacts.equals(contacts) && candidate.optics.equals(optics) && candidate.radius == radius) {
                    display = new Result(candidate.sections, candidate.shells, candidate.displayStatus, candidate.displayReason, candidate.geometryCache);
                    phase = 3;
                }
                else {
                    ShellMesh shells = new ShellMesh(List.of(List.of(), List.of(), input.base().faces()), 3, ShellReason.PREPARING);
                    display = new Result(input.base().sections(), shells, Status.PREPARING, "Current base geometry; halo preparation is pending.");
                    phase = 1;
                }
                bufferWork = LaserBeamPreparedBuffers.begin(input.context(), display.sections(), display.shells(), blockOrigin);
                return false;
            }

            if (phase == 2) {
                LaserBeamWork<Result> work = shellWork;
                if (work == null) {
                    shellWork = LaserBeamDisplayMesh.begin(input, previous);
                    return false;
                }
                if (!work.step()) {
                    return false;
                }

                display = work.result();
                shellWork = null;
                bufferWork = LaserBeamPreparedBuffers.begin(input.context(), display.sections(), display.shells(), blockOrigin);
                phase = 3;
                return false;
            }

            LaserBeamWork<LaserBeamPreparedBuffers.Result> work = Objects.requireNonNull(bufferWork, "Display publication requires prepared buffers.");
            if (!work.step()) {
                return false;
            }

            LaserBeamPreparedBuffers.Result buffers = work.result();
            Result display = Objects.requireNonNull(this.display, "Display publication requires current geometry.");
            if (snapshot == null) {
                ready = new CachedMesh(input, display, buffers.view(), buffers.particles(), buffers.vertices());
            }
            else {
                ready = new CachedMesh(snapshot, input.base().sections(), display.sections(), contacts, optics, radius, display.shells(), buffers.particles(), buffers.vertices(), display.status(), display.reason(), staticWorldPath, buffers.view(), snapshot, display.cache());
            }
            bufferWork = null;
            if (phase == 3) {
                return true;
            }

            baseReady = true;
            phase = 2;
            return false;
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
