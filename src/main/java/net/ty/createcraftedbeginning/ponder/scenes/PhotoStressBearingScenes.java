package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.phohostressbearing.PhotoStressBearingBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

public class PhotoStressBearingScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("photo-stress_bearing", "Generates Rotational Force using Photo-Stress Bearings");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos bearingPos = util.grid().at(3, 2, 2);
        BlockPos gearboxPos = util.grid().at(3, 1, 2);
        BlockPos shaftPos = util.grid().at(2, 1, 2);
        BlockPos speedometerPos = util.grid().at(1, 1, 2);
        BlockPos stressometerPos = util.grid().at(0, 1, 2);
        BlockPos ironBlockPos = util.grid().at(3, 3, 2);

        Selection allSelection = util.select().fromTo(stressometerPos, bearingPos);
        Selection ironBlocksSelection = util.select().fromTo(2, 4, 1, 4, 4, 3);
        Selection bearingSelection = util.select().fromTo(bearingPos, bearingPos);

        scene.world().setBlock(bearingPos, CCBBlocks.PHOTO_STRESS_BEARING_BLOCK.getDefaultState(), false);
        scene.world().modifyBlockEntityNBT(bearingSelection, PhotoStressBearingBlockEntity.class, compound -> compound.putInt("SkyLight", 15));
        scene.world().setBlock(gearboxPos, AllBlocks.GEARBOX.getDefaultState(), false);
        scene.world().modifyBlock(gearboxPos, s -> s.setValue(BlockStateProperties.AXIS, Direction.Axis.Z), false);
        scene.world().setBlock(shaftPos, AllBlocks.SHAFT.getDefaultState(), false);
        scene.world().modifyBlock(shaftPos, s -> s.setValue(BlockStateProperties.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(speedometerPos, AllBlocks.SPEEDOMETER.getDefaultState(), false);
        scene.world().setBlock(stressometerPos, AllBlocks.STRESSOMETER.getDefaultState(), false);
        scene.world().modifyBlocks(util.select().fromTo(speedometerPos, stressometerPos), s -> s.setValue(BlockStateProperties.FACING, Direction.UP), false);
        scene.world().showIndependentSection(allSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().setKineticSpeed(allSelection, -32);
        scene.effects().rotationSpeedIndicator(bearingPos);

        scene.idle(20);
        scene.overlay().showOutline(PonderPalette.BLUE, new Object(), util.select().fromTo(3, 3, 2, 3, 10, 2), 60);
        scene.overlay().showText(60).colored(PonderPalette.BLUE).text("Photo-Stress Bearing can absorb direct sunlight to generate kinetic stress").pointAt(Vec3.atCenterOf(bearingPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlocks(ironBlocksSelection, AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState(), false);
        ElementLink<WorldSectionElement> ironBlocks = scene.world().showIndependentSection(ironBlocksSelection, Direction.DOWN);

        scene.idle(10);
        scene.world().modifyBlockEntityNBT(bearingSelection, PhotoStressBearingBlockEntity.class, compound -> compound.putInt("SkyLight", 13));
        scene.world().setKineticSpeed(allSelection, -28);
        scene.effects().rotationSpeedIndicator(bearingPos);
        scene.overlay().showOutline(PonderPalette.OUTPUT, new Object(), util.select().fromTo(3, 5, 2, 3, 10, 2), 60);
        scene.overlay().showText(60).colored(PonderPalette.OUTPUT).text("Rotation speed is proportional to sky light level").pointAt(Vec3.atCenterOf(bearingPos)).placeNearTarget();

        scene.idle(80);
        scene.world().hideIndependentSection(ironBlocks, Direction.UP);

        scene.idle(10);
        scene.world().setBlock(ironBlockPos, AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState(), false);
        ElementLink<WorldSectionElement> ironBlock = scene.world().showIndependentSection(util.select().fromTo(ironBlockPos, ironBlockPos), Direction.DOWN);

        scene.idle(10);
        scene.world().modifyBlockEntityNBT(bearingSelection, PhotoStressBearingBlockEntity.class, compound -> compound.putInt("SkyLight", 0));
        scene.world().setKineticSpeed(allSelection, -2);
        scene.effects().rotationSpeedIndicator(bearingPos);
        scene.overlay().showOutline(PonderPalette.RED, new Object(), util.select().fromTo(3, 4, 2, 3, 10, 2), 60);
        scene.overlay().showText(60).colored(PonderPalette.RED).text("Rotation speed maintains a minimum of 2 RPM even at sky light level 0").pointAt(Vec3.atCenterOf(bearingPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().hideIndependentSection(ironBlock, Direction.UP);

        scene.idle(10);
        scene.world().modifyBlockEntityNBT(bearingSelection, PhotoStressBearingBlockEntity.class, compound -> compound.putInt("SkyLight", 7));
        scene.world().setKineticSpeed(allSelection, -16);
        scene.effects().rotationSpeedIndicator(bearingPos);

        scene.idle(20);
        ElementLink<EntityElement> lightningBolt = scene.world().createEntity(w -> {
            LightningBolt entity = EntityType.LIGHTNING_BOLT.create(w);
            Vec3 pos = util.vector().of(3, 1, 1);
            if (entity != null) {
                entity.setPos(pos.x, pos.y, pos.z);
            }
            return entity;
        });

        scene.idle(20);
        scene.overlay().showText(60).colored(PonderPalette.OUTPUT).text("During rain or thunderstorms, the rotation speed is halved").pointAt(Vec3.atCenterOf(bearingPos)).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.world().modifyEntity(lightningBolt, Entity::discard);

        scene.idle(60);
        Vec3 surface = util.vector().blockSurface(bearingPos, Direction.WEST).subtract(0, 1 / 8f, 0);
        scene.overlay().showControls(surface, Pointing.DOWN, 60).rightClick();
        scene.overlay().showFilterSlotInput(surface, Direction.WEST, 50);
        scene.world().setKineticSpeed(allSelection, 16);
        scene.effects().rotationSpeedIndicator(bearingPos);
        scene.overlay().showText(60).text("Use the value panel to configure its rotation direction").pointAt(surface).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void other_dimension(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("photo-stress_bearing_other_dimension", "Photo-Stress Bearings in other dimensions");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos bearingPos = util.grid().at(2, 1, 2);

        Selection bearingSelection = util.select().fromTo(bearingPos, bearingPos);

        scene.world().setBlock(bearingPos, CCBBlocks.PHOTO_STRESS_BEARING_BLOCK.getDefaultState(), false);
        scene.world().modifyBlockEntityNBT(bearingSelection, PhotoStressBearingBlockEntity.class, compound -> compound.putInt("SkyLight", 0));
        scene.world().showIndependentSection(bearingSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().setKineticSpeed(bearingSelection, 0);
        scene.effects().rotationSpeedIndicator(bearingPos);

        scene.idle(20);
        scene.overlay().showOutline(PonderPalette.RED, new Object(), util.select().fromTo(2, 2, 2, 2, 10, 2), 60);
        scene.overlay().showText(60).colored(PonderPalette.RED).text("In other dimensions, Photo-Stress Bearings will stop working").pointAt(Vec3.atCenterOf(bearingPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
