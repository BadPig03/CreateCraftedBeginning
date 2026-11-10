package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalApertureFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalAperture;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamOcclusionSolver.Cell;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamPropagationSolver.Trace;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BeamAperturePartition {
    private static final double ALIGNMENT_TOLERANCE = 1.0E-10;
    private static final double EDGE_TOLERANCE = 1.0E-7;

    private BeamAperturePartition() {
    }

    static @Nullable Cut find(BeamState source, Cell cell, List<Trace> samples) {
        List<Segment> centerSegments = samples.getFirst().segments();
        if (!centerSegments.isEmpty() && isAbsorbed(centerSegments.getLast())) {
            return null;
        }

        BeamFootprint original = source.footprint();
        Cut best = null;
        double bestBalance = 0;
        for (Trace sample : samples) {
            List<Segment> segments = sample.segments();
            int verticalSign = 1;
            for (Segment segment : segments) {
                BeamState beam = segment.start();
                BeamFrame frame = beam.frame();
                Vec3 direction = frame.direction();
                int parent = segment.parentIndex();
                if (parent >= 0) {
                    Segment previous = segments.get(parent);
                    OpticalHit contact = previous.hit();
                    if (contact != null) {
                        Vec3 normal = contact.surface().normal();
                        if (previous.incident().frame().direction().dot(normal) * direction.dot(normal) < 0) {
                            verticalSign = -verticalSign;
                        }
                    }
                }
                OpticalHit hit = segment.hit();
                if (hit == null) {
                    continue;
                }

                OpticalApertureFrame apertureFrame = hit.surface().apertureFrame();
                if (apertureFrame == null) {
                    continue;
                }

                for (OpticalAperture boundary : apertureFrame.boundaries()) {
                    Vec3 boundaryX = boundary.horizontal();
                    Vec3 boundaryY = boundary.vertical();
                    Vec3 boundaryCenter = boundary.center();
                    Vec3 normal = boundaryX.cross(boundaryY);
                    double incidence = direction.dot(normal);
                    if (Math.abs(Math.abs(incidence) - 1) > ALIGNMENT_TOLERANCE) {
                        continue;
                    }

                    double distance = boundaryCenter.subtract(frame.origin()).dot(normal) / incidence;
                    if (distance < 0 || distance > segment.length() + EDGE_TOLERANCE) {
                        continue;
                    }

                    BeamState atPlane = beam.advance(distance);
                    Vec3 center = atPlane.frame().origin();
                    double radius = atPlane.profile().radius();
                    BeamFootprint footprint = beam.footprint();
                    for (boolean horizontal : new boolean[]{true, false}) {
                        Vec3 axis = frame.getSecondTransverseAxis();
                        double scale = original.verticalScale();
                        double offset = original.verticalOffset();
                        double sampleOffset = footprint.verticalOffset();
                        double low = cell.bottom();
                        double high = cell.top();
                        int sign = verticalSign;
                        if (horizontal) {
                            axis = frame.transverseAxis();
                            scale = original.horizontalScale();
                            offset = original.horizontalOffset();
                            sampleOffset = footprint.horizontalOffset();
                            low = cell.left();
                            high = cell.right();
                            sign = 1;
                        }
                        double alignment = Math.max(Math.abs(axis.dot(boundaryX)), Math.abs(axis.dot(boundaryY)));
                        if (1 - alignment > ALIGNMENT_TOLERANCE) {
                            continue;
                        }

                        double planeOffset = boundaryCenter.subtract(center).dot(axis);
                        for (int side : new int[]{-1, 1}) {
                            double local = sampleOffset + (planeOffset + side * boundary.halfSize()) / radius;
                            double coordinate = (local * sign - offset) / scale;
                            if (coordinate <= low + EDGE_TOLERANCE || coordinate >= high - EDGE_TOLERANCE) {
                                continue;
                            }

                            double span = high - low;
                            double balance = (coordinate - low) * (high - coordinate) / (span * span);
                            if (balance <= bestBalance) {
                                continue;
                            }

                            best = new Cut(horizontal, coordinate);
                            bestBalance = balance;
                        }
                    }
                }
            }
        }
        return best;
    }

    static boolean isAbsorbed(Segment segment) {
        OpticalHit hit = segment.hit();
        if (hit == null || segment.absorbedFluxLm() != segment.incident().fluxLm()) {
            return false;
        }

        OpticalSurface surface = hit.surface();
        OpticalApertureFrame frame = surface.apertureFrame();
        if (frame == null || surface.aperture() != null) {
            return false;
        }

        OpticalAperture opening = frame.opening();
        Vec3 openingCenter = opening.center();
        Vec3 horizontal = opening.horizontal();
        Vec3 vertical = opening.vertical();
        Vec3 normal = horizontal.cross(vertical);
        BeamState beam = segment.start();
        BeamFrame start = beam.frame();
        double incidence = start.direction().dot(normal);
        if (Math.abs(Math.abs(incidence) - 1) > ALIGNMENT_TOLERANCE) {
            return false;
        }

        double distance = openingCenter.subtract(start.origin()).dot(normal) / incidence;
        if (distance < 0 || distance > segment.remainingRange()) {
            return false;
        }

        BeamState atPlane = beam.advance(distance);
        Vec3 center = atPlane.frame().origin().subtract(openingCenter);
        double radius = atPlane.profile().radius();
        BeamFootprint footprint = beam.footprint();
        Vec3 xAxis = start.transverseAxis();
        Vec3 yAxis = start.getSecondTransverseAxis();
        double scaleX = footprint.horizontalScale();
        double scaleY = footprint.verticalScale();
        double halfX = radius * (Math.abs(xAxis.dot(horizontal)) * scaleX + Math.abs(yAxis.dot(horizontal)) * scaleY);
        double halfY = radius * (Math.abs(xAxis.dot(vertical)) * scaleX + Math.abs(yAxis.dot(vertical)) * scaleY);
        double x = Math.abs(center.dot(horizontal));
        double y = Math.abs(center.dot(vertical));
        double outer = frame.outerHalfSize();
        if (x + halfX > outer + EDGE_TOLERANCE || y + halfY > outer + EDGE_TOLERANCE) {
            return false;
        }

        double inner = opening.halfSize();
        return x - halfX >= inner - EDGE_TOLERANCE || y - halfY >= inner - EDGE_TOLERANCE;
    }

    record Cut(boolean horizontal, double coordinate) {}
}
