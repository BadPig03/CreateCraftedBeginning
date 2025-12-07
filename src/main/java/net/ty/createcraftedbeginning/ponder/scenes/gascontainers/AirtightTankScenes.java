package net.ty.createcraftedbeginning.ponder.scenes.gascontainers;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.gui.AllIcons;
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
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

public class AirtightTankScenes {
    public static void storage(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_tank_storage", "Storing Gas in Airtight Tanks");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(3, 1, 3);
        BlockPos leftPipePos = tankPos.west();
        BlockPos pumpPos = leftPipePos.west();
        BlockPos cogPos = pumpPos.south();
        BlockPos motorPos = cogPos.east();
        BlockPos rightPipePos = pumpPos.west();

        Selection tankSelection = util.select().position(tankPos);
        Selection pipeSelection = util.select().fromTo(leftPipePos, rightPipePos);
        Selection pumpSelection = util.select().position(pumpPos);
        Selection cogSelection = util.select().fromTo(cogPos, motorPos);

        AABB inputArea = new AABB(util.vector().centerOf(rightPipePos), util.vector().centerOf(rightPipePos));
        AABB outputArea = new AABB(util.vector().centerOf(leftPipePos), util.vector().centerOf(leftPipePos));

        Object inputObject = new Object();
        Object outputObject = new Object();

        ItemStack gasCanisterItem = new ItemStack(CCBItems.GAS_CANISTER.asItem());

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Airtight Tanks can be used to store large amounts of gas").pointAt(Vec3.atCenterOf(tankPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(pipeSelection, Direction.EAST);

        scene.idle(20);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST), false);
        scene.world().showSection(cogSelection, Direction.NORTH);
        scene.world().setKineticSpeed(cogSelection, mediumSpeed);
        scene.world().setKineticSpeed(pumpSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputObject, outputArea, 3);

        scene.idle(3);
        outputArea = outputArea.inflate(0.5, 0.375, 0.375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputObject, outputArea, 3);

        scene.idle(3);
        outputArea = outputArea.expandTowards(-2, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputObject, outputArea, 30);
        scene.overlay().showText(66).text("Pipe networks can push and pull gases from any side").colored(PonderPalette.WHITE).pointAt(Vec3.atCenterOf(rightPipePos)).placeNearTarget().attachKeyFrame();

        scene.idle(30);
        scene.world().setBlock(pumpPos, CCBBlocks.AIRTIGHT_PUMP_BLOCK.getDefaultState().setValue(AirtightPumpBlock.FACING, Direction.EAST), true);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputObject, inputArea, 3);

        scene.idle(3);
        inputArea = inputArea.inflate(0.5, 0.375, 0.375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputObject, inputArea, 3);

        scene.idle(3);
        inputArea = inputArea.expandTowards(2, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputObject, inputArea, 30);

        scene.idle(50);
        scene.overlay().showControls(util.vector().blockSurface(tankPos, Direction.UP), Pointing.DOWN, 60).showing(AllIcons.I_MTD_CLOSE).withItem(gasCanisterItem);
        scene.overlay().showText(60).text("However, gases cannot be added or taken manually").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(tankPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void size(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_tank_size", "Size of Airtight Tanks");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9f);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(3, 1, 3);
        BlockPos leftPos = tankPos.north().east();
        BlockPos rightPos = tankPos.south().west().above(4);

        Selection tankSelection = util.select().fromTo(leftPos, rightPos);

        AABB tankArea = new AABB(util.vector().centerOf(leftPos), util.vector().centerOf(leftPos));

        Object tankObject = new Object();

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 3);

        scene.idle(3);
        tankArea = tankArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 3);

        scene.idle(3);
        tankArea = tankArea.expandTowards(-2, 3, 2);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 60);
        scene.overlay().showText(60).text("Airtight Tanks can be combined up to 3x3x4 to increase total capacity").pointAt(Vec3.atCenterOf(tankPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
