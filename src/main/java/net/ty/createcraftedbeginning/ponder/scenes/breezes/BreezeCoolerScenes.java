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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import org.jetbrains.annotations.NotNull;

public class BreezeCoolerScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("breeze_cooler", "Feeding Breeze Coolers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos coolerPos = util.grid().at(3, 1, 3);
        BlockPos deployerPos = coolerPos.east(2);
        BlockPos armPos = coolerPos.north().west(2);
        BlockPos depotPos = armPos.south(2);
        BlockPos pipePos = coolerPos.south();
        BlockPos pumpPos = pipePos.south();
        BlockPos tankPos = pumpPos.south();

        Selection coolerSelection = util.select().position(coolerPos);
        Selection deployerSelection = util.select().position(deployerPos);
        Selection armSelection = util.select().position(armPos);
        Selection depotSelection = util.select().position(depotPos);
        Selection pipeSelection = util.select().fromTo(pipePos, pumpPos);
        Selection tankSelection = util.select().fromTo(tankPos, tankPos.above());

        AABB coolerArea = new AABB(util.vector().centerOf(coolerPos), util.vector().centerOf(coolerPos));

        Object coolerObject = new Object();

        ItemStack iceBlock = new ItemStack(Blocks.ICE);

        scene.idle(20);
        scene.world().showSection(coolerSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, coolerObject, coolerArea, 3);

        scene.idle(3);
        coolerArea = coolerArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, coolerObject, coolerArea, 3);

        scene.idle(3);
        coolerArea = coolerArea.expandTowards(0, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, coolerObject, coolerArea, 60);
        scene.overlay().showText(60).text("The Breeze Cooler acts as a coolant source for blocks above it").pointAt(Vec3.atCenterOf(coolerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(coolerPos, Direction.UP).subtract(0, 0.125f, 0), Pointing.DOWN, 67).rightClick().withItem(iceBlock);
        
        scene.idle(7);
        scene.world().modifyBlockEntity(coolerPos, BreezeCoolerBlockEntity.class, BreezeCoolerBlockEntity::SwitchToChilledState);
        scene.world().modifyBlock(coolerPos, s -> s.setValue(BreezeCoolerBlock.FROST_LEVEL, FrostLevel.CHILLED), false);
		scene.overlay().showText(60).text("Requires feeding low-temperature items to the Breeze Cooler").pointAt(Vec3.atCenterOf(coolerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().modifyBlockEntityNBT(deployerSelection, DeployerBlockEntity.class, compoundTag -> compoundTag.put("HeldItem", iceBlock.saveOptional(scene.world().getHolderLookupProvider())));
        scene.world().showSection(deployerSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(armSelection, Direction.DOWN);
        scene.world().showSection(depotSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(pipeSelection, Direction.NORTH);
        scene.world().showSection(tankSelection, Direction.NORTH);

        scene.idle(20);
        scene.overlay().showText(60).text("Can be automated with Mechanical Arms, Deployers, or Mechanical Pumps").pointAt(Vec3.atCenterOf(depotPos)).attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
