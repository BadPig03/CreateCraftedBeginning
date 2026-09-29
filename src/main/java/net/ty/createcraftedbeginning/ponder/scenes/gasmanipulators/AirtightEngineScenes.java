package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllItems;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.InactiveChamberState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightEngineScenes {
    public static void settingUp(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_engine_setting_up", "Setting Up an Airtight Assembly Driver");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9F);
        scene.showBasePlate();

        BlockPos smallTankBottomPos = util.grid().at(5, 1, 1);
        BlockPos smallTankTopPos = smallTankBottomPos.above(2);
        BlockPos smallTankChamberPos = smallTankTopPos.above();
        BlockPos smallTankEnginePos = smallTankBottomPos.above().west();
        BlockPos smallTankOutletPos = smallTankTopPos.north();
        BlockPos smallEngineCogPos = smallTankEnginePos.above().north();
        BlockPos largeTankBottomPos = smallTankBottomPos.west(2).south(2);
        BlockPos largeTankTopPos = largeTankBottomPos.above(3).west(2).south(2);
        BlockPos largeTankChamberLeftPos = largeTankBottomPos.above(4);
        BlockPos largeTankChamberRightPos = largeTankChamberLeftPos.west(2).south(2);
        BlockPos largeTankEngineLeftPos = largeTankBottomPos.above(2).west().north();
        BlockPos largeTankEngineRightPos = largeTankBottomPos.above(3).east();
        BlockPos largeTankOutletPos = largeTankEngineLeftPos.west(2).south(2);

        Selection smallTankSelection = util.select().fromTo(smallTankBottomPos, smallTankTopPos);
        Selection smallTankChamberSelection = util.select().position(smallTankChamberPos);
        Selection smallTankEngineSelection = util.select().position(smallTankEnginePos);
        Selection smallTankOutletSelection = util.select().position(smallTankOutletPos);
        Selection smallEngineCogSelection = util.select().position(smallEngineCogPos);
        Selection largeTankSelection = util.select().fromTo(largeTankBottomPos, largeTankTopPos);
        Selection largeTankChamberSelection = util.select().fromTo(largeTankChamberLeftPos, largeTankChamberRightPos);
        Selection largeTankEngineLeftSelection = util.select().position(largeTankEngineLeftPos);
        Selection largeTankEngineRightSelection = util.select().position(largeTankEngineRightPos);
        Selection largeTankOutletSelection = util.select().position(largeTankOutletPos);

        Vec3 smallTankEngineVec = util.vector().centerOf(smallTankEnginePos);
        Vec3 smallTankTopVec = util.vector().centerOf(smallTankTopPos);
        Vec3 smallEngineCogVec = util.vector().centerOf(smallEngineCogPos);
        Vec3 largeTankVec = util.vector().centerOf(largeTankBottomPos.above(2));
        Vec3 smallTankChamberVec = util.vector().centerOf(smallTankChamberPos);
        Vec3 smallTankOutletVec = util.vector().centerOf(smallTankOutletPos);

        AABB smallTankArea = new AABB(util.vector().centerOf(smallTankBottomPos), smallTankTopVec);
        AABB smallTankChamberArea = new AABB(smallTankChamberVec, smallTankChamberVec);
        AABB smallTankOutletArea = new AABB(smallTankOutletVec, smallTankOutletVec);
        AABB largeTankArea = new AABB(util.vector().centerOf(largeTankBottomPos), util.vector().centerOf(largeTankTopPos));

        Object smallTankObject = new Object();
        Object smallTankChamberObject = new Object();
        Object smallTankOutletObject = new Object();
        Object largeTankObject = new Object();

        scene.idle(20);
        scene.world().showSection(smallTankSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(smallTankEngineSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, smallTankObject, smallTankArea, 3);

        scene.idle(3);
        smallTankArea = smallTankArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, smallTankObject, smallTankArea, 60);
        scene.overlay().showText(60).text("Attach an Airtight Engine directly to an Airtight Tank to form a Driver").colored(PonderPalette.GREEN).pointAt(smallTankEngineVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(smallTankOutletSelection, Direction.SOUTH);

        scene.idle(15);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, smallTankOutletObject, smallTankOutletArea, 3);

        scene.idle(3);
        smallTankOutletArea = smallTankOutletArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, smallTankOutletObject, smallTankOutletArea, 60);
        scene.overlay().showText(60).text("At least one Residue Outlet is required for the Driver to run").colored(PonderPalette.GREEN).pointAt(smallTankOutletVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(smallTankChamberSelection, Direction.DOWN);

        scene.idle(7);
        setWindLevel(scene, smallTankChamberPos, WindLevel.GALE);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, smallTankChamberObject, smallTankChamberArea, 3);

        scene.idle(3);
        smallTankChamberArea = smallTankChamberArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, smallTankChamberObject, smallTankChamberArea, 60);
        scene.overlay().showText(60).text("Gale Breeze Chambers above the tank provide the Driver's wind charge").colored(PonderPalette.GREEN).pointAt(smallTankChamberVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Each Airtight Engine acts as a large cogwheel output").pointAt(smallTankEngineVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(smallEngineCogSelection, Direction.NORTH);
        scene.overlay().showText(60).text("Mesh another cogwheel with it to join the kinetic network").colored(PonderPalette.GREEN).pointAt(smallEngineCogVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(largeTankSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(largeTankOutletSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(largeTankEngineLeftSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(largeTankEngineRightSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(largeTankChamberSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, largeTankObject, largeTankArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("Larger Airtight Tanks provide more gas storage and more attachment space").colored(PonderPalette.GREEN).pointAt(largeTankVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, largeTankObject, largeTankArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("Tank size alone does not increase the Driver's output").colored(PonderPalette.RED).pointAt(largeTankVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void generating(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_engine_generating_rotational_force", "Generating Rotational Force with an Airtight Assembly Driver");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9F);
        scene.showBasePlate();

        BlockPos tankBottomPos = util.grid().at(2, 1, 2);
        BlockPos tankTopPos = tankBottomPos.east(2).above(3).south(2);
        BlockPos innerGasPipePos = tankBottomPos.west();
        BlockPos outerGasPipePos = innerGasPipePos.west();
        BlockPos engineBottomPos = tankBottomPos.north().above().east(2);
        BlockPos engineTopPos = engineBottomPos.above(2).west();
        BlockPos outletPos = tankBottomPos.above(2).west();
        BlockPos chamberLeftPos = tankBottomPos.above(4);
        BlockPos chamberRightPos = chamberLeftPos.south(2).east(2);

        Selection tankSelection = util.select().fromTo(tankBottomPos, tankTopPos);
        Selection outletSelection = util.select().position(outletPos);
        Selection engineBottomSelection = util.select().position(engineBottomPos);
        Selection engineTopSelection = util.select().position(engineTopPos);
        Selection chamberSelection = util.select().fromTo(chamberLeftPos, chamberRightPos);
        Selection gasSupplySelection = util.select().fromTo(innerGasPipePos, outerGasPipePos);

        Vec3 outerGasPipeVec = util.vector().centerOf(outerGasPipePos);
        Vec3 chamberLeftVec = util.vector().centerOf(chamberLeftPos);
        Vec3 outletVec = util.vector().centerOf(outletPos);
        Vec3 engineTopVec = util.vector().centerOf(engineTopPos);

        AABB gasSupplyArea = new AABB(outerGasPipeVec, outerGasPipeVec);
        AABB chamberArea = new AABB(chamberLeftVec, util.vector().centerOf(chamberRightPos));
        AABB residueArea = new AABB(outletVec, outletVec);

        Object gasSupplyObject = new Object();
        Object chamberObject = new Object();
        Object residueObject = new Object();

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(engineBottomSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(outletSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(chamberSelection, Direction.DOWN);

        scene.idle(15);
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                setWindLevel(scene, chamberLeftPos.south(i * 2).east(j * 2), WindLevel.GALE);
            }
        }

        scene.idle(20);
        scene.world().showSection(gasSupplySelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, gasSupplyObject, gasSupplyArea, 3);

        scene.idle(3);
        gasSupplyArea = gasSupplyArea.inflate(0.5, 0.375, 0.375).expandTowards(1, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, gasSupplyObject, gasSupplyArea, 60);
        scene.overlay().showText(60).text("Supplying a supported gas starts the Driver").colored(PonderPalette.INPUT).pointAt(outerGasPipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().setKineticSpeed(engineBottomSelection, mediumSpeed);
        scene.effects().rotationSpeedIndicator(engineBottomPos);

        scene.idle(73);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, gasSupplyObject, gasSupplyArea, 60);
        scene.overlay().showText(60).text("Gas type and pressure determine how much output that supply can sustain").colored(PonderPalette.INPUT).pointAt(outerGasPipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        chamberArea = chamberArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, chamberObject, chamberArea, 60);
        scene.overlay().showText(60).text("Insufficient wind makes the Driver slow down even with gas available").colored(PonderPalette.RED).pointAt(chamberLeftVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }

                setWindLevel(scene, chamberLeftPos.south(i * 2).east(j * 2), WindLevel.CALM);
            }
        }
        scene.world().setKineticSpeed(engineBottomSelection, mediumSpeed / 4);
        scene.effects().rotationSpeedIndicator(engineBottomPos);

        scene.idle(73);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, chamberObject, chamberArea, 60);
        scene.overlay().showText(60).text("Restoring wind lets the Driver recover within its other limits").colored(PonderPalette.GREEN).pointAt(chamberLeftVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                setWindLevel(scene, chamberLeftPos.south(i * 2).east(j * 2), WindLevel.GALE);
            }
        }
        scene.world().setKineticSpeed(engineBottomSelection, mediumSpeed);
        scene.effects().rotationSpeedIndicator(engineBottomPos);

        scene.idle(73);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, residueObject, residueArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("Blocked Residue Outlets limit the Driver's output and can eventually stop it").colored(PonderPalette.RED).pointAt(outletVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().setKineticSpeed(engineBottomSelection, mediumSpeed / 2);
        scene.effects().rotationSpeedIndicator(engineBottomPos);

        scene.idle(73);
        scene.world().showSection(engineTopSelection, Direction.SOUTH);
        scene.overlay().showText(60).text("Adding another Airtight Engine adds another output point...").colored(PonderPalette.GREEN).pointAt(engineTopVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().setKineticSpeed(engineBottomSelection, mediumSpeed / 4);
        scene.world().setKineticSpeed(engineTopSelection, mediumSpeed / 4);
        scene.effects().rotationSpeedIndicator(engineBottomPos);
        scene.effects().rotationSpeedIndicator(engineTopPos);

        scene.idle(73);
        scene.overlay().showText(60).text("...but the same total output is shared evenly among all attached Engines").colored(PonderPalette.RED).pointAt(engineTopVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Use a Wrench on an Airtight Engine to reverse its rotation direction").colored(PonderPalette.BLUE).pointAt(engineTopVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().topOf(engineTopPos), Pointing.DOWN, 60).rightClick().withItem(wrenchItem.copy());

        scene.idle(7);
        scene.world().setKineticSpeed(engineTopSelection, -mediumSpeed / 4);
        scene.effects().rotationSpeedIndicator(engineTopPos);

        scene.idle(53);
        scene.markAsFinished();
    }

    private static void setWindLevel(CreateSceneBuilder scene, BlockPos chamberPos, WindLevel windLevel) {
        scene.world().modifyBlock(chamberPos, state -> state.setValue(BreezeChamberBlock.WIND_LEVEL, windLevel), false);
        scene.world().modifyBlockEntity(chamberPos, BreezeChamberBlockEntity.class, chamber -> {
            if (windLevel != WindLevel.GALE) {
                chamber.setChamberState(new InactiveChamberState());
                return;
            }

            chamber.SwitchToGaleState();
        });
    }
}
