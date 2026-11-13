package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.exposure.OpticalExposure;
import net.ty.createcraftedbeginning.content.opticalpower.laser.exposure.OpticalExposureLedger;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserCollisionContext;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserBehaviour;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.joml.Quaternionf;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorReflectionPlanner {
    private static final int MAX_SOURCES = 8;
    private static final double TARGET_TOLERANCE = 0.02;
    private static final double DIRECTION_TOLERANCE_SQR = 1.0E-8;

    private AmethystMirrorReflectionPlanner() {
    }

    static AmethystMirrorAimResult plan(ServerLevel level, AmethystMirrorBlockEntity mirror, BlockPos targetPos, Vec3 target) {
        BlockPos pos = mirror.getBlockPos();
        if (SubLevelBridge.resolve(level, pos).inSubLevel() || SubLevelBridge.resolve(level, targetPos).inSubLevel()) {
            return AmethystMirrorAimResult.failure(AmethystMirrorAimStatus.UNSUPPORTED);
        }

        Set<BlockPos> sources = new HashSet<>();
        Segment selected = null;
        LaserBehaviour selectedSource = null;
        Vec3 incomingDirection = null;
        for (OpticalExposure exposure : OpticalExposureLedger.getPrevious(mirror)) {
            BlockPos sourcePos = exposure.sourcePos();
            if (!sources.add(sourcePos)) {
                continue;
            }

            if (sources.size() > MAX_SOURCES) {
                return AmethystMirrorAimResult.failure(AmethystMirrorAimStatus.MULTIPLE_INPUTS);
            }

            if (!level.isLoaded(sourcePos) || !(level.getBlockEntity(sourcePos) instanceof SmartBlockEntity source) || SubLevelBridge.resolve(level, sourcePos).inSubLevel()) {
                continue;
            }

            LaserBehaviour laser = source.getBehaviour(LaserBehaviour.TYPE);
            if (laser == null) {
                continue;
            }

            BeamPropagation propagation = laser.trace(new LaserCollisionContext(1));
            if (propagation == null) {
                continue;
            }

            for (Segment segment : propagation.segments()) {
                OpticalHit hit = segment.hit();
                double power = segment.incident().fluxLm();
                if (hit == null || power <= 0) {
                    continue;
                }

                OpticalTarget hitTarget = hit.target();
                if (!hitTarget.blockPos().equals(pos) || hitTarget.contraptionId() != null || hitTarget.inSubLevel()) {
                    continue;
                }

                Vec3 direction = segment.start().frame().direction();
                if (incomingDirection != null && direction.distanceToSqr(incomingDirection) > DIRECTION_TOLERANCE_SQR) {
                    return AmethystMirrorAimResult.failure(AmethystMirrorAimStatus.MULTIPLE_INPUTS);
                }

                incomingDirection = direction;
                if (selected != null && power <= selected.incident().fluxLm()) {
                    continue;
                }

                selected = segment;
                selectedSource = laser;
            }
        }
        if (selected == null) {
            return AmethystMirrorAimResult.failure(AmethystMirrorAimStatus.NO_INPUT);
        }

        BeamFrame incoming = selected.start().frame();
        Quaternionf rotation = AmethystMirrorReflectionGeometry.solve(mirror.getStoredRotation(), Vec3.atCenterOf(pos), incoming, target);
        if (rotation == null) {
            return AmethystMirrorAimResult.failure(AmethystMirrorAimStatus.NO_SOLUTION);
        }

        AmethystMirrorOptics optics = new AmethystMirrorOptics(AmethystMirrorGeometry.create(rotation));
        Vec3 origin = incoming.origin();
        Vec3 direction = incoming.direction();
        double remainingRange = selected.remainingRange();
        BlockHitResult contact = optics.getCollisionShape().clip(origin, origin.add(direction.scale(remainingRange)), pos);
        if (contact == null || optics.getSurface(contact, direction).aperture() == null) {
            return AmethystMirrorAimResult.failure(AmethystMirrorAimStatus.MIRROR_FRAME);
        }

        Vec3 reflection = contact.getLocation();
        double distance = origin.distanceTo(reflection) + reflection.distanceTo(target);
        if (distance > remainingRange + TARGET_TOLERANCE) {
            return AmethystMirrorAimResult.failure(AmethystMirrorAimStatus.BEAM_RANGE);
        }

        BeamPropagation predicted = selectedSource.trace(new LaserCollisionContext(1, level, pos, optics));
        if (predicted == null) {
            return AmethystMirrorAimResult.failure(AmethystMirrorAimStatus.NO_INPUT);
        }

        List<Segment> segments = predicted.segments();
        for (Segment segment : segments) {
            OpticalHit hit = segment.hit();
            int parentIndex = segment.parentIndex();
            if (hit == null || parentIndex < 0 || segment.incident().fluxLm() <= 0) {
                continue;
            }

            OpticalTarget hitTarget = hit.target();
            Vec3 hitPoint = hit.position();
            if (!hitTarget.blockPos().equals(targetPos) || hitTarget.contraptionId() != null || hitTarget.inSubLevel()) {
                continue;
            }

            BeamFrame outgoing = segment.start().frame();
            Vec3 normal = hit.surface().normal();
            double incidence = outgoing.direction().dot(normal);
            if (Math.abs(incidence) < 1.0E-12) {
                continue;
            }

            double targetDistance = hitPoint.subtract(outgoing.origin()).dot(normal) / incidence;
            Vec3 aimedPoint = outgoing.origin().add(outgoing.direction().scale(targetDistance));
            if (aimedPoint.distanceTo(target) > TARGET_TOLERANCE) {
                continue;
            }

            Segment parent = segments.get(parentIndex);
            OpticalHit parentHit = parent.hit();
            if (parentHit == null) {
                continue;
            }

            OpticalTarget parentTarget = parentHit.target();
            if (!parentTarget.blockPos().equals(pos) || parentTarget.contraptionId() != null || parentTarget.inSubLevel()) {
                continue;
            }

            return new AmethystMirrorAimResult(AmethystMirrorAimStatus.READY, rotation, parent.start().frame().origin(), parentHit.position(), aimedPoint);
        }
        return AmethystMirrorAimResult.failure(AmethystMirrorAimStatus.BLOCKED);
    }
}
