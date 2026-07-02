package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.createmod.catnip.data.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineCore {
    public static final int MAX_LEVEL = 16;

    private static final Set<Pair<Integer, Integer>> CLOCKWISE_OFFSETS = Set.of(Pair.of(2, 1), Pair.of(-1, 2), Pair.of(1, -2), Pair.of(-2, -1));
    private static final Set<Pair<Integer, Integer>> COUNTER_CLOCKWISE_OFFSETS = Set.of(Pair.of(-2, 1), Pair.of(-1, -2), Pair.of(1, 2), Pair.of(2, -1));
    private static final String COMPOUND_KEY_FLOW_METER = "FlowMeter";
    private static final String COMPOUND_KEY_LEVEL_CALCULATOR = "LevelCalculator";
    private static final String COMPOUND_KEY_STRUCTURE_MANAGER = "StructureManager";

    private final IGasHandler clockwiseHandler;
    private final IGasHandler counterClockwiseHandler;
    private final TeslaTurbineBlockEntity turbine;
    private final TeslaTurbineStructureManager structureManager;
    private final TeslaTurbineTooltipBuilder tooltipBuilder;
    private final TeslaTurbineLevelCalculator levelCalculator;
    private final TeslaTurbineFlowMeter flowMeter;

    public TeslaTurbineCore(TeslaTurbineBlockEntity turbine) {
        this.turbine = turbine;
        structureManager = new TeslaTurbineStructureManager(this, turbine);
        levelCalculator = new TeslaTurbineLevelCalculator(this, turbine);
        flowMeter = new TeslaTurbineFlowMeter(this, turbine);
        tooltipBuilder = new TeslaTurbineTooltipBuilder(this);
        clockwiseHandler = new TeslaTurbineGasHandler(true);
        counterClockwiseHandler = new TeslaTurbineGasHandler(false);
    }

    public void tick() {
        Level level = turbine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        flowMeter.tick();
        levelCalculator.update();
    }

    public void lazyTick() {
        Level level = turbine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        structureManager.tick();
    }

    public boolean addToGoggleTooltip(List<Component> tooltip) {
        if (!structureManager.isActive()) {
            return false;
        }

        tooltipBuilder.addToGoggleTooltip(tooltip);
        return true;
    }

    public TeslaTurbineStructureManager getStructureManager() {
        return structureManager;
    }

    public TeslaTurbineLevelCalculator getLevelCalculator() {
        return levelCalculator;
    }

    public TeslaTurbineTooltipBuilder getTooltipBuilder() {
        return tooltipBuilder;
    }

    public TeslaTurbineFlowMeter getFlowMeter() {
        return flowMeter;
    }

    public Set<Pair<Integer, Integer>> getOffsets(boolean counterClockwise) {
        return counterClockwise ? COUNTER_CLOCKWISE_OFFSETS : CLOCKWISE_OFFSETS;
    }

    public CompoundTag write(Provider provider) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put(COMPOUND_KEY_FLOW_METER, flowMeter.write(provider));
        compoundTag.put(COMPOUND_KEY_LEVEL_CALCULATOR, levelCalculator.write());
        compoundTag.put(COMPOUND_KEY_STRUCTURE_MANAGER, structureManager.write());
        return compoundTag;
    }

    public void read(CompoundTag compoundTag, Provider provider) {
        if (compoundTag.contains(COMPOUND_KEY_FLOW_METER)) {
            flowMeter.read(compoundTag.getCompound(COMPOUND_KEY_FLOW_METER), provider);
        }
        if (compoundTag.contains(COMPOUND_KEY_LEVEL_CALCULATOR)) {
            levelCalculator.read(compoundTag.getCompound(COMPOUND_KEY_LEVEL_CALCULATOR));
        }
        if (compoundTag.contains(COMPOUND_KEY_STRUCTURE_MANAGER)) {
            structureManager.read(compoundTag.getCompound(COMPOUND_KEY_STRUCTURE_MANAGER));
        }
        levelCalculator.update();
    }

    public IGasHandler createGasHandler(boolean clockwise) {
        if (clockwise) {
            return clockwiseHandler;
        }
        return counterClockwiseHandler;
    }

    private class TeslaTurbineGasHandler implements IGasHandler {
        private final boolean clockwise;

        private TeslaTurbineGasHandler(boolean clockwise) {
            this.clockwise = clockwise;
        }

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return true;
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return GasStack.EMPTY;
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return GasStack.EMPTY;
        }

        @Override
        public GasStack getGasInTank(int tank) {
            return GasStack.EMPTY;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            return flowMeter.fill(resource, action, clockwise);
        }

        @Override
        public long getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }
    }
}
