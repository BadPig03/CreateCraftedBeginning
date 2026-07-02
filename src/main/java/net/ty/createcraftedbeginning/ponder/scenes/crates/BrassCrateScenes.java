package net.ty.createcraftedbeginning.ponder.scenes.crates;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.ty.createcraftedbeginning.ponder.PonderHelpers.generateItemDropVelocity;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BrassCrateScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        RandomSource random = RandomSource.create();

        scene.title("brass_crate", "Storing Items in Brass Crates");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos cratePos = util.grid().at(2, 1, 2);
        BlockPos chutePos = util.grid().at(2, 2, 2);
        BlockPos itemPos = util.grid().at(2, 3, 2);

        Selection crateSelection = util.select().position(cratePos);
        Selection chuteSelection = util.select().position(chutePos);

        Vec3 filterVec = util.vector().blockSurface(cratePos, Direction.UP).add(0, -0.0625, 0);
        Vec3 itemVec = util.vector().centerOf(itemPos);
        Vec3 crateVec = util.vector().centerOf(cratePos);
        Vec3 itemDropMotion = util.vector().of(0, -0.1, 0);

        ItemStack enderPearlItem = new ItemStack(Items.ENDER_PEARL);
        ItemStack ironIngotItem = new ItemStack(Items.IRON_INGOT);
        ItemStack chuteItem = new ItemStack(AllBlocks.CHUTE.asItem());
        ItemStack brassCrateItem = new ItemStack(CCBBlocks.BRASS_CRATE_BLOCK.asItem());

        scene.idle(20);
        scene.world().setBlock(cratePos, CCBBlocks.BRASS_CRATE_BLOCK.getDefaultState(), false);
        scene.world().showSection(crateSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Brass Crate has twice the storage capacity of Andesite Crate").colored(PonderPalette.GREEN).pointAt(crateVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showFilterSlotInput(filterVec, Direction.UP, 60);
        scene.overlay().showText(60).text("Items in the filter slot specify what to store").colored(PonderPalette.GREEN).pointAt(filterVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(filterVec, Pointing.DOWN, 40).rightClick().withItem(enderPearlItem);

        scene.idle(7);
        scene.world().setFilterData(crateSelection, BrassCrateBlockEntity.class, enderPearlItem);

        scene.idle(60);
        scene.world().setBlock(chutePos, AllBlocks.CHUTE.getDefaultState(), false);
        scene.world().showSection(chuteSelection, Direction.DOWN);

        scene.idle(20);
        ElementLink<EntityElement> remove = scene.world().createItemEntity(itemVec, itemDropMotion, ironIngotItem);

        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, ironIngotItem);
        scene.world().modifyEntity(remove, Entity::discard);

        scene.idle(20);
        ElementLink<EntityElement> remove2 = scene.world().createItemEntity(itemVec, itemDropMotion, ironIngotItem);

        scene.idle(20);
        for (int i = 0; i < 10; i++) {
            scene.idle(3);
            scene.world().incrementBlockBreakingProgress(chutePos);
        }
        scene.world().hideSection(chuteSelection, Direction.UP);
        ElementLink<EntityElement> remove3 = scene.world().createItemEntity(crateVec, generateItemDropVelocity(random), ironIngotItem);
        ElementLink<EntityElement> remove4 = scene.world().createItemEntity(crateVec, generateItemDropVelocity(random), chuteItem);
        scene.overlay().showControls(util.vector().blockSurface(cratePos, Direction.NORTH), Pointing.RIGHT, 60).showing(AllIcons.I_MTD_CLOSE).withItem(ironIngotItem);
        scene.overlay().showText(60).text("Thus preventing unnecessary items from entering").colored(PonderPalette.GREEN).pointAt(crateVec).attachKeyFrame().placeNearTarget();

        scene.idle(80);
        scene.world().hideSection(crateSelection, Direction.UP);

        scene.idle(10);
        scene.world().modifyEntity(remove2, Entity::discard);
        scene.world().modifyEntity(remove3, Entity::discard);
        scene.world().modifyEntity(remove4, Entity::discard);

        scene.idle(10);
        scene.world().setBlock(cratePos, CCBBlocks.BRASS_CRATE_BLOCK.getDefaultState(), false);
        scene.world().setFilterData(crateSelection, BrassCrateBlockEntity.class, ItemStack.EMPTY);
        scene.world().showSection(crateSelection, Direction.DOWN);
        scene.world().setBlock(chutePos, AllBlocks.CHUTE.getDefaultState(), false);
        scene.world().showSection(chuteSelection, Direction.DOWN);

        scene.idle(20);
        ElementLink<EntityElement> remove5 = scene.world().createItemEntity(itemVec, itemDropMotion, enderPearlItem);

        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, enderPearlItem);
        scene.world().modifyEntity(remove5, Entity::discard);

        scene.idle(20);
        scene.world().hideSection(chuteSelection, Direction.UP);

        scene.idle(20);
        scene.overlay().showText(60).text("Brass Crate drops itself and contents when broken").colored(PonderPalette.GREEN).pointAt(crateVec).placeNearTarget().attachKeyFrame();
        for (int i = 0; i < 10; i++) {
            scene.idle(3);
            scene.world().incrementBlockBreakingProgress(cratePos);
        }
        scene.world().createItemEntity(crateVec, generateItemDropVelocity(random), brassCrateItem);
        scene.world().createItemEntity(crateVec, generateItemDropVelocity(random), enderPearlItem);

        scene.idle(30);
        scene.markAsFinished();
    }
}
