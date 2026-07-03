package net.ty.createcraftedbeginning.api.gas.gases.behaviours;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeAttachmentTypes.AttachmentTypes;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.gases.GasPipeConnection;
import net.ty.createcraftedbeginning.api.gas.gases.GasPipeConnection.AirFlow;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.collisions.GasCollisionEvent;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public abstract class GasTransportBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<GasTransportBehaviour> TYPE = new BehaviourType<>();
    public static final WorldAttached<Map<BlockPos, Map<Direction, GasPipeConnection>>> interfaceTransfer = new WorldAttached<>($ -> new HashMap<>());

    public Map<Direction, GasPipeConnection> interfaces;
    public UpdatePhase phase;

    public GasTransportBehaviour(SmartBlockEntity be) {
        super(be);
        phase = UpdatePhase.WAIT_FOR_PUMPS;
    }

    public static void cacheFlows(LevelAccessor level, BlockPos pos) {
        GasTransportBehaviour pipe = get(level, pos, TYPE);
        if (pipe == null) {
            return;
        }

        interfaceTransfer.get(level).put(pos, pipe.interfaces);
    }

    public static void loadFlows(LevelAccessor level, BlockPos pos) {
        GasTransportBehaviour newPipe = get(level, pos, TYPE);
        if (newPipe == null) {
            return;
        }

        newPipe.interfaces = interfaceTransfer.get(level).remove(pos);
    }

    public static boolean isValidAirtightComponents(Level level, BlockPos pos, BlockState state, Direction direction) {
        return state.canBeReplaced() && state.getDestroySpeed(level, pos) != -1 || GasCapabilities.hasGasCapability(level, pos, direction) || state.getBlock() instanceof IAirtightComponent airtightComponent && airtightComponent.isAirtight(pos, state, direction);
    }

    public abstract boolean canHaveFlowToward(BlockState state, Direction direction);

    public GasStack getProvidedOutwardGas(Direction side) {
        createConnectionData();
        if (!interfaces.containsKey(side)) {
            return GasStack.EMPTY;
        }

        return interfaces.get(side).provideOutboundFlow();
    }

    private void createConnectionData() {
        if (interfaces != null) {
            return;
        }

        interfaces = new IdentityHashMap<>();
        for (Direction direction : Iterate.directions) {
            if (!canHaveFlowToward(blockEntity.getBlockState(), direction)) {
                continue;
            }

            interfaces.put(direction, new GasPipeConnection(direction));
        }
    }

    @Nullable
    public GasPipeConnection getConnection(Direction side) {
        createConnectionData();
        return interfaces.get(side);
    }

    public boolean hasAnyPressure() {
        createConnectionData();
        for (GasPipeConnection pipeConnection : interfaces.values()) {
            if (pipeConnection.hasPressure()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public AirFlow getFlow(Direction side) {
        createConnectionData();
        if (!interfaces.containsKey(side)) {
            return null;
        }

        return interfaces.get(side).flow.orElse(null);
    }

    public void addPressure(Direction side, boolean inbound, float pressure) {
        createConnectionData();
        if (!interfaces.containsKey(side)) {
            return;
        }

        interfaces.get(side).addPressure(inbound, pressure);
        blockEntity.sendData();
    }

    public void wipePressure() {
        if (interfaces != null) {
            for (Direction direction : Iterate.directions) {
                if (canHaveFlowToward(blockEntity.getBlockState(), direction)) {
                    interfaces.computeIfAbsent(direction, GasPipeConnection::new);
                }
                else {
                    interfaces.remove(direction);
                }
            }
        }
        phase = UpdatePhase.WAIT_FOR_PUMPS;
        createConnectionData();
        interfaces.values().forEach(GasPipeConnection::wipePressure);
        blockEntity.sendData();
    }

    public boolean isIncorrectAxis(BlockState state, Direction direction) {
        return state.getValue(BlockStateProperties.AXIS) != direction.getAxis();
    }

    public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction direction) {
        if (!canHaveFlowToward(state, direction)) {
            return AttachmentTypes.NONE;
        }

        BlockPos offsetPos = pos.relative(direction);
        BlockState facingState = level.getBlockState(offsetPos);
        if (facingState.getBlock() instanceof AirtightPumpBlock && facingState.getValue(AirtightPumpBlock.FACING) == direction.getOpposite()) {
            return AttachmentTypes.NONE;
        }

        if (GasCapabilities.hasGasCapability(level, offsetPos, direction.getOpposite())) {
            return AttachmentTypes.DRAIN;
        }

        return AttachmentTypes.RIM;
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public void initialize() {
        super.initialize();
        createConnectionData();
    }

    @Override
    public void tick() {
        super.tick();
        Level level = getWorld();
        BlockPos pos = getPos();
        boolean onServer = !level.isClientSide || blockEntity.isVirtual();
        if (interfaces == null) {
            return;
        }

        Collection<GasPipeConnection> connections = interfaces.values();
        GasPipeConnection singleSource = null;
        if (phase == UpdatePhase.WAIT_FOR_PUMPS) {
            phase = UpdatePhase.FLIP_FLOWS;
            return;
        }

        if (onServer) {
            boolean sendUpdate = false;
            for (GasPipeConnection connection : connections) {
                sendUpdate |= connection.flipFlowsIfPressureReversed();
                connection.manageSource(level, pos, blockEntity);
            }
            if (sendUpdate) {
                blockEntity.notifyUpdate();
            }
        }

        if (phase == UpdatePhase.FLIP_FLOWS) {
            phase = UpdatePhase.IDLE;
            return;
        }

        if (onServer) {
            GasStack availableFlow = GasStack.EMPTY;
            GasStack collidingFlow = GasStack.EMPTY;
            for (GasPipeConnection connection : connections) {
                GasStack gasInFlow = connection.getProvidedGas();
                if (gasInFlow.isEmpty()) {
                    continue;
                }
                if (availableFlow.isEmpty()) {
                    singleSource = connection;
                    availableFlow = gasInFlow;
                    continue;
                }
                if (GasStack.isSameGasSameComponents(availableFlow, gasInFlow)) {
                    singleSource = null;
                    availableFlow = gasInFlow;
                    continue;
                }

                collidingFlow = gasInFlow;
                break;
            }

            if (!collidingFlow.isEmpty()) {
                GasCollisionEvent.handleCollision(level, pos, availableFlow, collidingFlow);
                return;
            }

            boolean sendUpdate = false;
            for (GasPipeConnection connection : connections) {
                GasStack internalGas = singleSource == connection ? GasStack.EMPTY : availableFlow;
                Predicate<GasStack> extractionPredicate = extracted -> canPullGasFrom(extracted, blockEntity.getBlockState(), connection.side);
                sendUpdate |= connection.manageFlows(level, pos, internalGas, extractionPredicate);
            }
            if (sendUpdate) {
                blockEntity.notifyUpdate();
            }
        }

        for (GasPipeConnection connection : connections) {
            connection.tickFlowProgress(level, pos);
        }
    }

    @Override
    public void read(CompoundTag compoundTag, Provider registries, boolean clientPacket) {
        super.read(compoundTag, registries, clientPacket);
        if (interfaces == null) {
            interfaces = new IdentityHashMap<>();
        }
        for (Direction face : Iterate.directions) {
            if (compoundTag.contains(face.getName())) {
                interfaces.computeIfAbsent(face, GasPipeConnection::new);
            }
        }

        if (interfaces.isEmpty()) {
            interfaces = null;
            return;
        }

        interfaces.values().forEach(connection -> connection.read(compoundTag, registries, blockEntity.getBlockPos(), clientPacket));
    }

    @Override
    public void write(CompoundTag compoundTag, Provider registries, boolean clientPacket) {
        super.write(compoundTag, registries, clientPacket);
        if (clientPacket) {
            createConnectionData();
        }
        if (interfaces == null) {
            return;
        }

        interfaces.values().forEach(connection -> connection.write(compoundTag, registries, clientPacket));
    }

    public boolean canPullGasFrom(GasStack gas, BlockState state, Direction direction) {
        return true;
    }

    public enum UpdatePhase {
        WAIT_FOR_PUMPS,
        FLIP_FLOWS,
        IDLE
    }
}
