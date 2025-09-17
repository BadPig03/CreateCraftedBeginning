package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.foundation.ponder.element.BeltItemElement;
import net.createmod.catnip.math.Pointing;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airtightintakeport.AirtightIntakePortBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity.NOZZLE_IDLE_TIME;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity.NOZZLE_PART_TIME;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity.NOZZLE_TIME;
import static net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity.PROCESSING_TIME;

public class GasInjectionChamberScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        RandomSource random = RandomSource.create();

        scene.title("gas_injection_chamber", "Injecting Gas into Items using a Gas Injection Chamber");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos depotPos = util.grid().at(2, 1, 1);
        BlockPos chamberPos = util.grid().at(2, 3, 2);
        BlockPos pipePos = util.grid().at(2, 4, 2);
        BlockPos pumpPos = util.grid().at(2, 5, 2);
        BlockPos portPos = util.grid().at(2, 6, 2);
        BlockPos cogPos = util.grid().at(1, 5, 2);
        BlockPos motorPos = util.grid().at(1, 6, 2);
        BlockPos beltPos = util.grid().at(0, 1, 2);
        BlockPos newPos = util.grid().at(2, 1, 2);

        Vec3 depotVec = util.vector().centerOf(depotPos.east());

        Selection depotSelection = util.select().fromTo(depotPos, depotPos);
        Selection chamberSelection = util.select().fromTo(chamberPos, chamberPos);
        Selection pipesSelection = util.select().fromTo(pipePos, portPos);
        Selection shaftSelection = util.select().fromTo(cogPos, motorPos);
        Selection beltSelection = util.select().fromTo(0, 1, 2, 4, 1, 2);

        ItemStack blazePowderItem = new ItemStack(Items.BLAZE_POWDER);
        ItemStack windChargeItem = new ItemStack(Items.WIND_CHARGE, 2);
        ItemStack blazeRodItem = new ItemStack(Items.BLAZE_ROD);
        ItemStack breezeRodItem = new ItemStack(Items.BREEZE_ROD);

        scene.world().setBlock(depotPos, AllBlocks.DEPOT.getDefaultState(), false);
        scene.world().setBlock(chamberPos, CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK.getDefaultState(), false);
        scene.world().setBlock(pipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(AirtightPipeBlock.AXIS, Direction.Axis.Y), false);
        scene.world().setBlock(pumpPos, CCBBlocks.AIRTIGHT_PUMP_BLOCK.getDefaultState().setValue(AirtightPumpBlock.FACING, Direction.DOWN), false);
        scene.world().setBlock(portPos, CCBBlocks.AIRTIGHT_INTAKE_PORT_BLOCK.getDefaultState().setValue(AirtightIntakePortBlock.FACING, Direction.UP), false);
        scene.world().setBlock(cogPos, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.Y), false);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);

        ElementLink<WorldSectionElement> depot = scene.world().showIndependentSection(depotSelection, Direction.DOWN);
        scene.world().moveSection(depot, util.vector().of(0, 0, 1), 0);
        scene.world().showSection(chamberSelection, Direction.DOWN);

        scene.idle(10);
        scene.overlay().showText(60).text("The Gas Injection Chamber can inject gas into items directly beneath it").pointAt(Vec3.atCenterOf(chamberPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(pipesSelection.add(shaftSelection), Direction.DOWN);

        scene.idle(10);
        scene.world().setKineticSpeed(pipesSelection, 64);
        scene.world().setKineticSpeed(shaftSelection, -64);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.overlay().showOutline(PonderPalette.INPUT, new Object(), pipesSelection, 60);
        scene.overlay().showText(60).text("Pipes are the only way to supply it with gas").colored(PonderPalette.INPUT).pointAt(Vec3.atCenterOf(pipePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("The input items can be placed on a Depot under the Gas Injection Chamber").pointAt(Vec3.atCenterOf(newPos)).placeNearTarget().attachKeyFrame();

        scene.idle(50);
        scene.world().createItemOnBeltLike(depotPos, Direction.NORTH, blazePowderItem);
        scene.overlay().showControls(depotVec, Pointing.RIGHT, 30).withItem(blazePowderItem);

        scene.idle(30);
        scene.world().modifyBlockEntityNBT(chamberSelection, GasInjectionChamberBlockEntity.class, nbt -> nbt.putInt("ProcessingTicks", PROCESSING_TIME));

        scene.idle(PROCESSING_TIME - NOZZLE_TIME - NOZZLE_PART_TIME - NOZZLE_IDLE_TIME);
        scene.world().removeItemsFromBelt(depotPos);
        scene.world().createItemOnBeltLike(depotPos, Direction.UP, windChargeItem);
        Vec3 vec = VecHelper.getCenterOf(chamberPos).subtract(0, 2 - 5 / 16f, 0);
        for (int i = 0; i < random.nextInt(3, 6); i++) {
            Vec3 m = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125f);
            scene.effects().emitParticles(vec, scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(m.x, Math.abs(m.y), m.z)), 1, 1);
        }

        scene.idle(20);
        scene.overlay().showControls(depotVec, Pointing.RIGHT, 50).withItem(windChargeItem);

        scene.idle(70);
        scene.world().removeItemsFromBelt(depotPos);
        scene.world().hideIndependentSection(depot, Direction.NORTH);

        scene.idle(20);
        scene.world().modifyBlocks(beltSelection, s -> s.setValue(BeltBlock.CASING, false), false);
        scene.world().modifyBlockEntityNBT(beltSelection, BeltBlockEntity.class, nbt -> NBTHelper.writeEnum(nbt, "Casing", BeltBlockEntity.CasingType.NONE));
        scene.world().showSection(beltSelection, Direction.SOUTH);
        scene.world().showSection(util.select().fromTo(0, 1, 1, 0, 1, 1), Direction.SOUTH);

        scene.idle(20);
        ElementLink<BeltItemElement> blazeRod = scene.world().createItemOnBelt(beltPos, Direction.DOWN, blazeRodItem);
        scene.overlay().showText(60).text("Items on belts are also processed automatically").pointAt(Vec3.atCenterOf(beltPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.world().stallBeltItem(blazeRod, true);
        scene.world().modifyBlockEntityNBT(chamberSelection, GasInjectionChamberBlockEntity.class, nbt -> nbt.putInt("ProcessingTicks", PROCESSING_TIME));

        scene.idle(PROCESSING_TIME - NOZZLE_TIME - NOZZLE_PART_TIME - NOZZLE_IDLE_TIME);
        scene.world().removeItemsFromBelt(newPos);
        ElementLink<BeltItemElement> breezeRod = scene.world().createItemOnBelt(newPos, Direction.DOWN, breezeRodItem);
        scene.world().stallBeltItem(breezeRod, true);
        Vec3 vec2 = VecHelper.getCenterOf(chamberPos).subtract(0, 2 - 5 / 16f, 0);
        for (int i = 0; i < random.nextInt(3, 6); i++) {
            Vec3 m = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125f);
            scene.effects().emitParticles(vec2, scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(m.x, Math.abs(m.y), m.z)), 1, 1);
        }

        scene.idle(20);
        scene.world().stallBeltItem(breezeRod, false);

        scene.idle(40);
        scene.markAsFinished();
    }
}
