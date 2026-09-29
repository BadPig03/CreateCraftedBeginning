package net.ty.createcraftedbeginning.ponder.scenes.other;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose.FlappyPose;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock.VentState;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirVentScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("air_vent", "Using Air Vents");
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

        Vec3 centerVec = util.vector().centerOf(centerPos);
        Vec3 eastControlVec = util.vector().blockSurface(centerPos, Direction.EAST).subtract(0, 0.4, 0);
        Vec3 westControlVec = util.vector().blockSurface(centerPos, Direction.WEST).subtract(0, 0.4, 0);

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        scene.idle(20);
        scene.world().showSection(centerSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Air Vents automatically connect to adjacent Air Vents").pointAt(centerVec).placeNearTarget().attachKeyFrame();

        scene.idle(26);
        placeAirVent(scene, centerAbovePos);
        scene.world().showSection(centerAboveSelection, Direction.DOWN);

        scene.idle(8);
        placeAirVent(scene, centerAbove2Pos);
        scene.world().showSection(centerAbove2Selection, Direction.DOWN);

        scene.idle(8);
        scene.effects().indicateSuccess(centerPos);
        scene.effects().indicateSuccess(centerAbovePos);

        scene.idle(8);
        scene.effects().indicateSuccess(centerAbovePos);
        scene.effects().indicateSuccess(centerAbove2Pos);

        scene.idle(25);
        scene.overlay().showControls(eastControlVec, Pointing.RIGHT, 65).rightClick().withItem(wrenchItem.copy());

        scene.idle(5);
        scene.world().modifyBlockEntity(centerPos, AirVentBlockEntity.class, vent -> vent.setLouverState(Direction.NORTH, VentState.CLOSED));
        scene.overlay().showText(60).text("Using a Wrench, louvers can be added to exposed sides").colored(PonderPalette.BLUE).pointAt(centerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Closed louvers block entities").colored(PonderPalette.RED).pointAt(centerVec).placeNearTarget().attachKeyFrame();

        scene.idle(75);
        scene.overlay().showControls(eastControlVec, Pointing.RIGHT, 65).rightClick();

        scene.idle(5);
        scene.world().modifyBlockEntity(centerPos, AirVentBlockEntity.class, vent -> vent.setLouverState(Direction.NORTH, VentState.OPENED));
        scene.overlay().showText(60).text("Right-click a louver with an empty hand to open it").colored(PonderPalette.BLUE).pointAt(centerVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        ElementLink<ParrotElement> parrot = scene.special().createBirb(util.vector().blockSurface(parrotPos, Direction.DOWN), FlappyPose::new);

        scene.idle(20);
        scene.overlay().showText(60).text("Open louvers allow entities to pass through").colored(PonderPalette.GREEN).pointAt(centerVec).placeNearTarget().attachKeyFrame();
        scene.world().showSection(fanSelection, Direction.SOUTH);

        scene.idle(15);
        scene.world().setKineticSpeed(fanSelection, mediumSpeed);
        scene.effects().rotationSpeedIndicator(fanPos);
        scene.special().rotateParrot(parrot, 0, 240, 0, 40);
        scene.special().moveParrot(parrot, util.vector().of(0, 0, 2), 40);

        scene.idle(55);
        scene.rotateCameraY(-180);

        scene.idle(30);
        scene.overlay().showControls(westControlVec, Pointing.RIGHT, 27).rightClick().withItem(wrenchItem.copy());

        scene.idle(7);
        scene.world().modifyBlockEntity(centerPos, AirVentBlockEntity.class, vent -> vent.setLouverState(Direction.SOUTH, VentState.CLOSED));

        scene.idle(27);
        scene.overlay().showControls(westControlVec, Pointing.RIGHT, 27).rightClick();

        scene.idle(7);
        scene.world().modifyBlockEntity(centerPos, AirVentBlockEntity.class, vent -> vent.setLouverState(Direction.SOUTH, VentState.OPENED));
        scene.special().rotateParrot(parrot, 0, 360, 0, 60);
        scene.special().moveParrot(parrot, util.vector().of(0, 0, 3), 60);

        scene.idle(60);
        scene.overlay().showText(60).text("Players can also crawl through connected Air Vents").colored(PonderPalette.BLUE).attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    private static void placeAirVent(CreateSceneBuilder scene, BlockPos pos) {
        scene.addInstruction(ponderScene -> {
            Level level = ponderScene.getWorld();
            BlockState state = AirVentBlock.withConnections(CCBBlocks.AIR_VENT_BLOCK.getDefaultState(), level, pos);
            level.setBlockAndUpdate(pos, state);
        });
    }
}
