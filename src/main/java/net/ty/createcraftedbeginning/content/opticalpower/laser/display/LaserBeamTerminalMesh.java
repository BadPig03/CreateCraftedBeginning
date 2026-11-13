package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile.Span;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamGeometry.Section;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Patch;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Vertex;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamTerminal;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamTerminal.Face;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamTerminalMesh {
    private static final double EPSILON = 1.0E-8;

    private LaserBeamTerminalMesh() {}

    static List<DrawSection> create(Segment segment, int index, double pathDistance, BeamRegion domain, LaserBeamFade fade) {
        BeamTerminal terminal = Objects.requireNonNull(segment.terminal(), "Terminal display requires a captured receiving footprint.");
        BeamState beam = segment.start();
        BeamFrame frame = beam.frame();
        BeamProfile profile = beam.profile();
        BeamFootprint footprint = beam.footprint();
        List<Span> spans = profile.spans(segment.remainingRange());
        Vec3 relativeOrigin = terminal.worldOrigin().subtract(frame.origin());
        Vec3 x = terminal.axisX();
        Vec3 y = terminal.axisY();
        Vec3 z = terminal.axisZ();
        List<DrawSection> result = new ArrayList<>();
        for (Face face : terminal.faces()) {
            Vec3 localNormal = Vec3.atLowerCornerOf(face.direction().getNormal());
            Vec3 normal = y.cross(z).scale(localNormal.x).add(z.cross(x).scale(localNormal.y)).add(x.cross(y).scale(localNormal.z)).normalize();
            for (Patch patch : face.patches()) {
                List<Point> projected = new ArrayList<>();
                for (Vec3 vertex : patch.vertices()) {
                    Vec3 delta = relativeOrigin.add(x.scale(vertex.x)).add(y.scale(vertex.y)).add(z.scale(vertex.z));
                    double distance = Math.max(0, delta.dot(frame.direction()));
                    double radius = profile.advance(distance).radius();
                    projected.add(new Point((delta.dot(frame.transverseAxis()) + profile.radius() * footprint.horizontalOffset()) / radius, (delta.dot(frame.getSecondTransverseAxis()) + profile.radius() * footprint.verticalOffset()) / radius));
                }

                BeamRegion region = new BeamRegion(projected);
                List<Point> limits = domain.vertices();
                for (int edge = 0; edge < limits.size(); edge++) {
                    Point first = limits.get(edge);
                    Point second = limits.get((edge + 1) % limits.size());
                    double u = second.v() - first.v();
                    double v = first.u() - second.u();
                    region = region.clip(u, v, -u * first.u() - v * first.v());
                }
                List<Point> boundary = region.vertices();
                if (boundary.size() < 3 || region.area() <= EPSILON * EPSILON) {
                    continue;
                }

                OpticalPlane plane = new OpticalPlane(terminal.worldPoint(patch.vertices().getFirst()), normal);
                List<Vertex> contacts = new ArrayList<>();
                double end = segment.length();
                for (Point point : boundary) {
                    Vertex contact = BeamContact.intersect(beam, spans, plane, point.u(), point.v());
                    if (contact == null) {
                        throw new IllegalStateException("Captured terminal footprint has no physical display intersection at segment " + index + '.');
                    }

                    contacts.add(contact);
                    end = Math.max(end, contact.distance());
                }
                double cutoff = segment.length();
                if (end <= cutoff + LaserBeamSurfaces.EPSILON) {
                    continue;
                }

                List<Double> samples = new ArrayList<>();
                samples.add(cutoff);
                for (Span span : profile.spans(end)) {
                    double distance = span.to();
                    if (distance > cutoff + EPSILON) {
                        samples.add(distance);
                    }
                }
                List<Vec3> starts = boundary.stream().map(point -> BeamContact.point(beam, cutoff, point.u(), point.v())).toList();
                for (int step = 1; step < samples.size(); step++) {
                    double to = samples.get(step);
                    List<Vec3> ends = new ArrayList<>();
                    for (int corner = 0; corner < boundary.size(); corner++) {
                        Vertex contact = contacts.get(corner);
                        Point point = boundary.get(corner);
                        ends.add(to >= contact.distance() - EPSILON ? contact.position() : BeamContact.point(beam, to, point.u(), point.v()));
                    }

                    Section geometry = new Section(starts, ends, ends.stream().anyMatch(point -> fade.at(point) < 1), true);
                    result.add(new DrawSection(geometry, LaserBeamAppearance.fromBeam(beam), index, pathDistance + samples.get(step - 1), fade, LaserBeamMesh.sampling(region), false, LaserBeamBrightness.fromBeam(beam)));
                    starts = ends;
                }
            }
        }
        return List.copyOf(result);
    }
}
