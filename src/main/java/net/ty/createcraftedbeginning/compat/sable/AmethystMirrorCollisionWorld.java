package net.ty.createcraftedbeginning.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorBlock;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorCollisionShape;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystMirrorCollisionWorld {
    private static final int CORNER = 3;
    private static final Map<ServerLevel, AmethystMirrorCollisionWorld> WORLDS = new HashMap<>();
    private final ServerLevel level;
    private final Set<BlockPos> dirty = new HashSet<>();
    private final Map<BlockPos, Set<BlockPos>> sourceCells = new HashMap<>();
    private final Map<BlockPos, SourceShape> sourceShapes = new HashMap<>();
    private final Map<BlockPos, SubLevelAccess> sourceOwners = new HashMap<>();
    private final Map<SubLevelAccess, Set<BlockPos>> structureSources = new IdentityHashMap<>();
    private final Long2ObjectMap<Set<BlockPos>> sectionCells = new Long2ObjectOpenHashMap<>();
    private final Map<BlockPos, Map<BlockPos, List<AABB>>> parts = new HashMap<>();
    private final Map<BlockPos, Collider> colliders = new HashMap<>();
    private final Set<BlockPos> changedCells = new HashSet<>();

    public AmethystMirrorCollisionWorld(ServerLevel level) {
        this.level = level;
        WORLDS.put(level, this);
    }

    public static void update(AmethystMirrorBlockEntity mirror) {
        if (!(mirror.getLevel() instanceof ServerLevel level)) {
            return;
        }

        AmethystMirrorCollisionWorld world = WORLDS.get(level);
        if (world == null) {
            return;
        }

        world.dirty.add(mirror.getBlockPos().immutable());
    }

    public Set<BlockPos> refresh() {
        if (dirty.isEmpty()) {
            return Set.of();
        }

        Set<BlockPos> sources = Set.copyOf(dirty);
        dirty.clear();
        Set<BlockPos> affected = new HashSet<>();
        for (BlockPos source : sources) {
            AmethystMirrorCollisionShape shape = null;
            BlockState state = null;
            SubLevelAccess owner = null;
            if (level.isLoaded(source) && level.getBlockEntity(source) instanceof AmethystMirrorBlockEntity mirror && !mirror.isRemoved()) {
                shape = mirror.getPhysicalShape();
                state = mirror.getBlockState();
                owner = SableCompanion.INSTANCE.getContaining(level, source);
                SourceShape snapshot = sourceShapes.get(source);
                if (snapshot != null && snapshot.shape() == shape && snapshot.state() == state && sourceOwners.get(source) == owner) {
                    continue;
                }
            }
            Map<BlockPos, List<AABB>> divided = Map.of();
            if (shape != null) {
                divided = AmethystMirrorVoxelShape.create(shape).split(source);
            }
            Set<BlockPos> currentCells = Set.copyOf(divided.keySet());
            SubLevelAccess previousOwner = sourceOwners.get(source);
            if (previousOwner != null && previousOwner != owner) {
                sourceOwners.remove(source);
                Set<BlockPos> owned = structureSources.get(previousOwner);
                owned.remove(source);
                if (owned.isEmpty()) {
                    structureSources.remove(previousOwner);
                }
            }
            Set<BlockPos> previous = sourceCells.get(source);
            if (previous != null) {
                for (BlockPos cell : previous) {
                    if (currentCells.contains(cell)) {
                        continue;
                    }

                    Map<BlockPos, List<AABB>> contributors = parts.get(cell);
                    contributors.remove(source);
                    if (!contributors.isEmpty()) {
                        continue;
                    }

                    parts.remove(cell);
                    long section = SectionPos.asLong(cell);
                    Set<BlockPos> indexed = sectionCells.get(section);
                    indexed.remove(cell);
                    if (!indexed.isEmpty()) {
                        continue;
                    }

                    sectionCells.remove(section);
                }
                affected.addAll(previous);
            }
            if (shape == null) {
                sourceCells.remove(source);
                sourceShapes.remove(source);
                continue;
            }

            sourceCells.put(source, currentCells);
            divided.forEach((cell, boxes) -> parts.computeIfAbsent(cell, ignored -> new HashMap<>()).put(source, List.copyOf(boxes)));
            for (BlockPos cell : currentCells) {
                sectionCells.computeIfAbsent(SectionPos.asLong(cell), ignored -> new HashSet<>()).add(cell);
            }
            if (owner != null && previousOwner != owner) {
                sourceOwners.put(source, owner);
                structureSources.computeIfAbsent(owner, ignored -> new HashSet<>()).add(source);
            }
            sourceShapes.put(source, new SourceShape(shape, state));
            affected.addAll(currentCells);
        }
        changedCells.addAll(affected);
        for (BlockPos cell : affected) {
            if (level.isLoaded(cell)) {
                continue;
            }

            Collider removed = colliders.remove(cell);
            if (removed != null) {
                AmethystMirrorRapierAccess.release(removed.material(), removed.handle());
            }
            changedCells.remove(cell);
        }
        return affected;
    }

    public int pack(BlockPos pos, int original) {
        Map<BlockPos, List<AABB>> contributors = parts.get(pos);
        Collider previous = colliders.get(pos);
        if (contributors == null) {
            if (previous != null) {
                colliders.remove(pos);
                AmethystMirrorRapierAccess.release(previous.material(), previous.handle());
            }
            changedCells.remove(pos);
            return original;
        }

        BlockState state = level.getBlockState(pos);
        VoxelShape base = Shapes.empty();
        if (!(state.getBlock() instanceof AmethystMirrorBlock)) {
            base = state.getCollisionShape(level, pos);
        }
        if (Block.isShapeFullBlock(base)) {
            if (previous != null) {
                colliders.remove(pos);
                AmethystMirrorRapierAccess.release(previous.material(), previous.handle());
            }
            changedCells.remove(pos);
            return original;
        }

        BlockState material = state;
        if (base.isEmpty()) {
            material = CCBBlocks.AMETHYST_MIRROR_BLOCK.getDefaultState();
        }
        if (previous != null && previous.original() == original && previous.state() == state && !changedCells.contains(pos)) {
            int encodedHandle = previous.handle() + 1;
            return CORNER | encodedHandle << 16;
        }

        List<AABB> boxes = new ArrayList<>(base.toAabbs());
        contributors.values().forEach(boxes::addAll);
        int handle = AmethystMirrorRapierAccess.acquire(material, boxes);
        colliders.put(pos.immutable(), new Collider(handle, material, state, original));
        changedCells.remove(pos);
        if (previous != null) {
            AmethystMirrorRapierAccess.release(previous.material(), previous.handle());
        }
        int encodedHandle = handle + 1;
        return CORNER | encodedHandle << 16;
    }

    public void packSection(int x, int y, int z, int[] data) {
        Set<BlockPos> cells = sectionCells.get(SectionPos.asLong(x, y, z));
        if (cells == null) {
            return;
        }

        for (BlockPos pos : cells) {
            int index = (pos.getY() & 15) << 8 | (pos.getZ() & 15) << 4 | pos.getX() & 15;
            data[index] = pack(pos, data[index]);
        }
    }

    public void expandBounds(SubLevelAccess body, int[] bounds) {
        Set<BlockPos> sources = structureSources.get(body);
        if (sources == null) {
            return;
        }

        int minX = bounds[0];
        int minY = bounds[1];
        int minZ = bounds[2];
        int maxX = bounds[3];
        int maxY = bounds[4];
        int maxZ = bounds[5];
        for (BlockPos source : sources) {
            int x = source.getX();
            int y = source.getY();
            int z = source.getZ();
            if (x < minX || y < minY || z < minZ || x > maxX || y > maxY || z > maxZ) {
                continue;
            }

            for (BlockPos cell : sourceCells.get(source)) {
                int cellX = cell.getX();
                int cellY = cell.getY();
                int cellZ = cell.getZ();
                bounds[0] = Math.min(bounds[0], cellX);
                bounds[1] = Math.min(bounds[1], cellY);
                bounds[2] = Math.min(bounds[2], cellZ);
                bounds[3] = Math.max(bounds[3], cellX);
                bounds[4] = Math.max(bounds[4], cellY);
                bounds[5] = Math.max(bounds[5], cellZ);
            }
        }
    }

    public void close() {
        WORLDS.remove(level, this);
        for (Collider collider : colliders.values()) {
            AmethystMirrorRapierAccess.release(collider.material(), collider.handle());
        }
        colliders.clear();
        sourceCells.clear();
        sourceShapes.clear();
        sourceOwners.clear();
        structureSources.clear();
        sectionCells.clear();
        parts.clear();
        dirty.clear();
        changedCells.clear();
    }

    private record Collider(int handle, BlockState material, BlockState state, int original) {}

    private record SourceShape(AmethystMirrorCollisionShape shape, BlockState state) {}
}
