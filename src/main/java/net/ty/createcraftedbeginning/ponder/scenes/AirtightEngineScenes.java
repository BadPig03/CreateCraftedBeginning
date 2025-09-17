package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.condensatedrain.CondensateDrainBlock;
import net.ty.createcraftedbeginning.content.airtights.condensatedrain.CondensateDrainBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

public class AirtightEngineScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_engine", "Setting up Airtight Engines");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos lowerTankPos = util.grid().at(3, 1, 3);
        BlockPos upperTankPos = util.grid().at(3, 3, 3);
        BlockPos enginePos = util.grid().at(3, 4, 3);
        BlockPos cogPos = util.grid().at(2, 4, 2);
        BlockPos pipePos = util.grid().at(3, 1, 2);
        BlockPos pumpPos = util.grid().at(3, 1, 1);
        BlockPos pipe2Pos = util.grid().at(3, 1, 0);
        BlockPos cog2Pos = util.grid().at(4, 1, 1);
        BlockPos motorPos = util.grid().at(4, 1, 0);
        BlockPos newEnginePos = util.grid().at(3, 5, 3);
        BlockPos drainPos = util.grid().at(2, 5, 2);
        BlockPos pipe3Pos = util.grid().at(1, 5, 2);
        BlockPos pump2Pos = util.grid().at(0, 5, 2);
        BlockPos cog3Pos = util.grid().at(0, 4, 2);

        Selection tankSelection = util.select().fromTo(lowerTankPos, upperTankPos);
        Selection engineSelection = util.select().fromTo(enginePos, enginePos);
        Selection cogSelection = util.select().fromTo(cogPos, cogPos);
        Selection pumpSelection = util.select().fromTo(pumpPos, pipe2Pos);
        Selection sourceSelection = util.select().fromTo(cog2Pos, motorPos);
        Selection pipeSelection = util.select().fromTo(pipePos, pipePos);
        Selection drainSelection = util.select().fromTo(drainPos, drainPos);
        Selection largeTankSelection = util.select().fromTo(util.grid().at(2, 1, 4), util.grid().at(0, 4, 6));
        Selection newEngineSelection = util.select().fromTo(enginePos, newEnginePos);
        Selection newLargeTankSelection = util.select().fromTo(util.grid().at(4, 1, 2), util.grid().at(2, 4, 4));
        Selection newPumpSelection = util.select().fromTo(pipe3Pos, cog3Pos);

        ItemStack canister = new ItemStack(CCBItems.COMPRESSED_AIR_CANISTER.asItem());
        ItemStack highCanister = new ItemStack(CCBItems.HIGH_PRESSURE_COMPRESSED_AIR_CANISTER.asItem());

        scene.world().setBlock(enginePos, CCBBlocks.AIRTIGHT_ENGINE_BLOCK.getDefaultState().setValue(AirtightEngineBlock.AXIS, Direction.Axis.Y).setValue(AirtightEngineBlock.FACE, AttachFace.FLOOR).setValue(AirtightEngineBlock.FACING, Direction.NORTH), false);
        scene.world().setBlock(cogPos, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.Y), false);
        scene.world().setBlock(pumpPos, CCBBlocks.AIRTIGHT_PUMP_BLOCK.getDefaultState().setValue(AirtightPumpBlock.FACING, Direction.SOUTH), false);
        scene.world().setBlock(pipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(AirtightPipeBlock.AXIS, Direction.Axis.Z), false);
        scene.world().setBlock(pipe2Pos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(AirtightPipeBlock.AXIS, Direction.Axis.Z), false);
        scene.world().setBlock(cog2Pos, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.Z), false);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);
        scene.world().setBlock(drainPos, CCBBlocks.CONDENSATE_DRAIN_BLOCK.getDefaultState().setValue(CondensateDrainBlock.FACE, AttachFace.FLOOR).setValue(CondensateDrainBlock.FACING, Direction.NORTH), false);

        scene.idle(10);
        ElementLink<WorldSectionElement> tank = scene.world().showIndependentSection(tankSelection, Direction.DOWN);

        scene.idle(10);
        ElementLink<WorldSectionElement> engine = scene.world().showIndependentSection(engineSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Airtight Engines can be placed on an Airtight Tank").pointAt(util.vector().centerOf(enginePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        ElementLink<WorldSectionElement> cog = scene.world().showIndependentSection(cogSelection, Direction.DOWN);

        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.OUTPUT, new Object(), cogSelection, 60);
        scene.overlay().showText(60).text("The Rotational Force of the Airtight Engine can be output through Cogwheels").colored(PonderPalette.OUTPUT).pointAt(util.vector().centerOf(cogPos)).placeNearTarget();

        scene.idle(80);
        scene.world().hideIndependentSection(cog, Direction.UP);
        ElementLink<WorldSectionElement> pipe = scene.world().showIndependentSection(pipeSelection, Direction.DOWN);
        scene.world().showSection(pumpSelection, Direction.DOWN);
        scene.world().showSection(sourceSelection, Direction.DOWN);
        scene.world().setKineticSpeed(pumpSelection, 30);
        scene.world().setKineticSpeed(sourceSelection, -30);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(10);
        scene.overlay().showText(40).text("With sufficient Compressed Air and space...").colored(PonderPalette.INPUT).pointAt(util.vector().centerOf(pumpPos)).placeNearTarget().attachKeyFrame();

        scene.idle(45);
        scene.overlay().showText(40).text("...Airtight Engines will generate Rotational Force").colored(PonderPalette.INPUT).pointAt(util.vector().centerOf(enginePos)).placeNearTarget();
        scene.world().setKineticSpeed(engineSelection, 8);
        scene.effects().rotationSpeedIndicator(enginePos);

        scene.idle(70);
        scene.overlay().showOutline(PonderPalette.GREEN, new Object(), tankSelection, 60);
        scene.overlay().showText(60).text("The minimal setup requires 3 Airtight Tanks").colored(PonderPalette.GREEN).pointAt(util.vector().centerOf(upperTankPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().hideIndependentSection(tank, Direction.WEST);
        scene.world().hideIndependentSection(pipe, Direction.WEST);

        scene.idle(10);
        scene.world().moveSection(engine, util.vector().of(0, 1, 0), 10);

        scene.idle(10);
        scene.world().setBlock(newEnginePos, CCBBlocks.AIRTIGHT_ENGINE_BLOCK.getDefaultState().setValue(AirtightEngineBlock.AXIS, Direction.Axis.Y).setValue(AirtightEngineBlock.FACE, AttachFace.FLOOR).setValue(AirtightEngineBlock.FACING, Direction.NORTH), false);
        scene.world().setKineticSpeed(newEngineSelection, 8);

        scene.idle(5);
        ElementLink<WorldSectionElement> largeTank = scene.world().showIndependentSection(largeTankSelection, Direction.EAST);
        scene.world().moveSection(largeTank, util.vector().of(2, 0, -2), 0);

        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.FAST, new Object(), newLargeTankSelection, 60);
        scene.overlay().showText(60).text("While supporting a maximum structure of 3x3x4").colored(PonderPalette.FAST).pointAt(util.vector().centerOf(lowerTankPos)).placeNearTarget();

        scene.idle(80);
        scene.world().multiplyKineticSpeed(sourceSelection, 3);
        scene.world().multiplyKineticSpeed(pumpSelection, 3);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.overlay().showControls(util.vector().blockSurface(pumpPos, Direction.UP), Pointing.DOWN, 60).withItem(canister);
        scene.overlay().showText(60).text("If supplied with Compressed Air, the Airtight Drive Assembly will remain passive state regardless of input quantity").colored(PonderPalette.RED).pointAt(util.vector().centerOf(newEnginePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setKineticSpeed(newEngineSelection, 16);
        scene.effects().rotationSpeedIndicator(newEnginePos);
        scene.overlay().showControls(util.vector().blockSurface(pumpPos, Direction.UP), Pointing.DOWN, 60).withItem(highCanister);
        scene.overlay().showText(60).text("If supplied with High Pressure Compressed Air, it will generate additional Stress").colored(PonderPalette.OUTPUT).pointAt(util.vector().centerOf(newEnginePos)).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.world().setKineticSpeed(newEngineSelection, 24);

        scene.idle(20);
        scene.world().setKineticSpeed(newEngineSelection, 32);
        scene.effects().rotationSpeedIndicator(newEnginePos);

        scene.idle(20);
        scene.world().setKineticSpeed(newEngineSelection, 40);

        scene.idle(20);
        scene.world().setKineticSpeed(newEngineSelection, 48);
        scene.effects().rotationSpeedIndicator(newEnginePos);

        scene.idle(20);
        scene.world().setKineticSpeed(newEngineSelection, 32);
        scene.effects().rotationSpeedIndicator(newEnginePos);
        scene.overlay().showText(60).text("It will also generate Condensate, and excessive accumulation of Condensate will limit the maximum level").colored(PonderPalette.RED).pointAt(util.vector().centerOf(newEnginePos)).placeNearTarget();

        scene.idle(20);
        scene.world().setKineticSpeed(newEngineSelection, 16);

        scene.idle(20);
        scene.world().setKineticSpeed(newEngineSelection, 8);
        scene.effects().rotationSpeedIndicator(newEnginePos);

        scene.idle(20);
        scene.world().setKineticSpeed(newEngineSelection, 0);

        scene.idle(20);
        scene.world().setBlock(pipe3Pos, AllBlocks.GLASS_FLUID_PIPE.getDefaultState().setValue(GlassFluidPipeBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(pump2Pos, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(cog3Pos, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.X), false);
        scene.world().showSection(drainSelection, Direction.DOWN);
        scene.world().showSection(newPumpSelection, Direction.DOWN);

        scene.idle(10);
        scene.world().modifyBlockEntity(drainPos, CondensateDrainBlockEntity.class, be -> {
            SmartFluidTankBehaviour.InternalFluidHandler fluidHandler = (SmartFluidTankBehaviour.InternalFluidHandler) be.getTankBehaviour().getCapability();
            fluidHandler.forceFill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);
		});
        scene.world().setKineticSpeed(newPumpSelection, 64);
        scene.effects().rotationSpeedIndicator(pump2Pos);
        scene.effects().rotationSpeedIndicator(cog3Pos);
        scene.world().propagatePipeChange(pump2Pos);

        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.OUTPUT, new Object(), drainSelection, 60);
        scene.overlay().showText(60).text("Condensate can be drained by pumping out the Condensate Drain").colored(PonderPalette.OUTPUT).pointAt(util.vector().centerOf(drainPos)).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.world().setKineticSpeed(newEngineSelection, 16);
        scene.effects().rotationSpeedIndicator(newEnginePos);

        scene.idle(20);
        scene.world().setKineticSpeed(newEngineSelection, 24);

        scene.idle(20);
        scene.world().setKineticSpeed(newEngineSelection, 32);
        scene.effects().rotationSpeedIndicator(newEnginePos);

        scene.idle(20);
        scene.world().setKineticSpeed(newEngineSelection, 40);
        scene.overlay().showText(60).text("The higher the level of the Airtight Drive Assembly, the faster the Condensate generation rate").colored(PonderPalette.OUTPUT).pointAt(util.vector().centerOf(drainPos)).placeNearTarget();

        scene.idle(20);
        scene.world().setKineticSpeed(newEngineSelection, 48);
        scene.effects().rotationSpeedIndicator(newEnginePos);

        scene.idle(20);
        scene.world().setKineticSpeed(newEngineSelection, 56);

        scene.idle(20);
        scene.world().setKineticSpeed(newEngineSelection, 64);
        scene.effects().rotationSpeedIndicator(newEnginePos);
        scene.markAsFinished();
    }
}
