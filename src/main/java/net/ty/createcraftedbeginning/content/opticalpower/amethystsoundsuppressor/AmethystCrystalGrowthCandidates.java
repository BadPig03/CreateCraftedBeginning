package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.content.opticalpower.experiencecrystal.ExperienceCrystalBlock;
import net.ty.createcraftedbeginning.content.opticalpower.experiencecrystal.ExperienceCrystalSectionAccess;
import net.ty.createcraftedbeginning.content.opticalpower.experiencecrystal.ExperienceCrystalSectionIndex;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.LocalRegion;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystCrystalGrowthCandidates {
    private final Set<BlockPos> immature = new HashSet<>();
    private final Targets growth = new Targets();
    private final Targets seeds = new Targets();
    private List<LocalRegion> regions = List.of();
    private @Nullable Long2ObjectLinkedOpenHashMap<SectionView> sections;
    private @Nullable Long2ObjectLinkedOpenHashMap<SectionView> workingSections;
    private @Nullable ServerLevel cachedLevel;
    private @Nullable AABB cachedBounds;
    private boolean noTarget;

    private static void addCellAndNeighbors(Set<BlockPos> cells, BlockPos pos) {
        cells.add(pos);
        for (Direction direction : Iterate.directions) {
            cells.add(pos.relative(direction));
        }
    }

    @Nullable Target select(ServerLevel level, BlockPos center) {
        AABB bounds = AmethystSoundSuppressorCrystalGrowth.bounds(center);
        List<LocalRegion> currentRegions = SubLevelBridge.localRegions(level, bounds);
        Long2ObjectMap<SectionView> previousSections = sections;
        if (previousSections == null) {
            previousSections = Long2ObjectMaps.emptyMap();
        }
        Long2ObjectLinkedOpenHashMap<SectionView> currentSections = workingSections;
        if (currentSections == null) {
            currentSections = new Long2ObjectLinkedOpenHashMap<>();
        }
        currentSections.clear();
        for (LocalRegion region : currentRegions) {
            AABB local = region.bounds().inflate(1);
            int minY = Math.max(level.getMinSection(), (int) Math.floor(local.minY) >> 4);
            int maxY = Math.min(level.getMaxSection() - 1, (int) Math.floor(local.maxY) >> 4);
            for (int x = (int) Math.floor(local.minX) >> 4; x <= (int) Math.floor(local.maxX) >> 4; x++) {
                for (int z = (int) Math.floor(local.minZ) >> 4; z <= (int) Math.floor(local.maxZ) >> 4; z++) {
                    if (!level.isLoaded(new BlockPos(x << 4, level.getMinBuildHeight(), z << 4))) {
                        continue;
                    }

                    LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                    if (chunk == null) {
                        continue;
                    }

                    for (int y = minY; y <= maxY; y++) {
                        long key = SectionPos.asLong(x, y, z);
                        if (currentSections.containsKey(key)) {
                            continue;
                        }

                        ExperienceCrystalSectionIndex index = ((ExperienceCrystalSectionAccess) chunk.getSection(level.getSectionIndexFromSectionY(y))).ccb$getCrystalIndex();
                        long revision = index.revision();
                        SectionView previous = previousSections.get(key);
                        if (previous != null && previous.index() == index && previous.revision() == revision) {
                            currentSections.put(key, previous);
                            continue;
                        }

                        BlockPos origin = previous == null ? new BlockPos(x << 4, y << 4, z << 4) : previous.origin();
                        currentSections.put(key, new SectionView(origin, index, revision));
                    }
                }
            }
        }

        boolean rebuild = cachedLevel != level || !bounds.equals(cachedBounds) || !currentRegions.equals(regions) || !currentSections.keySet().equals(previousSections.keySet());
        Set<BlockPos> changed = null;
        if (!rebuild) {
            for (Entry<SectionView> entry : Long2ObjectMaps.fastIterable(currentSections)) {
                SectionView current = entry.getValue();
                SectionView previous = previousSections.get(entry.getLongKey());
                ExperienceCrystalSectionIndex index = current.index();
                if (previous.index() != index) {
                    rebuild = true;
                    break;
                }

                long previousRevision = previous.revision();
                if (previousRevision == index.revision()) {
                    continue;
                }

                if (changed == null) {
                    changed = new HashSet<>();
                }
                Set<BlockPos> changedCells = changed;
                if (!index.collectChanges(previousRevision, current.origin(), pos -> addCellAndNeighbors(changedCells, pos))) {
                    rebuild = true;
                    break;
                }
            }
        }

        if (rebuild) {
            immature.clear();
            growth.clear();
            seeds.clear();
            if (changed == null) {
                changed = new HashSet<>();
            }
            changed.clear();
            Set<BlockPos> changedCells = changed;
            for (SectionView section : currentSections.values()) {
                section.index().collectRelevant(section.origin(), pos -> addCellAndNeighbors(changedCells, pos));
            }
        }
        if (changed != null) {
            for (BlockPos pos : changed) {
                refresh(level, center, pos);
            }
        }
        cachedLevel = level;
        cachedBounds = bounds;
        regions = currentRegions;
        workingSections = sections;
        sections = currentSections;
        if (workingSections != null) {
            workingSections.clear();
        }

        if (noTarget && !rebuild && changed == null) {
            return null;
        }

        noTarget = false;
        while (true) {
            Targets candidates = growth;
            if (immature.isEmpty()) {
                candidates = seeds;
            }
            Target target = candidates.random(level.random);
            if (target == null) {
                noTarget = true;
                return null;
            }

            refresh(level, center, target.pos());
            if (candidates.contains(target)) {
                return target;
            }
        }
    }

    void clear() {
        immature.clear();
        growth.clear();
        seeds.clear();
        regions = List.of();
        sections = null;
        workingSections = null;
        cachedLevel = null;
        cachedBounds = null;
        noTarget = false;
    }

    private void refresh(ServerLevel level, BlockPos center, BlockPos pos) {
        immature.remove(pos);
        for (Direction facing : Iterate.directions) {
            Target target = new Target(pos, facing);
            growth.remove(target);
            seeds.remove(target);
        }
        if (!level.isLoaded(pos) || !AmethystSoundSuppressorCrystalGrowth.isInRange(center, SubLevelBridge.resolve(level, pos).blockPos())) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ExperienceCrystalBlock crystal) {
            if (crystal.isMature()) {
                return;
            }

            immature.add(pos);
            Direction facing = state.getValue(ExperienceCrystalBlock.FACING);
            BlockPos support = pos.relative(facing.getOpposite());
            if (level.isLoaded(support) && level.getBlockState(support).is(Blocks.SCULK)) {
                growth.add(new Target(pos, facing));
            }
            return;
        }

        if (!ExperienceCrystalBlock.canSeedIn(state)) {
            return;
        }

        for (Direction facing : Iterate.directions) {
            BlockPos support = pos.relative(facing.getOpposite());
            if (!level.isLoaded(support) || !level.getBlockState(support).is(Blocks.SCULK)) {
                continue;
            }

            seeds.add(new Target(pos, facing));
        }
    }

    record Target(BlockPos pos, Direction facing) {}

    private record SectionView(BlockPos origin, ExperienceCrystalSectionIndex index, long revision) {}

    private static final class Targets {
        private final List<Target> values = new ArrayList<>();
        private final Map<Target, Integer> indices = new HashMap<>();

        private void add(Target target) {
            if (indices.containsKey(target)) {
                return;
            }

            indices.put(target, values.size());
            values.add(target);
        }

        private void remove(Target target) {
            Integer index = indices.remove(target);
            if (index == null) {
                return;
            }

            Target last = values.removeLast();
            if (index == values.size()) {
                return;
            }

            values.set(index, last);
            indices.put(last, index);
        }

        private boolean contains(Target target) {
            return indices.containsKey(target);
        }

        private @Nullable Target random(RandomSource random) {
            if (values.isEmpty()) {
                return null;
            }

            return values.get(random.nextInt(values.size()));
        }

        private void clear() {
            values.clear();
            indices.clear();
        }
    }
}
