package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasTags;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberFilterItem;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionFanCost;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipeLookup;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlanner;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBGasTags;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionChamberScenes {
    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        RandomSource random = RandomSource.create();

        scene.title("gas_injection_chamber_processing", "Processing Items with a Gas Injection Chamber");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos targetPos = util.grid().at(3, 1, 3);
        BlockPos chamberPos = targetPos.above(2);
        BlockPos lowerPipePos = chamberPos.above();
        BlockPos upperPipePos = chamberPos.above(3);

        Selection targetSelection = util.select().position(targetPos);
        Selection chamberSelection = util.select().position(chamberPos);
        Selection pipeSelection = util.select().fromTo(lowerPipePos, upperPipePos);

        Vec3 targetVec = util.vector().centerOf(targetPos);
        Vec3 chamberVec = util.vector().centerOf(chamberPos);
        Vec3 upperPipeVec = util.vector().centerOf(upperPipePos);
        Vec3 nozzleVec = chamberVec.subtract(0, 1.6875, 0);

        ItemStack blazePowder = new ItemStack(Items.BLAZE_POWDER);
        ItemStack windCharge = new ItemStack(Items.WIND_CHARGE);
        ItemStack gasCanister = new ItemStack(CCBItems.GAS_CANISTER.get());
        ItemStack filledGasCanister = gasCanister.copy();
        filledGasCanister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.NATURAL_AIR.get(), 4000));

        Object gasFlowObject = new Object();

        AABB gasArea = new AABB(upperPipeVec, upperPipeVec);

        scene.idle(20);
        scene.world().showSection(targetSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(chamberSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(pipeSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, gasFlowObject, gasArea, 3);

        scene.idle(3);
        gasArea = gasArea.inflate(0.375, 0.5, 0.375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, gasFlowObject, gasArea, 3);

        scene.idle(3);
        gasArea = gasArea.expandTowards(0, -2, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, gasFlowObject, gasArea, 60);
        scene.overlay().showText(60).text("Only the chamber's top face can connect to a gas network").colored(PonderPalette.RED).pointAt(util.vector().centerOf(lowerPipePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Incoming gas is stored in the chamber's internal gas tank").pointAt(chamberVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Matching Gas Injection recipes can process items on Depots or Belts two blocks below").colored(PonderPalette.GREEN).pointAt(targetVec).placeNearTarget().attachKeyFrame();

        scene.idle(15);
        scene.world().createItemOnBeltLike(targetPos, Direction.NORTH, blazePowder.copyWithCount(16));

        scene.idle(65);
        scene.overlay().showText(60).text("When a matching item is held there, the nozzle injects gas...").colored(PonderPalette.GREEN).pointAt(chamberVec).placeNearTarget().attachKeyFrame();
        scene.world().modifyBlockEntityNBT(chamberSelection, GasInjectionChamberBlockEntity.class, compoundTag -> compoundTag.putInt("ProcessingTicks", 60));

        scene.idle(20);
        scene.world().modifyBlockEntity(chamberPos, GasInjectionChamberBlockEntity.class, chamber -> {
            ItemStack input = blazePowder.copyWithCount(16);
            Level level = chamber.getLevel();
            if (level == null || input.isEmpty()) {
                return;
            }

            GasPressureCompartment tank = getGasTank(chamber);
            new GasInjectionRecipeLookup(level, tank).findRecipeMatch(input).ifPresent(match -> {
                int desiredCount = Math.min(input.getCount(), input.getMaxStackSize());
                int batchSize = GasConsumptionPlanner.findMaximumMultiplier(match.recipe().getGasRequirement(), tank, desiredCount);
                if (batchSize <= 0) {
                    return;
                }

                GasConsumptionPlanner.plan(match.recipe().getGasRequirement(), tank, batchSize).ifPresent(GasConsumptionPlan::execute);
            });
        });
        scene.world().removeItemsFromBelt(targetPos);
        scene.world().createItemOnBeltLike(targetPos, Direction.UP, windCharge.copyWithCount(32));
        emitCloud(scene, nozzleVec, random);
        scene.effects().indicateSuccess(targetPos);

        scene.idle(40);
        scene.overlay().showText(60).text("...then returns the processed result to the Depot or Belt").colored(PonderPalette.GREEN).pointAt(targetVec).placeNearTarget().attachKeyFrame();

        scene.idle(70);
        scene.world().removeItemsFromBelt(targetPos);
        scene.world().createItemOnBeltLike(targetPos, Direction.NORTH, gasCanister.copy());
        scene.world().modifyBlockEntity(chamberPos, GasInjectionChamberBlockEntity.class, chamber -> getGasTank(chamber).fill(new GasStack(CCBGases.NATURAL_AIR.get(), 40000), GasAction.EXECUTE));
        scene.overlay().showText(60).text("Compatible Gas Canisters can also be filled beneath the chamber").colored(PonderPalette.BLUE).pointAt(targetVec).placeNearTarget().attachKeyFrame();

        scene.idle(70);
        scene.overlay().showText(60).text("Gas transfers only while the chamber's pressure is higher than the canister's").colored(PonderPalette.RED).pointAt(targetVec).placeNearTarget().attachKeyFrame();
        scene.world().modifyBlockEntityNBT(chamberSelection, GasInjectionChamberBlockEntity.class, compoundTag -> compoundTag.putInt("ProcessingTicks", 60));

        scene.idle(20);
        scene.world().modifyBlockEntity(chamberPos, GasInjectionChamberBlockEntity.class, chamber -> {
            GasPressureCompartment tank = getGasTank(chamber);
            GasStack gas = tank.getGasStack();
            if (!gas.isEmpty()) {
                tank.drain(gas.copyWithAmount(Math.min(4000, gas.getAmount())), GasAction.EXECUTE);
            }
        });
        scene.world().removeItemsFromBelt(targetPos);
        scene.world().createItemOnBeltLike(targetPos, Direction.UP, filledGasCanister.copy());
        emitCloud(scene, nozzleVec, random);
        scene.effects().indicateSuccess(targetPos);

        scene.idle(40);
        scene.markAsFinished();
    }

    public static void filtering(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        RandomSource random = RandomSource.create();

        scene.title("gas_injection_chamber_filtering", "Reproducing Bulk Processing with a Gas Injection Chamber");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos targetPos = util.grid().at(3, 1, 3);
        BlockPos chamberPos = targetPos.above(2);
        BlockPos lowerPipePos = chamberPos.above();
        BlockPos upperPipePos = chamberPos.above(3);
        BlockPos fanPos = targetPos.west(3);
        BlockPos coolerPos = targetPos.west(2);

        Selection targetSelection = util.select().position(targetPos);
        Selection chamberSelection = util.select().position(chamberPos);
        Selection pipeSelection = util.select().fromTo(lowerPipePos, upperPipePos);
        Selection fanSelection = util.select().position(fanPos);
        Selection coolerSelection = util.select().position(coolerPos);

        Vec3 targetVec = util.vector().centerOf(targetPos);
        Vec3 chamberVec = util.vector().centerOf(chamberPos);
        Vec3 undersideVec = util.vector().blockSurface(chamberPos, Direction.DOWN).add(0, 0.0625, 0);
        Vec3 nozzleVec = chamberVec.subtract(0, 1.6875, 0);

        ItemStack blankFilter = new ItemStack(CCBItems.GAS_INJECTION_CHAMBER_FILTER.get());
        ItemStack configuredFilter = GasInjectionChamberFilterItem.getFanProcessingType(Create.asResource("blasting")).map(type -> GasInjectionChamberFilterItem.create(blankFilter, type)).orElse(blankFilter.copy());
        ItemStack rawIron = new ItemStack(Items.RAW_IRON);
        ItemStack ironIngot = new ItemStack(Items.IRON_INGOT);

        float fastSpeed = SpeedLevel.FAST.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(targetSelection, Direction.DOWN);
        scene.world().showSection(fanSelection, Direction.DOWN);
        scene.world().showSection(coolerSelection, Direction.DOWN);

        scene.idle(15);
        scene.world().setKineticSpeed(fanSelection, fastSpeed);
        scene.effects().rotationSpeedIndicator(fanPos);

        scene.idle(20);
        scene.world().createItemOnBeltLike(targetPos, Direction.NORTH, blankFilter.copy());
        scene.overlay().showText(60).text("A blank Gas Injection Chamber Filter records the Bulk Processing effect it passes through").colored(PonderPalette.BLUE).pointAt(targetVec).placeNearTarget().attachKeyFrame();

        scene.idle(25);
        scene.world().removeItemsFromBelt(targetPos);
        scene.world().createItemOnBeltLike(targetPos, Direction.UP, configuredFilter.copy());
        scene.effects().indicateSuccess(targetPos);

        scene.idle(50);
        scene.world().hideSection(fanSelection, Direction.WEST);
        scene.world().hideSection(coolerSelection, Direction.WEST);

        scene.idle(20);
        scene.world().showSection(chamberSelection, Direction.DOWN);
        scene.world().showSection(pipeSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().removeItemsFromBelt(targetPos);

        scene.overlay().showText(60).text("Right-click the underside of the chamber with the configured filter to install it").colored(PonderPalette.BLUE).pointAt(undersideVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(undersideVec, Pointing.UP, 60).rightClick().withItem(configuredFilter.copy());

        scene.idle(7);
        scene.world().modifyBlockEntityNBT(chamberSelection, GasInjectionChamberBlockEntity.class, compoundTag -> compoundTag.put("InstalledFilter", configuredFilter.copy().saveOptional(scene.world().getHolderLookupProvider())));

        scene.idle(73);
        scene.overlay().showText(60).text("The chamber can then reproduce that Bulk Processing effect on compatible items below").colored(PonderPalette.GREEN).pointAt(targetVec).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.world().createItemOnBeltLike(targetPos, Direction.NORTH, rawIron.copyWithCount(16));

        scene.idle(60);
        scene.overlay().showText(60).text("Reproduced Bulk Processing consumes gas for each item processed").colored(PonderPalette.RED).pointAt(chamberVec).placeNearTarget().attachKeyFrame();
        scene.world().modifyBlockEntityNBT(chamberSelection, GasInjectionChamberBlockEntity.class, compoundTag -> compoundTag.putInt("ProcessingTicks", 60));

        scene.idle(35);
        scene.world().modifyBlockEntity(chamberPos, GasInjectionChamberBlockEntity.class, chamber -> {
            GasPressureCompartment tank = getGasTank(chamber);
            GasStack gas = tank.getGasStack();
            if (gas.isEmpty() || GasTags.isTag(gas, CCBGasTags.CREATIVE.tag)) {
                return;
            }

            int efficiencyDivisor = GasTags.isTag(gas, CCBGasTags.ENERGIZED.tag) ? 5 : 1;
            efficiencyDivisor *= GasInjectionFanCost.getFanProcessingPressureEfficiencyDivisor(tank.getPressurePa());
            long baseCost = CCBConfig.server().machines.gasInjectionChamber.fanProcessingGasPerItem.get() * 16L;
            long gasCost = baseCost / efficiencyDivisor + (baseCost % efficiencyDivisor == 0 ? 0 : 1);
            if (gasCost <= 0) {
                return;
            }

            tank.drain(gas.copyWithAmount(gasCost), GasAction.EXECUTE);
        });
        scene.world().removeItemsFromBelt(targetPos);
        scene.world().createItemOnBeltLike(targetPos, Direction.UP, ironIngot.copyWithCount(16));
        emitCloud(scene, nozzleVec, random);
        scene.effects().indicateSuccess(targetPos);

        scene.idle(45);
        scene.overlay().showText(60).text("When the filter is idle, empty-hand right-click the underside to retrieve it").colored(PonderPalette.BLUE).pointAt(undersideVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(undersideVec, Pointing.UP, 60).rightClick();

        scene.idle(7);
        scene.world().modifyBlockEntityNBT(chamberSelection, GasInjectionChamberBlockEntity.class, compoundTag -> compoundTag.remove("InstalledFilter"));

        scene.idle(53);
        scene.markAsFinished();
    }

    public static void basin(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        RandomSource random = RandomSource.create();

        scene.title("gas_injection_chamber_basin", "Injecting Gas into Fluids");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos basinPos = util.grid().at(3, 1, 3);
        BlockPos chamberPos = basinPos.above(2);
        BlockPos lowerPipePos = chamberPos.above();
        BlockPos upperPipePos = chamberPos.above(3);

        Selection basinSelection = util.select().position(basinPos);
        Selection chamberSelection = util.select().position(chamberPos);
        Selection pipeSelection = util.select().fromTo(lowerPipePos, upperPipePos);

        Vec3 basinVec = util.vector().centerOf(basinPos);
        Vec3 chamberVec = util.vector().centerOf(chamberPos);
        Vec3 nozzleVec = chamberVec.subtract(0, 1.6875, 0);
        Vec3 basinFilterVec = util.vector().blockSurface(basinPos, Direction.NORTH).add(0, 0.25, 0);
        Vec3 upperPipeVec = util.vector().centerOf(upperPipePos);

        ItemStack lavaBucket = new ItemStack(Items.LAVA_BUCKET);
        ItemStack brimstoneBucket = new ItemStack(CCBFluids.BRIMSTONE.getBucket().orElseThrow());
        ItemStack energizedUltrawarmAirCanister = new ItemStack(CCBItems.GAS_CANISTER.get());
        energizedUltrawarmAirCanister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), 1));

        scene.idle(20);
        scene.world().showSection(basinSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(chamberSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(pipeSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("A Basin exactly two blocks below enables fluid Gas Injection recipes").colored(PonderPalette.GREEN).pointAt(basinVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showFilterSlotInput(basinFilterVec, Direction.NORTH, 60);
        scene.overlay().showControls(basinFilterVec, Pointing.RIGHT, 60).rightClick().withItem(brimstoneBucket.copy());
        scene.world().modifyBlockEntity(basinPos, BasinBlockEntity.class, blockEntity -> blockEntity.getFilter().setFilter(brimstoneBucket.copy()));
        scene.overlay().showText(60).text("A Basin filter can restrict processing to a specific fluid output").colored(PonderPalette.BLUE).pointAt(basinFilterVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Matching fluid and gas are processed together, leaving the resulting fluid in the Basin").colored(PonderPalette.GREEN).pointAt(chamberVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(basinVec, Pointing.LEFT, 60).withItem(lavaBucket.copy());
        scene.overlay().showControls(upperPipeVec, Pointing.DOWN, 60).withItem(energizedUltrawarmAirCanister.copy());

        scene.idle(10);
        scene.world().modifyBlockEntity(basinPos, BasinBlockEntity.class, blockEntity -> blockEntity.inputTank.getCapability().fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE));
        scene.world().modifyBlockEntityNBT(chamberSelection, GasInjectionChamberBlockEntity.class, compoundTag -> compoundTag.putInt("ProcessingTicks", 60));

        scene.idle(25);
        scene.world().modifyBlockEntity(chamberPos, GasInjectionChamberBlockEntity.class, chamber -> {
            Level level = chamber.getLevel();
            if (level == null || !(level.getBlockEntity(basinPos) instanceof BasinBlockEntity basin)) {
                return;
            }

            GasPressureCompartment tank = getGasTank(chamber);
            new GasInjectionRecipeLookup(level, tank).findFluidRecipeMatch(basin.inputTank.getCapability()).flatMap(match -> GasConsumptionPlanner.plan(match.recipe().getGasRequirement(), tank)).ifPresent(GasConsumptionPlan::execute);
        });
        scene.world().modifyBlockEntity(basinPos, BasinBlockEntity.class, blockEntity -> {
            IFluidHandler fluidHandler = blockEntity.inputTank.getCapability();
            fluidHandler.drain(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE);
            fluidHandler.fill(new FluidStack(CCBFluids.BRIMSTONE, 100), FluidAction.EXECUTE);
        });
        emitCloud(scene, nozzleVec, random);
        scene.effects().indicateSuccess(basinPos);

        scene.idle(55);
        scene.markAsFinished();
    }

    private static GasPressureCompartment getGasTank(GasInjectionChamberBlockEntity chamber) {
        SmartGasTankBehaviour tankBehaviour = BlockEntityBehaviour.get(chamber, SmartGasTankBehaviour.TYPE);
        if (tankBehaviour == null) {
            throw new IllegalStateException("Gas injection chamber at " + chamber.getBlockPos() + " is missing its gas tank behaviour.");
        }

        return tankBehaviour.getPrimaryHandler();
    }

    private static void emitCloud(CreateSceneBuilder scene, Vec3 nozzleVec, RandomSource random) {
        for (int i = 0; i < random.nextInt(3, 6); i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125F);
            scene.effects().emitParticles(nozzleVec, scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(offset.x, Math.abs(offset.y), offset.z)), 1, 1);
        }
    }
}
