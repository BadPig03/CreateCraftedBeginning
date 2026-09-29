package net.ty.createcraftedbeginning.ponder.scenes.gascontainers;

import com.simibubi.create.foundation.gui.AllIcons;
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
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightTankScenes {
    public static void storage(SceneBuilder builder, SceneBuildingUtil util) {
        storageScene(builder, util);
    }

    public static void horizontalStorage(SceneBuilder builder, SceneBuildingUtil util) {
        storageScene(builder, util);
    }

    public static void size(SceneBuilder builder, SceneBuildingUtil util) {
        sizeScene(builder, util, false);
    }

    public static void horizontalSize(SceneBuilder builder, SceneBuildingUtil util) {
        sizeScene(builder, util, true);
    }

    private static void storageScene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_tank_storage", "Storing Pressurized Gas in Airtight Tanks");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(3, 1, 3);
        BlockPos tankPipePos = tankPos.west();
        BlockPos pipeStartPos = tankPipePos.west();
        BlockPos pipeEndPos = pipeStartPos.west();

        Selection tankSelection = util.select().position(tankPos);
        Selection pipeSelection = util.select().fromTo(tankPipePos, pipeEndPos);

        Vec3 tankVec = util.vector().centerOf(tankPos);
        Vec3 pipeStartVec = util.vector().centerOf(pipeEndPos);
        Vec3 tankWestFaceVec = util.vector().blockSurface(tankPos, Direction.WEST);

        AABB flowArea = new AABB(pipeStartVec, pipeStartVec);
        AABB lowPressureArea = new AABB(tankVec, tankVec);
        AABB pressureLimitArea = new AABB(pipeStartVec, pipeStartVec);
        AABB gasTypeArea = new AABB(tankVec, tankVec);
        AABB releaseArea = new AABB(tankVec, tankVec);

        Object flowObject = new Object();
        Object lowPressureObject = new Object();
        Object pressureLimitObject = new Object();
        Object gasTypeObject = new Object();
        Object releaseObject = new Object();

        ItemStack gasCanisterItem = new ItemStack(CCBItems.GAS_CANISTER.asItem());

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, lowPressureObject, lowPressureArea, 3);

        scene.idle(3);
        lowPressureArea = lowPressureArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, lowPressureObject, lowPressureArea, 60);
        scene.overlay().showText(60).text("Airtight Tanks can store a finite amount of pressurized gas").pointAt(tankVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.world().showSection(pipeSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea, 3);

        scene.idle(3);
        flowArea = flowArea.inflate(0.5, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea, 3);

        scene.idle(3);
        flowArea = flowArea.expandTowards(3, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea, 60);
        scene.overlay().showText(60).text("Gas can enter or leave through connected faces according to the pressure difference").colored(PonderPalette.GREEN).pointAt(tankWestFaceVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Adding more gas to the same tank volume raises its pressure").pointAt(tankVec).placeNearTarget().attachKeyFrame();

        scene.idle(74);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pressureLimitObject, pressureLimitArea, 3);

        scene.idle(3);
        pressureLimitArea = pressureLimitArea.inflate(0.5, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pressureLimitObject, pressureLimitArea, 3);

        scene.idle(3);
        pressureLimitArea = pressureLimitArea.expandTowards(3, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pressureLimitObject, pressureLimitArea, 60);
        scene.overlay().showText(60).text("Once the tank reaches its maximum pressure, it stops accepting more gas").colored(PonderPalette.RED).pointAt(tankWestFaceVec).placeNearTarget().attachKeyFrame();

        scene.idle(77);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, gasTypeObject, gasTypeArea, 3);

        scene.idle(3);
        gasTypeArea = gasTypeArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, gasTypeObject, gasTypeArea, 60);
        scene.overlay().showText(60).text("A tank that already contains gas only accepts more of the same type").colored(PonderPalette.RED).pointAt(tankVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(tankPos, Direction.UP), Pointing.DOWN, 60).showing(AllIcons.I_MTD_CLOSE).withItem(gasCanisterItem.copy());
        scene.overlay().showText(60).text("Gas Canisters cannot fill or drain an Airtight Tank directly").colored(PonderPalette.RED).pointAt(tankVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea, 60);
        scene.overlay().showText(60).text("Use a gas network to transfer gas to or from the tank instead").colored(PonderPalette.GREEN).pointAt(tankWestFaceVec).placeNearTarget().attachKeyFrame();

        scene.idle(74);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, releaseObject, releaseArea, 3);

        scene.idle(3);
        releaseArea = releaseArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, releaseObject, releaseArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, releaseObject, releaseArea.inflate(1.25), 60);
        scene.overlay().showText(60).text("Breaking a filled Airtight Tank releases its stored gas").colored(PonderPalette.RED).pointAt(tankVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().destroyBlock(tankPos);

        scene.idle(53);
        scene.markAsFinished();
    }

    private static void sizeScene(SceneBuilder builder, SceneBuildingUtil util, boolean horizontal) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_tank_size", "Building Larger Airtight Tanks");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9F);
        scene.showBasePlate();

        BlockPos startPos = util.grid().at(2, 1, 2);
        BlockPos firstSliceEndPos = horizontal ? startPos.above(2).south(2) : startPos.east(2).south(2);
        BlockPos remainderStartPos = horizontal ? startPos.east() : startPos.above();
        BlockPos fullEndPos = horizontal ? startPos.east(3).above(2).south(2) : startPos.above(3).east(2).south(2);

        Selection firstBlockSelection = util.select().position(startPos);
        Selection firstSliceSelection = util.select().fromTo(startPos, firstSliceEndPos).substract(firstBlockSelection);
        Selection remainderSelection = util.select().fromTo(remainderStartPos, fullEndPos);

        Vec3 startVec = util.vector().centerOf(startPos);
        Vec3 structureVec = util.vector().centerOf(startPos.east().above().south());

        AABB tankArea = new AABB(startVec, startVec);
        AABB gasAmountArea = new AABB(startVec, startVec).inflate(0.1875);

        Object tankObject = new Object();
        Object gasAmountObject = new Object();

        scene.idle(20);
        scene.world().showSection(firstBlockSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().showSection(firstSliceSelection, Direction.DOWN);

        scene.idle(15);
        for (BlockPos pos : firstSliceSelection) {
            scene.effects().indicateSuccess(pos);
        }
        scene.overlay().showText(60).text("Airtight Tank blocks can combine into larger tanks within the configured size limit").colored(PonderPalette.GREEN).pointAt(structureVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().showSection(remainderSelection, horizontal ? Direction.WEST : Direction.DOWN);

        scene.idle(8);
        for (BlockPos pos : remainderSelection) {
            scene.effects().indicateSuccess(pos);
        }
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 3);

        scene.idle(3);
        tankArea = tankArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 3);

        scene.idle(3);
        tankArea = tankArea.expandTowards(horizontal ? 3 : 2, horizontal ? 2 : 3, 2);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 60);

        scene.idle(59);
        scene.overlay().showText(60).text("Each added block increases the tank's total volume").pointAt(structureVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, gasAmountObject, gasAmountArea, 60);
        scene.overlay().showText(60).text("For the same amount of gas, a larger tank has lower pressure").pointAt(structureVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Larger tanks extend along the tank's main axis").pointAt(structureVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
