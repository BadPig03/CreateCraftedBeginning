package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapChunk;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapLayer;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AmethystLoaderMapSyncPacket(int containerId, UUID request, UUID identity, long generation, long sequence, ResourceKey<Level> dimension, AmethystLoaderMapLayer layer, List<AmethystLoaderMapChunk> chunks) implements ClientboundPacketPayload {
    private static final int MAX_CHUNKS = 81;
    public static final StreamCodec<RegistryFriendlyByteBuf, AmethystLoaderMapSyncPacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> {
        buffer.writeVarInt(packet.containerId());
        buffer.writeUUID(packet.request());
        buffer.writeUUID(packet.identity());
        buffer.writeVarLong(packet.generation());
        buffer.writeVarLong(packet.sequence());
        buffer.writeResourceKey(packet.dimension());
        packet.layer().write(buffer);
        buffer.writeCollection(packet.chunks(), (output, chunk) -> chunk.write(output));
    }, AmethystLoaderMapSyncPacket::read);

    public AmethystLoaderMapSyncPacket {
        int count = chunks.size();
        if (count > MAX_CHUNKS) {
            throw new IllegalArgumentException("Amethyst map chunk count must be at most " + MAX_CHUNKS + "; got " + count + '.');
        }

        chunks = List.copyOf(chunks);
    }

    private static AmethystLoaderMapSyncPacket read(RegistryFriendlyByteBuf buffer) {
        int container = buffer.readVarInt();
        UUID request = buffer.readUUID();
        UUID identity = buffer.readUUID();
        long generation = buffer.readVarLong();
        long sequence = buffer.readVarLong();
        ResourceKey<Level> dimension = buffer.readResourceKey(Registries.DIMENSION);
        AmethystLoaderMapLayer layer = AmethystLoaderMapLayer.read(buffer);
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_CHUNKS) {
            throw new IllegalArgumentException("Amethyst map chunk count must be in [0, " + MAX_CHUNKS + "]; got " + size + '.');
        }

        List<AmethystLoaderMapChunk> chunks = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            chunks.add(AmethystLoaderMapChunk.read(buffer));
        }
        return new AmethystLoaderMapSyncPacket(container, request, identity, generation, sequence, dimension, layer, chunks);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (!(player.containerMenu instanceof AmethystLoaderMenu menu) || menu.containerId != containerId) {
            return;
        }

        menu.maps().accept(this);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AMETHYST_LOADER_MAP_SYNC;
    }
}
