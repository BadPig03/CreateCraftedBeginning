package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
import org.jetbrains.annotations.NotNull;

import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity.NOZZLE_IDLE_TIME;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity.NOZZLE_PART_TIME;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity.NOZZLE_TIME;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity.PROCESSING_TIME;

public class GasInjectionChamberScenes {
    public static void gasInjection(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        RandomSource random = RandomSource.create();

        scene.title("gas_injection_chamber_gas_injection", "Injecting Gases with Gas Injection Chambers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos chamberTargetPos = util.grid().at(3, 1, 3);
        BlockPos chamberPos = chamberTargetPos.above(2);
        BlockPos lowerPipePos = chamberPos.above();
        BlockPos pumpPos = lowerPipePos.above();
        BlockPos upperPipePos = pumpPos.above();
        BlockPos cogPos = pumpPos.south();
        BlockPos motorPos = cogPos.above();

        Selection chamberSelection = util.select().position(chamberPos);
        Selection pipeSelection = util.select().fromTo(upperPipePos, lowerPipePos);
        Selection sourceSelection = util.select().fromTo(cogPos, motorPos);
        Selection depotSelection = util.select().position(chamberTargetPos);

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        Object pipeObject = new Object();

        AABB pipeArea = new AABB(util.vector().centerOf(upperPipePos), util.vector().centerOf(upperPipePos));

        ItemStack blazePowderItem = new ItemStack(Items.BLAZE_POWDER);
        ItemStack windChargeItem = new ItemStack(Items.WIND_CHARGE);

        scene.idle(20);
        scene.world().showSection(depotSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(chamberSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("").pointAt(Vec3.atCenterOf(chamberPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(pipeSelection, Direction.DOWN);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(sourceSelection, Direction.DOWN);
        scene.world().setKineticSpeed(pipeSelection, mediumSpeed);
        scene.world().setKineticSpeed(sourceSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, pipeObject, pipeArea, 3);

        scene.idle(3);
        pipeArea = pipeArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, pipeObject, pipeArea, 3);

        scene.idle(3);
        pipeArea = pipeArea.expandTowards(0, -2, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, pipeObject, pipeArea, 60);
        scene.overlay().showText(60).text("").colored(PonderPalette.INPUT).pointAt(Vec3.atCenterOf(lowerPipePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("").pointAt(Vec3.atCenterOf(chamberPos)).placeNearTarget().attachKeyFrame();

        scene.idle(10);
        scene.world().createItemOnBeltLike(chamberTargetPos, Direction.NORTH, blazePowderItem);
        scene.overlay().showControls(util.vector().centerOf(chamberTargetPos), Pointing.UP, 30).withItem(blazePowderItem);

        scene.idle(30);
		scene.world().modifyBlockEntityNBT(chamberSelection, GasInjectionChamberBlockEntity.class, compoundTag -> compoundTag.putInt("ProcessingTicks", PROCESSING_TIME));

        scene.idle(PROCESSING_TIME - NOZZLE_TIME - NOZZLE_PART_TIME - NOZZLE_IDLE_TIME);
        scene.world().removeItemsFromBelt(chamberTargetPos);
        scene.world().createItemOnBeltLike(chamberTargetPos, Direction.UP, windChargeItem);
        Vec3 vec = VecHelper.getCenterOf(chamberPos).subtract(0, 1.6875f, 0);
        for (int i = 0; i < random.nextInt(3, 6); i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125f);
            scene.effects().emitParticles(vec, scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(offset.x, Math.abs(offset.y), offset.z)), 1, 1);
        }

        scene.idle(10);
        scene.overlay().showControls(util.vector().centerOf(chamberTargetPos), Pointing.UP, 30).withItem(windChargeItem);

        scene.idle(30);
        scene.markAsFinished();
    }
}
