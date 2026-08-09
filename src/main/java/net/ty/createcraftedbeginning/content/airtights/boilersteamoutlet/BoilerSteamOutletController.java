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

        production.ensureCurrentTick();
        TickResult sampleResult = extractionMeter.tick();
        if (sampleResult == TickResult.NONE) {
            return;
        }

        outlet.setChanged();
        if (sampleResult != TickResult.AVERAGE_CHANGED) {
            return;
        }

        outlet.sendData();
    }

    void lazyTickServer() {
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

    void ensureCurrentTick() {
        production.ensureCurrentTick();
    }

    void recordExtraction(GasStack drained, GasAction action) {
        if (!extractionMeter.recordExtraction(drained, action)) {
            return;
        }

        outlet.setChanged();
    }

    void write(CompoundTag tag, boolean clientPacket) {
        extractionMeter.write(tag, clientPacket);
    }

    void read(CompoundTag tag, boolean clientPacket) {
        extractionMeter.read(tag, clientPacket);
        production.resetTickAccounting();
    }
}
