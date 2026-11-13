package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorShape;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Boundary;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.OcclusionWork;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Reason;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalInteractor;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.PropagationBudget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Vertex;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamTerminal;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalAperture;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamOcclusionSolver.Query;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamSweepSection.Contact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceGeometry.PreparedBlock;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.LocalRegion;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BeamFirstHitQuery implements Query {
    private final Level level;
    private final LaserCollisionContext collision;
    private final boolean rendering;
    private final PropagationBudget budget;
    private int queries;
    private int candidates;
    private int shapeTests;

    BeamFirstHitQuery(Level level, LaserCollisionContext collision, boolean rendering, PropagationBudget budget) {
        this.level = level;
        this.collision = collision;
        this.rendering = rendering;
        this.budget = budget;
    }

    private static OpticalPlane transformPlane(OpticalPlane plane, UnaryOperator<Vec3> transform) {
        Vec3 normal = plane.normal();
        Vec3 reference = Math.abs(normal.x) < 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 horizontal = normal.cross(reference).normalize();
        Vec3 vertical = normal.cross(horizontal);
        Vec3 origin = plane.point();
        Vec3 point = transform.apply(origin);
        Vec3 transformed = transform.apply(origin.add(horizontal)).subtract(point).cross(transform.apply(origin.add(vertical)).subtract(point)).normalize();
        return new OpticalPlane(point, transformed);
    }

    @Override
    public OcclusionWork work() {
        return new OcclusionWork(queries, candidates, shapeTests);
    }

    @Override
    public Result findNext(BeamState beam, double range, @Nullable OpticalHit previous) {
        queries++;
        int maxCandidates = budget.maxCandidates();
        int maxShapeTests = budget.maxShapeTests();
        BeamFrame frame = beam.frame();
        Vec3 origin = frame.origin();
        Vec3 direction = frame.direction();
        OpticalPlane start = new OpticalPlane(origin, direction);
        if (previous != null) {
            Vec3 normal = previous.surface().normal();
            if (normal.dot(direction) < 0) {
                normal = normal.scale(-1);
            }
            start = new OpticalPlane(previous.position(), normal);
        }
        List<BeamSweepSection> volumes = BeamSweepSection.create(beam, range, start);
        AABB bounds = new AABB(origin, origin);
        for (BeamSweepSection volume : volumes) {
            bounds = bounds.minmax(volume.bounds());
        }
        LaserTraceGeometry geometry = collision.getGeometry();
        geometry.includeBounds(bounds);
        float partialTick = collision.getPartialTick();
        List<LaserQuerySpace> spaces = new ArrayList<>();
        for (LocalRegion region : SubLevelBridge.localRegions(level, bounds, partialTick, rendering)) {
            spaces.add(LaserQuerySpace.world(level, region));
        }
        AABB touchingBounds = bounds.inflate(BeamSweepSection.CONTACT_TOLERANCE);
        for (LaserQuerySpace space : geometry.mounted(level, partialTick)) {
            if (!space.worldBounds().intersects(touchingBounds)) {
                continue;
            }

            spaces.add(space);
        }
        Result nearest = new Result(range, null, null, Reason.RANGE_EXHAUSTED);
        TerminalCandidate terminal = null;
        for (LaserQuerySpace space : spaces) {
            List<BeamSweepSection> local = volumes.stream().map(volume -> volume.transform(space::localPosition)).toList();
            OpticalPlane localStart = transformPlane(start, space::localPosition);
            Vec3 localDirection = space.localVector(direction).normalize();
            Vec3 localOrigin = space.localPosition(origin);
            Map<BlockPos, StructureBlockInfo> mountedBlocks = space.blocks();
            AABB localBounds = space.bounds().inflate(LaserBlockClip.NEIGHBOR_REACH + BeamSweepSection.CONTACT_TOLERANCE);
            if (mountedBlocks != null) {
                localBounds = null;
            }
            BeamVolumeCells cells = new BeamVolumeCells(local.stream().map(BeamSweepSection::points).toList(), localBounds);
            ClipContext context = new ClipContext(localOrigin, space.localPosition(origin.add(direction.scale(range))), Block.COLLIDER, Fluid.NONE, collision);
            for (BlockPos cursor : cells) {
                if (candidates >= maxCandidates) {
                    return new Result(0, null, null, Reason.OCCLUSION_LIMIT);
                }
                candidates++;
                BlockPos pos = cursor.immutable();
                PreparedBlock block;
                if (mountedBlocks != null) {
                    block = geometry.getMountedBlock(space.owner(), mountedBlocks, pos, collision);
                }
                else {
                    if (level.isOutsideBuildHeight(pos)) {
                        continue;
                    }

                    if (!geometry.isLoaded(level, pos)) {
                        for (BeamSweepSection volume : local) {
                            if (shapeTests >= maxShapeTests) {
                                return new Result(0, null, null, Reason.OCCLUSION_LIMIT);
                            }
                            shapeTests++;
                            Contact contact = volume.intersect(new AABB(pos), localDirection, null);
                            if (contact == null || contact.distance() > nearest.distance()) {
                                continue;
                            }

                            Vec3 endpoint = beam.advance(contact.distance()).frame().origin();
                            Boundary boundary = new Boundary(contact.distance(), space.transformPosition(contact.point()), new OpticalPlane(endpoint, direction.scale(-1)), false);
                            nearest = new Result(contact.distance(), null, boundary, Reason.UNLOADED);
                            terminal = null;
                        }
                        continue;
                    }

                    block = geometry.getWorldBlock(level, pos, context, partialTick);
                }
                if (block == null) {
                    continue;
                }

                VoxelShape shape = block.shape();
                if (shape.isEmpty()) {
                    continue;
                }

                List<AABB> boxes = block.precise() ? shape.toAabbs() : block.boxes();
                OpticalPlane ignored = localStart;
                Vec3 offset = Vec3.atLowerCornerOf(pos);
                UnaryOperator<Vec3> toBlock = point -> point.subtract(offset);
                UnaryOperator<Vec3> fromBlock = point -> point.add(offset);
                if (shape instanceof AmethystMirrorShape mirror) {
                    toBlock = point -> mirror.toPanelCoordinates(point.subtract(offset));
                    fromBlock = point -> mirror.fromPanelCoordinates(point).add(offset);
                    boxes = mirror.getSurfaceBoxes();
                }
                UnaryOperator<Vec3> transform = toBlock;
                List<BeamSweepSection> parts = local.stream().map(volume -> volume.transform(transform)).toList();
                ignored = transformPlane(ignored, transform);
                Vec3 shapeDirection = transform.apply(localOrigin.add(localDirection)).subtract(transform.apply(localOrigin)).normalize();
                Result blockNearest = null;
                Result opticalEvent = null;
                OpticalGeometry optical = block.optical();
                for (AABB box : boxes) {
                    for (BeamSweepSection volume : parts) {
                        if (shapeTests >= maxShapeTests) {
                            return new Result(0, null, null, Reason.OCCLUSION_LIMIT);
                        }
                        shapeTests++;
                        Contact contact = volume.intersect(box, shapeDirection, ignored);
                        if (contact == null) {
                            continue;
                        }

                        Vec3 point = fromBlock.apply(contact.point());
                        Vec3 normal = fromBlock.apply(contact.point().add(contact.normal())).subtract(point).normalize();
                        BlockHitResult localHit = new BlockHitResult(point, Direction.getNearest(normal.x, normal.y, normal.z), pos, contact.distance() == 0);
                        Result opaque = opaque(beam, space, pos, point, normal, contact.distance());
                        if (blockNearest == null || opaque.distance() < blockNearest.distance()) {
                            blockNearest = opaque;
                        }
                        OpticalSurface surface = new OpticalSurface(normal, null, OpticalInteractor.ABSORBING);
                        if (optical != null) {
                            surface = optical.getSurface(localHit, localDirection);
                        }
                        if (surface.aperture() == null || optical == null) {
                            continue;
                        }

                        Result interaction = admit(beam, range, space, pos, localHit, surface, optical, localDirection, volumes, contact.distance());
                        if (interaction == null) {
                            continue;
                        }

                        if (interaction.reason() != null) {
                            return interaction;
                        }

                        if (opticalEvent == null || interaction.distance() < opticalEvent.distance()) {
                            opticalEvent = interaction;
                        }
                    }
                }
                if (opticalEvent != null) {
                    OpticalPlane opticalPlane = opticalEvent.requireBoundary().cutoffPlane();
                    OpticalPlane localPlane = transformPlane(opticalPlane, value -> transform.apply(space.localPosition(value)));
                    Result frameHit = null;
                    for (BeamSweepSection volume : parts) {
                        BeamSweepSection incoming = volume.clip(localPlane);
                        if (incoming == null) {
                            continue;
                        }

                        for (AABB box : boxes) {
                            if (shapeTests >= maxShapeTests) {
                                return new Result(0, null, null, Reason.OCCLUSION_LIMIT);
                            }
                            shapeTests++;
                            Contact contact = incoming.intersect(box, shapeDirection, localPlane);
                            if (contact == null || frameHit != null && contact.distance() >= frameHit.distance()) {
                                continue;
                            }

                            Vec3 point = fromBlock.apply(contact.point());
                            Vec3 normal = fromBlock.apply(contact.point().add(contact.normal())).subtract(point).normalize();
                            frameHit = opaque(beam, space, pos, point, normal, contact.distance());
                        }
                    }
                    blockNearest = frameHit == null ? opticalEvent : frameHit;
                }
                if (blockNearest == null || blockNearest.distance() > nearest.distance() || blockNearest.distance() == nearest.distance() && nearest.reason() == Reason.UNLOADED) {
                    continue;
                }

                boolean interaction = blockNearest.requireBoundary().opticalInteraction();
                OpticalHit nearestHit = nearest.hit();
                if (nearestHit != null && blockNearest.distance() == nearest.distance()) {
                    boolean nearestInteraction = nearest.requireBoundary().opticalInteraction();
                    if (interaction && !nearestInteraction || interaction == nearestInteraction && pos.compareTo(nearestHit.target().blockPos()) >= 0) {
                        continue;
                    }
                }

                nearest = blockNearest;
                terminal = null;
                if (!interaction && optical != null) {
                    List<AABB> receiving = optical.getReceivingShape().toAabbs();
                    if (!receiving.isEmpty()) {
                        terminal = new TerminalCandidate(space, pos, local, shape.toAabbs(), receiving);
                    }
                }
            }
        }
        if (terminal != null) {
            double receivingDistance = Double.POSITIVE_INFINITY;
            Vec3 localDirection = terminal.space().localVector(direction).normalize();
            for (BeamSweepSection volume : terminal.volumes()) {
                for (AABB box : terminal.receiving()) {
                    if (shapeTests >= maxShapeTests) {
                        return new Result(0, null, null, Reason.OCCLUSION_LIMIT);
                    }
                    shapeTests++;
                    Contact contact = volume.intersect(box.move(terminal.pos()), localDirection, null);
                    if (contact == null) {
                        continue;
                    }

                    receivingDistance = Math.min(receivingDistance, contact.distance());
                }
            }
            if (receivingDistance > nearest.distance() + BeamSweepSection.CONTACT_TOLERANCE) {
                return nearest;
            }

            BeamTerminalQuery receiver = new BeamTerminalQuery(beam, terminal.space(), terminal.pos(), range, start, terminal.obstacles(), terminal.receiving(), maxShapeTests - shapeTests);
            BeamTerminal contact = receiver.resolve();
            shapeTests += receiver.tests();
            if (contact == null) {
                return new Result(0, null, null, Reason.OCCLUSION_LIMIT);
            }

            return new Result(nearest.distance(), nearest.hit(), nearest.boundary(), nearest.reason(), contact);
        }
        return nearest;
    }

    private @Nullable Result admit(BeamState beam, double range, LaserQuerySpace space, BlockPos pos, BlockHitResult firstHit, OpticalSurface firstSurface, OpticalGeometry optical, Vec3 localDirection, List<BeamSweepSection> volumes, double firstDistance) {
        Vec3 point = space.transformPosition(firstHit.getLocation());
        Vec3 normal = transformPlane(new OpticalPlane(firstHit.getLocation(), firstSurface.normal()), space::transformPosition).normal();
        OpticalTarget target = new OpticalTarget(level.dimension(), pos, space.id(), space.inSubLevel());
        OpticalHit planeHit = new OpticalHit(target, firstHit, point, new OpticalSurface(normal, null, firstSurface.interactor()), firstSurface.normal());
        Vertex center = BeamContact.intersect(beam, beam.profile().spans(range), planeHit, beam.footprint().horizontalOffset(), beam.footprint().verticalOffset());
        if (center == null) {
            return null;
        }

        Vec3 position = center.position();
        BlockHitResult localHit = new BlockHitResult(space.localPosition(position), firstHit.getDirection(), pos, false);
        OpticalSurface surface = optical.getSurface(localHit, localDirection);
        OpticalAperture aperture = surface.aperture();
        if (aperture == null || BeamContact.create(beam, range, planeHit) == null) {
            return null;
        }

        OpticalPlane plane = new OpticalPlane(position, normal);
        OpticalPlane reverse = new OpticalPlane(position, normal.scale(-1));
        for (BeamSweepSection volume : volumes) {
            if (shapeTests >= budget.maxShapeTests()) {
                return new Result(0, null, null, Reason.OCCLUSION_LIMIT);
            }
            shapeTests++;
            BeamSweepSection section = volume.clip(plane);
            if (section == null) {
                continue;
            }

            section = section.clip(reverse);
            if (section == null) {
                continue;
            }

            for (Vec3 vertex : section.points()) {
                Vec3 relative = space.localPosition(vertex).subtract(aperture.center());
                if (Math.abs(relative.dot(aperture.horizontal())) > aperture.halfSize() + BeamSweepSection.CONTACT_TOLERANCE || Math.abs(relative.dot(aperture.vertical())) > aperture.halfSize() + BeamSweepSection.CONTACT_TOLERANCE) {
                    return null;
                }
            }
        }
        OpticalSurface transformed = surface.transform(space);
        transformed = new OpticalSurface(normal, transformed.aperture(), transformed.interactor(), transformed.apertureFrame());
        OpticalHit hit = new OpticalHit(target, localHit, position, transformed, surface.normal());
        Boundary boundary = new Boundary(firstDistance, point, plane, true);
        return new Result(center.distance(), hit, boundary, null);
    }

    private Result opaque(BeamState beam, LaserQuerySpace space, BlockPos pos, Vec3 point, Vec3 normal, double distance) {
        Vec3 worldPoint = space.transformPosition(point);
        BlockHitResult localHit = new BlockHitResult(point, Direction.getNearest(normal.x, normal.y, normal.z), pos, distance == 0);
        Vec3 worldNormal = transformPlane(new OpticalPlane(point, normal), space::transformPosition).normal();
        OpticalHit hit = new OpticalHit(new OpticalTarget(level.dimension(), pos, space.id(), space.inSubLevel()), localHit, worldPoint, new OpticalSurface(worldNormal, null, OpticalInteractor.ABSORBING), null);
        Boundary boundary = new Boundary(distance, worldPoint, new OpticalPlane(beam.advance(distance).frame().origin(), beam.frame().direction().scale(-1)), false);
        return new Result(distance, hit, boundary, null);
    }

    private record TerminalCandidate(LaserQuerySpace space, BlockPos pos, List<BeamSweepSection> volumes, List<AABB> obstacles, List<AABB> receiving) {}

    record Result(double distance, @Nullable OpticalHit hit, @Nullable Boundary boundary, @Nullable Reason reason, @Nullable BeamTerminal terminal) {
        Result(double distance, @Nullable OpticalHit hit, @Nullable Boundary boundary, @Nullable Reason reason) {
            this(distance, hit, boundary, reason, null);
        }

        private Boundary requireBoundary() {
            return Objects.requireNonNull(boundary, "A beam collision result must include its boundary.");
        }
    }
}
