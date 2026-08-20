package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightAssemblyDriverStructureScanner {
    private static void scanTankPosition(AirtightTankBlockEntity tankController, Level level, Block controllerBlock, BlockPos controllerPos, BlockPos tankPos, Set<BlockPos> visitedPositions, ScanAccumulator accumulator) {
        if (visitedPositions.contains(tankPos)) {
            return;
        }

        if (!level.isLoaded(tankPos)) {
            accumulator.complete = false;
            return;
        }

        BlockState tankState = level.getBlockState(tankPos);
        if (tankState.getBlock() != controllerBlock) {
            accumulator.structureValid = false;
            return;
        }

        if (!(level.getBlockEntity(tankPos) instanceof AirtightTankBlockEntity tank) || tank.getType() != tankController.getType() || tank.isRemoved() || !tank.getController().equals(controllerPos)) {
            accumulator.complete = false;
            return;
        }

        visitedPositions.add(tankPos);
        scanAttachedBlocks(tankPos, level, accumulator);
        scanChamberBlock(tankPos, level, accumulator);
    }

    private static void scanAttachedBlocks(BlockPos tankPos, Level level, ScanAccumulator accumulator) {
        for (Direction attachmentDirection : Iterate.directions) {
            BlockPos attachedPos = tankPos.relative(attachmentDirection);
            if (!level.isLoaded(attachedPos)) {
                accumulator.complete = false;
                continue;
            }

            BlockState attachedState = level.getBlockState(attachedPos);
            Block attachedBlock = attachedState.getBlock();
            if (attachedBlock instanceof AirtightEngineBlock && AirtightEngineBlock.getFacing(attachedState).getOpposite() == attachmentDirection) {
                accumulator.attachedEngines++;
            }

            if (attachedBlock instanceof ResidueOutletBlock && ResidueOutletBlock.getFacing(attachedState).getOpposite() == attachmentDirection) {
                accumulator.attachedOutlets++;
                accumulator.outletPositions.add(attachedPos);
            }
        }
    }

    private static void scanChamberBlock(BlockPos tankPos, Level level, ScanAccumulator accumulator) {
        BlockPos chamberPos = tankPos.above();
        if (!level.isLoaded(chamberPos)) {
            accumulator.complete = false;
            return;
        }

        BlockState chamberState = level.getBlockState(chamberPos);
        if (!(chamberState.getBlock() instanceof BreezeChamberBlock)) {
            return;
        }

        if (!(level.getBlockEntity(chamberPos) instanceof BreezeChamberBlockEntity chamber)) {
            accumulator.complete = false;
            return;
        }

        accumulator.attachedChambers++;
        accumulator.attachedWindChargingLevel += chamber.getWindRemainingLevel();
    }

    ScanResult scan(AirtightTankBlockEntity tankController, Level level) {
        ScanAccumulator accumulator = new ScanAccumulator();
        Set<BlockPos> visitedPositions = new HashSet<>();
        BlockPos controllerPos = tankController.getBlockPos();
        Block controllerBlock = tankController.getBlockState().getBlock();
        Axis tankAxis = tankController.getMainConnectionAxis();
        int tankWidth = tankController.getWidth();
        int tankLength = tankController.getHeight();

        for (int lengthOffset = 0; lengthOffset < tankLength; lengthOffset++) {
            for (int uOffset = 0; uOffset < tankWidth; uOffset++) {
                for (int vOffset = 0; vOffset < tankWidth; vOffset++) {
                    BlockPos tankPos = AirtightTankBlockEntity.offsetInMulti(controllerPos, tankAxis, lengthOffset, uOffset, vOffset);
                    scanTankPosition(tankController, level, controllerBlock, controllerPos, tankPos, visitedPositions, accumulator);
                }
            }
        }

        return accumulator.toResult();
    }

    record ScanResult(boolean complete, boolean structureValid, int attachedEngines, int attachedOutlets, int attachedChambers, int attachedWindChargingLevel, Set<BlockPos> outletPositions) {}

    private static final class ScanAccumulator {
        private final Set<BlockPos> outletPositions = new HashSet<>();
        private boolean complete = true;
        private boolean structureValid = true;
        private int attachedEngines;
        private int attachedOutlets;
        private int attachedChambers;
        private int attachedWindChargingLevel;

        private ScanResult toResult() {
            return new ScanResult(complete, structureValid, attachedEngines, attachedOutlets, attachedChambers, Math.max(0, attachedWindChargingLevel), Set.copyOf(outletPositions));
        }
    }
}
