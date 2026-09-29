package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResidueOutletScenes {
    public static void residueOutlet(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("residue_outlet_handling", "Handling Residue with Residue Outlets");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9F);
        scene.showBasePlate();

        BlockPos tankBottomPos = util.grid().at(2, 1, 2);
        BlockPos tankTopPos = tankBottomPos.east(2).above(3).south(2);
        BlockPos enginePos = tankBottomPos.north().above().east(2);
        BlockPos outletPos = tankBottomPos.above(2).west();
        BlockPos funnelPos = outletPos.north();
        BlockPos fluidPipePos = outletPos.above();
        BlockPos fluidPipeEndPos = fluidPipePos.west();
        BlockPos chamberLeftPos = tankBottomPos.above(4);
        BlockPos chamberRightPos = chamberLeftPos.south(2).east(2);

        Selection tankSelection = util.select().fromTo(tankBottomPos, tankTopPos);
        Selection engineSelection = util.select().position(enginePos);
        Selection outletSelection = util.select().position(outletPos);
        Selection funnelSelection = util.select().position(funnelPos);
        Selection fluidPipeSelection = util.select().fromTo(fluidPipePos, fluidPipeEndPos);
        Selection chamberSelection = util.select().fromTo(chamberLeftPos, chamberRightPos);

        Vec3 outletVec = util.vector().centerOf(outletPos);
        Vec3 funnelVec = util.vector().centerOf(funnelPos);
        Vec3 fluidPipeVec = util.vector().centerOf(fluidPipePos);

        AABB outletArea = new AABB(outletVec, outletVec);
        AABB funnelArea = new AABB(funnelVec, funnelVec);
        AABB fluidPipeArea = new AABB(fluidPipeVec, fluidPipeVec);

        Object outletObject = new Object();
        Object funnelObject = new Object();
        Object fluidPipeObject = new Object();

        ItemStack clayBall = new ItemStack(Items.CLAY_BALL);

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(engineSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(outletSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(chamberSelection, Direction.DOWN);

        scene.idle(10);
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                setWindLevel(scene, chamberLeftPos.south(i * 2).east(j * 2));
            }
        }
        scene.world().setKineticSpeed(engineSelection, (float) 32);
        scene.effects().rotationSpeedIndicator(enginePos);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, outletObject, outletArea, 3);

        scene.idle(3);
        outletArea = outletArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, outletObject, outletArea, 60);
        scene.overlay().showText(60).text("Some gases produce Residue while powering the Driver").pointAt(outletVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(funnelSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, funnelObject, funnelArea, 60);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, funnelObject, funnelArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("Item Residue can be removed with item logistics such as Funnels").colored(PonderPalette.GREEN).pointAt(funnelVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().createItemEntity(funnelVec, Vec3.ZERO, clayBall.copy());

        scene.idle(53);
        scene.world().showSection(fluidPipeSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, fluidPipeObject, fluidPipeArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, fluidPipeObject, fluidPipeArea.inflate(0.5).expandTowards(-1, 0, 0), 60);
        scene.overlay().showText(60).text("Fluid Residue can be drained through connected fluid logistics").colored(PonderPalette.GREEN).pointAt(fluidPipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, outletObject, outletArea, 60);
        scene.overlay().showText(60).text("If no Outlet can accept more Residue, the Driver's output falls over time").colored(PonderPalette.RED).pointAt(outletVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().hideSection(funnelSelection, Direction.NORTH);
        scene.world().hideSection(fluidPipeSelection, Direction.UP);
        scene.world().setKineticSpeed(engineSelection, 16);
        scene.effects().rotationSpeedIndicator(enginePos);

        scene.idle(36);
        scene.world().setKineticSpeed(engineSelection, 8);
        scene.effects().rotationSpeedIndicator(enginePos);

        scene.idle(17);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, outletObject, outletArea, 60);
        scene.overlay().showText(60).text("Once Residue can leave again, the Driver gradually recovers").colored(PonderPalette.GREEN).pointAt(outletVec).placeNearTarget().attachKeyFrame();

        scene.idle(19);
        scene.world().setKineticSpeed(engineSelection, 4);
        scene.effects().rotationSpeedIndicator(enginePos);

        scene.idle(36);
        scene.world().setKineticSpeed(engineSelection, 0);
        scene.effects().rotationSpeedIndicator(enginePos);

        scene.idle(5);
        scene.markAsFinished();
    }

    private static void setWindLevel(CreateSceneBuilder scene, BlockPos chamberPos) {
        scene.world().modifyBlock(chamberPos, state -> state.setValue(BreezeChamberBlock.WIND_LEVEL, WindLevel.GALE), false);
        scene.world().modifyBlockEntity(chamberPos, BreezeChamberBlockEntity.class, BreezeChamberBlockEntity::SwitchToGaleState);
    }
}
