package net.ty.createcraftedbeginning.ponder.scenes.other;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose.FlappyPose;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock.VentState;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public class AirVentScenes {
    public static void placement(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("air_vent_placement", "Placing Air Vents");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos centerPos = util.grid().at(3, 1, 3);
        BlockPos centerAbovePos = centerPos.above();
        BlockPos centerAbove2Pos = centerAbovePos.above();
        BlockPos parrotPos = centerPos.north(2);
        BlockPos fanPos = centerPos.north(3);

        Selection centerSelection = util.select().position(centerPos);
        Selection centerAboveSelection = util.select().position(centerAbovePos);
        Selection centerAbove2Selection = util.select().position(centerAbove2Pos);
        Selection fanSelection = util.select().position(fanPos);

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        scene.idle(20);
        scene.world().showSection(centerSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Placed Air Vents automatically connect to adjacent vents").pointAt(Vec3.atCenterOf(centerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(30);
        scene.world().setBlock(centerAbovePos, CCBBlocks.AIR_VENT_BLOCK.getDefaultState(), false);
        scene.world().showSection(centerAboveSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().setBlock(centerAbove2Pos, CCBBlocks.AIR_VENT_BLOCK.getDefaultState(), false);
        scene.world().showSection(centerAbove2Selection, Direction.DOWN);

        scene.idle(5);
        scene.effects().indicateSuccess(centerPos);
        scene.effects().indicateSuccess(centerAbovePos);
        scene.world().modifyBlock(centerPos, s -> s.setValue(AirVentBlock.UP, VentState.CONNECTED), false);
        scene.world().modifyBlock(centerAbovePos, s -> s.setValue(AirVentBlock.DOWN, VentState.CONNECTED), false);

        scene.idle(5);
        scene.effects().indicateSuccess(centerAbovePos);
        scene.effects().indicateSuccess(centerAbove2Pos);
        scene.world().modifyBlock(centerAbovePos, s -> s.setValue(AirVentBlock.UP, VentState.CONNECTED), false);
        scene.world().modifyBlock(centerAbove2Pos, s -> s.setValue(AirVentBlock.DOWN, VentState.CONNECTED), false);

        scene.idle(35);
        scene.overlay().showControls(util.vector().blockSurface(centerPos, Direction.EAST).subtract(0, 0.4f, 0), Pointing.RIGHT, 67).rightClick().withItem(wrenchItem);

        scene.idle(7);
        scene.world().modifyBlock(centerPos, s -> s.setValue(AirVentBlock.NORTH, VentState.CLOSED), false);
        scene.overlay().showText(60).text("Use a Wrench to install Louvered Vents on Air Vents").pointAt(Vec3.atCenterOf(centerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(centerPos, Direction.EAST).subtract(0, 0.4f, 0), Pointing.RIGHT, 67).rightClick();

        scene.idle(7);
        scene.world().modifyBlock(centerPos, s -> s.setValue(AirVentBlock.NORTH, VentState.OPENED), false);
        scene.overlay().showText(60).text("Right-click with empty hand to open Louvered Vents").pointAt(Vec3.atCenterOf(centerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        ElementLink<ParrotElement> parrot = scene.special().createBirb(util.vector().blockSurface(parrotPos, Direction.DOWN), FlappyPose::new);

        scene.idle(20);
        scene.overlay().showText(60).text("Only open Louvered Vents permit passage").pointAt(Vec3.atCenterOf(centerPos)).placeNearTarget().attachKeyFrame();
        scene.world().showSection(fanSelection, Direction.SOUTH);

        scene.idle(20);
        scene.world().setKineticSpeed(fanSelection, mediumSpeed);
        scene.effects().rotationSpeedIndicator(fanPos);
        scene.special().rotateParrot(parrot, 0, 240, 0, 40);
        scene.special().moveParrot(parrot, util.vector().of(0, 0, 2), 40);

        scene.idle(60);
        scene.rotateCameraY(-180);

        scene.idle(30);
        scene.overlay().showControls(util.vector().blockSurface(centerPos, Direction.WEST).subtract(0, 0.4f, 0), Pointing.RIGHT, 27).rightClick().withItem(wrenchItem);

        scene.idle(7);
        scene.world().modifyBlock(centerPos, s -> s.setValue(AirVentBlock.SOUTH, VentState.CLOSED), false);

        scene.idle(27);
        scene.overlay().showControls(util.vector().blockSurface(centerPos, Direction.WEST).subtract(0, 0.4f, 0), Pointing.RIGHT, 27).rightClick();

        scene.idle(7);
        scene.world().modifyBlock(centerPos, s -> s.setValue(AirVentBlock.SOUTH, VentState.OPENED), false);
        scene.special().rotateParrot(parrot, 0, 360, 0, 60);
        scene.special().moveParrot(parrot, util.vector().of(0, 0, 3), 60);

        scene.idle(60);
        scene.overlay().showText(60).text("Players can crawl through connected Air Vents").attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
