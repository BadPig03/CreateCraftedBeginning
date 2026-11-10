package net.ty.createcraftedbeginning.ponder.scenes.opticalpower;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver.PhotothermalHeatState;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver.PhotothermalReceiverBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver.PhotothermalReceiverPort;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver.PhotothermalReceiverSerialization;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PhotothermalReceiverScenes {
    private PhotothermalReceiverScenes() {
    }

    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("photothermal_receiver", "Heating with Lasers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos receiverPos = util.grid().at(4, 1, 3);
        BlockPos basinPos = receiverPos.above();
        BlockPos mixerPos = receiverPos.above(3);
        BlockPos motorPos = receiverPos.east().above(2);
        BlockPos cogPos = receiverPos.east().above(3);
        BlockPos sideEmitterPos = receiverPos.west(4);
        BlockPos frontEmitterPos = receiverPos.north(3);

        Selection receiverSelection = util.select().position(receiverPos);
        Selection basinSelection = util.select().position(basinPos);
        Selection mixerSelection = util.select().position(mixerPos);
        Selection driveSelection = util.select().position(mixerPos).add(util.select().fromTo(motorPos, cogPos));
        Selection sourceSelection = util.select().position(sideEmitterPos).add(util.select().position(frontEmitterPos));

        Vec3 receiverVec = util.vector().centerOf(receiverPos);
        Vec3 basinVec = util.vector().centerOf(basinPos);
        Vec3 sideEmitterVec = util.vector().topOf(sideEmitterPos);
        Vec3 westVec = util.vector().blockSurface(receiverPos, Direction.WEST);
        Vec3 eastVec = util.vector().blockSurface(receiverPos, Direction.EAST);
        Vec3 northVec = util.vector().blockSurface(receiverPos, Direction.NORTH);
        Vec3 southVec = util.vector().blockSurface(receiverPos, Direction.SOUTH);
        Vec3 bottomVec = util.vector().blockSurface(receiverPos, Direction.DOWN);

        AABB heatingArea = new AABB(receiverPos).expandTowards(0, 1, 0);
        AABB westArea = new AABB(westVec, westVec).inflate(0.04, 0.125, 0.125);
        AABB eastArea = new AABB(eastVec, eastVec).inflate(0.04, 0.125, 0.125);
        AABB northArea = new AABB(northVec, northVec).inflate(0.125, 0.125, 0.04);
        AABB southArea = new AABB(southVec, southVec).inflate(0.125, 0.125, 0.04);
        AABB bottomArea = new AABB(bottomVec, bottomVec).inflate(0.125, 0.04, 0.125);
        AABB sideSourceArea = new AABB(sideEmitterPos);
        AABB frontSourceArea = new AABB(frontEmitterPos);
        AABB receiverArea = new AABB(receiverPos);

        Object heatingObject = new Object();
        Object westObject = new Object();
        Object eastObject = new Object();
        Object northObject = new Object();
        Object southObject = new Object();
        Object bottomObject = new Object();
        Object sideSourceObject = new Object();
        Object frontSourceObject = new Object();
        Object receiverObject = new Object();

        ItemStack copperIngot = new ItemStack(Items.COPPER_INGOT);
        ItemStack zincIngot = new ItemStack(AllItems.ZINC_INGOT.get());
        ItemStack brassIngots = new ItemStack(AllItems.BRASS_INGOT.get(), 2);
        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE);
        ItemStack lavaBucket = new ItemStack(Items.LAVA_BUCKET);

        ResourceLocation brassRecipe = ResourceLocation.fromNamespaceAndPath("create", "mixing/brass_ingot");
        ResourceLocation lavaRecipe = ResourceLocation.fromNamespaceAndPath("create", "mixing/lava_from_cobble");
        PhotothermalHeatState heatState = new PhotothermalHeatState();
        int westPort = 1 << PhotothermalReceiverPort.WEST.ordinal();
        int bothPorts = westPort | 1 << PhotothermalReceiverPort.NORTH.ordinal();

        scene.idle(20);
        scene.world().showSection(receiverSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Photothermal Receivers use laser light to heat machines directly above them").colored(PonderPalette.GREEN).pointAt(receiverVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, heatingObject, receiverArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, heatingObject, heatingArea, 57);

        scene.idle(4);
        scene.world().showSection(basinSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().showSection(driveSelection, Direction.DOWN);

        scene.idle(15);
        scene.world().setKineticSpeed(driveSelection, 64);
        scene.world().setKineticSpeed(mixerSelection, -64);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(5);

        scene.idle(33);
        scene.overlay().showText(60).text("Aim the laser at a receiving port on the sides or underneath").colored(PonderPalette.BLUE).pointAt(westVec).placeNearTarget().attachKeyFrame();
        scene.world().showSection(sourceSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().modifyBlockEntity(sideEmitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(1));
        scene.world().modifyBlockEntity(frontEmitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(1));
        setReceiverState(scene, receiverPos, heatState, 0, 2, bothPorts);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, westObject, westArea, 40);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, eastObject, eastArea, 40);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, northObject, northArea, 40);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, southObject, southArea, 40);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, bottomObject, bottomArea, 40);

        advanceHeat(scene, receiverPos, heatState, 0, 2, bothPorts, 40);
        scene.world().modifyBlockEntity(sideEmitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(0));
        scene.world().modifyBlockEntity(frontEmitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(0));
        setReceiverState(scene, receiverPos, heatState, 0, 0, 0);

        scene.idle(40);
        scene.overlay().showText(60).text("Keep the laser shining on the receiver; once warmed up, it can heat the Basin").colored(PonderPalette.GREEN).pointAt(sideEmitterVec).placeNearTarget().attachKeyFrame();
        scene.world().modifyBlockEntity(sideEmitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(16));
        scene.world().modifyBlockEntity(basinPos, BasinBlockEntity.class, basin -> {
            basin.inputInventory.setStackInSlot(0, copperIngot.copy());
            basin.inputInventory.setStackInSlot(1, zincIngot.copy());
        });
        scene.overlay().showControls(basinVec, Pointing.LEFT, 60).withItem(copperIngot.copy());
        scene.overlay().showControls(basinVec, Pointing.RIGHT, 60).withItem(zincIngot.copy());
        setReceiverState(scene, receiverPos, heatState, 100, 16, westPort);
        advanceHeat(scene, receiverPos, heatState, 100, 16, westPort, 34);
        scene.world().modifyBlockEntity(mixerPos, MechanicalMixerBlockEntity.class, mixer -> {
            mixer.startProcessingBasin();
            mixer.processingTicks = 20;
        });

        advanceHeat(scene, receiverPos, heatState, 100, 16, westPort, 40);
        applyRecipe(scene, basinPos, brassRecipe);
        scene.overlay().showControls(basinVec, Pointing.RIGHT, 20).withItem(brassIngots.copy());

        advanceHeat(scene, receiverPos, heatState, 100, 16, westPort, 46);
        scene.overlay().showText(60).text("Multiple lasers can supply light to the receiver together").colored(PonderPalette.BLUE).pointAt(receiverVec).placeNearTarget().attachKeyFrame();
        scene.world().modifyBlockEntity(frontEmitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(16));
        setReceiverState(scene, receiverPos, heatState, 100, 32, bothPorts);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, sideSourceObject, sideSourceArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, frontSourceObject, frontSourceArea, 60);
        advanceHeat(scene, receiverPos, heatState, 100, 32, bothPorts, 80);

        scene.overlay().showText(60).text("More light does not always mean a higher heating level").colored(PonderPalette.BLUE).pointAt(receiverVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, receiverObject, receiverArea, 60);

        advanceHeat(scene, receiverPos, heatState, 100, 32, bothPorts, 20);
        scene.world().modifyBlockEntity(basinPos, BasinBlockEntity.class, basin -> {
            basin.clearContent();
            basin.inputInventory.setStackInSlot(0, cobblestone.copy());
        });
        scene.overlay().showControls(basinVec, Pointing.RIGHT, 53).withItem(cobblestone.copy());

        advanceHeat(scene, receiverPos, heatState, 100, 32, bothPorts, 60);
        scene.overlay().showText(60).text("Use stronger laser light and let the receiver warm up for superheated recipes").colored(PonderPalette.GREEN).pointAt(sideEmitterVec).placeNearTarget().attachKeyFrame();
        scene.world().modifyBlockEntity(frontEmitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(0));
        scene.world().modifyBlockEntity(sideEmitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(48));
        setReceiverState(scene, receiverPos, heatState, 300, 48, westPort);
        advanceHeat(scene, receiverPos, heatState, 300, 48, westPort, 67);
        scene.world().modifyBlockEntity(mixerPos, MechanicalMixerBlockEntity.class, mixer -> {
            mixer.startProcessingBasin();
            mixer.processingTicks = 20;
        });

        advanceHeat(scene, receiverPos, heatState, 300, 48, westPort, 40);
        applyRecipe(scene, basinPos, lavaRecipe);
        scene.overlay().showControls(basinVec, Pointing.RIGHT, 40).withItem(lavaBucket.copy());

        advanceHeat(scene, receiverPos, heatState, 300, 48, westPort, 66);
        scene.overlay().showText(60).text("After the laser is switched off, the receiver keeps heating briefly as it cools down").colored(PonderPalette.RED).pointAt(receiverVec).placeNearTarget().attachKeyFrame();
        scene.world().modifyBlockEntity(sideEmitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(0));
        scene.world().modifyBlockEntity(frontEmitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(0));
        setReceiverState(scene, receiverPos, heatState, 0, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, receiverObject, receiverArea, 60);
        scene.world().modifyBlockEntity(basinPos, BasinBlockEntity.class, basin -> {
            basin.clearContent();
            basin.getTanks().getSecond().getPrimaryHandler().drain(50, FluidAction.EXECUTE);
        });
        advanceHeat(scene, receiverPos, heatState, 0, 0, 0, 60);
        scene.markAsFinished();
    }

    private static void advanceHeat(CreateSceneBuilder scene, BlockPos receiverPos, PhotothermalHeatState heatState, int targetProgress, double fluxLm, int ports, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            scene.idle(1);
            heatState.tick(targetProgress, fluxLm * 0.05, fluxLm);
            setReceiverState(scene, receiverPos, heatState, targetProgress, fluxLm, ports);
        }
    }

    private static void setReceiverState(CreateSceneBuilder scene, BlockPos receiverPos, PhotothermalHeatState heatState, int targetProgress, double fluxLm, int ports) {
        CompoundTag snapshot = new CompoundTag();
        heatState.write(snapshot);
        HeatLevel heatLevel = switch (heatState.getHeatLevel()) {
            case 1 -> HeatLevel.KINDLED;
            case 2 -> HeatLevel.SEETHING;
            default -> HeatLevel.NONE;
        };
        scene.world().modifyBlock(receiverPos, state -> state.setValue(BlazeBurnerBlock.HEAT_LEVEL, heatLevel), false);
        scene.world().modifyBlockEntity(receiverPos, PhotothermalReceiverBlockEntity.class, receiver -> {
            CompoundTag tag = snapshot.copy();
            PhotothermalReceiverSerialization.writeClientState(tag, targetProgress, ports, fluxLm, fluxLm, fluxLm);
            receiver.handleUpdateTag(tag, scene.world().getHolderLookupProvider());
        });
    }

    private static void applyRecipe(CreateSceneBuilder scene, BlockPos basinPos, ResourceLocation recipeId) {
        scene.addInstruction(ponder -> {
            PonderLevel world = ponder.getWorld();
            if (!(world.getBlockEntity(basinPos) instanceof BasinBlockEntity basin)) {
                throw new IllegalStateException("Missing Ponder basin at " + basinPos + '.');
            }

            Recipe<?> recipe = world.getRecipeManager().byKey(recipeId).orElseThrow(() -> new IllegalStateException("Missing Ponder mixing recipe '" + recipeId + "'.")).value();
            if (BasinRecipe.apply(basin, recipe)) {
                return;
            }

            throw new IllegalStateException("Ponder basin could not process '" + recipeId + "' at " + basinPos + '.');
        });
    }
}
