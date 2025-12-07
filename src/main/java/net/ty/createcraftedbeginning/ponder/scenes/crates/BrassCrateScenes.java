package net.ty.createcraftedbeginning.ponder.scenes.crates;

import com.simibubi.create.AllBlocks;
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
import net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

import static net.ty.createcraftedbeginning.ponder.PonderHelpers.generateItemDropVelocity;

public class BrassCrateScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        RandomSource random = RandomSource.create();

        scene.title("brass_crate", "Storing Items in Brass Crates");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos cratePos = util.grid().at(2, 1, 2);
        BlockPos chutePos = util.grid().at(2, 2, 2);
        BlockPos itemPos = util.grid().at(2, 3, 2);

        Vec3 filterVec = util.vector().blockSurface(cratePos, Direction.UP).add(0, -0.125, 0);

        ItemStack enderPearl = new ItemStack(Items.ENDER_PEARL);
        ItemStack ironIngot = new ItemStack(Items.IRON_INGOT);
        ItemStack chuteItem = AllBlocks.CHUTE.asStack();
        ItemStack brassCrate = new ItemStack(CCBBlocks.BRASS_CRATE_BLOCK.value().asItem());

        scene.idle(20);
        scene.world().setBlock(cratePos, CCBBlocks.BRASS_CRATE_BLOCK.getDefaultState(), false);
        ElementLink<WorldSectionElement> crate = scene.world().showIndependentSection(util.select().fromTo(cratePos, cratePos), Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Brass Crate has twice the storage capacity of Andesite Crate").pointAt(Vec3.atCenterOf(cratePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showFilterSlotInput(filterVec, Direction.UP, 70);

        scene.idle(10);
        scene.overlay().showText(60).text("Items in the filter slot specify what to store").pointAt(filterVec.add(0, 0.125, 0)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(filterVec.add(0, 0.125, 0), Pointing.DOWN, 40).rightClick().withItem(enderPearl);

        scene.idle(7);
        scene.world().setFilterData(util.select().position(cratePos), BrassCrateBlockEntity.class, enderPearl);

        scene.idle(60);
        scene.world().setBlock(chutePos, AllBlocks.CHUTE.getDefaultState(), false);
        ElementLink<WorldSectionElement> chute = scene.world().showIndependentSection(util.select().fromTo(chutePos, chutePos), Direction.DOWN);

        scene.idle(20);
        ElementLink<EntityElement> remove1 = scene.world().createItemEntity(util.vector().centerOf(itemPos), util.vector().of(0, -0.1, 0), ironIngot);

        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, ironIngot);
        scene.world().modifyEntity(remove1, Entity::discard);

        scene.idle(20);
        ElementLink<EntityElement> remove2 = scene.world().createItemEntity(util.vector().centerOf(itemPos), util.vector().of(0, -0.1, 0), ironIngot);

        scene.idle(20);
        for (int i = 0; i < 10; i++) {
            scene.idle(3);
            scene.world().incrementBlockBreakingProgress(chutePos);
        }
        scene.world().hideIndependentSection(chute, Direction.UP);
        ElementLink<EntityElement> remove3 = scene.world().createItemEntity(util.vector().centerOf(cratePos), generateItemDropVelocity(random), ironIngot);
        ElementLink<EntityElement> remove4 = scene.world().createItemEntity(util.vector().centerOf(cratePos), generateItemDropVelocity(random), chuteItem);
        scene.overlay().showControls(util.vector().blockSurface(cratePos, Direction.NORTH), Pointing.RIGHT, 60).showing(AllIcons.I_MTD_CLOSE).withItem(ironIngot);
        scene.overlay().showText(60).text("Thus preventing unnecessary items from entering").pointAt(Vec3.atCenterOf(cratePos)).attachKeyFrame().placeNearTarget();

        scene.idle(80);
        scene.world().hideIndependentSection(crate, Direction.UP);

        scene.idle(10);
        scene.world().modifyEntity(remove2, Entity::discard);
        scene.world().modifyEntity(remove3, Entity::discard);
        scene.world().modifyEntity(remove4, Entity::discard);

        scene.idle(10);
        scene.world().setBlock(cratePos, CCBBlocks.BRASS_CRATE_BLOCK.getDefaultState(), false);
        scene.world().setFilterData(util.select().position(cratePos), BrassCrateBlockEntity.class, ItemStack.EMPTY);
        scene.world().showIndependentSection(util.select().fromTo(cratePos, cratePos), Direction.DOWN);
        scene.world().setBlock(chutePos, AllBlocks.CHUTE.getDefaultState(), false);
        ElementLink<WorldSectionElement> chute2 = scene.world().showIndependentSection(util.select().fromTo(chutePos, chutePos), Direction.DOWN);

        scene.idle(20);
        ElementLink<EntityElement> remove5 = scene.world().createItemEntity(util.vector().centerOf(itemPos), util.vector().of(0, -0.1, 0), enderPearl);

        scene.idle(2);
        scene.world().createItemOnBeltLike(chutePos, Direction.DOWN, enderPearl);
        scene.world().modifyEntity(remove5, Entity::discard);

        scene.idle(20);
        scene.world().hideIndependentSection(chute2, Direction.UP);

        scene.idle(20);
        scene.overlay().showText(60).text("Brass Crate drops itself and contents when broken").colored(PonderPalette.OUTPUT).pointAt(util.vector().centerOf(cratePos)).placeNearTarget().attachKeyFrame();
        for (int i = 0; i < 10; i++) {
            scene.idle(3);
            scene.world().incrementBlockBreakingProgress(cratePos);
        }
        scene.world().createItemEntity(util.vector().centerOf(cratePos), generateItemDropVelocity(random), brassCrate);
        scene.world().createItemEntity(util.vector().centerOf(cratePos), generateItemDropVelocity(random), enderPearl);

        scene.idle(30);
        scene.markAsFinished();
    }
}
