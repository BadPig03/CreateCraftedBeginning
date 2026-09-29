package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PortableGasInterfaceScenes {
    private static final String COMPOUND_KEY_DISTANCE = "Distance";
    private static final String COMPOUND_KEY_TIMER = "Timer";

    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("portable_gas_interface", "Gas Exchange on Contraptions");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos bearingPos = util.grid().at(4, 1, 4);
        BlockPos contraptionIronPos = bearingPos.above();
        BlockPos contraptionTankPos = contraptionIronPos.south();
        BlockPos contraptionInterfacePos = contraptionIronPos.north();
        BlockPos interfacePos = contraptionInterfacePos.north(2);
        BlockPos encasedPos = interfacePos.below();
        BlockPos tankPos = encasedPos.west(4);
        BlockPos redstoneDustPos = interfacePos.east();
        BlockPos redstoneBasePos = redstoneDustPos.below();
        BlockPos leverPos = redstoneDustPos.east();

        Selection bearingSelection = util.select().position(bearingPos);
        Selection contraptionInterfaceSelection = util.select().position(contraptionInterfacePos);
        Selection contraptionTankSelection = util.select().fromTo(contraptionTankPos, contraptionTankPos.above());
        Selection contraptionSelection = util.select().fromTo(contraptionIronPos, contraptionTankPos.above());
        Selection stationaryInterfaceSelection = util.select().position(interfacePos);
        Selection bothInterfaceSelection = util.select().fromTo(contraptionInterfacePos, interfacePos);
        Selection stationaryPipeSelection = util.select().fromTo(encasedPos, tankPos.east());
        Selection stationaryTankSelection = util.select().fromTo(tankPos, tankPos.above());
        Selection redstoneSelection = util.select().fromTo(leverPos, redstoneBasePos);

        Vec3 contraptionTankVec = util.vector().centerOf(contraptionTankPos.above());
        Vec3 contraptionInterfaceVec = util.vector().centerOf(contraptionInterfacePos);
        Vec3 connectionVec = util.vector().centerOf(interfacePos).add(0, 0, 1);
        Vec3 stationaryPipeVec = util.vector().centerOf(encasedPos.west());
        Vec3 interfaceVec = util.vector().topOf(interfacePos);

        Object stationaryTankObject = new Object();
        Object stationaryPipeObject = new Object();
        Object stationaryInterfaceObject = new Object();
        Object contraptionInterfaceObject = new Object();
        Object contraptionTankObject = new Object();

        scene.idle(20);
        scene.world().showSection(bearingSelection, Direction.DOWN);
        ElementLink<WorldSectionElement> contraption = scene.world().showIndependentSection(contraptionSelection, Direction.DOWN);
        scene.world().configureCenterOfRotation(contraption, util.vector().centerOf(bearingPos));

        scene.idle(20);
        scene.world().rotateBearing(bearingPos, 360, 60);
        scene.world().rotateSection(contraption, 0, 360, 0, 60);
        scene.overlay().showText(60).text("Airtight Tanks on a moving contraption cannot be accessed directly by stationary gas networks").colored(PonderPalette.RED).pointAt(contraptionTankVec).placeNearTarget().attachKeyFrame();

        scene.idle(65);
        scene.world().showSectionAndMerge(contraptionInterfaceSelection, Direction.SOUTH, contraption);

        scene.idle(15);
        scene.effects().superGlue(contraptionInterfacePos, Direction.SOUTH, true);

        scene.idle(20);
        scene.world().rotateBearing(bearingPos, 360, 60);
        scene.world().rotateSection(contraption, 0, 360, 0, 60);
        scene.overlay().showText(60).text("Place one Portable Gas Interface on the contraption and one on the stationary network").colored(PonderPalette.GREEN).pointAt(contraptionInterfaceVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("The interfaces must face each other with one or two blocks of space between them").colored(PonderPalette.RED).pointAt(connectionVec).placeNearTarget().attachKeyFrame();
        scene.world().showSection(stationaryTankSelection, Direction.DOWN);
        scene.world().showSection(stationaryPipeSelection, Direction.DOWN);
        scene.world().showSection(stationaryInterfaceSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().modifyBlockEntityNBT(bothInterfaceSelection, PortableGasInterfaceBlockEntity.class, compoundTag -> {
            compoundTag.putFloat(COMPOUND_KEY_DISTANCE, 1);
            compoundTag.putFloat(COMPOUND_KEY_TIMER, 14);
        });

        scene.idle(60);
        scene.overlay().showOutline(PonderPalette.GREEN, stationaryTankObject, stationaryTankSelection, 60);
        scene.overlay().showOutline(PonderPalette.GREEN, stationaryPipeObject, stationaryPipeSelection, 60);
        scene.overlay().showOutline(PonderPalette.GREEN, stationaryInterfaceObject, stationaryInterfaceSelection, 60);
        scene.overlay().showOutline(PonderPalette.GREEN, contraptionInterfaceObject, contraptionInterfaceSelection, 60);
        scene.overlay().showOutline(PonderPalette.GREEN, contraptionTankObject, contraptionTankSelection, 60);
        scene.overlay().showText(60).text("When the interfaces pass each other while aligned, they engage automatically").pointAt(connectionVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("While engaged, the stationary interface provides access to all Airtight Tanks on the contraption").colored(PonderPalette.GREEN).pointAt(connectionVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Portable Gas Interfaces do not create pressure or impose a flow direction").colored(PonderPalette.RED).pointAt(stationaryPipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Gas still flows according to pressure in the connected network").pointAt(stationaryPipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("After no gas is exchanged for a while, the interfaces disengage and the contraption can move again").pointAt(connectionVec).placeNearTarget().attachKeyFrame();

        scene.idle(68);
        scene.world().showSection(redstoneSelection, Direction.WEST);

        scene.idle(12);
        scene.overlay().showText(60).text("Redstone power on the stationary interface disengages it and prevents new connections").colored(PonderPalette.RED).pointAt(interfaceVec).placeNearTarget().attachKeyFrame();
        scene.world().toggleRedstonePower(redstoneSelection);
        scene.effects().indicateRedstone(leverPos);
        scene.world().modifyBlockEntityNBT(bothInterfaceSelection, PortableGasInterfaceBlockEntity.class, compoundTag -> compoundTag.putFloat(COMPOUND_KEY_TIMER, 2));
        scene.world().rotateBearing(bearingPos, 360, 60);
        scene.world().rotateSection(contraption, 0, 360, 0, 60);

        scene.idle(60);
        scene.markAsFinished();
    }
}
