package net.ty.createcraftedbeginning.gas.behaviour;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.gas.overpressure.OverpressureState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OverpressureBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<OverpressureBehaviour> TYPE = new BehaviourType<>();

    private static final String COMPOUND_KEY_CHANNELS = "OverpressureChannels";
    private static final int CLIENT_SYNC_RATE = 5;

    private final BooleanSupplier active;
    private final LongSupplier[] currentPressurePa;
    private final OverpressureState[] states;

    private int syncCooldown;
    private boolean syncQueued;

    public OverpressureBehaviour(SmartBlockEntity blockEntity, LongSupplier currentPressurePa) {
        this(blockEntity, () -> true, new LongSupplier[] {currentPressurePa});
    }

    public OverpressureBehaviour(SmartBlockEntity blockEntity, LongSupplier... currentPressurePa) {
        this(blockEntity, () -> true, currentPressurePa);
    }

    public OverpressureBehaviour(SmartBlockEntity blockEntity, BooleanSupplier active, LongSupplier currentPressurePa) {
        this(blockEntity, active, new LongSupplier[] {currentPressurePa});
    }

    public OverpressureBehaviour(SmartBlockEntity blockEntity, BooleanSupplier active, LongSupplier... currentPressurePa) {
        super(blockEntity);
        if (currentPressurePa.length == 0) {
            throw new IllegalArgumentException("Overpressure behaviour requires at least one pressure channel; got " + currentPressurePa.length + '.');
        }

        this.active = active;
        this.currentPressurePa = Arrays.copyOf(currentPressurePa, currentPressurePa.length);
        states = new OverpressureState[currentPressurePa.length];
        Arrays.setAll(states, ignoredIndex -> new OverpressureState());
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public void initialize() {
        super.initialize();
        Level level = blockEntity.getLevel();
        if (!(level != null && !level.isClientSide)) {
            return;
        }

        restartStabilizationGrace();
    }

    @Override
    public void tick() {
        super.tick();
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide || blockEntity.isVirtual()) {
            return;
        }

        tickClientSync();
        if (!active.getAsBoolean()) {
            return;
        }

        boolean stressChanged = false;
        for (int channel = 0; channel < states.length; channel++) {
            stressChanged |= states[channel].tick(currentPressurePa[channel].getAsLong());
        }

        if (!stressChanged) {
            return;
        }

        blockEntity.setChanged();
        queueClientSync();
    }

    @Override
    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        ListTag channelTags = compoundTag.getList(COMPOUND_KEY_CHANNELS, Tag.TAG_COMPOUND);
        for (int channel = 0; channel < states.length; channel++) {
            CompoundTag channelTag = channel < channelTags.size() ? channelTags.getCompound(channel) : new CompoundTag();
            states[channel].readStress(channelTag);
        }

        if (clientPacket) {
            return;
        }

        restartStabilizationGrace();
        syncCooldown = 0;
        syncQueued = false;
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        ListTag channelTags = new ListTag();
        for (OverpressureState state : states) {
            CompoundTag channelTag = new CompoundTag();
            state.writeStress(channelTag);
            channelTags.add(channelTag);
        }
        compoundTag.put(COMPOUND_KEY_CHANNELS, channelTags);
    }

    public int getChannelCount() {
        return states.length;
    }

    public double getStress() {
        return getMaxStress();
    }

    public double getStress(int channel) {
        return state(channel).getStress();
    }

    public float getStressFraction() {
        return (float) getMaxStress();
    }

    @SuppressWarnings("unused")
    public float getStressFraction(int channel) {
        return state(channel).getStressFraction();
    }

    public void setStress(int channel, double stress) {
        if (!state(channel).setStress(stress)) {
            return;
        }

        blockEntity.setChanged();
        Level level = blockEntity.getLevel();
        if (!(level != null && !level.isClientSide && active.getAsBoolean())) {
            return;
        }

        queueClientSync();
    }

    public void setStress(double stress) {
        if (states.length != 1) {
            throw new IllegalStateException("Single-value structural stress transfer requires exactly one pressure channel; got " + states.length + '.');
        }

        setStress(0, stress);
    }

    public double getMaxStress() {
        double maxStress = 0;
        for (OverpressureState state : states) {
            maxStress = Math.max(maxStress, state.getStress());
        }
        return maxStress;
    }

    public int getMostStressedChannel() {
        int mostStressedChannel = 0;
        double maxStress = states[0].getStress();
        for (int channel = 1; channel < states.length; channel++) {
            double stress = states[channel].getStress();
            if (stress <= maxStress) {
                continue;
            }

            maxStress = stress;
            mostStressedChannel = channel;
        }
        return mostStressedChannel;
    }

    @SuppressWarnings("unused")
    public int getStabilizationTicksRemaining() {
        int maxTicksRemaining = 0;
        for (OverpressureState state : states) {
            maxTicksRemaining = Math.max(maxTicksRemaining, state.getStabilizationTicksRemaining());
        }
        return maxTicksRemaining;
    }

    public boolean isStabilizing() {
        for (OverpressureState state : states) {
            if (state.isStabilizing()) {
                return true;
            }
        }
        return false;
    }

    public boolean isAtFailureThreshold() {
        for (OverpressureState state : states) {
            if (state.isAtFailureThreshold()) {
                return true;
            }
        }
        return false;
    }

    public boolean isAtFailureThreshold(int channel) {
        return state(channel).isAtFailureThreshold();
    }

    @SuppressWarnings("unused")
    public boolean isFailureReady() {
        for (OverpressureState state : states) {
            if (state.isFailureReady()) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    public boolean isFailureReady(int channel) {
        return state(channel).isFailureReady();
    }

    public int getFailureReadyChannel() {
        for (int channel = 0; channel < states.length; channel++) {
            if (states[channel].isFailureReady()) {
                return channel;
            }
        }
        return -1;
    }

    public void restartStabilizationGrace() {
        for (OverpressureState state : states) {
            state.restartStabilizationGrace();
        }
    }

    @SuppressWarnings("unused")
    public void restartStabilizationGrace(int channel) {
        state(channel).restartStabilizationGrace();
    }

    private OverpressureState state(int channel) {
        if (channel < 0 || channel >= states.length) {
            throw new IndexOutOfBoundsException("Overpressure channel index must be in [0, " + states.length + "); got " + channel + '.');
        }

        return states[channel];
    }

    private void queueClientSync() {
        syncQueued = true;
        if (!(syncCooldown <= 0)) {
            return;
        }

        flushClientSync();
    }

    private void tickClientSync() {
        if (syncCooldown <= 0) {
            return;
        }

        syncCooldown--;
        if (!(syncCooldown == 0 && syncQueued)) {
            return;
        }

        flushClientSync();
    }

    private void flushClientSync() {
        syncQueued = false;
        syncCooldown = CLIENT_SYNC_RATE;
        blockEntity.sendData();
    }
}
