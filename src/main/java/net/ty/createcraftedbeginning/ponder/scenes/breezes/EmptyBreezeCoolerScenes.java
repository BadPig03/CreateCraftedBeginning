package net.ty.createcraftedbeginning.ponder.scenes.breezes;

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
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public class EmptyBreezeCoolerScenes {
    public static void using(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("empty_breeze_cooler_using", "Using Empty Breeze Coolers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos coolerPos = util.grid().at(3, 0, 3);
        BlockPos spawnerPos = coolerPos.above();
        BlockPos aboveTwoPos = spawnerPos.above();

        Selection spawnerSelection = util.select().position(spawnerPos);

        ItemStack emptyChamber = new ItemStack(CCBBlocks.EMPTY_BREEZE_COOLER_BLOCK);

        ParticleEmitter gust = scene.effects().simpleParticleEmitter(ParticleTypes.GUST, Vec3.ZERO);

        scene.idle(20);
        scene.world().setBlock(spawnerPos, Blocks.AIR.defaultBlockState(), false);
        ElementLink<EntityElement> breeze = scene.world().createEntity(w -> {
            Breeze breezeEntity = EntityType.BREEZE.create(w);
            if (breezeEntity != null) {
                Vec3 centerVector = util.vector().topOf(coolerPos);
                breezeEntity.setPos(centerVector);
                breezeEntity.setPosRaw(centerVector.x, centerVector.y, centerVector.z);
                breezeEntity.setYRot(breezeEntity.yRotO = 180);
            }
            return breezeEntity;
        });

        scene.idle(20);
		scene.overlay().showText(60).text("Right-click a Breeze with an Empty Breeze Cooler to capture it").pointAt(util.vector().blockSurface(aboveTwoPos, Direction.WEST)).placeNearTarget().attachKeyFrame();

		scene.idle(20);
        scene.overlay().showControls(util.vector().centerOf(aboveTwoPos), Pointing.DOWN, 40).rightClick().withItem(emptyChamber);

        scene.idle(7);
		scene.world().modifyEntity(breeze, Entity::discard);
        scene.effects().emitParticles(util.vector().centerOf(spawnerPos), gust, 1, 1);

        scene.idle(53);
        scene.world().restoreBlocks(spawnerSelection);
        scene.world().modifyBlockEntity(spawnerPos, TrialSpawnerBlockEntity.class, spawner -> spawner.setEntityId(EntityType.BREEZE, RandomSource.create()));
        scene.world().showSection(spawnerSelection, Direction.DOWN);

        scene.idle(20);
		scene.overlay().showControls(util.vector().topOf(spawnerPos), Pointing.DOWN, 67).rightClick().withItem(emptyChamber);

        scene.idle(7);
        scene.effects().emitParticles(util.vector().centerOf(spawnerPos), gust, 1, 1);
		scene.overlay().showText(60).text("Breezes can also be collected from Spawners and Trial Spawners directly").pointAt(util.vector().blockSurface(spawnerPos, Direction.WEST)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
