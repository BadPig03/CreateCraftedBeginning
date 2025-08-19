package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

public class BreezeChamberScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("breeze_chamber", "Feeding Breeze Chambers");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos compressorPos = util.grid().at(2, 2, 2);
        BlockPos chamberPos = util.grid().at(2, 1, 2);
        BlockPos deployerPos = util.grid().at(4, 1, 2);

        Selection chamberSelection = util.select().fromTo(chamberPos, chamberPos);
        Selection compressorSelection = util.select().fromTo(compressorPos, compressorPos);
        Selection deployerSelection = util.select().fromTo(deployerPos, deployerPos);
        Selection extraSelection = util.select().fromTo(0, 1, 0, 4, 4, 4).substract(chamberSelection);

        ItemStack ice = new ItemStack(Blocks.ICE.asItem());
        ItemStack blueIce = new ItemStack(Blocks.BLUE_ICE.asItem());
        ItemStack powderSnowBucket = new ItemStack(Items.POWDER_SNOW_BUCKET);

        ParticleEmitter snow = scene.effects().simpleParticleEmitter(ParticleTypes.SNOWFLAKE, Vec3.ZERO);

        scene.world().setBlock(chamberPos, CCBBlocks.BREEZE_CHAMBER_BLOCK.getDefaultState().setValue(BreezeChamberBlock.FROST_LEVEL, BreezeChamberBlock.FrostLevel.RIMING), false);
        scene.world().setBlock(compressorPos, CCBBlocks.AIR_COMPRESSOR_BLOCK.getDefaultState(), false);

        scene.world().showSection(chamberSelection, Direction.DOWN);

        scene.idle(20);
        ElementLink<WorldSectionElement> compressor = scene.world().showIndependentSection(compressorSelection, Direction.DOWN);

        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.INPUT, new Object(), chamberSelection, 60);
        scene.world().modifyBlock(chamberPos, s -> s.setValue(BreezeChamberBlock.COOLER, true), false);
        scene.overlay().showText(60).text("Breeze Chambers can act as coolants for the blocks directly above them").colored(PonderPalette.INPUT).pointAt(Vec3.atCenterOf(chamberPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().modifyBlock(chamberPos, s -> s.setValue(BreezeChamberBlock.COOLER, false), false);
        scene.world().hideIndependentSection(compressor, Direction.UP);

        scene.idle(30);
        scene.world().setBlock(compressorPos, Blocks.AIR.defaultBlockState(), false);
        scene.overlay().showText(60).text("For this, the Breeze Chamber requires feeding with low-temperature items").pointAt(Vec3.atCenterOf(chamberPos)).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().topOf(chamberPos), Pointing.DOWN, 40).rightClick().withItem(ice);

        scene.idle(7);
        scene.effects().emitParticles(util.vector().centerOf(chamberPos), snow, 20, 1);
        scene.world().modifyBlockEntity(chamberPos, BreezeChamberBlockEntity.class, bcbe -> {
            bcbe.wind = true;
            bcbe.windRotationSpeed = 24.f;
        });
        scene.world().modifyBlock(chamberPos, s -> s.setValue(BreezeChamberBlock.FROST_LEVEL, BreezeChamberBlock.FrostLevel.CHILLED), false);

        scene.idle(73);
        scene.overlay().showText(60).text("With a Powder Snow Bucket, the Breeze Chamber can reach an even stronger level of coolness").pointAt(Vec3.atCenterOf(chamberPos)).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().topOf(chamberPos), Pointing.DOWN, 40).rightClick().withItem(powderSnowBucket);

        scene.idle(7);
        scene.effects().emitParticles(util.vector().centerOf(chamberPos), snow, 20, 2);
        scene.world().modifyBlockEntity(chamberPos, BreezeChamberBlockEntity.class, bcbe -> {
            bcbe.wind = true;
            bcbe.windRotationSpeed = 36.f;
        });
        scene.world().modifyBlock(chamberPos, s -> s.setValue(BreezeChamberBlock.FROST_LEVEL, BreezeChamberBlock.FrostLevel.GALLING), false);

        scene.idle(73);
        scene.world().modifyBlockEntityNBT(deployerSelection, DeployerBlockEntity.class, nbt -> nbt.put("HeldItem", blueIce.saveOptional(scene.world().getHolderLookupProvider())));
        scene.world().showSection(extraSelection, Direction.DOWN);

        scene.idle(10);
        scene.overlay().showText(60).text("The feeding process can be automated using Deployers, Mechanical Arms or Mechanical Pumps").pointAt(util.vector().blockSurface(chamberPos.east(2), Direction.UP)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
