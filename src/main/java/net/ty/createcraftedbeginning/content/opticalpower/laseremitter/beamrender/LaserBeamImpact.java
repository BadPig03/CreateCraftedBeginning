package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamBrightness;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplaySegment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender.LaserBeamVertices.Layer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamImpact {
    private static final double SURFACE_OFFSET = 0.002;
    private static final int SPOT_SIDES = 8;
    private static final double CAP_GAIN = 1.6;
    private static final double SPOT_GAIN = 2.5;
    private static final List<Style> STYLES = List.of(new Style(1.04, 72, 218, 175, 1.4, 100), new Style(1, 142, 255, 242, 0.9, 190), new Style(0.85, 220, 255, 255, 0.45, 255));
    private final @Nullable Contact contact;
    private final List<Layer> layers;
    private List<Layer> body = List.of();
    private List<Layer> attached = List.of();

    private LaserBeamImpact(@Nullable Contact contact, List<Layer> layers) {
        this.contact = contact;
        this.layers = layers;
    }

    static LaserBeamImpact create(LaserBeamContext context, List<DrawSection> sections, Vec3 origin) {
        List<DisplaySegment> segments = context.segments();
        if (segments.isEmpty() || sections.isEmpty()) {
            return new LaserBeamImpact(null, List.of());
        }

        int index = segments.size() - 1;
        DisplaySegment segment = segments.get(index);
        OpticalPlane impact = segment.impact();
        if (impact == null) {
            return new LaserBeamImpact(null, List.of());
        }

        DrawSection draw = sections.getLast();
        List<Vec3> ends = draw.geometry().ends();
        if (draw.segmentIndex() != index || ends.size() != 4) {
            return new LaserBeamImpact(null, List.of());
        }

        Vec3 center = ends.getFirst().lerp(ends.get(2), 0.5);
        Vec3 direction = context.toLocalDirection(segment.start().frame().direction()).normalize();
        Vec3 normal = context.toLocalDirection(impact.normal()).normalize();
        Vec3 point = context.toLocalPosition(impact.point());
        double width = Math.min(ends.getFirst().distanceTo(ends.get(1)), ends.get(1).distanceTo(ends.get(2)));
        if (width <= 1.0E-6) {
            return new LaserBeamImpact(null, List.of());
        }

        double opacity = draw.opacityAt(center) * draw.fadeAt(center);
        Contact contact = new Contact(point, normal, direction, width, opacity, draw);
        double referenceWidth = width;
        LaserBeamBrightness brightness = draw.brightness();
        if (brightness != null) {
            referenceWidth = brightness.referenceWidthAt(center);
        }
        Vec3 offset = direction.scale(-SURFACE_OFFSET);
        Vec3 tangent = normal.cross(new Vec3(0, 1, 0));
        if (tangent.lengthSqr() < 0.01) {
            tangent = normal.cross(new Vec3(1, 0, 0));
        }
        tangent = tangent.normalize();
        Vec3 bitangent = normal.cross(tangent);
        Vec3 spotCenter = point.add(normal.scale(SURFACE_OFFSET * 2));
        List<Layer> layers = new ArrayList<>();
        for (Style style : STYLES) {
            double scale = style.capScale();
            int alpha = style.capAlpha();
            int red = (int) Math.round(255 * draw.appearance().red());
            int green = (int) Math.round(style.green() * draw.appearance().green());
            int blue = (int) Math.round(style.blue() * draw.appearance().blue());
            int bright = ARGB32.color((int) Math.round(alpha * draw.fadeAt(center)), red, green, blue);
            int clear = ARGB32.color(0, red, green, blue);
            float exposure = (float) draw.appearance().opacity();
            float capWidth = (float) (referenceWidth / CAP_GAIN);
            List<Vertex> inner = new ArrayList<>();
            List<Vertex> outer = new ArrayList<>();
            for (Vec3 end : ends) {
                Vec3 radial = end.subtract(center);
                inner.add(new Vertex(center.add(radial.scale(scale * 0.65)).add(offset), bright, capWidth, exposure));
                outer.add(new Vertex(center.add(radial.scale(scale)).add(offset), clear, capWidth, exposure));
            }
            List<Vertex> vertices = new ArrayList<>(inner);
            for (int edge = 0; edge < 4; edge++) {
                int next = (edge + 1) % 4;
                vertices.addAll(List.of(inner.get(edge), outer.get(edge), outer.get(next), inner.get(next)));
            }
            double radius = Math.clamp(width * 0.65, 0.1, 0.24) * style.spotScale();
            int spotColor = ARGB32.color((int) Math.round(style.spotAlpha() * draw.fadeAt(center)), red, green, blue);
            float spotWidth = (float) (referenceWidth / SPOT_GAIN);
            Vertex spot = new Vertex(spotCenter, spotColor, spotWidth, exposure);
            for (int side = 0; side < SPOT_SIDES; side++) {
                double from = side * Math.TAU / SPOT_SIDES;
                double to = (side + 1) * Math.TAU / SPOT_SIDES;
                Vertex first = new Vertex(spotCenter.add(tangent.scale(Math.cos(from) * radius)).add(bitangent.scale(Math.sin(from) * radius)), clear, spotWidth, exposure);
                Vertex second = new Vertex(spotCenter.add(tangent.scale(Math.cos(to) * radius)).add(bitangent.scale(Math.sin(to) * radius)), clear, spotWidth, exposure);
                vertices.addAll(List.of(spot, first, second, second));
            }
            layers.add(Layer.impact(vertices, origin));
        }
        return new LaserBeamImpact(contact, List.copyOf(layers));
    }

    @Nullable Contact contact() {
        return contact;
    }

    List<Layer> attach(List<Layer> body) {
        if (layers.isEmpty()) {
            return body;
        }
        if (this.body == body) {
            return attached;
        }

        List<Layer> result = new ArrayList<>(3);
        for (int index = 0; index < body.size(); index++) {
            Layer layer = body.get(index);
            result.add(layer.isEmpty() ? layer : layer.withAttachment(layers.get(index)));
        }
        this.body = body;
        attached = List.copyOf(result);
        return attached;
    }

    private record Style(double capScale, int capAlpha, int green, int blue, double spotScale, int spotAlpha) {}

    record Contact(Vec3 point, Vec3 normal, Vec3 direction, double width, double opacity, DrawSection draw) {}

    record Vertex(Vec3 position, int color, float width, float opacity) {}
}
