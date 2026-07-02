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
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionChamberScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        RandomSource random = RandomSource.create();

        scene.title("gas_injection_chamber", "Injecting Gases with Gas Injection Chambers");
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

        Vec3 chamberVec = util.vector().centerOf(chamberPos);
        Vec3 lowerPipeVec = util.vector().centerOf(lowerPipePos);
        Vec3 chamberTargetVec = util.vector().centerOf(chamberTargetPos);
        Vec3 upperPipeVec = util.vector().centerOf(upperPipePos);
        Vec3 subtractedVec = chamberVec.subtract(0, 1.6875f, 0);

        AABB pipeArea = new AABB(upperPipeVec, upperPipeVec);

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        Object pipeObject = new Object();

        ItemStack blazePowderItem = new ItemStack(Items.BLAZE_POWDER);
        ItemStack windChargeItem = new ItemStack(Items.WIND_CHARGE);

        scene.idle(20);
        scene.world().showSection(depotSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(chamberSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Gas Injection Chambers inject gases into items on Depots or Belts below them").colored(PonderPalette.GREEN).pointAt(chamberVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(pipeSelection, Direction.DOWN);
        scene.world().showSection(sourceSelection, Direction.DOWN);

        scene.idle(15);
        scene.world().setKineticSpeed(pipeSelection, mediumSpeed);
        scene.world().setKineticSpeed(sourceSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, pipeObject, pipeArea, 3);

        scene.idle(3);
        pipeArea = pipeArea.inflate(0.375, 0.5, 0.375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, pipeObject, pipeArea, 3);

        scene.idle(3);
        pipeArea = pipeArea.expandTowards(0, -2, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, pipeObject, pipeArea, 60);
        scene.overlay().showText(60).text("Gas must be supplied to the Gas Injection Chamber from above").colored(PonderPalette.INPUT).pointAt(lowerPipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Processing automatically initiates when items accept the gas").colored(PonderPalette.GREEN).pointAt(chamberVec).placeNearTarget().attachKeyFrame();

        scene.idle(10);
        scene.world().createItemOnBeltLike(chamberTargetPos, Direction.NORTH, blazePowderItem.copy());
        scene.overlay().showControls(chamberTargetVec, Pointing.UP, 30).withItem(blazePowderItem.copy());

        scene.idle(30);
		scene.world().modifyBlockEntityNBT(chamberSelection, GasInjectionChamberBlockEntity.class, compoundTag -> compoundTag.putInt("ProcessingTicks", 60));

        scene.idle(25);
        scene.world().removeItemsFromBelt(chamberTargetPos);
        scene.world().createItemOnBeltLike(chamberTargetPos, Direction.UP, windChargeItem.copy());
        for (int i = 0; i < random.nextInt(3, 6); i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125f);
            scene.effects().emitParticles(subtractedVec, scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(offset.x, Math.abs(offset.y), offset.z)), 1, 1);
        }

        scene.idle(10);
        scene.overlay().showControls(chamberTargetVec, Pointing.UP, 30).withItem(windChargeItem.copy());

        scene.idle(30);
        scene.markAsFinished();
    }
}
