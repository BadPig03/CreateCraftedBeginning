package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

public class PneumaticEngineScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("pneumatic_engine", "Generates Rotational Force using Pneumatic Engines");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(2, 1, 2);
        BlockPos enginePos = util.grid().at(2, 2, 2);

        ItemStack wrench = AllItems.WRENCH.asStack();

        scene.world().setBlock(tankPos, AllBlocks.COPPER_BACKTANK.getDefaultState(), false);
        scene.world().setBlock(enginePos, CCBBlocks.PNEUMATIC_ENGINE_BLOCK.getDefaultState(), false);
        scene.world().showIndependentSection(util.select().fromTo(tankPos, enginePos), Direction.DOWN);

        scene.idle(5);
        scene.world().setKineticSpeed(util.select().fromTo(enginePos, enginePos), 64);
        scene.effects().rotationSpeedIndicator(enginePos);

        scene.idle(5);
        scene.overlay().showText(60).text("Pneumatic Engine can absorb compressed air from Backtanks to generate kinetic stress").pointAt(Vec3.atCenterOf(enginePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showOutline(PonderPalette.BLUE, new Object(), util.select().fromTo(tankPos, tankPos), 60);
        scene.overlay().showText(60).text("Backtanks must be placed directly below the Pneumatic Engine").colored(PonderPalette.BLUE).pointAt(Vec3.atCenterOf(tankPos)).placeNearTarget();

        scene.idle(80);
        scene.overlay().showText(60).text("Using a Wrench, the rotation direction can be toggled").colored(PonderPalette.BLUE).pointAt(Vec3.atCenterOf(tankPos)).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().blockSurface(enginePos, Direction.NORTH), Pointing.RIGHT, 40).rightClick().withItem(wrench);

        scene.idle(7);
        scene.world().setKineticSpeed(util.select().fromTo(enginePos, enginePos), -64);
        scene.effects().rotationSpeedIndicator(enginePos);

        scene.idle(53);
        scene.markAsFinished();
    }

    public static void limitation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("pneumatic_engine_limitation", "Limitations of Pneumatic Engines");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(2, 1, 2);
        BlockPos enginePos = util.grid().at(2, 2, 2);

        Selection tankSelection = util.select().fromTo(tankPos, tankPos);
        Selection engineSelection = util.select().fromTo(enginePos, enginePos);

        ItemStack waterBucket = Items.WATER_BUCKET.getDefaultInstance();

        scene.world().setBlock(tankPos, AllBlocks.COPPER_BACKTANK.getDefaultState(), false);
        scene.world().setBlock(enginePos, CCBBlocks.PNEUMATIC_ENGINE_BLOCK.getDefaultState(), false);
        scene.world().showIndependentSection(engineSelection, Direction.DOWN);
        ElementLink<WorldSectionElement> tank = scene.world().showIndependentSection(tankSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().setKineticSpeed(engineSelection, 64);
        scene.effects().rotationSpeedIndicator(enginePos);

        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.BLUE, new Object(), tankSelection, 60);
        scene.overlay().showText(60).colored(PonderPalette.BLUE).text("Pneumatic Engine consumes 2 seconds worth of compressed air supply per second from the backtank").pointAt(Vec3.atCenterOf(tankPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showOutline(PonderPalette.OUTPUT, new Object(), tankSelection, 60);
        scene.overlay().showText(60).text("Pneumatic Engine stops working when compressed air is depleted").colored(PonderPalette.OUTPUT).pointAt(Vec3.atCenterOf(tankPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setKineticSpeed(engineSelection, 0);
        scene.effects().rotationSpeedIndicator(enginePos);
        scene.overlay().showOutline(PonderPalette.RED, new Object(), tankSelection, 60);
        scene.overlay().showText(60).colored(PonderPalette.RED).text("Copper Backtank with depleted air").pointAt(Vec3.atCenterOf(tankPos)).placeNearTarget();

        scene.idle(80);
        scene.world().hideIndependentSection(tank, Direction.NORTH);

        scene.idle(20);
        scene.world().setBlock(util.grid().at(1, 1, 2), Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(2, 1, 1), Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(3, 1, 2), Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(2, 1, 3), Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), false);
        scene.world().setBlock(tankPos, AllBlocks.NETHERITE_BACKTANK.getDefaultState(), false);
        scene.world().showSection(util.select().fromTo(1, 1, 1, 3, 1, 3), Direction.EAST);

        scene.idle(10);
        scene.world().setKineticSpeed(engineSelection, 64);
        scene.effects().rotationSpeedIndicator(enginePos);
        scene.addKeyframe();

        scene.idle(20);
        scene.overlay().showControls(util.vector().blockSurface(tankPos, Direction.NORTH), Pointing.RIGHT, 40).rightClick().withItem(waterBucket);

        scene.idle(7);
        scene.world().modifyBlock(tankPos, s -> s.setValue(BlockStateProperties.WATERLOGGED, true), false);
        scene.world().setKineticSpeed(engineSelection, 0);
        scene.effects().rotationSpeedIndicator(enginePos);
        scene.overlay().showOutline(PonderPalette.RED, new Object(), tankSelection, 60);
        scene.overlay().showText(60).colored(PonderPalette.RED).text("Pneumatic Engine stops working when the backtank is waterlogged").pointAt(Vec3.atCenterOf(tankPos)).placeNearTarget();

        scene.idle(60);
        scene.markAsFinished();
    }
}
