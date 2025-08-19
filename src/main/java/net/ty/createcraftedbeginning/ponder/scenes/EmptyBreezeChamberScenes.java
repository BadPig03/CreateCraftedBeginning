package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

public class EmptyBreezeChamberScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        RandomSource randomSource = RandomSource.create();

        scene.title("empty_breeze_chamber", "Using Empty Breeze Chambers");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos centerPos = util.grid().at(2, 0, 2);
        BlockPos spawnerPos = util.grid().at(2, 1, 2);
        BlockPos aboveTwoPos = util.grid().at(2, 2, 2);

        Selection spawnerSelection = util.select().fromTo(spawnerPos, spawnerPos);

        ItemStack emptyChamber = new ItemStack(CCBBlocks.EMPTY_BREEZE_CHAMBER_BLOCK.asItem());

        ParticleEmitter gust = scene.effects().simpleParticleEmitter(ParticleTypes.GUST, Vec3.ZERO);

        scene.world().setBlock(spawnerPos, Blocks.AIR.defaultBlockState(), false);

        scene.idle(10);
        ElementLink<EntityElement> breeze = scene.world().createEntity(w -> {
            Breeze breezeEntity = EntityType.BREEZE.create(w);
            if (breezeEntity != null) {
                Vec3 centerVector = util.vector().topOf(centerPos);
                breezeEntity.setPos(centerVector);
                breezeEntity.setPosRaw(centerVector.x, centerVector.y, centerVector.z);
                breezeEntity.setYRot(breezeEntity.yRotO = 180);
            }
            return breezeEntity;
        });

        scene.idle(20);
        scene.overlay().showText(60).text("Right-click a Breeze with the empty chamber to capture it").pointAt(util.vector().blockSurface(aboveTwoPos, Direction.WEST)).placeNearTarget().attachKeyFrame();

        scene.idle(40);
        scene.overlay().showControls(util.vector().centerOf(aboveTwoPos), Pointing.DOWN, 20).rightClick().withItem(emptyChamber);

        scene.idle(7);
        scene.world().modifyEntity(breeze, Entity::discard);
        scene.effects().emitParticles(util.vector().centerOf(spawnerPos), gust, 1, 1);

        scene.idle(33);
        scene.world().restoreBlocks(spawnerSelection);
        scene.world().showSection(spawnerSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("However, Breezes cannot be collected from the Trial Spawners directly").pointAt(util.vector().blockSurface(spawnerPos, Direction.WEST)).placeNearTarget().attachKeyFrame();

        scene.idle(10);
        scene.overlay().showControls(util.vector().topOf(spawnerPos), Pointing.DOWN, 40).rightClick().showing(AllIcons.I_MTD_CLOSE).withItem(emptyChamber);

        scene.idle(60);
        scene.markAsFinished();
    }
}
