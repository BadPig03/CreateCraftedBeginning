package net.ty.createcraftedbeginning.ponder.scenes.gaspipes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SmartAirtightPipeScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("smart_airtight_pipe", "Controlling Gas Flow with Smart Airtight Pipes");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos encasedPipePos = util.grid().at(1, 1, 1);
        BlockPos secondPipePos = encasedPipePos.east();
        BlockPos smartPos = secondPipePos.east();
        BlockPos firstPipePos = smartPos.east();
        BlockPos firstTankPos = firstPipePos.east();
        BlockPos thirdPipePos = encasedPipePos.south();
        BlockPos pumpPos = thirdPipePos.south();
        BlockPos cogPos = pumpPos.west();
        BlockPos motorPos = cogPos.north();
        BlockPos fourthPipePos = pumpPos.south();
        BlockPos secondTankPos = fourthPipePos.south();

        Selection firstTankSelection = util.select().fromTo(firstTankPos, firstTankPos.above());
        Selection secondTankSelection = util.select().fromTo(secondTankPos, secondTankPos.above());
        Selection pumpPipeSelection = util.select().fromTo(thirdPipePos, fourthPipePos);
        Selection smartSelection = util.select().fromTo(firstPipePos, secondPipePos);
        Selection encasedSelection = util.select().position(encasedPipePos);
        Selection motorSelection = util.select().fromTo(motorPos, cogPos);
        Selection smartSingleSelection = util.select().position(smartPos);

        Vec3 smartVec = util.vector().topOf(smartPos);
        Vec3 firstPipeVec = util.vector().centerOf(firstPipePos);

        AABB smartArea = new AABB(firstPipeVec, firstPipeVec);

        Object smartObject = new Object();

        ItemStack gasCanisterItem = new ItemStack(CCBItems.GAS_CANISTER.asItem());
        ItemStack gasFilterItem = new ItemStack(CCBItems.GAS_FILTER.asItem());

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(firstTankSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(pumpPipeSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(encasedSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(smartSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(secondTankSelection, Direction.EAST);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);
        scene.world().showSection(motorSelection, Direction.EAST);

        scene.idle(15);
        scene.world().setKineticSpeed(motorSelection, mediumSpeed);
        scene.world().setKineticSpeed(pumpPipeSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.overlay().showFilterSlotInput(smartVec, Direction.UP, 60);
        scene.overlay().showText(60).text("Smart Airtight Pipes can restrict gas types passing through").colored(PonderPalette.GREEN).pointAt(smartVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(smartVec, Pointing.DOWN, 27).rightClick().withItem(gasCanisterItem.copy());

        scene.idle(7);
        scene.world().setFilterData(smartSingleSelection, SmartAirtightPipeBlockEntity.class, gasCanisterItem.copy());
        scene.overlay().showText(60).text("Right-click the filter slot with a Gas Canister or a Gas Filter to mark or change filtered gases").colored(PonderPalette.BLUE).pointAt(smartVec).placeNearTarget().attachKeyFrame();

        scene.idle(30);
        scene.overlay().showControls(smartVec, Pointing.DOWN, 37).rightClick().withItem(gasFilterItem.copy());

        scene.idle(7);
        scene.world().setFilterData(smartSingleSelection, SmartAirtightPipeBlockEntity.class, gasFilterItem.copy());

        scene.idle(43);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, smartObject, smartArea, 3);

        scene.idle(3);
        smartArea = smartArea.inflate(0.5, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, smartObject, smartArea, 3);

        scene.idle(3);
        smartArea = smartArea.expandTowards(-2.875, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, smartObject, smartArea, 60);
        scene.overlay().showText(60).text("Only matching gases are permitted to pass").colored(PonderPalette.GREEN).pointAt(smartVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}