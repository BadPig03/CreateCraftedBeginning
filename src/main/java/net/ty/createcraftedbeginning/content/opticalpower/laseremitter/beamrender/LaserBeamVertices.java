package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamAppearance;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamBrightness;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.Face;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamWork;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamVertices {
    private static final List<LayerColor> COLORS = List.of(new LayerColor(255, 218, 175, 20), new LayerColor(255, 255, 242, 88), new LayerColor(255, 255, 255, 216));
    private static final Layer EMPTY = new Layer(List.of(), Vec3.ZERO, COLORS.getFirst(), true);

    private final Vec3 origin;
    private final Layer[] layers = new Layer[COLORS.size()];
    private List<Layer> selected = List.of();
    private boolean prepared;

    LaserBeamVertices(Vec3 origin) {
        this.origin = origin;
    }

    static LaserBeamWork<LaserBeamVertices> begin(Vec3 origin, List<List<Face>> surfaces) {
        return new VerticesWork(origin, surfaces);
    }

    boolean matchesOrigin(Vec3 origin) {
        return this.origin.equals(origin);
    }

    List<Layer> select(List<List<Face>> surfaces) {
        List<Face> outer = surfaces.get(0);
        List<Face> middle = surfaces.get(1);
        List<Face> inner = surfaces.get(2);
        if (prepared && selected.get(0).matches(outer) && selected.get(1).matches(middle) && selected.get(2).matches(inner)) {
            return selected;
        }

        selected = List.of(prepareLayer(outer, 0), prepareLayer(middle, 1), prepareLayer(inner, 2));
        prepared = selected.get(0).packed != null && selected.get(1).packed != null && selected.get(2).packed != null;
        return selected;
    }

    private Layer prepareLayer(List<Face> surfaces, int index) {
        if (surfaces.isEmpty()) {
            return EMPTY;
        }

        Layer layer = layers[index];
        if (layer == null || layer.surfaces != surfaces || layer.packed == null) {
            layer = new Layer(surfaces, origin, COLORS.get(index), layer != null && layer.surfaces == surfaces);
            layers[index] = layer;
        }

        return layer;
    }

    static final class Layer {
        private final List<Face> surfaces;
        private final Vec3 origin;
        private final LayerColor color;
        private final @Nullable PackedVertices packed;
        private final AABB bounds;

        private Layer(List<Face> surfaces, Vec3 origin, LayerColor color, boolean prepare) {
            this.surfaces = surfaces;
            this.origin = origin;
            this.color = color;
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            for (Face face : surfaces) {
                for (Vec3 vertex : face.vertices()) {
                    float x = (float) (vertex.x - origin.x);
                    float y = (float) (vertex.y - origin.y);
                    float z = (float) (vertex.z - origin.z);
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    maxZ = Math.max(maxZ, z);
                }
            }

            if (minX == Double.POSITIVE_INFINITY) {
                bounds = new AABB(0, 0, 0, 0, 0, 0);
            }
            else {
                bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
            }

            if (!prepare) {
                packed = null;
                return;
            }

            packed = new PackedVertices(surfaces, origin, color);
        }

        private Layer(List<Face> surfaces, Vec3 origin, LayerColor color, AABB bounds, PackedVertices packed) {
            this.surfaces = surfaces;
            this.origin = origin;
            this.color = color;
            this.bounds = bounds;
            this.packed = packed;
        }

        boolean isEmpty() {
            if (packed == null) {
                return surfaces.isEmpty();
            }

            return packed.colors.length == 0;
        }

        AABB bounds() {
            return bounds;
        }

        void render(VertexConsumer buffer, Matrix4f pose) {
            if (packed == null) {
                renderDirect(buffer, pose);
                return;
            }

            for (int index = 0; index < packed.colors.length; index++) {
                int offset = index * 3;
                int attributeOffset = index * 2;
                buffer.addVertex(pose, packed.positions[offset], packed.positions[offset + 1], packed.positions[offset + 2]).setColor(packed.colors[index]).setUv(packed.brightness[attributeOffset], packed.brightness[attributeOffset + 1]);
            }
        }

        private boolean matches(List<Face> surfaces) {
            return this.surfaces == surfaces || this.surfaces.isEmpty() && surfaces.isEmpty();
        }

        private void renderDirect(VertexConsumer buffer, Matrix4f pose) {
            for (Face face : surfaces) {
                LaserBeamAppearance appearance = face.draw().appearance();
                double opacity = appearance.opacity();
                if (opacity == 0) {
                    continue;
                }

                int red = (int) Math.round(color.red() * appearance.red());
                int green = (int) Math.round(color.green() * appearance.green());
                int blue = (int) Math.round(color.blue() * appearance.blue());
                double alpha = color.alpha();
                LaserBeamBrightness brightness = face.draw().brightness();
                List<Vec3> vertices = face.vertices();
                if (vertices.size() == 4) {
                    for (Vec3 vertex : vertices) {
                        Vec3 local = vertex.subtract(origin);
                        float width = (float) (2 * LaserBeamBrightness.REFERENCE_RADIUS);
                        if (brightness != null) {
                            width = (float) brightness.referenceWidthAt(vertex);
                        }

                        buffer.addVertex(pose, (float) local.x, (float) local.y, (float) local.z).setColor(red, green, blue, (int) Math.round(alpha * face.fadeAt(vertex))).setUv(width, (float) opacity);
                    }

                    continue;
                }

                for (int index = 1; index + 1 < vertices.size(); index++) {
                    Vec3 next = vertices.get(index + 1);
                    for (Vec3 vertex : List.of(vertices.getFirst(), vertices.get(index), next, next)) {
                        Vec3 local = vertex.subtract(origin);
                        float width = (float) (2 * LaserBeamBrightness.REFERENCE_RADIUS);
                        if (brightness != null) {
                            width = (float) brightness.referenceWidthAt(vertex);
                        }

                        buffer.addVertex(pose, (float) local.x, (float) local.y, (float) local.z).setColor(red, green, blue, (int) Math.round(alpha * face.fadeAt(vertex))).setUv(width, (float) opacity);
                    }
                }
            }
        }
    }

    private static final class PackedVertices {
        private final float[] positions;
        private final float[] brightness;
        private final int[] colors;

        private PackedVertices(List<Face> surfaces, Vec3 origin, LayerColor layer) {
            int count = 0;
            for (Face face : surfaces) {
                if (face.draw().appearance().opacity() == 0) {
                    continue;
                }

                int size = face.vertices().size();
                count += size == 4 ? 4 : Math.max(0, size - 2) * 4;
            }

            positions = new float[count * 3];
            brightness = new float[count * 2];
            colors = new int[count];
            int offset = 0;
            for (Face face : surfaces) {
                LaserBeamAppearance appearance = face.draw().appearance();
                double opacity = appearance.opacity();
                if (opacity == 0) {
                    continue;
                }

                int red = (int) Math.round(layer.red() * appearance.red());
                int green = (int) Math.round(layer.green() * appearance.green());
                int blue = (int) Math.round(layer.blue() * appearance.blue());
                double alpha = layer.alpha();
                LaserBeamBrightness field = face.draw().brightness();
                List<Vec3> vertices = face.vertices();
                if (vertices.size() == 4) {
                    for (Vec3 vertex : vertices) {
                        writeVertex(offset++, vertex, face, field, opacity, origin, red, green, blue, alpha);
                    }

                    continue;
                }

                for (int index = 1; index + 1 < vertices.size(); index++) {
                    Vec3 next = vertices.get(index + 1);
                    writeVertex(offset++, vertices.getFirst(), face, field, opacity, origin, red, green, blue, alpha);
                    writeVertex(offset++, vertices.get(index), face, field, opacity, origin, red, green, blue, alpha);
                    writeVertex(offset++, next, face, field, opacity, origin, red, green, blue, alpha);
                    writeVertex(offset++, next, face, field, opacity, origin, red, green, blue, alpha);
                }
            }
        }

        private PackedVertices(int count) {
            positions = new float[count * 3];
            brightness = new float[count * 2];
            colors = new int[count];
        }

        private void writeVertex(int index, Vec3 vertex, Face face, @Nullable LaserBeamBrightness field, double opacity, Vec3 origin, int red, int green, int blue, double alpha) {
            Vec3 local = vertex.subtract(origin);
            int offset = index * 3;
            positions[offset] = (float) local.x;
            positions[offset + 1] = (float) local.y;
            positions[offset + 2] = (float) local.z;
            colors[index] = ARGB32.color((int) Math.round(alpha * face.fadeAt(vertex)), red, green, blue);
            float width = (float) (2 * LaserBeamBrightness.REFERENCE_RADIUS);
            if (field != null) {
                width = (float) field.referenceWidthAt(vertex);
            }

            brightness[index * 2] = width;
            brightness[index * 2 + 1] = (float) opacity;
        }
    }

    private static final class VerticesWork implements LaserBeamWork<LaserBeamVertices> {
        private final LaserBeamVertices vertices;
        private final List<List<Face>> surfaces;
        private int layer;
        private int phase;
        private int face;
        private int vertex;
        private int count;
        private int offset;
        private int red;
        private int green;
        private int blue;
        private double opacity;
        private @Nullable LaserBeamBrightness field;
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;
        private double maxZ = Double.NEGATIVE_INFINITY;
        private @Nullable AABB bounds;
        private @Nullable PackedVertices packed;
        private @Nullable LaserBeamVertices result;

        private VerticesWork(Vec3 origin, List<List<Face>> surfaces) {
            vertices = new LaserBeamVertices(origin);
            this.surfaces = surfaces;
        }

        @Override
        public boolean step() {
            if (result != null) {
                return true;
            }

            if (layer >= COLORS.size()) {
                Layer first = vertices.layers[0] == null ? EMPTY : vertices.layers[0];
                Layer second = vertices.layers[1] == null ? EMPTY : vertices.layers[1];
                Layer third = vertices.layers[2] == null ? EMPTY : vertices.layers[2];
                vertices.selected = List.of(first, second, third);
                vertices.prepared = true;
                result = vertices;
                return true;
            }

            List<Face> current = surfaces.get(layer);
            if (current.isEmpty()) {
                layer++;
                return false;
            }

            if (phase == 0) {
                if (face < current.size()) {
                    List<Vec3> points = current.get(face).vertices();
                    if (vertex < points.size()) {
                        Vec3 point = points.get(vertex++);
                        float x = (float) (point.x - vertices.origin.x);
                        float y = (float) (point.y - vertices.origin.y);
                        float z = (float) (point.z - vertices.origin.z);
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        minZ = Math.min(minZ, z);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                        maxZ = Math.max(maxZ, z);
                    }
                    else {
                        vertex = 0;
                        face++;
                    }

                    return false;
                }

                if (minX == Double.POSITIVE_INFINITY) {
                    bounds = new AABB(0, 0, 0, 0, 0, 0);
                }
                else {
                    bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
                }

                face = 0;
                phase = 1;
                return false;
            }

            if (phase == 1) {
                if (face < current.size()) {
                    Face item = current.get(face++);
                    if (item.draw().appearance().opacity() != 0) {
                        int size = item.vertices().size();
                        count += size == 4 ? 4 : Math.max(0, size - 2) * 4;
                    }

                    return false;
                }

                phase = 2;
                return false;
            }

            if (phase == 2) {
                packed = new PackedVertices(count);
                face = 0;
                vertex = 0;
                offset = 0;
                phase = 3;
                return false;
            }

            if (face < current.size()) {
                Face item = current.get(face);
                LaserBeamAppearance appearance = item.draw().appearance();
                if (vertex == 0) {
                    LayerColor color = COLORS.get(layer);
                    opacity = appearance.opacity();
                    red = (int) Math.round(color.red() * appearance.red());
                    green = (int) Math.round(color.green() * appearance.green());
                    blue = (int) Math.round(color.blue() * appearance.blue());
                    field = item.draw().brightness();
                }

                List<Vec3> points = item.vertices();
                int emitted = points.size() == 4 ? 4 : Math.max(0, points.size() - 2) * 4;
                if (opacity == 0 || vertex >= emitted) {
                    face++;
                    vertex = 0;
                    return false;
                }

                int index = vertex;
                if (points.size() != 4) {
                    int corner = vertex % 4;
                    int triangle = vertex / 4 + 1;
                    if (corner == 0) {
                        index = 0;
                    }
                    else if (corner == 1) {
                        index = triangle;
                    }
                    else {
                        index = triangle + 1;
                    }
                }

                Objects.requireNonNull(packed, "Packing laser beam vertices requires an allocated vertex buffer.").writeVertex(offset++, points.get(index), item, field, opacity, vertices.origin, red, green, blue, COLORS.get(layer).alpha());
                vertex++;
                return false;
            }

            vertices.layers[layer] = new Layer(current, vertices.origin, COLORS.get(layer), Objects.requireNonNull(bounds, "Finalizing a laser beam layer requires bounds."), Objects.requireNonNull(packed, "Finalizing a laser beam layer requires packed vertices."));
            layer++;
            phase = 0;
            face = 0;
            vertex = 0;
            count = 0;
            minX = Double.POSITIVE_INFINITY;
            minY = Double.POSITIVE_INFINITY;
            minZ = Double.POSITIVE_INFINITY;
            maxX = Double.NEGATIVE_INFINITY;
            maxY = Double.NEGATIVE_INFINITY;
            maxZ = Double.NEGATIVE_INFINITY;
            bounds = null;
            packed = null;
            return false;
        }

        @Override
        public LaserBeamVertices result() {
            return Objects.requireNonNull(result, "Vertex preparation has not completed.");
        }
    }

    private record LayerColor(int red, int green, int blue, int alpha) {}
}
