package net.ty.createcraftedbeginning.content.airtights.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record ShortPos(short relativeX, short relativeY, short relativeZ) {
    @Contract("_ -> new")
    public static @NotNull ShortPos fromGlobalPos(@NotNull BlockPos globalPos) {
        short relativeX = (short) (globalPos.getX() & 15);
        short relativeY = (short) globalPos.getY();
        short relativeZ = (short) (globalPos.getZ() & 15);
        return new ShortPos(relativeX, relativeY, relativeZ);
    }

    public @NotNull BlockPos toGlobalPos(@NotNull ChunkPos chunkPos) {
        int globalX = chunkPos.getMinBlockX() + relativeX;
        int globalZ = chunkPos.getMinBlockZ() + relativeZ;
        return new BlockPos(globalX, relativeY, globalZ);
    }


}
