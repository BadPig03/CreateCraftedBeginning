package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.management;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkBinding;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkRuntime.Status;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record ChunkStabilizerSnapshot(@Nullable UUID network, String name, boolean editable, int color, int defaultColor, boolean customized, int total, int assembled, List<Member> members) {
    public ChunkStabilizerSnapshot {
        members = List.copyOf(members);
    }

    static ChunkStabilizerSnapshot read(RegistryFriendlyByteBuf buffer) {
        UUID network = buffer.readNullable(input -> input.readUUID());
        String name = buffer.readUtf(LoadingNetworkBinding.MAX_NAME_LENGTH);
        boolean editable = buffer.readBoolean();
        int color = buffer.readInt();
        int defaultColor = buffer.readInt();
        boolean customized = buffer.readBoolean();
        int total = buffer.readVarInt();
        int assembled = buffer.readVarInt();
        List<Member> members = buffer.readList(input -> new Member(input.readUUID(), input.readGlobalPos(), input.readBoolean(), input.readBoolean(), input.readEnum(Status.class), input.readBoolean(), input.readBoolean(), input.readBoolean(), input.readUtf()));
        return new ChunkStabilizerSnapshot(network, name, editable, color, defaultColor, customized, total, assembled, members);
    }

    void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeNullable(network, (output, identity) -> output.writeUUID(Objects.requireNonNull(identity)));
        buffer.writeUtf(name, LoadingNetworkBinding.MAX_NAME_LENGTH);
        buffer.writeBoolean(editable);
        buffer.writeInt(color);
        buffer.writeInt(defaultColor);
        buffer.writeBoolean(customized);
        buffer.writeVarInt(total);
        buffer.writeVarInt(assembled);
        buffer.writeCollection(members, (output, member) -> {
            output.writeUUID(member.identity());
            output.writeGlobalPos(member.position());
            output.writeBoolean(member.enabled());
            output.writeBoolean(member.beamEnabled());
            output.writeEnum(member.status());
            output.writeBoolean(member.train());
            output.writeBoolean(member.moving());
            output.writeBoolean(member.physical());
            output.writeUtf(member.name());
        });
    }

    public record Member(UUID identity, GlobalPos position, boolean enabled, boolean beamEnabled, Status status, boolean train, boolean moving, boolean physical, String name) {
        public Member {
            position = GlobalPos.of(position.dimension(), position.pos().immutable());
        }
    }
}
