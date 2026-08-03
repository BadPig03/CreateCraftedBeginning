package net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.SmartGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.data.CCBGases;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BoilerSteamOutletBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int STRESS_PER_STEAM_MB = 1024;
    private static final int STEAM_ENGINE_BASE_SPEED = 16;
    private static final int SAMPLE_RATE = 5;
    private static final int SAMPLE_COUNT = 10;
    private static final int SAMPLE_WINDOW_TICKS = SAMPLE_RATE * SAMPLE_COUNT;
    private static final int LAZY_TICK_RATE = 20;

    private static final String COMPOUND_KEY_CURRENT_INDEX = "CurrentIndex";
    private static final String COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE = "TicksUntilNextSample";
    private static final String COMPOUND_KEY_GATHERED_EXTRACTION = "GatheredExtraction";
    private static final String COMPOUND_KEY_SAMPLES = "ExtractionSamples";
    private static final String COMPOUND_KEY_AVERAGE_EXTRACTION_RATE = "AverageExtractionRate";

    private final long[] extractedPerSample = new long[SAMPLE_COUNT];
    private int currentIndex;
    private int ticksUntilNextSample = SAMPLE_RATE;
    private long gatheredExtraction;
    private long rollingExtraction;
    private double averageExtractionRate;
    private double productionRemainder;

    private long accountingTick = Long.MIN_VALUE;
    private long productionThisTick;
    private SmartGasTankBehaviour steamTank;
    private IGasHandler exposedGasHandler;

    public BoilerSteamOutletBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.BOILER_STEAM_OUTLET.get(), (be, direction) -> be.exposedGasHandler);
    }

    public static long saturatedAdd(long current, long amount) {
        if (amount <= 0) {
            return current;
        }
        return Long.MAX_VALUE - current < amount ? Long.MAX_VALUE : current + amount;
    }

    private static long getMaximumOutputCapacity() {
        double capacity = getFullLoadProductionRate();
        if (!Double.isFinite(capacity) || capacity <= 0) {
            return 1;
        }

        if (capacity >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(1, (long) Math.ceil(capacity));
    }

    private static double getFullLoadProductionRate() {
        double fullLoadStress = STEAM_ENGINE_BASE_SPEED * BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE.get());
        return fullLoadStress / STRESS_PER_STEAM_MB;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        steamTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.OUTPUT, this, 1, getMaximumOutputCapacity(), false).forbidInsertion().allowExtraction();
        steamTank.getPrimaryHandler().setValidator(stack -> !stack.isEmpty() && stack.is(CCBGases.STEAM));
        exposedGasHandler = new SteamOutletGasHandler(this, steamTank.getCapability());
        behaviours.add(steamTank);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        ensureCurrentTick();
        if (!hasSampleState()) {
            ticksUntilNextSample = SAMPLE_RATE;
            return;
        }

        ticksUntilNextSample--;
        if (ticksUntilNextSample > 0) {
            return;
        }

        ticksUntilNextSample = SAMPLE_RATE;
        double previousAverage = averageExtractionRate;
        recordSample();
        setChanged();
        if (Double.compare(previousAverage, averageExtractionRate) == 0) {
            return;
        }

        sendData();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        if (state.getBlock() instanceof BoilerSteamOutletBlock outlet && outlet.canSurvive(state, level, worldPosition)) {
            return;
        }

        level.destroyBlock(worldPosition, true);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.putDouble(COMPOUND_KEY_AVERAGE_EXTRACTION_RATE, averageExtractionRate);
        if (clientPacket) {
            return;
        }

        compoundTag.putInt(COMPOUND_KEY_CURRENT_INDEX, currentIndex);
        compoundTag.putInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE, ticksUntilNextSample);
        compoundTag.putLong(COMPOUND_KEY_GATHERED_EXTRACTION, gatheredExtraction);
        compoundTag.putLongArray(COMPOUND_KEY_SAMPLES, extractedPerSample);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        averageExtractionRate = Math.max(0, compoundTag.getDouble(COMPOUND_KEY_AVERAGE_EXTRACTION_RATE));
        resetTickAccounting();
        if (clientPacket) {
            return;
        }

        clearSamplingState();
        currentIndex = Math.floorMod(compoundTag.getInt(COMPOUND_KEY_CURRENT_INDEX), SAMPLE_COUNT);
        ticksUntilNextSample = compoundTag.contains(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE) ? Math.clamp(compoundTag.getInt(COMPOUND_KEY_TICKS_UNTIL_NEXT_SAMPLE), 1, SAMPLE_RATE) : SAMPLE_RATE;
        gatheredExtraction = Math.max(0, compoundTag.getLong(COMPOUND_KEY_GATHERED_EXTRACTION));
        long[] storedSamples = compoundTag.getLongArray(COMPOUND_KEY_SAMPLES);
        for (int i = 0; i < Math.min(storedSamples.length, SAMPLE_COUNT); i++) {
            extractedPerSample[i] = Math.max(0, storedSamples[i]);
        }
        recalculateRollingExtraction();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.boiler_steam_outlet.header").forGoggles(tooltip);
        return true;
    }

    public void recordExtraction(GasStack drained, GasAction action) {
        if (action.simulate() || drained.isEmpty()) {
            return;
        }

        gatheredExtraction = saturatedAdd(gatheredExtraction, drained.getAmount());
        setChanged();
    }

    public void ensureCurrentTick() {
        Level currentLevel = level;
        if (currentLevel == null || currentLevel.isClientSide) {
            return;
        }

        long gameTime = currentLevel.getGameTime();
        if (accountingTick == gameTime) {
            return;
        }

        accountingTick = gameTime;
        double idealProduction = getMaximumProductionRate();
        if (!Double.isFinite(idealProduction) || idealProduction <= 0) {
            productionThisTick = 0;
            setAvailableSteam(0);
            return;
        }

        double availableProduction = productionRemainder + idealProduction;
        productionThisTick = availableProduction >= Long.MAX_VALUE ? Long.MAX_VALUE : Mth.lfloor(availableProduction);
        productionRemainder = availableProduction >= Long.MAX_VALUE ? 0 : availableProduction - productionThisTick;
        setAvailableSteam(productionThisTick);
    }

    private void resetTickAccounting() {
        accountingTick = Long.MIN_VALUE;
        productionThisTick = 0;
        setAvailableSteam(0);
    }

    private void setAvailableSteam(long amount) {
        if (steamTank == null) {
            return;
        }

        long capacity = getMaximumOutputCapacity();
        long clamped = Math.clamp(amount, 0, capacity);
        SmartGasTank tank = steamTank.getPrimaryHandler();
        tank.setCapacity(capacity);
        GasStack replacement = clamped <= 0 ? GasStack.EMPTY : new GasStack(CCBGases.STEAM.get(), clamped);
        GasStack current = tank.getGasStack();
        boolean unchanged = replacement.isEmpty() && current.isEmpty() || replacement.getAmount() == current.getAmount() && GasStack.isSameGasSameComponents(replacement, current);
        if (unchanged) {
            return;
        }

        tank.setGasStack(replacement);
    }

    private double getMaximumProductionRate() {
        if (!BoilerSteamOutletBlock.isActive(getBlockState())) {
            return 0;
        }

        FluidTankBlockEntity controller = getControllerTank();
        if (controller == null || !controller.boiler.isActive()) {
            return 0;
        }

        double efficiency = controller.boiler.getEngineEfficiency(controller.getTotalTankSize());
        double production = getFullLoadProductionRate() * efficiency;
        return Double.isFinite(production) && production > 0 ? production : 0;
    }

    private @Nullable FluidTankBlockEntity getControllerTank() {
        if (level == null) {
            return null;
        }

        BlockPos tankPos = BoilerSteamOutletBlock.getAttachedTankPos(getBlockState(), worldPosition);
        BlockEntity blockEntity = level.getBlockEntity(tankPos);
        if (!(blockEntity instanceof FluidTankBlockEntity tank)) {
            return null;
        }
        return tank.getControllerBE();
    }

    private boolean hasSampleState() {
        return rollingExtraction != 0 || gatheredExtraction != 0;
    }

    private void recordSample() {
        rollingExtraction = Math.max(0, rollingExtraction - extractedPerSample[currentIndex]);
        extractedPerSample[currentIndex] = gatheredExtraction;
        rollingExtraction = saturatedAdd(rollingExtraction, gatheredExtraction);
        currentIndex = (currentIndex + 1) % SAMPLE_COUNT;
        gatheredExtraction = 0;
        averageExtractionRate = (double) rollingExtraction / SAMPLE_WINDOW_TICKS;
    }

    private void recalculateRollingExtraction() {
        rollingExtraction = 0;
        for (long sample : extractedPerSample) {
            rollingExtraction = saturatedAdd(rollingExtraction, sample);
        }
        averageExtractionRate = (double) rollingExtraction / SAMPLE_WINDOW_TICKS;
    }

    private void clearSamplingState() {
        currentIndex = 0;
        ticksUntilNextSample = SAMPLE_RATE;
        gatheredExtraction = 0;
        rollingExtraction = 0;
        averageExtractionRate = 0;
        Arrays.fill(extractedPerSample, 0);
    }
}
