package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirCompressorScenes {
    public static void gasProcessing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("air_compressor_gas_processing", "Processing Gases with Air Compressors");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos compressorPos = util.grid().at(3, 2, 3);
        BlockPos compressorMotorPos = compressorPos.above();
        BlockPos inputPipePos = compressorPos.east();
        BlockPos inputPumpPos = inputPipePos.east();
        BlockPos inputCogPos = inputPumpPos.south();
        BlockPos inputMotorPos = inputCogPos.east();
        BlockPos inputTankPos = inputPumpPos.east().below();
        BlockPos outputPipePos = compressorPos.west();
        BlockPos outputPumpPos = outputPipePos.west();
        BlockPos outputCogPos = outputPumpPos.south();
        BlockPos outputMotorPos = outputCogPos.west();
        BlockPos outputTankPos = outputPumpPos.west().below();

        Selection compressorSelection = util.select().position(compressorPos);
        Selection inputSelection = util.select().fromTo(inputTankPos, inputPipePos);
        Selection outputSelection = util.select().fromTo(outputPipePos, outputTankPos);
        Selection compressorMotorSelection = util.select().position(compressorMotorPos);
        Selection motorSelection = util.select().fromTo(inputCogPos, inputMotorPos).add(util.select().fromTo(outputCogPos, outputMotorPos));
        Selection pumpSelection = util.select().position(inputPumpPos).add(util.select().position(outputPumpPos));

        Vec3 compressorVec = util.vector().centerOf(compressorPos);
        Vec3 outputPumpVec = util.vector().centerOf(outputPumpPos);
        Vec3 inputPumpVec = util.vector().centerOf(inputPumpPos);
        Vec3 outputPipeVec = util.vector().centerOf(outputPipePos);

        AABB compressorArea = new AABB(compressorVec, compressorVec);
        AABB inputArea = new AABB(inputPumpVec, inputPumpVec);
        AABB outputArea = new AABB(outputPipeVec, outputPipeVec);
        AABB expandedCompressorArea = compressorArea.inflate(0.5, 0.5, 0.5);

        Object inputCompressorObject = new Object();
        Object outputCompressorObject = new Object();
        Object inputObject = new Object();
        Object outputObject = new Object();

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();
        float fastSpeed = SpeedLevel.FAST.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(compressorSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(inputSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(outputSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputObject, inputArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputCompressorObject, compressorArea, 6);

        scene.idle(3);
        inputArea = inputArea.inflate(0.5, 0.375, 0.375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputObject, inputArea, 3);

        scene.idle(3);
        inputArea = inputArea.expandTowards(-1, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputObject, inputArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputCompressorObject, expandedCompressorArea, 60);
        scene.overlay().showText(60).text("Air Compressors can pressurize input gases").pointAt(compressorVec).placeNearTarget().attachKeyFrame();

        scene.idleSeconds(4);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputObject, outputArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputCompressorObject, compressorArea, 6);

        scene.idle(3);
        outputArea = outputArea.inflate(0.5, 0.375, 0.375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputObject, outputArea, 3);

        scene.idle(3);
        outputArea = outputArea.expandTowards(-1, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputCompressorObject, expandedCompressorArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputObject, outputArea, 60);
        scene.overlay().showText(60).text("Every 10mB of gas is pressurized into 1mB of Pressurized Gas").pointAt(outputPumpVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(compressorMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().setBlock(inputMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(outputMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), false);
        scene.world().showSection(compressorMotorSelection, Direction.DOWN);
        scene.world().showSection(motorSelection, Direction.DOWN);

        scene.idle(15);
        scene.world().setKineticSpeed(compressorMotorSelection, mediumSpeed);
        scene.world().setKineticSpeed(compressorSelection, mediumSpeed);
        scene.world().setKineticSpeed(motorSelection, mediumSpeed);
        scene.world().setKineticSpeed(pumpSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(inputPumpPos);
        scene.effects().rotationSpeedIndicator(outputPumpPos);
        scene.effects().rotationSpeedIndicator(compressorPos);
        scene.effects().rotationSpeedIndicator(inputMotorPos);
        scene.effects().rotationSpeedIndicator(outputMotorPos);
        scene.effects().rotationSpeedIndicator(compressorMotorPos);
        scene.overlay().showText(60).text("Operates when sufficient Rotational Force is supplied").colored(PonderPalette.GREEN).pointAt(compressorVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setKineticSpeed(compressorMotorSelection, fastSpeed);
        scene.world().setKineticSpeed(compressorSelection, fastSpeed);
        scene.effects().rotationSpeedIndicator(compressorPos);
        scene.effects().rotationSpeedIndicator(compressorMotorPos);
        scene.overlay().showText(60).text("Higher rotational speed increases pressurization rate").colored(PonderPalette.FAST).pointAt(compressorVec).placeNearTarget();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void overheatManagement(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("air_compressor_overheat_management", "Managing Air Compressor Overheating");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos compressorPos = util.grid().at(3, 2, 3);
        BlockPos compressorMotorPos = compressorPos.above();
        BlockPos coolantPos = compressorPos.below();
        BlockPos inputPipePos = compressorPos.east();
        BlockPos inputPumpPos = inputPipePos.east();
        BlockPos inputCogPos = inputPumpPos.south();
        BlockPos inputMotorPos = inputCogPos.east();
        BlockPos inputTankPos = inputPumpPos.east().below();
        BlockPos outputPipePos = compressorPos.west();
        BlockPos outputPumpPos = outputPipePos.west();
        BlockPos outputCogPos = outputPumpPos.south();
        BlockPos outputMotorPos = outputCogPos.west();
        BlockPos outputTankPos = outputPumpPos.west().below();

        Selection compressorSelection = util.select().position(compressorPos);
        Selection coolantSelection = util.select().position(coolantPos);
        Selection inputSelection = util.select().fromTo(inputTankPos, inputPipePos);
        Selection outputSelection = util.select().fromTo(outputPipePos, outputTankPos);
        Selection compressorMotorSelection = util.select().position(compressorMotorPos);
        Selection motorSelection = util.select().fromTo(inputCogPos, inputMotorPos).add(util.select().fromTo(outputCogPos, outputMotorPos));
        Selection pumpSelection = util.select().position(inputPumpPos).add(util.select().position(outputPumpPos));

        Vec3 compressorVec = util.vector().centerOf(compressorPos);
        Vec3 coolantVec = util.vector().centerOf(coolantPos);

        AABB compressorArea = new AABB(compressorVec, compressorVec);
        AABB coolantArea = new AABB(coolantVec, coolantVec);

        Object compressorObject = new Object();
        Object coolantObject = new Object();

        ParticleEmitter explosion = scene.effects().simpleParticleEmitter(ParticleTypes.EXPLOSION, Vec3.ZERO);

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();
        int fastSpeed = AllConfigs.server().kinetics.maxRotationSpeed.get();

        scene.idle(20);
        scene.world().showSection(compressorSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(inputSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(outputSelection, Direction.EAST);

        scene.idle(20);
        scene.world().setBlock(compressorMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().setBlock(inputMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(outputMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), false);
        scene.world().showSection(compressorMotorSelection, Direction.DOWN);
        scene.world().showSection(motorSelection, Direction.DOWN);

        scene.idle(15);
        scene.world().setKineticSpeed(compressorSelection, fastSpeed);
        scene.world().setKineticSpeed(compressorMotorSelection, fastSpeed);
        scene.world().setKineticSpeed(motorSelection, mediumSpeed);
        scene.world().setKineticSpeed(pumpSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(compressorPos);
        scene.effects().rotationSpeedIndicator(inputPumpPos);
        scene.effects().rotationSpeedIndicator(outputPumpPos);
        scene.effects().rotationSpeedIndicator(inputMotorPos);
        scene.effects().rotationSpeedIndicator(outputMotorPos);
        scene.effects().rotationSpeedIndicator(compressorMotorPos);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, compressorObject, compressorArea, 3);

        scene.idle(3);
        compressorArea = compressorArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, compressorObject, compressorArea, 3);

        scene.idle(3);
        compressorArea = compressorArea.expandTowards(0, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, compressorObject, compressorArea, 60);
        scene.overlay().showText(60).text("Continuous operation causes heat accumulation").colored(PonderPalette.RED).pointAt(compressorVec).placeNearTarget().attachKeyFrame();

        scene.idle(40);
        scene.world().modifyBlockEntity(compressorPos, AirCompressorBlockEntity.class, AirCompressorBlockEntity::increaseHeat);

        scene.idle(40);
        scene.world().modifyBlockEntity(compressorPos, AirCompressorBlockEntity.class, AirCompressorBlockEntity::increaseHeat);
        scene.overlay().showText(60).text("Increasing overheating reduces pressurization rate").colored(PonderPalette.RED).pointAt(compressorVec).placeNearTarget();

        scene.idle(40);
        scene.world().modifyBlockEntity(compressorPos, AirCompressorBlockEntity.class, AirCompressorBlockEntity::increaseHeat);

        scene.idle(40);
        scene.overlay().showText(30).text("If left unchecked...").colored(PonderPalette.RED).pointAt(compressorVec).placeNearTarget().attachKeyFrame();

        scene.idle(30);
        scene.effects().emitParticles(compressorVec, explosion, 1, 1);
        scene.world().destroyBlock(compressorPos);
        scene.world().hideSection(compressorSelection, Direction.UP);
        Set<ElementLink<WorldSectionElement>> fireSection = new HashSet<>();
        Set<BlockPos> firePositions = getPossibleEmptyPos(util);
        for (BlockPos firePos : firePositions) {
            scene.world().setBlock(firePos, Blocks.FIRE.defaultBlockState(), false);
            fireSection.add(scene.world().showIndependentSectionImmediately(util.select().fromTo(firePos, firePos)));
        }

        scene.idle(60);
        for (BlockPos firePos : firePositions) {
            scene.world().replaceBlocks(util.select().fromTo(firePos, firePos), Blocks.AIR.defaultBlockState(), true);
        }
        for (ElementLink<WorldSectionElement> fireIndependentSection : fireSection) {
            scene.world().hideIndependentSection(fireIndependentSection, Direction.UP);
        }
        scene.world().restoreBlocks(compressorSelection);
        scene.world().setBlock(coolantPos, Blocks.PACKED_ICE.defaultBlockState(), false);
        scene.world().showSection(compressorSelection, Direction.SOUTH);
        scene.world().showSection(coolantSelection, Direction.SOUTH);

        scene.idle(15);
        scene.world().setKineticSpeed(compressorSelection, mediumSpeed);
        scene.world().setKineticSpeed(compressorMotorSelection, mediumSpeed);
        scene.effects().rotationSpeedIndicator(compressorPos);
        scene.effects().rotationSpeedIndicator(compressorMotorPos);

        scene.idle(23);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, coolantObject, coolantArea, 3);

        scene.idle(3);
        coolantArea = coolantArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, coolantObject, coolantArea, 3);

        scene.idle(3);
        coolantArea = coolantArea.expandTowards(0, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, coolantObject, coolantArea, 60);
        scene.overlay().showText(60).text("Placed coolers slow overheating progress").colored(PonderPalette.GREEN).pointAt(coolantVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(coolantPos, Blocks.ICE.defaultBlockState(), true);
        scene.overlay().showText(60).text("But passive coolers degrade or melt over time").colored(PonderPalette.RED).pointAt(coolantVec).placeNearTarget().attachKeyFrame();

        scene.idle(40);
        scene.world().setBlock(coolantPos, Blocks.AIR.defaultBlockState(), true);
        scene.world().hideSection(coolantSelection, Direction.UP);

        scene.idle(40);
        scene.world().restoreBlocks(coolantSelection);
        scene.world().modifyBlock(coolantPos, s -> s.setValue(BreezeCoolerBlock.COOLER, false), false);
        scene.world().showSection(coolantSelection, Direction.SOUTH);

        scene.idle(15);
        scene.world().modifyBlock(coolantPos, s -> s.setValue(BreezeCoolerBlock.COOLER, true), false);

        scene.idle(5);
        scene.overlay().showText(60).text("Breeze Coolers provide more durable and efficient cooling").colored(PonderPalette.GREEN).pointAt(coolantVec).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.world().modifyBlockEntity(coolantPos, BreezeCoolerBlockEntity.class, BreezeCoolerBlockEntity::SwitchToChilledState);
        scene.world().modifyBlock(coolantPos, s -> s.setValue(BreezeCoolerBlock.FROST_LEVEL, FrostLevel.CHILLED), false);

        scene.idle(40);
        scene.markAsFinished();
    }

    private static Set<BlockPos> getPossibleEmptyPos(SceneBuildingUtil util) {
        Set<BlockPos> blockPosSet = new HashSet<>();
        RandomSource random = RandomSource.create();
        for (int i = 0; i < random.nextInt(7, 12); i++) {
            int x = random.nextInt(0, 7);
            int z = random.nextInt(0, 7);
            if (z == 3 && x % 3 == 0) {
                continue;
            }

            blockPosSet.add(util.grid().at(x, 1, z));
        }

        return blockPosSet;
    }
}
