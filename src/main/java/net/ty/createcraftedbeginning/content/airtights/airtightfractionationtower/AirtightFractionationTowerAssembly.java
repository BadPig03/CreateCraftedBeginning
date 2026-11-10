package net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.foundation.transaction.TransactionParticipant;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightFractionationTowerAssembly implements TransactionParticipant<List<BlockSnapshot>> {
    private static final int RESTORE_FLAGS = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_MOVE_BY_PISTON;

    private final Level level;
    private final List<AirtightTankBlockEntity> tanks;
    private final BlockPos origin;
    private final int height;

    AirtightFractionationTowerAssembly(Level level, List<AirtightTankBlockEntity> tanks, BlockPos origin, int height) {
        this.level = level;
        this.tanks = List.copyOf(tanks);
        this.origin = origin.immutable();
        this.height = height;
    }

    @Override
    public boolean validate() {
        return true;
    }

    @Override
    public List<BlockSnapshot> snapshot() {
        List<BlockSnapshot> snapshots = new ArrayList<>(tanks.size());
        for (AirtightTankBlockEntity tank : tanks) {
            BlockPos pos = tank.getBlockPos();
            BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
            Objects.requireNonNull(snapshot.getTag(), "Airtight fractionation tower assembly must capture tank data at " + pos + '.');
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    @Override
    public boolean execute() {
        for (AirtightTankBlockEntity tank : tanks) {
            tank.removeController(false);
        }
        BlockState towerState = CCBBlocks.AIRTIGHT_FRACTIONATION_TOWER_BLOCK.getDefaultState();
        for (AirtightTankBlockEntity tank : tanks) {
            BlockPos pos = tank.getBlockPos();
            int layer = pos.getY() - origin.getY();
            if (!level.setBlockAndUpdate(pos, towerState.setValue(AirtightFractionationTowerBlock.TOP, layer == height - 1).setValue(AirtightFractionationTowerBlock.BOTTOM, layer == 0))) {
                return false;
            }

            if (!(level.getBlockEntity(pos) instanceof AirtightFractionationTowerBlockEntity tower)) {
                throw new IllegalStateException("Failed to initialize airtight fractionation tower at " + pos + '.');
            }

            tower.assemble(origin, height);
        }
        BlockPos controllerPos = origin.offset(1, 0, 1);
        if (!(level.getBlockEntity(controllerPos) instanceof AirtightFractionationTowerBlockEntity controller)) {
            throw new IllegalStateException("Missing airtight fractionation tower controller at " + controllerPos + '.');
        }

        controller.getStructureManager().tick();
        return true;
    }

    @Override
    public void restore(List<BlockSnapshot> snapshots) {
        List<Throwable> failures = new ArrayList<>();
        for (BlockSnapshot snapshot : snapshots) {
            try {
                if (!(level.getBlockEntity(snapshot.getPos()) instanceof AirtightFractionationTowerBlockEntity tower)) {
                    continue;
                }

                tower.loadStructure(null, 0);
            }
            catch (RuntimeException | Error failure) {
                failures.add(failure);
            }
        }
        for (BlockSnapshot snapshot : snapshots) {
            BlockPos pos = snapshot.getPos();
            BlockState state = snapshot.getState();
            try {
                if (level.getBlockEntity(pos) instanceof AirtightFractionationTowerBlockEntity tower) {
                    tower.clearAssemblyInventory();
                    tower.invalidateCapabilities();
                }
                level.setBlock(pos, state, RESTORE_FLAGS);
                if (level.getBlockState(pos) != state) {
                    throw new IllegalStateException("Failed to restore airtight tank block at " + pos + '.');
                }
            }
            catch (RuntimeException | Error failure) {
                failures.add(failure);
            }
        }
        List<AirtightTankBlockEntity> restoredTanks = new ArrayList<>(snapshots.size());
        for (BlockSnapshot snapshot : snapshots) {
            BlockPos pos = snapshot.getPos();
            try {
                if (level.getBlockState(pos) != snapshot.getState() || !(level.getBlockEntity(pos) instanceof AirtightTankBlockEntity tank)) {
                    throw new IllegalStateException("Missing restored airtight tank block entity at " + pos + '.');
                }

                CompoundTag tag = Objects.requireNonNull(snapshot.getTag(), "Missing airtight tank recovery data at " + pos + '.');
                tank.loadForAssemblyRecovery(tag, level.registryAccess());
                restoredTanks.add(tank);
            }
            catch (RuntimeException | Error failure) {
                failures.add(failure);
            }
        }
        for (AirtightTankBlockEntity tank : restoredTanks) {
            try {
                BlockPos pos = tank.getBlockPos();
                BlockState state = tank.getBlockState();
                tank.invalidateCapabilities();
                tank.setChanged();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
                level.updateNeighborsAt(pos, state.getBlock());
            }
            catch (RuntimeException | Error failure) {
                failures.add(failure);
            }
        }
        if (failures.isEmpty()) {
            return;
        }

        IllegalStateException recoveryFailure = new IllegalStateException("Failed to fully restore airtight fractionation tower assembly at " + origin + '.');
        failures.forEach(recoveryFailure::addSuppressed);
        throw recoveryFailure;
    }
}
