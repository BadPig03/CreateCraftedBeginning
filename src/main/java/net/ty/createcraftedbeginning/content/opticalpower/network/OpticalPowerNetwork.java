package net.ty.createcraftedbeginning.content.opticalpower.network;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerSource.Source;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpticalPowerNetwork {
    static final int ORDINARY_LIGHT_FLUX_LM = 1;

    private final List<BlockPos> nodes;
    private final List<BlockPos> consumers;
    private final Map<BlockPos, SourceEntry> sources;
    private final Set<BlockPos> sourceDependencies;
    private final boolean scanLimitExceeded;

    private double totalSourceFluxLm;

    OpticalPowerNetwork(List<BlockPos> nodes, List<BlockPos> consumers, Map<BlockPos, SourceEntry> sources, Set<BlockPos> sourceDependencies, boolean scanLimitExceeded) {
        this.nodes = nodes;
        this.consumers = consumers;
        this.sources = sources;
        this.sourceDependencies = sourceDependencies;
        this.scanLimitExceeded = scanLimitExceeded;
        recalculateTotalSourceFluxLm();
    }

    public static double getMaxConsumerFluxLm() {
        return CCBConfig.server().opticalPower.network.maxConsumerFluxLm.get();
    }

    static double getMaxNetworkFluxLm() {
        return CCBConfig.server().opticalPower.network.maxNetworkFluxLm.get();
    }

    RefreshResult refreshDynamicSourceFluxes(Level level) {
        boolean anySourceFluxChanged = false;
        for (SourceEntry entry : sources.values()) {
            Source source = entry.discoveredSource();
            if (!source.dynamic()) {
                continue;
            }

            BlockPos contactPos = entry.contactPos();
            if (!level.isLoaded(contactPos)) {
                return RefreshResult.RESCAN_REQUIRED;
            }

            BlockState state = level.getBlockState(contactPos);
            if (!(state.getBlock() instanceof OpticalPowerSource sourceProvider)) {
                return RefreshResult.RESCAN_REQUIRED;
            }

            double newFluxLm = sourceProvider.getCurrentOpticalFluxLm(level, contactPos, state, source);
            if (!Double.isFinite(newFluxLm) || newFluxLm < 0) {
                throw new IllegalArgumentException("Source luminous flux must be finite and non-negative; got " + newFluxLm + " lm.");
            }

            if (newFluxLm == entry.currentFluxLm()) {
                continue;
            }

            entry.setCurrentFluxLm(newFluxLm);
            anySourceFluxChanged = true;
        }
        if (!anySourceFluxChanged) {
            return RefreshResult.TOTAL_UNCHANGED;
        }

        double previousTotalSourceFluxLm = totalSourceFluxLm;
        recalculateTotalSourceFluxLm();
        if (totalSourceFluxLm == previousTotalSourceFluxLm) {
            return RefreshResult.TOTAL_UNCHANGED;
        }

        return RefreshResult.TOTAL_CHANGED;
    }

    double getAllocatedFluxLm(BlockPos consumerPos) {
        if (!consumers.contains(consumerPos)) {
            return 0;
        }

        return calculateFluxPerConsumerLm();
    }

    double calculateFluxPerConsumerLm() {
        int consumerCount = consumers.size();
        if (scanLimitExceeded || consumerCount == 0) {
            return 0;
        }

        double distributableFluxLm = Math.min(Math.min(totalSourceFluxLm, getMaxNetworkFluxLm()), consumerCount * getMaxConsumerFluxLm());
        return distributableFluxLm / consumerCount;
    }

    List<BlockPos> getNodes() {
        return nodes;
    }

    List<BlockPos> getConsumers() {
        return consumers;
    }

    Set<BlockPos> getSourceDependencies() {
        return sourceDependencies;
    }

    boolean hasDynamicSources() {
        return sources.values().stream().anyMatch(entry -> entry.discoveredSource().dynamic());
    }

    private void recalculateTotalSourceFluxLm() {
        if (scanLimitExceeded) {
            totalSourceFluxLm = 0;
            return;
        }

        int ordinaryContributionLm = sources.values().stream().anyMatch(SourceEntry::ordinary) ? ORDINARY_LIGHT_FLUX_LM : 0;
        double nonOrdinaryFluxLm = sources.values().stream().filter(source -> !source.ordinary()).mapToDouble(SourceEntry::currentFluxLm).sum();
        totalSourceFluxLm = Math.min(ordinaryContributionLm + nonOrdinaryFluxLm, Double.MAX_VALUE);
    }

    enum RefreshResult {
        TOTAL_UNCHANGED,
        TOTAL_CHANGED,
        RESCAN_REQUIRED
    }

    static final class SourceEntry {
        private final BlockPos contactPos;
        private final Source discoveredSource;
        private final boolean ordinary;
        private double currentFluxLm;

        SourceEntry(BlockPos contactPos, Source discoveredSource, double currentFluxLm, boolean ordinary) {
            this.contactPos = contactPos;
            this.discoveredSource = discoveredSource;
            this.currentFluxLm = currentFluxLm;
            this.ordinary = ordinary;
        }

        Source discoveredSource() {
            return discoveredSource;
        }

        double currentFluxLm() {
            return currentFluxLm;
        }

        private BlockPos contactPos() {
            return contactPos;
        }

        private boolean ordinary() {
            return ordinary;
        }

        private void setCurrentFluxLm(double currentFluxLm) {
            this.currentFluxLm = currentFluxLm;
        }
    }
}
