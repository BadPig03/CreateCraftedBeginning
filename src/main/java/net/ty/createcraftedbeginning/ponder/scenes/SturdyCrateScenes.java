package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import static net.ty.createcraftedbeginning.util.Helpers.generateItemDropVelocity;

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

        Vec3 filter = util.vector().blockSurface(cratePos, Direction.UP).add(0, -0.125, 0);

        ItemStack shulkerBox = new ItemStack(Items.SHULKER_BOX);
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        ItemStack toolbox = AllBlocks.TOOLBOXES.get(DyeColor.BROWN).asStack();
        ItemStack sturdyCrate = new ItemStack(CCBBlocks.STURDY_CRATE_BLOCK.value().asItem());

        scene.world().setBlock(cratePos, CCBBlocks.STURDY_CRATE_BLOCK.getDefaultState(), false);
        scene.world().showIndependentSection(util.select().fromTo(cratePos, cratePos), Direction.DOWN);

        scene.idle(10);
        scene.overlay().showText(60).text("Sturdy Crate has four times the storage capacity of Andesite Crate").pointAt(Vec3.atCenterOf(cratePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showFilterSlotInput(filter, Direction.UP, 70);

        scene.idle(10);
        scene.overlay().showText(60).text("Items in the filter slot specify what to store").pointAt(filter.add(0, 0.125, 0)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Sturdy Crate can't store Shulker Boxes, Toolboxes, or other Sturdy Crates").pointAt(filter.add(0, 0.125, 0)).placeNearTarget().attachKeyFrame();

        scene.idle(10);
        scene.overlay().showControls(util.vector().blockSurface(cratePos, Direction.UP), Pointing.DOWN, 40).showing(AllIcons.I_MTD_CLOSE).withItem(shulkerBox);
        scene.overlay().showControls(util.vector().blockSurface(cratePos, Direction.NORTH), Pointing.RIGHT, 40).showing(AllIcons.I_MTD_CLOSE).withItem(toolbox);
        scene.overlay().showControls(util.vector().blockSurface(cratePos, Direction.DOWN), Pointing.UP, 40).showing(AllIcons.I_MTD_CLOSE).withItem(sturdyCrate);

        scene.idle(50);
        scene.addLazyKeyframe();

        scene.idle(20);
        scene.world().setBlock(chutePos, AllBlocks.CHUTE.getDefaultState(), false);
        ElementLink<WorldSectionElement> chute = scene.world().showIndependentSection(util.select().fromTo(chutePos, chutePos), Direction.DOWN);

        scene.idle(10);
        ElementLink<EntityElement> remove = scene.world().createItemEntity(util.vector().centerOf(itemPos), util.vector().of(0, -0.1, 0), diamond);

        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, diamond);
        scene.world().modifyEntity(remove, Entity::discard);

        scene.idle(10);
        scene.world().hideIndependentSection(chute, Direction.UP);

        scene.idle(10);
        scene.overlay().showText(60).text("Sturdy Crate drops itself with contents stored when broken").pointAt(util.vector().centerOf(cratePos)).placeNearTarget();
        for (int i = 0; i < 10; i++) {
            scene.idle(2);
            scene.world().incrementBlockBreakingProgress(cratePos);
        }
        scene.world().createItemEntity(util.vector().centerOf(cratePos), generateItemDropVelocity(random), sturdyCrate);
        scene.overlay().showControls(util.vector().blockSurface(cratePos, Direction.NORTH), Pointing.RIGHT, 40).withItem(diamond);

        scene.idle(40);
        scene.markAsFinished();
    }
}
