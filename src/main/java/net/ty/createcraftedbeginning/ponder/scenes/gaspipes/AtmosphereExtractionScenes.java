package net.ty.createcraftedbeginning.ponder.scenes.gaspipes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AtmosphereExtractionScenes {
    public static void dimensions(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("atmosphere_extraction_dimensions", "Extracting Gas from Different Dimensions");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos intakePos = util.grid().at(2, 1, 3);
        BlockPos atmospherePos = intakePos.west();
        BlockPos tankPos = util.grid().at(5, 1, 3);

        Selection pipeSelection = util.select().fromTo(intakePos, tankPos.west());
        Selection tankSelection = util.select().fromTo(tankPos, tankPos.above());

        Vec3 intakeVec = util.vector().blockSurface(intakePos, Direction.WEST);
        Vec3 atmosphereVec = util.vector().centerOf(atmospherePos);
        Vec3 tankTopVec = util.vector().topOf(tankPos.above());

        AABB atmosphereArea = new AABB(atmosphereVec, atmosphereVec);
        AABB flowArea = new AABB(atmosphereVec, atmosphereVec);

        Object atmosphereObject = new Object();
        Object flowObject = new Object();

        ItemStack naturalAirItem = VirtualGasItems.createVirtualItem(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        ItemStack ultrawarmAirItem = VirtualGasItems.createVirtualItem(new GasStack(CCBGases.ULTRAWARM_AIR.get(), 1));
        ItemStack etherealAirItem = VirtualGasItems.createVirtualItem(new GasStack(CCBGases.ETHEREAL_AIR.get(), 1));

        scene.world().setBlock(atmospherePos, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(atmospherePos.above(), Blocks.AIR.defaultBlockState(), false);

        scene.idle(20);
        scene.world().showSection(pipeSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(tankSelection, Direction.WEST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, atmosphereObject, atmosphereArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, atmosphereObject, atmosphereArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("Below atmospheric pressure, open pipe ends draw in ambient gas").pointAt(intakeVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                scene.world().setBlock(util.grid().at(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), false);
            }

            scene.idle(3);
        }

        scene.overlay().showText(60).text("Ordinary Overworld environments provide Natural Air").pointAt(intakeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea.inflate(0, 0.3125, 0.3125), 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, flowObject, flowArea.inflate(0, 0.3125, 0.3125).expandTowards(3.5, 0, 0), 47);
        scene.overlay().showControls(tankTopVec, Pointing.DOWN, 47).withItem(naturalAirItem.copy());

        scene.idle(53);
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                scene.world().setBlock(util.grid().at(x, 0, z), Blocks.NETHERRACK.defaultBlockState(), false);
            }

            scene.idle(3);
        }

        scene.overlay().showText(60).text("The Nether provides Ultrawarm Air").pointAt(intakeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea.inflate(0, 0.3125, 0.3125), 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, flowObject, flowArea.inflate(0, 0.3125, 0.3125).expandTowards(3.5, 0, 0), 47);
        scene.overlay().showControls(tankTopVec, Pointing.DOWN, 47).withItem(ultrawarmAirItem.copy());

        scene.idle(53);
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                scene.world().setBlock(util.grid().at(x, 0, z), Blocks.END_STONE.defaultBlockState(), false);
            }

            scene.idle(3);
        }

        scene.overlay().showText(60).text("The End provides Ethereal Air").pointAt(intakeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea.inflate(0, 0.3125, 0.3125), 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, flowObject, flowArea.inflate(0, 0.3125, 0.3125).expandTowards(3.5, 0, 0), 47);
        scene.overlay().showControls(tankTopVec, Pointing.DOWN, 47).withItem(etherealAirItem.copy());

        scene.idle(70);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, atmosphereObject, atmosphereArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, atmosphereObject, atmosphereArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("Special biomes can override a dimension's usual gas").colored(PonderPalette.BLUE).pointAt(intakeVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void biomes(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("atmosphere_extraction_biomes", "Extracting Gas from Special Biomes");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos intakePos = util.grid().at(2, 1, 3);
        BlockPos atmospherePos = intakePos.west();
        BlockPos tankPos = util.grid().at(5, 1, 3);

        Selection pipeSelection = util.select().fromTo(intakePos, tankPos.west());
        Selection tankSelection = util.select().fromTo(tankPos, tankPos.above());

        Vec3 intakeVec = util.vector().blockSurface(intakePos, Direction.WEST);
        Vec3 atmosphereVec = util.vector().centerOf(atmospherePos);
        Vec3 tankTopVec = util.vector().topOf(tankPos.above());
        Vec3 groundVec = util.vector().centerOf(atmospherePos.below());

        AABB atmosphereArea = new AABB(atmosphereVec, atmosphereVec);
        AABB flowArea = new AABB(atmosphereVec, atmosphereVec);
        AABB groundArea = new AABB(groundVec, groundVec);

        Object atmosphereObject = new Object();
        Object flowObject = new Object();
        Object groundObject = new Object();

        ItemStack sporeAirItem = VirtualGasItems.createVirtualItem(new GasStack(CCBGases.SPORE_AIR.get(), 1));
        ItemStack sculkAirItem = VirtualGasItems.createVirtualItem(new GasStack(CCBGases.SCULK_AIR.get(), 1));

        scene.world().setBlock(atmospherePos, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(atmospherePos.above(), Blocks.AIR.defaultBlockState(), false);

        scene.idle(20);
        scene.world().showSection(pipeSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(tankSelection, Direction.WEST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, atmosphereObject, atmosphereArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, atmosphereObject, atmosphereArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("The biome directly outside the pipe determines its gas").pointAt(intakeVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                scene.world().setBlock(util.grid().at(x, 0, z), Blocks.MYCELIUM.defaultBlockState(), false);
            }

            scene.idle(3);
        }

        scene.overlay().showText(60).text("Mushroom Fields provide Spore Air").pointAt(intakeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea.inflate(0, 0.3125, 0.3125), 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, flowObject, flowArea.inflate(0, 0.3125, 0.3125).expandTowards(3.5, 0, 0), 47);
        scene.overlay().showControls(tankTopVec, Pointing.DOWN, 47).withItem(sporeAirItem.copy());

        scene.idle(53);
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                scene.world().setBlock(util.grid().at(x, 0, z), Blocks.SCULK.defaultBlockState(), false);
            }

            scene.idle(3);
        }

        scene.overlay().showText(60).text("The Deep Dark provides Sculk Air").pointAt(intakeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea.inflate(0, 0.3125, 0.3125), 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, flowObject, flowArea.inflate(0, 0.3125, 0.3125).expandTowards(3.5, 0, 0), 47);
        scene.overlay().showControls(tankTopVec, Pointing.DOWN, 47).withItem(sculkAirItem.copy());

        scene.idle(70);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, groundObject, groundArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, groundObject, groundArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("Placing Mycelium or Sculk does not change the biome").colored(PonderPalette.RED).pointAt(groundVec).placeNearTarget().attachKeyFrame();

        scene.idle(77);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, atmosphereObject, atmosphereArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, atmosphereObject, atmosphereArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("Instead, an intake in the actual biome can extract its gas").colored(PonderPalette.GREEN).pointAt(intakeVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void bubbleColumns(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("atmosphere_extraction_bubble_columns", "Extracting Gas from Bubble Columns");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos intakePos = util.grid().at(2, 1, 3);
        BlockPos atmospherePos = intakePos.west();
        BlockPos tankPos = util.grid().at(5, 1, 3);

        Selection pipeSelection = util.select().fromTo(intakePos, tankPos.west());
        Selection tankSelection = util.select().fromTo(tankPos, tankPos.above());
        Selection columnBottomSelection = util.select().position(atmospherePos);
        Selection columnTopSelection = util.select().position(atmospherePos.above());

        Vec3 intakeVec = util.vector().blockSurface(intakePos, Direction.WEST);
        Vec3 atmosphereVec = util.vector().centerOf(atmospherePos);
        Vec3 tankTopVec = util.vector().topOf(tankPos.above());

        AABB atmosphereArea = new AABB(atmosphereVec, atmosphereVec);
        AABB flowArea = new AABB(atmosphereVec, atmosphereVec);

        Object atmosphereObject = new Object();
        Object flowObject = new Object();
        Object columnObject = new Object();
        Object waterObject = new Object();

        ItemStack moistAirItem = VirtualGasItems.createVirtualItem(new GasStack(CCBGases.MOIST_AIR.get(), 1));

        ParticleEmitter bubbles = scene.effects().simpleParticleEmitter(ParticleTypes.BUBBLE_COLUMN_UP, new Vec3(0, 0.04, 0));
        ParticleEmitter downwardBubbles = scene.effects().simpleParticleEmitter(ParticleTypes.CURRENT_DOWN, Vec3.ZERO);

        scene.world().setBlock(atmospherePos, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(atmospherePos.above(), Blocks.AIR.defaultBlockState(), false);

        scene.idle(20);
        scene.world().showSection(pipeSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(tankSelection, Direction.WEST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, atmosphereObject, atmosphereArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, atmosphereObject, atmosphereArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("Open pipe ends can extract gas from Bubble Columns").pointAt(intakeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Soul Sand creates Bubble Columns in source water").pointAt(atmosphereVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().setBlock(atmospherePos.below(), Blocks.SOUL_SAND.defaultBlockState(), false);
        scene.world().setBlock(atmospherePos, Blocks.WATER.defaultBlockState(), false);
        scene.world().setBlock(atmospherePos.above(), Blocks.WATER.defaultBlockState(), false);
        scene.world().showSection(columnBottomSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(columnTopSelection, Direction.DOWN);

        scene.idle(14);
        scene.world().setBlock(atmospherePos, Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(BubbleColumnBlock.DRAG_DOWN, false), false);
        scene.world().setBlock(atmospherePos.above(), Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(BubbleColumnBlock.DRAG_DOWN, false), false);
        scene.effects().emitParticles(atmosphereVec.add(0, -0.35, 0), bubbles, 1, 100);
        scene.effects().emitParticles(atmosphereVec.add(0, 0.65, 0), bubbles, 1, 100);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, columnObject, atmosphereArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, columnObject, atmosphereArea.inflate(0.5).expandTowards(0, 1, 0), 33);

        scene.idle(47);
        scene.overlay().showText(60).text("Bubble Columns provide Moist Air in any biome").pointAt(intakeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea.inflate(0, 0.3125, 0.3125), 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, flowObject, flowArea.inflate(0, 0.3125, 0.3125).expandTowards(3.5, 0, 0), 47);
        scene.overlay().showControls(tankTopVec, Pointing.DOWN, 47).withItem(moistAirItem.copy());

        scene.idle(73);
        scene.overlay().showText(60).text("Ordinary water cannot supply gas").colored(PonderPalette.RED).pointAt(atmosphereVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().setBlock(atmospherePos.below(), Blocks.SAND.defaultBlockState(), false);
        scene.world().setBlock(atmospherePos, Blocks.WATER.defaultBlockState(), false);
        scene.world().setBlock(atmospherePos.above(), Blocks.WATER.defaultBlockState(), false);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, waterObject, atmosphereArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, waterObject, atmosphereArea.inflate(0.5), 60);

        scene.idle(70);
        scene.overlay().showText(60).text("Instead, a Bubble Column directly beside the pipe can supply gas").colored(PonderPalette.GREEN).pointAt(intakeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().setBlock(atmospherePos.below(), Blocks.SOUL_SAND.defaultBlockState(), false);
        scene.world().setBlock(atmospherePos, Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(BubbleColumnBlock.DRAG_DOWN, false), false);
        scene.world().setBlock(atmospherePos.above(), Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(BubbleColumnBlock.DRAG_DOWN, false), false);
        scene.effects().emitParticles(atmosphereVec.add(0, -0.35, 0), bubbles, 1, 60);
        scene.effects().emitParticles(atmosphereVec.add(0, 0.65, 0), bubbles, 1, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea.inflate(0, 0.3125, 0.3125), 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, flowObject, flowArea.inflate(0, 0.3125, 0.3125).expandTowards(3.5, 0, 0), 60);
        scene.overlay().showControls(tankTopVec, Pointing.DOWN, 60).withItem(moistAirItem.copy());

        scene.idle(80);
        scene.overlay().showText(60).text("Downward Bubble Columns above Magma Blocks can also supply Moist Air").colored(PonderPalette.BLUE).pointAt(atmosphereVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().setBlock(atmospherePos.below(), Blocks.MAGMA_BLOCK.defaultBlockState(), false);
        scene.world().setBlock(atmospherePos, Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(BubbleColumnBlock.DRAG_DOWN, true), false);
        scene.world().setBlock(atmospherePos.above(), Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(BubbleColumnBlock.DRAG_DOWN, true), false);
        scene.effects().emitParticles(atmosphereVec.add(0, 0.3, 0), downwardBubbles, 0.5F, 100);
        scene.effects().emitParticles(atmosphereVec.add(0, 1.3, 0), downwardBubbles, 0.5F, 100);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, columnObject, atmosphereArea.move(0, -1, 0), 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, columnObject, atmosphereArea.move(0, -1, 0).inflate(0.5), 50);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, flowObject, flowArea.inflate(0, 0.3125, 0.3125), 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, flowObject, flowArea.inflate(0, 0.3125, 0.3125).expandTowards(3.5, 0, 0), 47);
        scene.overlay().showControls(tankTopVec, Pointing.DOWN, 47).withItem(moistAirItem.copy());

        scene.idle(47);
        scene.markAsFinished();
    }
}
