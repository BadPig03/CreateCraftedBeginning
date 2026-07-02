package net.ty.createcraftedbeginning.ponder.scenes.end;

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
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.content.end.endincinerationblower.EndIncinerationBlowerBlockEntity;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EndIncinerationBlowerScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("end_incineration_blower", "Using End Incineration Blowers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos blowerPos = util.grid().at(3, 2, 3);
        BlockPos blowerTopPos = blowerPos.above();
        BlockPos cogPos = blowerPos.below();
        BlockPos largeCogPos = cogPos.east().north();
        BlockPos depotPos = cogPos.west(2).north(2);
        BlockPos motorPos = largeCogPos.above();
        BlockPos itemPos = depotPos.south();

        Selection blowerSelection = util.select().position(blowerPos);
        Selection blowerTopSelection = util.select().position(blowerTopPos);
        Selection blowerAllSelection = util.select().fromTo(blowerPos, blowerTopPos);
        Selection cogSelection = util.select().position(cogPos);
        Selection largeCogSelection = util.select().position(largeCogPos);
        Selection motorSelection = util.select().position(motorPos);
        Selection depotSelection = util.select().position(depotPos);

        Vec3 blowerSideVec = util.vector().blockSurface(blowerPos, Direction.NORTH);
        Vec3 blowerTopVec = util.vector().centerOf(blowerTopPos);

        AABB workingArea = new AABB(blowerTopPos).deflate(0.5);

        Object outlineObject = new Object();

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        ItemStack beefItem = new ItemStack(Items.BEEF);
        ItemStack cookedBeefItem = new ItemStack(Items.COOKED_BEEF);
        ItemStack stoneItem = new ItemStack(Blocks.STONE);
        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        scene.idle(20);
        scene.world().showSection(cogSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().setBlock(blowerPos, CCBBlocks.END_CASING_BLOCK.getDefaultState(), false);
        scene.world().showSection(blowerSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().setBlock(blowerTopPos, CCBBlocks.END_INCINERATION_BLOWER_BLOCK.getDefaultState(), false);
        scene.world().showSection(blowerTopSelection, Direction.DOWN);
        scene.overlay().showText(60).text("The End Incineration Blower must be placed on an End Casing").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(blowerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(12);
        scene.effects().indicateSuccess(blowerTopPos);
        scene.effects().indicateSuccess(blowerPos);
        scene.world().restoreBlocks(blowerAllSelection);

        scene.idle(78);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(largeCogSelection, Direction.WEST);
        scene.world().showSection(motorSelection, Direction.WEST);

        scene.idle(15);
        scene.world().setKineticSpeed(motorSelection, mediumSpeed / 2);
        scene.world().setKineticSpeed(largeCogSelection, mediumSpeed / 2);
        scene.world().setKineticSpeed(cogSelection, -mediumSpeed);
        scene.world().setKineticSpeed(blowerAllSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(blowerTopPos);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.overlay().showText(60).text("It can smelt items or ignite entities within its working range").colored(PonderPalette.GREEN).pointAt(blowerTopVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, outlineObject, workingArea, 5);

        scene.idle(5);
        workingArea = workingArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, outlineObject, workingArea, 5);

        scene.idle(5);
        workingArea = EndIncinerationBlowerBlockEntity.calculateArea(blowerTopPos, mediumSpeed);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, outlineObject, workingArea, 80);

        scene.idle(80);
        workingArea = EndIncinerationBlowerBlockEntity.calculateArea(blowerTopPos, mediumSpeed * 3);
        scene.world().setKineticSpeed(motorSelection, mediumSpeed * 1.5f);
        scene.world().setKineticSpeed(largeCogSelection, mediumSpeed * 1.5f);
        scene.world().setKineticSpeed(cogSelection, -mediumSpeed * 3);
        scene.world().setKineticSpeed(blowerAllSelection, -mediumSpeed * 3);
        scene.effects().rotationSpeedIndicator(blowerTopPos);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.overlay().showText(60).text("A higher rotation speed increases its working range").colored(PonderPalette.FAST).pointAt(blowerTopVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, outlineObject, workingArea, 80);

        scene.idle(80);
        scene.overlay().showFilterSlotInput(blowerSideVec.add(0, 0, -0.0625), Direction.NORTH, 60);
        scene.overlay().showText(60).text("Working modes are configurable").colored(PonderPalette.BLUE).pointAt(blowerSideVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, outlineObject, workingArea, 80);

        scene.idle(80);
        scene.world().createItemEntity(util.vector().centerOf(itemPos), util.vector().of(0, 0.1, 0), beefItem);
        scene.overlay().showControls(blowerSideVec, Pointing.DOWN, 60).showing(CCBIcons.I_SMOKING);
        scene.overlay().showText(60).text("\"Bulk Smoking\": Cooks food").colored(PonderPalette.GREEN).pointAt(blowerTopVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, outlineObject, workingArea, 80);

        scene.idle(10);
        scene.effects().emitParticles(util.vector().topOf(itemPos.below()), scene.effects().simpleParticleEmitter(ParticleTypes.LARGE_SMOKE, Vec3.ZERO), 1, 40);

        scene.idle(40);
        scene.world().modifyEntities(ItemEntity.class, ie -> ie.setItem(cookedBeefItem));

        scene.idle(20);
        scene.world().modifyEntities(ItemEntity.class, Entity::discard);
        scene.world().showSection(depotSelection, Direction.NORTH);

        scene.idle(10);
        scene.overlay().showControls(blowerSideVec, Pointing.DOWN, 60).showing(CCBIcons.I_BLASTING);
        scene.overlay().showText(60).text("\"Bulk Smelting\": Smelting ores or other items").colored(PonderPalette.GREEN).pointAt(blowerTopVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, outlineObject, workingArea, 90);

        scene.idle(10);
        scene.effects().emitParticles(util.vector().topOf(depotPos), scene.effects().simpleParticleEmitter(ParticleTypes.LARGE_SMOKE, Vec3.ZERO), 1, 40);

        scene.idle(40);
        scene.world().modifyBlockEntity(depotPos, DepotBlockEntity.class, be -> be.setHeldItem(stoneItem));

        scene.idle(20);
        scene.world().hideSection(depotSelection, Direction.UP);

        scene.idle(20);
        scene.overlay().showControls(blowerSideVec, Pointing.DOWN, 60).showing(CCBIcons.I_IGNITION);
        scene.overlay().showText(60).text("\"Mob Ignition\": Sets mobs on fire, and also counts as a player kill").colored(PonderPalette.GREEN).pointAt(blowerTopVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, outlineObject, workingArea, 80);

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(blowerTopPos, Direction.NORTH), Pointing.RIGHT, 27).rightClick().withItem(wrenchItem);

        scene.idle(7);
        scene.overlay().showText(60).text("Right-click with a Wrench to toggle the working range display").colored(PonderPalette.BLUE).pointAt(blowerTopVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
