package net.ty.createcraftedbeginning.ponder.scenes.crates;

import com.simibubi.create.AllBlocks;
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

import static net.ty.createcraftedbeginning.ponder.CCBPonderScenes.generateItemDropVelocity;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CardboardCrateScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        RandomSource random = RandomSource.create();

        scene.title("cardboard_crate", "Disposing Items with Cardboard Crates");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos cratePos = util.grid().at(2, 1, 2);
        BlockPos chutePos = cratePos.above();
        BlockPos itemPos = cratePos.above(2);

        Selection chuteSelection = util.select().position(chutePos);
        Selection crateSelection = util.select().position(cratePos);

        Vec3 crateVec = util.vector().centerOf(cratePos);
        Vec3 itemVec = util.vector().centerOf(itemPos);
        Vec3 crateNorthVec = util.vector().blockSurface(cratePos, Direction.NORTH);
        Vec3 itemDropMotion = Vec3.ZERO.add(0, -0.1, 0);

        ItemStack coalItem = new ItemStack(Items.COAL);
        ItemStack copperIngotItem = new ItemStack(Items.COPPER_INGOT);
        ItemStack ironIngotItem = new ItemStack(Items.IRON_INGOT);
        ItemStack goldIngotItem = new ItemStack(Items.GOLD_INGOT);
        ItemStack diamondItem = new ItemStack(Items.DIAMOND);
        ItemStack minecartItem = new ItemStack(Items.MINECART);
        ItemStack cardboardCrateItem = new ItemStack(CCBBlocks.CARDBOARD_CRATE_BLOCK.asItem());

        scene.idle(20);
        scene.world().setBlock(cratePos, CCBBlocks.CARDBOARD_CRATE_BLOCK.getDefaultState(), false);
        scene.world().showSection(crateSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Cardboard Crate can only temporarily store a single type of item").colored(PonderPalette.RED).pointAt(crateVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.world().setBlock(chutePos, AllBlocks.CHUTE.getDefaultState(), false);
        scene.world().showSection(chuteSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).colored(PonderPalette.RED).text("When a different item is inserted, the previous contents are discarded").pointAt(crateVec).placeNearTarget().attachKeyFrame();

        scene.idle(10);
        ElementLink<EntityElement> coalEntity = scene.world().createItemEntity(itemVec, itemDropMotion, coalItem.copy());

        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, coalItem.copy());
        scene.world().modifyEntity(coalEntity, Entity::discard);

        scene.idle(10);
        ElementLink<EntityElement> ironEntity = scene.world().createItemEntity(itemVec, itemDropMotion, ironIngotItem.copy());

        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, ironIngotItem.copy());
        scene.world().modifyEntity(ironEntity, Entity::discard);

        scene.idle(10);
        ElementLink<EntityElement> copperEntity = scene.world().createItemEntity(itemVec, itemDropMotion, copperIngotItem.copyWithCount(16));

        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, copperIngotItem.copyWithCount(16));
        scene.world().modifyEntity(copperEntity, Entity::discard);

        scene.idle(10);
        ElementLink<EntityElement> goldEntity = scene.world().createItemEntity(itemVec, itemDropMotion, goldIngotItem.copyWithCount(32));

        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, goldIngotItem.copyWithCount(32));
        scene.world().modifyEntity(goldEntity, Entity::discard);

        scene.idle(10);
        ElementLink<EntityElement> diamondEntity = scene.world().createItemEntity(itemVec, itemDropMotion, diamondItem.copyWithCount(64));

        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, diamondItem.copyWithCount(64));
        scene.world().modifyEntity(diamondEntity, Entity::discard);

        scene.idle(20);
        scene.overlay().showText(60).colored(PonderPalette.RED).text("When storing identical items, any excess beyond the capacity limit will be disposed as well").pointAt(crateVec).placeNearTarget().attachKeyFrame();

        scene.idle(10);
        ElementLink<EntityElement> firstMinecartEntity = scene.world().createItemEntity(itemVec, itemDropMotion, minecartItem.copy());

        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, minecartItem.copy());
        scene.world().modifyEntity(firstMinecartEntity, Entity::discard);

        scene.idle(10);
        ElementLink<EntityElement> secondMinecartEntity = scene.world().createItemEntity(itemVec, itemDropMotion, minecartItem.copy());

        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, minecartItem.copy());
        scene.world().modifyEntity(secondMinecartEntity, Entity::discard);

        scene.idle(10);
        scene.overlay().showControls(crateNorthVec, Pointing.RIGHT, 40).withItem(minecartItem.copy());

        scene.idle(60);
        scene.world().hideSection(chuteSelection, Direction.UP);

        scene.idle(20);
        scene.overlay().showText(60).text("Cardboard Crate drops only itself when broken").colored(PonderPalette.RED).pointAt(crateVec).placeNearTarget().attachKeyFrame();
        for (int i = 0; i < 10; i++) {
            scene.idle(3);
            scene.world().incrementBlockBreakingProgress(cratePos);
        }
        scene.world().createItemEntity(crateVec, generateItemDropVelocity(random), cardboardCrateItem.copy());

        scene.idle(30);
        scene.markAsFinished();
    }
}
