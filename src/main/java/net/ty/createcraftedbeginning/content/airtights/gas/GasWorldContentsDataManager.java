package net.ty.createcraftedbeginning.content.airtights.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class GasWorldContentsDataManager {
    private GasWorldContentsData savedData;
    private Map<BlockPos, GasWorldContents> contentsMap;

    public void onLevelLoad(@NotNull LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null || server.overworld() != level) {
            return;
        }

        savedData = GasWorldContentsData.load(server);
        contentsMap = savedData.getContentsMap();
    }

    public GasWorldContents getContents(@NotNull BlockPos blockPos) {
        return contentsMap.computeIfAbsent(blockPos, GasWorldContents::new);
    }

    public void addContents(@NotNull BlockPos blockPos, GasWorldContents contents) {
        contentsMap.put(blockPos, contents);
        markDirty();
    }

    public void removeContents(@NotNull BlockPos blockPos) {
        contentsMap.remove(blockPos);
        markDirty();
    }

    public Map<BlockPos, GasWorldContents> getContentsMap() {
        return contentsMap;
    }

    public void markDirty() {
        if (savedData == null) {
            return;
        }

        savedData.setDirty();
    }

    public void tick(Level level) {

    }
}
