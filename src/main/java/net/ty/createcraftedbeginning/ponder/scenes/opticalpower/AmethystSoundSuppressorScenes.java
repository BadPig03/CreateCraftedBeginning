package net.ty.createcraftedbeginning.ponder.scenes.opticalpower;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor.AmethystSoundSuppressorBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor.AmethystSoundSuppressorClient;
import net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor.AmethystSoundSuppressorController;
import net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor.AmethystSoundSuppressorSerialization;
import net.ty.createcraftedbeginning.content.opticalpower.energy.OpticalEnergyStorage;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystSoundSuppressorScenes {
    private AmethystSoundSuppressorScenes() {
    }

    public static void suppression(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("amethyst_sound_suppressor_suppression", "Suppressing Sounds and Vibrations");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos suppressorPos = util.grid().at(3, 1, 3);
        BlockPos leverPos = suppressorPos.west(2);
        BlockPos sensorPos = suppressorPos.east(2).north();
        BlockPos wirePos = suppressorPos.east(2);
        BlockPos lampPos = suppressorPos.east(2).south();
        BlockPos chunkMinPos = suppressorPos.west(3).north(3);
        BlockPos chunkMaxPos = suppressorPos.east(12).south(12);

        Selection suppressorSelection = util.select().position(suppressorPos);
        Selection leverSelection = util.select().position(leverPos);
        Selection redstoneSelection = util.select().fromTo(sensorPos, lampPos);

        Vec3 suppressorVec = util.vector().centerOf(suppressorPos);
        Vec3 suppressorTopVec = util.vector().topOf(suppressorPos);
        Vec3 leverVec = util.vector().centerOf(leverPos);
        Vec3 sensorVec = util.vector().centerOf(sensorPos);
        Vec3 chunkMinVec = util.vector().centerOf(chunkMinPos).add(-0.5, -0.4375, -0.5);
        Vec3 chunkMaxVec = util.vector().centerOf(chunkMaxPos).add(0.5, -0.3125, 0.5);

        AABB suppressorArea = new AABB(suppressorPos);
        AABB leverArea = new AABB(leverPos);
        AABB sensorArea = new AABB(sensorPos);
        AABB coverageStartArea = new AABB(suppressorVec, suppressorVec);
        AABB coverageRestartArea = new AABB(suppressorVec, suppressorVec).move(0, -0.375, 0).inflate(0.0625);
        AABB coverageArea = new AABB(chunkMinVec, chunkMaxVec);

        Object coverageObject = new Object();
        Object suppressorObject = new Object();

        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        scene.idle(20);
        scene.world().showSection(leverSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(redstoneSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showControls(leverVec, Pointing.LEFT, 30).rightClick();

        scene.idle(7);
        toggleLever(scene, leverSelection, leverPos);
        scene.effects().emitParticles(leverVec, scene.effects().simpleParticleEmitter(new VibrationParticleOption(new BlockPositionSource(sensorPos), 20), Vec3.ZERO), 1, 1);

        scene.idle(20);
        setSensorOutput(scene, sensorPos, wirePos, lampPos, true);
        scene.effects().indicateRedstone(lampPos);

        scene.idle(20);
        setSensorOutput(scene, sensorPos, wirePos, lampPos, false);
        scene.world().showSection(suppressorSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Amethyst Sound Suppressors can suppress sounds and vibrations without power").colored(PonderPalette.GREEN).pointAt(suppressorVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(leverVec, Pointing.LEFT, 30).rightClick();

        scene.idle(7);
        toggleLever(scene, leverSelection, leverPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, sensorPos, sensorArea, 53);

        scene.idle(60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, coverageObject, coverageStartArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, coverageObject, coverageArea, 60);
        scene.overlay().showText(60).text("Suppression covers the device's chunk at every height").pointAt(suppressorVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Vibrations are blocked if their source or receiver is inside the covered area").pointAt(sensorVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, leverPos, leverArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, sensorPos, sensorArea, 60);

        scene.idle(7);
        toggleLever(scene, leverSelection, leverPos);

        scene.idle(73);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, suppressorObject, suppressorArea, 140);
        scene.overlay().showText(60).text("Most world sounds are muted if their source or the player is inside the covered area").colored(PonderPalette.BLUE).pointAt(suppressorVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Background music, normal UI sounds and a few other sounds are unaffected").colored(PonderPalette.BLUE).pointAt(suppressorVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Equip Engineer's Goggles to see the suppression range").colored(PonderPalette.BLUE).pointAt(suppressorVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, coverageObject, coverageRestartArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, coverageObject, coverageArea, 84);

        scene.idle(77);
        scene.overlay().showText(60).text("Right-click the suppressor with a Wrench to toggle the range display").colored(PonderPalette.BLUE).pointAt(suppressorVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(suppressorTopVec, Pointing.RIGHT, 40).rightClick().withItem(wrenchItem.copy());

        scene.idle(7);
        scene.world().modifyBlockEntityNBT(suppressorSelection, AmethystSoundSuppressorBlockEntity.class, compoundTag -> AmethystSoundSuppressorSerialization.writeShowOutline(compoundTag, false));

        scene.idle(53);
        scene.markAsFinished();
    }

    public static void lighting(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("amethyst_sound_suppressor_lighting", "Illuminating the Amethyst Sound Suppressor");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos suppressorPos = util.grid().at(3, 1, 3);
        BlockPos emitterPos = suppressorPos.above(3);

        Selection suppressorSelection = util.select().position(suppressorPos);
        Selection emitterSelection = util.select().position(emitterPos);

        Vec3 suppressorVec = util.vector().centerOf(suppressorPos);
        Vec3 crystalVec = util.vector().centerOf(suppressorPos).add(0, 0.6, 0);

        AABB suppressorArea = new AABB(suppressorPos);
        AABB crystalArea = new AABB(crystalVec, crystalVec).inflate(0.1875, 0.28125, 0.1875);

        Object crystalObject = new Object();
        Object suppressorObject = new Object();
        OpticalEnergyStorage storage = new OpticalEnergyStorage(AmethystSoundSuppressorController.CAPACITY_LUMEN_SECONDS);

        scene.idle(20);
        scene.world().showSection(suppressorSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(emitterSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, crystalObject, crystalArea, 60);
        scene.overlay().showText(60).text("The laser must hit the amethyst on top to store light for crystal growth").colored(PonderPalette.RED).pointAt(crystalVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        setReceivedPower(scene, suppressorPos, 15);

        scene.idle(73);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, suppressorObject, suppressorArea, 60);
        scene.overlay().showText(60).text("A beam that is too weak or too spread out will not charge the suppressor").colored(PonderPalette.RED).pointAt(suppressorVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Keep the laser shining to store energy for crystal growth").colored(PonderPalette.GREEN).pointAt(crystalVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        setReceivedPower(scene, suppressorPos, 16);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, crystalObject, crystalArea, 133);

        advanceQuantity(scene, suppressorPos, storage, 16, 73);
        scene.overlay().showText(60).text("A stronger beam can shorten the wait between growth steps").colored(PonderPalette.GREEN).pointAt(crystalVec).placeNearTarget().attachKeyFrame();

        advanceQuantity(scene, suppressorPos, storage, 16, 7);
        setReceivedPower(scene, suppressorPos, 32);

        advanceQuantity(scene, suppressorPos, storage, 32, 73);
        scene.overlay().showText(60).text("Stored light remains after the laser stops and can still grow crystals if enough is saved. Sound suppression continues as normal").colored(PonderPalette.RED).pointAt(crystalVec).placeNearTarget().attachKeyFrame();

        advanceQuantity(scene, suppressorPos, storage, 32, 7);
        setReceivedPower(scene, suppressorPos, 0);

        scene.idle(73);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, suppressorObject, suppressorArea, 140);
        scene.overlay().showText(60).text("On Create contraptions, the suppressor can still block sounds and vibrations").colored(PonderPalette.BLUE).pointAt(suppressorVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("However, it cannot receive laser light or grow Experience Crystals while mounted on a contraption").colored(PonderPalette.BLUE).pointAt(suppressorVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void growth(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("amethyst_sound_suppressor_growth", "Growing Experience Crystals");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos suppressorPos = util.grid().at(3, 1, 3);
        BlockPos emitterPos = suppressorPos.above(3);
        BlockPos supportPos = suppressorPos.east(2);
        BlockPos crystalPos = suppressorPos.east(2).above();
        BlockPos secondCrystalPos = suppressorPos.east(2).north(2).above();
        BlockPos basinMinPos = suppressorPos.east().north(3);
        BlockPos basinMaxPos = suppressorPos.east(3).above().north();

        Selection suppressorSelection = util.select().position(suppressorPos);
        Selection emitterSelection = util.select().position(emitterPos);
        Selection supportSelection = util.select().position(supportPos);
        Selection crystalSelection = util.select().position(crystalPos);
        Selection waterBasinSelection = util.select().fromTo(basinMinPos, basinMaxPos);

        Vec3 suppressorVec = util.vector().centerOf(suppressorPos);
        Vec3 suppressorTipVec = util.vector().topOf(suppressorPos).add(0, 0.2, 0);
        Vec3 crystalVec = util.vector().centerOf(crystalPos);
        Vec3 secondCrystalVec = util.vector().centerOf(secondCrystalPos);

        AABB crystalArea = new AABB(crystalPos);
        AABB secondCrystalArea = new AABB(secondCrystalPos);

        Object growthObject = new Object();
        Object secondGrowthObject = new Object();

        OpticalEnergyStorage storage = new OpticalEnergyStorage(AmethystSoundSuppressorController.CAPACITY_LUMEN_SECONDS);

        scene.idle(20);
        scene.world().showSection(suppressorSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(crystalSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(supportSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(waterBasinSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(emitterSelection, Direction.DOWN);

        scene.idle(20);
        setReceivedPower(scene, suppressorPos, 16);
        scene.overlay().showText(60).text("With enough light stored, Experience Crystals grow automatically; no sounds or vibrations are needed").colored(PonderPalette.GREEN).pointAt(suppressorVec).placeNearTarget().attachKeyFrame();
        advanceQuantity(scene, suppressorPos, storage, 16, 80);
        storage.extract(64);
        setQuantity(scene, suppressorPos, storage);
        scene.world().setBlock(crystalPos, CCBBlocks.SMALL_EXPERIENCE_BUD.getDefaultState(), false);
        scene.addInstruction(ponder -> AmethystSoundSuppressorClient.showGrowthParticles(ponder.getWorld(), crystalVec, suppressorTipVec));

        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, growthObject, crystalArea, 60);
        scene.overlay().showText(60).text("The suppressor first helps immature crystals within a radius of 4 blocks grow larger").colored(PonderPalette.BLUE).pointAt(crystalVec).placeNearTarget().attachKeyFrame();
        advanceQuantity(scene, suppressorPos, storage, 16, 80);
        storage.extract(64);
        setQuantity(scene, suppressorPos, storage);
        scene.world().setBlock(crystalPos, CCBBlocks.MEDIUM_EXPERIENCE_BUD.getDefaultState(), false);
        scene.addInstruction(ponder -> AmethystSoundSuppressorClient.showGrowthParticles(ponder.getWorld(), crystalVec, suppressorTipVec));

        scene.overlay().showText(60).text("Each time, only one crystal grows by one stage").pointAt(crystalVec).placeNearTarget().attachKeyFrame();
        advanceQuantity(scene, suppressorPos, storage, 16, 80);
        storage.extract(64);
        setQuantity(scene, suppressorPos, storage);
        scene.world().setBlock(crystalPos, CCBBlocks.EXPERIENCE_CLUSTER.getDefaultState(), false);
        scene.addInstruction(ponder -> AmethystSoundSuppressorClient.showGrowthParticles(ponder.getWorld(), crystalVec, suppressorTipVec));

        scene.overlay().showText(60).text("Keep supplying light to grow small buds into mature clusters").pointAt(crystalVec).placeNearTarget().attachKeyFrame();
        advanceQuantity(scene, suppressorPos, storage, 16, 80);

        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, growthObject, crystalArea, 60);
        scene.overlay().showText(60).text("When no immature crystals remain, the suppressor will try to grow new buds").colored(PonderPalette.GREEN).pointAt(crystalVec).placeNearTarget().attachKeyFrame();
        advanceQuantity(scene, suppressorPos, storage, 16, 80);

        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, secondGrowthObject, secondCrystalArea, 60);
        scene.overlay().showText(60).text("New buds can grow in air or source water on Sculk surfaces").colored(PonderPalette.GREEN).pointAt(secondCrystalVec).placeNearTarget().attachKeyFrame();
        advanceQuantity(scene, suppressorPos, storage, 16, 20);
        storage.extract(64);
        setQuantity(scene, suppressorPos, storage);
        scene.world().setBlock(secondCrystalPos, CCBBlocks.SMALL_EXPERIENCE_BUD.getDefaultState().setValue(BlockStateProperties.WATERLOGGED, true), false);
        scene.addInstruction(ponder -> AmethystSoundSuppressorClient.showGrowthParticles(ponder.getWorld(), secondCrystalVec, suppressorTipVec));
        advanceQuantity(scene, suppressorPos, storage, 16, 80);

        scene.overlay().showText(60).text("With nowhere to grow crystals, the suppressor can save enough light for up to two growth steps").colored(PonderPalette.BLUE).pointAt(suppressorVec).placeNearTarget().attachKeyFrame();
        setReceivedPower(scene, suppressorPos, 0);

        scene.idle(60);
        scene.markAsFinished();
    }

    private static void toggleLever(CreateSceneBuilder scene, Selection leverSelection, BlockPos leverPos) {
        scene.world().toggleRedstonePower(leverSelection);
        scene.effects().indicateRedstone(leverPos);
    }

    private static void setSensorOutput(CreateSceneBuilder scene, BlockPos sensorPos, BlockPos wirePos, BlockPos lampPos, boolean powered) {
        scene.world().modifyBlock(sensorPos, state -> state.setValue(BlockStateProperties.SCULK_SENSOR_PHASE, powered ? SculkSensorPhase.ACTIVE : SculkSensorPhase.INACTIVE).setValue(BlockStateProperties.POWER, powered ? 15 : 0), false);
        scene.world().modifyBlock(wirePos, state -> state.setValue(BlockStateProperties.POWER, powered ? 15 : 0), false);
        scene.world().modifyBlock(lampPos, state -> state.setValue(BlockStateProperties.LIT, powered), false);
    }

    private static void advanceQuantity(CreateSceneBuilder scene, BlockPos suppressorPos, OpticalEnergyStorage storage, double fluxLm, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            scene.idle(1);
            storage.receive(fluxLm * 0.05);
            setQuantity(scene, suppressorPos, storage);
        }
    }

    private static void setQuantity(CreateSceneBuilder scene, BlockPos suppressorPos, OpticalEnergyStorage storage) {
        CompoundTag snapshot = new CompoundTag();
        storage.write(snapshot);
        scene.world().modifyBlockEntity(suppressorPos, AmethystSoundSuppressorBlockEntity.class, suppressor -> {
            CompoundTag tag = suppressor.getUpdateTag(scene.world().getHolderLookupProvider());
            tag.merge(snapshot);
            suppressor.handleUpdateTag(tag, scene.world().getHolderLookupProvider());
        });
    }

    private static void setReceivedPower(CreateSceneBuilder scene, BlockPos suppressorPos, double fluxLm) {
        scene.world().modifyBlockEntity(suppressorPos.above(3), LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(fluxLm));
        scene.world().modifyBlockEntity(suppressorPos, AmethystSoundSuppressorBlockEntity.class, suppressor -> {
            CompoundTag compoundTag = suppressor.getUpdateTag(scene.world().getHolderLookupProvider());
            double qualified = 0;
            if (fluxLm >= 16) {
                qualified = fluxLm;
            }
            AmethystSoundSuppressorSerialization.writeIllumination(compoundTag, fluxLm, qualified);
            suppressor.handleUpdateTag(compoundTag, scene.world().getHolderLookupProvider());
        });
    }
}
