package net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandler;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlers;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BoilerSteamOutletIntegration {
    private static volatile boolean verified;
    private static volatile boolean verificationAttempted;

    private BoilerSteamOutletIntegration() {
    }

    public static void markVerified() {
        verified = true;
    }

    public static boolean isVerified() {
        return verified;
    }

    public static int scanAttachedSteamOutlets(FluidTankBlockEntity controllerTank) {
        Level level = controllerTank.getLevel();
        if (level == null) {
            return 0;
        }

        int attachedSteamOutletCount = 0;
        BlockPos controllerPos = controllerTank.getBlockPos();
        for (int yOffset = 0; yOffset < controllerTank.getHeight(); yOffset++) {
            for (int xOffset = 0; xOffset < controllerTank.getWidth(); xOffset++) {
                for (int zOffset = 0; zOffset < controllerTank.getWidth(); zOffset++) {
                    BlockPos tankPos = controllerPos.offset(xOffset, yOffset, zOffset);
                    if (!FluidTankBlock.isTank(level.getBlockState(tankPos))) {
                        continue;
                    }

                    for (Direction outletDirection : Iterate.directions) {
                        BlockState adjacentState = level.getBlockState(tankPos.relative(outletDirection));
                        if (!BoilerSteamOutletBlock.isActive(adjacentState) || BoilerSteamOutletBlock.getFacing(adjacentState) != outletDirection) {
                            continue;
                        }

                        attachedSteamOutletCount++;
                    }
                }
            }
        }
        return attachedSteamOutletCount;
    }

    static double getSteamEngineFullLoadStressCapacity() {
        return SteamEngineBlock.getSpeedRange().getFirst() * BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE.get());
    }

    static double getNormalPressureSteamStressCapacityPerGasUnit() {
        AirtightEngineHandler steamHandler = AirtightEngineHandlers.resolve(CCBGases.STEAM.get(), GasPressure.REFERENCE_PRESSURE_PA);
        return AirtightAssemblyDriverCore.getStressCapacityPerGasUnit(steamHandler.getWorkFactor());
    }

    static synchronized boolean ensureVerified(FluidTankBlockEntity controllerTank) {
        if (verified) {
            return true;
        }

        if (verificationAttempted) {
            return false;
        }

        verificationAttempted = true;
        controllerTank.updateBoilerState();
        if (verified) {
            return true;
        }

        CCBAPI.LOGGER.error("Boiler steam outlet integration with Create's BoilerData is unavailable.");
        return false;
    }
}
