package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceBlockEntity;
import org.jetbrains.annotations.NotNull;

public class PortableGasInterfaceScenes {
    public static void transfer(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("portable_gas_interface_transfer", "Gas Exchange on Contraptions");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos bearingPos = util.grid().at(4, 1, 4);
        BlockPos contraptionIronPos = bearingPos.above();
        BlockPos contraptionTopPos = contraptionIronPos.above();
        BlockPos contraptionTankPos = contraptionIronPos.south();
        BlockPos contraptionInterfacePos = contraptionIronPos.north();
        BlockPos interfacePos = contraptionInterfacePos.north(2);
        BlockPos encasedPos = interfacePos.below();
        BlockPos firstPipePos = encasedPos.west();
        BlockPos pumpPos = firstPipePos.west();
        BlockPos secondPipePos = pumpPos.west();
        BlockPos tankPos = secondPipePos.west();
        BlockPos cogPos = pumpPos.north();
        BlockPos motorPos = cogPos.west();
        BlockPos redstoneDustPos = interfacePos.east();
        BlockPos redstoneBasePos = redstoneDustPos.below();
        BlockPos leverPos = redstoneDustPos.east();

        Selection bearingSelection = util.select().position(bearingPos);
        Selection contraptionInterfaceSelection = util.select().position(contraptionInterfacePos);
        Selection contraptionSelection = util.select().fromTo(contraptionIronPos, contraptionTankPos.above());
        Selection bothInterfaceSelection = util.select().fromTo(contraptionInterfacePos, interfacePos);
        Selection pipeSelection = util.select().fromTo(interfacePos, tankPos);
        Selection sourceSelection = util.select().fromTo(cogPos, motorPos);
        Selection redstoneSelection = util.select().fromTo(leverPos, redstoneBasePos);

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(bearingSelection, Direction.DOWN);
        ElementLink<WorldSectionElement> contraption = scene.world().showIndependentSection(contraptionSelection, Direction.DOWN);
        scene.world().configureCenterOfRotation(contraption, util.vector().centerOf(bearingPos));

        scene.idle(20);
        scene.world().rotateBearing(bearingPos, 360, 60);
		scene.world().rotateSection(contraption, 0, 360, 0, 60);
        scene.overlay().showText(60).text("Airtight Pipes cannot directly interact with Airtight Tanks on contraptions").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(contraptionTopPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSectionAndMerge(contraptionInterfaceSelection, Direction.SOUTH, contraption);

        scene.idle(13);
		scene.effects().superGlue(contraptionInterfacePos, Direction.SOUTH, true);
        scene.overlay().showText(60).text("Similarly to Portable Fluid Interfaces, Portable Gas Interfaces enable direct gas exchange with contraptions").pointAt(util.vector().topOf(contraptionInterfacePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().rotateBearing(bearingPos, 360, 60);
		scene.world().rotateSection(contraption, 0, 360, 0, 60);

        scene.idle(10);
        scene.world().showSection(pipeSelection, Direction.DOWN);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST), false);
        scene.world().showSection(sourceSelection, Direction.SOUTH);

        scene.idle(10);
        scene.world().setKineticSpeed(sourceSelection, mediumSpeed);
        scene.world().setKineticSpeed(pipeSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);

        scene.idle(20);
        scene.overlay().showText(60).text("Their behavior mirrors Portable Fluid Interfaces exactly").pointAt(util.vector().topOf(interfacePos)).placeNearTarget().attachKeyFrame();

        scene.idle(30);
        scene.world().modifyBlockEntityNBT(bothInterfaceSelection, PortableGasInterfaceBlockEntity.class, compoundTag -> {
			compoundTag.putFloat("Distance", 1);
			compoundTag.putFloat("Timer", 14);
		});

        scene.idle(80);
        scene.world().modifyBlockEntityNBT(bothInterfaceSelection, PortableGasInterfaceBlockEntity.class, compoundTag -> compoundTag.putFloat("Timer", 2));
        scene.world().rotateBearing(bearingPos, 360, 60);
		scene.world().rotateSection(contraption, 0, 360, 0, 60);

        scene.idle(20);
        scene.world().showSection(redstoneSelection, Direction.WEST);

        scene.idle(20);
        scene.world().toggleRedstonePower(redstoneSelection);
        scene.effects().indicateRedstone(leverPos);
        scene.overlay().showText(60).text("Redstone power will prevent the interface from engaging").colored(PonderPalette.RED).pointAt(util.vector().topOf(interfacePos)).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.world().rotateBearing(bearingPos, 360, 60);
		scene.world().rotateSection(contraption, 0, 360, 0, 60);

        scene.idle(60);
        scene.markAsFinished();
    }
}
