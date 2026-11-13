package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.levelWrappers.WrappedClientLevel;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayMesh.Cache;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayMesh.Result;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayMesh.Status;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticles.FlowPoint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticles.ShellSampler;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamShells.ShellMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.Face;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamView;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderSnapshot;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderTraceCache;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserDisplayRequestPacket;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserDisplaySnapshot;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender.LaserBeamVertices.Layer;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LaserEmitterRenderer extends SafeBlockEntityRenderer<LaserEmitterBlockEntity> {
    private static final double PARTICLE_VIEW_DISTANCE_SQR = 4096;
    private static final long DISPLAY_RETRY_DELAY_NS = 1000000000L;

    private static final Set<LaserEmitterRenderer> RENDERERS = Collections.newSetFromMap(new WeakHashMap<>());

    private final Map<LaserEmitterBlockEntity, RenderState> renderStates = new WeakHashMap<>();

    public LaserEmitterRenderer(Context ignored) {
        RENDERERS.add(this);
    }

    static void clearCaches() {
        LaserBeamMeshCache.clear();
        LaserBeamCoverageRenderer.clear();
        for (LaserEmitterRenderer renderer : RENDERERS) {
            for (RenderState state : renderer.renderStates.values()) {
                state.particleFlow.clear();
                state.impactParticles.clear();
            }
            renderer.renderStates.clear();
        }
    }

    static void clearWorld(ClientLevel level) {
        for (LaserEmitterRenderer renderer : RENDERERS) {
            renderer.renderStates.values().removeIf(state -> state.ownerLevel.get() == level);
        }
    }

    @Override
    protected void renderSafe(LaserEmitterBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        boolean shaderAvailable = LaserRenderTypes.isAvailable();
        if (!shaderAvailable || level == null || !blockEntity.isLaserActive()) {
            LaserBeamMeshCache.remove(blockEntity);
            RenderState removed = renderStates.remove(blockEntity);
            if (removed == null) {
                return;
            }

            if (!shaderAvailable || blockEntity.isDisplaySuspended()) {
                removed.particleFlow.clear();
                removed.impactParticles.clear();
                return;
            }

            removed.particleFlow.detach(partialTick);
            return;
        }

        BlockPos blockPos = blockEntity.getBlockPos();
        Vec3 blockOrigin = Vec3.atLowerCornerOf(blockPos);
        RenderState state = renderStates.get(blockEntity);
        if (state == null || state.ownerLevel.get() != level) {
            if (state != null) {
                state.clearGeometry(blockEntity);
                state.particleFlow.clear();
                state.impactParticles.clear();
            }

            state = new RenderState(level);
            renderStates.put(blockEntity, state);
        }
        PonderLevel ponderLevel = level instanceof PonderLevel sceneLevel ? sceneLevel : null;
        boolean ponder = ponderLevel != null;
        if (!ponder) {
            if (!blockEntity.isVirtual()) {
                UUID instance = blockEntity.getDisplayInstance();
                boolean suspended = blockEntity.isDisplaySuspended();
                if (blockEntity.takeDisplayBaselineRetry(level.getGameTime()) || !suspended && !instance.equals(state.requestedInstance)) {
                    state.requestedInstance = instance;
                    state.retryAtNs = Long.MIN_VALUE;
                    CatnipServices.NETWORK.sendToServer(new LaserDisplayRequestPacket(level.dimension().location(), blockPos, instance));
                }
                if (suspended) {
                    state.clearGeometry(blockEntity);
                    state.particleFlow.clear();
                    state.impactParticles.clear();
                    state.lastParticleTick = Long.MIN_VALUE;
                    state.retryAtNs = Long.MIN_VALUE;
                    return;
                }
            }

            if (blockEntity.isDisplayOff()) {
                state.clearGeometry(blockEntity);
                state.particleFlow.detach(partialTick);
                state.retryAtNs = Long.MIN_VALUE;
                return;
            }
        }

        long retryAtNs = state.retryAtNs;
        if (retryAtNs != Long.MIN_VALUE && System.nanoTime() - retryAtNs < 0) {
            return;
        }

        Matrix4f pose;
        List<Layer> layers;
        try {
            Minecraft minecraft = Minecraft.getInstance();
            BeamState source = blockEntity.createBeamSource();
            LaserDisplaySnapshot synchronizedInput = LaserBeamSync.current(blockEntity, level, source, partialTick);
            CachedMesh mesh;
            LaserBeamContext context;
            LaserBeamContext rigidSnapshotContext = null;
            if (synchronizedInput != null) {
                mesh = LaserBeamMeshCache.requestSynchronized(blockEntity, state, synchronizedInput, blockOrigin);
                context = synchronizedInput.input().context();
                if (synchronizedInput.rigid() != null) {
                    rigidSnapshotContext = context;
                    context = context.atRenderPose(level, partialTick);
                }
            }
            else {
                LaserBeamMeshCache.local(blockEntity);
                LaserRenderSnapshot snapshot = state.traceCache.trace(level, source, blockOrigin, blockEntity.getLaserRange(), partialTick);
                BeamPropagation propagation = snapshot.propagation();
                List<Segment> segments = propagation.segments();
                double radius = source.profile().radius();
                mesh = state.mesh;
                if (state.checkedSnapshot != snapshot || state.checkedRadius != radius) {
                    state.context = LaserBeamContext.from(snapshot);
                    state.checkedSnapshot = snapshot;
                    state.checkedRadius = radius;
                    state.staticWorldPath = level == minecraft.level && !SubLevelBridge.resolve(level, blockPos).inSubLevel() && segments.stream().map(Segment::hit).filter(Objects::nonNull).noneMatch(hit -> {
                        OpticalTarget target = hit.target();
                        return target.contraptionId() != null || target.inSubLevel();
                    });
                    state.cacheEligible = state.staticWorldPath && LaserBeamMeshCache.supports(snapshot);
                }

                context = Objects.requireNonNull(state.context, "Local laser rendering requires a display context.");
                if (state.cacheEligible) {
                    mesh = LaserBeamMeshCache.request(blockEntity, state, snapshot, blockOrigin, radius);
                }
                else {
                    LaserBeamMeshCache.detach(blockEntity);
                    if (mesh == null || mesh.snapshot != snapshot) {
                        List<DrawSection> inputSections = LaserBeamMesh.create(snapshot);
                        boolean staticWorldPath = state.staticWorldPath;
                        List<DrawSection> sections;
                        List<OpticalSurface> optics = segments.stream().map(Segment::hit).filter(Objects::nonNull).map(OpticalHit::surface).filter(surface -> surface.aperture() != null).toList();
                        List<OpticalPlane> contacts = segments.stream().map(Segment::hit).filter(Objects::nonNull).map(hit -> new OpticalPlane(hit.position(), hit.surface().normal())).toList();
                        ShellMesh shells;
                        ShellSampler particles;
                        LaserBeamVertices vertices;
                        Status displayStatus;
                        String displayReason;
                        if (mesh != null && mesh.geometrySnapshot == mesh.snapshot && mesh.staticWorldPath == staticWorldPath && mesh.inputSections.equals(inputSections) && mesh.contacts.equals(contacts) && mesh.optics.equals(optics) && mesh.radius == radius) {
                            sections = mesh.sections;
                            displayStatus = mesh.displayStatus;
                            displayReason = mesh.displayReason;
                            shells = mesh.shells;
                            particles = mesh.particles;
                            vertices = mesh.vertices;
                        }
                        else {
                            Result display = LaserBeamDisplayMesh.create(LaserBeamDisplayMesh.prepare(snapshot, inputSections));

                            sections = display.sections();
                            shells = display.shells();
                            displayStatus = display.status();
                            displayReason = display.reason();
                            particles = null;
                            vertices = null;
                        }

                        mesh = new CachedMesh(snapshot, inputSections, sections, contacts, optics, radius, shells, particles, vertices, displayStatus, displayReason, staticWorldPath);
                        state.mesh = mesh;
                    }
                }
            }
            pose = poseStack.last().pose();
            Vec3 xAxis = context.xAxis();
            Vec3 yAxis = context.yAxis();
            Vec3 zAxis = context.zAxis();
            double scaleX = xAxis.length();
            double scaleY = yAxis.length();
            double scaleZ = zAxis.length();
            if (scaleX != 1 || scaleY != 1 || scaleZ != 1) {
                pose = new Matrix4f(pose).scale((float) (1 / scaleX), (float) (1 / scaleY), (float) (1 / scaleZ));
            }

            Camera camera = minecraft.gameRenderer.getMainCamera();
            Vec3 cameraPosition = camera.getPosition();
            Vec3 viewPosition = cameraPosition;
            if (rigidSnapshotContext != null) {
                viewPosition = rigidSnapshotContext.toWorldPosition(context.toLocalPosition(cameraPosition));
            }
            double widthPixels = Double.POSITIVE_INFINITY;
            if (!ponder) {
                widthPixels = mesh.view.radiusDistanceRatio(viewPosition) * minecraft.getWindow().getHeight() / Math.tan(Math.toRadians(minecraft.options.fov().get()) * 0.5);
            }

            List<List<Face>> surfacesByLayer = state.display.select(mesh.shells, widthPixels);
            List<Face> surfaces = surfacesByLayer.getFirst();
            LaserBeamVertices vertices = mesh.vertices;
            if (vertices == null || !vertices.matchesOrigin(blockOrigin)) {
                vertices = new LaserBeamVertices(blockOrigin);
                mesh.vertices = vertices;
            }

            if (mesh.impact == null) {
                mesh.impact = LaserBeamImpact.create(mesh.particleContext, mesh.sections, blockOrigin);
            }
            layers = mesh.impact.attach(vertices.select(surfacesByLayer));
            state.updateParticles(minecraft, ponderLevel, context, cameraPosition, mesh, surfaces, blockPos.asLong(), partialTick);
        }
        catch (RuntimeException failure) {
            if (retryAtNs == Long.MIN_VALUE) {
                CCBAPI.LOGGER.warn("Failed to prepare the laser display at {} in dimension '{}'; pausing this display and retrying after {} ms.", blockPos, level.dimension().location(), DISPLAY_RETRY_DELAY_NS / 1000000, failure);
            }

            state.retryAtNs = System.nanoTime() + DISPLAY_RETRY_DELAY_NS;
            state.clearGeometry(blockEntity);
            state.particleFlow.clear();
            state.impactParticles.clear();
            state.lastParticleTick = Long.MIN_VALUE;
            return;
        }

        state.retryAtNs = Long.MIN_VALUE;
        VertexConsumer buffer;
        if (ponder && bufferSource instanceof SuperRenderTypeBuffer ponderBuffer) {
            buffer = ponderBuffer.getLateBuffer(LaserRenderTypes.PONDER_LASER);
        }
        else {
            buffer = bufferSource.getBuffer(ponder ? LaserRenderTypes.PONDER_LASER : LaserRenderTypes.LASER);
        }

        if (!ponder) {
            LaserBeamCoverageRenderer.submit(blockEntity, pose, layers);
            return;
        }

        for (Layer layer : layers) {
            layer.render(buffer, pose);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(LaserEmitterBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    static final class RenderState {
        private final WeakReference<Level> ownerLevel;
        final LaserRenderTraceCache traceCache = new LaserRenderTraceCache();
        private final LaserBeamDisplayState display = new LaserBeamDisplayState();
        private final LaserBeamParticleFlow particleFlow = new LaserBeamParticleFlow();
        private final LaserBeamImpactParticles impactParticles = new LaserBeamImpactParticles();
        @Nullable CachedMesh mesh;
        private @Nullable LaserRenderSnapshot checkedSnapshot;
        private @Nullable LaserBeamContext context;
        private WeakReference<ClientLevel> particleLevel = new WeakReference<>(null);
        private long lastParticleTick = Long.MIN_VALUE;
        private long retryAtNs = Long.MIN_VALUE;
        private double checkedRadius;
        private boolean staticWorldPath;
        private boolean cacheEligible;
        private @Nullable UUID requestedInstance;

        private RenderState(Level level) {
            ownerLevel = new WeakReference<>(level);
        }

        private void clearGeometry(LaserEmitterBlockEntity owner) {
            LaserBeamMeshCache.remove(owner);
            traceCache.clear();
            mesh = null;
            checkedSnapshot = null;
            context = null;
        }

        private void updateParticles(Minecraft minecraft, @Nullable PonderLevel ponderLevel, LaserBeamContext context, Vec3 cameraPosition, CachedMesh mesh, List<Face> surfaces, long seed, float partialTick) {
            int stride = switch (minecraft.options.particles().get()) {
                case ALL -> 1;
                case DECREASED -> 2;
                case MINIMAL -> 0;
            };
            if (stride == 0 || surfaces.isEmpty()) {
                particleFlow.detach(partialTick);
                return;
            }

            long particleTick = minecraft.levelRenderer.getTicks();
            if (ponderLevel != null) {
                PonderScene scene = ponderLevel.scene;
                if (scene == null) {
                    return;
                }

                particleTick = scene.getCurrentTime();
            }
            else if (cameraPosition.distanceToSqr(context.worldOrigin().add(context.xAxis().add(context.yAxis()).add(context.zAxis()).scale(0.5))) > PARTICLE_VIEW_DISTANCE_SQR) {
                particleFlow.detach(partialTick);
                return;
            }

            particleFlow.touch();
            if (ponderLevel == null && minecraft.isPaused() || lastParticleTick == particleTick) {
                return;
            }

            if (ponderLevel != null && particleTick < lastParticleTick) {
                impactParticles.clear();
            }
            lastParticleTick = particleTick;
            ClientLevel particleLevel = minecraft.level;
            if (ponderLevel != null) {
                particleLevel = this.particleLevel.get();
                if (particleLevel == null) {
                    particleLevel = WrappedClientLevel.of(ponderLevel);
                    this.particleLevel = new WeakReference<>(particleLevel);
                }
            }
            if (particleLevel == null) {
                return;
            }

            impactParticles.update(particleLevel, ponderLevel, context, Objects.requireNonNull(mesh.impact, "Impact particles require prepared impact geometry."), cameraPosition, particleTick, seed, stride);
            List<FlowPoint> points = mesh.sampleParticles(surfaces, particleTick, seed, stride);
            particleFlow.update(particleLevel, ponderLevel, Objects.requireNonNull(mesh.particles, "Laser particle flow requires a shell sampler."), context, points, particleTick, seed, stride, partialTick);
        }
    }

    static final class CachedMesh {
        private final LaserBeamContext particleContext;
        final @Nullable LaserRenderSnapshot snapshot;
        final @Nullable Cache geometryCache;
        final @Nullable LaserRenderSnapshot geometrySnapshot;
        final boolean staticWorldPath;
        final List<DrawSection> inputSections;
        final List<DrawSection> sections;
        final List<OpticalSurface> optics;
        final Status displayStatus;
        final String displayReason;
        final List<OpticalPlane> contacts;
        final double radius;
        final ShellMesh shells;
        final LaserBeamView view;
        @Nullable ShellSampler particles;
        @Nullable LaserBeamVertices vertices;
        private @Nullable LaserBeamImpact impact;

        private CachedMesh(LaserRenderSnapshot snapshot, List<DrawSection> inputSections, List<DrawSection> sections, List<OpticalPlane> contacts, List<OpticalSurface> optics, double radius, ShellMesh shells, @Nullable ShellSampler particles, @Nullable LaserBeamVertices vertices, Status displayStatus, String displayReason, boolean staticWorldPath) {
            this(snapshot, inputSections, sections, contacts, optics, radius, shells, particles, vertices, displayStatus, displayReason, staticWorldPath, LaserBeamView.create(LaserBeamContext.from(snapshot), sections), snapshot, null);
        }

        CachedMesh(LaserRenderSnapshot snapshot, List<DrawSection> inputSections, List<DrawSection> sections, List<OpticalPlane> contacts, List<OpticalSurface> optics, double radius, ShellMesh shells, @Nullable ShellSampler particles, @Nullable LaserBeamVertices vertices, Status displayStatus, String displayReason, boolean staticWorldPath, LaserBeamView view, @Nullable LaserRenderSnapshot geometrySnapshot, @Nullable Cache geometryCache) {
            this.snapshot = snapshot;
            particleContext = LaserBeamContext.from(geometrySnapshot == null ? snapshot : geometrySnapshot);
            this.geometryCache = geometryCache;
            this.geometrySnapshot = geometrySnapshot;
            this.staticWorldPath = staticWorldPath;
            this.inputSections = inputSections;
            this.sections = sections;
            this.optics = optics;
            this.displayStatus = displayStatus;
            this.displayReason = displayReason;
            this.contacts = contacts;
            this.radius = radius;
            this.shells = shells;
            this.view = view;
            this.particles = particles;
            this.vertices = vertices;
        }

        CachedMesh(LaserBeamDisplayInput input, Result result, LaserBeamView view, @Nullable ShellSampler particles, @Nullable LaserBeamVertices vertices) {
            snapshot = null;
            particleContext = input.context();
            geometrySnapshot = null;
            geometryCache = result.cache();
            staticWorldPath = true;
            inputSections = input.base().sections();
            sections = result.sections();
            contacts = List.of();
            optics = List.of();
            radius = particleContext.sourceRadius();
            shells = result.shells();
            displayStatus = result.status();
            displayReason = result.reason();
            this.view = view;
            this.particles = particles;
            this.vertices = vertices;
        }

        private List<FlowPoint> sampleParticles(List<Face> surfaces, long tick, long seed, int stride) {
            if (particles == null) {
                particles = new ShellSampler(particleContext, sections, surfaces);
            }

            return particles.sampleFlow(tick, seed, stride);
        }
    }
}
