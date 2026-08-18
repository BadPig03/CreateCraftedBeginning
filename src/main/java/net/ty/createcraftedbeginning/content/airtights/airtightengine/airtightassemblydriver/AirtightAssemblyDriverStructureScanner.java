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
    private static void scanTankPosition(AirtightTankBlockEntity controller, Level level, Block controllerBlock, BlockPos controllerPos, BlockPos pos, Set<BlockPos> visitedPositions, ScanAccumulator accumulator) {
        if (visitedPositions.contains(pos)) {
            return;
        }

        if (!level.isLoaded(pos)) {
            accumulator.complete = false;
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != controllerBlock) {
            accumulator.structureValid = false;
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof AirtightTankBlockEntity tank) || tank.getType() != controller.getType() || tank.isRemoved() || !tank.getController().equals(controllerPos)) {
            accumulator.complete = false;
            return;
        }

        visitedPositions.add(pos);
        scanAttachedBlocks(pos, level, accumulator);
        scanChamberBlock(pos, level, accumulator);
    }

    private static void scanAttachedBlocks(BlockPos pos, Level level, ScanAccumulator accumulator) {
        for (Direction direction : Iterate.directions) {
            BlockPos attachedPos = pos.relative(direction);
            if (!level.isLoaded(attachedPos)) {
                accumulator.complete = false;
                continue;
            }

            BlockState attachedState = level.getBlockState(attachedPos);
            Block attachedBlock = attachedState.getBlock();
            if (attachedBlock instanceof AirtightEngineBlock && AirtightEngineBlock.getFacing(attachedState).getOpposite() == direction) {
                accumulator.attachedEngines++;
            }

            if (attachedBlock instanceof ResidueOutletBlock && ResidueOutletBlock.getFacing(attachedState).getOpposite() == direction) {
                accumulator.attachedOutlets++;
                accumulator.outletPositions.add(attachedPos);
            }
        }
    }

    private static void scanChamberBlock(BlockPos pos, Level level, ScanAccumulator accumulator) {
        BlockPos attachedPos = pos.above();
        if (!level.isLoaded(attachedPos)) {
            accumulator.complete = false;
            return;
        }

        BlockState attachedState = level.getBlockState(attachedPos);
        if (!(attachedState.getBlock() instanceof BreezeChamberBlock)) {
            return;
        }

        if (!(level.getBlockEntity(attachedPos) instanceof BreezeChamberBlockEntity chamber)) {
            accumulator.complete = false;
            return;
        }

        accumulator.attachedChambers++;
        accumulator.attachedWindChargingLevel += chamber.getWindRemainingLevel();
    }

    ScanResult scan(AirtightTankBlockEntity controller, Level level) {
        ScanAccumulator accumulator = new ScanAccumulator();
        Set<BlockPos> visitedPositions = new HashSet<>();
        BlockPos controllerPos = controller.getBlockPos();
        Block controllerBlock = controller.getBlockState().getBlock();
        Axis axis = controller.getMainConnectionAxis();
        int width = controller.getWidth();
        int length = controller.getHeight();

        for (int lengthOffset = 0; lengthOffset < length; lengthOffset++) {
            for (int uOffset = 0; uOffset < width; uOffset++) {
                for (int vOffset = 0; vOffset < width; vOffset++) {
                    BlockPos pos = AirtightTankBlockEntity.offsetInMulti(controllerPos, axis, lengthOffset, uOffset, vOffset);
                    scanTankPosition(controller, level, controllerBlock, controllerPos, pos, visitedPositions, accumulator);
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
