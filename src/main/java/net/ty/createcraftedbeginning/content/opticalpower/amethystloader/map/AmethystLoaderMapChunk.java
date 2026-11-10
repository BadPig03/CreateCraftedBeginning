package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSnapshot.Column;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSnapshot.Tint;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public record AmethystLoaderMapChunk(long chunk, @Nullable Version version, List<Column> columns) {
    private static final int COLUMN_COUNT = 256;

    public AmethystLoaderMapChunk {
        int expected = version == null ? 0 : COLUMN_COUNT;
        int count = columns.size();
        if (count != expected) {
            throw new IllegalArgumentException("Amethyst map chunk column count must be " + expected + "; got " + count + '.');
        }

        columns = List.copyOf(columns);
    }

    public static AmethystLoaderMapChunk read(FriendlyByteBuf buffer) {
        long chunk = buffer.readLong();
        if (!buffer.readBoolean()) {
            return new AmethystLoaderMapChunk(chunk, null, List.of());
        }

        Version version = new Version(buffer.readUUID(), buffer.readVarLong());
        List<Column> columns = new ArrayList<>(COLUMN_COUNT);
        for (int index = 0; index < COLUMN_COUNT; index++) {
            int color = buffer.readByte();
            if (color == Column.WALL.color()) {
                columns.add(Column.WALL);
                continue;
            }

            if (color <= 0) {
                columns.add(color == 0 ? Column.VOID : Column.UNKNOWN);
                continue;
            }

            columns.add(new Column(color, buffer.readInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readEnum(Tint.class)));
        }
        return new AmethystLoaderMapChunk(chunk, version, columns);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeLong(chunk);
        buffer.writeBoolean(version != null);
        if (version == null) {
            return;
        }

        buffer.writeUUID(version.incarnation());
        buffer.writeVarLong(version.revision());
        for (Column column : columns) {
            int color = column.color();
            buffer.writeByte(color);
            if (color <= 0) {
                continue;
            }

            buffer.writeInt(column.height());
            buffer.writeVarInt(column.waterDepth());
            buffer.writeVarInt(column.biome());
            buffer.writeEnum(column.tint());
        }
    }

    record Version(UUID incarnation, long revision) {}
}
