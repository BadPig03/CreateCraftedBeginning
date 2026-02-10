package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.InternalFluidHandler;
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
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import org.jetbrains.annotations.NotNull;

public class ResidueOutletScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("residue_outlet", "Expelling Residue via Residue Outlets");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9f);
        scene.showBasePlate();

        BlockPos rightTankPos = util.grid().at(3, 1, 3);
        BlockPos leftTankPos = rightTankPos.east().south().above(2);
        BlockPos chamberPos = leftTankPos.above();
        BlockPos enginePos = leftTankPos.west(2);
        BlockPos airtightPipePos = leftTankPos.north(2);
        BlockPos airtightPumpPos = airtightPipePos.north(2);
        BlockPos airtightCogPos = airtightPumpPos.east();
        BlockPos airtightMotorPos = airtightCogPos.south();
        BlockPos rightOutletPos = rightTankPos.west().above();
        BlockPos leftOutletPos = rightTankPos.east().north().above();
        BlockPos funnelPos = rightOutletPos.west();
        BlockPos leftPipePos = leftOutletPos.west();
        BlockPos middlePipePos = leftPipePos.west();
        BlockPos rightPipePos = middlePipePos.west();
        BlockPos pumpPos = rightPipePos.west();
        BlockPos cogPos = pumpPos.north();
        BlockPos motorPos = cogPos.east();

        Selection tankSelection = util.select().fromTo(rightTankPos, leftTankPos);
        Selection rightOutletSelection = util.select().position(rightOutletPos);
        Selection chamberSelection = util.select().position(chamberPos);
        Selection engineSelection = util.select().position(enginePos);
        Selection airtightPipeSelection = util.select().fromTo(airtightPumpPos, airtightPipePos);
        Selection airtightSourceSelection = util.select().fromTo(airtightCogPos, airtightMotorPos);
        Selection firstPipeSelection = util.select().fromTo(middlePipePos, pumpPos);
        Selection cogSelection = util.select().fromTo(cogPos, motorPos);
        Selection secondPipeSelection = util.select().fromTo(leftOutletPos, leftPipePos);
        Selection funnelSelection = util.select().position(funnelPos);

        AABB tankArea = new AABB(util.vector().centerOf(rightTankPos), util.vector().centerOf(rightTankPos));
        AABB pipeArea = new AABB(util.vector().centerOf(middlePipePos), util.vector().centerOf(middlePipePos));
        AABB funnelArea = new AABB(util.vector().centerOf(funnelPos), util.vector().centerOf(funnelPos));

        Object tankObject = new Object();
        Object pipeObject = new Object();
        Object funnelObject = new Object();

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(rightOutletSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 3);

        scene.idle(3);
        tankArea = tankArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 3);

        scene.idle(3);
        tankArea = tankArea.expandTowards(1, 2, 1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 60);
        scene.overlay().showText(60).text("Residue Outlets must be placed on Airtight Tanks").pointAt(Vec3.atCenterOf(rightOutletPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(engineSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(chamberSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(airtightPipeSelection, Direction.SOUTH);
        scene.world().setBlock(airtightMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.NORTH), false);
        scene.world().showSection(airtightSourceSelection, Direction.WEST);
        scene.world().modifyBlockEntity(chamberPos, BreezeChamberBlockEntity.class, BreezeChamberBlockEntity::SwitchToGaleState);
        scene.world().modifyBlock(chamberPos, s -> s.setValue(BreezeChamberBlock.WIND_LEVEL, WindLevel.GALE), false);
        scene.world().setKineticSpeed(airtightSourceSelection, mediumSpeed);
        scene.world().setKineticSpeed(airtightPipeSelection, -mediumSpeed);
        scene.world().setKineticSpeed(engineSelection, 8);
        scene.effects().rotationSpeedIndicator(airtightPumpPos);
        scene.effects().rotationSpeedIndicator(enginePos);
        scene.overlay().showText(60).text("Airtight Assembly Driver gradually generate residue while producing Stress").pointAt(Vec3.atCenterOf(enginePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().modifyBlock(middlePipePos, s -> s.setValue(FluidPipeBlock.EAST, false), false);
        scene.world().showSection(firstPipeSelection, Direction.EAST);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), false);
        scene.world().showSection(cogSelection, Direction.EAST);
        scene.world().showSection(funnelSelection, Direction.EAST);
        scene.world().setKineticSpeed(firstPipeSelection, mediumSpeed);
        scene.world().setKineticSpeed(cogSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.world().modifyBlockEntity(rightOutletPos, ResidueOutletBlockEntity.class, be -> {
            InternalFluidHandler handler = (InternalFluidHandler) be.getFluidTankBehaviour().getCapability();
            handler.forceFill(new FluidStack(Fluids.WATER, 4000), FluidAction.EXECUTE);
        });
        scene.world().propagatePipeChange(pumpPos);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pipeObject, pipeArea, 3);

        scene.idle(3);
        pipeArea = pipeArea.inflate(0.5, 0.375, 0.375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pipeObject, pipeArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, funnelObject, funnelArea, 3);

        scene.idle(3);
        pipeArea = pipeArea.expandTowards(-2, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pipeObject, pipeArea, 60);
        funnelArea = funnelArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, funnelObject, funnelArea, 60);
        scene.overlay().showText(60).text("Untreated residue accumulation will degrade the Residue Level of the Airtight Assembly Driver").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(rightOutletPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(secondPipeSelection, Direction.WEST);

        scene.idle(10);
        scene.world().modifyBlock(middlePipePos, s -> s.setValue(FluidPipeBlock.EAST, true), false);
        scene.world().modifyBlockEntity(leftOutletPos, ResidueOutletBlockEntity.class, be -> {
            InternalFluidHandler handler = (InternalFluidHandler) be.getFluidTankBehaviour().getCapability();
            handler.forceFill(new FluidStack(Fluids.WATER, 4000), FluidAction.EXECUTE);
        });
        scene.world().propagatePipeChange(pumpPos);

        scene.idle(10);
        scene.overlay().showText(60).text("Additional Residue Outlets can be placed as needed").pointAt(Vec3.atCenterOf(leftOutletPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
