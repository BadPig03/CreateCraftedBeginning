package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record LaserDisplayPacket(ResourceLocation dimension, BlockPos pos, UUID instance, long sequence, long revision, long serverTick, Mode mode, byte[] data) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, LaserDisplayPacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> {
        buffer.writeVarInt(LaserDisplayCodec.FORMAT);
        buffer.writeVarInt(LaserDisplayCodec.QUALITY);
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeBlockPos(packet.pos);
        buffer.writeUUID(packet.instance);
        buffer.writeVarLong(packet.sequence);
        buffer.writeVarLong(packet.revision);
        buffer.writeVarLong(packet.serverTick);
        buffer.writeEnum(packet.mode);
        buffer.writeByteArray(packet.data);
    }, buffer -> {
        int format = buffer.readVarInt();
        int quality = buffer.readVarInt();
        if (format != LaserDisplayCodec.FORMAT || quality != LaserDisplayCodec.QUALITY) {
            throw new IllegalArgumentException("Unsupported laser display format or quality: " + format + '/' + quality + '.');
        }

        return new LaserDisplayPacket(buffer.readResourceLocation(), buffer.readBlockPos(), buffer.readUUID(), buffer.readVarLong(), buffer.readVarLong(), buffer.readVarLong(), buffer.readEnum(Mode.class), buffer.readByteArray(LaserDisplayCodec.MAX_PACKET_BYTES));
    });

    public LaserDisplayPacket {
        pos = pos.immutable();
        data = data.clone();
        if (sequence < 1 || revision < 0 || data.length > LaserDisplayCodec.MAX_PACKET_BYTES || (mode == Mode.FULL) == (data.length == 0)) {
            throw new IllegalArgumentException("Laser display sequence must be positive, revision non-negative, payload length at most " + LaserDisplayCodec.MAX_PACKET_BYTES + " bytes, positive in FULL mode and zero otherwise; got sequence=" + sequence + ", revision=" + revision + ", mode=" + mode + ", payload=" + data.length + " bytes.");
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        Level level = player.level();
        if (!level.dimension().location().equals(dimension) || !level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof LaserEmitterBlockEntity emitter)) {
            return;
        }

        emitter.acceptDisplay(this);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.LASER_DISPLAY;
    }

    @Override
    public byte[] data() {
        return data.clone();
    }

    public enum Mode {
        PREPARING,
        FULL,
        LOCAL,
        TOO_LARGE,
        OFF
    }
}
