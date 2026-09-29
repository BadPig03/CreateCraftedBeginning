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
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightFlowmeterBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightManometerBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightMeterScenes {
    public static void manometer(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_manometer", "Monitoring Gas Pressure with Airtight Manometers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos meterPos = util.grid().at(3, 1, 3);
        BlockPos westPipePos = meterPos.west();
        BlockPos eastPipePos = meterPos.east();
        BlockPos westTankBottomPos = meterPos.west(2);
        BlockPos eastTankBottomPos = meterPos.east(2);
        BlockPos invalidPipePos = meterPos.south();
        BlockPos comparatorPos = meterPos.north();
        BlockPos nearRedstonePos = comparatorPos.north();
        BlockPos displayLinkPos = meterPos.above();

        Selection westTankSelection = util.select().fromTo(westTankBottomPos, westTankBottomPos.above());
        Selection eastTankSelection = util.select().fromTo(eastTankBottomPos, eastTankBottomPos.above());
        Selection pipeSelection = util.select().fromTo(westPipePos, eastPipePos);
        Selection invalidPipeSelection = util.select().position(invalidPipePos);
        Selection comparatorSelection = util.select().position(comparatorPos);
        Selection redstoneSelection = util.select().position(nearRedstonePos);
        Selection displayLinkSelection = util.select().position(displayLinkPos);

        Vec3 meterVec = util.vector().centerOf(meterPos);
        Vec3 invalidPipeVec = util.vector().centerOf(invalidPipePos);
        Vec3 westFlowVec = util.vector().blockSurface(westTankBottomPos, Direction.EAST);
        Vec3 eastFlowVec = util.vector().blockSurface(eastTankBottomPos, Direction.WEST);

        AABB invalidPipeArea = new AABB(invalidPipeVec, invalidPipeVec);
        AABB westFlowArea = new AABB(westFlowVec, westFlowVec);
        AABB eastFlowArea = new AABB(eastFlowVec, eastFlowVec);

        Object invalidPipeObject = new Object();
        Object westFlowObject = new Object();
        Object eastFlowObject = new Object();

        ItemStack gogglesItem = new ItemStack(AllItems.GOGGLES.asItem());

        long minimumPressurePa = GasPressure.pascals(2);
        long initialMaximumPressurePa = GasPressure.pascals(6);
        long raisedMaximumPressurePa = GasPressure.pascals(12);
        int comparatorSignal = 8;

        scene.idle(20);
        scene.world().showSection(westTankSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(pipeSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(eastTankSelection, Direction.WEST);

        scene.idle(20);
        scene.world().showSection(invalidPipeSelection, Direction.NORTH);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, invalidPipeObject, invalidPipeArea, 3);

        scene.idle(3);
        invalidPipeArea = invalidPipeArea.inflate(0.3125, 0.3125, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, invalidPipeObject, invalidPipeArea, 60);
        scene.overlay().showText(60).text("Airtight Manometers can only connect to gas pipes along their axis").colored(PonderPalette.RED).pointAt(invalidPipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().hideSection(invalidPipeSelection, Direction.SOUTH);

        scene.idle(20);
        scene.world().modifyBlockEntity(meterPos, AirtightManometerBlockEntity.class, manometer -> manometer.acceptPressureTelemetry(minimumPressurePa, initialMaximumPressurePa));
        scene.overlay().showText(60).text("The Manometer measures pressure without driving the gas flow").pointAt(meterVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, westFlowObject, westFlowArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, eastFlowObject, eastFlowArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, westFlowObject, westFlowArea.inflate(0, 0.3125, 0.3125).expandTowards(1.5, 0, 0), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, eastFlowObject, eastFlowArea.inflate(0, 0.3125, 0.3125).expandTowards(1.5, 0, 0), 60);

        scene.idle(80);
        scene.overlay().showText(60).text("Its needle follows the highest pressure at the meter").pointAt(meterVec).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.world().modifyBlockEntity(meterPos, AirtightManometerBlockEntity.class, manometer -> manometer.acceptPressureTelemetry(minimumPressurePa, raisedMaximumPressurePa));

        scene.idle(60);
        scene.overlay().showControls(util.vector().topOf(meterPos), Pointing.DOWN, 60).withItem(gogglesItem.copy());
        scene.overlay().showText(60).text("Engineer's Goggles show the exact Maximum Pressure and Pressure Difference").colored(PonderPalette.BLUE).pointAt(meterVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(comparatorSelection, Direction.SOUTH);
        scene.world().showSection(redstoneSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().showText(60).text("Comparators convert Maximum Pressure into a Redstone Signal from 0 to 15").colored(PonderPalette.BLUE).pointAt(util.vector().centerOf(comparatorPos)).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.world().toggleRedstonePower(comparatorSelection);
        scene.world().modifyBlock(nearRedstonePos, state -> state.setValue(RedStoneWireBlock.POWER, comparatorSignal), false);
        scene.effects().indicateRedstone(comparatorPos);

        scene.idle(60);
        scene.world().showSection(displayLinkSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Display Links can report either Maximum Pressure or Pressure Difference").colored(PonderPalette.BLUE).pointAt(util.vector().centerOf(displayLinkPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void flowmeter(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_flowmeter", "Monitoring Gas Flow with Airtight Flowmeters");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos meterPos = util.grid().at(3, 1, 3);
        BlockPos westPipePos = meterPos.west();
        BlockPos eastPipePos = meterPos.east();
        BlockPos westTankBottomPos = meterPos.west(2);
        BlockPos eastTankBottomPos = meterPos.east(2);
        BlockPos invalidPipePos = meterPos.south();
        BlockPos comparatorPos = meterPos.north();
        BlockPos nearRedstonePos = comparatorPos.north();
        BlockPos displayLinkPos = meterPos.above();

        Selection westTankSelection = util.select().fromTo(westTankBottomPos, westTankBottomPos.above());
        Selection eastTankSelection = util.select().fromTo(eastTankBottomPos, eastTankBottomPos.above());
        Selection pipeSelection = util.select().fromTo(westPipePos, eastPipePos);
        Selection invalidPipeSelection = util.select().position(invalidPipePos);
        Selection comparatorSelection = util.select().position(comparatorPos);
        Selection redstoneSelection = util.select().position(nearRedstonePos);
        Selection displayLinkSelection = util.select().position(displayLinkPos);

        Vec3 meterVec = util.vector().centerOf(meterPos);
        Vec3 invalidPipeVec = util.vector().centerOf(invalidPipePos);
        Vec3 westFlowVec = util.vector().blockSurface(westTankBottomPos, Direction.EAST);
        Vec3 eastFlowVec = util.vector().blockSurface(eastTankBottomPos, Direction.WEST);

        AABB invalidPipeArea = new AABB(invalidPipeVec, invalidPipeVec);
        AABB westFlowArea = new AABB(westFlowVec, westFlowVec);
        AABB eastFlowArea = new AABB(eastFlowVec, eastFlowVec);

        Object invalidPipeObject = new Object();
        Object westFlowObject = new Object();
        Object eastFlowObject = new Object();

        ItemStack gogglesItem = new ItemStack(AllItems.GOGGLES.asItem());

        long demonstrationFlowRate = 8000;
        int comparatorSignal = 8;

        scene.idle(20);
        scene.world().showSection(westTankSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(pipeSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(eastTankSelection, Direction.WEST);

        scene.idle(20);
        scene.world().showSection(invalidPipeSelection, Direction.NORTH);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, invalidPipeObject, invalidPipeArea, 3);

        scene.idle(3);
        invalidPipeArea = invalidPipeArea.inflate(0.3125, 0.3125, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, invalidPipeObject, invalidPipeArea, 60);
        scene.overlay().showText(60).text("Airtight Flowmeters can only connect to gas pipes along their axis").colored(PonderPalette.RED).pointAt(invalidPipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().hideSection(invalidPipeSelection, Direction.SOUTH);

        scene.idle(20);
        scene.world().modifyBlockEntity(meterPos, AirtightFlowmeterBlockEntity.class, flowmeter -> flowmeter.acceptFlowTelemetry(demonstrationFlowRate));
        scene.overlay().showText(60).text("The Flowmeter measures gas passing through it without driving the flow").pointAt(meterVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, westFlowObject, westFlowArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, eastFlowObject, eastFlowArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, westFlowObject, westFlowArea.inflate(0, 0.3125, 0.3125).expandTowards(1.5, 0, 0), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, eastFlowObject, eastFlowArea.inflate(0, 0.3125, 0.3125).expandTowards(1.5, 0, 0), 60);

        scene.idle(80);
        scene.overlay().showText(60).text("Flow Rate is measured by magnitude, regardless of direction").pointAt(meterVec).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.world().modifyBlockEntity(meterPos, AirtightFlowmeterBlockEntity.class, flowmeter -> flowmeter.acceptFlowTelemetry(demonstrationFlowRate));

        scene.idle(60);
        scene.overlay().showControls(util.vector().topOf(meterPos), Pointing.DOWN, 60).withItem(gogglesItem.copy());
        scene.overlay().showText(60).text("Engineer's Goggles show the exact Flow Rate in GU/t").colored(PonderPalette.BLUE).pointAt(meterVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.world().showSection(comparatorSelection, Direction.SOUTH);
        scene.world().showSection(redstoneSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().showText(60).text("Comparators convert Flow Rate into a Redstone Signal from 0 to 15").colored(PonderPalette.BLUE).pointAt(util.vector().centerOf(comparatorPos)).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.world().toggleRedstonePower(comparatorSelection);
        scene.world().modifyBlock(nearRedstonePos, state -> state.setValue(RedStoneWireBlock.POWER, comparatorSignal), false);
        scene.effects().indicateRedstone(comparatorPos);

        scene.idle(60);
        scene.world().showSection(displayLinkSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Display Links can report the exact Flow Rate numerically").colored(PonderPalette.BLUE).pointAt(util.vector().centerOf(displayLinkPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
