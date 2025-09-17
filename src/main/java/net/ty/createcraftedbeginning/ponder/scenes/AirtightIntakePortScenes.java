package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airtightintakeport.AirtightIntakePortBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public class AirtightIntakePortScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_intake_port", "Producing Compressed Air using Airtight Intake Ports");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos portPos = util.grid().at(2, 1, 3);
        BlockPos pumpPos = util.grid().at(3, 1, 3);
        BlockPos cogPos = util.grid().at(3, 1, 4);
        BlockPos pipePos = util.grid().at(4, 1, 3);
        BlockPos tankPos = util.grid().at(5, 1, 3);
        BlockPos motorPos = util.grid().at(4, 1, 4);
        BlockPos glassPos = util.grid().at(1, 1, 3);

        Selection pumpSelection = util.select().fromTo(pumpPos, pumpPos);
        Selection portSelection = util.select().fromTo(portPos, portPos);
        Selection sourceSelection = util.select().fromTo(cogPos, motorPos);
        Selection tankSelection = util.select().fromTo(pipePos, tankPos);
        Selection glassSelection = util.select().fromTo(glassPos, glassPos);

        ItemStack wrench = new ItemStack(AllItems.WRENCH.asItem());
        ItemStack bucket = new ItemStack(Items.BUCKET);
        ItemStack waterBucket = new ItemStack(Items.WATER_BUCKET);

        AABB bb = new AABB(util.vector().centerOf(pumpPos), util.vector().centerOf(pumpPos)).inflate(0.5);
        AABB bb1 = bb.move(-1, 0, 0);

        scene.world().setBlock(portPos, CCBBlocks.AIRTIGHT_INTAKE_PORT_BLOCK.getDefaultState().setValue(AirtightIntakePortBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(pumpPos, CCBBlocks.AIRTIGHT_PUMP_BLOCK.getDefaultState().setValue(AirtightPumpBlock.FACING, Direction.EAST), false);
        scene.world().setBlock(cogPos, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(pipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(AirtightPipeBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(tankPos, CCBBlocks.AIRTIGHT_TANK_BLOCK.getDefaultState(), false);

        scene.world().showSection(pumpSelection, Direction.DOWN);
        scene.world().showSection(portSelection, Direction.DOWN);
        scene.world().showSection(sourceSelection, Direction.DOWN);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(10);
        scene.world().setKineticSpeed(sourceSelection, 128);
        scene.world().setKineticSpeed(pumpSelection, -128);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.world().modifyBlockEntity(pumpPos, AirtightPumpBlockEntity.class, be -> be.onSpeedChanged(0));
        scene.overlay().showText(60).text("The Airtight Intake Port can continuously produce Compressed Air under specified conditions").pointAt(Vec3.atCenterOf(portPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Condition 1: \nThe Airtight Intake Port must be directly attached to an Airtight Pump").colored(PonderPalette.BLUE).pointAt(Vec3.atCenterOf(portPos)).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, bb, bb, 1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, bb1, bb, 1);

        scene.idle(1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, bb, bb, 59);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, bb1, bb1, 59);

        scene.idle(79);
        scene.overlay().showControls(util.vector().blockSurface(pumpPos, Direction.NORTH), Pointing.RIGHT, 10).rightClick().withItem(wrench);

        scene.idle(7);
        scene.world().modifyBlock(pumpPos, s -> s.setValue(PumpBlock.FACING, Direction.WEST), false);

        scene.idle(20);
        scene.overlay().showText(60).text("Condition 2: \nThe direction of the Airtight Pump must oppose the intake direction of the Airtight Intake Port").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(portPos)).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().blockSurface(pumpPos, Direction.NORTH), Pointing.RIGHT, 60).showing(AllIcons.I_MTD_CLOSE);

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(pumpPos, Direction.NORTH), Pointing.RIGHT, 10).rightClick().withItem(wrench);

        scene.idle(7);
        scene.world().modifyBlock(pumpPos, s -> s.setValue(PumpBlock.FACING, Direction.EAST), false);

        scene.idle(20);
        scene.world().setBlock(glassPos, Blocks.GLASS.defaultBlockState(), false);
        scene.world().showSection(glassSelection, Direction.DOWN);

        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.RED, new Object(), glassSelection, 60);
        scene.overlay().showText(60).text("Condition 3: \nThe front face of the Airtight Intake Port must remain unobstructed").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(glassPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().destroyBlock(glassPos);

        scene.idle(20);
        scene.overlay().showControls(util.vector().blockSurface(portPos, Direction.UP), Pointing.DOWN, 10).rightClick().withItem(waterBucket);

        scene.idle(7);
        scene.world().modifyBlock(portPos, s -> s.setValue(AirtightIntakePortBlock.WATERLOGGED, true), false);

        scene.idle(20);
        scene.overlay().showOutline(PonderPalette.RED, new Object(), portSelection, 60);
        scene.overlay().showText(60).text("Condition 4: \nThe Airtight Intake Port must not be waterlogged").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(portPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(portPos, Direction.UP), Pointing.DOWN, 10).rightClick().withItem(bucket);

        scene.idle(7);
        scene.world().modifyBlock(portPos, s -> s.setValue(AirtightIntakePortBlock.WATERLOGGED, false), false);

        scene.idle(20);
        scene.world().setKineticSpeed(sourceSelection, 30);
        scene.world().setKineticSpeed(pumpSelection, -30);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.world().modifyBlockEntity(pumpPos, AirtightPumpBlockEntity.class, be -> be.onSpeedChanged(0));

        scene.idle(10);
        scene.overlay().showText(60).text("Condition 5: \nThe Airtight Pump must maintain at least medium rotational speed").colored(PonderPalette.MEDIUM).pointAt(Vec3.atCenterOf(pumpPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
