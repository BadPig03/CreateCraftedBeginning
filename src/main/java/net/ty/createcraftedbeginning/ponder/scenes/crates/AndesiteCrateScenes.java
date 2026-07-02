package net.ty.createcraftedbeginning.ponder.scenes.crates;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
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
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.ty.createcraftedbeginning.ponder.PonderHelpers.generateItemDropVelocity;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AndesiteCrateScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        RandomSource random = RandomSource.create();

        scene.title("andesite_crate", "Storing Items in Andesite Crates");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos cratePos = util.grid().at(2, 1, 2);
        BlockPos chutePos = util.grid().at(2, 2, 2);
        BlockPos itemPos = util.grid().at(2, 3, 2);

        Selection crateSelection = util.select().position(cratePos);
        Selection chuteSelection = util.select().position(chutePos);

        Vec3 crateVec = util.vector().centerOf(cratePos);
        Vec3 itemVec = util.vector().centerOf(itemPos);
        Vec3 crateNorthVec = util.vector().blockSurface(cratePos, Direction.NORTH);
        Vec3 itemDropMotion = util.vector().of(0, -0.1, 0);

        ItemStack diamondPickaxeItem = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack andesiteCrateItem = new ItemStack(CCBBlocks.ANDESITE_CRATE_BLOCK.asItem());
        ItemStack brassHandItem = new ItemStack(AllItems.BRASS_HAND.asItem());

        scene.idle(20);
        scene.world().setBlock(cratePos, CCBBlocks.ANDESITE_CRATE_BLOCK.getDefaultState(), false);
        scene.world().showSection(crateSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Andesite Crate can store a large amount of of identical items").pointAt(crateVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(chutePos, AllBlocks.CHUTE.getDefaultState(), false);
        scene.world().showSection(chuteSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Even if they are unstackable").colored(PonderPalette.GREEN).pointAt(crateVec).placeNearTarget().attachKeyFrame();
        for (int i = 0; i < 5; i++) {
            scene.idle(10);
            ElementLink<EntityElement> remove = scene.world().createItemEntity(itemVec, itemDropMotion, diamondPickaxeItem.copy());

            scene.idle(2);
            scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, diamondPickaxeItem.copy());
            scene.world().modifyEntity(remove, Entity::discard);
        }

        scene.idle(20);
        scene.overlay().showControls(crateNorthVec, Pointing.RIGHT, 40).withItem(diamondPickaxeItem.copy());

        scene.idle(40);
        scene.world().hideSection(chuteSelection, Direction.UP);

        scene.idle(20);
        scene.overlay().showControls(crateNorthVec, Pointing.RIGHT, 60).showing(AllIcons.I_MTD_CLOSE).withItem(brassHandItem);
        scene.overlay().showText(60).text("However, contents cannot be added or taken manually").colored(PonderPalette.RED).placeNearTarget().pointAt(util.vector().blockSurface(cratePos, Direction.WEST)).attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Andesite Crate drops itself and contents when broken").colored(PonderPalette.GREEN).pointAt(crateVec).placeNearTarget().attachKeyFrame();
        for (int i = 0; i < 10; i++) {
            scene.idle(3);
            scene.world().incrementBlockBreakingProgress(cratePos);
        }
        scene.world().createItemEntity(crateVec, generateItemDropVelocity(random), andesiteCrateItem.copy());
        for (int i = 0; i < 5; i++) {
            scene.world().createItemEntity(crateVec, generateItemDropVelocity(random), diamondPickaxeItem.copy());
        }

        scene.idle(30);
        scene.markAsFinished();
    }
}
