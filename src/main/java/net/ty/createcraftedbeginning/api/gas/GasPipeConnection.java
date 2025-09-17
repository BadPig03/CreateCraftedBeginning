package net.ty.createcraftedbeginning.api.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Predicate;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public class GasPipeConnection {
    public Direction side;
    Couple<Float> pressure;

    Optional<GasFlowSource> source;
    Optional<GasFlowSource> previousSource;
    Optional<AirFlow> flow;
    Optional<GasNetwork> network;

    public GasPipeConnection(Direction side) {
        this.side = side;
        pressure = Couple.create(() -> 0f);
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
        AirFlow airFlow = this.flow.get();
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
        AirFlow airFlow = this.flow.get();
        if (!singlePressure || comparePressure() < 0 == airFlow.inbound) {
            return false;
        }
        airFlow.inbound = !airFlow.inbound;
        return true;
    }

    public void manageSource(Level world, BlockPos pos, BlockEntity blockEntity) {
        if (source.isEmpty() && !determineSource(world, pos)) {
            return;
        }
        GasFlowSource flowSource = source.get();
        flowSource.manageSource(world, blockEntity);
    }

    public boolean manageFlows(Level world, BlockPos pos, GasStack internalGas, Predicate<GasStack> extractionPredicate) {
        Optional<GasNetwork> retainedNetwork = network;
        network = Optional.empty();

        if (source.isEmpty() && !determineSource(world, pos)) {
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
                if (tryStartingNewFlow(inbound, inbound ? flowSource.provideGas(extractionPredicate) : internalGas)) {
                    return true;
                }
            }
            return false;
        }

        AirFlow airFlow = this.flow.get();
        GasStack provided = airFlow.inbound ? flowSource.provideGas(extractionPredicate) : internalGas;
        if (!hasPressure() || provided.isEmpty() || !GasStack.isSameGas(provided, airFlow.gas)) {
            this.flow = Optional.empty();
            return true;
        }

        if (airFlow.inbound != comparePressure() < 0) {
            boolean inbound = !airFlow.inbound;
            if (inbound && !provided.isEmpty() || !inbound && !internalGas.isEmpty()) {
                GasPropagator.resetAffectedNetworks(world, pos, side);
                tryStartingNewFlow(inbound, inbound ? flowSource.provideGas(extractionPredicate) : internalGas);
                return true;
            }
        }

        flowSource.whileFlowPresent(world, airFlow.inbound);

        if (!flowSource.isEndpoint()) {
            return false;
        }
        if (!airFlow.inbound) {
            return false;
        }

        network = retainedNetwork;
        if (network.isEmpty()) {
            network = Optional.of(new GasNetwork(world, new BlockFace(pos, side), flowSource::provideHandler));
        }
        network.get().tick();

        return false;
    }

    private boolean tryStartingNewFlow(boolean inbound, @NotNull GasStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        AirFlow flow = new AirFlow(inbound, stack);
        this.flow = Optional.of(flow);
        return true;
    }

    public boolean determineSource(@NotNull Level world, @NotNull BlockPos pos) {
        BlockPos relative = pos.relative(side);
        if (world.getChunk(relative.getX() >> 4, relative.getZ() >> 4, ChunkStatus.FULL, false) == null) {
            return false;
        }

        BlockFace location = new BlockFace(pos, side);
        if (GasPropagator.isOpenEnd(world, pos, side)) {
            if (previousSource.orElse(null) instanceof OpenEndedGasPipe) {
                source = previousSource;
            } else {
                source = Optional.of(new OpenEndedGasPipe(location));
            }
            return true;
        }

        if (GasPropagator.hasGasCapability(world, location.getConnectedPos(), side.getOpposite())) {
            source = Optional.of(new GasFlowSource.GasHandler(location));
            return true;
        }

        GasTransportBehaviour behaviour = BlockEntityBehaviour.get(world, relative, GasTransportBehaviour.TYPE);
        source = Optional.of(behaviour == null ? new GasFlowSource.Blocked(location) : new GasFlowSource.OtherPipe(location));
        return true;
    }

    public void tickFlowProgress(Level world, BlockPos pos) {
        if (flow.isEmpty()) {
            return;
        }
        AirFlow airFlow = this.flow.get();
        if (airFlow.gas.isEmpty()) {
            return;
        }

        if (!world.isClientSide || source.isPresent()) {
            return;
        }
        determineSource(world, pos);
    }

    public void serializeNBT(@NotNull CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        CompoundTag connectionData = new CompoundTag();
        tag.put(side.getName(), connectionData);

        if (hasPressure()) {
            ListTag pressureData = new ListTag();
            pressureData.add(FloatTag.valueOf(getInboundPressure()));
            pressureData.add(FloatTag.valueOf(getOutwardPressure()));
            connectionData.put("Pressure", pressureData);
        }

        if (source.orElse(null) instanceof OpenEndedGasPipe) {
            connectionData.put("OpenEnd", ((OpenEndedGasPipe) source.get()).serializeNBT(registries));
        }

        if (flow.isPresent()) {
            CompoundTag flowData = new CompoundTag();
            AirFlow airFlow = this.flow.get();
            flowData.put("Gas", airFlow.gas.saveOptional(registries));
            flowData.putBoolean("Inbound", airFlow.inbound);
            connectionData.put("AirFlow", flowData);
        }
    }

    public void deserializeNBT(@NotNull CompoundTag tag, HolderLookup.Provider registries, BlockPos blockEntityPos, boolean clientPacket) {
        CompoundTag connectionData = tag.getCompound(side.getName());

        if (connectionData.contains("Pressure")) {
            ListTag pressureData = connectionData.getList("Pressure", Tag.TAG_FLOAT);
            pressure = Couple.create(pressureData.getFloat(0), pressureData.getFloat(1));
        } else {
            pressure.replace(f -> 0f);
        }

        source = Optional.empty();
        if (connectionData.contains("OpenEnd")) {
            source = Optional.of(OpenEndedGasPipe.fromNBT(connectionData.getCompound("OpenEnd"), registries, blockEntityPos));
        }

        if (connectionData.contains("AirFlow")) {
            CompoundTag flowData = connectionData.getCompound("AirFlow");

            GasStack gas = GasStack.parseOptional(registries, flowData.getCompound("Gas"));
            boolean inbound = flowData.getBoolean("Inbound");
            if (flow.isEmpty()) {
                flow = Optional.of(new AirFlow(inbound, gas));
            }
            AirFlow airFlow = this.flow.get();
            airFlow.gas = gas;
            airFlow.inbound = inbound;
        } else {
            flow = Optional.empty();
        }

    }

    public float comparePressure() {
        return getOutwardPressure() - getInboundPressure();
    }

    public void wipePressure() {
        this.pressure.replace(f -> 0f);
        if (this.source.isPresent()) {
            this.previousSource = this.source;
        }
        this.source = Optional.empty();
        resetNetwork();
    }

    public GasStack provideOutboundFlow() {
        if (flow.isEmpty()) {
            return GasStack.EMPTY;
        }
        AirFlow airFlow = this.flow.get();
        if (airFlow.inbound) {
            return GasStack.EMPTY;
        }
        return airFlow.gas;
    }

    public void addPressure(boolean inbound, float pressure) {
        this.pressure = this.pressure.mapWithContext((f, in) -> in == inbound ? f + pressure : f);
    }

    public Couple<Float> getPressure() {
        return pressure;
    }

    public boolean hasPressure() {
        return getInboundPressure() != 0 || getOutwardPressure() != 0;
    }

    float getOutwardPressure() {
        return pressure.getSecond();
    }

    float getInboundPressure() {
        return pressure.getFirst();
    }

    public void resetNetwork() {
        network.ifPresent(GasNetwork::reset);
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
