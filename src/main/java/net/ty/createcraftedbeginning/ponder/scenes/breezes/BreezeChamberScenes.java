package net.ty.createcraftedbeginning.ponder.scenes.breezes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity.Phase;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.InactiveChamberState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeChamberScenes {
    public static void feeding(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("breeze_chamber_feeding", "Feeding Breeze Chambers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(3, 1, 3);
        BlockPos chamberPos = tankPos.above();
        BlockPos deployerPos = chamberPos.east(2);
        BlockPos deployerMotorPos = deployerPos.north();
        BlockPos armPos = tankPos.north().west(2);
        BlockPos armCogPos = armPos.north().west();
        BlockPos armMotorPos = armCogPos.above();
        BlockPos breadDepotPos = armPos.south(2);
        BlockPos pufferFishDepotPos = breadDepotPos.south().east(2);

        Selection chamberSelection = util.select().position(chamberPos);
        Selection tankSelection = util.select().position(tankPos);
        Selection deployerSelection = util.select().fromTo(deployerPos, deployerPos.below());
        Selection deployerMotorSelection = util.select().position(deployerMotorPos);
        Selection armSelection = util.select().position(armPos);
        Selection armPowerSelection = util.select().fromTo(armCogPos, armMotorPos);
        Selection breadSelection = util.select().position(breadDepotPos);
        Selection pufferFishSelection = util.select().position(pufferFishDepotPos);

        Vec3 tankVec = util.vector().centerOf(tankPos);
        Vec3 chamberVec = util.vector().centerOf(chamberPos);
        Vec3 depotVec = util.vector().centerOf(breadDepotPos);
        Vec3 chamberInteractionVec = util.vector().blockSurface(chamberPos, Direction.UP).subtract(0, 0.125, 0);

        AABB supportArea = new AABB(tankVec, chamberVec);

        Object supportObject = new Object();

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        ItemStack breadItem = new ItemStack(Items.BREAD);
        ItemStack pufferFishItem = new ItemStack(Items.PUFFERFISH);
        ItemStack milkBucketItem = new ItemStack(Items.MILK_BUCKET);
        ItemStack beetrootSoupItem = new ItemStack(Items.BEETROOT_SOUP);
        ItemStack bowlItem = new ItemStack(Items.BOWL);

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.DOWN);
        scene.world().showSection(chamberSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, supportObject, supportArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("Breeze Chambers must be placed atop Airtight Tanks").colored(PonderPalette.RED).pointAt(tankVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Foods with positive Wind Charge add to the stored Wind Charge and can put the Breeze into Gale state").colored(PonderPalette.GREEN).pointAt(chamberVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(chamberInteractionVec, Pointing.DOWN, 60).rightClick().withItem(breadItem.copy());

        scene.idle(7);
        setWindLevel(scene, chamberPos, WindLevel.GALE);

        scene.idle(73);
        scene.overlay().showText(60).text("Wind Charge is consumed over time, returning the Breeze to Calm").colored(PonderPalette.RED).pointAt(chamberVec).placeNearTarget().attachKeyFrame();

        scene.idle(40);
        setWindLevel(scene, chamberPos, WindLevel.CALM);

        scene.idle(40);
        scene.overlay().showText(60).text("Foods with negative Wind Charge reduce stored charge and can push the Breeze into Ill state").colored(PonderPalette.RED).pointAt(chamberVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(chamberInteractionVec, Pointing.DOWN, 60).rightClick().withItem(pufferFishItem.copy());

        scene.idle(7);
        setWindLevel(scene, chamberPos, WindLevel.ILL);

        scene.idle(73);
        scene.overlay().showText(60).text("Milk immediately clears the Ill state and returns the Breeze to Calm").colored(PonderPalette.GREEN).pointAt(chamberVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(chamberInteractionVec, Pointing.DOWN, 60).rightClick().withItem(milkBucketItem.copy());

        scene.idle(7);
        setWindLevel(scene, chamberPos, WindLevel.CALM);

        scene.idle(73);
        scene.world().setBlock(armMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(armSelection, Direction.DOWN);
        scene.world().showSection(armPowerSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(breadSelection, Direction.DOWN);
        scene.world().showSection(pufferFishSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().setKineticSpeed(armPowerSelection, -mediumSpeed / 2);
        scene.world().setKineticSpeed(armSelection, mediumSpeed);
        scene.effects().rotationSpeedIndicator(armPos);
        scene.overlay().showText(60).text("Mechanical Arms and Deployers can automatically feed valid items").colored(PonderPalette.BLUE).pointAt(depotVec).attachKeyFrame();
        scene.world().instructArm(armPos, Phase.MOVE_TO_INPUT, ItemStack.EMPTY, 0);

        scene.idle(40);
        scene.world().removeItemsFromBelt(breadDepotPos);
        scene.world().instructArm(armPos, Phase.MOVE_TO_OUTPUT, breadItem.copy(), 0);

        scene.idle(40);
        setWindLevel(scene, chamberPos, WindLevel.GALE);
        scene.effects().indicateSuccess(chamberPos);
        scene.world().instructArm(armPos, Phase.SEARCH_INPUTS, ItemStack.EMPTY, -1);

        scene.idle(40);
        scene.world().modifyBlockEntityNBT(deployerSelection, DeployerBlockEntity.class, compoundTag -> compoundTag.put("HeldItem", beetrootSoupItem.copy().saveOptional(scene.world().getHolderLookupProvider())));
        scene.world().setBlock(deployerMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);
        scene.world().showSection(deployerSelection, Direction.WEST);
        scene.world().showSection(deployerMotorSelection, Direction.WEST);

        scene.idle(15);
        scene.world().setKineticSpeed(deployerMotorSelection, -mediumSpeed);
        scene.world().setKineticSpeed(deployerSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(deployerPos);
        scene.world().moveDeployer(deployerPos, 1, 20);

        scene.idle(21);
        scene.world().modifyBlockEntityNBT(deployerSelection, DeployerBlockEntity.class, compoundTag -> compoundTag.put("HeldItem", bowlItem.copy().saveOptional(scene.world().getHolderLookupProvider())));
        scene.effects().indicateSuccess(chamberPos);
        scene.world().moveDeployer(deployerPos, -1, 20);

        scene.idle(20);
        scene.markAsFinished();
    }

    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("breeze_chamber_processing", "Energizing and Dissipating Gases");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(3, 1, 3);
        BlockPos chamberPos = tankPos.above();
        BlockPos outputPipePos = chamberPos.east();
        BlockPos outputPipeEndPos = outputPipePos.east();

        Selection tankSelection = util.select().position(tankPos);
        Selection chamberSelection = util.select().position(chamberPos);
        Selection outputPipeSelection = util.select().fromTo(outputPipePos, outputPipeEndPos);

        Vec3 tankVec = util.vector().centerOf(tankPos);
        Vec3 chamberVec = util.vector().centerOf(chamberPos);
        Vec3 outputPipeVec = util.vector().centerOf(outputPipeEndPos);

        AABB tankArea = new AABB(tankVec, tankVec);
        AABB chamberArea = new AABB(chamberVec, chamberVec);
        AABB processingPathArea = new AABB(tankVec, tankVec);
        AABB outputPathArea = new AABB(chamberVec, chamberVec);

        Object tankObject = new Object();
        Object chamberObject = new Object();
        Object processingPathObject = new Object();
        Object outputPathObject = new Object();

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.DOWN);
        scene.world().showSection(chamberSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, tankObject, tankArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, chamberObject, chamberArea, 3);

        scene.idle(3);
        tankArea = tankArea.inflate(0.5);
        chamberArea = chamberArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, tankObject, tankArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, chamberObject, chamberArea, 60);
        scene.overlay().showText(60).text("The Airtight Tank below supplies input gas; processed gas is buffered inside the Breeze Chamber").pointAt(chamberVec).placeNearTarget().attachKeyFrame();

        scene.idle(74);
        setWindLevel(scene, chamberPos, WindLevel.GALE);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, processingPathObject, processingPathArea, 3);

        scene.idle(3);
        processingPathArea = processingPathArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, processingPathObject, processingPathArea, 3);

        scene.idle(3);
        processingPathArea = processingPathArea.expandTowards(0, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, processingPathObject, processingPathArea, 60);
        scene.overlay().showText(60).text("In Gale state, matching Energization recipes process gas drawn from the tank below").colored(PonderPalette.GREEN).pointAt(chamberVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        setWindLevel(scene, chamberPos, WindLevel.ILL);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, processingPathObject, processingPathArea, 60);
        scene.overlay().showText(60).text("In Ill state, the Breeze Chamber runs Dissipation recipes instead").colored(PonderPalette.BLUE).pointAt(chamberVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        setWindLevel(scene, chamberPos, WindLevel.CALM);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, processingPathObject, processingPathArea, 60);
        scene.overlay().showText(60).text("Calm Breeze Chambers do not process gas").colored(PonderPalette.RED).pointAt(chamberVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        setWindLevel(scene, chamberPos, WindLevel.GALE);
        scene.world().showSection(outputPipeSelection, Direction.WEST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputPathObject, outputPathArea, 3);

        scene.idle(3);
        outputPathArea = outputPathArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputPathObject, outputPathArea, 3);

        scene.idle(3);
        outputPathArea = outputPathArea.expandTowards(2, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputPathObject, outputPathArea, 60);
        scene.overlay().showText(60).text("Processed gas can be extracted from the Breeze Chamber itself through an Airtight Pipe").colored(PonderPalette.OUTPUT).pointAt(outputPipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().hideSection(outputPipeSelection, Direction.EAST);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, chamberObject, chamberArea, 60);
        scene.overlay().showText(60).text("If its internal output buffer cannot accept more product, processing pauses until enough output is extracted").colored(PonderPalette.RED).pointAt(chamberVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void assemblyDriver(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("breeze_chamber_assembly_driver", "Supporting Airtight Assembly Drivers");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9F);
        scene.showBasePlate();

        BlockPos smallTankBottomPos = util.grid().at(5, 1, 1);
        BlockPos smallTankTopPos = smallTankBottomPos.above(2);
        BlockPos smallChamberPos = smallTankTopPos.above();
        BlockPos smallEnginePos = smallTankBottomPos.above().west();
        BlockPos smallOutletPos = smallTankTopPos.north();
        BlockPos largeTankBottomPos = smallTankBottomPos.west(2).south(2);
        BlockPos largeTankTopPos = largeTankBottomPos.above(3).west(2).south(2);
        BlockPos largeChamberLeftPos = largeTankBottomPos.above(4);
        BlockPos largeChamberRightPos = largeChamberLeftPos.west(2).south(2);
        BlockPos largeEngineLeftPos = largeTankBottomPos.above(2).west().north();
        BlockPos largeEngineRightPos = largeTankBottomPos.above(3).east();
        BlockPos largeOutletPos = largeEngineLeftPos.west(2).south(2);

        Selection smallTankSelection = util.select().fromTo(smallTankBottomPos, smallTankTopPos);
        Selection smallChamberSelection = util.select().position(smallChamberPos);
        Selection smallEngineSelection = util.select().position(smallEnginePos);
        Selection smallOutletSelection = util.select().position(smallOutletPos);
        Selection largeTankSelection = util.select().fromTo(largeTankBottomPos, largeTankTopPos);
        Selection largeChamberSelection = util.select().fromTo(largeChamberLeftPos, largeChamberRightPos);
        Selection largeEngineRightSelection = util.select().position(largeEngineRightPos);
        Selection largeOutletSelection = util.select().position(largeOutletPos);

        Vec3 smallChamberVec = util.vector().centerOf(smallChamberPos);
        Vec3 smallTankVec = util.vector().centerOf(smallTankTopPos);
        Vec3 largeChamberVec = util.vector().centerOf(largeChamberLeftPos);

        AABB smallChamberArea = new AABB(smallChamberVec, smallChamberVec);
        AABB largeChamberArea = new AABB(largeChamberVec, util.vector().centerOf(largeChamberRightPos));

        Object smallChamberObject = new Object();
        Object largeChamberObject = new Object();

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(smallTankSelection, Direction.DOWN);
        scene.world().showSection(smallChamberSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(smallEngineSelection, Direction.EAST);
        scene.world().showSection(smallOutletSelection, Direction.SOUTH);

        scene.idle(15);
        setWindLevel(scene, smallChamberPos, WindLevel.GALE);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, smallChamberObject, smallChamberArea, 3);

        scene.idle(3);
        smallChamberArea = smallChamberArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, smallChamberObject, smallChamberArea, 60);
        scene.overlay().showText(60).text("A Gale Breeze Chamber can provide Wind Charging to an Airtight Assembly Driver").colored(PonderPalette.GREEN).pointAt(smallChamberVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setKineticSpeed(smallEngineSelection, mediumSpeed);
        scene.effects().rotationSpeedIndicator(smallEnginePos);

        scene.idle(20);
        scene.overlay().showText(60).text("While the Driver is active, the chamber stops normal gas processing and serves the Driver instead").colored(PonderPalette.RED).pointAt(smallTankVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, smallChamberObject, smallChamberArea, 60);
        scene.overlay().showText(60).text("Only Gale chambers contribute Wind Charging Level; Calm and Ill chambers contribute none").colored(PonderPalette.RED).pointAt(smallChamberVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        setWindLevel(scene, smallChamberPos, WindLevel.CALM);
        scene.world().setKineticSpeed(smallEngineSelection, 0);
        scene.effects().rotationSpeedIndicator(smallEnginePos);

        scene.idle(23);
        setWindLevel(scene, smallChamberPos, WindLevel.ILL);

        scene.idle(50);
        scene.world().showSection(largeTankSelection, Direction.DOWN);
        scene.world().showSection(largeOutletSelection, Direction.EAST);
        scene.world().showSection(largeEngineRightSelection, Direction.SOUTH);
        scene.world().showSection(largeChamberSelection, Direction.DOWN);
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                BlockPos chamberPos = largeChamberLeftPos.south(i * 2).west(j * 2);
                setWindLevel(scene, chamberPos, WindLevel.GALE);
            }
        }

        scene.idle(15);
        scene.world().setKineticSpeed(largeEngineRightSelection, mediumSpeed * 2);
        scene.effects().rotationSpeedIndicator(largeEngineRightPos);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, largeChamberObject, largeChamberArea, 3);

        scene.idle(3);
        largeChamberArea = largeChamberArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, largeChamberObject, largeChamberArea, 60);
        scene.overlay().showText(60).text("Gale chambers stack their contributions; well-charged chambers contribute more").colored(PonderPalette.GREEN).pointAt(largeChamberVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    private static void setWindLevel(CreateSceneBuilder scene, BlockPos chamberPos, WindLevel windLevel) {
        scene.world().modifyBlock(chamberPos, state -> state.setValue(BreezeChamberBlock.WIND_LEVEL, windLevel), false);
        scene.world().modifyBlockEntity(chamberPos, BreezeChamberBlockEntity.class, chamber -> {
            switch (windLevel) {
                case GALE -> chamber.SwitchToGaleState();
                case ILL -> chamber.SwitchToIllState();
                case CALM -> chamber.setChamberState(new InactiveChamberState());
            }
        });
    }
}
