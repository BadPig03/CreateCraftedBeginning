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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.ty.createcraftedbeginning.ponder.PonderHelpers.generateItemDropVelocity;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SturdyCrateScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        RandomSource random = RandomSource.create();

        scene.title("sturdy_crate", "Storing Items in Sturdy Crates");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos cratePos = util.grid().at(2, 1, 2);
        BlockPos chutePos = util.grid().at(2, 2, 2);
        BlockPos itemPos = util.grid().at(2, 3, 2);

        Selection crateSelection = util.select().position(cratePos);
        Selection chuteSelection = util.select().position(chutePos);

        Vec3 crateVec = util.vector().centerOf(cratePos);
        Vec3 itemVec = util.vector().centerOf(itemPos);
        Vec3 filterVec = util.vector().blockSurface(cratePos, Direction.UP).add(0, -0.0625, 0);
        Vec3 crateNorthVec = util.vector().blockSurface(cratePos, Direction.NORTH);
        Vec3 itemDropMotion = util.vector().of(0, -0.1, 0);

        ItemStack shulkerBoxItem = new ItemStack(Items.SHULKER_BOX);
        ItemStack diamondItem = new ItemStack(Items.DIAMOND);
        ItemStack toolboxItem = new ItemStack(AllBlocks.TOOLBOXES.get(DyeColor.BROWN));
        ItemStack sturdyCrateItem = new ItemStack(CCBBlocks.STURDY_CRATE_BLOCK.asItem());

        scene.idle(20);
        scene.world().setBlock(cratePos, CCBBlocks.STURDY_CRATE_BLOCK.getDefaultState(), false);
        scene.world().showIndependentSection(crateSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Sturdy Crate has four times the storage capacity of Andesite Crate").colored(PonderPalette.GREEN).pointAt(crateVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showFilterSlotInput(filterVec, Direction.UP, 60);
        scene.overlay().showText(60).text("Items in the filter slot specify what to store").colored(PonderPalette.GREEN).pointAt(filterVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Sturdy Crate can't store Shulker Boxes, Toolboxes, or other Sturdy Crates").colored(PonderPalette.RED).pointAt(filterVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.overlay().showControls(util.vector().blockSurface(cratePos, Direction.UP), Pointing.DOWN, 40).showing(AllIcons.I_MTD_CLOSE).withItem(shulkerBoxItem.copy());
        scene.overlay().showControls(crateNorthVec, Pointing.RIGHT, 40).showing(AllIcons.I_MTD_CLOSE).withItem(toolboxItem.copy());
        scene.overlay().showControls(util.vector().blockSurface(cratePos, Direction.DOWN).add(0, -0.5, 0), Pointing.UP, 40).showing(AllIcons.I_MTD_CLOSE).withItem(sturdyCrateItem.copy());

        scene.idle(80);
        scene.world().setBlock(chutePos, AllBlocks.CHUTE.getDefaultState(), false);
        scene.world().showSection(chuteSelection, Direction.DOWN);
        for (int i = 0; i < 4; i++) {
            scene.idle(10);
            ElementLink<EntityElement> remove = scene.world().createItemEntity(itemVec, itemDropMotion, diamondItem.copy());

            scene.idle(2);
            scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, diamondItem.copy());
            scene.world().modifyEntity(remove, Entity::discard);
        }

        scene.idle(20);
        scene.world().hideSection(chuteSelection, Direction.UP);

        scene.idle(20);
        scene.overlay().showText(60).text("Sturdy Crate drops itself with contents stored when broken").colored(PonderPalette.GREEN).pointAt(crateVec).placeNearTarget().attachKeyFrame();
        for (int i = 0; i < 10; i++) {
            scene.idle(3);
            scene.world().incrementBlockBreakingProgress(cratePos);
        }
        scene.world().createItemEntity(crateVec, generateItemDropVelocity(random), sturdyCrateItem.copy());
        scene.overlay().showControls(crateNorthVec, Pointing.RIGHT, 30).withItem(diamondItem.copy());

        scene.idle(30);
        scene.markAsFinished();
    }
}
