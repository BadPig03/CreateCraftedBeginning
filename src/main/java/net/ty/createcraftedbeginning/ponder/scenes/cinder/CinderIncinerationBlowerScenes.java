package net.ty.createcraftedbeginning.ponder.scenes.cinder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.cinder.cinderincinerationblower.CinderIncinerationBlowerBlockEntity;
import net.ty.createcraftedbeginning.data.CCBIcons;
import org.jetbrains.annotations.NotNull;

public class CinderIncinerationBlowerScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("cinder_incineration_blower", "Using Cinder Incineration Blowers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos blowerPos = util.grid().at(3, 2, 3);
        BlockPos cogPos = blowerPos.below();
        BlockPos largeCogPos = cogPos.east().north();
        BlockPos depotPos = cogPos.west(2).north(2);
        BlockPos motorPos = largeCogPos.above();
        BlockPos itemPos = depotPos.south();

        Selection blowerSelection = util.select().position(blowerPos);
        Selection cogSelection = util.select().position(cogPos);
        Selection largeCogSelection = util.select().position(largeCogPos);
        Selection motorSelection = util.select().position(motorPos);
        Selection depotSelection = util.select().position(depotPos);

        Vec3 blowerTopVec = util.vector().topOf(blowerPos);

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        Object outlineObject = new Object();

        ItemStack beefItem = new ItemStack(Items.BEEF);
        ItemStack cookedBeefItem = new ItemStack(Items.COOKED_BEEF);
        ItemStack stoneItem = new ItemStack(Blocks.STONE);
        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        scene.idle(20);
        scene.world().showSection(cogSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(blowerSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(largeCogSelection, Direction.WEST);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(motorSelection, Direction.DOWN);
        scene.world().setKineticSpeed(motorSelection, mediumSpeed / 2);
        scene.world().setKineticSpeed(largeCogSelection, mediumSpeed / 2);
        scene.world().setKineticSpeed(cogSelection, -mediumSpeed);
        scene.world().setKineticSpeed(blowerSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(blowerPos);
        scene.overlay().showText(60).text("The Cinder Incineration Blower can smelt items or ignite entities within its working range.").pointAt(Vec3.atCenterOf(blowerPos)).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outlineObject, new AABB(blowerPos), 5);

        scene.idle(5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outlineObject, CinderIncinerationBlowerBlockEntity.calculateArea(blowerPos, mediumSpeed), 80);

        scene.idle(80);
        scene.world().setKineticSpeed(motorSelection, mediumSpeed * 1.5f);
        scene.world().setKineticSpeed(largeCogSelection, mediumSpeed * 1.5f);
        scene.world().setKineticSpeed(cogSelection, -mediumSpeed * 3);
        scene.world().setKineticSpeed(blowerSelection, -mediumSpeed * 3);
        scene.effects().rotationSpeedIndicator(blowerPos);
        scene.overlay().showText(60).text("A higher rotation speed increases its working range.").pointAt(Vec3.atCenterOf(blowerPos)).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outlineObject, CinderIncinerationBlowerBlockEntity.calculateArea(blowerPos, mediumSpeed * 3), 80);

        scene.idle(80);
        scene.overlay().showScrollInput(blowerTopVec, Direction.UP, 60);
        scene.overlay().showText(60).text("Working modes are configurable.").pointAt(blowerTopVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outlineObject, CinderIncinerationBlowerBlockEntity.calculateArea(blowerPos, mediumSpeed * 3), 80);

        scene.idle(80);
        scene.world().createItemEntity(util.vector().centerOf(itemPos), util.vector().of(0, 0.1, 0), beefItem);
        scene.overlay().showControls(blowerTopVec, Pointing.DOWN, 60).showing(CCBIcons.I_SMOKING);
        scene.overlay().showText(60).text("\"Bulk Smoking\": Cooks food.").pointAt(Vec3.atCenterOf(blowerPos)).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outlineObject, CinderIncinerationBlowerBlockEntity.calculateArea(blowerPos, mediumSpeed * 3), 80);

        scene.idle(10);
        scene.effects().emitParticles(util.vector().topOf(itemPos.below()), scene.effects().simpleParticleEmitter(ParticleTypes.LARGE_SMOKE, Vec3.ZERO), 1, 40);

        scene.idle(40);
        scene.world().modifyEntities(ItemEntity.class, ie -> ie.setItem(cookedBeefItem));

        scene.idle(20);
        scene.world().modifyEntities(ItemEntity.class, Entity::discard);
        scene.world().showSection(depotSelection, Direction.NORTH);

        scene.idle(10);
        scene.overlay().showControls(blowerTopVec, Pointing.DOWN, 60).showing(CCBIcons.I_BLASTING);
        scene.overlay().showText(60).text("\"Bulk Smelting\": Smelting ores or other items.").pointAt(Vec3.atCenterOf(blowerPos)).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outlineObject, CinderIncinerationBlowerBlockEntity.calculateArea(blowerPos, mediumSpeed * 3), 90);

        scene.idle(10);
        scene.effects().emitParticles(util.vector().topOf(depotPos), scene.effects().simpleParticleEmitter(ParticleTypes.LARGE_SMOKE, Vec3.ZERO), 1, 40);

        scene.idle(40);
        scene.world().modifyBlockEntity(depotPos, DepotBlockEntity.class, be -> be.setHeldItem(stoneItem));

        scene.idle(20);
        scene.world().hideSection(depotSelection, Direction.UP);

        scene.idle(20);
        scene.overlay().showControls(blowerTopVec, Pointing.DOWN, 60).showing(CCBIcons.I_IGNITION);
        scene.overlay().showText(60).text("\"Mob Ignition\": Sets mobs on fire, but does not count as a player kill.").pointAt(Vec3.atCenterOf(blowerPos)).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outlineObject, CinderIncinerationBlowerBlockEntity.calculateArea(blowerPos, mediumSpeed * 3), 80);

        scene.idle(80);
        scene.overlay().showControls(blowerTopVec, Pointing.DOWN, 60).showing(CCBIcons.I_GRINDING);
        scene.overlay().showText(60).text("\"Mob Grinding\": Counts as a player kill instead.").pointAt(Vec3.atCenterOf(blowerPos)).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outlineObject, CinderIncinerationBlowerBlockEntity.calculateArea(blowerPos, mediumSpeed * 3), 80);

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(blowerPos, Direction.NORTH), Pointing.RIGHT, 27).rightClick().withItem(wrenchItem);

        scene.idle(7);
        scene.overlay().showText(60).text("Right-click with a Wrench to toggle the working range display.").pointAt(Vec3.atCenterOf(blowerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
