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
public final class BoilerSteamOutletController {
    private final BoilerSteamOutletBlockEntity outlet;
    private final BoilerSteamOutletProduction production;
    private final BoilerSteamOutletExtractionMeter extractionMeter;

    public BoilerSteamOutletController(BoilerSteamOutletBlockEntity outlet) {
        this.outlet = outlet;
        production = new BoilerSteamOutletProduction(outlet);
        extractionMeter = new BoilerSteamOutletExtractionMeter();
    }

    public void tickServer() {
        Level level = outlet.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        boolean productionRateChanged = production.ensureCurrentTick();
        TickResult sampleResult = extractionMeter.tick();
        if (!productionRateChanged && sampleResult == TickResult.NONE) {
            return;
        }

        outlet.setChanged();
        if (!productionRateChanged && sampleResult != TickResult.AVERAGE_CHANGED) {
            return;
        }

        outlet.sendData();
    }

    public void lazyTickServer() {
        Level level = outlet.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = outlet.getBlockState();
        if (state.getBlock() instanceof BoilerSteamOutletBlock block && block.canSurvive(state, level, outlet.getBlockPos())) {
            return;
        }

        level.destroyBlock(outlet.getBlockPos(), true);
    }

    public void ensureCurrentTick() {
        if (!production.ensureCurrentTick()) {
            return;
        }

        outlet.setChanged();
        outlet.sendData();
    }

    public double getSteamGenerationRate() {
        return production.getProductionRatePerSecond();
    }

    public double getSteamOutputRate() {
        return extractionMeter.getAverageExtractionRatePerSecond();
    }

    public void recordExtraction(GasStack drained, GasAction action) {
        if (!extractionMeter.recordExtraction(drained, action)) {
            return;
        }

        outlet.setChanged();
    }

    public void write(CompoundTag tag, boolean clientPacket) {
        production.write(tag, clientPacket);
        extractionMeter.write(tag, clientPacket);
    }

    public void read(CompoundTag tag, boolean clientPacket) {
        production.read(tag, clientPacket);
        extractionMeter.read(tag, clientPacket);
    }
}
