package net.ty.createcraftedbeginning.ponder.scenes.end;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public class EndSculkSilencerScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("end_sculk_silencer", "Using End Sculk Silencers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos silencerPos = util.grid().at(3, 2, 3);
        BlockPos silencerTopPos = silencerPos.above();
        BlockPos cogPos = silencerPos.below();
        BlockPos largeCogPos = cogPos.east().north();
        BlockPos motorPos = largeCogPos.above();
        BlockPos shriekerPos = cogPos.west(2);
        BlockPos calibratedPos = shriekerPos.north();
        BlockPos sensorPos = calibratedPos.north();

        Selection silencerSelection = util.select().position(silencerPos);
        Selection silencerTopSelection = util.select().position(silencerTopPos);
        Selection silencerAllSelection = util.select().fromTo(silencerPos, silencerTopPos);
        Selection cogSelection = util.select().position(cogPos);
        Selection largeCogSelection = util.select().position(largeCogPos);
        Selection motorSelection = util.select().position(motorPos);
        Selection sensorsSelection = util.select().fromTo(shriekerPos, sensorPos);

        Vec3 silencerSideVec = util.vector().blockSurface(silencerPos, Direction.NORTH);

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        Object outlineObject = new Object();

        scene.idle(20);
        scene.world().showSection(cogSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().setBlock(silencerPos, CCBBlocks.END_CASING_BLOCK.getDefaultState(), false);
        scene.world().showSection(silencerSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().setBlock(silencerTopPos, CCBBlocks.END_SCULK_SILENCER_BLOCK.getDefaultState(), false);
        scene.world().showSection(silencerTopSelection, Direction.DOWN);
        scene.overlay().showText(60).text("The End Sculk Silencer must be placed on an End Casing").pointAt(Vec3.atCenterOf(silencerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(12);
        scene.effects().indicateSuccess(silencerTopPos);
        scene.effects().indicateSuccess(silencerPos);
        scene.world().restoreBlocks(silencerAllSelection);

        scene.idle(78);
        scene.world().showSection(largeCogSelection, Direction.WEST);
        scene.world().showSection(sensorsSelection, Direction.NORTH);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(motorSelection, Direction.DOWN);
        scene.world().setKineticSpeed(motorSelection, mediumSpeed / 2);
        scene.world().setKineticSpeed(largeCogSelection, mediumSpeed / 2);
        scene.world().setKineticSpeed(cogSelection, -mediumSpeed);
        scene.world().setKineticSpeed(silencerAllSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(silencerTopPos);
        scene.overlay().showText(60).text("It can mute sounds and suppress vibration detection within its working range").pointAt(Vec3.atCenterOf(silencerTopPos)).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, outlineObject, new AABB(calibratedPos).inflate(0, 0, 1), 60);

        scene.idle(80);
        scene.overlay().showScrollInput(silencerSideVec.add(new Vec3(0, 0, 0.2)), Direction.NORTH, 60);
        scene.overlay().showText(60).text("Working ranges are configurable").pointAt(silencerSideVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("But larger working range requires higher rotation speed").pointAt(silencerSideVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(silencerSideVec, Pointing.DOWN, 60).showing(CCBIcons.I_1X1);
        scene.overlay().showText(60).text("\"1x1\": A single chunk").pointAt(Vec3.atCenterOf(silencerTopPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setKineticSpeed(motorSelection, mediumSpeed * 1.5f);
        scene.world().setKineticSpeed(largeCogSelection, mediumSpeed * 1.5f);
        scene.world().setKineticSpeed(cogSelection, -mediumSpeed * 3);
        scene.world().setKineticSpeed(silencerAllSelection, -mediumSpeed * 3);
        scene.effects().rotationSpeedIndicator(silencerTopPos);
        scene.overlay().showControls(silencerSideVec, Pointing.DOWN, 60).showing(CCBIcons.I_3X3);
        scene.overlay().showText(60).text("\"3x3\": 3x3 chunks").pointAt(Vec3.atCenterOf(silencerTopPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setKineticSpeed(motorSelection, mediumSpeed * 3);
        scene.world().setKineticSpeed(largeCogSelection, mediumSpeed * 3);
        scene.world().setKineticSpeed(cogSelection, -mediumSpeed * 6);
        scene.world().setKineticSpeed(silencerAllSelection, -mediumSpeed * 6);
        scene.effects().rotationSpeedIndicator(silencerTopPos);
        scene.overlay().showControls(silencerSideVec, Pointing.DOWN, 60).showing(CCBIcons.I_5X5);
        scene.overlay().showText(60).text("\"5x5\": 5x5 chunks").pointAt(Vec3.atCenterOf(silencerTopPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
