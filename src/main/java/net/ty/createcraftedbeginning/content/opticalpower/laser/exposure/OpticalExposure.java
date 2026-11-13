package net.ty.createcraftedbeginning.content.opticalpower.laser.exposure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Density;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Patch;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamTerminal;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RayProjection;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record OpticalExposure(long gameTime, BlockPos sourcePos, int segmentIndex, OpticalHit hit, BeamState incident, double absorbedFluxLm, BeamFrame localFrame, List<Patch> contactPatches, @Nullable BeamTerminal terminal) {
    public OpticalExposure(long gameTime, BlockPos sourcePos, int segmentIndex, OpticalHit hit, BeamState incident, double absorbedFluxLm, BeamFrame localFrame, List<Patch> contactPatches) {
        this(gameTime, sourcePos, segmentIndex, hit, incident, absorbedFluxLm, localFrame, contactPatches, null);
    }

    public OpticalExposure {
        sourcePos = sourcePos.immutable();
        Objects.requireNonNull(hit, "Optical exposure hit must not be null.");
        Objects.requireNonNull(incident, "Optical exposure requires an incident beam state.");
        Objects.requireNonNull(localFrame, "Optical exposure requires a local beam frame.");
        contactPatches = List.copyOf(contactPatches);
        if (!Double.isFinite(absorbedFluxLm) || absorbedFluxLm < 0) {
            throw new IllegalArgumentException("Absorbed exposure power must be finite and non-negative; got " + absorbedFluxLm + " lm.");
        }

        absorbedFluxLm = Math.max(0, absorbedFluxLm);
        if (segmentIndex < 0) {
            throw new IllegalArgumentException("Exposure segment index must be non-negative; got " + segmentIndex + '.');
        }
    }

    public static @Unmodifiable List<OpticalExposure> fromPropagation(Level level, long gameTime, BlockPos sourcePos, BeamPropagation propagation) {
        List<OpticalExposure> exposures = new ArrayList<>();
        List<Segment> segments = propagation.segments();
        for (int index = 0; index < segments.size(); index++) {
            Segment segment = segments.get(index);
            OpticalHit hit = segment.hit();
            BeamState incident = segment.incident();
            if (hit == null || incident.fluxLm() == 0) {
                continue;
            }

            BlockHitResult localHit = hit.localHit();
            OpticalTarget target = hit.target();
            Vec3 anchor = Vec3.atLowerCornerOf(localHit.getBlockPos());
            BeamFrame frame = incident.frame();
            Vec3 direction = frame.direction();
            Vec3 transverse = frame.transverseAxis();
            if (target.inSubLevel() && target.contraptionId() == null) {
                double[] localDirection = new double[3];
                double[] localTransverse = new double[3];
                for (Axis axis : Axis.values()) {
                    Vec3 offset = Vec3.atLowerCornerOf(Direction.get(AxisDirection.POSITIVE, axis).getNormal());
                    RayProjection ray = SubLevelBridge.projectRay(level, anchor, anchor.add(offset));
                    Vec3 worldAxis = ray.worldEnd().subtract(ray.worldStart()).normalize();
                    int component = axis.ordinal();
                    localDirection[component] = direction.dot(worldAxis);
                    localTransverse[component] = transverse.dot(worldAxis);
                }
                direction = new Vec3(localDirection[0], localDirection[1], localDirection[2]);
                transverse = new Vec3(localTransverse[0], localTransverse[1], localTransverse[2]);
            }
            BeamFrame localFrame = new BeamFrame(localHit.getLocation().subtract(anchor), direction, transverse);
            if (segment.terminal() != null) {
                exposures.add(new OpticalExposure(gameTime, sourcePos, index, hit, incident, segment.absorbedFluxLm(), localFrame, List.of(), segment.terminal()));
                continue;
            }

            BeamContact contact = segment.contact();
            BeamState start = segment.start();
            if (segment.boundary() != null && !segment.boundary().opticalInteraction()) {
                continue;
            }

            if (contact == null && start.profile().widthSlope() != 0) {
                contact = BeamContact.create(start, segment.remainingRange(), hit);
                if (contact == null) {
                    throw new IllegalStateException("Varying exposure has unresolved contact geometry at " + target + '.');
                }
            }
            Vec3 localOrigin = localFrame.origin();
            List<Patch> localPatches = new ArrayList<>();
            if (contact != null) {
                for (Patch patch : contact.patches()) {
                    List<Vec3> vertices = new ArrayList<>();
                    for (Vec3 delta : patch.vertices()) {
                        vertices.add(localOrigin.add(transform(delta, frame, localFrame)));
                    }
                    Density density = patch.density();
                    if (density != null) {
                        Vec3 focus = localOrigin.add(transform(density.focus(), frame, localFrame));
                        density = new Density(focus, transform(density.direction(), frame, localFrame), transform(density.transverse(), frame, localFrame), density.slope(), density.fluxPerSourceArea());
                    }
                    localPatches.add(new Patch(vertices, patch.fluxFraction(), density));
                }
            }
            exposures.add(new OpticalExposure(gameTime, sourcePos, index, hit, incident, segment.absorbedFluxLm(), localFrame, localPatches));
        }
        return List.copyOf(exposures);
    }

    private static Vec3 transform(Vec3 vector, BeamFrame from, BeamFrame to) {
        return to.direction().scale(vector.dot(from.direction())).add(to.transverseAxis().scale(vector.dot(from.transverseAxis()))).add(to.getSecondTransverseAxis().scale(vector.dot(from.getSecondTransverseAxis())));
    }

    public double receivedFluxLm() {
        if (terminal == null) {
            return absorbedFluxLm;
        }

        return absorbedFluxLm * terminal.receivedFraction();
    }

    public double dissipatedFluxLm() {
        return absorbedFluxLm - receivedFluxLm();
    }

}
