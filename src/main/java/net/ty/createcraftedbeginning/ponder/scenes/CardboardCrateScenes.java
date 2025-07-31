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
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import static net.ty.createcraftedbeginning.util.Helpers.generateItemDropVelocity;

public class CardboardCrateScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        RandomSource random = RandomSource.create();

        scene.title("cardboard_crate", "Destroying Items with Cardboard Crates");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos cratePos = util.grid().at(2, 1, 2);
        BlockPos chutePos = util.grid().at(2, 2, 2);
        BlockPos itemPos = util.grid().at(2, 3, 2);

        ItemStack copperIngot = new ItemStack(Items.COPPER_INGOT);
        ItemStack ironIngot = new ItemStack(Items.IRON_INGOT);
        ItemStack goldIngot = new ItemStack(Items.GOLD_INGOT);
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        ItemStack minecart = new ItemStack(Items.MINECART);
        ItemStack cardboardCrate = new ItemStack(CCBBlocks.CARDBOARD_CRATE_BLOCK.asItem());

        scene.world().setBlock(cratePos, CCBBlocks.CARDBOARD_CRATE_BLOCK.getDefaultState(), false);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 2, 1, 2), Direction.DOWN);

        scene.idle(10);
        scene.overlay().showText(60)
            .text("Cardboard Crate temporarily stores only one stack of items")
            .pointAt(Vec3.atCenterOf(cratePos))
            .placeNearTarget()
            .attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(chutePos, AllBlocks.CHUTE.getDefaultState(), false);
        ElementLink<WorldSectionElement> chute = scene.world().showIndependentSection(util.select().fromTo(2, 2, 2, 2, 2, 2), Direction.DOWN);

        scene.idle(10);
        scene.overlay().showText(60)
                .text("Destroys existing items when new items are stored")
                .pointAt(Vec3.atCenterOf(cratePos))
                .placeNearTarget();

        scene.idle(10);
        ElementLink<EntityElement> remove = scene.world().createItemEntity(util.vector().centerOf(itemPos), util.vector().of(0, -0.1, 0), ironIngot);
        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, ironIngot);
        scene.world().modifyEntity(remove, Entity::discard);

        scene.idle(10);
        ElementLink<EntityElement> remove2 = scene.world().createItemEntity(util.vector().centerOf(itemPos), util.vector().of(0, -0.1, 0), copperIngot.copyWithCount(16));
        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, copperIngot);
        scene.world().modifyEntity(remove2, Entity::discard);

        scene.idle(10);
        ElementLink<EntityElement> remove3 = scene.world().createItemEntity(util.vector().centerOf(itemPos), util.vector().of(0, -0.1, 0), goldIngot.copyWithCount(32));
        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, goldIngot);
        scene.world().modifyEntity(remove3, Entity::discard);

        scene.idle(10);
        ElementLink<EntityElement> remove4 = scene.world().createItemEntity(util.vector().centerOf(itemPos), util.vector().of(0, -0.1, 0), diamond.copyWithCount(64));
        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, goldIngot);
        scene.world().modifyEntity(remove4, Entity::discard);

        scene.idle(10);
        ElementLink<EntityElement> remove5 = scene.world().createItemEntity(util.vector().centerOf(itemPos), util.vector().of(0, -0.1, 0), minecart);
        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, minecart);
        scene.world().modifyEntity(remove5, Entity::discard);

        scene.idle(10);
        scene.overlay().showControls(util.vector().blockSurface(cratePos, Direction.NORTH), Pointing.RIGHT, 40).withItem(minecart);

        scene.idle(60);
        scene.world().hideIndependentSection(chute, Direction.UP);

        scene.idle(10);
        scene.overlay().showText(60)
                .text("Cardboard Crate drops only itself when broken")
                .pointAt(Vec3.atCenterOf(cratePos))
                .placeNearTarget()
                .attachKeyFrame();
        for (int i = 0; i < 10; i++) {
            scene.idle(2);
            scene.world().incrementBlockBreakingProgress(cratePos);
        }
        scene.world().createItemEntity(util.vector().centerOf(cratePos), generateItemDropVelocity(random), cardboardCrate);

        scene.idle(40);
        scene.markAsFinished();
    }
}
