package net.ty.createcraftedbeginning.ponder.scenes.gascontainers;

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
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeAirtightTankScenes {
    public static void storage(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("creative_airtight_tank_storage", "Creative Airtight Tanks");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos pipeMiddlePos = util.grid().at(3, 1, 3);
        BlockPos tankPipePos = pipeMiddlePos.west();
        BlockPos tankPos = tankPipePos.west();
        BlockPos creativePipePos = pipeMiddlePos.east();
        BlockPos creativePos = creativePipePos.east();
        BlockPos creativeTopPos = creativePos.above();

        Selection tankSelection = util.select().fromTo(tankPos, tankPos.above());
        Selection pipeSelection = util.select().fromTo(tankPipePos, creativePipePos);
        Selection creativeSelection = util.select().fromTo(creativePos, creativeTopPos);

        Vec3 creativeVec = util.vector().centerOf(creativePos);
        Vec3 networkVec = util.vector().centerOf(pipeMiddlePos);
        Vec3 pressurePanelVec = util.vector().blockSurface(creativeTopPos, Direction.NORTH);
        Vec3 sourceStartVec = util.vector().blockSurface(creativePos, Direction.WEST);
        Vec3 sinkStartVec = util.vector().blockSurface(tankPos, Direction.EAST);

        AABB sourceFlowArea = new AABB(sourceStartVec, sourceStartVec);
        AABB sinkFlowArea = new AABB(sinkStartVec, sinkStartVec);
        AABB pressureSettingArea = new AABB(creativeVec, creativeVec);

        Object sourceFlowObject = new Object();
        Object sinkFlowObject = new Object();
        Object pressureSettingObject = new Object();

        ItemStack naturalAirCanister = new ItemStack(CCBItems.GAS_CANISTER.asItem());
        naturalAirCanister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        ItemStack ultrawarmAirCanister = new ItemStack(CCBItems.GAS_CANISTER.asItem());
        ultrawarmAirCanister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.ULTRAWARM_AIR.get(), 1));
        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        long selectedPressurePa = GasPressure.pascals(4);
        long lowNetworkPressurePa = GasPressure.pascals(2);
        long highNetworkPressurePa = GasPressure.pascals(8);
        long adjustedPressurePa = GasPressure.pascals(6);

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(pipeSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(creativeSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(creativeTopPos), Pointing.DOWN, 60).rightClick().withItem(naturalAirCanister.copy());
        scene.overlay().showText(60).text("Right-click with a filled Gas Canister to select the gas supplied").colored(PonderPalette.BLUE).pointAt(creativeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().modifyBlockEntity(creativePos, CreativeAirtightTankBlockEntity.class, tank -> tank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1)));

        scene.idle(73);
        scene.overlay().showCenteredScrollInput(creativeTopPos, Direction.NORTH, 60);
        scene.overlay().showControls(pressurePanelVec, Pointing.RIGHT, 60).withItem(wrenchItem.copy()).scroll();
        scene.overlay().showText(60).text("Hold a Wrench and scroll the value panel to set the pressure").colored(PonderPalette.BLUE).pointAt(pressurePanelVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().modifyBlockEntity(creativePos, CreativeAirtightTankBlockEntity.class, tank -> tank.setExtraData(selectedPressurePa));

        scene.idle(67);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, sourceFlowObject, sourceFlowArea, 3);

        scene.idle(3);
        sourceFlowArea = sourceFlowArea.inflate(0, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, sourceFlowObject, sourceFlowArea, 3);

        scene.idle(3);
        sourceFlowArea = sourceFlowArea.expandTowards(-3, 0, 0);
        scene.world().modifyBlockEntity(tankPos, AirtightTankBlockEntity.class, tank -> setTankPressure(tank, CCBGases.NATURAL_AIR.get(), lowNetworkPressurePa));
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, sourceFlowObject, sourceFlowArea, 60);
        scene.overlay().showText(60).text("Below the set pressure, the tank supplies the selected gas indefinitely").colored(PonderPalette.OUTPUT).pointAt(networkVec).placeNearTarget().attachKeyFrame();

        scene.idle(74);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, sinkFlowObject, sinkFlowArea, 3);

        scene.idle(3);
        sinkFlowArea = sinkFlowArea.inflate(0, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, sinkFlowObject, sinkFlowArea, 3);

        scene.idle(3);
        sinkFlowArea = sinkFlowArea.expandTowards(3, 0, 0);
        scene.world().modifyBlockEntity(tankPos, AirtightTankBlockEntity.class, tank -> setTankPressure(tank, CCBGases.NATURAL_AIR.get(), highNetworkPressurePa));
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, sinkFlowObject, sinkFlowArea, 60);
        scene.overlay().showText(60).text("Above the set pressure, the tank absorbs and destroys incoming gas").colored(PonderPalette.INPUT).pointAt(networkVec).placeNearTarget().attachKeyFrame();

        scene.idle(77);
        scene.overlay().showCenteredScrollInput(creativeTopPos, Direction.SOUTH, 27);
        scene.overlay().showControls(pressurePanelVec, Pointing.RIGHT, 27).withItem(wrenchItem.copy()).scroll();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, pressureSettingObject, pressureSettingArea, 3);

        scene.idle(3);
        pressureSettingArea = pressureSettingArea.inflate(0.5).expandTowards(0, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, pressureSettingObject, pressureSettingArea, 60);
        scene.overlay().showText(60).text("Pressure and selected gas can be adjusted independently").colored(PonderPalette.BLUE).pointAt(creativeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().modifyBlockEntity(creativePos, CreativeAirtightTankBlockEntity.class, tank -> tank.setExtraData(adjustedPressurePa));

        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(creativeTopPos), Pointing.DOWN, 33).rightClick().withItem(ultrawarmAirCanister.copy());

        scene.idle(7);
        scene.world().modifyBlockEntity(creativePos, CreativeAirtightTankBlockEntity.class, tank -> tank.getTankInventory().setContainedGas(new GasStack(CCBGases.ULTRAWARM_AIR.get(), 1)));

        scene.idle(26);
        scene.markAsFinished();
    }

    public static void size(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("creative_airtight_tank_size", "Building Larger Creative Airtight Tanks");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9F);
        scene.showBasePlate();

        BlockPos startPos = util.grid().at(2, 1, 2);
        BlockPos structurePos = startPos.east().above().south();
        BlockPos firstSliceEndPos = startPos.east(2).south(2);
        BlockPos remainderStartPos = startPos.above();
        BlockPos fullEndPos = startPos.east(2).above(3).south(2);

        Selection firstBlockSelection = util.select().position(startPos);
        Selection firstSliceSelection = util.select().fromTo(startPos, firstSliceEndPos).substract(firstBlockSelection);
        Selection remainderSelection = util.select().fromTo(remainderStartPos, fullEndPos);

        Vec3 startVec = util.vector().centerOf(startPos);
        Vec3 structureVec = util.vector().centerOf(structurePos);

        AABB tankArea = new AABB(startVec, startVec);
        Object tankObject = new Object();

        scene.idle(20);
        scene.world().showSection(firstBlockSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().showSection(firstSliceSelection, Direction.DOWN);

        scene.idle(15);
        for (BlockPos pos : firstSliceSelection) {
            scene.effects().indicateSuccess(pos);
        }
        scene.overlay().showText(60).text("Creative Airtight Tanks can combine into larger tanks within the configured size limit").colored(PonderPalette.GREEN).pointAt(structureVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().showSection(remainderSelection, Direction.DOWN);

        scene.idle(8);
        for (BlockPos pos : remainderSelection) {
            scene.effects().indicateSuccess(pos);
        }
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 3);

        scene.idle(3);
        tankArea = tankArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 3);

        scene.idle(3);
        tankArea = tankArea.expandTowards(2, 3, 2);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 60);

        scene.idle(59);
        scene.overlay().showText(60).text("The whole tank shares one selected gas and pressure setting").colored(PonderPalette.BLUE).pointAt(structureVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    private static void setTankPressure(AirtightTankBlockEntity tank, Gas gas, long pressurePa) {
        long amount = Math.min(tank.getTankInventory().getMaxAmount(), GasPressure.amount(tank.getTankInventory().getVolume(), pressurePa));
        tank.getTankInventory().tryReplaceContents(new GasStack(gas, amount)).requireAccepted();
    }
}
