package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public class AirtightPipeScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_pipe", "Moving Compressed Air using Airtight Pipes");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos lowerCreativeTankPos = util.grid().at(4, 1, 2);
        BlockPos upperCreativeTankPos = util.grid().at(4, 2, 2);
        BlockPos lowerTankPos = util.grid().at(0, 1, 2);
        BlockPos upperTankPos = util.grid().at(0, 2, 2);
        BlockPos leftPipePos = util.grid().at(3, 2, 2);
        BlockPos pumpPos = util.grid().at(2, 2, 2);
        BlockPos rightPipePos = util.grid().at(1, 2, 2);
        BlockPos upPipePos = util.grid().at(1, 3, 2);
        BlockPos cogPos = util.grid().at(2, 2, 3);
        BlockPos motorPos = util.grid().at(3, 2, 3);

        Selection creativeTankSelection = util.select().fromTo(lowerCreativeTankPos, upperCreativeTankPos);
        Selection tankSelection = util.select().fromTo(lowerTankPos, upperTankPos);
        Selection pumpsSelection = util.select().fromTo(leftPipePos, rightPipePos);
        Selection sourceSelection = util.select().fromTo(cogPos, motorPos);
        Selection upPipeSelection = util.select().fromTo(upPipePos, upPipePos);

        FluidStack water = new FluidStack(Fluids.WATER, 8000);

		AABB bb = new AABB(util.vector().centerOf(rightPipePos), util.vector().centerOf(rightPipePos)).inflate(1 / 6f);
		AABB bb1 = bb.move(0, 1 / 2f, 0);
        AABB bb2 = bb.move(0, 0, -1 / 2f);
        AABB bb3 = bb.move(0, -1 / 2f, 0);

        scene.world().setBlock(leftPipePos, AllBlocks.GLASS_FLUID_PIPE.getDefaultState().setValue(GlassFluidPipeBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(pumpPos, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(rightPipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(AirtightPipeBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(cogPos, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(upPipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(AirtightPipeBlock.AXIS, Direction.Axis.Y), false);
        scene.world().modifyBlockEntity(lowerCreativeTankPos, CreativeFluidTankBlockEntity.class, be -> ((CreativeFluidTankBlockEntity.CreativeSmartFluidTank) be.getTankInventory()).setContainedFluid(water));
        scene.world().showSection(creativeTankSelection, Direction.DOWN);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().showSection(pumpsSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().showSection(sourceSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().setKineticSpeed(sourceSelection, 64);
        scene.world().setKineticSpeed(pumpsSelection, -64);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.world().propagatePipeChange(pumpPos);
        scene.overlay().showText(60).text("Airtight Pipes cannot transport any fluids; they exclusively transport Compressed Air").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(rightPipePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(upPipeSelection, Direction.DOWN);

		scene.idle(10);
        scene.overlay().showText(60).text("Airtight Pipes cannot connect to any other adjacent pipe segments").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(rightPipePos)).placeNearTarget().attachKeyFrame();
		scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, bb1, bb, 1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, bb2, bb, 1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, bb3, bb, 1);

		scene.idle(1);
		scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, bb1, bb1, 49);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, bb2, bb2, 49);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, bb3, bb3, 49);

        scene.idle(49);
        scene.world().hideSection(upPipeSelection, Direction.UP);

        scene.idle(10);
        scene.markAsFinished();
    }
}
