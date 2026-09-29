package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.RequestPromise;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.foundation.ponder.element.BeltItemElement;
import net.createmod.catnip.math.Pointing;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonFactory;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPackingLimits;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlock;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerBlock;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerOutputs;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.Candidate;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipeLookup;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlanner;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.apache.commons.lang3.mutable.MutableBoolean;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasFactoryGaugeScenes {
    private static final UUID PONDER_NETWORK = UUID.fromString("7bd66c30-0a7d-4d98-a116-7b4d00b17c33");
    private static final int RESTOCK_ORDER_ID = 7403;

    @SuppressWarnings("ConstantExpression")
    public static void restocking(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("gas_factory_gauge_restocking", "Restocking Gas Storage");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.925F);
        scene.setSceneOffsetY(-0.25F);
        scene.showBasePlate();

        BlockPos sourceTankPos = util.grid().at(6, 2, 4);
        BlockPos sourceTankSupportPos = sourceTankPos.below();
        BlockPos sourcePackagerPos = sourceTankPos.north();
        BlockPos sourceLeverPos = sourceTankPos.north().above();
        BlockPos sourceFunnelPos = sourceTankPos.north(2);
        BlockPos sourceBeltPos = sourceTankPos.north(2).below();

        BlockPos repackagerPos = sourceTankPos.west(3).north(2);
        BlockPos repackagerLeverPos = sourceTankPos.west(3).north(2).above();
        BlockPos cachePos = sourceTankPos.west(2).north(2);
        BlockPos repackagerInputFunnelPos = sourceTankPos.west().north(2);
        BlockPos repackagerInputBeltPos = sourceTankPos.west().north(2).below();
        BlockPos repackagerOutputFunnelPos = sourceTankPos.west(4).north(2);
        BlockPos repackagerOutputBeltPos = sourceTankPos.west(4).north(2).below();

        BlockPos targetTankPos = sourceTankPos.west(6);
        BlockPos targetTankSupportPos = sourceTankPos.west(6).below();
        BlockPos targetPackagerPos = sourceTankPos.west(6).north();
        BlockPos targetGaugePos = sourceTankPos.west(6).north().above();
        BlockPos targetFunnelPos = sourceTankPos.west(6).north(2);
        BlockPos targetBeltPos = sourceTankPos.west(6).north(2).below();
        BlockPos beltStartPos = sourceTankPos.west(6).north(2).below();
        BlockPos beltEndPos = sourceTankPos.north(2).below();
        BlockPos beltMotorPos = sourceTankPos.north(3).below();

        Selection beltSelection = util.select().fromTo(beltStartPos, beltEndPos).add(util.select().position(beltMotorPos));
        Selection sourceLeverSelection = util.select().position(sourceLeverPos);
        Selection repackagerLeverSelection = util.select().position(repackagerLeverPos);
        Selection sourceSelection = util.select().position(sourceTankSupportPos).add(util.select().position(sourceTankPos)).add(util.select().fromTo(sourcePackagerPos, sourcePackagerPos.below())).add(util.select().position(sourceFunnelPos)).add(sourceLeverSelection);
        Selection repackagerSelection = util.select().position(repackagerPos).add(util.select().position(cachePos)).add(util.select().position(repackagerInputFunnelPos)).add(util.select().position(repackagerOutputFunnelPos)).add(repackagerLeverSelection);
        Selection targetSelection = util.select().position(targetTankSupportPos).add(util.select().position(targetTankPos)).add(util.select().fromTo(targetPackagerPos, targetPackagerPos.below())).add(util.select().position(targetGaugePos)).add(util.select().position(targetFunnelPos)).add(util.select().position(targetTankPos.above()));

        Vec3 gaugeVec = util.vector().centerOf(targetGaugePos).add(0.25, -0.4375, -0.25);
        Vec3 sourcePackagerVec = util.vector().centerOf(sourcePackagerPos);
        Vec3 repackagerVec = util.vector().centerOf(repackagerPos);
        Vec3 targetTankVec = util.vector().centerOf(targetTankPos);

        AABB targetGaugeArea = new AABB(gaugeVec, gaugeVec);
        AABB targetTankArea = new AABB(targetTankVec, targetTankVec);

        Object targetGaugeObject = new Object();
        Object targetTankObject = new Object();

        long localLimit = Math.max(1, BalloonPackingLimits.getLocalPackingLimit(GasPressure.REFERENCE_PRESSURE_PA));
        int missingAmount = (int) Math.min(GasFactoryGaugeBehaviour.MAX_TARGET_AMOUNT / 2, localLimit + Math.max(1, localLimit / 2));
        int initialAmount = Mth.clamp(missingAmount / 3, 1, 750);
        int targetAmount = initialAmount + missingAmount;
        int firstAmount = (int) Math.min(localLimit, missingAmount);
        int secondAmount = missingAmount - firstAmount;
        int mediumSpeed = 48;

        ItemStack gasToken = VirtualGasItems.createVirtualItem(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        ItemStack firstBalloon = BalloonFactory.createOrdered(new GasStack(CCBGases.NATURAL_AIR.get(), firstAmount), "Gas Workshop", RESTOCK_ORDER_ID, 0, true, 0, secondAmount <= 0, null);
        ItemStack secondBalloon = secondAmount <= 0 ? ItemStack.EMPTY : BalloonFactory.createOrdered(new GasStack(CCBGases.NATURAL_AIR.get(), secondAmount), "Gas Workshop", RESTOCK_ORDER_ID, 0, true, 1, true, null);
        ItemStack canisterItem = new ItemStack(CCBItems.GAS_CANISTER.asItem());

        List<Candidate> candidates = new ArrayList<>();
        candidates.add(new Candidate(0, firstBalloon, BalloonItem.getGas(firstBalloon)));
        if (!secondBalloon.isEmpty()) {
            candidates.add(new Candidate(1, secondBalloon, BalloonItem.getGas(secondBalloon)));
        }
        List<BigItemStack> rebuilt = GasRepackagerOutputs.createMixedOrderOutput(RESTOCK_ORDER_ID, candidates, GasPressure.REFERENCE_PRESSURE_PA, RandomSource.create(RESTOCK_ORDER_ID));
        List<PackagingRequest> requestQueue = new ArrayList<>();
        requestQueue.add(PackagingRequest.create(gasToken.copy(), missingAmount, "Gas Workshop", 0, new MutableBoolean(true), 0, RESTOCK_ORDER_ID, null));

        scene.idle(20);
        scene.world().setBlock(beltMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);
        scene.world().showSection(beltSelection, Direction.SOUTH);
        scene.world().showSection(targetSelection, Direction.EAST);

        scene.idle(15);
        scene.world().setKineticSpeed(beltSelection, mediumSpeed);
        scene.world().modifyBlockEntity(targetGaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
            gauge.addPanel(PanelSlot.TOP_LEFT, PONDER_NETWORK);
            gauge.restocker = true;
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            panel.setFilter(ItemStack.EMPTY.copyWithCount(1));
            panel.count = 0;
            panel.upTo = true;
            panel.recipeAddress = "";
            panel.network = PONDER_NETWORK;
            panel.satisfied = false;
            panel.promisedSatisfied = false;
            panel.redstonePowered = false;
            gauge.redraw = true;
            gauge.notifyUpdate();
        });
        scene.world().modifyBlockEntity(targetTankPos, AirtightTankBlockEntity.class, tank -> tank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), initialAmount)).requireAccepted());

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, targetGaugeObject, targetGaugeArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, targetTankObject, targetTankArea, 3);

        scene.idle(3);
        targetGaugeArea = targetGaugeArea.inflate(0.25, 0.0625, 0.25);
        targetTankArea = targetTankArea.inflate(0.5).expandTowards(0, -1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, targetGaugeObject, targetGaugeArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, targetTankObject, targetTankArea, 60);
        scene.overlay().showText(60).text("A Gas Factory Gauge on a Gas Packager monitors the gas storage behind it").pointAt(gaugeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(gaugeVec, Pointing.DOWN, 60).rightClick().withItem(canisterItem.copy());
        scene.overlay().showText(60).text("Right-click with a container holding exactly one gas to choose the monitored gas").colored(PonderPalette.BLUE).pointAt(gaugeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().modifyBlockEntity(targetGaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            if (panel == null) {
                return;
            }

            panel.setFilter(gasToken.copyWithCount(1));
            gauge.redraw = true;
            gauge.notifyUpdate();
        });

        scene.idle(73);
        scene.overlay().showControls(gaugeVec, Pointing.DOWN, 60).rightClick();
        scene.overlay().showText(60).text("Hold Right-click to set the target gas amount").colored(PonderPalette.BLUE).pointAt(gaugeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().modifyBlockEntity(targetGaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            if (panel == null) {
                return;
            }

            panel.count = targetAmount;
            panel.upTo = true;
            gauge.redraw = true;
            gauge.notifyUpdate();
        });
        scene.world().modifyBlockEntity(targetGaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            if (panel == null || !panel.isActive()) {
                return;
            }

            int stored = panel.getLevelInStorage();
            int promised = panel.getPromised();
            int target = panel.getAmount();
            panel.satisfied = stored >= target;
            panel.promisedSatisfied = stored + promised >= target;
            panel.bulb.setValue(panel.redstonePowered || panel.satisfied ? 1 : 0);
            gauge.redraw = true;
            gauge.notifyUpdate();
        });

        scene.idle(73);
        scene.overlay().showControls(gaugeVec, Pointing.DOWN, 60).rightClick();
        scene.overlay().showText(60).text("Right-click to open the gauge settings and set the delivery address").colored(PonderPalette.BLUE).pointAt(gaugeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().modifyBlockEntity(targetGaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            if (panel == null) {
                return;
            }

            panel.recipeAddress = "Gas Workshop";
            gauge.redraw = true;
            gauge.notifyUpdate();
        });

        scene.idle(73);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, targetTankObject, targetTankArea, 60);
        scene.overlay().showText(60).text("If stored gas is below the target, the gauge requests only the missing amount").pointAt(targetTankVec).placeNearTarget().attachKeyFrame();

        scene.idle(10);
        scene.world().modifyBlockEntity(targetGaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            panel.restockerPromises.add(new RequestPromise(new BigItemStack(gasToken.copyWithCount(1), missingAmount)));
            panel.promisedSatisfied = panel.getLevelInStorage() + panel.getPromised() >= panel.getAmount();
            gauge.notifyUpdate();
        });
        scene.world().modifyBlockEntity(targetGaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            if (panel == null || !panel.isActive()) {
                return;
            }

            int stored = panel.getLevelInStorage();
            int promised = panel.getPromised();
            int target = panel.getAmount();
            panel.satisfied = stored >= target;
            panel.promisedSatisfied = stored + promised >= target;
            panel.bulb.setValue(panel.redstonePowered || panel.satisfied ? 1 : 0);
            gauge.redraw = true;
            gauge.notifyUpdate();
        });

        scene.idle(70);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, targetGaugeObject, targetGaugeArea, 60);
        scene.overlay().showText(60).text("Promised gas also counts toward the target, preventing duplicate requests").colored(PonderPalette.BLUE).pointAt(gaugeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().modifyBlockEntity(sourceTankPos, AirtightTankBlockEntity.class, tank -> tank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), missingAmount + Math.max(1, firstAmount))).requireAccepted());
        scene.world().showSection(sourceSelection, Direction.WEST);

        scene.idle(20);
        scene.overlay().showText(60).text("A source Gas Packager fills addressed balloons from real gas stock").pointAt(sourcePackagerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Requests exceeding one balloon's limit are split across several balloons").colored(PonderPalette.BLUE).pointAt(sourcePackagerVec).placeNearTarget().attachKeyFrame();
        scene.world().showSection(repackagerSelection, Direction.DOWN);

        scene.idle(20);
        createRequestedBalloon(scene, sourcePackagerPos, sourceLeverPos, sourceLeverSelection, requestQueue, sourceFunnelPos, sourceBeltPos, firstBalloon.copy());

        scene.idle(15);
        scene.world().removeItemsFromBelt(repackagerInputBeltPos);
        scene.world().flapFunnel(repackagerInputFunnelPos, false);
        scene.world().modifyBlockEntity(cachePos, BlockEntity.class, blockEntity -> insertIntoAttachedInventory(blockEntity, firstBalloon.copy()));

        scene.idle(5);
        if (!secondBalloon.isEmpty()) {
            scene.idle(10);
            createRequestedBalloon(scene, sourcePackagerPos, sourceLeverPos, sourceLeverSelection, requestQueue, sourceFunnelPos, sourceBeltPos, secondBalloon.copy());

            scene.idle(15);
            scene.world().removeItemsFromBelt(repackagerInputBeltPos);
            scene.world().flapFunnel(repackagerInputFunnelPos, false);
            scene.world().modifyBlockEntity(cachePos, BlockEntity.class, blockEntity -> insertIntoAttachedInventory(blockEntity, secondBalloon.copy()));

            scene.idle(5);
        }
        scene.overlay().showText(60).text("A Gas Repackager can rebuild split requests before forwarding them").colored(PonderPalette.GREEN).pointAt(repackagerVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.world().toggleRedstonePower(repackagerLeverSelection);
        scene.effects().indicateRedstone(repackagerLeverPos);
        scene.world().modifyBlock(repackagerPos, state -> state.setValue(GasRepackagerBlock.POWERED, true), false);
        scene.world().modifyBlockEntity(repackagerPos, GasRepackagerBlockEntity.class, repackager -> {
            repackager.activate();
            repackager.heldBox = ItemStack.EMPTY;
            repackager.animationTicks = 0;
            repackager.queuedExitingPackages.clear();
            repackager.notifyUpdate();
        });

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, targetTankObject, targetTankArea, 60);
        scene.overlay().showText(60).text("Each delivered balloon adds gas to the tank and clears its outstanding promise").pointAt(targetTankVec).placeNearTarget().attachKeyFrame();

        scene.idle(10);
        int rebuiltBalloonCount = 0;
        for (BigItemStack output : rebuilt) {
            rebuiltBalloonCount += Math.max(1, output.count);
        }
        int rebuiltBalloonIndex = 0;
        for (BigItemStack output : rebuilt) {
            ItemStack balloon = output.stack.copyWithCount(1);
            int repeats = Math.max(1, output.count);
            for (int i = 0; i < repeats; i++) {
                rebuiltBalloonIndex++;
                scene.world().modifyBlockEntity(repackagerPos, GasRepackagerBlockEntity.class, repackager -> {
                    repackager.heldBox = balloon.copy();
                    repackager.animationInward = false;
                    repackager.animationTicks = PackagerBlockEntity.CYCLE;
                    repackager.notifyUpdate();
                });

                scene.idle(25);
                scene.world().flapFunnel(repackagerOutputFunnelPos, true);
                scene.world().createItemOnBelt(repackagerOutputBeltPos, Direction.EAST, balloon.copy());
                scene.world().modifyBlockEntity(repackagerPos, GasRepackagerBlockEntity.class, repackager -> repackager.inventory.extractItem(0, 1, false));
                if (rebuiltBalloonIndex == rebuiltBalloonCount) {
                    scene.world().toggleRedstonePower(repackagerLeverSelection);
                    scene.effects().indicateRedstone(repackagerPos.above());
                    scene.world().modifyBlock(repackagerPos, state -> state.setValue(GasRepackagerBlock.POWERED, false), false);
                }

                scene.idle(25);
                scene.world().removeItemsFromBelt(targetBeltPos);
                scene.world().flapFunnel(targetFunnelPos, false);
                scene.world().modifyBlockEntity(targetPackagerPos, GasPackagerBlockEntity.class, packager -> packager.inventory.insertItem(0, balloon.copy(), false));

                scene.idle(25);
                scene.world().modifyBlockEntity(targetGaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
                    FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
                    if (panel == null || !panel.isActive()) {
                        return;
                    }

                    int stored = panel.getLevelInStorage();
                    int promised = panel.getPromised();
                    int target = panel.getAmount();
                    panel.satisfied = stored >= target;
                    panel.promisedSatisfied = stored + promised >= target;
                    panel.bulb.setValue(panel.redstonePowered || panel.satisfied ? 1 : 0);
                    gauge.redraw = true;
                    gauge.notifyUpdate();
                });
            }
        }

        scene.world().modifyBlockEntity(targetGaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            if (panel == null || !panel.isActive()) {
                return;
            }

            int stored = panel.getLevelInStorage();
            int promised = panel.getPromised();
            int target = panel.getAmount();
            panel.satisfied = stored >= target;
            panel.promisedSatisfied = stored + promised >= target;
            panel.bulb.setValue(panel.redstonePowered || panel.satisfied ? 1 : 0);
            gauge.redraw = true;
            gauge.notifyUpdate();
        });
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, targetGaugeObject, targetGaugeArea, 60);
        scene.overlay().showText(60).text("Only stored gas reaching the target satisfies the gauge and stops new requests").colored(PonderPalette.GREEN).pointAt(gaugeVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    @SuppressWarnings("ConstantExpression")
    public static void recipes(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        RandomSource random = RandomSource.create();

        scene.title("gas_factory_gauge_recipes", "Gas in Factory Recipes");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.925F);
        scene.setSceneOffsetY(-0.35F);
        scene.showBasePlate();

        BlockPos itemSupport = util.grid().at(1, 3, 5);
        BlockPos itemGauge = itemSupport.north();
        BlockPos gasGauge = itemSupport.east(2).below().north();
        BlockPos outputGauge = itemSupport.east(4).north();
        BlockPos gaugeBoardMin = itemSupport.below(2);
        BlockPos gaugeBoardMax = itemSupport.east(4);

        BlockPos outputBeltPos = itemSupport.below(2).north(3);
        BlockPos processingBeltPos = itemSupport.east().below(2).north(3);
        BlockPos inputBeltPos = itemSupport.east(2).below(2).north(3);
        BlockPos beltMotorPos = itemSupport.east(2).below(2).north(4);
        BlockPos inputFunnelPos = itemSupport.east(2).below().north(3);
        BlockPos inputBarrelPos = itemSupport.east(3).below().north(3);
        BlockPos inputBarrelSupportPos = itemSupport.east(3).below(2).north(3);
        BlockPos stockLinkPos = itemSupport.east(4).below().north(3);
        BlockPos chamberPos = itemSupport.east().north(3);
        BlockPos gasPipePos = itemSupport.east().north(3).above();

        Selection gaugeSelection = util.select().fromTo(gaugeBoardMin, gaugeBoardMax).add(util.select().position(itemGauge)).add(util.select().position(gasGauge)).add(util.select().position(outputGauge));
        Selection inputStorageSelection = util.select().position(inputBarrelSupportPos).add(util.select().position(inputBarrelPos)).add(util.select().position(stockLinkPos));
        Selection beltSelection = util.select().fromTo(outputBeltPos, inputBeltPos).add(util.select().position(beltMotorPos));
        Selection funnelSelection = util.select().position(inputFunnelPos);
        Selection processorSelection = util.select().position(chamberPos).add(util.select().position(gasPipePos));

        Vec3 itemGaugeVec = util.vector().centerOf(itemGauge).add(0.25, 0.25, 0.4375);
        Vec3 gasGaugeVec = util.vector().centerOf(gasGauge).add(0.25, 0.25, 0.4375);
        Vec3 outputGaugeVec = util.vector().centerOf(outputGauge).add(0.25, 0.25, 0.4375);
        Vec3 chamberVec = util.vector().centerOf(chamberPos);
        Vec3 processingBeltVec = util.vector().topOf(processingBeltPos);
        Vec3 gasPipeVec = util.vector().centerOf(gasPipePos);
        Vec3 inputBarrelVec = util.vector().centerOf(inputBarrelPos);
        Vec3 nozzleVec = chamberVec.subtract(0, 1.6875, 0);

        AABB itemGaugeArea = new AABB(itemGaugeVec, itemGaugeVec);
        AABB gasGaugeArea = new AABB(gasGaugeVec, gasGaugeVec);
        AABB outputGaugeArea = new AABB(outputGaugeVec, outputGaugeVec);
        AABB inputBarrelArea = new AABB(inputBarrelVec, inputBarrelVec);
        AABB gasPipeArea = new AABB(gasPipeVec, gasPipeVec);
        AABB chamberArea = new AABB(chamberVec, chamberVec);

        Object itemGaugeObject = new Object();
        Object gasGaugeObject = new Object();
        Object outputGaugeObject = new Object();
        Object inputBarrelObject = new Object();
        Object gasPipeObject = new Object();
        Object chamberObject = new Object();

        ItemStack blazePowder = new ItemStack(Items.BLAZE_POWDER);
        ItemStack windCharge = new ItemStack(Items.WIND_CHARGE, 2);
        ItemStack gasToken = VirtualGasItems.createVirtualItem(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        int mediumSpeed = 16;

        scene.world().modifyBlockEntity(itemGauge, FactoryPanelBlockEntity.class, blockEntity -> {
            blockEntity.addPanel(PanelSlot.TOP_LEFT, PONDER_NETWORK);
            blockEntity.restocker = false;
            FactoryPanelBehaviour panel = blockEntity.panels.get(PanelSlot.TOP_LEFT);
            panel.setFilter(blazePowder.copyWithCount(1));
            panel.count = 0;
            panel.upTo = true;
            panel.recipeAddress = "";
            panel.network = PONDER_NETWORK;
            blockEntity.redraw = true;
            blockEntity.notifyUpdate();
        });
        scene.world().modifyBlockEntity(gasGauge, GasFactoryGaugeBlockEntity.class, gauge -> {
            gauge.addPanel(PanelSlot.TOP_LEFT, PONDER_NETWORK);
            gauge.restocker = false;
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            panel.setFilter(gasToken.copyWithCount(1));
            panel.count = 0;
            panel.upTo = true;
            panel.recipeAddress = "Injection Line";
            panel.network = PONDER_NETWORK;
            panel.satisfied = false;
            panel.promisedSatisfied = false;
            panel.redstonePowered = false;
            gauge.redraw = true;
            gauge.notifyUpdate();
        });
        scene.world().modifyBlockEntity(outputGauge, FactoryPanelBlockEntity.class, blockEntity -> {
            blockEntity.addPanel(PanelSlot.TOP_LEFT, PONDER_NETWORK);
            blockEntity.restocker = false;
            FactoryPanelBehaviour panel = blockEntity.panels.get(PanelSlot.TOP_LEFT);
            panel.setFilter(windCharge.copyWithCount(1));
            panel.count = 2;
            panel.upTo = true;
            panel.recipeAddress = "Injection Line";
            panel.network = PONDER_NETWORK;
            blockEntity.redraw = true;
            blockEntity.notifyUpdate();
        });
        scene.world().modifyBlockEntity(outputGauge, FactoryPanelBlockEntity.class, blockEntity -> {
            FactoryPanelBehaviour panel = blockEntity.panels.get(PanelSlot.TOP_LEFT);
            panel.recipeOutput = 2;
            blockEntity.notifyUpdate();
        });
        scene.world().modifyBlockEntity(inputBarrelPos, BlockEntity.class, blockEntity -> insertIntoAttachedInventory(blockEntity, blazePowder.copyWithCount(16)));
        scene.world().showSection(gaugeSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, gasGaugeObject, gasGaugeArea, 3);

        scene.idle(3);
        gasGaugeArea = gasGaugeArea.inflate(0.25, 0.25, 0.0625);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, gasGaugeObject, gasGaugeArea, 60);
        scene.overlay().showText(60).text("Gas Factory Gauges can be used as inputs in Factory Gauge recipes").pointAt(gasGaugeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(outputGaugeVec, Pointing.DOWN, 60).showing(AllIcons.I_ADD);
        addPanelConnection(scene, outputGauge, itemGauge, 1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, itemGaugeObject, itemGaugeArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputGaugeObject, outputGaugeArea, 3);

        scene.idle(3);
        itemGaugeArea = itemGaugeArea.inflate(0.25, 0.25, 0.0625);
        outputGaugeArea = outputGaugeArea.inflate(0.25, 0.25, 0.0625);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, itemGaugeObject, itemGaugeArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputGaugeObject, outputGaugeArea, 60);
        scene.overlay().showText(60).text("Connect item ingredients to the output gauge like a normal Factory Gauge recipe").colored(PonderPalette.GREEN).pointAt(itemGaugeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(outputGaugeVec, Pointing.DOWN, 60).showing(AllIcons.I_ADD);
        addPanelConnection(scene, outputGauge, gasGauge, 500);
        scene.world().modifyBlockEntity(outputGauge, FactoryPanelBlockEntity.class, blockEntity -> {
            FactoryPanelBehaviour panel = blockEntity.panels.get(PanelSlot.TOP_LEFT);
            FactoryPanelConnection connection = panel.targetedBy.get(new FactoryPanelPosition(gasGauge, PanelSlot.TOP_LEFT));
            if (connection == null) {
                return;
            }

            connection.arrowBendMode = 2;
            blockEntity.notifyUpdate();
        });
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, gasGaugeObject, gasGaugeArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputGaugeObject, outputGaugeArea, 60);
        scene.overlay().showText(60).text("Connect a Gas Factory Gauge the same way and set its required amount").colored(PonderPalette.GREEN).pointAt(gasGaugeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(beltMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);
        scene.world().showSection(inputStorageSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(beltSelection, Direction.SOUTH);
        scene.world().showSection(funnelSelection, Direction.SOUTH);

        scene.idle(15);
        scene.world().setKineticSpeed(beltSelection, mediumSpeed);
        scene.effects().rotationSpeedIndicator(beltMotorPos);

        scene.idle(20);
        scene.world().showSection(processorSelection, Direction.NORTH);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputBarrelObject, inputBarrelArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, gasPipeObject, gasPipeArea, 3);

        scene.idle(3);
        inputBarrelArea = inputBarrelArea.inflate(0.5);
        gasPipeArea = gasPipeArea.inflate(0.375, 0.5, 0.375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputBarrelObject, inputBarrelArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, gasPipeObject, gasPipeArea, 60);
        scene.overlay().showText(60).text("Linked storage feeds the item ingredient onto the belt while gas enters from above").colored(PonderPalette.INPUT).pointAt(chamberVec).placeNearTarget().attachKeyFrame();

        scene.idle(10);
        scene.world().flapFunnel(inputFunnelPos, true);
        ElementLink<BeltItemElement> blazePower = scene.world().createItemOnBelt(inputBeltPos, Direction.EAST, blazePowder.copy());

        scene.idle(45);
        scene.world().stallBeltItem(blazePower, true);
        scene.world().modifyBlockEntityNBT(util.select().position(chamberPos), GasInjectionChamberBlockEntity.class, tag -> tag.putInt("ProcessingTicks", 60));

        scene.idle(35);
        scene.world().modifyBlockEntity(chamberPos, GasInjectionChamberBlockEntity.class, chamber -> {
            ItemStack input = blazePowder.copy();
            Level level = chamber.getLevel();
            if (level == null || input.isEmpty()) {
                return;
            }

            SmartGasTankBehaviour tankBehaviour = BlockEntityBehaviour.get(chamber, SmartGasTankBehaviour.TYPE);
            if (tankBehaviour == null) {
                return;
            }

            GasPressureCompartment tank = tankBehaviour.getPrimaryHandler();
            new GasInjectionRecipeLookup(level, tank).findRecipeMatch(input).ifPresent(match -> {
                int batchSize = GasConsumptionPlanner.findMaximumMultiplier(match.recipe().getGasRequirement(), tank, 1);
                if (batchSize <= 0) {
                    return;
                }

                GasConsumptionPlanner.plan(match.recipe().getGasRequirement(), tank, batchSize).ifPresent(GasConsumptionPlan::execute);
            });
        });
        scene.world().removeItemsFromBelt(processingBeltPos);
        scene.world().createItemOnBeltLike(processingBeltPos, Direction.UP, windCharge.copy());
        for (int i = 0; i < random.nextInt(3, 6); i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125F);
            scene.effects().emitParticles(nozzleVec, scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, offset.add(0, Math.abs(offset.y) - offset.y, 0)), 1, 1);
        }
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, chamberObject, chamberArea, 3);

        scene.idle(3);
        chamberArea = chamberArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, chamberObject, chamberArea, 60);
        scene.overlay().showText(60).text("The Gas Injection Chamber consumes both inputs and produces the configured output").colored(PonderPalette.GREEN).pointAt(processingBeltVec).placeNearTarget().attachKeyFrame();

        scene.idle(30);
        scene.world().setKineticSpeed(beltSelection, 0);
        scene.effects().rotationSpeedIndicator(beltMotorPos);

        scene.idle(50);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, itemGaugeObject, itemGaugeArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, inputBarrelObject, inputBarrelArea, 60);
        scene.overlay().showText(60).text("The gauges track stock through the logistics network; the belt and chamber perform the recipe").colored(PonderPalette.BLUE).pointAt(inputBarrelVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setKineticSpeed(beltSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(beltMotorPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputGaugeObject, outputGaugeArea, 60);
        scene.overlay().showText(60).text("Processed output continues along the belt toward linked storage").colored(PonderPalette.OUTPUT).pointAt(processingBeltVec).placeNearTarget().attachKeyFrame();

        scene.idle(55);
        scene.world().removeItemsFromBelt(inputBeltPos);
        scene.world().flapFunnel(inputFunnelPos, false);

        scene.idle(25);
        scene.world().modifyBlockEntity(outputGauge, FactoryPanelBlockEntity.class, blockEntity -> {
            FactoryPanelBehaviour panel = blockEntity.panels.get(PanelSlot.TOP_LEFT);
            panel.satisfied = true;
            panel.bulb.setValue(1);
            blockEntity.notifyUpdate();
        });
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, outputGaugeObject, outputGaugeArea, 60);
        scene.overlay().showText(60).text("Once that output enters the logistics network, the output gauge becomes satisfied").colored(PonderPalette.GREEN).pointAt(outputGaugeVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    @SuppressWarnings("ConstantExpression")
    public static void links(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("gas_factory_gauge_links", "Redstone and Display Links");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.925F);
        scene.setSceneOffsetY(-0.35F);
        scene.showBasePlate();

        BlockPos tankPos = util.grid().at(3, 2, 4);
        BlockPos tankSupportPos = tankPos.below();
        BlockPos packagerPos = tankPos.north();
        BlockPos gaugePos = tankPos.above();
        BlockPos funnelPos = tankPos.north(2);
        BlockPos beltPos = tankPos.north(2).below();
        BlockPos redstoneLinkPos = tankPos.east(2);
        BlockPos displayLinkPos = tankPos.west(2);
        BlockPos gaugeBoardMin = tankPos.west(2).below().south();
        BlockPos gaugeBoardMax = tankPos.east(2).above().south();
        BlockPos beltStartPos = tankPos.west(3).north(2).below();
        BlockPos beltEndPos = tankPos.east(3).north(2).below();
        BlockPos motorPos = beltEndPos.north();

        Selection beltSelection = util.select().fromTo(beltStartPos, beltEndPos).add(util.select().position(motorPos));
        Selection machineSelection = util.select().position(tankSupportPos).add(util.select().fromTo(tankPos, tankPos.west())).add(util.select().fromTo(packagerPos, packagerPos.below())).add(util.select().position(funnelPos));
        Selection gaugeSelection = util.select().fromTo(gaugeBoardMin, gaugeBoardMax).add(util.select().position(gaugePos));
        Selection redstoneSelection = util.select().position(redstoneLinkPos);
        Selection displaySelection = util.select().position(displayLinkPos);

        Vec3 gaugeVec = util.vector().centerOf(gaugePos).add(0.25, 0.25, 0.4375);
        Vec3 tankVec = util.vector().centerOf(tankPos);
        Vec3 redstoneVec = util.vector().centerOf(redstoneLinkPos).add(0, 0, 0.40625);
        Vec3 displayVec = util.vector().centerOf(displayLinkPos).add(0, 0, 0.21875);

        AABB gaugeArea = new AABB(gaugeVec, gaugeVec);
        AABB redstoneArea = new AABB(redstoneVec, redstoneVec);
        AABB displayArea = new AABB(displayVec, displayVec);

        Object gaugeObject = new Object();
        Object redstoneObject = new Object();
        Object displayObject = new Object();

        int targetAmount = 1000;
        int lowAmount = 300;
        int promisedAmount = 500;
        int mediumSpeed = 48;

        ItemStack gasToken = VirtualGasItems.createVirtualItem(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        ItemStack inFlightBalloon = BalloonFactory.createOrdered(new GasStack(CCBGases.NATURAL_AIR.get(), promisedAmount), "Gas Workshop", RESTOCK_ORDER_ID + 1, 0, true, 0, true, null);
        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        scene.idle(20);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);
        scene.world().showSection(beltSelection, Direction.SOUTH);

        scene.idle(15);
        scene.world().setKineticSpeed(beltSelection, mediumSpeed);

        scene.idle(20);
        scene.world().modifyBlockEntity(gaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
            gauge.addPanel(PanelSlot.TOP_LEFT, PONDER_NETWORK);
            gauge.restocker = true;
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            panel.setFilter(gasToken.copyWithCount(1));
            panel.count = targetAmount;
            panel.upTo = true;
            panel.recipeAddress = "Gas Workshop";
            panel.network = PONDER_NETWORK;
            panel.satisfied = false;
            panel.promisedSatisfied = false;
            panel.redstonePowered = false;
            gauge.redraw = true;
            gauge.notifyUpdate();
        });
        scene.world().modifyBlockEntity(tankPos, AirtightTankBlockEntity.class, tank -> tank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), lowAmount)).requireAccepted());
        scene.world().modifyBlockEntity(gaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            if (panel == null || !panel.isActive()) {
                return;
            }

            int stored = panel.getLevelInStorage();
            int promised = panel.getPromised();
            int target = panel.getAmount();
            panel.satisfied = stored >= target;
            panel.promisedSatisfied = stored + promised >= target;
            panel.bulb.setValue(panel.redstonePowered || panel.satisfied ? 1 : 0);
            gauge.redraw = true;
            gauge.notifyUpdate();
        });
        scene.world().showSection(machineSelection, Direction.EAST);
        scene.world().showSection(gaugeSelection, Direction.SOUTH);
        scene.world().showSection(redstoneSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().showControls(gaugeVec, Pointing.DOWN, 60).showing(AllIcons.I_ADD);
        addPanelConnection(scene, gaugePos, redstoneLinkPos, 1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, gaugeObject, gaugeArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, redstoneObject, redstoneArea, 3);

        scene.idle(3);
        gaugeArea = gaugeArea.inflate(0.25, 0.25, 0.0625);
        redstoneArea = redstoneArea.inflate(0.375, 0.375, 0.09375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, gaugeObject, gaugeArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, redstoneObject, redstoneArea, 60);
        scene.overlay().showText(60).text("Add a Redstone Link from the gauge's connection UI").colored(PonderPalette.BLUE).pointAt(gaugeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, gaugeObject, gaugeArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, redstoneObject, redstoneArea, 60);
        scene.overlay().showText(60).text("Once its target is met, the gauge can power the connected Redstone Link").colored(PonderPalette.GREEN).pointAt(redstoneVec).placeNearTarget().attachKeyFrame();

        scene.idle(10);
        scene.world().modifyBlockEntity(tankPos, AirtightTankBlockEntity.class, tank -> tank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), targetAmount)).requireAccepted());
        scene.world().modifyBlockEntity(gaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            if (panel == null || !panel.isActive()) {
                return;
            }

            int stored = panel.getLevelInStorage();
            int promised = panel.getPromised();
            int target = panel.getAmount();
            panel.satisfied = stored >= target;
            panel.promisedSatisfied = stored + promised >= target;
            panel.bulb.setValue(panel.redstonePowered || panel.satisfied ? 1 : 0);
            gauge.redraw = true;
            gauge.notifyUpdate();
        });
        scene.world().toggleRedstonePower(util.select().position(redstoneLinkPos));
        scene.effects().indicateRedstone(redstoneLinkPos);

        scene.idle(70);
        scene.world().toggleRedstonePower(util.select().position(redstoneLinkPos));
        scene.world().modifyBlockEntity(tankPos, AirtightTankBlockEntity.class, tank -> tank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), lowAmount)).requireAccepted());
        scene.world().modifyBlockEntity(gaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            panel.restockerPromises.add(new RequestPromise(new BigItemStack(gasToken.copyWithCount(1), promisedAmount)));
            panel.promisedSatisfied = panel.getLevelInStorage() + panel.getPromised() >= panel.getAmount();
            gauge.notifyUpdate();
        });
        scene.world().modifyBlockEntity(gaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            if (panel == null || !panel.isActive()) {
                return;
            }

            int stored = panel.getLevelInStorage();
            int promised = panel.getPromised();
            int target = panel.getAmount();
            panel.satisfied = stored >= target;
            panel.promisedSatisfied = stored + promised >= target;
            panel.bulb.setValue(panel.redstonePowered || panel.satisfied ? 1 : 0);
            gauge.redraw = true;
            gauge.notifyUpdate();
        });

        scene.overlay().showControls(redstoneVec, Pointing.RIGHT, 60).rightClick().withItem(wrenchItem.copy());
        scene.overlay().showText(60).text("Wrench the Redstone Link into receiver mode to control the gauge instead").colored(PonderPalette.BLUE).pointAt(redstoneVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().cycleBlockProperty(redstoneLinkPos, RedstoneLinkBlock.RECEIVER);

        scene.idle(73);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, gaugeObject, gaugeArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, redstoneObject, redstoneArea, 60);
        scene.overlay().showText(60).text("While the receiver is powered, the gauge pauses new restocking requests").colored(PonderPalette.RED).pointAt(gaugeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().modifyBlockEntity(gaugePos, FactoryPanelBlockEntity.class, blockEntity -> {
            FactoryPanelBehaviour panel = blockEntity.panels.get(PanelSlot.TOP_LEFT);
            panel.redstonePowered = true;
            panel.bulb.setValue(1);
            blockEntity.notifyUpdate();
        });
        scene.world().toggleRedstonePower(util.select().position(redstoneLinkPos));
        scene.effects().indicateRedstone(redstoneLinkPos);

        scene.idle(73);
        scene.overlay().showText(60).text("Power only pauses new requests; promised gas already in transit still arrives").colored(PonderPalette.BLUE).pointAt(tankVec).placeNearTarget().attachKeyFrame();

        scene.idle(5);
        scene.world().createItemOnBelt(beltPos.east(2), Direction.EAST, inFlightBalloon.copy());

        scene.idle(25);
        scene.world().removeItemsFromBelt(beltPos);
        scene.world().flapFunnel(funnelPos, false);
        scene.world().modifyBlockEntity(packagerPos, GasPackagerBlockEntity.class, packager -> packager.inventory.insertItem(0, inFlightBalloon.copy(), false));

        scene.idle(25);
        scene.world().modifyBlockEntity(gaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            if (panel == null || !panel.isActive()) {
                return;
            }

            int stored = panel.getLevelInStorage();
            int promised = panel.getPromised();
            int target = panel.getAmount();
            panel.satisfied = stored >= target;
            panel.promisedSatisfied = stored + promised >= target;
            panel.bulb.setValue(panel.redstonePowered || panel.satisfied ? 1 : 0);
            gauge.redraw = true;
            gauge.notifyUpdate();
        });

        scene.idle(25);
        scene.world().showSection(displaySelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().showControls(gaugeVec, Pointing.DOWN, 60).showing(AllIcons.I_ADD);
        addPanelConnection(scene, gaugePos, displayLinkPos, 1);
        scene.world().modifyBlockEntity(gaugePos, GasFactoryGaugeBlockEntity.class, gauge -> {
            FactoryPanelBehaviour panel = gauge.panels.get(PanelSlot.TOP_LEFT);
            if (panel == null) {
                return;
            }

            FactoryPanelConnection connection = panel.targetedByLinks.get(displayLinkPos);
            if (connection != null) {
                connection.arrowBendMode = 2;
            }
            gauge.notifyUpdate();
        });
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, displayObject, displayArea, 3);

        scene.idle(3);
        displayArea = displayArea.inflate(0.4375, 0.4375, 0.28125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, gaugeObject, gaugeArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, displayObject, displayArea, 60);
        scene.overlay().showText(60).text("A Display Link can report the gauge's stored gas and target status").colored(PonderPalette.BLUE).pointAt(displayVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    private static void createRequestedBalloon(CreateSceneBuilder scene, BlockPos packagerPos, BlockPos leverPos, Selection leverSelection, List<PackagingRequest> requests, BlockPos funnelPos, BlockPos beltPos, ItemStack expectedBalloon) {
        scene.world().toggleRedstonePower(leverSelection);
        scene.effects().indicateRedstone(leverPos);
        scene.world().modifyBlock(packagerPos, state -> state.setValue(GasPackagerBlock.POWERED, true), false);
        scene.world().modifyBlockEntity(packagerPos, GasPackagerBlockEntity.class, packager -> {
            packager.attemptToSend(requests);
            packager.heldBox = expectedBalloon.copy();
            packager.animationInward = false;
            packager.animationTicks = PackagerBlockEntity.CYCLE;
            packager.notifyUpdate();
        });

        scene.idle(25);
        scene.world().flapFunnel(funnelPos, true);
        scene.world().createItemOnBelt(beltPos, Direction.EAST, expectedBalloon.copy());
        scene.world().modifyBlockEntity(packagerPos, GasPackagerBlockEntity.class, packager -> packager.inventory.extractItem(0, 1, false));
        scene.world().toggleRedstonePower(leverSelection);
        scene.effects().indicateRedstone(leverPos);
        scene.world().modifyBlock(packagerPos, state -> state.setValue(GasPackagerBlock.POWERED, false), false);
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

    private static void addPanelConnection(CreateSceneBuilder scene, BlockPos targetGauge, BlockPos input, int amount) {
        scene.world().modifyBlockEntity(targetGauge, FactoryPanelBlockEntity.class, blockEntity -> {
            FactoryPanelBehaviour panel = blockEntity.panels.get(PanelSlot.TOP_LEFT);
            panel.addConnection(new FactoryPanelPosition(input, PanelSlot.TOP_LEFT));
            FactoryPanelPosition from = new FactoryPanelPosition(input, PanelSlot.TOP_LEFT);
            FactoryPanelConnection connection = panel.targetedBy.get(from);
            if (connection == null) {
                connection = panel.targetedByLinks.get(input);
            }
            if (connection != null) {
                connection.amount = amount;
                connection.arrowBendMode = 0;
            }
            blockEntity.notifyUpdate();
        });
    }
}
