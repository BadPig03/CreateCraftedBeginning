package net.ty.createcraftedbeginning.ponder.scenes.gaspipes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
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
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlock;
import org.jetbrains.annotations.NotNull;

public class AirtightCheckValveScenes {
    public static void transport(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_check_valve_transport", "One-Way Transport with Airtight Check Valves");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos encasedPipePos = util.grid().at(5, 1, 5);
        BlockPos secondPipePos = encasedPipePos.north();
        BlockPos pumpPos = secondPipePos.north();
        BlockPos cogPos = pumpPos.east();
        BlockPos motorPos = cogPos.south();
        BlockPos firstPipePos = pumpPos.north();
        BlockPos firstTankPos = firstPipePos.north();
        BlockPos thirdPipePos = encasedPipePos.west();
        BlockPos valvePos = thirdPipePos.west();
        BlockPos fourthPipePos = valvePos.west();
        BlockPos secondTankPos = fourthPipePos.west();

        Selection firstTankSelection = util.select().fromTo(firstTankPos, firstTankPos.above());
        Selection secondTankSelection = util.select().fromTo(secondTankPos, secondTankPos.above());
        Selection pumpPipeSelection = util.select().fromTo(firstPipePos, secondPipePos);
        Selection valvePipeSelection = util.select().fromTo(thirdPipePos, fourthPipePos);
        Selection encasedSelection = util.select().position(encasedPipePos);
        Selection motorSelection = util.select().fromTo(motorPos, cogPos);

        AABB valveReverseArea = new AABB(util.vector().centerOf(fourthPipePos), util.vector().centerOf(fourthPipePos));

        Object valveReverseObject = new Object();

        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(firstTankSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(pumpPipeSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(encasedSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(valvePipeSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(secondTankSelection, Direction.EAST);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.NORTH), false);
        scene.world().showSection(motorSelection, Direction.WEST);
        scene.world().setKineticSpeed(motorSelection, mediumSpeed);
        scene.world().setKineticSpeed(pumpPipeSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.overlay().showText(60).text("Airtight Check Valves allow gas flowing in a single direction only").pointAt(Vec3.atCenterOf(valvePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, valveReverseObject, valveReverseArea, 3);

        scene.idle(3);
        valveReverseArea = valveReverseArea.inflate(0.5, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, valveReverseObject, valveReverseArea, 3);

        scene.idle(3);
        valveReverseArea = valveReverseArea.expandTowards(2, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, valveReverseObject, valveReverseArea, 60);
        scene.overlay().showText(60).text("Gas attempting to flow from the opposite direction is blocked").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(valvePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
		scene.overlay().showControls(util.vector().blockSurface(valvePos, Direction.UP).subtract(0, 0.125f, 0), Pointing.DOWN, 67).rightClick().withItem(wrenchItem);
        
		scene.idle(7);
		scene.world().modifyBlock(valvePos, s -> s.setValue(AirtightCheckValveBlock.INVERTED, true), false);
        scene.overlay().showText(60).text("A Wrench can be used to reverse the direction").pointAt(Vec3.atCenterOf(valvePos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
