package net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightFractionationTowerStructure {
    private static final String INVALID_SIZE = "invalid_size";
    private static final String GAS_INSIDE = "gas_inside";
    private static final String ALREADY_ASSEMBLED = "already_assembled";
    private static final String INVALID_STRUCTURE = "invalid_structure";

    private AirtightFractionationTowerStructure() {
    }

    static @Nullable AssemblyFailure assemble(Level level, BlockPos clickedPos, Player player) {
        if (!player.mayBuild() || !level.mayInteract(player, clickedPos)) {
            return new AssemblyFailure(clickedPos, clickedPos, null, true);
        }

        BlockState clickedState = level.getBlockState(clickedPos);
        if (clickedState.is(CCBBlocks.AIRTIGHT_FRACTIONATION_TOWER_BLOCK.get())) {
            return new AssemblyFailure(clickedPos, clickedPos, ALREADY_ASSEMBLED, false);
        }

        if (clickedState.is(CCBBlocks.HORIZONTAL_AIRTIGHT_TANK_BLOCK.get())) {
            return new AssemblyFailure(clickedPos, clickedPos, INVALID_STRUCTURE, false);
        }

        if (!clickedState.is(CCBBlocks.AIRTIGHT_TANK_BLOCK.get())) {
            return new AssemblyFailure(clickedPos, clickedPos, null, true);
        }

        Set<BlockPos> positions = new HashSet<>();
        Deque<BlockPos> pending = new ArrayDeque<>();
        BlockPos immutableClickedPos = clickedPos.immutable();
        pending.add(immutableClickedPos);
        positions.add(immutableClickedPos);
        int minX = clickedPos.getX();
        int minY = clickedPos.getY();
        int minZ = clickedPos.getZ();
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;
        int width = AirtightFractionationTowerBlock.WIDTH;
        List<AirtightTankBlockEntity> tanks = new ArrayList<>();
        while (!pending.isEmpty()) {
            BlockPos pos = pending.removeFirst();
            if (!level.isLoaded(pos)) {
                return new AssemblyFailure(pos, pos, INVALID_STRUCTURE, false);
            }

            if (!level.getBlockState(pos).is(CCBBlocks.AIRTIGHT_TANK_BLOCK.get()) || !(level.getBlockEntity(pos) instanceof AirtightTankBlockEntity part)) {
                return new AssemblyFailure(pos, pos, INVALID_STRUCTURE, false);
            }

            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
            BlockPos min = new BlockPos(minX, minY, minZ);
            BlockPos max = new BlockPos(maxX, maxY, maxZ);
            if (maxX - minX + 1 > width || maxZ - minZ + 1 > width) {
                return new AssemblyFailure(min, max, INVALID_SIZE, false);
            }

            if (maxY - minY + 1 > AirtightFractionationTowerBlock.MAX_HEIGHT) {
                return new AssemblyFailure(min, max, INVALID_SIZE, false);
            }

            tanks.add(part);
            for (Direction direction : Iterate.directions) {
                BlockPos neighbor = pos.relative(direction);
                if (!level.isLoaded(neighbor)) {
                    return new AssemblyFailure(min, max, INVALID_STRUCTURE, false);
                }

                if (!level.getBlockState(neighbor).is(CCBBlocks.AIRTIGHT_TANK_BLOCK.get()) || !positions.add(neighbor)) {
                    continue;
                }

                pending.addLast(neighbor);
            }
        }

        int height = maxY - minY + 1;
        BlockPos origin = new BlockPos(minX, minY, minZ);
        BlockPos end = new BlockPos(maxX, maxY, maxZ);
        if (maxX - minX + 1 != width || maxZ - minZ + 1 != width) {
            return new AssemblyFailure(origin, end, INVALID_SIZE, false);
        }

        if (height < AirtightFractionationTowerBlock.MIN_HEIGHT) {
            return new AssemblyFailure(origin, end, INVALID_SIZE, false);
        }

        if (tanks.size() != width * width * height) {
            return new AssemblyFailure(origin, end, INVALID_STRUCTURE, false);
        }

        for (AirtightTankBlockEntity part : tanks) {
            if (!level.mayInteract(player, part.getBlockPos())) {
                return new AssemblyFailure(origin, end, INVALID_STRUCTURE, false);
            }

            if (!part.getGas(0).isEmpty()) {
                return new AssemblyFailure(origin, end, GAS_INSIDE, false);
            }
        }

        for (AirtightTankBlockEntity part : tanks) {
            part.removeController(false);
        }
        BlockState towerState = CCBBlocks.AIRTIGHT_FRACTIONATION_TOWER_BLOCK.getDefaultState();
        for (AirtightTankBlockEntity part : tanks) {
            BlockPos pos = part.getBlockPos();
            int layer = pos.getY() - origin.getY();
            level.setBlockAndUpdate(pos, towerState.setValue(AirtightFractionationTowerBlock.TOP, layer == height - 1).setValue(AirtightFractionationTowerBlock.BOTTOM, layer == 0));
            if (!(level.getBlockEntity(pos) instanceof AirtightFractionationTowerBlockEntity tower)) {
                throw new IllegalStateException("Failed to initialize airtight fractionation tower at " + pos + '.');
            }

            tower.assemble(origin, height);
        }
        CCBAdvancements.DISSOCIATIVE_RECOMBINATION.awardTo(player);
        if (height != AirtightFractionationTowerBlock.MAX_HEIGHT) {
            return null;
        }

        CCBAdvancements.BREAK_THROUGH_THE_DOME.awardTo(player);
        return null;
    }

    static void disassemble(Level level, BlockPos removedPos, AirtightFractionationTowerBlockEntity removedTower) {
        BlockPos origin = removedTower.getOrigin();
        if (origin == null) {
            return;
        }

        int height = removedTower.getHeight();
        List<BlockPos> remaining = new ArrayList<>();
        BlockPos end = origin.offset(2, height - 1, 2);
        int minChunkX = SectionPos.blockToSectionCoord(origin.getX());
        int maxChunkX = SectionPos.blockToSectionCoord(end.getX());
        int minChunkZ = SectionPos.blockToSectionCoord(origin.getZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(end.getZ());
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }
        List<GasReleaseRequest> gasReleases = new ArrayList<>(removedTower.getInventory().dropItemsAndCollectGasReleases(level, removedPos));
        removedTower.clearStructure();
        for (BlockPos pos : BlockPos.betweenClosed(origin, end)) {
            if (pos.equals(removedPos) || !(level.getBlockEntity(pos) instanceof AirtightFractionationTowerBlockEntity tower) || !origin.equals(tower.getOrigin())) {
                continue;
            }

            gasReleases.addAll(tower.getInventory().dropItemsAndCollectGasReleases(level, removedPos));
            tower.clearStructure();
            remaining.add(pos.immutable());
        }
        for (BlockPos pos : remaining) {
            level.setBlock(pos, CCBBlocks.AIRTIGHT_TANK_BLOCK.getDefaultState(), Block.UPDATE_ALL);
        }
        Block.popResource(level, removedPos, new ItemStack(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get()));
        for (GasReleaseRequest release : gasReleases) {
            GasReleaseService.release(level, release);
        }
    }

    record AssemblyFailure(BlockPos min, BlockPos max, @Nullable String reason, boolean failed) {
        AssemblyFailure {
            min = min.immutable();
            max = max.immutable();
        }
    }
}
