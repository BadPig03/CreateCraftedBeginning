package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonFactory;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasTransferPlan;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasTransferPlan.ExecutionResult;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasTransferPlan.TransferPolicy;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPackingLimits;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPressureSemantics;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlock;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasunpackager.GasUnpackagerBlock;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasunpackager.GasUnpackagerBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasPackagerScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("gas_packager", "Packaging and Unpacking Gas");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos centerPos = util.grid().at(3, 1, 3);
        BlockPos sourceTankPos = centerPos.east(2).south();
        BlockPos sourcePackagerPos = sourceTankPos.above().north();
        BlockPos leverPos = sourcePackagerPos.above();
        BlockPos sourceFunnelPos = sourcePackagerPos.north();
        BlockPos sourceBeltPos = sourceFunnelPos.below();
        BlockPos targetPackagerPos = centerPos.west(2).above();
        BlockPos targetFunnelPos = targetPackagerPos.north();
        BlockPos targetBeltPos = targetFunnelPos.below();
        BlockPos targetTankPos = targetPackagerPos.below().south();
        BlockPos motorPos = sourceBeltPos.east().north();

        Selection sourceTankSelection = util.select().fromTo(sourceTankPos, sourceTankPos.above());
        Selection sourcePackagerSelection = util.select().fromTo(sourcePackagerPos, sourcePackagerPos.below());
        Selection leverSelection = util.select().position(leverPos);
        Selection sourceFunnelSelection = util.select().position(sourceFunnelPos);
        Selection beltSelection = util.select().fromTo(sourceBeltPos.east(), targetBeltPos.west());
        Selection targetFunnelSelection = util.select().position(targetFunnelPos);
        Selection targetPackagerSelection = util.select().fromTo(targetPackagerPos, targetPackagerPos.below());
        Selection targetTankSelection = util.select().fromTo(targetTankPos, targetTankPos.above());
        Selection motorSelection = util.select().position(motorPos);

        Vec3 sourcePackagerVec = util.vector().centerOf(sourcePackagerPos);
        Vec3 sourceBackVec = util.vector().blockSurface(sourcePackagerPos, Direction.SOUTH);
        Vec3 sourceFrontVec = util.vector().blockSurface(sourcePackagerPos, Direction.NORTH);
        Vec3 leverVec = util.vector().centerOf(leverPos);
        Vec3 targetPackagerVec = util.vector().centerOf(targetPackagerPos);
        Vec3 targetFrontVec = util.vector().blockSurface(targetPackagerPos, Direction.NORTH);
        Vec3 beltVec = util.vector().topOf(centerPos.north());
        Vec3 targetTankVec = util.vector().centerOf(targetTankPos);

        AABB sourceConnectionArea = new AABB(sourceBackVec, sourceBackVec);
        AABB targetTankArea = new AABB(targetTankVec, targetTankVec);

        Object sourceConnectionObject = new Object();
        Object targetTankObject = new Object();

        float slowSpeed = 6.4F;

        long demoBalloonAmount = BalloonPackingLimits.getBaseAmount();
        ItemStack demoBalloon = BalloonFactory.create(new GasStack(CCBGases.NATURAL_AIR.get(), demoBalloonAmount));
        ItemStack gasFactoryGaugeItem = new ItemStack(CCBBlocks.GAS_FACTORY_GAUGE_BLOCK.asItem());

        scene.idle(20);
        scene.world().showSection(sourceTankSelection, Direction.NORTH);

        scene.idle(3);
        scene.world().showSection(sourcePackagerSelection, Direction.SOUTH);
        scene.world().showSection(leverSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, sourceConnectionObject, sourceConnectionArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, sourceConnectionObject, sourceConnectionArea.inflate(0.5, 0.5, 0).expandTowards(0, -1, 0), 60);
        scene.overlay().showText(60).text("Connect the rear face of the Gas Packager directly to a gas inventory").colored(PonderPalette.GREEN).pointAt(sourceBackVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("The front handles balloons, and no rotational power is required").colored(PonderPalette.BLUE).pointAt(sourceFrontVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().modifyBlockEntity(sourceTankPos, AirtightTankBlockEntity.class, tank -> tank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), demoBalloonAmount * 2)).requireAccepted());
        scene.overlay().showText(60).text("A redstone signal packages gas directly from the attached inventory").pointAt(leverVec).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.world().toggleRedstonePower(leverSelection);
        scene.effects().indicateRedstone(leverPos);
        scene.world().modifyBlock(sourcePackagerPos, state -> state.setValue(GasPackagerBlock.POWERED, true), false);
        scene.world().modifyBlockEntity(sourcePackagerPos, GasPackagerBlockEntity.class, packager -> packager.attemptToSend(null));

        scene.idle(60);
        scene.overlay().showText(60).text("Each balloon can contain only one gas type").colored(PonderPalette.RED).pointAt(sourcePackagerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Its capacity follows the local atmospheric pressure, not the attached tank pressure").colored(PonderPalette.RED).pointAt(sourcePackagerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);
        scene.world().showSection(beltSelection, Direction.SOUTH);
        scene.world().showSection(motorSelection, Direction.SOUTH);

        scene.idle(15);
        scene.world().setKineticSpeed(beltSelection, slowSpeed);
        scene.world().setKineticSpeed(motorSelection, slowSpeed);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.world().modifyBlockEntity(targetTankPos, AirtightTankBlockEntity.class, tank -> prepareTargetTank(tank, targetPackagerPos, demoBalloonAmount));
        scene.world().showSection(targetPackagerSelection, Direction.SOUTH);
        scene.world().showSection(targetTankSelection, Direction.NORTH);
        scene.world().showSection(sourceFunnelSelection, Direction.SOUTH);
        scene.world().showSection(targetFunnelSelection, Direction.SOUTH);

        scene.idle(15);
        scene.world().modifyBlockEntity(sourcePackagerPos, GasPackagerBlockEntity.class, packager -> packager.inventory.extractItem(0, 1, false));
        scene.world().flapFunnel(sourceFunnelPos, true);
        scene.world().createItemOnBelt(sourceBeltPos, Direction.EAST, demoBalloon.copy());
        scene.world().modifyBlockEntity(sourcePackagerPos, GasPackagerBlockEntity.class, packager -> packager.attemptToSend(null));
        scene.overlay().showText(60).text("Filled balloons behave like Create packages and travel through normal package logistics").colored(PonderPalette.GREEN).pointAt(beltVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().toggleRedstonePower(leverSelection);
        scene.effects().indicateRedstone(leverPos);
        scene.world().modifyBlock(sourcePackagerPos, state -> state.setValue(GasPackagerBlock.POWERED, false), false);
        scene.overlay().showControls(sourceFrontVec, Pointing.DOWN, 60).withItem(gasFactoryGaugeItem.copy());
        scene.overlay().showText(60).text("Stored gases can be requested through Create logistics").colored(PonderPalette.BLUE).pointAt(sourceFrontVec).placeNearTarget().attachKeyFrame();

        scene.idle(50);
        scene.world().modifyBlockEntity(sourcePackagerPos, GasPackagerBlockEntity.class, packager -> packager.inventory.extractItem(0, 1, false));
        scene.world().flapFunnel(sourceFunnelPos, true);
        scene.world().createItemOnBelt(sourceBeltPos, Direction.EAST, demoBalloon.copy());

        scene.idle(30);
        scene.overlay().showText(60).text("The Gas Packager can also unpack incoming balloons").colored(PonderPalette.BLUE).pointAt(targetPackagerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("The target must be able to accept the whole balloon at local atmospheric pressure").colored(PonderPalette.RED).pointAt(targetTankVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, targetTankObject, targetTankArea.inflate(0.5).expandTowards(0, 1, 0), 60);
        scene.overlay().showControls(targetFrontVec, Pointing.DOWN, 60).rightClick().withItem(demoBalloon.copy());
        scene.overlay().showText(60).text("If the whole balloon cannot be accepted, no gas is transferred and the balloon stays intact").colored(PonderPalette.RED).pointAt(targetTankVec).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.world().flapFunnel(targetFunnelPos, false);
        scene.world().removeItemsFromBelt(targetBeltPos);
        scene.world().modifyBlockEntity(targetPackagerPos, GasPackagerBlockEntity.class, packager -> packager.unwrapBox(demoBalloon.copy(), false));
        scene.world().modifyBlockEntity(targetTankPos, AirtightTankBlockEntity.class, tank -> tank.getTankInventory().fill(new GasStack(CCBGases.NATURAL_AIR.get(), demoBalloonAmount), GasAction.EXECUTE));

        scene.idle(40);
        scene.world().setBlock(targetPackagerPos, CCBBlocks.GAS_UNPACKAGER_BLOCK.getDefaultState().setValue(GasUnpackagerBlock.FACING, Direction.NORTH), true);

        scene.idle(15);
        scene.overlay().showText(60).text("The Gas Unpackager instead transfers as much gas as the target can accept").colored(PonderPalette.GREEN).pointAt(targetPackagerVec).placeNearTarget().attachKeyFrame();

        scene.idle(40);
        scene.world().flapFunnel(targetFunnelPos, false);
        scene.world().removeItemsFromBelt(targetBeltPos);
        scene.world().modifyBlockEntity(targetPackagerPos, GasUnpackagerBlockEntity.class, unpackager -> {
            unpackager.previouslyUnwrapped = demoBalloon.copy();
            unpackager.animationInward = true;
            unpackager.animationTicks = PackagerBlockEntity.CYCLE;
        });

        scene.idle(20);
        scene.world().modifyBlockEntity(targetTankPos, AirtightTankBlockEntity.class, tank -> executePartialUnpackForPonder(tank, targetPackagerPos, demoBalloon.copy()));

        scene.idle(20);
        scene.overlay().showText(60).text("Any remainder stays in the returned balloon, allowing partial unloading").colored(PonderPalette.GREEN).pointAt(targetFrontVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    private static void executePartialUnpackForPonder(AirtightTankBlockEntity targetTank, BlockPos unpackagerPos, ItemStack balloon) {
        Level level = targetTank.getLevel();
        if (level == null) {
            return;
        }

        GasStack gas = BalloonItem.getGas(balloon);
        long sourcePressurePa = BalloonPressureSemantics.ambientPressurePa(level, unpackagerPos);
        ExecutionResult result = BalloonGasTransferPlan.plan(targetTank.getTankInventory(), gas, sourcePressurePa, TransferPolicy.BEST_EFFORT).execute();
        long remainingAmount = Math.max(0, gas.getAmount() - result.transferredAmount());
        if (!(level.getBlockEntity(unpackagerPos) instanceof GasUnpackagerBlockEntity unpackager)) {
            return;
        }

        ItemStack returnedBalloon = balloon.copyWithCount(1);
        BalloonItem.setGas(returnedBalloon, remainingAmount > 0 ? gas.copyWithAmount(remainingAmount) : GasStack.EMPTY);
        unpackager.previouslyUnwrapped = ItemStack.EMPTY;
        unpackager.heldBox = remainingAmount > 0 ? returnedBalloon : ItemStack.EMPTY;
        unpackager.animationInward = false;
        unpackager.animationTicks = PackagerBlockEntity.CYCLE;
    }

    private static void prepareTargetTank(AirtightTankBlockEntity targetTank, BlockPos packagerPos, long balloonAmount) {
        Level level = targetTank.getLevel();
        if (level == null) {
            return;
        }

        long ambientPressurePa = BalloonPressureSemantics.ambientPressurePa(level, packagerPos);
        long amountAtAmbientPressure = Math.min(GasPressure.amount(targetTank.getTankInventory().getVolume(), ambientPressurePa), targetTank.getTankInventory().getMaxAmount());
        long partialTransferAmount = Math.max(1, balloonAmount / 2);
        long requiredHeadroom = balloonAmount + partialTransferAmount;
        long initialAmount = Math.max(0, amountAtAmbientPressure - requiredHeadroom);
        GasStack initialGas = initialAmount > 0 ? new GasStack(CCBGases.NATURAL_AIR.get(), initialAmount) : GasStack.EMPTY;
        targetTank.getTankInventory().tryReplaceContents(initialGas).requireAccepted();
    }
}
