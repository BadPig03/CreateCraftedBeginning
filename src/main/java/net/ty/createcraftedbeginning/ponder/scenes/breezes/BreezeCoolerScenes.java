package net.ty.createcraftedbeginning.ponder.scenes.breezes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity.Phase;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.InactiveCoolerState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeCoolerScenes {
    public static void feeding(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("breeze_cooler_feeding", "Feeding Breeze Coolers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos coolerPos = util.grid().at(3, 1, 3);
        BlockPos deployerPos = coolerPos.east(2);
        BlockPos deployerMotorPos = deployerPos.north();
        BlockPos armPos = coolerPos.north().west(2);
        BlockPos armCogPos = armPos.north().west();
        BlockPos armMotorPos = armCogPos.above();
        BlockPos depotPos = armPos.south(2);
        BlockPos pipePos = coolerPos.south();
        BlockPos pumpPos = pipePos.south();
        BlockPos tankPos = pumpPos.south();
        BlockPos cogPos = pumpPos.east();
        BlockPos motorPos = cogPos.south();

        Selection coolerSelection = util.select().position(coolerPos);
        Selection deployerSelection = util.select().position(deployerPos);
        Selection deployerMotorSelection = util.select().position(deployerMotorPos);
        Selection armSelection = util.select().position(armPos);
        Selection armPowerSelection = util.select().fromTo(armCogPos, armMotorPos);
        Selection depotSelection = util.select().position(depotPos);
        Selection pipeSelection = util.select().fromTo(pipePos, pumpPos);
        Selection tankSelection = util.select().fromTo(tankPos, tankPos.above());
        Selection cogSelection = util.select().fromTo(cogPos, motorPos);
        Selection pumpSelection = util.select().position(pumpPos);

        Vec3 coolerVec = util.vector().centerOf(coolerPos);
        Vec3 depotVec = util.vector().centerOf(depotPos);
        Vec3 pumpVec = util.vector().centerOf(pumpPos);

        AABB coolerArea = new AABB(coolerVec, coolerVec);
        AABB fluidPathArea = new AABB(pumpVec, pumpVec);

        Object coolerObject = new Object();
        Object fluidPathObject = new Object();

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        ItemStack packedIceItem = new ItemStack(Blocks.PACKED_ICE);
        ItemStack iceItem = new ItemStack(Blocks.ICE);

        scene.idle(20);
        scene.world().showSection(coolerSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, coolerObject, coolerArea, 3);

        scene.idle(3);
        coolerArea = coolerArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, coolerObject, coolerArea, 60);
        scene.overlay().showText(60).text("A Riming Breeze Cooler cannot enable Chilled processing").colored(PonderPalette.RED).pointAt(coolerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Valid cooling items like Packed Ice add cooling time and switch the cooler to Chilled").colored(PonderPalette.GREEN).pointAt(coolerVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(coolerVec, Pointing.DOWN, 60).rightClick().withItem(packedIceItem.copy());

        scene.idle(7);
        setFrostLevel(scene, coolerPos, FrostLevel.CHILLED);

        scene.idle(73);
        scene.overlay().showText(60).text("When cooling time runs out, the cooler returns to Riming").colored(PonderPalette.RED).pointAt(coolerVec).placeNearTarget().attachKeyFrame();

        scene.idle(40);
        setFrostLevel(scene, coolerPos, FrostLevel.RIMING);

        scene.idle(40);
        scene.world().setBlock(armMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().removeItemsFromBelt(depotPos);
        scene.world().createItemOnBeltLike(depotPos, Direction.UP, packedIceItem.copy());
        scene.world().showSection(armSelection, Direction.DOWN);
        scene.world().showSection(armPowerSelection, Direction.DOWN);
        scene.world().showSection(depotSelection, Direction.DOWN);

        scene.idle(15);
        scene.world().setKineticSpeed(armPowerSelection, -mediumSpeed / 2);
        scene.world().setKineticSpeed(armSelection, mediumSpeed);
        scene.effects().rotationSpeedIndicator(armPos);
        scene.world().instructArm(armPos, Phase.MOVE_TO_INPUT, ItemStack.EMPTY, 0);
        scene.overlay().showText(60).text("Mechanical Arms and Deployers can automatically supply valid cooling items").colored(PonderPalette.BLUE).pointAt(depotVec).attachKeyFrame();

        scene.idle(40);
        scene.world().removeItemsFromBelt(depotPos);
        scene.world().instructArm(armPos, Phase.MOVE_TO_OUTPUT, packedIceItem.copy(), 0);

        scene.idle(40);
        setFrostLevel(scene, coolerPos, FrostLevel.CHILLED);
        scene.effects().indicateSuccess(coolerPos);
        scene.world().instructArm(armPos, Phase.SEARCH_INPUTS, ItemStack.EMPTY, -1);

        scene.idle(40);
        scene.world().modifyBlockEntityNBT(deployerSelection, DeployerBlockEntity.class, compoundTag -> compoundTag.put("HeldItem", iceItem.copy().saveOptional(scene.world().getHolderLookupProvider())));
        scene.world().setBlock(deployerMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);
        scene.world().showSection(deployerSelection, Direction.WEST);
        scene.world().showSection(deployerMotorSelection, Direction.WEST);

        scene.idle(15);
        scene.world().setKineticSpeed(deployerMotorSelection, -mediumSpeed);
        scene.world().setKineticSpeed(deployerSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(deployerPos);
        scene.world().moveDeployer(deployerPos, 1, 20);

        scene.idle(21);
        scene.world().modifyBlockEntityNBT(deployerSelection, DeployerBlockEntity.class, compoundTag -> compoundTag.put("HeldItem", ItemStack.EMPTY.saveOptional(scene.world().getHolderLookupProvider())));
        scene.effects().indicateSuccess(coolerPos);
        scene.world().moveDeployer(deployerPos, -1, 20);

        scene.idle(21);
        scene.world().hideSection(deployerSelection, Direction.EAST);
        scene.world().hideSection(deployerMotorSelection, Direction.EAST);
        scene.world().hideSection(armSelection, Direction.WEST);
        scene.world().hideSection(armPowerSelection, Direction.WEST);
        scene.world().hideSection(depotSelection, Direction.WEST);

        scene.idle(20);
        scene.world().showSection(tankSelection, Direction.NORTH);

        scene.idle(3);
        scene.world().showSection(pipeSelection, Direction.NORTH);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.NORTH), false);
        scene.world().showSection(cogSelection, Direction.WEST);

        scene.idle(15);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, fluidPathObject, fluidPathArea, 3);

        scene.idle(3);
        fluidPathArea = fluidPathArea.inflate(0.375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, fluidPathObject, fluidPathArea, 3);

        scene.idle(3);
        fluidPathArea = fluidPathArea.expandTowards(0, 0, -2);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, fluidPathObject, fluidPathArea, 60);
        scene.overlay().showText(60).text("Mechanical Pumps can automatically supply valid coolant fluids through Fluid Pipes").colored(PonderPalette.BLUE).pointAt(pumpVec).placeNearTarget().attachKeyFrame();
        scene.world().setKineticSpeed(cogSelection, mediumSpeed);
        scene.world().setKineticSpeed(pumpSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.world().propagatePipeChange(pumpPos);

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void basin(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("breeze_cooler_basin", "Using Breeze Coolers with Basins");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos coolerPos = util.grid().at(3, 1, 3);
        BlockPos basinPos = coolerPos.above();
        BlockPos machinePos = coolerPos.above(3);
        BlockPos cogPos = machinePos.east();
        BlockPos motorPos = cogPos.above();

        Selection coolerSelection = util.select().position(coolerPos);
        Selection basinSelection = util.select().position(basinPos);
        Selection machineSelection = util.select().position(machinePos);
        Selection cogSelection = util.select().fromTo(cogPos, motorPos);

        Vec3 coolerVec = util.vector().centerOf(coolerPos);
        Vec3 basinVec = util.vector().centerOf(basinPos);
        Vec3 basinTop = util.vector().topOf(basinPos);

        AABB sourceArea = new AABB(coolerVec, coolerVec);

        Object sourceObject = new Object();

        float fastSpeed = SpeedLevel.FAST.getSpeedValue();

        ItemStack packedIceItem = new ItemStack(Blocks.PACKED_ICE);
        ItemStack waterBucketItem = new ItemStack(Items.WATER_BUCKET);
        ItemStack iceItem = new ItemStack(Blocks.ICE);

        scene.idle(20);
        scene.world().showSection(coolerSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(basinSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, sourceObject, sourceArea, 3);

        scene.idle(3);
        sourceArea = sourceArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, sourceObject, sourceArea, 3);

        scene.idle(3);
        sourceArea = sourceArea.expandTowards(0, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, sourceObject, sourceArea, 60);
        scene.overlay().showText(60).text("Chilled Basin recipes require a Breeze Cooler directly below the Basin").colored(PonderPalette.RED).pointAt(basinVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("A Riming cooler does not satisfy those recipes").colored(PonderPalette.RED).pointAt(coolerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("A Basin above a Chilled Breeze Cooler can run Chilled Mixing and Chilled Compacting recipes").colored(PonderPalette.GREEN).pointAt(basinVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(coolerVec, Pointing.RIGHT, 60).rightClick().withItem(packedIceItem.copy());

        scene.idle(7);
        setFrostLevel(scene, coolerPos, FrostLevel.CHILLED);

        scene.idle(73);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(machineSelection, Direction.DOWN);

        scene.idle(15);
        scene.world().showSection(cogSelection, Direction.DOWN);
        scene.world().setKineticSpeed(cogSelection, fastSpeed);
        scene.world().setKineticSpeed(machineSelection, -fastSpeed);
        scene.effects().rotationSpeedIndicator(machinePos);

        scene.idle(20);
        scene.overlay().showText(60).text("For example, Chilled Mixing can turn water into Ice").pointAt(basinVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(basinTop, Pointing.RIGHT, 60).withItem(waterBucketItem.copy());

        scene.idle(7);
        scene.world().modifyBlockEntity(basinPos, BasinBlockEntity.class, basin -> basin.inputTank.getCapability().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
        scene.world().modifyBlockEntity(machinePos, MechanicalMixerBlockEntity.class, MechanicalMixerBlockEntity::startProcessingBasin);

        scene.idle(45);
        scene.world().modifyBlockEntity(basinPos, BasinBlockEntity.class, basin -> basin.inputTank.getCapability().drain(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
        scene.world().createItemOnBeltLike(basinPos, Direction.UP, iceItem.copy());

        scene.idle(45);
        scene.world().removeItemsFromBelt(basinPos);
        scene.world().hideSection(cogSelection, Direction.UP);
        scene.world().hideSection(machineSelection, Direction.UP);

        scene.idle(20);
        scene.world().setKineticSpeed(cogSelection, 0);
        scene.world().setKineticSpeed(machineSelection, 0);
        scene.world().setBlock(machinePos, AllBlocks.MECHANICAL_PRESS.getDefaultState(), false);
        scene.world().showSection(machineSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("A Mechanical Press over the same chilled Basin can run Chilled Compacting recipes").colored(PonderPalette.GREEN).pointAt(basinVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void bulkChilling(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("breeze_cooler_bulk_chilling", "Bulk Chilling with Breeze Coolers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos coolerPos = util.grid().at(3, 1, 3);
        BlockPos fanPos = coolerPos.west(2);
        BlockPos motorPos = fanPos.west();
        BlockPos depotPos = coolerPos.east(2);

        Selection coolerSelection = util.select().position(coolerPos);
        Selection fanSelection = util.select().position(fanPos);
        Selection motorSelection = util.select().position(motorPos);
        Selection depotSelection = util.select().position(depotPos);
        Selection kineticSelection = util.select().fromTo(motorPos, fanPos);

        Vec3 coolerVec = util.vector().centerOf(coolerPos);
        Vec3 depotVec = util.vector().centerOf(depotPos);

        AABB airCurrentArea = new AABB(coolerVec, coolerVec);

        Object airCurrentObject = new Object();

        ItemStack magmaItem = new ItemStack(Items.MAGMA_BLOCK);
        ItemStack obsidianItem = new ItemStack(Items.OBSIDIAN);
        ItemStack packedIceItem = new ItemStack(Blocks.PACKED_ICE);

        float fastSpeed = SpeedLevel.FAST.getSpeedValue();

        scene.idle(20);
        scene.world().createItemOnBeltLike(depotPos, Direction.UP, magmaItem.copy());
        scene.world().showSection(depotSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(coolerSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(fanSelection, Direction.EAST);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), false);
        scene.world().showSection(motorSelection, Direction.EAST);

        scene.idle(15);
        scene.world().setKineticSpeed(kineticSelection, fastSpeed);
        scene.effects().rotationSpeedIndicator(fanPos);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, airCurrentObject, airCurrentArea, 3);

        scene.idle(3);
        airCurrentArea = airCurrentArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, airCurrentObject, airCurrentArea, 3);

        scene.idle(3);
        airCurrentArea = airCurrentArea.expandTowards(-1, 0, 0).expandTowards(2, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, airCurrentObject, airCurrentArea, 60);
        scene.overlay().showText(60).text("An Encased Fan performs Bulk Chilling only when airflow passes through a Chilled Breeze Cooler").colored(PonderPalette.RED).pointAt(coolerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("A Riming Breeze Cooler does not create a chilling air current").colored(PonderPalette.RED).pointAt(coolerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Items in the chilled air current can use Bulk Chilling recipes").colored(PonderPalette.GREEN).pointAt(depotVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(coolerVec, Pointing.UP, 60).rightClick().withItem(packedIceItem.copy());

        scene.idle(7);
        setFrostLevel(scene, coolerPos, FrostLevel.CHILLED);

        scene.idle(12);
        scene.effects().emitParticles(depotVec, scene.effects().simpleParticleEmitter(ParticleTypes.SNOWFLAKE, Vec3.ZERO), 0.5F, 60);

        scene.idle(35);
        scene.world().removeItemsFromBelt(depotPos);
        scene.world().createItemOnBeltLike(depotPos, Direction.UP, obsidianItem.copy());

        scene.idle(6);
        scene.markAsFinished();
    }

    private static void setFrostLevel(CreateSceneBuilder scene, BlockPos coolerPos, FrostLevel frostLevel) {
        scene.world().modifyBlock(coolerPos, state -> state.setValue(BreezeCoolerBlock.FROST_LEVEL, frostLevel), false);
        scene.world().modifyBlockEntity(coolerPos, BreezeCoolerBlockEntity.class, cooler -> {
            switch (frostLevel) {
                case RIMING -> cooler.setCoolerState(new InactiveCoolerState());
                case CHILLED -> cooler.switchToChilledState();
            }
        });
    }
}