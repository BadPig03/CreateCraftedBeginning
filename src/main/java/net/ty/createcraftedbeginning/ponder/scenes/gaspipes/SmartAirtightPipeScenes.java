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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

public class SmartAirtightPipeScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
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

        Vec3 filterVec = util.vector().topOf(smartPos).subtract(0, 0, 0);

        AABB smartArea = new AABB(util.vector().centerOf(firstPipePos), util.vector().centerOf(firstPipePos));

        Object smartObject = new Object();

        ItemStack gasCanister = new ItemStack(CCBItems.GAS_CANISTER.asItem());
        ItemStack gasFilter = new ItemStack(CCBItems.GAS_FILTER.asItem());

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
        scene.world().setKineticSpeed(motorSelection, mediumSpeed);
        scene.world().setKineticSpeed(pumpPipeSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.overlay().showText(60).text("Smart Airtight Pipes can restrict gas types passing through").pointAt(Vec3.atCenterOf(smartPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(filterVec, Pointing.DOWN, 37).rightClick().withItem(gasCanister);

        scene.idle(7);
        scene.world().setFilterData(util.select().position(smartPos), SmartAirtightPipeBlockEntity.class, gasCanister);
        scene.overlay().showText(80).text("Right-click the filter slot with a Gas Canister or a Gas Filter to mark or change filtered gases").pointAt(Vec3.atCenterOf(smartPos)).placeNearTarget().attachKeyFrame();

        scene.idle(40);
        scene.overlay().showControls(filterVec, Pointing.DOWN, 37).rightClick().withItem(gasFilter);

        scene.idle(7);
        scene.world().setFilterData(util.select().position(smartPos), SmartAirtightPipeBlockEntity.class, gasFilter);

        scene.idle(73);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, smartObject, smartArea, 3);

        scene.idle(3);
        smartArea = smartArea.inflate(0.5, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, smartObject, smartArea, 3);

        scene.idle(3);
        smartArea = smartArea.expandTowards(-2.875f, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, smartObject, smartArea, 60);
        scene.overlay().showText(60).text("Only matching gases are permitted to pass").colored(PonderPalette.INPUT).pointAt(Vec3.atCenterOf(smartPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}