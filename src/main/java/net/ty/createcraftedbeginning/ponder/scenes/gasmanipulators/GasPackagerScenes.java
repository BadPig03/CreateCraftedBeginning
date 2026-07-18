package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonStyleUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasPackagerScenes {
    public static void packaging(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("gas_packager_packaging", "Creating and unwrapping balloons");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos centerPos = util.grid().at(3, 1, 3);
        BlockPos creativeTankPos = centerPos.east(2).south();
        BlockPos creativePackagerPos = creativeTankPos.above().north();
        BlockPos leverPos = creativePackagerPos.above();
        BlockPos creativeFunnelPos = creativePackagerPos.north();
        BlockPos ejectorPos = creativeFunnelPos.below();
        BlockPos depotPos = ejectorPos.west(4);
        BlockPos funnelPos = depotPos.above();
        BlockPos motorPos = ejectorPos.north();
        BlockPos packagerPos = funnelPos.south();
        BlockPos tankPos = packagerPos.below().south();

        Selection creativeTankSelection = util.select().fromTo(creativeTankPos, creativeTankPos.above());
        Selection creativePackagerSelection = util.select().fromTo(leverPos, creativePackagerPos.below());
        Selection creativeFunnelSelection = util.select().position(creativeFunnelPos);
        Selection ejectorSelection = util.select().position(ejectorPos);
        Selection depotSelection = util.select().position(depotPos);
        Selection funnelSelection = util.select().position(funnelPos);
        Selection packagerSelection = util.select().fromTo(packagerPos, packagerPos.below());
        Selection tankSelection = util.select().fromTo(tankPos, tankPos.above());
        Selection motorSelection = util.select().position(motorPos);

        Vec3 creativePackagerVec = util.vector().centerOf(creativePackagerPos);
        Vec3 creativeTankVec = util.vector().centerOf(creativeTankPos);
        Vec3 leverVec = util.vector().centerOf(leverPos);
        Vec3 packagerVec = util.vector().centerOf(packagerPos);

        ItemStack balloon = BalloonStyleUtils.getDefaultBalloon().copy();

        AABB creativeTankArea = new AABB(creativeTankVec, creativeTankVec);

        Object creativeTankObject = new Object();

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(creativeTankSelection, Direction.NORTH);

        scene.idle(3);
        scene.world().showSection(creativePackagerSelection, Direction.WEST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, creativeTankObject, creativeTankArea, 3);

        scene.idle(3);
        creativeTankArea = creativeTankArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, creativeTankObject, creativeTankArea, 3);

        scene.idle(3);
        creativeTankArea = creativeTankArea.expandTowards(0, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, creativeTankObject, creativeTankArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, creativeTankObject, creativeTankArea, 60);
        scene.overlay().showText(60).text("Attach the Gas Packager to its target gas inventory").pointAt(creativePackagerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(tankSelection, Direction.NORTH);

        scene.idle(3);
        scene.world().showSection(packagerSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(ejectorSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().setBlock(funnelPos, Blocks.AIR.defaultBlockState(), false);
        scene.world().showSection(depotSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);
        scene.world().showSection(motorSelection, Direction.SOUTH);

        scene.idle(15);
        scene.world().setKineticSpeed(ejectorSelection, mediumSpeed);
        scene.world().setKineticSpeed(motorSelection, mediumSpeed);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(20);
        scene.overlay().showText(60).text("Apply a redstone signal, and it will pack the gas from the inventory into balloons").pointAt(leverVec).placeNearTarget().attachKeyFrame();

        scene.idle(10);
        scene.world().toggleRedstonePower(creativePackagerSelection);
        scene.effects().indicateRedstone(leverPos);

        scene.idle(10);
        PackageItem.addAddress(balloon, "Endpoint");
        scene.world().modifyBlockEntity(creativePackagerPos, GasPackagerBlockEntity.class, be -> {
            be.animationTicks = GasPackagerBlockEntity.CYCLE;
            be.animationInward = false;
            be.heldBox = balloon;
        });

        scene.idle(40);
        scene.world().showSection(creativeFunnelSelection, Direction.SOUTH);

        scene.idle(10);
        scene.world().createItemOnBeltLike(ejectorPos, Direction.SOUTH, balloon);
        scene.world().modifyBlockEntity(creativePackagerPos, GasPackagerBlockEntity.class, be -> be.heldBox = ItemStack.EMPTY);

        scene.idle(40);
        scene.world().restoreBlocks(funnelSelection);
        scene.world().showSection(funnelSelection, Direction.EAST);

        scene.idle(15);
        scene.world().removeItemsFromBelt(depotPos);
        scene.world().flapFunnel(funnelPos, false);
        scene.world().modifyBlockEntity(packagerPos, GasPackagerBlockEntity.class, be -> {
            be.animationTicks = GasPackagerBlockEntity.CYCLE;
            be.animationInward = true;
            be.previouslyUnwrapped = balloon;
        });
        scene.overlay().showText(60).text("Balloons placed into the Gas Packager will be unpacked, and the gas will be stored in the inventory").pointAt(packagerVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
