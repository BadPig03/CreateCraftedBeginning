package net.ty.createcraftedbeginning.ponder.scenes.gaspipes;

import com.simibubi.create.AllItems;
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
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlock;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightCheckValveScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_check_valve", "Controlling Gas Flow using Airtight Check Valves");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos encasedPipePos = util.grid().at(5, 1, 5);
        BlockPos secondPipePos = encasedPipePos.north();
        BlockPos middlePipePos = secondPipePos.north();
        BlockPos firstPipePos = middlePipePos.north();
        BlockPos firstTankPos = firstPipePos.north();
        BlockPos thirdPipePos = encasedPipePos.west();
        BlockPos valvePos = thirdPipePos.west();
        BlockPos fourthPipePos = valvePos.west();
        BlockPos secondTankPos = fourthPipePos.west();

        Selection firstTankSelection = util.select().fromTo(firstTankPos, firstTankPos.above());
        Selection secondTankSelection = util.select().fromTo(secondTankPos, secondTankPos.above());
        Selection northPipeSelection = util.select().fromTo(firstPipePos, secondPipePos);
        Selection valvePipeSelection = util.select().fromTo(thirdPipePos, fourthPipePos);
        Selection encasedSelection = util.select().position(encasedPipePos);

        Vec3 valveVec = util.vector().centerOf(valvePos);
        Vec3 encasedVec = util.vector().centerOf(encasedPipePos);

        AABB connectionArea = new AABB(valveVec, valveVec).inflate(0.3125);
        AABB blockedArea = new AABB(encasedVec, encasedVec);

        Object outputConnectionObject = new Object();
        Object inputConnectionObject = new Object();
        Object blockedObject = new Object();
        Object reversedOutputConnectionObject = new Object();
        Object reversedInputConnectionObject = new Object();

        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        scene.idle(20);
        scene.world().showSection(firstTankSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(northPipeSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(encasedSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(valvePipeSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(secondTankSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputConnectionObject, connectionArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputConnectionObject, connectionArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputConnectionObject, connectionArea.move(0.5, 0, 0), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputConnectionObject, connectionArea.move(-0.5, 0, 0), 60);
        scene.overlay().showText(60).text("Airtight Check Valves only allow gas to flow from input to output").pointAt(valveVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Check Valves do not drive gas; a pressure difference is still required").colored(PonderPalette.RED).pointAt(valveVec).placeNearTarget().attachKeyFrame();

        scene.idle(74);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, blockedObject, blockedArea, 3);

        scene.idle(3);
        blockedArea = blockedArea.inflate(0.5, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, blockedObject, blockedArea, 3);

        scene.idle(3);
        blockedArea = blockedArea.expandTowards(-1.5, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, blockedObject, blockedArea, 60);
        scene.overlay().showText(60).text("Pressure on the output side cannot force gas back through the valve").colored(PonderPalette.RED).pointAt(valveVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, reversedOutputConnectionObject, connectionArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, reversedInputConnectionObject, connectionArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, reversedOutputConnectionObject, connectionArea.move(0.5, 0, 0), 7);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, reversedInputConnectionObject, connectionArea.move(-0.5, 0, 0), 7);
        scene.overlay().showText(60).text("Use a Wrench to swap the input and output sides").colored(PonderPalette.BLUE).pointAt(valveVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().topOf(valvePos), Pointing.DOWN, 60).rightClick().withItem(wrenchItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(valvePos, state -> state.setValue(AirtightCheckValveBlock.INVERTED, false), false);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, reversedOutputConnectionObject, connectionArea.move(-0.5, 0, 0), 53);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, reversedInputConnectionObject, connectionArea.move(0.5, 0, 0), 53);

        scene.idle(53);
        scene.markAsFinished();
    }
}
