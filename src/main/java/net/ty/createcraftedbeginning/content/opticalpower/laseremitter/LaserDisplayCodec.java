package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import io.netty.buffer.Unpooled;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalApertureFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamAppearance;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamBrightness;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplayFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplaySegment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplayState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.Base;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.MappedFamily;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.Mapping;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.MappingState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.Tile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamFade;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamGeometry.Section;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.MappedData;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.SamplingDomain;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamPathSignature.Event;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamPathSignature.Input;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamPathSignature.Path;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamPathSignature.Target;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.Face;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.SurfaceReason;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalAperture;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRigidDisplay;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserStaticDependencies;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserStaticDependencies.Sample;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserDisplayCodec {
    static final int FORMAT = 2;
    static final int QUALITY = 2;
    static final int MAX_PACKET_BYTES = 900000;
    private static final int MAX_RAW_BYTES = 8388608;
    private static final int MAX_ELEMENTS = 65536;
    private static final int MAX_MAPPING_REASON_CHARS = 4096;
    private static final int MAX_INTERACTOR_NAME_CHARS = 1024;

    private LaserDisplayCodec() {}

    static byte @Nullable [] encode(LaserDisplaySnapshot snapshot) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer(4096, MAX_RAW_BYTES));
        try {
            snapshot(buffer, snapshot);
            byte[] raw = new byte[buffer.readableBytes()];
            buffer.readBytes(raw);
            Deflater deflater = new Deflater(Deflater.BEST_SPEED);
            try {
                deflater.setInput(raw);
                deflater.finish();
                byte[] output = new byte[MAX_PACKET_BYTES - Integer.BYTES];
                int length = deflater.deflate(output);
                if (!deflater.finished()) {
                    return null;
                }

                byte[] result = new byte[length + Integer.BYTES];
                FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.wrappedBuffer(result));
                encoded.clear();
                encoded.writeInt(raw.length);
                encoded.writeBytes(output, 0, length);
                encoded.release();
                return result;
            }
            finally {
                deflater.end();
            }
        }
        catch (CapacityExceeded exceeded) {
            return null;
        }
        catch (IndexOutOfBoundsException exceeded) {
            if (buffer.capacity() != MAX_RAW_BYTES) {
                throw exceeded;
            }

            return null;
        }
        finally {
            buffer.release();
        }
    }

    static LaserDisplaySnapshot decode(byte[] encoded) {
        if (encoded.length < Integer.BYTES || encoded.length > MAX_PACKET_BYTES) {
            throw new IllegalArgumentException("Laser display packet length must be in [" + Integer.BYTES + ", " + MAX_PACKET_BYTES + "] bytes; got " + encoded.length + " bytes.");
        }

        FriendlyByteBuf header = new FriendlyByteBuf(Unpooled.wrappedBuffer(encoded));
        int size = header.readInt();
        header.release();
        if (size < 0 || size > MAX_RAW_BYTES) {
            throw new IllegalArgumentException("Decoded laser display length must be in [0, " + MAX_RAW_BYTES + "] bytes; got " + size + " bytes.");
        }

        byte[] raw = new byte[size];
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(encoded, Integer.BYTES, encoded.length - Integer.BYTES);
            int length = inflater.inflate(raw);
            if (length != size || !inflater.finished() || inflater.getRemaining() != 0) {
                throw new IllegalArgumentException("Laser display input must inflate to " + size + " bytes with no compressed bytes left; got " + length + " bytes, finished=" + inflater.finished() + ", remaining=" + inflater.getRemaining() + " bytes.");
            }
        }
        catch (DataFormatException invalid) {
            throw new IllegalArgumentException("Failed to decode compressed laser display input.", invalid);
        }
        finally {
            inflater.end();
        }
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(raw));
        try {
            LaserDisplaySnapshot snapshot = snapshot(buffer);
            if (buffer.isReadable()) {
                throw new IllegalArgumentException("Display input has trailing bytes.");
            }

            validate(snapshot);
            return snapshot;
        }
        finally {
            buffer.release();
        }
    }

    private static void validate(LaserDisplaySnapshot snapshot) {
        LaserBeamDisplayInput input = snapshot.input();
        LaserBeamContext context = input.context();
        List<DisplaySegment> displaySegments = context.segments();
        int segments = displaySegments.size();
        int range = snapshot.range();
        double fluxLm = snapshot.fluxLm();
        double radius = context.sourceRadius();
        if (range < LaserBehaviour.MIN_RANGE || range > LaserBehaviour.MAX_RANGE || fluxLm <= 0 || radius <= 0) {
            throw new IllegalArgumentException("Display range must be in [" + LaserBehaviour.MIN_RANGE + ", " + LaserBehaviour.MAX_RANGE + "] blocks, luminous flux and source radius must be positive; got range=" + range + " blocks, flux=" + fluxLm + " lm, radius=" + radius + " blocks.");
        }

        LaserRigidDisplay rigid = snapshot.rigid();
        Vec3 xAxis = context.xAxis();
        Vec3 yAxis = context.yAxis();
        Vec3 zAxis = context.zAxis();
        Vec3 localOrigin = context.localOrigin();
        Vec3 worldOrigin = context.worldOrigin();
        if (rigid == null && (!xAxis.equals(new Vec3(1, 0, 0)) || !yAxis.equals(new Vec3(0, 1, 0)) || !zAxis.equals(new Vec3(0, 0, 1)) || !localOrigin.equals(worldOrigin))) {
            throw new IllegalArgumentException("Static display axes must be identity and local/world origins must match; got x=" + xAxis + ", y=" + yAxis + ", z=" + zAxis + ", localOrigin=" + localOrigin + ", worldOrigin=" + worldOrigin + '.');
        }

        Mapping mapping = input.mapping();
        List<MappedFamily> families = mapping.families();
        if (rigid != null) {
            MappingState mappingState = mapping.state();
            if (xAxis.lengthSqr() == 0 || yAxis.lengthSqr() == 0 || zAxis.lengthSqr() == 0 || xAxis.dot(yAxis.cross(zAxis)) <= 0 || mappingState != MappingState.UNSUPPORTED || !families.isEmpty()) {
                throw new IllegalArgumentException("Rigid display axes must be nonzero and right-handed, with unsupported mapping and no mapped families; got x=" + xAxis + ", y=" + yAxis + ", z=" + zAxis + ", mapping=" + mappingState + ", families=" + families.size() + '.');
            }
        }

        Input clipping = snapshot.clipping();
        int paths = clipping.paths().size();
        int targets = clipping.targets().size();
        if (paths != segments || targets != segments) {
            throw new IllegalArgumentException("Display clipping paths and targets must each contain " + segments + " entries; got paths=" + paths + ", targets=" + targets + '.');
        }

        for (int index = 0; index < segments; index++) {
            DisplaySegment segment = displaySegments.get(index);
            int parent = segment.parentIndex();
            double length = segment.length();
            double remainingRange = segment.remainingRange();
            Vec3 direction = segment.start().frame().direction();
            if (parent < -1 || parent >= index || length < 0 || remainingRange < 0 || direction.lengthSqr() < 0.5) {
                throw new IllegalArgumentException("Display segment " + index + " must have parent in [-1, " + index + "), non-negative length/range and direction squared length at least 0.5; got parent=" + parent + ", length=" + length + " blocks, range=" + remainingRange + " blocks, direction=" + direction + '.');
            }
        }
        Base base = input.base();
        for (DrawSection draw : base.sections()) {
            reference(draw.segmentIndex(), segments);
        }
        for (Face face : base.faces()) {
            reference(face.draw().segmentIndex(), segments);
            int vertices = face.vertices().size();
            if (vertices < 3) {
                throw new IllegalArgumentException("Display face vertex count must be at least 3; got " + vertices + '.');
            }
        }
        for (int selected : mapping.selected()) {
            reference(selected, segments);
        }
        for (int invalid : base.invalidSegments()) {
            reference(invalid, segments);
        }
        for (MappedFamily family : families) {
            int representative = family.representative();
            reference(representative, segments);
            for (int selected : family.displayIds()) {
                reference(selected, segments);
            }
            boolean polygonal = family.polygonal();
            List<Tile> tiles = family.tiles();
            for (int index = 0; index < tiles.size(); index++) {
                Tile tile = tiles.get(index);
                int size = tile.polygon().size();
                int edges = tile.exposedEdges().size();
                int normals = tile.outward().size();
                if (size < 3 || edges != size || normals != size) {
                    throw new IllegalArgumentException("Mapped display tile polygon must have at least 3 vertices and matching edge/normal counts; got vertices=" + size + ", edges=" + edges + ", normals=" + normals + '.');
                }

                if (polygonal && tile.interior() == null) {
                    throw new IllegalArgumentException("Polygonal display tile " + index + " for segment " + representative + " must have an interior point; got null.");
                }
            }
        }
    }

    private static int reference(int index, int size) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("Display reference must be in [0, " + size + "); got " + index + '.');
        }

        return index;
    }

    private static void input(FriendlyByteBuf buffer, LaserBeamDisplayInput value) {
        context(buffer, value.context());
        Base base = value.base();
        List<DrawSection> sections = base.sections();
        List<Face> faces = base.faces();
        List<DrawSection> table = new ArrayList<>();
        Map<DrawSection, Integer> indices = new HashMap<>();
        for (DrawSection draw : sections) {
            indices.computeIfAbsent(draw, key -> {
                table.add(key);
                return table.size() - 1;
            });
        }
        for (Face face : faces) {
            indices.computeIfAbsent(face.draw(), key -> {
                table.add(key);
                return table.size() - 1;
            });
        }
        list(buffer, table, LaserDisplayCodec::draw);
        list(buffer, sections, (target, draw) -> integer(target, indices.get(draw)));
        list(buffer, faces, (target, face) -> {
            list(target, face.vertices(), LaserDisplayCodec::vector);
            vector(target, face.normal());
            integer(target, indices.get(face.draw()));
            vector(target, face.startCenter());
            vector(target, face.endCenter());
            box(target, face.bounds());
        });
        integer(buffer, base.reason().ordinal());
        integer(buffer, base.remainingBudget());
        list(buffer, base.invalidSegments().stream().sorted().toList(), LaserDisplayCodec::integer);
        Mapping mapping = value.mapping();
        integer(buffer, mapping.state().ordinal());
        buffer.writeUtf(mapping.reason(), MAX_MAPPING_REASON_CHARS);
        list(buffer, mapping.selected().stream().sorted().toList(), LaserDisplayCodec::integer);
        list(buffer, mapping.families(), LaserDisplayCodec::family);
    }

    private static LaserBeamDisplayInput input(FriendlyByteBuf buffer) {
        LaserBeamContext context = context(buffer);
        List<DrawSection> table = list(buffer, LaserDisplayCodec::draw);
        List<DrawSection> sections = list(buffer, source -> table.get(reference(integer(source), table.size())));
        List<Face> faces = list(buffer, source -> new Face(list(source, LaserDisplayCodec::vector), vector(source), table.get(reference(integer(source), table.size())), vector(source), vector(source), box(source)));
        SurfaceReason reason = SurfaceReason.values()[reference(integer(buffer), SurfaceReason.values().length)];
        int budget = integer(buffer);
        Set<Integer> invalid = Set.copyOf(list(buffer, LaserDisplayCodec::integer));
        MappingState state = MappingState.values()[reference(integer(buffer), MappingState.values().length)];
        String detail = buffer.readUtf(MAX_MAPPING_REASON_CHARS);
        Set<Integer> selected = Set.copyOf(list(buffer, LaserDisplayCodec::integer));
        return new LaserBeamDisplayInput(context, new Base(sections, faces, reason, budget, invalid), new Mapping(state, detail, selected, list(buffer, LaserDisplayCodec::family)));
    }

    private static void profile(FriendlyByteBuf buffer, BeamProfile value) {
        number(buffer, value.referenceWidth());
        number(buffer, value.widthSlope());
    }

    private static BeamProfile profile(FriendlyByteBuf buffer) {
        return new BeamProfile(number(buffer), number(buffer));
    }

    private static void point(FriendlyByteBuf buffer, Point value) {
        number(buffer, value.u());
        number(buffer, value.v());
    }

    private static Point point(FriendlyByteBuf buffer) {
        return new Point(number(buffer), number(buffer));
    }

    private static void region(FriendlyByteBuf buffer, BeamRegion value) {
        list(buffer, value.vertices(), LaserDisplayCodec::point);
    }

    private static BeamRegion region(FriendlyByteBuf buffer) {
        return new BeamRegion(list(buffer, LaserDisplayCodec::point));
    }

    @SuppressWarnings("DataFlowIssue")
    private static void footprint(FriendlyByteBuf buffer, BeamFootprint value) {
        number(buffer, value.horizontalScale());
        number(buffer, value.verticalScale());
        number(buffer, value.horizontalOffset());
        number(buffer, value.verticalOffset());
        optional(buffer, value.region(), LaserDisplayCodec::region);
    }

    private static BeamFootprint footprint(FriendlyByteBuf buffer) {
        return new BeamFootprint(number(buffer), number(buffer), number(buffer), number(buffer), optional(buffer, LaserDisplayCodec::region));
    }

    private static void frame(FriendlyByteBuf buffer, DisplayFrame value) {
        vector(buffer, value.origin());
        vector(buffer, value.direction());
        vector(buffer, value.transverseAxis());
    }

    private static DisplayFrame frame(FriendlyByteBuf buffer) {
        return new DisplayFrame(vector(buffer), vector(buffer), vector(buffer));
    }

    private static void beam(FriendlyByteBuf buffer, DisplayState value) {
        frame(buffer, value.frame());
        profile(buffer, value.profile());
        number(buffer, value.fluxLm());
        footprint(buffer, value.footprint());
    }

    private static DisplayState beam(FriendlyByteBuf buffer) {
        return new DisplayState(frame(buffer), profile(buffer), number(buffer), footprint(buffer));
    }

    private static void plane(FriendlyByteBuf buffer, OpticalPlane value) {
        vector(buffer, value.point());
        vector(buffer, value.normal());
    }

    private static OpticalPlane plane(FriendlyByteBuf buffer) {
        return new OpticalPlane(vector(buffer), vector(buffer));
    }

    @SuppressWarnings("DataFlowIssue")
    private static void segment(FriendlyByteBuf buffer, DisplaySegment value) {
        integer(buffer, value.parentIndex());
        beam(buffer, value.start());
        number(buffer, value.length());
        number(buffer, value.remainingRange());
        optional(buffer, value.contact(), LaserDisplayCodec::plane);
    }

    private static DisplaySegment segment(FriendlyByteBuf buffer) {
        return new DisplaySegment(integer(buffer), beam(buffer), number(buffer), number(buffer), optional(buffer, LaserDisplayCodec::plane));
    }

    private static void context(FriendlyByteBuf buffer, LaserBeamContext value) {
        number(buffer, value.sourceRadius());
        list(buffer, value.segments(), LaserDisplayCodec::segment);
        vector(buffer, value.localOrigin());
        vector(buffer, value.worldOrigin());
        vector(buffer, value.xAxis());
        vector(buffer, value.yAxis());
        vector(buffer, value.zAxis());
    }

    private static LaserBeamContext context(FriendlyByteBuf buffer) {
        return new LaserBeamContext(number(buffer), list(buffer, LaserDisplayCodec::segment), vector(buffer), vector(buffer), vector(buffer), vector(buffer), vector(buffer));
    }

    private static void section(FriendlyByteBuf buffer, Section value) {
        list(buffer, value.starts(), LaserDisplayCodec::vector);
        list(buffer, value.ends(), LaserDisplayCodec::vector);
        flag(buffer, value.fadesAtEnd());
        flag(buffer, value.variableWidth());
    }

    private static Section section(FriendlyByteBuf buffer) {
        return new Section(list(buffer, LaserDisplayCodec::vector), list(buffer, LaserDisplayCodec::vector), flag(buffer), flag(buffer));
    }

    private static void appearance(FriendlyByteBuf buffer, LaserBeamAppearance value) {
        number(buffer, value.red());
        number(buffer, value.green());
        number(buffer, value.blue());
        number(buffer, value.opacity());
    }

    private static LaserBeamAppearance appearance(FriendlyByteBuf buffer) {
        return new LaserBeamAppearance(number(buffer), number(buffer), number(buffer), number(buffer));
    }

    private static void brightness(FriendlyByteBuf buffer, LaserBeamBrightness value) {
        vector(buffer, value.origin());
        vector(buffer, value.direction());
        profile(buffer, value.profile());
    }

    private static LaserBeamBrightness brightness(FriendlyByteBuf buffer) {
        return new LaserBeamBrightness(vector(buffer), vector(buffer), profile(buffer));
    }

    private static void fade(FriendlyByteBuf buffer, LaserBeamFade value) {
        vector(buffer, value.origin());
        vector(buffer, value.direction());
        number(buffer, value.remainingRange());
        number(buffer, value.length());
    }

    private static LaserBeamFade fade(FriendlyByteBuf buffer) {
        return new LaserBeamFade(vector(buffer), vector(buffer), number(buffer), number(buffer));
    }

    @SuppressWarnings("DataFlowIssue")
    private static void domain(FriendlyByteBuf buffer, SamplingDomain value) {
        number(buffer, value.minU());
        number(buffer, value.minV());
        number(buffer, value.maxU());
        number(buffer, value.maxV());
        optional(buffer, value.region(), LaserDisplayCodec::region);
    }

    private static SamplingDomain domain(FriendlyByteBuf buffer) {
        return new SamplingDomain(number(buffer), number(buffer), number(buffer), number(buffer), optional(buffer, LaserDisplayCodec::region));
    }

    @SuppressWarnings("DataFlowIssue")
    private static void mapped(FriendlyByteBuf buffer, MappedData value) {
        section(buffer, value.support());
        brightness(buffer, value.widthField());
        brightness(buffer, value.haloField());
        optional(buffer, value.endPlanes(), LaserDisplayCodec::planes);
    }

    private static MappedData mapped(FriendlyByteBuf buffer) {
        return new MappedData(section(buffer), brightness(buffer), brightness(buffer), optional(buffer, LaserDisplayCodec::planes));
    }

    @SuppressWarnings("DataFlowIssue")
    private static void draw(FriendlyByteBuf buffer, DrawSection value) {
        section(buffer, value.geometry());
        appearance(buffer, value.appearance());
        integer(buffer, value.segmentIndex());
        number(buffer, value.pathDistance());
        optional(buffer, value.fade(), LaserDisplayCodec::fade);
        domain(buffer, value.samplingDomain());
        flag(buffer, value.grazingTerminal());
        optional(buffer, value.brightness(), LaserDisplayCodec::brightness);
        optional(buffer, value.mapped(), LaserDisplayCodec::mapped);
    }

    private static DrawSection draw(FriendlyByteBuf buffer) {
        return new DrawSection(section(buffer), appearance(buffer), integer(buffer), number(buffer), optional(buffer, LaserDisplayCodec::fade), domain(buffer), flag(buffer), optional(buffer, LaserDisplayCodec::brightness), optional(buffer, LaserDisplayCodec::mapped));
    }

    @SuppressWarnings("DataFlowIssue")
    private static void tile(FriendlyByteBuf buffer, Tile value) {
        section(buffer, value.geometry());
        section(buffer, value.support());
        list(buffer, value.polygon(), LaserDisplayCodec::point);
        vector(buffer, value.startCenter());
        vector(buffer, value.endCenter());
        brightness(buffer, value.field());
        brightness(buffer, value.haloField());
        number(buffer, value.pathOffset());
        list(buffer, value.exposedEdges(), LaserDisplayCodec::flag);
        list(buffer, value.outward(), LaserDisplayCodec::vector);
        optional(buffer, value.endPlanes(), LaserDisplayCodec::planes);
        optional(buffer, value.interior(), LaserDisplayCodec::vector);
    }

    private static Tile tile(FriendlyByteBuf buffer) {
        return new Tile(section(buffer), section(buffer), list(buffer, LaserDisplayCodec::point), vector(buffer), vector(buffer), brightness(buffer), brightness(buffer), number(buffer), list(buffer, LaserDisplayCodec::flag), list(buffer, LaserDisplayCodec::vector), optional(buffer, LaserDisplayCodec::planes), optional(buffer, LaserDisplayCodec::vector));
    }

    private static void family(FriendlyByteBuf buffer, MappedFamily value) {
        list(buffer, value.displayIds(), LaserDisplayCodec::integer);
        integer(buffer, value.representative());
        number(buffer, value.pathDistance());
        appearance(buffer, value.appearance());
        list(buffer, value.tiles(), LaserDisplayCodec::tile);
        flag(buffer, value.polygonal());
    }

    private static MappedFamily family(FriendlyByteBuf buffer) {
        return new MappedFamily(list(buffer, LaserDisplayCodec::integer), integer(buffer), number(buffer), appearance(buffer), list(buffer, LaserDisplayCodec::tile), flag(buffer));
    }

    @SuppressWarnings("DataFlowIssue")
    private static void sample(FriendlyByteBuf buffer, Sample value) {
        position(buffer, value.pos());
        state(buffer, value.state());
        list(buffer, value.boxes(), LaserDisplayCodec::box);
        flag(buffer, value.precise());
        optional(buffer, value.optical(), LaserDisplayCodec::numbers);
    }

    private static Sample sample(FriendlyByteBuf buffer) {
        return new Sample(position(buffer), state(buffer), list(buffer, LaserDisplayCodec::box), flag(buffer), optional(buffer, LaserDisplayCodec::numbers));
    }

    private static void dependencies(FriendlyByteBuf buffer, LaserStaticDependencies value) {
        list(buffer, value.blocks(), LaserDisplayCodec::sample);
        list(buffer, value.loaded(), LaserDisplayCodec::position);
        list(buffer, value.bounds(), LaserDisplayCodec::box);
    }

    private static LaserStaticDependencies dependencies(FriendlyByteBuf buffer) {
        return new LaserStaticDependencies(list(buffer, LaserDisplayCodec::sample), list(buffer, LaserDisplayCodec::position), list(buffer, LaserDisplayCodec::box));
    }

    @SuppressWarnings("DataFlowIssue")
    private static void snapshot(FriendlyByteBuf buffer, LaserDisplaySnapshot value) {
        vector(buffer, value.origin());
        vector(buffer, value.direction());
        number(buffer, value.fluxLm());
        integer(buffer, value.range());
        optional(buffer, value.rigid(), (target, rigid) -> {
            target.writeUUID(rigid.host());
            vector(target, rigid.scale());
            box(target, rigid.localBounds());
        });
        dependencies(buffer, value.dependencies());
        input(buffer, value.input());
        Input clipping = value.clipping();
        beam(buffer, clipping.source());
        list(buffer, clipping.paths(), (target, path) -> list(target, path.events(), LaserDisplayCodec::event));
        list(buffer, clipping.targets(), (target, hit) -> optional(target, hit.pos(), LaserDisplayCodec::position));
    }

    private static LaserDisplaySnapshot snapshot(FriendlyByteBuf buffer) {
        Vec3 origin = vector(buffer);
        Vec3 direction = vector(buffer);
        double flux = number(buffer);
        int range = integer(buffer);
        LaserRigidDisplay rigid = optional(buffer, source -> new LaserRigidDisplay(source.readUUID(), vector(source), box(source)));
        LaserStaticDependencies dependencies = dependencies(buffer);
        LaserBeamDisplayInput input = input(buffer);
        Input clipping = new Input(beam(buffer), input.context(), list(buffer, source -> new Path(list(source, LaserDisplayCodec::event))), list(buffer, source -> new Target(optional(source, LaserDisplayCodec::position))));
        return new LaserDisplaySnapshot(origin, direction, flux, range, dependencies, input, clipping, rigid);
    }

    @SuppressWarnings("DataFlowIssue")
    private static void event(FriendlyByteBuf buffer, Event value) {
        OpticalTarget target = value.target();
        buffer.writeResourceLocation(target.dimension().location());
        position(buffer, target.blockPos());
        optional(buffer, target.contraptionId(), (targetBuffer, id) -> targetBuffer.writeUUID(id));
        flag(buffer, target.inSubLevel());
        vector(buffer, value.normal());
        buffer.writeLong(value.plane());
        aperture(buffer, value.aperture());
        optional(buffer, value.frame(), (out, frame) -> {
            aperture(out, frame.opening());
            number(out, frame.outerHalfSize());
            number(out, frame.halfThickness());
        });
        buffer.writeUtf(value.interactor(), MAX_INTERACTOR_NAME_CHARS);
    }

    private static Event event(FriendlyByteBuf buffer) {
        OpticalTarget target = new OpticalTarget(ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation()), position(buffer), optional(buffer, source -> source.readUUID()), flag(buffer));
        return new Event(target, vector(buffer), buffer.readLong(), aperture(buffer), optional(buffer, source -> new OpticalApertureFrame(aperture(source), number(source), number(source))), buffer.readUtf(MAX_INTERACTOR_NAME_CHARS));
    }

    private static void aperture(FriendlyByteBuf buffer, OpticalAperture value) {
        vector(buffer, value.center());
        vector(buffer, value.horizontal());
        vector(buffer, value.vertical());
        number(buffer, value.halfSize());
    }

    private static OpticalAperture aperture(FriendlyByteBuf buffer) {
        return new OpticalAperture(vector(buffer), vector(buffer), vector(buffer), number(buffer));
    }

    private static void vector(FriendlyByteBuf buffer, Vec3 value) {
        number(buffer, value.x);
        number(buffer, value.y);
        number(buffer, value.z);
    }

    private static Vec3 vector(FriendlyByteBuf buffer) {
        return new Vec3(number(buffer), number(buffer), number(buffer));
    }

    private static void box(FriendlyByteBuf buffer, AABB value) {
        number(buffer, value.minX);
        number(buffer, value.minY);
        number(buffer, value.minZ);
        number(buffer, value.maxX);
        number(buffer, value.maxY);
        number(buffer, value.maxZ);
    }

    private static AABB box(FriendlyByteBuf buffer) {
        return new AABB(number(buffer), number(buffer), number(buffer), number(buffer), number(buffer), number(buffer));
    }

    private static void number(FriendlyByteBuf buffer, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Display number must be finite; got " + value + '.');
        }

        buffer.writeDouble(value);
    }

    private static double number(FriendlyByteBuf buffer) {
        double value = buffer.readDouble();
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Display number must be finite; got " + value + '.');
        }

        return value;
    }

    private static void integer(FriendlyByteBuf buffer, int value) {
        buffer.writeVarInt(value);
    }

    private static int integer(FriendlyByteBuf buffer) {
        return buffer.readVarInt();
    }

    private static void flag(FriendlyByteBuf buffer, boolean value) {
        buffer.writeBoolean(value);
    }

    private static boolean flag(FriendlyByteBuf buffer) {
        return buffer.readBoolean();
    }

    private static void position(FriendlyByteBuf buffer, BlockPos value) {
        buffer.writeBlockPos(value);
    }

    private static BlockPos position(FriendlyByteBuf buffer) {
        return buffer.readBlockPos();
    }

    private static void state(FriendlyByteBuf buffer, BlockState value) {
        integer(buffer, Block.getId(value));
    }

    private static BlockState state(FriendlyByteBuf buffer) {
        int id = integer(buffer);
        BlockState state = Block.stateById(id);
        if (Block.getId(state) != id) {
            throw new IllegalArgumentException("Unknown block state ID in display dependency: " + id + '.');
        }

        return state;
    }

    private static void planes(FriendlyByteBuf buffer, List<OpticalPlane> value) {
        list(buffer, value, LaserDisplayCodec::plane);
    }

    private static List<OpticalPlane> planes(FriendlyByteBuf buffer) {
        return list(buffer, LaserDisplayCodec::plane);
    }

    private static void numbers(FriendlyByteBuf buffer, List<Double> value) {
        list(buffer, value, LaserDisplayCodec::number);
    }

    private static List<Double> numbers(FriendlyByteBuf buffer) {
        return list(buffer, LaserDisplayCodec::number);
    }

    private static <T> void list(FriendlyByteBuf buffer, List<T> values, BiConsumer<FriendlyByteBuf, T> write) {
        if (values.size() > MAX_ELEMENTS) {
            throw new CapacityExceeded("Display list exceeds " + MAX_ELEMENTS + " elements; got " + values.size() + '.');
        }

        integer(buffer, values.size());
        for (T value : values) {
            write.accept(buffer, value);
        }
    }

    private static <T> List<T> list(FriendlyByteBuf buffer, Function<FriendlyByteBuf, T> read) {
        int count = integer(buffer);
        if (count < 0 || count > MAX_ELEMENTS) {
            throw new IllegalArgumentException("Laser display list length must be in [0, " + MAX_ELEMENTS + "] elements; got " + count + '.');
        }

        int remainingBytes = buffer.readableBytes();
        if (count > remainingBytes) {
            throw new IllegalArgumentException("Laser display list length " + count + " exceeds the " + remainingBytes + " remaining bytes.");
        }

        List<T> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            result.add(read.apply(buffer));
        }
        return List.copyOf(result);
    }

    private static <T> void optional(FriendlyByteBuf buffer, @Nullable T value, BiConsumer<FriendlyByteBuf, T> write) {
        buffer.writeBoolean(value != null);
        if (value == null) {
            return;
        }

        write.accept(buffer, value);
    }

    private static <T> @Nullable T optional(FriendlyByteBuf buffer, Function<FriendlyByteBuf, T> read) {
        if (!buffer.readBoolean()) {
            return null;
        }

        return read.apply(buffer);
    }

    private static final class CapacityExceeded extends RuntimeException {
        private CapacityExceeded(String message) {
            super(message);
        }
    }
}
