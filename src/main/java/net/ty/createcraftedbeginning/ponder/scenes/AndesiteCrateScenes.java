package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import static net.ty.createcraftedbeginning.util.Helpers.generateItemDropVelocity;

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

        ItemStack diamondPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack andesiteCrate = new ItemStack(CCBBlocks.ANDESITE_CRATE_BLOCK.asItem());
        ItemStack brassHand = AllItems.BRASS_HAND.asStack();

        scene.world().setBlock(cratePos, CCBBlocks.ANDESITE_CRATE_BLOCK.getDefaultState(), false);
        scene.world().showSection(util.select().fromTo(cratePos, cratePos), Direction.DOWN);

        scene.idle(10);
        scene.overlay().showText(60).text("Andesite Crate can store up to 32 identical items").pointAt(Vec3.atCenterOf(cratePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(chutePos, AllBlocks.CHUTE.getDefaultState(), false);
        ElementLink<WorldSectionElement> chute = scene.world().showIndependentSection(util.select().fromTo(chutePos, chutePos), Direction.DOWN);

        scene.idle(10);
        scene.overlay().showText(60).text("Even if they are unstackable").pointAt(Vec3.atCenterOf(cratePos)).placeNearTarget();
        for (int i = 0; i < 5; i++) {
            scene.idle(10);
            ElementLink<EntityElement> remove = scene.world().createItemEntity(util.vector().centerOf(itemPos), util.vector().of(0, -0.1, 0), diamondPickaxe);

            scene.idle(2);
            scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, diamondPickaxe);
            scene.world().modifyEntity(remove, Entity::discard);
        }

        scene.idle(20);
        scene.overlay().showControls(util.vector().blockSurface(cratePos, Direction.NORTH), Pointing.RIGHT, 40).withItem(diamondPickaxe);

        scene.idle(60);
        scene.world().hideIndependentSection(chute, Direction.UP);

        scene.idle(20);
        scene.overlay().showControls(util.vector().blockSurface(cratePos, Direction.NORTH), Pointing.RIGHT, 40).showing(AllIcons.I_MTD_CLOSE).withItem(brassHand);
        scene.overlay().showText(60).text("However, contents cannot be added or taken manually").colored(PonderPalette.RED).placeNearTarget().pointAt(util.vector().blockSurface(cratePos, Direction.WEST)).attachKeyFrame();
        scene.addLazyKeyframe();

        scene.idle(80);
        scene.overlay().showText(60).text("Andesite Crate drops itself and contents when broken").pointAt(Vec3.atCenterOf(cratePos)).placeNearTarget().attachKeyFrame();
        for (int i = 0; i < 10; i++) {
            scene.idle(2);
            scene.world().incrementBlockBreakingProgress(cratePos);
        }
        scene.world().createItemEntity(util.vector().centerOf(cratePos), generateItemDropVelocity(random), andesiteCrate);
        for (int i = 0; i < 5; i++) {
            scene.world().createItemEntity(util.vector().centerOf(cratePos), generateItemDropVelocity(random), diamondPickaxe);
        }

        scene.idle(40);
        scene.markAsFinished();
    }
}
