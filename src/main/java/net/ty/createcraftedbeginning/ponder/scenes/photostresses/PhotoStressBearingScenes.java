package net.ty.createcraftedbeginning.ponder.scenes.photostresses;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PhotoStressBearingScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("photo_stress_bearing", "Generating Rotational Force using Photo-Stress Bearings");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos bearingPos = util.grid().at(4, 2, 3);
        BlockPos gearboxPos = bearingPos.below();
        BlockPos shaftPos = gearboxPos.west();
        BlockPos speedometerPos = shaftPos.west();
        BlockPos stressometerPos = speedometerPos.west();
        BlockPos fiberPos = bearingPos.south();
        BlockPos primaryLightPos = fiberPos.south();
        BlockPos eastLightPos = fiberPos.east();
        BlockPos westLightPos = fiberPos.west();
        BlockPos upperLightPos = fiberPos.above();

        Selection bearingSelection = util.select().fromTo(gearboxPos, bearingPos);
        Selection shaftSelection = util.select().position(shaftPos);
        Selection metersSelection = util.select().fromTo(speedometerPos, stressometerPos);
        Selection driveSelection = util.select().fromTo(stressometerPos, bearingPos);
        Selection inputSelection = util.select().fromTo(fiberPos, primaryLightPos);

        Vec3 bearingSurface = util.vector().blockSurface(bearingPos, Direction.WEST).subtract(0, 0.125, 0);
        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.world().setBlock(fiberPos, CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState(), false);
        scene.world().setBlock(primaryLightPos, Blocks.SEA_LANTERN.defaultBlockState(), false);

        scene.idle(20);
        scene.world().showIndependentSection(bearingSelection, Direction.DOWN);
        scene.idle(5);
        scene.world().showIndependentSection(shaftSelection, Direction.EAST);
        scene.idle(5);
        scene.world().showIndependentSection(metersSelection, Direction.EAST);
        scene.idle(5);
        scene.world().showIndependentSection(inputSelection, Direction.NORTH);

        scene.idle(15);
        scene.world().setKineticSpeed(driveSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(bearingPos);

        scene.idle(20);
        scene.overlay().showText(60).text("Connect a full-bright solid light source to the bearing with Optical Fiber").pointAt(Vec3.atCenterOf(fiberPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).colored(PonderPalette.GREEN).text("One ordinary light source provides 32 SU while the bearing runs at 32 RPM").pointAt(Vec3.atCenterOf(speedometerPos)).placeNearTarget().attachKeyFrame();
        scene.effects().rotationSpeedIndicator(speedometerPos);

        scene.idle(80);
        scene.world().setBlock(eastLightPos, Blocks.SEA_LANTERN.defaultBlockState(), false);
        scene.world().showIndependentSection(util.select().position(eastLightPos), Direction.WEST);
        scene.idle(8);
        scene.world().setBlock(westLightPos, Blocks.SEA_LANTERN.defaultBlockState(), false);
        scene.world().showIndependentSection(util.select().position(westLightPos), Direction.EAST);
        scene.idle(8);
        scene.world().setBlock(upperLightPos, Blocks.SEA_LANTERN.defaultBlockState(), false);
        scene.world().showIndependentSection(util.select().position(upperLightPos), Direction.DOWN);
        scene.idle(12);
        scene.overlay().showText(70).colored(PonderPalette.OUTPUT).text("Adding independent light sources increases power, but their coupling efficiency falls").pointAt(Vec3.atCenterOf(fiberPos)).placeNearTarget().attachKeyFrame();

        scene.idle(90);
        scene.overlay().showText(70).colored(PonderPalette.RED).text("Four to five ordinary sources are the sweet spot; adding more begins to reduce total output").pointAt(Vec3.atCenterOf(stressometerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(90);
        scene.overlay().showText(70).text("The bearing itself never checks the sky or weather; each source decides when it is active").pointAt(Vec3.atCenterOf(bearingPos)).placeNearTarget().attachKeyFrame();

        scene.idle(90);
        scene.overlay().showControls(bearingSurface, Pointing.DOWN, 60).rightClick();
        scene.overlay().showFilterSlotInput(bearingSurface, Direction.WEST, 50);
        scene.world().setKineticSpeed(driveSelection, mediumSpeed);
        scene.effects().rotationSpeedIndicator(bearingPos);

        scene.idle(7);
        scene.overlay().showText(60).text("Use the value panel to configure its rotation direction").pointAt(bearingSurface).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
