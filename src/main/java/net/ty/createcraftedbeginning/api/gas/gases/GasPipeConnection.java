package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.ty.createcraftedbeginning.api.gas.gases.GasFlowSource.Blocked;
import net.ty.createcraftedbeginning.api.gas.gases.GasFlowSource.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasFlowSource.OtherPipe;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class GasPipeConnection {
    private static final String COMPOUND_KEY_PRESSURE = "Pressure";
    private static final String COMPOUND_KEY_OPEN_END = "OpenEnd";
    private static final String COMPOUND_KEY_AIR_FLOW = "AirFlow";
    private static final String COMPOUND_KEY_INBOUND = "Inbound";
    private static final String COMPOUND_KEY_GAS = "Gas";

    public Direction side;
    public Couple<Float> pressure;

    public Optional<GasFlowSource> source;
    public Optional<AirFlow> flow;
    public Optional<GasNetwork> network;
    private Optional<GasFlowSource> previousSource;

    public GasPipeConnection(Direction side) {
        this.side = side;
        pressure = Couple.create(() -> 0.0f);
        flow = Optional.empty();
        previousSource = Optional.empty();
        source = Optional.empty();
        network = Optional.empty();
    }

    public GasStack getProvidedGas() {
        GasStack empty = GasStack.EMPTY;
        if (flow.isEmpty()) {
            return empty;
        }

        AirFlow airFlow = flow.get();
        if (!airFlow.inbound) {
            return empty;
        }

        return airFlow.gas;
    }

    public boolean flipFlowsIfPressureReversed() {
        if (flow.isEmpty()) {
            return false;
        }

        boolean singlePressure = comparePressure() != 0 && (getInboundPressure() == 0 || getOutwardPressure() == 0);
        AirFlow airFlow = flow.get();
        if (!singlePressure || comparePressure() < 0 == airFlow.inbound) {
            return false;
        }

        airFlow.inbound = !airFlow.inbound;
        return true;
    }

    public float comparePressure() {
        return getOutwardPressure() - getInboundPressure();
    }

    float getOutwardPressure() {
        return pressure.getSecond();
    }

    float getInboundPressure() {
        return pressure.getFirst();
    }

    public void manageSource(Level level, BlockPos pos, BlockEntity blockEntity) {
        if (source.isEmpty() && !determineSource(level, pos)) {
            return;
        }

        GasFlowSource flowSource = source.get();
        flowSource.manageSource(level, blockEntity);
    }

    public boolean determineSource(@NotNull Level level, @NotNull BlockPos pos) {
        BlockPos relative = pos.relative(side);
        if (level.getChunk(relative.getX() >> 4, relative.getZ() >> 4, ChunkStatus.FULL, false) == null) {
            return false;
        }

        BlockFace location = new BlockFace(pos, side);
        if (GasPropagator.isOpenEnd(level, pos, side)) {
            if (previousSource.orElse(null) instanceof OpenEndedGasPipe) {
                source = previousSource;
            }
            else {
                source = Optional.of(new OpenEndedGasPipe(location));
            }
            return true;
        }

        if (GasPropagator.hasGasCapability(level, location.getConnectedPos(), side.getOpposite())) {
            source = Optional.of(new GasHandler(location));
            return true;
        }

        GasTransportBehaviour behaviour = BlockEntityBehaviour.get(level, relative, GasTransportBehaviour.TYPE);
        source = Optional.of(behaviour == null ? new Blocked(location) : new OtherPipe(location));
        return true;
    }

    public boolean manageFlows(Level level, BlockPos pos, GasStack internalGas, Predicate<GasStack> extractionPredicate) {
        Optional<GasNetwork> retainedNetwork = network;
        network = Optional.empty();
        if (source.isEmpty() && !determineSource(level, pos)) {
            return false;
        }

        GasFlowSource flowSource = source.get();
        if (flow.isEmpty()) {
            if (!hasPressure()) {
                return false;
            }

            boolean prioritizeInbound = comparePressure() < 0;
            for (boolean trueFalse : Iterate.trueAndFalse) {
                boolean inbound = prioritizeInbound == trueFalse;
                if (pressure.get(inbound) == 0) {
                    continue;
                }
                if (!tryStartingNewFlow(inbound, inbound ? flowSource.provideGas(extractionPredicate) : internalGas)) {
                    continue;
                }

                return true;
            }
            return false;
        }

        AirFlow airFlow = flow.get();
        GasStack provided = airFlow.inbound ? flowSource.provideGas(extractionPredicate) : internalGas;
        if (!hasPressure() || provided.isEmpty() || !GasStack.isSameGasSameComponents(provided, airFlow.gas)) {
            flow = Optional.empty();
            return true;
        }

        if (airFlow.inbound != comparePressure() < 0) {
            boolean inbound = !airFlow.inbound;
            if (inbound && !provided.isEmpty() || !inbound && !internalGas.isEmpty()) {
                GasPropagator.resetAffectedNetworks(level, pos, side);
                tryStartingNewFlow(inbound, inbound ? flowSource.provideGas(extractionPredicate) : internalGas);
                return true;
            }
        }

        flowSource.whileFlowPresent(level, airFlow.inbound);
        if (!flowSource.isEndpoint() || !airFlow.inbound) {
            return false;
        }

        network = retainedNetwork;
        if (network.isEmpty()) {
            network = Optional.of(new GasNetwork(level, new BlockFace(pos, side), flowSource::provideHandler));
        }
        network.get().tick();

        return false;
    }

    private boolean tryStartingNewFlow(boolean inbound, @NotNull GasStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        AirFlow airFlow = new AirFlow(inbound, stack);
        flow = Optional.of(airFlow);
        return true;
    }

    public boolean hasPressure() {
        return getInboundPressure() != 0 || getOutwardPressure() != 0;
    }

    public void tickFlowProgress(Level level, BlockPos pos) {
        if (flow.isEmpty()) {
            return;
        }

        AirFlow airFlow = flow.get();
        if (airFlow.gas.isEmpty() || !level.isClientSide || source.isPresent()) {
            return;
        }

        determineSource(level, pos);
    }

    public void write(@NotNull CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        CompoundTag connectionData = new CompoundTag();
        compoundTag.put(side.getName(), connectionData);
        if (hasPressure()) {
            ListTag pressureData = new ListTag();
            pressureData.add(FloatTag.valueOf(getInboundPressure()));
            pressureData.add(FloatTag.valueOf(getOutwardPressure()));
            connectionData.put(COMPOUND_KEY_PRESSURE, pressureData);
        }

        if (source.orElse(null) instanceof OpenEndedGasPipe) {
            connectionData.put(COMPOUND_KEY_OPEN_END, ((OpenEndedGasPipe) source.get()).write(provider));
        }

        if (flow.isPresent()) {
            CompoundTag flowData = new CompoundTag();
            AirFlow airFlow = flow.get();
            flowData.put(COMPOUND_KEY_GAS, airFlow.gas.saveOptional(provider));
            flowData.putBoolean(COMPOUND_KEY_INBOUND, airFlow.inbound);
            connectionData.put(COMPOUND_KEY_AIR_FLOW, flowData);
        }
    }

    public void read(@NotNull CompoundTag compoundTag, Provider provider, BlockPos blockPos, boolean clientPacket) {
        CompoundTag connectionData = compoundTag.getCompound(side.getName());

        if (connectionData.contains(COMPOUND_KEY_PRESSURE)) {
            ListTag pressureData = connectionData.getList(COMPOUND_KEY_PRESSURE, Tag.TAG_FLOAT);
            pressure = Couple.create(pressureData.getFloat(0), pressureData.getFloat(1));
        }
        else {
            pressure.replace(f -> 0.0f);
        }

        source = Optional.empty();
        if (connectionData.contains(COMPOUND_KEY_OPEN_END)) {
            source = Optional.of(OpenEndedGasPipe.read(connectionData.getCompound(COMPOUND_KEY_OPEN_END), provider, blockPos));
        }

        if (connectionData.contains(COMPOUND_KEY_AIR_FLOW)) {
            CompoundTag flowData = connectionData.getCompound(COMPOUND_KEY_AIR_FLOW);

            GasStack gas = GasStack.parseOptional(provider, flowData.getCompound(COMPOUND_KEY_GAS));
            boolean inbound = flowData.getBoolean(COMPOUND_KEY_INBOUND);
            if (flow.isEmpty()) {
                flow = Optional.of(new AirFlow(inbound, gas));
            }

            AirFlow airFlow = flow.get();
            airFlow.gas = gas;
            airFlow.inbound = inbound;
        }
        else {
            flow = Optional.empty();
        }

    }

    public void wipePressure() {
        pressure.replace(f -> 0.0f);
        if (source.isPresent()) {
            previousSource = source;
        }
        source = Optional.empty();
        resetNetwork();
    }

    public void resetNetwork() {
        network.ifPresent(GasNetwork::reset);
    }

    public GasStack provideOutboundFlow() {
        if (flow.isEmpty()) {
            return GasStack.EMPTY;
        }

        AirFlow airFlow = flow.get();
        if (airFlow.inbound) {
            return GasStack.EMPTY;
        }

        return airFlow.gas;
    }

    public void addPressure(boolean inbound, float newAmount) {
        pressure = pressure.mapWithContext((f, in) -> in == inbound ? f + newAmount : f);
    }

    public Couple<Float> getPressure() {
        return pressure;
    }

    public class AirFlow {
        public boolean inbound;
        public GasStack gas;

        public AirFlow(boolean inbound, GasStack gas) {
            this.inbound = inbound;
            this.gas = gas;
        }
    }
}
