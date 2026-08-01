package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.GasConnectivityHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

import static net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore.MAX_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightAssemblyDriverStructureManager {
    private static final String COMPOUND_KEY_ATTACHED_ENGINES = "AttachedEngines";
    private static final String COMPOUND_KEY_ATTACHED_OUTLETS = "AttachedOutlets";
    private static final String COMPOUND_KEY_ATTACHED_CHAMBERS = "AttachedChambers";
    private static final String COMPOUND_KEY_ATTACHED_WIND_CHARGING_LEVEL = "AttachedWindChargingLevel";
    private static final String COMPOUND_KEY_STRUCTURE_VALID = "StructureValid";

    private static final int INITIAL_EVALUATION_DELAY = 2;
    private static final int INCOMPLETE_EVALUATION_RETRY_DELAY = 20;
    private static int getMaxAttachedSurfaceBlocks() {
        long width = AirtightTankBlockEntity.getConfiguredMaxWidth();
        long length = AirtightTankBlockEntity.getConfiguredMaxLength();
        long surfaceArea = 2L * (width * width + 2L * width * length);
        return Math.clamp(surfaceArea, 0, Integer.MAX_VALUE);
    }

    private static int getMaxAttachedChambers() {
        long width = AirtightTankBlockEntity.getConfiguredMaxWidth();
        long length = AirtightTankBlockEntity.getConfiguredMaxLength();
        return Math.clamp(width * Math.max(width, length), 0, Integer.MAX_VALUE);
    }

    private static int getMaxAttachedWindChargingLevel() {
        return Math.clamp((long) getMaxAttachedChambers() * MAX_LEVEL, 0, Integer.MAX_VALUE);
    }

    private final AirtightAssemblyDriverCore driverCore;

    private boolean structureValid;
    private boolean evaluationRequired = true;
    private int evaluationCooldown;
    private int attachedChambers;
    private int attachedWindChargingLevel;
    private int attachedEngines;
    private int attachedOutlets;

    public AirtightAssemblyDriverStructureManager(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    private static int readBoundedInt(CompoundTag compoundTag, String key, int max) {
        return compoundTag.contains(key) ? Mth.clamp(compoundTag.getInt(key), 0, max) : 0;
    }

    public void tick(AirtightTankBlockEntity controller) {
        if (!evaluationRequired) {
            return;
        }
        if (evaluationCooldown > 0) {
            evaluationCooldown--;
            return;
        }

        evaluate(controller);
        if (evaluationRequired) {
            evaluationCooldown = INCOMPLETE_EVALUATION_RETRY_DELAY;
        }
    }

    public void requestEvaluation() {
        evaluationRequired = true;
        evaluationCooldown = 0;
    }

    public void evaluate(AirtightTankBlockEntity controller) {
        Level level = controller.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        int previousEngines = attachedEngines;
        int previousOutlets = attachedOutlets;
        int previousChambers = attachedChambers;
        int previousWindLevel = attachedWindChargingLevel;
        boolean wasStructureValid = structureValid;

        clearDerivedState();
        structureValid = true;
        evaluationRequired = false;
        evaluationCooldown = 0;

        Set<BlockPos> outletPositions = new HashSet<>();
        boolean isScanComplete = scanMultiblockStructure(controller, level, outletPositions);
        if (!isScanComplete) {
            clearDerivedState();
            outletPositions.clear();
            evaluationRequired = true;
        }
        else if (previousOutlets > attachedOutlets) {
            driverCore.getResidueManager().applyRemovalPenalty(false);
        }

        driverCore.getResidueManager().updateOutletsPositions(outletPositions);
        driverCore.getLevelCalculator().updateWindChargingLevel(attachedWindChargingLevel);

        boolean hasChanged = previousEngines != attachedEngines || previousOutlets != attachedOutlets || previousChambers != attachedChambers || previousWindLevel != attachedWindChargingLevel || wasStructureValid != structureValid;
        if (hasChanged) {
            driverCore.markForClientSync();
        }
    }

    private boolean scanMultiblockStructure(AirtightTankBlockEntity controller, Level level, Set<BlockPos> outletPositions) {
        Set<BlockPos> visitedPositions = new HashSet<>();
        BlockPos controllerPos = controller.getBlockPos();
        Block controllerBlock = controller.getBlockState().getBlock();
        Axis axis = controller.getMainConnectionAxis();
        int width = controller.getWidth();
        int length = controller.getHeight();
        int chamberLevels = 0;
        boolean isScanComplete = true;
        for (int lengthOffset = 0; lengthOffset < length; lengthOffset++) {
            for (int uOffset = 0; uOffset < width; uOffset++) {
                for (int vOffset = 0; vOffset < width; vOffset++) {
                    BlockPos pos = AirtightTankBlockEntity.offsetInMulti(controllerPos, axis, lengthOffset, uOffset, vOffset);
                    TankScanResult result = scanTankPosition(controller, level, controllerBlock, controllerPos, pos, visitedPositions, outletPositions);
                    if (!result.complete()) {
                        isScanComplete = false;
                    }
                    chamberLevels += result.chamberLevel();
                }
            }
        }

        attachedWindChargingLevel = Math.max(0, chamberLevels);
        return isScanComplete;
    }

    private TankScanResult scanTankPosition(AirtightTankBlockEntity controller, Level level, Block controllerBlock, BlockPos controllerPos, BlockPos pos, Set<BlockPos> visitedPositions, Set<BlockPos> outletPositions) {
        if (visitedPositions.contains(pos)) {
            return TankScanResult.COMPLETE;
        }
        if (!level.isLoaded(pos)) {
            return TankScanResult.INCOMPLETE;
        }

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != controllerBlock) {
            structureValid = false;
            return TankScanResult.COMPLETE;
        }

        AirtightTankBlockEntity tank = GasConnectivityHandler.partAt(controller.getType(), level, pos);
        if (tank == null || !tank.getController().equals(controllerPos)) {
            return TankScanResult.INCOMPLETE;
        }

        visitedPositions.add(pos);
        boolean isComplete = scanAttachedBlocks(pos, level, outletPositions);
        int chamberLevel = scanChamberBlocks(pos, level);
        if (chamberLevel < 0) {
            return TankScanResult.INCOMPLETE;
        }

        return new TankScanResult(isComplete, chamberLevel);
    }

    private boolean scanAttachedBlocks(BlockPos pos, Level level, Set<BlockPos> outletPositions) {
        boolean scanComplete = true;
        for (Direction direction : Iterate.directions) {
            BlockPos attachedPos = pos.relative(direction);
            if (!level.isLoaded(attachedPos)) {
                scanComplete = false;
                continue;
            }

            BlockState attachedState = level.getBlockState(attachedPos);
            Block attachedBlock = attachedState.getBlock();
            boolean isEngine = attachedBlock instanceof AirtightEngineBlock && AirtightEngineBlock.getFacing(attachedState).getOpposite() == direction;
            if (isEngine) {
                attachedEngines++;
            }

            boolean isOutlet = attachedBlock instanceof ResidueOutletBlock && ResidueOutletBlock.getFacing(attachedState).getOpposite() == direction;
            if (isOutlet) {
                attachedOutlets++;
                outletPositions.add(attachedPos);
            }
        }
        return scanComplete;
    }

    private int scanChamberBlocks(BlockPos pos, Level level) {
        BlockPos attachedPos = pos.above();
        if (!level.isLoaded(attachedPos)) {
            return -1;
        }

        BlockState attachedState = level.getBlockState(attachedPos);
        if (!(attachedState.getBlock() instanceof BreezeChamberBlock)) {
            return 0;
        }
        if (!(level.getBlockEntity(attachedPos) instanceof BreezeChamberBlockEntity chamber)) {
            return -1;
        }

        attachedChambers++;
        return chamber.getWindRemainingLevel();
    }

    public void reset() {
        boolean hasChanged = attachedEngines != 0 || attachedOutlets != 0 || attachedChambers != 0 || attachedWindChargingLevel != 0 || structureValid;
        clearDerivedState();
        evaluationRequired = true;
        evaluationCooldown = 0;
        driverCore.getResidueManager().clearOutletsPositions();
        driverCore.getLevelCalculator().updateWindChargingLevel(0);
        if (hasChanged) {
            driverCore.markForClientSync();
        }
    }

    public void invalidateForServerLoad() {
        clearDerivedState();
        evaluationRequired = true;
        evaluationCooldown = INITIAL_EVALUATION_DELAY;
        driverCore.getResidueManager().clearOutletsPositions();
        driverCore.getLevelCalculator().loadWindChargingLevel(0);
    }

    public boolean isEvaluationRequired() {
        return evaluationRequired;
    }

    public boolean isAssembled() {
        return attachedEngines > 0 && structureValid;
    }

    public boolean isActive() {
        return isAssembled() && attachedOutlets > 0 && attachedWindChargingLevel > 0;
    }

    public int getAttachedEngines() {
        return attachedEngines;
    }

    public int getAttachedOutlets() {
        return attachedOutlets;
    }

    public CompoundTag writeClient() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(COMPOUND_KEY_ATTACHED_ENGINES, attachedEngines);
        tag.putInt(COMPOUND_KEY_ATTACHED_OUTLETS, attachedOutlets);
        tag.putInt(COMPOUND_KEY_ATTACHED_CHAMBERS, attachedChambers);
        tag.putInt(COMPOUND_KEY_ATTACHED_WIND_CHARGING_LEVEL, attachedWindChargingLevel);
        tag.putBoolean(COMPOUND_KEY_STRUCTURE_VALID, structureValid);
        return tag;
    }

    public void readClient(CompoundTag compoundTag) {
        int maxAttachedSurfaceBlocks = getMaxAttachedSurfaceBlocks();
        attachedEngines = readBoundedInt(compoundTag, COMPOUND_KEY_ATTACHED_ENGINES, maxAttachedSurfaceBlocks);
        attachedOutlets = readBoundedInt(compoundTag, COMPOUND_KEY_ATTACHED_OUTLETS, maxAttachedSurfaceBlocks);
        attachedChambers = readBoundedInt(compoundTag, COMPOUND_KEY_ATTACHED_CHAMBERS, getMaxAttachedChambers());
        attachedWindChargingLevel = readBoundedInt(compoundTag, COMPOUND_KEY_ATTACHED_WIND_CHARGING_LEVEL, getMaxAttachedWindChargingLevel());
        structureValid = compoundTag.getBoolean(COMPOUND_KEY_STRUCTURE_VALID);
        evaluationRequired = false;
        evaluationCooldown = 0;
    }

    private void clearDerivedState() {
        attachedEngines = 0;
        attachedOutlets = 0;
        attachedChambers = 0;
        attachedWindChargingLevel = 0;
        structureValid = false;
    }

    private record TankScanResult(boolean complete, int chamberLevel) {
        private static final TankScanResult COMPLETE = new TankScanResult(true, 0);
        private static final TankScanResult INCOMPLETE = new TankScanResult(false, 0);
    }
}
