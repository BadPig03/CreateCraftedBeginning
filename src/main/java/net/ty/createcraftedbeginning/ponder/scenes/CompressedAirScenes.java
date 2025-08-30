package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class CompressedAirScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("compressed_air", "Moving Compressed Air using Copper Fluid Pipes");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        ParticleEmitter explosion = scene.effects().simpleParticleEmitter(ParticleTypes.EXPLOSION, Vec3.ZERO);

        BlockPos lowerCreativeTankPos = util.grid().at(4, 1, 2);
        BlockPos upperCreativeTankPos = util.grid().at(4, 2, 2);
        BlockPos lowerTankPos = util.grid().at(0, 1, 2);
        BlockPos upperTankPos = util.grid().at(0, 2, 2);
        BlockPos leftPipePos = util.grid().at(3, 2, 2);
        BlockPos pumpPos = util.grid().at(2, 2, 2);
        BlockPos rightPipePos = util.grid().at(1, 2, 2);
        BlockPos newLeftPipePos = util.grid().at(3, 2, 1);
        BlockPos newPumpPos = util.grid().at(2, 2, 1);
        BlockPos newRightPipePos = util.grid().at(1, 2, 1);
        BlockPos cogPos = util.grid().at(2, 2, 3);
        BlockPos motorPos = util.grid().at(3, 2, 3);
        BlockPos leftInterfacePos = util.grid().at(3, 1, 2);
        BlockPos rightInterfacePos = util.grid().at(1, 1, 2);

        Selection creativeTankSelection = util.select().fromTo(lowerCreativeTankPos, upperCreativeTankPos);
        Selection tankSelection = util.select().fromTo(lowerTankPos, upperTankPos);
        Selection pumpsSelection = util.select().fromTo(leftPipePos, rightPipePos);
        Selection newPumpsSelection = util.select().fromTo(newLeftPipePos, newRightPipePos);
        Selection pumpSelection = util.select().fromTo(pumpPos, pumpPos);
        Selection rightPumpSelection = util.select().fromTo(rightPipePos, rightPipePos);
        Selection rightPipeSelection = util.select().fromTo(rightPipePos, rightPipePos);
        Selection sourceSelection = util.select().fromTo(cogPos, motorPos);
        Selection upperTankSelection = util.select().fromTo(upperTankPos, upperTankPos);
        Selection leftInterfaceSelection = util.select().fromTo(leftInterfacePos, leftInterfacePos);
        Selection rightInterfaceSelection = util.select().fromTo(rightInterfacePos, rightInterfacePos);
        Selection everywhere = util.select().everywhere();

        ItemStack emptyBucket = new ItemStack(Items.BUCKET);
        ItemStack pumpItem = new ItemStack(AllBlocks.MECHANICAL_PUMP.asItem());
        ItemStack pipeItem = new ItemStack(AllBlocks.FLUID_PIPE.asItem());
        ItemStack tankItem = new ItemStack(AllBlocks.FLUID_TANK.asItem());

        scene.world().setBlock(leftPipePos, AllBlocks.FLUID_PIPE.getDefaultState().setValue(FluidPipeBlock.UP, false).setValue(FluidPipeBlock.DOWN, false).setValue(FluidPipeBlock.SOUTH, false).setValue(FluidPipeBlock.NORTH, false), false);
        scene.world().setBlock(pumpPos, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(rightPipePos, AllBlocks.FLUID_PIPE.getDefaultState().setValue(FluidPipeBlock.UP, false).setValue(FluidPipeBlock.DOWN, false).setValue(FluidPipeBlock.SOUTH, false).setValue(FluidPipeBlock.NORTH, false), false);
        scene.world().setBlock(cogPos, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST), false);
        scene.world().showSection(creativeTankSelection, Direction.DOWN);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().showSection(pumpsSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().showSection(sourceSelection, Direction.DOWN);

        scene.idle(10);
        scene.overlay().showControls(util.vector().blockSurface(upperCreativeTankPos, Direction.UP).subtract(0, 1 / 8f, 0), Pointing.DOWN, 60).withItem(emptyBucket);
        scene.overlay().showText(60).text("Transporting Compressed Air poses a greater hazard than handling ordinary fluids").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(lowerCreativeTankPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setKineticSpeed(sourceSelection, 128);
        scene.world().setKineticSpeed(pumpSelection, -128);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.effects().emitParticles(util.vector().centerOf(pumpPos), explosion, 1, 1);
        scene.world().destroyBlock(pumpPos);
        scene.world().hideSection(pumpSelection, Direction.UP);
        ElementLink<EntityElement> pump = scene.world().createItemEntity(util.vector().centerOf(pumpPos), util.vector().of(0, -0.1, 0), pumpItem);
        scene.overlay().showText(60).text("Excessive pressure from high rotation speeds in Mechanical Pumps will result in explosions").colored(PonderPalette.FAST).pointAt(Vec3.atCenterOf(pumpPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(pumpPos, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.WEST), false);
        scene.world().showSection(pumpSelection, Direction.DOWN);
        scene.world().multiplyKineticSpeed(everywhere, 0.25f);
        scene.world().setKineticSpeed(pumpSelection, -32);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.effects().rotationSpeedIndicator(pumpPos);

        scene.idle(10);
        scene.world().modifyEntity(pump, Entity::discard);
        scene.overlay().showText(60).text("Mechanical Pumps must maintain low rotation speeds when transporting Compressed Air").colored(PonderPalette.SLOW).pointAt(Vec3.atCenterOf(pumpPos)).placeNearTarget();

        scene.idle(80);
        scene.world().setBlock(newLeftPipePos, AllBlocks.FLUID_PIPE.getDefaultState().setValue(FluidPipeBlock.UP, false).setValue(FluidPipeBlock.DOWN, false).setValue(FluidPipeBlock.EAST, false).setValue(FluidPipeBlock.NORTH, false), false);
        scene.world().setBlock(newPumpPos, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(newRightPipePos, AllBlocks.FLUID_PIPE.getDefaultState().setValue(FluidPipeBlock.UP, false).setValue(FluidPipeBlock.DOWN, false).setValue(FluidPipeBlock.WEST, false).setValue(FluidPipeBlock.NORTH, false), false);
        scene.world().showSection(newPumpsSelection, Direction.DOWN);

        scene.idle(10);
        scene.world().setKineticSpeed(newPumpsSelection, 32);
        scene.effects().rotationSpeedIndicator(newPumpPos);
        scene.world().modifyBlock(leftPipePos, s -> s.setValue(FluidPipeBlock.NORTH, true), false);
        scene.world().modifyBlock(rightPipePos, s -> s.setValue(FluidPipeBlock.NORTH, true), false);
        scene.world().modifyBlock(newRightPipePos, s -> s.setValue(FluidPipeBlock.WEST, true).setValue(FluidPipeBlock.SOUTH, false), false);
        scene.effects().emitParticles(util.vector().centerOf(rightPipePos), explosion, 1, 1);
        scene.world().destroyBlock(rightPipePos);
        scene.world().hideSection(rightPumpSelection, Direction.UP);
        ElementLink<EntityElement> pipe = scene.world().createItemEntity(util.vector().centerOf(rightPipePos), util.vector().of(0, -0.1, 0), pipeItem);
        scene.overlay().showText(60).text("Excessive pressure within fluid pipelines will also result in explosions").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(rightPipePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().hideSection(newPumpsSelection, Direction.UP);
        scene.world().setBlock(rightPipePos, AllBlocks.FLUID_PIPE.getDefaultState().setValue(FluidPipeBlock.UP, false).setValue(FluidPipeBlock.DOWN, false).setValue(FluidPipeBlock.SOUTH, false).setValue(FluidPipeBlock.NORTH, false), false);
        scene.world().modifyBlock(leftPipePos, s -> s.setValue(FluidPipeBlock.NORTH, false), false);
        scene.world().showSection(rightPipeSelection, Direction.DOWN);

        scene.idle(10);
        scene.world().modifyEntity(pipe, Entity::discard);
        scene.overlay().showOutline(PonderPalette.RED, new Object(), tankSelection, 60);
        scene.overlay().showText(60).text("Besides, Fluid Tanks can only store small amounts of Compressed Air").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(upperTankPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("A large amounts of Compressed Air cause another explosion as well").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(upperTankPos)).placeNearTarget();
        scene.effects().emitParticles(util.vector().centerOf(upperTankPos), explosion, 1, 1);
        scene.world().destroyBlock(upperTankPos);
        scene.world().modifyBlock(lowerTankPos, s -> s.setValue(FluidTankBlock.TOP, true), false);
        ElementLink<EntityElement> tank = scene.world().createItemEntity(util.vector().centerOf(upperTankPos), util.vector().of(0, -0.1, 0), tankItem);

        scene.idle(80);
        scene.world().hideSection(pumpsSelection, Direction.UP);
        scene.world().hideSection(sourceSelection, Direction.UP);

        scene.idle(10);
        scene.world().modifyEntity(tank, Entity::discard);
        scene.world().restoreBlocks(upperTankSelection);
        scene.world().modifyBlock(lowerTankPos, s -> s.setValue(FluidTankBlock.TOP, false), false);
        scene.world().setBlock(leftInterfacePos, AllBlocks.PORTABLE_FLUID_INTERFACE.getDefaultState().setValue(PortableStorageInterfaceBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(rightInterfacePos, AllBlocks.PORTABLE_FLUID_INTERFACE.getDefaultState().setValue(PortableStorageInterfaceBlock.FACING, Direction.EAST), false);
        scene.world().showSection(leftInterfaceSelection, Direction.DOWN);
        scene.world().showSection(rightInterfaceSelection, Direction.DOWN);

        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.RED, new Object(), leftInterfaceSelection, 60);
        scene.overlay().showOutline(PonderPalette.RED, new Object(), rightInterfaceSelection, 60);
        scene.overlay().showLine(PonderPalette.RED, util.vector().centerOf(leftInterfacePos), util.vector().centerOf(rightInterfacePos), 60);
        scene.overlay().showText(60).text("Furthermore, Portable Fluid Interfaces cannot transport Compressed Air").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(rightInterfacePos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
