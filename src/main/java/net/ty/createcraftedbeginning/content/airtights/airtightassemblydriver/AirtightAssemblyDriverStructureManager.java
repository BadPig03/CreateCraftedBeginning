package net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlock;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class AirtightAssemblyDriverStructureManager {
    private static final String COMPOUND_KEY_ATTACHED_ENGINES = "AttachedEngines";
    private static final String COMPOUND_KEY_ATTACHED_OUTLETS = "AttachedOutlets";
    private static final String COMPOUND_KEY_ATTACHED_CHAMBERS = "AttachedChambers";
    private static final String COMPOUND_KEY_STRUCTURE_VALID = "StructureValid";

    private final AirtightAssemblyDriverCore driverCore;
    private boolean structureValid;
    private int attachedChambers;
    private int attachedEngines;
    private int attachedOutlets;
    private int previousChambers = -1;
    private int previousEngines = -1;
    private int previousOutlets = -1;

    public AirtightAssemblyDriverStructureManager(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    public boolean evaluate(@NotNull AirtightTankBlockEntity controller) {
        Level level = controller.getLevel();
        if (level == null) {
            return false;
        }

        previousEngines = attachedEngines;
        previousOutlets = attachedOutlets;
        previousChambers = attachedChambers;
        attachedEngines = 0;
        attachedOutlets = 0;
        attachedChambers = 0;
        structureValid = true;
        scanMultiblockStructure(controller.getBlockPos(), level, controller.getWidth(), controller.getHeight());
        if (previousOutlets > attachedOutlets) {
            driverCore.getResidueManager().applyRemovalPenalty(false);
        }
        return previousEngines != attachedEngines || previousOutlets != attachedOutlets || previousChambers != attachedChambers;
    }

    private void scanMultiblockStructure(BlockPos controllerPos, Level level, int width, int height) {
        Set<BlockPos> visitedPositions = new HashSet<>();
        Set<BlockPos> outletsPositions = new HashSet<>();
        int chamberLevels = 0;
        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = controllerPos.offset(xOffset, yOffset, zOffset);
                    if (visitedPositions.contains(pos)) {
                        continue;
                    }

                    BlockState blockState = level.getBlockState(pos);
                    if (!AirtightTankBlock.isTank(blockState)) {
                        structureValid = false;
                        continue;
                    }

                    visitedPositions.add(pos);
                    outletsPositions.addAll(scanAttachedBlocks(pos, level));
                    chamberLevels += scanChamberBlocks(pos, level);
                }
            }
        }
        driverCore.getResidueManager().updateOutletsPositions(outletsPositions);
        driverCore.getLevelCalculator().updateWindChargingLevel(chamberLevels);
    }

    private @NotNull Set<BlockPos> scanAttachedBlocks(@NotNull BlockPos pos, @NotNull Level level) {
        Set<BlockPos> outlets = new HashSet<>();
        for (Direction direction : Iterate.directions) {
            BlockPos attachedPos = pos.relative(direction);
            BlockState attachedState = level.getBlockState(attachedPos);
            Block attachedBlock = attachedState.getBlock();
            if (attachedBlock instanceof AirtightEngineBlock && AirtightEngineBlock.getFacing(attachedState).getOpposite() == direction) {
                attachedEngines++;
            }
            if (attachedBlock instanceof ResidueOutletBlock && ResidueOutletBlock.getFacing(attachedState).getOpposite() == direction) {
                attachedOutlets++;
                outlets.add(attachedPos);
            }
        }
        return outlets;
    }

    private int scanChamberBlocks(@NotNull BlockPos pos, @NotNull Level level) {
        BlockPos attachedPos = pos.above();
        BlockState attachedState = level.getBlockState(attachedPos);
        Block attachedBlock = attachedState.getBlock();
        if (!(attachedBlock instanceof BreezeChamberBlock) || !(level.getBlockEntity(attachedPos) instanceof BreezeChamberBlockEntity chamber)) {
            return 0;
        }

        attachedChambers++;
        return chamber.getWindRemainingLevel();
    }

    public void reset() {
        attachedEngines = 0;
        attachedOutlets = 0;
        attachedChambers = 0;
        structureValid = false;
        previousEngines = -1;
        previousOutlets = -1;
        previousChambers = -1;
    }

    public boolean isActive() {
        return attachedEngines > 0 && structureValid;
    }

    public int getAttachedEngines() {
        return attachedEngines;
    }

    public int getAttachedOutlets() {
        return attachedOutlets;
    }

    public CompoundTag write() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putInt(COMPOUND_KEY_ATTACHED_ENGINES, attachedEngines);
        compoundTag.putInt(COMPOUND_KEY_ATTACHED_OUTLETS, attachedOutlets);
        compoundTag.putInt(COMPOUND_KEY_ATTACHED_CHAMBERS, attachedChambers);
        compoundTag.putBoolean(COMPOUND_KEY_STRUCTURE_VALID, structureValid);
        return compoundTag;
    }

    public void read(@NotNull CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_ATTACHED_ENGINES)) {
            attachedEngines = compoundTag.getInt(COMPOUND_KEY_ATTACHED_ENGINES);
        }
        if (compoundTag.contains(COMPOUND_KEY_ATTACHED_OUTLETS)) {
            attachedOutlets = compoundTag.getInt(COMPOUND_KEY_ATTACHED_OUTLETS);
        }
        if (compoundTag.contains(COMPOUND_KEY_ATTACHED_CHAMBERS)) {
            attachedChambers = compoundTag.getInt(COMPOUND_KEY_ATTACHED_CHAMBERS);
        }
        if (compoundTag.contains(COMPOUND_KEY_STRUCTURE_VALID)) {
            structureValid = compoundTag.getBoolean(COMPOUND_KEY_STRUCTURE_VALID);
        }
    }
}
