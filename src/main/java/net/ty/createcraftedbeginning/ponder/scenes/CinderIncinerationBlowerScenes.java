package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public class CinderIncinerationBlowerScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("cinder_incineration_blower", "Igniting Mobs using Cinder Incineration Blowers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos motorPos = util.grid().at(3, 1, 3);
        BlockPos blowerPos = util.grid().at(3, 2, 3);

        Selection allSelection = util.select().fromTo(motorPos, blowerPos);

        Vec3 scroll = util.vector().blockSurface(blowerPos, Direction.UP);

        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState(), false);
        scene.world().modifyBlock(motorPos, s -> s.setValue(BlockStateProperties.FACING, Direction.UP), false);
        scene.world().setBlock(blowerPos, CCBBlocks.CINDER_INCINERATION_BLOWER_BLOCK.getDefaultState(), false);
        scene.world().showSection(allSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().setKineticSpeed(allSelection, 32);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(20);
        scene.overlay().showText(60).text("Cinder Incineration Blower can damage and ignite nearby mobs when rotational force is applied").pointAt(Vec3.atCenterOf(blowerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showFilterSlotInput(scroll, Direction.UP, 110);
        AABB working_range = new AABB(Vec3.atCenterOf(blowerPos), Vec3.atCenterOf(blowerPos));
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, blowerPos, working_range, 1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, blowerPos, working_range.inflate(1), 29);

        scene.idle(10);
        scene.overlay().showText(60).colored(PonderPalette.OUTPUT).text("Use the value panel to configure its working range").pointAt(Vec3.atCenterOf(blowerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, blowerPos, working_range.inflate(2.5), 40);

        scene.idle(40);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, blowerPos, working_range.inflate(1.5), 40);

        scene.idle(60);
        scene.world().setKineticSpeed(allSelection, 64);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(10);
        scene.overlay().showText(60).colored(PonderPalette.MEDIUM).text("Higher rotation speed shortens damage and ignition intervals").pointAt(Vec3.atCenterOf(blowerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setKineticSpeed(allSelection, 256);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(10);
        scene.overlay().showText(60).colored(PonderPalette.FAST).text("At 256 RPM, it deals 2 damage and ignites for 2 seconds to mobs within range every half-second").pointAt(Vec3.atCenterOf(blowerPos)).placeNearTarget();

        scene.idle(80);
        scene.world().setKineticSpeed(allSelection, 16);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(10);
        scene.overlay().showText(60).colored(PonderPalette.RED).text("However, Cinder Incineration Blower stops working when rotation speed falls below 32 RPM").pointAt(Vec3.atCenterOf(blowerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void working_range(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("cinder_incineration_blower_working_range", "Display Working Range of Cinder Incineration Blowers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos blowerPos = util.grid().at(3, 1, 3);

        Selection blowerSelection = util.select().fromTo(blowerPos, blowerPos);

        ItemStack wrench = AllItems.WRENCH.asStack();
        ItemStack goggle = AllItems.GOGGLES.asStack();

        scene.world().setBlock(blowerPos, CCBBlocks.CINDER_INCINERATION_BLOWER_BLOCK.getDefaultState(), false);
        scene.world().showSection(blowerSelection, Direction.DOWN);

        scene.idle(10);
        scene.overlay().showText(60).text("While wearing the Engineer's Goggles, using a Wrench toggles the working range visualization").pointAt(Vec3.atCenterOf(blowerPos)).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().blockSurface(blowerPos.above(), Direction.UP), Pointing.DOWN, 40).withItem(goggle);
        scene.overlay().showControls(util.vector().blockSurface(blowerPos, Direction.NORTH), Pointing.RIGHT, 40).rightClick().withItem(wrench);

        scene.idle(7);
        AABB working_range = new AABB(Vec3.atCenterOf(blowerPos), Vec3.atCenterOf(blowerPos));
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, blowerPos, working_range, 1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, blowerPos, working_range.inflate(1.5), 59);

        scene.idle(53);
        scene.overlay().showControls(util.vector().blockSurface(blowerPos, Direction.NORTH), Pointing.RIGHT, 40).rightClick().withItem(wrench);

        scene.idle(60);
        scene.overlay().showText(60).colored(PonderPalette.RED).text("Only one Cinder Incineration Blower's working range can be displayed at the same time").pointAt(Vec3.atCenterOf(blowerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
