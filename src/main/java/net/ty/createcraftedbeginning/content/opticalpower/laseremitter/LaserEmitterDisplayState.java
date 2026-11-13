package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserDisplayPacket.Mode;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserEmitterDisplayState {
    private static final int BASELINE_RETRY_INTERVAL_TICKS = 20;

    private UUID instance = UUID.randomUUID();
    private @Nullable LaserDisplayPacket packet;
    private @Nullable LaserDisplaySnapshot snapshot;
    private boolean allocationRejected;
    private long rejectedSequence;
    private long nextRetryTick = Long.MIN_VALUE;
    private Retry retry = Retry.NONE;

    UUID instance() {
        return instance;
    }

    @Nullable LaserDisplayPacket packet() {
        return packet;
    }

    @Nullable LaserDisplaySnapshot snapshot() {
        return snapshot;
    }

    boolean isSuspended() {
        return allocationRejected || rejectedSequence != 0;
    }

    boolean rejectAllocation() {
        if (allocationRejected) {
            return false;
        }

        allocationRejected = true;
        return true;
    }

    void acceptAllocation(@Nullable UUID instance) {
        allocationRejected = false;
        if (instance == null || this.instance.equals(instance)) {
            return;
        }

        this.instance = instance;
        packet = null;
        snapshot = null;
        rejectedSequence = 0;
        nextRetryTick = Long.MIN_VALUE;
        retry = Retry.NONE;
    }

    boolean takeBaselineRetry(long tick) {
        if (allocationRejected || retry != Retry.PENDING || tick < nextRetryTick) {
            return false;
        }

        retry = Retry.REQUESTED;
        nextRetryTick = tick + BASELINE_RETRY_INTERVAL_TICKS;
        return true;
    }

    void accept(LaserDisplayPacket packet) {
        long sequence = packet.sequence();
        boolean retrying = sequence == rejectedSequence;
        if (!packet.instance().equals(instance) || this.packet != null && sequence <= this.packet.sequence() || sequence < rejectedSequence || retrying && (retry != Retry.REQUESTED || packet.mode() != Mode.FULL)) {
            return;
        }

        LaserDisplaySnapshot snapshot = null;
        if (packet.mode() == Mode.FULL) {
            try {
                snapshot = LaserDisplayCodec.decode(packet.data());
            }
            catch (RuntimeException failure) {
                if (rejectedSequence == 0) {
                    CCBAPI.LOGGER.warn("Failed to decode the laser display at {} in dimension '{}' for instance '{}' and sequence {}; suspending this display until a valid update arrives.", packet.pos(), packet.dimension(), instance, sequence, failure);
                }

                rejectedSequence = sequence;
                retry = retrying ? Retry.NONE : Retry.PENDING;
                return;
            }
        }
        this.snapshot = snapshot;
        this.packet = packet;
        rejectedSequence = 0;
        retry = Retry.NONE;
    }

    private enum Retry {
        NONE,
        PENDING,
        REQUESTED
    }
}
