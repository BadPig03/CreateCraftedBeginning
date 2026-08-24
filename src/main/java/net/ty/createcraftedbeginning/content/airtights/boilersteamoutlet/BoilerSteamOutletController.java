package net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet.BoilerSteamOutletExtractionMeter.TickResult;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BoilerSteamOutletController {
    private final BoilerSteamOutletBlockEntity outlet;
    private final BoilerSteamOutletProduction production;
    private final BoilerSteamOutletExtractionMeter extractionMeter;

    BoilerSteamOutletController(BoilerSteamOutletBlockEntity outlet) {
        this.outlet = outlet;
        production = new BoilerSteamOutletProduction(outlet);
        extractionMeter = new BoilerSteamOutletExtractionMeter();
    }

    void tickServer() {
        Level level = outlet.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        boolean productionRateChanged = production.ensureCurrentTick();
        TickResult extractionTickResult = extractionMeter.tick();
        if (!productionRateChanged && extractionTickResult == TickResult.NONE) {
            return;
        }

        outlet.setChanged();
        if (!productionRateChanged && extractionTickResult != TickResult.AVERAGE_CHANGED) {
            return;
        }

        outlet.sendData();
    }

    void lazyTickServer() {
        Level level = outlet.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState outletState = outlet.getBlockState();
        if (outletState.getBlock() instanceof BoilerSteamOutletBlock outletBlock && outletBlock.canSurvive(outletState, level, outlet.getBlockPos())) {
            return;
        }

        level.destroyBlock(outlet.getBlockPos(), true);
    }

    void ensureCurrentTick() {
        if (!production.ensureCurrentTick()) {
            return;
        }

        outlet.setChanged();
        outlet.sendData();
    }

    double getSteamGenerationRate() {
        return production.getProductionRatePerSecond();
    }

    double getSteamOutputRate() {
        return extractionMeter.getAverageExtractionRatePerSecond();
    }

    void recordExtraction(GasStack drained, GasAction action) {
        if (!extractionMeter.recordExtraction(drained, action)) {
            return;
        }

        outlet.setChanged();
    }

    void write(CompoundTag compoundTag, boolean clientPacket) {
        production.write(compoundTag, clientPacket);
        extractionMeter.write(compoundTag, clientPacket);
    }

    void read(CompoundTag compoundTag, boolean clientPacket) {
        production.read(compoundTag, clientPacket);
        extractionMeter.read(compoundTag, clientPacket);
    }
}
