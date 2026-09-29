package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
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
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerBlock;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerOutputs;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.Candidate;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasunpackager.GasUnpackagerBlockEntity;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasRepackagerScenes {
    private static final int MAIN_ORDER_ID = 7202;
    private static final int SIDE_ORDER_ID = 7302;

    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("gas_repackager", "Organizing Gas Deliveries");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos centerPos = util.grid().at(3, 1, 3);
        BlockPos repackagerPos = centerPos.east(2).above();
        BlockPos leverPos = repackagerPos.above();
        BlockPos outputFunnelPos = repackagerPos.north();
        BlockPos outputBeltPos = outputFunnelPos.below();
        BlockPos cachePos = repackagerPos.south();
        BlockPos cacheChutePos = cachePos.above();
        BlockPos motorPos = outputBeltPos.east().north();
        BlockPos naturalUnpackagerPos = centerPos.above();
        BlockPos naturalFunnelPos = naturalUnpackagerPos.north();
        BlockPos naturalBeltPos = naturalFunnelPos.below();
        BlockPos naturalTankPos = naturalUnpackagerPos.south();
        BlockPos naturalTankSupportPos = naturalTankPos.below();
        BlockPos steamUnpackagerPos = centerPos.west(2).above();
        BlockPos steamFunnelPos = steamUnpackagerPos.north();
        BlockPos steamBeltPos = steamFunnelPos.below();
        BlockPos steamTankPos = steamUnpackagerPos.south();
        BlockPos steamTankSupportPos = steamTankPos.below();
        BlockPos balloonPos = centerPos.west(2).north();
        BlockPos beltStartPos = balloonPos.west();
        BlockPos balloon2Pos = balloonPos.east(2);
        BlockPos balloon3Pos = balloon2Pos.east(2);

        Selection beltSelection = util.select().fromTo(outputBeltPos.east(), steamBeltPos.west());
        Selection motorSelection = util.select().position(motorPos);
        Selection repackagerSelection = util.select().fromTo(repackagerPos, repackagerPos.below());
        Selection leverSelection = util.select().position(leverPos);
        Selection outputFunnelSelection = util.select().position(outputFunnelPos);
        Selection cacheSelection = util.select().fromTo(cachePos, cachePos.below());
        Selection cacheChuteSelection = util.select().position(cacheChutePos);
        Selection naturalReceiverSelection = util.select().fromTo(naturalUnpackagerPos, naturalUnpackagerPos.below()).add(util.select().position(naturalFunnelPos)).add(util.select().position(naturalTankPos)).add(util.select().position(naturalTankSupportPos));
        Selection steamReceiverSelection = util.select().fromTo(steamUnpackagerPos, steamUnpackagerPos.below()).add(util.select().position(steamFunnelPos)).add(util.select().position(steamTankPos)).add(util.select().position(steamTankSupportPos));

        Vec3 beltVec = util.vector().topOf(centerPos.north());
        Vec3 repackagerVec = util.vector().centerOf(repackagerPos);
        Vec3 cacheVec = util.vector().centerOf(cachePos);
        Vec3 naturalTankVec = util.vector().centerOf(naturalTankPos);
        Vec3 steamTankVec = util.vector().centerOf(steamTankPos);
        Vec3 dropVec = util.vector().centerOf(cacheChutePos.above());
        Vec3 dropMotion = util.vector().of(0, -0.12, 0);

        Object naturalTankOutline = new Object();
        Object steamTankOutline = new Object();

        AABB naturalTankArea = new AABB(naturalTankVec, naturalTankVec);
        AABB steamTankArea = new AABB(steamTankVec, steamTankVec);

        long localPackingLimit = Math.max(1, BalloonPackingLimits.getLocalPackingLimit(GasPressure.REFERENCE_PRESSURE_PA));
        long naturalFragmentAmount = Math.max(1, localPackingLimit / 2 + localPackingLimit / 10);
        long steamAmount = Math.max(1, localPackingLimit / 3);
        long sideAmount = Math.max(1, localPackingLimit / 4);

        float slowSpeed = 6.4F;

        ItemStack main0 = BalloonFactory.createOrdered(new GasStack(CCBGases.NATURAL_AIR.get(), naturalFragmentAmount), "Gas Depot", MAIN_ORDER_ID, 0, false, 0, false, null);
        ItemStack main1 = BalloonFactory.createOrdered(new GasStack(CCBGases.NATURAL_AIR.get(), naturalFragmentAmount), "Gas Depot", MAIN_ORDER_ID, 0, false, 1, true, null);
        ItemStack main2 = BalloonFactory.createOrdered(new GasStack(CCBGases.NATURAL_AIR.get(), naturalFragmentAmount), "Gas Workshop", MAIN_ORDER_ID, 1, true, 0, false, null);
        ItemStack main3 = BalloonFactory.createOrdered(new GasStack(CCBGases.STEAM.get(), steamAmount), "Gas Workshop", MAIN_ORDER_ID, 1, true, 1, true, null);
        ItemStack sideRequest = BalloonFactory.createOrdered(new GasStack(CCBGases.ULTRAWARM_AIR.get(), sideAmount), "Side Request", SIDE_ORDER_ID, 0, true, 0, true, null);

        List<Candidate> mainCandidates = List.of(new Candidate(0, main0, BalloonItem.getGas(main0)), new Candidate(1, main1, BalloonItem.getGas(main1)), new Candidate(2, main2, BalloonItem.getGas(main2)), new Candidate(3, main3, BalloonItem.getGas(main3)));
        List<BigItemStack> expectedOutput = GasRepackagerOutputs.createMixedOrderOutput(MAIN_ORDER_ID, mainCandidates, GasPressure.REFERENCE_PRESSURE_PA, RandomSource.create(MAIN_ORDER_ID));
        List<ItemStack> naturalOutputs = new ArrayList<>();
        List<ItemStack> steamOutputs = new ArrayList<>();
        for (BigItemStack output : expectedOutput) {
            GasStack gas = BalloonItem.getGas(output.stack);
            if (gas.is(CCBGases.NATURAL_AIR.get())) {
                naturalOutputs.add(output.stack.copyWithCount(1));
                continue;
            }

            if (!gas.is(CCBGases.STEAM.get())) {
                continue;
            }

            steamOutputs.add(output.stack.copyWithCount(1));
        }

        scene.idle(20);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);
        scene.world().showSection(beltSelection, Direction.SOUTH);
        scene.world().showSection(motorSelection, Direction.SOUTH);
        scene.world().setKineticSpeed(beltSelection, slowSpeed / 4);
        scene.world().setKineticSpeed(motorSelection, slowSpeed / 4);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(15);
        scene.world().createItemOnBelt(balloon3Pos, Direction.EAST, main0.copy());
        scene.world().createItemOnBelt(balloon2Pos, Direction.EAST, sideRequest.copy());
        scene.world().createItemOnBelt(balloonPos, Direction.EAST, main1.copy());
        scene.overlay().showText(60).text("A single logistics request can arrive as several balloon packages").colored(PonderPalette.INPUT).pointAt(beltVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("An unrelated complete request can pass between those fragments").colored(PonderPalette.RED).pointAt(beltVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        for (int x = 0; x <= 6; x++) {
            scene.world().removeItemsFromBelt(beltStartPos.east(x));
        }
        scene.world().showSection(cacheSelection, Direction.DOWN);
        scene.world().showSection(cacheChuteSelection, Direction.DOWN);
        scene.world().showSection(repackagerSelection, Direction.EAST);
        scene.world().showSection(leverSelection, Direction.EAST);
        scene.world().showSection(outputFunnelSelection, Direction.SOUTH);
        scene.world().setKineticSpeed(beltSelection, 7.5F * slowSpeed);
        scene.world().setKineticSpeed(motorSelection, 7.5F * slowSpeed);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(20);
        scene.overlay().showText(60).text("Route the fragments into an inventory connected to a Gas Repackager").colored(PonderPalette.GREEN).pointAt(cacheVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().toggleRedstonePower(leverSelection);
        scene.world().modifyBlock(repackagerPos, state -> state.setValue(GasRepackagerBlock.POWERED, true), false);
        scene.effects().indicateRedstone(leverPos);
        scene.overlay().showText(60).text("Incomplete requests stay cached, while unrelated complete requests can pass through").colored(PonderPalette.BLUE).pointAt(repackagerVec).placeNearTarget().attachKeyFrame();

        scene.idle(15);
        dropIntoCache(scene, cachePos, dropVec, dropMotion, main0.copy());
        scene.world().modifyBlockEntity(repackagerPos, GasRepackagerBlockEntity.class, repackager -> repackager.attemptToSend(null));

        scene.idle(5);
        dropIntoCache(scene, cachePos, dropVec, dropMotion, main1.copy());
        scene.world().modifyBlockEntity(repackagerPos, GasRepackagerBlockEntity.class, repackager -> repackager.attemptToSend(null));

        scene.idle(5);
        dropIntoCache(scene, cachePos, dropVec, dropMotion, sideRequest.copy());
        scene.world().modifyBlockEntity(repackagerPos, GasRepackagerBlockEntity.class, repackager -> repackager.attemptToSend(null));
        scene.world().modifyBlockEntity(repackagerPos, GasRepackagerBlockEntity.class, GasRepackagerScenes::promoteNextQueuedPackageForPonder);

        scene.idle(20);
        scene.world().modifyBlockEntity(repackagerPos, GasRepackagerBlockEntity.class, repackager -> repackager.inventory.extractItem(0, 1, false));
        scene.world().flapFunnel(outputFunnelPos, true);
        scene.world().createItemOnBelt(outputBeltPos, Direction.EAST, sideRequest.copy());

        scene.idle(35);
        scene.overlay().showText(60).text("When the final fragment arrives, the Gas Repackager rebuilds the complete request").colored(PonderPalette.GREEN).pointAt(cacheVec).placeNearTarget().attachKeyFrame();
        dropIntoCache(scene, cachePos, dropVec, dropMotion, main2.copy());
        scene.world().modifyBlockEntity(repackagerPos, GasRepackagerBlockEntity.class, repackager -> repackager.attemptToSend(null));

        scene.idle(47);
        dropIntoCache(scene, cachePos, dropVec, dropMotion, main3.copy());
        scene.world().modifyBlockEntity(repackagerPos, GasRepackagerBlockEntity.class, repackager -> repackager.attemptToSend(null));

        scene.idle(18);
        scene.world().showSection(naturalReceiverSelection, Direction.SOUTH);
        scene.world().showSection(steamReceiverSelection, Direction.SOUTH);

        scene.idle(15);
        scene.overlay().showText(60).text("Matching gases are combined when the complete request is rebuilt").colored(PonderPalette.GREEN).pointAt(repackagerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Each rebuilt balloon still obeys the local atmospheric-pressure packing limit").colored(PonderPalette.RED).pointAt(repackagerVec).placeNearTarget().attachKeyFrame();
        for (ItemStack output : naturalOutputs) {
            sendRepackedBalloon(scene, repackagerPos, outputFunnelPos, outputBeltPos, naturalFunnelPos, naturalBeltPos, naturalUnpackagerPos, naturalTankPos, output, 25);
        }

        scene.idle(20);
        scene.world().modifyBlock(naturalFunnelPos, state -> state.setValue(BeltFunnelBlock.POWERED, true), false);
        scene.overlay().showText(60).text("Different gases stay separate, so one request may still produce several balloons").colored(PonderPalette.BLUE).pointAt(repackagerVec).placeNearTarget().attachKeyFrame();
        for (ItemStack output : steamOutputs) {
            sendRepackedBalloon(scene, repackagerPos, outputFunnelPos, outputBeltPos, steamFunnelPos, steamBeltPos, steamUnpackagerPos, steamTankPos, output, 45);
        }
        scene.world().modifyBlock(naturalFunnelPos, state -> state.setValue(BeltFunnelBlock.POWERED, false), false);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, naturalTankOutline, naturalTankArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, steamTankOutline, steamTankArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, naturalTankOutline, naturalTankArea.inflate(0.5).expandTowards(0, -1, 0), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, steamTankOutline, steamTankArea.inflate(0.5).expandTowards(0, -1, 0), 60);
        scene.overlay().showText(60).text("Rebuilt balloons unpack normally into their matching downstream gas inventories").colored(PonderPalette.OUTPUT).pointAt(naturalTankVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Repackaging keeps completed gas requests organized across multiple balloons").pointAt(beltVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    private static void dropIntoCache(CreateSceneBuilder scene, BlockPos cachePos, Vec3 dropVec, Vec3 dropMotion, ItemStack packageStack) {
        ElementLink<EntityElement> droppedPackage = scene.world().createEntity(level -> {
            PackageEntity packageEntity = PackageEntity.fromItemStack(level, dropVec, packageStack.copy());
            packageEntity.setDeltaMovement(dropMotion);
            return packageEntity;
        });

        scene.idle(8);
        scene.world().modifyEntity(droppedPackage, Entity::discard);
        scene.world().modifyBlockEntity(cachePos, BlockEntity.class, blockEntity -> insertIntoAttachedInventory(blockEntity, packageStack.copy()));

        scene.idle(5);
    }

    private static void insertIntoAttachedInventory(BlockEntity blockEntity, ItemStack packageStack) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        IItemHandler handler = level.getCapability(ItemHandler.BLOCK, blockEntity.getBlockPos(), null);
        if (handler == null) {
            return;
        }

        ItemHandlerHelper.insertItemStacked(handler, packageStack.copy(), false);
    }

    private static void promoteNextQueuedPackageForPonder(GasRepackagerBlockEntity blockEntity) {
        if (!blockEntity.heldBox.isEmpty() || blockEntity.animationTicks != 0 || blockEntity.queuedExitingPackages.isEmpty()) {
            return;
        }

        BigItemStack next = blockEntity.queuedExitingPackages.getFirst();
        blockEntity.heldBox = next.stack.copyWithCount(1);
        next.count--;
        if (next.count <= 0) {
            blockEntity.queuedExitingPackages.removeFirst();
        }
        blockEntity.animationInward = false;
        blockEntity.animationTicks = PackagerBlockEntity.CYCLE;
        blockEntity.notifyUpdate();
    }

    private static void sendRepackedBalloon(CreateSceneBuilder scene, BlockPos repackagerPos, BlockPos outputFunnelPos, BlockPos outputBeltPos, BlockPos receiverFunnelPos, BlockPos receiverBeltPos, BlockPos unpackagerPos, BlockPos tankPos, ItemStack output, int travelTicks) {
        scene.world().modifyBlockEntity(repackagerPos, GasRepackagerBlockEntity.class, GasRepackagerScenes::promoteNextQueuedPackageForPonder);

        scene.idle(20);
        scene.world().modifyBlockEntity(repackagerPos, GasRepackagerBlockEntity.class, repackager -> repackager.inventory.extractItem(0, 1, false));
        scene.world().flapFunnel(outputFunnelPos, true);
        scene.world().createItemOnBelt(outputBeltPos, Direction.EAST, output.copy());

        scene.idle(travelTicks);
        scene.world().removeItemsFromBelt(receiverBeltPos);
        scene.world().flapFunnel(receiverFunnelPos, false);
        scene.world().modifyBlockEntity(unpackagerPos, GasUnpackagerBlockEntity.class, unpackager -> {
            unpackager.previouslyUnwrapped = output.copy();
            unpackager.animationInward = true;
            unpackager.animationTicks = PackagerBlockEntity.CYCLE;
        });

        scene.idle(20);
        scene.world().modifyBlockEntity(tankPos, AirtightTankBlockEntity.class, tank -> executeBalloonTransfer(tank, unpackagerPos, output));

        scene.idle(5);
    }

    private static void executeBalloonTransfer(AirtightTankBlockEntity targetTank, BlockPos unpackagerPos, ItemStack balloon) {
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
}
