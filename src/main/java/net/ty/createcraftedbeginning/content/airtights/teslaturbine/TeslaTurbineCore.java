package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.createmod.catnip.data.Pair;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class TeslaTurbineCore {
    public static final int MAX_LEVEL = 16;

    private static final long INTERNAL_CAPACITY = 8000L;

    private static final Set<Pair<Integer, Integer>> ALL_OFFSETS = Set.of(Pair.of(2, 1), Pair.of(-1, 2), Pair.of(1, -2), Pair.of(-2, -1), Pair.of(-2, 1), Pair.of(-1, -2), Pair.of(1, 2), Pair.of(2, -1));
    private static final Set<Pair<Integer, Integer>> CLOCKWISE_OFFSETS = Set.of(Pair.of(2, 1), Pair.of(-1, 2), Pair.of(1, -2), Pair.of(-2, -1));
    private static final Set<Pair<Integer, Integer>> COUNTER_CLOCKWISE_OFFSETS = Set.of(Pair.of(-2, 1), Pair.of(-1, -2), Pair.of(1, 2), Pair.of(2, -1));

    private static final String COMPOUND_KEY_FLOW_METER = "FlowMeter";
    private static final String COMPOUND_KEY_LEVEL_CALCULATOR = "LevelCalculator";
    private static final String COMPOUND_KEY_STRUCTURE_MANAGER = "StructureManager";

    private final TeslaTurbineBlockEntity turbine;
    private final TeslaTurbineStructureManager structureManager;
    private final TeslaTurbineTooltipBuilder tooltipBuilder;
    private final TeslaTurbineLevelCalculator levelCalculator;
    private final TeslaTurbineFlowMeter flowMeter;

    public TeslaTurbineCore(TeslaTurbineBlockEntity turbine) {
        this.turbine = turbine;
        structureManager = new TeslaTurbineStructureManager(this);
        levelCalculator = new TeslaTurbineLevelCalculator(this);
        tooltipBuilder = new TeslaTurbineTooltipBuilder(this);
        flowMeter = new TeslaTurbineFlowMeter(this);
    }

    public TeslaTurbineBlockEntity getTurbine() {
        return turbine;
    }

    public void tick() {
        Level level = turbine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        flowMeter.tick(level);
        levelCalculator.update();
        turbine.notifyUpdate();
    }

    public void lazyTick(@NotNull TeslaTurbineBlockEntity turbine) {
        Level level = turbine.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        structureManager.tick();
        turbine.notifyUpdate();
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

    public Set<Pair<Integer, Integer>> getOffsets(Boolean counterClockwise) {
        if (counterClockwise == null) {
            return ALL_OFFSETS;
        }

        return counterClockwise ? COUNTER_CLOCKWISE_OFFSETS : CLOCKWISE_OFFSETS;
    }

    public CompoundTag write(Provider lookupProvider) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put(COMPOUND_KEY_FLOW_METER, flowMeter.write(lookupProvider));
        compoundTag.put(COMPOUND_KEY_LEVEL_CALCULATOR, levelCalculator.write());
        compoundTag.put(COMPOUND_KEY_STRUCTURE_MANAGER, structureManager.write());
        return compoundTag;
    }

    public void read(@NotNull CompoundTag compoundTag, Provider lookupProvider) {
        if (compoundTag.contains(COMPOUND_KEY_FLOW_METER)) {
            flowMeter.read(compoundTag.getCompound(COMPOUND_KEY_FLOW_METER), lookupProvider);
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
        return new TeslaTurbineGasHandler(clockwise);
    }

    private class TeslaTurbineGasHandler implements IGasHandler {
        private final boolean clockwise;

        private TeslaTurbineGasHandler(boolean clockwise) {
            this.clockwise = clockwise;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull GasStack getGasInTank(int tank) {
            return GasStack.EMPTY;
        }

        @Override
        public long getTankCapacity(int tank) {
            return INTERNAL_CAPACITY;
        }

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return true;
        }

        @Override
        public long fill(@NotNull GasStack resource, @NotNull GasAction action) {
            return flowMeter.fill(resource, action, clockwise);
        }

        @Override
        public @NotNull GasStack drain(@NotNull GasStack resource, @NotNull GasAction action) {
            return GasStack.EMPTY;
        }

        @Override
        public @NotNull GasStack drain(long maxDrain, @NotNull GasAction action) {
            return GasStack.EMPTY;
        }
    }
}
