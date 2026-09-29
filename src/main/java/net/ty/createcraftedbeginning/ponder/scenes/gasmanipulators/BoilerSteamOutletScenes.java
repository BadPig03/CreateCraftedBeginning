package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet.BoilerSteamOutletBlock;
import net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet.BoilerSteamOutletBlockEntity;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BoilerSteamOutletScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("boiler_steam_outlet", "Extracting Steam from Boilers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos basePos = util.grid().at(1, 1, 3);
        BlockPos tankPos = basePos.above();
        BlockPos innerPipePos = tankPos.east();
        BlockPos outerPipePos = innerPipePos.east();
        BlockPos outletPos = outerPipePos.east();
        BlockPos boilerTankPos = outletPos.east();
        BlockPos tankTopPos = boilerTankPos.east().south().above();
        BlockPos burnerPos = boilerTankPos.below();
        BlockPos burnerRightPos = burnerPos.east().south();
        BlockPos enginePos = boilerTankPos.north().above();
        BlockPos redstonePos = outletPos.below();
        BlockPos fluidPipePos = enginePos.above().south(2);
        BlockPos fluidPumpPos = fluidPipePos.above(2);
        BlockPos fluidCogPos = fluidPumpPos.west();
        BlockPos fluidMotorPos = fluidCogPos.below();

        Selection boilerSelection = util.select().fromTo(boilerTankPos, tankTopPos);
        Selection burnerSelection = util.select().fromTo(burnerPos, burnerRightPos);
        Selection outletSelection = util.select().position(outletPos);
        Selection engineSelection = util.select().position(enginePos);
        Selection tankSelection = util.select().fromTo(basePos, tankPos);
        Selection pipeSelection = util.select().fromTo(innerPipePos, outerPipePos);
        Selection redstoneSelection = util.select().position(redstonePos);
        Selection fluidPumpSelection = util.select().fromTo(fluidPipePos, fluidPumpPos);
        Selection fluidSourceSelection = util.select().fromTo(fluidCogPos, fluidMotorPos);

        Vec3 outletVec = util.vector().centerOf(outletPos);
        Vec3 boilerVec = util.vector().centerOf(boilerTankPos);
        Vec3 outletOutputVec = util.vector().blockSurface(outletPos, Direction.WEST);
        Vec3 engineVec = util.vector().centerOf(enginePos);
        Vec3 gasNetworkVec = util.vector().centerOf(innerPipePos);

        AABB outletArea = new AABB(outletVec, outletVec);
        AABB engineArea = new AABB(engineVec, engineVec);
        AABB gasPathArea = new AABB(outletVec, outletVec);
        AABB receivingTankArea = new AABB(util.vector().centerOf(basePos), util.vector().centerOf(tankPos));

        Object outletObject = new Object();
        Object engineObject = new Object();
        Object gasPathObject = new Object();
        Object receivingNetworkObject = new Object();
        Object blockedNetworkObject = new Object();
        Object disabledOutletObject = new Object();
        Object releasedCapacityObject = new Object();

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(burnerSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(boilerSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(outletSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(fluidPumpSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().setBlock(fluidMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.UP), false);
        scene.world().showSection(fluidSourceSelection, Direction.DOWN);

        scene.idle(15);
        scene.world().setKineticSpeed(fluidSourceSelection, mediumSpeed);
        scene.world().setKineticSpeed(fluidPumpSelection, -mediumSpeed);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, outletObject, outletArea, 3);

        scene.idle(3);
        outletArea = outletArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, outletObject, outletArea, 60);
        scene.overlay().showText(60).text("Boiler Steam Outlets attach directly to Fluid Tanks in a boiler").pointAt(outletVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("While enabled on an active boiler, the Outlet continuously produces Steam").pointAt(outletVec).placeNearTarget().attachKeyFrame();

        scene.idle(12);
        scene.world().modifyBlockEntity(outletPos, BoilerSteamOutletBlockEntity.class, outlet -> setOutletSteam(outlet, 1000));

        scene.idle(55);
        scene.world().showSection(engineSelection, Direction.SOUTH);

        scene.idle(10);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, engineObject, engineArea, 3);

        scene.idle(3);
        engineArea = engineArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, engineObject, engineArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, outletObject, outletArea, 60);
        scene.overlay().showText(60).text("Steam Engines and enabled Outlets share the same boiler power").pointAt(engineVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("If boiler power is insufficient, Steam Engines and Outlets both run below full capacity").colored(PonderPalette.RED).pointAt(boilerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(pipeSelection, Direction.NORTH);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, gasPathObject, gasPathArea, 3);

        scene.idle(3);
        gasPathArea = gasPathArea.inflate(0.5, 0.375, 0.375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, gasPathObject, gasPathArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, gasPathObject, gasPathArea.expandTowards(-2, 0, 0), 60);
        scene.overlay().showText(60).text("Only the outward face can connect to a gas network...").colored(PonderPalette.OUTPUT).pointAt(outletOutputVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, receivingNetworkObject, receivingTankArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("...and Steam only flows out when the connected network is at lower pressure").colored(PonderPalette.RED).pointAt(gasNetworkVec).placeNearTarget().attachKeyFrame();

        scene.idle(27);
        scene.world().modifyBlockEntity(basePos, AirtightTankBlockEntity.class, tank -> tank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.STEAM.get(), Math.min(1000, tank.getTankInventory().getMaxAmount()))).requireAccepted());
        scene.world().modifyBlockEntity(outletPos, BoilerSteamOutletBlockEntity.class, outlet -> setOutletSteam(outlet, 0));

        scene.idle(68);
        scene.world().modifyBlockEntity(basePos, AirtightTankBlockEntity.class, tank -> tank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.STEAM.get(), tank.getTankInventory().getMaxAmount())).requireAccepted());
        scene.world().modifyBlockEntity(outletPos, BoilerSteamOutletBlockEntity.class, outlet -> setOutletSteam(outlet, Long.MAX_VALUE));
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, blockedNetworkObject, receivingTankArea.inflate(0.5), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, disabledOutletObject, outletArea, 60);
        scene.overlay().showText(60).text("When Steam cannot flow out, the Outlet only buffers a small amount...").colored(PonderPalette.RED).pointAt(outletVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, disabledOutletObject, outletArea, 60);
        scene.overlay().showText(60).text("...once the buffer is full, any further Steam produced is lost").colored(PonderPalette.RED).pointAt(outletVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(redstoneSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, disabledOutletObject, outletArea, 60);
        scene.overlay().showText(60).text("Redstone power can be used to disable the Outlet").colored(PonderPalette.BLUE).pointAt(outletVec).placeNearTarget().attachKeyFrame();

        scene.idle(12);
        scene.world().modifyBlock(redstonePos, state -> state.setValue(LeverBlock.POWERED, true), false);
        scene.world().modifyBlock(outletPos, state -> state.setValue(BoilerSteamOutletBlock.POWERED, true), false);
        scene.effects().indicateRedstone(redstonePos);

        scene.idle(68);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, disabledOutletObject, outletArea, 60);
        scene.overlay().showText(60).text("Disabling it also clears its Steam buffer...").colored(PonderPalette.RED).pointAt(outletVec).placeNearTarget().attachKeyFrame();

        scene.idle(12);
        scene.world().modifyBlockEntity(outletPos, BoilerSteamOutletBlockEntity.class, outlet -> setOutletSteam(outlet, 0));

        scene.idle(68);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, releasedCapacityObject, engineArea, 60);
        scene.overlay().showText(60).text("...freeing its share of boiler power for other outputs").colored(PonderPalette.GREEN).pointAt(engineVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    private static void setOutletSteam(BoilerSteamOutletBlockEntity outlet, long amount) {
        Level level = outlet.getLevel();
        if (level == null) {
            return;
        }

        GasHandler handler = level.getCapability(GasCapabilities.BLOCK, outlet.getBlockPos(), Direction.WEST);
        if (!(handler instanceof GasStorageHandler storage)) {
            return;
        }

        handler.drain(Long.MAX_VALUE, GasAction.EXECUTE);
        long storedAmount = Math.min(amount, storage.getTankMaxAmount(0));
        if (storedAmount <= 0) {
            return;
        }

        storage.getPressureCompartment(0).restoreDrainedGas(new GasStack(CCBGases.STEAM.get(), storedAmount), GasAction.EXECUTE);
    }
}
