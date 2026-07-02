package net.ty.createcraftedbeginning.ponder.scenes.gascontainers;

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
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeAirtightTankScenes {
    public static void storage(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("creative_airtight_tank_storage", "Creative Airtight Tanks");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos pumpPos = util.grid().at(3, 1, 3);
        BlockPos cogPos = pumpPos.south();
        BlockPos motorPos = cogPos.east();
        BlockPos rightPipePos = pumpPos.west();
        BlockPos tankPos = rightPipePos.west();
        BlockPos leftPipePos = pumpPos.east();
        BlockPos creativePos = leftPipePos.east();
        BlockPos creativeTopPos = creativePos.above();

        Selection tankSelection = util.select().fromTo(tankPos, tankPos.above());
        Selection creativeSelection = util.select().fromTo(creativePos, creativeTopPos);
        Selection pipeSelection = util.select().fromTo(leftPipePos, rightPipePos);
        Selection cogSelection = util.select().fromTo(cogPos, motorPos);

        Vec3 creativeVec = util.vector().centerOf(creativePos);
        Vec3 pumpVec = util.vector().centerOf(pumpPos);
        Vec3 rightPipeVec = util.vector().centerOf(rightPipePos);
        Vec3 leftPipeVec = util.vector().centerOf(leftPipePos);

        AABB creativeArea = new AABB(creativeVec, creativeVec);
        AABB pipeArea = new AABB(leftPipeVec, leftPipeVec);
        AABB pipeReverseArea = new AABB(rightPipeVec, rightPipeVec);

        Object creativeObject = new Object();
        Object pipeObject = new Object();
        Object pipeReverseObject = new Object();

        ItemStack gasCanisterItem = new ItemStack(CCBItems.GAS_CANISTER.asItem());

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(pipeSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(creativeSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, creativeObject, creativeArea, 3);

        scene.idle(3);
        creativeArea = creativeArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, creativeObject, creativeArea, 3);

        scene.idle(3);
        creativeArea = creativeArea.expandTowards(0, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, creativeObject, creativeArea, 60);
        scene.overlay().showText(60).text("Creative Airtight Tanks provide an infinite supply of gas").colored(PonderPalette.GREEN).pointAt(creativeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(creativeTopPos, Direction.UP), Pointing.DOWN, 67).rightClick().withItem(gasCanisterItem.copy());
        
		scene.idle(7);
		scene.overlay().showText(60).text("Right-click the tank with a Gas Canister to designate its gas type").colored(PonderPalette.BLUE).pointAt(creativeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), false);
        scene.world().showSection(cogSelection, Direction.NORTH);

        scene.idle(15);
        scene.world().setKineticSpeed(cogSelection, mediumSpeed);
        scene.world().setKineticSpeed(pipeSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, pipeObject, pipeArea, 3);

        scene.idle(3);
        pipeArea = pipeArea.inflate(0.5, 0.375, 0.375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, pipeObject, pipeArea, 3);

        scene.idle(3);
        pipeArea = pipeArea.expandTowards(-2, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, pipeObject, pipeArea, 60);
        scene.overlay().showText(60).text("Gas pipes can indefinitely drain the designated gas from it").colored(PonderPalette.OUTPUT).pointAt(pumpVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(pumpPos, CCBBlocks.AIRTIGHT_PUMP_BLOCK.getDefaultState().setValue(AirtightPumpBlock.FACING, Direction.EAST), true);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pipeReverseObject, pipeReverseArea, 3);

        scene.idle(3);
        pipeReverseArea = pipeReverseArea.inflate(0.5, 0.375, 0.375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pipeReverseObject, pipeReverseArea, 3);

        scene.idle(3);
        pipeReverseArea = pipeReverseArea.expandTowards(2, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pipeReverseObject, pipeReverseArea, 60);
        scene.overlay().showText(60).text("Any gas pumped into a Creative Gas Tank will be disposed").colored(PonderPalette.RED).pointAt(pumpVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void size(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("creative_airtight_tank_size", "Size of Creative Airtight Tanks");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9f);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(3, 1, 3);
        BlockPos leftPos = tankPos.north().east();
        BlockPos rightPos = tankPos.south().west().above(4);

        Selection tankSelection = util.select().fromTo(leftPos, rightPos);

        Vec3 leftVec = util.vector().centerOf(leftPos);
        Vec3 tankVec = util.vector().centerOf(tankPos);

        AABB tankArea = new AABB(leftVec, leftVec);

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
        scene.overlay().showText(60).text("Creative Airtight Tanks can be combined up to 3x3x4").colored(PonderPalette.GREEN).pointAt(tankVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
