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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.brasscrate.BrassCrateBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import java.util.ArrayList;
import java.util.List;

import static net.ty.createcraftedbeginning.util.Helpers.generateItemDropVelocity;

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

        Vec3 filter = util.vector().blockSurface(cratePos, Direction.UP).add(0, -0.125, 0);

        ItemStack enderPearl = new ItemStack(Items.ENDER_PEARL);
        ItemStack ironIngot = new ItemStack(Items.IRON_INGOT);
        ItemStack chuteItem = AllBlocks.CHUTE.asStack();
        ItemStack brassCrate = new ItemStack(CCBBlocks.BRASS_CRATE_BLOCK.value().asItem());

        List<ElementLink<EntityElement>> removes = new ArrayList<>();

        scene.world().setBlock(cratePos, CCBBlocks.BRASS_CRATE_BLOCK.getDefaultState(), false);
        ElementLink<WorldSectionElement> crate = scene.world().showIndependentSection(util.select().fromTo(cratePos, cratePos), Direction.DOWN);

        scene.idle(10);
        scene.overlay().showText(60)
            .text("Brass Crate has twice the storage capacity of Andesite Crate")
            .pointAt(Vec3.atCenterOf(cratePos))
            .placeNearTarget()
            .attachKeyFrame();

        scene.idle(80);
        scene.overlay().showFilterSlotInput(filter, Direction.UP, 70);

        scene.idle(10);
        scene.overlay().showText(60)
            .text("Items in the filter slot specify what to store")
            .pointAt(filter.add(0, 0.125, 0))
            .placeNearTarget()
            .attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(filter.add(0, 0.125, 0), Pointing.DOWN, 40).rightClick().withItem(enderPearl);

        scene.idle(7);
        scene.world().setFilterData(util.select().position(cratePos), BrassCrateBlockEntity.class, enderPearl);

        scene.idle(60);
        scene.world().setBlock(chutePos, AllBlocks.CHUTE.getDefaultState(), false);
        ElementLink<WorldSectionElement> chute = scene.world().showIndependentSection(util.select().fromTo(chutePos, chutePos), Direction.DOWN);

        scene.idle(10);
        scene.overlay().showText(60)
            .text("Thus preventing unnecessary items from entering")
            .pointAt(Vec3.atCenterOf(chutePos))
            .placeNearTarget();
        for (int i = 0; i < 5; i++)
        {
            scene.idle(10);
            ElementLink<EntityElement> remove = scene.world().createItemEntity(util.vector().centerOf(itemPos), util.vector().of(0, -0.1, 0), ironIngot);
            scene.idle(2);
            scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, ironIngot);
            scene.world().modifyEntity(remove, Entity::discard);
        }

        scene.idle(10);
        for (int i = 0; i < 10; i++) {
            scene.idle(2);
            scene.world().incrementBlockBreakingProgress(chutePos);
        }
        scene.world().hideIndependentSection(chute, Direction.UP);
        for (int i = 0; i < 5; i++) {
            ElementLink<EntityElement> remove = scene.world().createItemEntity(util.vector().centerOf(cratePos), generateItemDropVelocity(random), ironIngot);
            removes.add(remove);
        }
        removes.add(scene.world().createItemEntity(util.vector().centerOf(cratePos), generateItemDropVelocity(random), chuteItem));
        scene.overlay().showControls(util.vector().blockSurface(cratePos, Direction.NORTH), Pointing.RIGHT, 40).showing(AllIcons.I_MTD_CLOSE).withItem(ironIngot);

        scene.idle(45);
        scene.world().hideIndependentSection(crate, Direction.UP);

        scene.idle(5);
        for (ElementLink<EntityElement> remove : removes) {
            scene.world().modifyEntity(remove, Entity::discard);
        }

        scene.idle(10);
        scene.addLazyKeyframe();

        scene.idle(10);
        scene.world().setBlock(cratePos, CCBBlocks.BRASS_CRATE_BLOCK.getDefaultState(), false);
        scene.world().setFilterData(util.select().position(cratePos), BrassCrateBlockEntity.class, ItemStack.EMPTY);
        scene.world().showIndependentSection(util.select().fromTo(cratePos, cratePos), Direction.DOWN);
        scene.world().setBlock(chutePos, AllBlocks.CHUTE.getDefaultState(), false);
        ElementLink<WorldSectionElement> chute2 = scene.world().showIndependentSection(util.select().fromTo(chutePos, chutePos), Direction.DOWN);

        scene.idle(10);
        ElementLink<EntityElement> remove = scene.world().createItemEntity(util.vector().centerOf(itemPos), util.vector().of(0, -0.1, 0), ironIngot);

        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, ironIngot);
        scene.world().modifyEntity(remove, Entity::discard);

        scene.idle(10);
        scene.world().hideIndependentSection(chute2, Direction.UP);

        scene.idle(10);
        scene.overlay().showText(60)
            .text("Brass Crate drops itself and contents when broken")
            .pointAt(util.vector().centerOf(cratePos))
            .placeNearTarget();
        for (int i = 0; i < 10; i++) {
            scene.idle(2);
            scene.world().incrementBlockBreakingProgress(cratePos);
        }
        scene.world().createItemEntity(util.vector().centerOf(cratePos), generateItemDropVelocity(random), brassCrate);
            scene.world().createItemEntity(util.vector().centerOf(cratePos), generateItemDropVelocity(random), ironIngot);

        scene.idle(40);
        scene.markAsFinished();
    }
}
