package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserDisplayPacket.Mode;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserEmitterDisplayState {
    private UUID instance = UUID.randomUUID();
    private @Nullable LaserDisplayPacket packet;
    private @Nullable LaserDisplaySnapshot snapshot;

    UUID instance() {
        return instance;
    }

    @Nullable LaserDisplayPacket packet() {
        return packet;
    }

    @Nullable LaserDisplaySnapshot snapshot() {
        return snapshot;
    }

    void restoreInstance(UUID instance) {
        if (this.instance.equals(instance)) {
            return;
        }

        this.instance = instance;
        packet = null;
        snapshot = null;
    }

    void accept(LaserDisplayPacket packet) {
        if (!packet.instance().equals(instance) || this.packet != null && packet.sequence() <= this.packet.sequence()) {
            return;
        }

        LaserDisplaySnapshot snapshot = null;
        if (packet.mode() == Mode.FULL) {
            snapshot = LaserDisplayCodec.decode(packet.data());
        }
        this.snapshot = snapshot;
        this.packet = packet;
    }
}
