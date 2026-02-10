package net.ty.createcraftedbeginning.ponder.scenes.breezes;

import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import org.jetbrains.annotations.NotNull;

public class BreezeChamberScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("breeze_chamber", "Feeding Breeze Chambers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(3, 1, 3);
        BlockPos chamberPos = tankPos.above();
        BlockPos deployerPos = chamberPos.east(2);
        BlockPos armPos = tankPos.north().west(2);
        BlockPos breadDepotPos = armPos.south(2);
        BlockPos pufferFishDepotPos = breadDepotPos.south().east(2);

        Selection chamberSelection = util.select().position(chamberPos);
        Selection tankSelection = util.select().position(tankPos);
        Selection deployerSelection = util.select().fromTo(deployerPos, deployerPos.below());
        Selection armSelection = util.select().position(armPos);
        Selection breadSelection = util.select().position(breadDepotPos);
        Selection pufferFishSelection = util.select().position(pufferFishDepotPos);

        AABB chamberArea = new AABB(util.vector().centerOf(chamberPos), util.vector().centerOf(chamberPos));

        Object chamberObject = new Object();

        ItemStack bread = new ItemStack(Items.BREAD);
        ItemStack pufferFish = new ItemStack(Items.PUFFERFISH);
        ItemStack beetrootSoup = new ItemStack(Items.BEETROOT_SOUP);

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(10);
        scene.world().showSection(chamberSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Breeze Chambers must be placed atop Airtight Tanks").pointAt(Vec3.atCenterOf(tankPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, chamberObject, chamberArea, 3);

        scene.idle(3);
        chamberArea = chamberArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, chamberObject, chamberArea, 3);

        scene.idle(3);
        chamberArea = chamberArea.expandTowards(0, -1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, chamberObject, chamberArea, 60);
        scene.overlay().showText(60).text("The Breeze Chamber energizes gases in Airtight Tanks below it").pointAt(Vec3.atCenterOf(chamberPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(chamberPos, Direction.UP).subtract(0, 0.125f, 0), Pointing.DOWN, 67).rightClick().withItem(bread);
        
        scene.idle(7);
        scene.world().modifyBlockEntity(chamberPos, BreezeChamberBlockEntity.class, BreezeChamberBlockEntity::SwitchToGaleState);
        scene.world().modifyBlock(chamberPos, s -> s.setValue(BreezeChamberBlock.WIND_LEVEL, WindLevel.GALE), false);
        scene.overlay().showText(60).text("Requires feeding food to the Breeze Chamber").pointAt(Vec3.atCenterOf(chamberPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(chamberPos, Direction.UP).subtract(0, 0.125f, 0), Pointing.DOWN, 67).rightClick().withItem(pufferFish);

        scene.idle(7);
        scene.world().modifyBlockEntity(chamberPos, BreezeChamberBlockEntity.class, BreezeChamberBlockEntity::SwitchToIllState);
        scene.world().modifyBlock(chamberPos, s -> s.setValue(BreezeChamberBlock.WIND_LEVEL, WindLevel.ILL), false);
		scene.overlay().showText(60).text("Improper foods reduce duration and may cause the Breeze ill").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(chamberPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().modifyBlockEntityNBT(deployerSelection, DeployerBlockEntity.class, compoundTag -> compoundTag.put("HeldItem", beetrootSoup.saveOptional(scene.world().getHolderLookupProvider())));
        scene.world().showSection(deployerSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(armSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(breadSelection, Direction.DOWN);
        scene.world().showSection(pufferFishSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Can be automated with Mechanical Arms or Deployers").pointAt(Vec3.atCenterOf(breadDepotPos)).attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
