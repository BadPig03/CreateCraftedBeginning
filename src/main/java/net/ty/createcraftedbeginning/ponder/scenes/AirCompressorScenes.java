package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtightintakeport.AirtightIntakePortBlock;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

public class AirCompressorScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("air_compressor", "Pressurize Compressed Air using Air Compressors");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(0, 2, 2);
        BlockPos rightPumpPos = util.grid().at(1, 2, 2);
        BlockPos rightCogPos = util.grid().at(1, 3, 2);
        BlockPos chamberPos = util.grid().at(2, 1, 2);
        BlockPos compressorPos = util.grid().at(2, 2, 2);
        BlockPos leftPumpPos = util.grid().at(3, 2, 2);
        BlockPos leftCogPos = util.grid().at(3, 3, 2);
        BlockPos portPos = util.grid().at(4, 2, 2);
        BlockPos gearBoxPos = util.grid().at(2, 3, 2);
        BlockPos motorPos = util.grid().at(2, 4, 2);

        Selection chamberSelection = util.select().fromTo(chamberPos, chamberPos);
        Selection compressorSelection = util.select().fromTo(compressorPos, compressorPos);
        Selection tankSelection = util.select().fromTo(tankPos, rightPumpPos);
        Selection airSelection = util.select().fromTo(portPos, leftPumpPos);
        Selection sourceSelection = util.select().fromTo(leftCogPos.above(), rightCogPos);
        Selection leftCogSelection = util.select().fromTo(leftCogPos, leftCogPos);
        Selection leftPumpSelection = util.select().fromTo(leftPumpPos, leftPumpPos);
        Selection rightCogSelection = util.select().fromTo(rightCogPos, rightCogPos);
        Selection rightPumpSelection = util.select().fromTo(rightPumpPos, rightPumpPos);

        ItemStack packedIceItem = new ItemStack(Blocks.PACKED_ICE);
        ItemStack powderSnowBucket = new ItemStack(Items.POWDER_SNOW_BUCKET);

        ParticleEmitter snow = scene.effects().simpleParticleEmitter(ParticleTypes.SNOWFLAKE, Vec3.ZERO);

        scene.world().setBlock(tankPos, CCBBlocks.AIRTIGHT_TANK_BLOCK.getDefaultState(), false);
        scene.world().setBlock(rightPumpPos, CCBBlocks.AIRTIGHT_PUMP_BLOCK.getDefaultState().setValue(PumpBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(rightCogPos, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(chamberPos, CCBBlocks.BREEZE_CHAMBER_BLOCK.getDefaultState().setValue(BreezeChamberBlock.FROST_LEVEL, BreezeChamberBlock.FrostLevel.RIMING).setValue(BreezeChamberBlock.COOLER, true), false);
        scene.world().setBlock(compressorPos, CCBBlocks.AIR_COMPRESSOR_BLOCK.getDefaultState(), false);
        scene.world().setBlock(leftPumpPos, CCBBlocks.AIRTIGHT_PUMP_BLOCK.getDefaultState().setValue(PumpBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(leftCogPos, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(portPos, CCBBlocks.AIRTIGHT_INTAKE_PORT_BLOCK.getDefaultState().setValue(AirtightIntakePortBlock.FACING, Direction.EAST), false);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().setBlock(gearBoxPos, AllBlocks.GEARBOX.getDefaultState().setValue(GearboxBlock.AXIS, Direction.Axis.Z), false);
        scene.world().showSection(chamberSelection, Direction.DOWN);
        scene.world().showSection(compressorSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().showSection(airSelection, Direction.DOWN);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().showSection(sourceSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().setKineticSpeed(sourceSelection, 128);
        scene.world().setKineticSpeed(compressorSelection, 128);
        scene.world().setKineticSpeed(leftCogSelection, -128);
        scene.world().setKineticSpeed(leftPumpSelection, 128);
        scene.world().setKineticSpeed(rightCogSelection, 128);
        scene.world().setKineticSpeed(rightPumpSelection, -128);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.effects().rotationSpeedIndicator(compressorPos);
        scene.overlay().showText(60).text("The Air Compressor can further pressurize Compressed Air under specified conditions").pointAt(Vec3.atCenterOf(compressorPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showOutline(PonderPalette.RED, new Object(), compressorSelection, 60);
        scene.overlay().showOutline(PonderPalette.RED, new Object(), chamberSelection, 60);
        scene.overlay().showText(60).text("Condition 1: \nA Breeze Chamber must be placed directly beneath The Air Compressor").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(chamberPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().multiplyKineticSpeed(util.select().everywhere(), 0.5f);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.effects().rotationSpeedIndicator(compressorPos);
        scene.overlay().showText(60).text("Condition 2: \nThe Air Compressor must maintain at least medium rotational speed").colored(PonderPalette.MEDIUM).pointAt(Vec3.atCenterOf(compressorPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showOutline(PonderPalette.INPUT, new Object(), leftPumpSelection, 60);
        scene.overlay().showOutline(PonderPalette.OUTPUT, new Object(), rightPumpSelection, 60);
        scene.overlay().showText(60).text("Condition 3: \nInputting Compressed Air into one side will generate High-Pressure Compressed Aas at the opposite side").colored(PonderPalette.OUTPUT).pointAt(Vec3.atCenterOf(rightPumpPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(chamberPos, Direction.NORTH), Pointing.RIGHT, 40).rightClick().withItem(packedIceItem);

        scene.idle(7);
        scene.effects().emitParticles(util.vector().centerOf(chamberPos), snow, 20, 1);
        scene.world().modifyBlockEntity(chamberPos, BreezeChamberBlockEntity.class, bcbe -> {
            bcbe.wind = true;
            bcbe.windRotationSpeed = 24.f;
        });
        scene.world().modifyBlock(chamberPos, s -> s.setValue(BreezeChamberBlock.FROST_LEVEL, BreezeChamberBlock.FrostLevel.CHILLED), false);
        scene.overlay().showText(60).text("The §3Chilled§r and §bGalling§r states of the Breeze Chamber increase the pressurization rate, while the §8Riming§r state decreases it").pointAt(Vec3.atCenterOf(chamberPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(chamberPos, Direction.NORTH), Pointing.RIGHT, 40).rightClick().withItem(powderSnowBucket);

        scene.idle(7);
        scene.effects().emitParticles(util.vector().centerOf(chamberPos), snow, 20, 2);
        scene.world().modifyBlockEntity(chamberPos, BreezeChamberBlockEntity.class, bcbe -> bcbe.windRotationSpeed = 36.f);
        scene.world().modifyBlock(chamberPos, s -> s.setValue(BreezeChamberBlock.FROST_LEVEL, BreezeChamberBlock.FrostLevel.GALLING), false);
        scene.overlay().showText(60).text("Specially, the §bGalling§r state doubles the pressurization rate").pointAt(Vec3.atCenterOf(chamberPos)).placeNearTarget();

        scene.idle(60);
        scene.markAsFinished();
    }
}
