package net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.GasConnectivityHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightAssemblyDriverStructureManager {
    private static final String COMPOUND_KEY_ATTACHED_ENGINES = "AttachedEngines";
    private static final String COMPOUND_KEY_ATTACHED_OUTLETS = "AttachedOutlets";
    private static final String COMPOUND_KEY_ATTACHED_CHAMBERS = "AttachedChambers";
    private static final String COMPOUND_KEY_ATTACHED_WIND_CHARGING_LEVEL = "AttachedWindChargingLevel";
    private static final String COMPOUND_KEY_STRUCTURE_VALID = "StructureValid";

    private final AirtightAssemblyDriverCore driverCore;
    private boolean structureValid;
    private int attachedChambers;
    private int attachedWindChargingLevel;
    private int attachedEngines;
    private int attachedOutlets;
    private int previousChambers = -1;
    private int previousEngines = -1;
    private int previousOutlets = -1;

    public AirtightAssemblyDriverStructureManager(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    public boolean evaluate(AirtightTankBlockEntity controller) {
        Level level = controller.getLevel();
        if (level == null) {
            return false;
        }

        int previousWindChargingLevel = attachedWindChargingLevel;
        boolean previousStructureValid = structureValid;
        previousEngines = attachedEngines;
        previousOutlets = attachedOutlets;
        previousChambers = attachedChambers;
        attachedEngines = 0;
        attachedOutlets = 0;
        attachedChambers = 0;
        attachedWindChargingLevel = 0;
        structureValid = true;
        scanMultiblockStructure(controller, level);
        if (previousOutlets > attachedOutlets) {
            driverCore.getResidueManager().applyRemovalPenalty(false);
        }
        boolean changed = previousEngines != attachedEngines || previousOutlets != attachedOutlets || previousChambers != attachedChambers || previousWindChargingLevel != attachedWindChargingLevel || previousStructureValid != structureValid;
        if (changed) {
            driverCore.markDirty();
        }
        return changed;
    }

    private void scanMultiblockStructure(AirtightTankBlockEntity controller, Level level) {
        Set<BlockPos> visitedPositions = new HashSet<>();
        Set<BlockPos> outletsPositions = new HashSet<>();

        BlockPos controllerPos = controller.getBlockPos();
        Axis axis = controller.getMainConnectionAxis();
        int width = controller.getWidth();
        int length = controller.getHeight();
        int chamberLevels = 0;
        for (int lengthOffset = 0; lengthOffset < length; lengthOffset++) {
            for (int uOffset = 0; uOffset < width; uOffset++) {
                for (int vOffset = 0; vOffset < width; vOffset++) {
                    BlockPos pos = AirtightTankBlockEntity.offsetInMulti(controllerPos, axis, lengthOffset, uOffset, vOffset);
                    if (visitedPositions.contains(pos)) {
                        continue;
                    }

                    AirtightTankBlockEntity tank = GasConnectivityHandler.partAt(controller.getType(), level, pos);
                    if (tank == null || !tank.getController().equals(controllerPos)) {
                        structureValid = false;
                        continue;
                    }

                    visitedPositions.add(pos);
                    outletsPositions.addAll(scanAttachedBlocks(pos, level));
                    chamberLevels += scanChamberBlocks(pos, level);
                }
            }
        }

        attachedWindChargingLevel = chamberLevels;
        driverCore.getResidueManager().updateOutletsPositions(outletsPositions);
        driverCore.getLevelCalculator().updateWindChargingLevel(chamberLevels);
    }

    private Set<BlockPos> scanAttachedBlocks(BlockPos pos, Level level) {
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

    private int scanChamberBlocks(BlockPos pos, Level level) {
        BlockPos attachedPos = pos.above();
        if (!(level.getBlockEntity(attachedPos) instanceof BreezeChamberBlockEntity chamber)) {
            return 0;
        }

        attachedChambers++;
        return chamber.getWindRemainingLevel();
    }

    public void reset() {
        boolean changed = attachedEngines != 0 || attachedOutlets != 0 || attachedChambers != 0 || attachedWindChargingLevel != 0 || structureValid;
        attachedEngines = 0;
        attachedOutlets = 0;
        attachedChambers = 0;
        attachedWindChargingLevel = 0;
        structureValid = false;
        previousEngines = -1;
        previousOutlets = -1;
        previousChambers = -1;
        if (!changed) {
            return;
        }

        driverCore.markDirty();
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
        compoundTag.putInt(COMPOUND_KEY_ATTACHED_WIND_CHARGING_LEVEL, attachedWindChargingLevel);
        compoundTag.putBoolean(COMPOUND_KEY_STRUCTURE_VALID, structureValid);
        return compoundTag;
    }

    public void read(CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_ATTACHED_ENGINES)) {
            attachedEngines = compoundTag.getInt(COMPOUND_KEY_ATTACHED_ENGINES);
        }
        if (compoundTag.contains(COMPOUND_KEY_ATTACHED_OUTLETS)) {
            attachedOutlets = compoundTag.getInt(COMPOUND_KEY_ATTACHED_OUTLETS);
        }
        if (compoundTag.contains(COMPOUND_KEY_ATTACHED_CHAMBERS)) {
            attachedChambers = compoundTag.getInt(COMPOUND_KEY_ATTACHED_CHAMBERS);
        }
        if (compoundTag.contains(COMPOUND_KEY_ATTACHED_WIND_CHARGING_LEVEL)) {
            attachedWindChargingLevel = compoundTag.getInt(COMPOUND_KEY_ATTACHED_WIND_CHARGING_LEVEL);
        }
        if (compoundTag.contains(COMPOUND_KEY_STRUCTURE_VALID)) {
            structureValid = compoundTag.getBoolean(COMPOUND_KEY_STRUCTURE_VALID);
        }
    }
}
