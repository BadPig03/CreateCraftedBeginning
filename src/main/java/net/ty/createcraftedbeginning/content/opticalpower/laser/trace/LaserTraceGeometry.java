package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.amethystlens.AmethystLensBlock;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorBlock;
import net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor.AmethystSoundSuppressorBlock;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Reason;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalElement;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile.Span;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Patch;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserQuerySpace.Pose;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceDependencies.BlockSample;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceDependencies.LoadSample;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalchamber.PhotothermalChamberBlock;
import net.ty.createcraftedbeginning.mixin.common.accessor.ClipContextAccessor;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RenderGeometry;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserTraceGeometry {
    private static final int MAX_CACHED_BLOCKS = 4096;
    private static final int MAX_CACHED_CELLS = 256;
    private static final int MAX_DEPENDENCIES = 65536;

    private final Map<BlockGetter, Long2ObjectOpenHashMap<PreparedBlock>> blocks = new IdentityHashMap<>();
    private final Map<BlockGetter, Long2ObjectOpenHashMap<List<BlockPos>>> cells = new IdentityHashMap<>();
    private int cachedCells;
    private @Nullable List<LaserQuerySpace> mounted;
    private final Long2BooleanOpenHashMap loaded = new Long2BooleanOpenHashMap();
    private final boolean tracking;
    private int cachedBlocks;
    private int observedBlocks;
    private final Map<BlockGetter, Long2ObjectOpenHashMap<PreparedBlock>> observed = new IdentityHashMap<>();
    private @Nullable LaserWorldSections sections;
    private @Nullable LaserAirRegions air;
    private boolean complete = true;
    private final Set<AABB> bounds = new LinkedHashSet<>();

    LaserTraceGeometry(boolean tracking) {
        this.tracking = tracking;
    }

    static @Nullable PreparedBlock prepareWorldBlock(BlockGetter level, BlockPos pos, ClipContext context, float partialTick) {
        if (level instanceof Level world && !world.isLoaded(pos)) {
            return null;
        }

        BlockState state = level.getBlockState(pos);
        if (((ClipContextAccessor) context).ccb$getCollisionContext() instanceof LaserCollisionContext collision) {
            OpticalGeometry override = collision.getOverride(level, pos);
            if (override != null) {
                return new PreparedBlock(state, override.getCollisionShape(), List.of(), true, false, override);
            }
        }
        VoxelShape shape = context.getBlockShape(state, level, pos);
        OpticalGeometry optical = null;
        if (state.getBlock() instanceof OpticalElement element) {
            optical = element.getOpticalGeometry(level, pos, state, partialTick);
        }
        List<AABB> boxes = List.of();
        boolean precise = LaserOcclusionShape.isPrecise(state);
        boolean transformed = false;
        if (!precise && !shape.isEmpty()) {
            boxes = LaserOcclusionShape.boxes(shape);
            transformed = level instanceof Level world && SubLevelBridge.resolve(world, pos).inSubLevel();
        }
        return new PreparedBlock(state, shape, boxes, precise, transformed, optical);
    }

    private static @Nullable AABB terminalBounds(Segment segment) {
        OpticalHit hit = segment.hit();
        if (hit == null) {
            return null;
        }

        BeamState beam = segment.start();
        BeamContact contact = segment.contact();
        if (contact == null) {
            contact = BeamContact.create(beam, segment.remainingRange(), hit);
        }
        if (contact == null || contact.patches().isEmpty()) {
            return rangeBounds(beam, segment.remainingRange());
        }

        Vec3 origin = hit.position();
        AABB bounds = new AABB(origin, origin);
        for (Patch patch : contact.patches()) {
            for (Vec3 vertex : patch.vertices()) {
                Vec3 point = origin.add(vertex);
                if (!Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z)) {
                    return rangeBounds(beam, segment.remainingRange());
                }

                bounds = bounds.minmax(new AABB(point, point));
            }
        }
        return bounds;
    }

    private static AABB rangeBounds(BeamState beam, double range) {
        Vec3 origin = beam.frame().origin();
        AABB bounds = new AABB(origin, origin);
        List<Point> vertices = beam.footprint().domain().vertices();
        for (Span span : beam.profile().spans(range)) {
            for (Point point : vertices) {
                Vec3 first = BeamContact.point(beam, span.from(), point.u(), point.v());
                Vec3 last = BeamContact.point(beam, span.to(), point.u(), point.v());
                bounds = bounds.minmax(new AABB(first, last));
            }
        }
        return bounds;
    }

    List<LaserQuerySpace> mounted(Level level, float partialTick) {
        if (mounted == null) {
            mounted = LaserQuerySpace.mounted(level, partialTick);
        }
        return mounted;
    }

    boolean isLoaded(Level level, BlockPos pos) {
        boolean available = level.isLoaded(pos);
        if (tracking && complete) {
            if (sections == null) {
                sections = new LaserWorldSections(level);
            }
            sections.include(level, pos);
            long position = pos.asLong();
            if (loaded.size() >= MAX_DEPENDENCIES && !loaded.containsKey(position)) {
                complete = false;
                return available;
            }

            loaded.put(position, available);
        }
        return available;
    }

    void includeIndexed(Level level, BlockPos minimum, BlockPos maximum, BeamVolumeCells cells) {
        if (!tracking) {
            return;
        }

        if (air == null) {
            air = new LaserAirRegions(level);
        }
        air.include(level, minimum, maximum, cells);
    }

    void includeBounds(AABB queryBounds) {
        if (!tracking) {
            return;
        }

        AABB dependencyBounds = new AABB(Math.floor(queryBounds.minX) - LaserBlockClip.NEIGHBOR_REACH, Math.floor(queryBounds.minY) - LaserBlockClip.NEIGHBOR_REACH, Math.floor(queryBounds.minZ) - LaserBlockClip.NEIGHBOR_REACH, Math.ceil(queryBounds.maxX) + LaserBlockClip.NEIGHBOR_REACH, Math.ceil(queryBounds.maxY) + LaserBlockClip.NEIGHBOR_REACH, Math.ceil(queryBounds.maxZ) + LaserBlockClip.NEIGHBOR_REACH);
        if (bounds.size() >= MAX_CACHED_BLOCKS && !bounds.contains(dependencyBounds)) {
            complete = false;
            return;
        }

        bounds.add(dependencyBounds);
    }

    @Nullable LaserTraceDependencies capture(Level level, BeamPropagation propagation, float partialTick, boolean rendering) {
        if (!tracking || !complete || bounds.isEmpty() || air != null && !air.matches(level)) {
            return null;
        }

        boolean boundedContraptions = (level.isClientSide || !rendering) && propagation.terminations().stream().allMatch(termination -> termination.reason() == Reason.ABSORBED || termination.reason() == Reason.RANGE_EXHAUSTED || termination.reason() == Reason.BELOW_MIN_POWER);
        Map<AABB, AABB> paths = new LinkedHashMap<>();
        List<AABB> serverMotionBounds = !rendering && boundedContraptions ? new ArrayList<>() : List.of();
        List<Segment> segments = propagation.segments();
        AABB[] terminalBounds = null;
        if (rendering || boundedContraptions) {
            terminalBounds = new AABB[segments.size()];
        }
        for (int index = 0; index < segments.size(); index++) {
            Segment segment = segments.get(index);
            BeamState beam = segment.start();
            BeamFrame frame = beam.frame();
            Vec3 start = frame.origin();
            BeamState incident = segment.incident();
            Vec3 end = incident.frame().origin();
            BeamFootprint footprint = beam.footprint();
            double margin = Math.max(beam.profile().radius(), incident.profile().radius()) * (footprint.horizontalScale() + footprint.verticalScale() + Math.abs(footprint.horizontalOffset()) + Math.abs(footprint.verticalOffset()));
            AABB path = new AABB(start, end).inflate(margin);
            AABB physicalPath = path;
            if (terminalBounds != null) {
                AABB terminal = terminalBounds(segment);
                terminalBounds[index] = terminal;
                if (terminal != null) {
                    path = path.minmax(terminal);
                }
                int parentIndex = segment.parentIndex();
                if (parentIndex >= 0 && parentIndex < index) {
                    AABB parent = terminalBounds[parentIndex];
                    if (parent != null) {
                        path = path.minmax(parent);
                    }
                }
            }
            if (!rendering && boundedContraptions) {
                serverMotionBounds.add(path.inflate(BeamSweepSection.CONTACT_TOLERANCE));
                path = physicalPath;
            }
            AABB cell = new AABB(Math.floor(path.minX), Math.floor(path.minY), Math.floor(path.minZ), Math.ceil(path.maxX), Math.ceil(path.maxY), Math.ceil(path.maxZ));
            paths.merge(cell, path, AABB::minmax);
        }
        if (paths.isEmpty()) {
            return null;
        }

        List<AABB> queries = List.copyOf(bounds);
        List<AABB> tracedPaths = List.copyOf(paths.values());
        List<AABB> contraptionBounds = queries;
        if (boundedContraptions) {
            if (rendering) {
                contraptionBounds = tracedPaths.stream().map(path -> path.inflate(BeamSweepSection.CONTACT_TOLERANCE)).toList();
            }
            else {
                contraptionBounds = List.copyOf(serverMotionBounds);
            }
        }
        if (!rendering && !LaserTraceDependencies.isStaticWorld(level, queries, contraptionBounds, 1)) {
            return null;
        }

        List<BlockSample> samples = new ArrayList<>();
        Map<BlockGetter, UUID> owners = new IdentityHashMap<>();
        for (LaserQuerySpace space : mounted(level, partialTick)) {
            owners.put(space.owner(), space.id());
        }
        for (Entry<BlockGetter, Long2ObjectOpenHashMap<PreparedBlock>> entry : observed.entrySet()) {
            BlockGetter owner = entry.getKey();
            UUID id = owners.get(owner);
            if (owner != level && id == null) {
                return null;
            }

            if (!rendering && owner != level) {
                if (!boundedContraptions) {
                    return null;
                }

                continue;
            }

            Long2ObjectOpenHashMap<PreparedBlock> ownerBlocks = entry.getValue();
            for (long position : ownerBlocks.keySet()) {
                if (!rendering && SubLevelBridge.resolve(level, BlockPos.of(position)).inSubLevel()) {
                    return null;
                }

                PreparedBlock block = ownerBlocks.get(position);
                if (block == null) {
                    if (!rendering) {
                        return null;
                    }

                    samples.add(new BlockSample(BlockPos.of(position), id, null, List.of(), false, null));
                    continue;
                }

                BlockState state = block.state();
                Block type = state.getBlock();
                if (type instanceof OpticalElement && !(type instanceof AmethystMirrorBlock) && !(type instanceof AmethystSoundSuppressorBlock) && !(type instanceof AmethystLensBlock) && !(type instanceof PhotothermalChamberBlock)) {
                    return null;
                }

                boolean precise = block.precise();
                List<AABB> boxes = block.boxes();
                if (precise) {
                    boxes = List.copyOf(block.shape().toAabbs());
                }
                samples.add(new BlockSample(BlockPos.of(position), id, state, boxes, precise, block.optical()));
            }
        }
        List<LoadSample> loading = new ArrayList<>();
        for (long position : loaded.keySet()) {
            boolean available = loaded.get(position);
            if (!rendering && !available) {
                return null;
            }

            loading.add(new LoadSample(BlockPos.of(position), available));
        }
        LaserWorldSections sections = this.sections;
        if (!rendering) {
            if (sections != null && !sections.matches(level)) {
                return null;
            }

            return new LaserTraceDependencies(List.copyOf(samples), List.copyOf(loading), queries, tracedPaths, contraptionBounds, Map.of(), Map.of(), sections, air);
        }

        Map<UUID, RenderGeometry> subLevels = SubLevelBridge.captureRenderGeometry(level, queries, tracedPaths, partialTick);
        if (subLevels == null) {
            return null;
        }

        Map<UUID, Pose> contraptions = LaserTraceDependencies.poses(mounted(level, partialTick), contraptionBounds);
        if (contraptionBounds != queries) {
            samples.removeIf(sample -> sample.owner() != null && !contraptions.containsKey(sample.owner()));
        }
        if (!subLevels.isEmpty() || !contraptions.isEmpty()) {
            sections = null;
        }
        else if (sections != null && !sections.matches(level)) {
            return null;
        }

        return new LaserTraceDependencies(List.copyOf(samples), List.copyOf(loading), queries, tracedPaths, contraptionBounds, subLevels, contraptions, sections, air);
    }

    List<BlockPos> getWorldCandidates(BlockGetter level, BlockPos cell, ClipContext context, float partialTick) {
        return getCandidates(level, cell, pos -> getWorldBlock(level, pos, context, partialTick));
    }

    @Nullable PreparedBlock getWorldBlock(BlockGetter level, BlockPos pos, ClipContext context, float partialTick) {
        return getBlock(level, pos, () -> {
            if (tracking && complete && level instanceof Level world) {
                if (sections == null) {
                    sections = new LaserWorldSections(world);
                }
                sections.include(world, pos);
            }
            return prepareWorldBlock(level, pos, context, partialTick);
        });
    }

    @Nullable PreparedBlock getMountedBlock(BlockGetter level, Map<BlockPos, StructureBlockInfo> mountedBlocks, BlockPos pos, LaserCollisionContext context) {
        return getBlock(level, pos, () -> {
            StructureBlockInfo info = mountedBlocks.get(pos);
            if (info == null) {
                return null;
            }

            BlockState state = info.state();
            if (state.getBlock() instanceof OpticalElement element) {
                OpticalGeometry optical = element.getMountedOpticalGeometry(state, info.nbt());
                return new PreparedBlock(state, optical.getCollisionShape(), List.of(), true, true, optical);
            }

            VoxelShape shape = state.getCollisionShape(level, pos, context);
            List<AABB> boxes = List.of();
            boolean precise = LaserOcclusionShape.isPrecise(state);
            if (!precise && !shape.isEmpty()) {
                boxes = LaserOcclusionShape.boxes(shape);
            }
            return new PreparedBlock(state, shape, boxes, precise, true, null);
        });
    }

    private List<BlockPos> getCandidates(BlockGetter level, BlockPos cell, Function<BlockPos, @Nullable PreparedBlock> query) {
        Long2ObjectOpenHashMap<List<BlockPos>> ownerCells = cells.get(level);
        long key = cell.asLong();
        if (ownerCells != null) {
            List<BlockPos> cached = ownerCells.get(key);
            if (cached != null) {
                return cached;
            }
        }

        int reach = LaserBlockClip.NEIGHBOR_REACH;
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos cursor : BlockPos.betweenClosed(cell.offset(-reach, -reach, -reach), cell.offset(reach, reach, reach))) {
            PreparedBlock block = query.apply(cursor);
            if (block == null || block.shape().isEmpty()) {
                continue;
            }

            candidates.add(cursor.immutable());
        }
        List<BlockPos> result = List.copyOf(candidates);
        if (cachedCells >= MAX_CACHED_CELLS) {
            cells.clear();
            cachedCells = 0;
            ownerCells = null;
        }
        if (ownerCells == null) {
            ownerCells = new Long2ObjectOpenHashMap<>();
            cells.put(level, ownerCells);
        }
        ownerCells.put(key, result);
        cachedCells++;
        return result;
    }

    private @Nullable PreparedBlock getBlock(BlockGetter owner, BlockPos pos, Supplier<@Nullable PreparedBlock> prepare) {
        Long2ObjectOpenHashMap<PreparedBlock> ownerBlocks = blocks.get(owner);
        long key = pos.asLong();
        if (ownerBlocks != null && ownerBlocks.containsKey(key)) {
            return ownerBlocks.get(key);
        }

        PreparedBlock block = prepare.get();
        if (tracking && complete) {
            Long2ObjectOpenHashMap<PreparedBlock> ownerObserved = observed.computeIfAbsent(owner, ignored -> new Long2ObjectOpenHashMap<>());
            if (!ownerObserved.containsKey(key)) {
                if (observedBlocks >= MAX_DEPENDENCIES) {
                    complete = false;
                }
                else {
                    ownerObserved.put(key, block);
                    observedBlocks++;
                }
            }
        }
        if (cachedBlocks >= MAX_CACHED_BLOCKS) {
            blocks.clear();
            cachedBlocks = 0;
            ownerBlocks = null;
        }
        if (ownerBlocks == null) {
            ownerBlocks = new Long2ObjectOpenHashMap<>();
            blocks.put(owner, ownerBlocks);
        }
        ownerBlocks.put(key, block);
        cachedBlocks++;
        return block;
    }

    record PreparedBlock(BlockState state, VoxelShape shape, List<AABB> boxes, boolean precise, boolean transformed, @Nullable OpticalGeometry optical) {
        @Nullable BlockHitResult clip(Vec3 start, Vec3 end, BlockPos pos) {
            if (precise) {
                return shape.clip(start, end, pos);
            }

            return LaserOcclusionShape.clip(boxes, start, end, pos, transformed);
        }
    }
}
