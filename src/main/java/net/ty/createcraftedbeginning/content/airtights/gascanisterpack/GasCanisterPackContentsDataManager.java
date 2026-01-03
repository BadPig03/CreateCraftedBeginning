package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GasCanisterPackContentsDataManager {
    private GasCanisterPackContentsData savedData;
    private Map<UUID, GasCanisterPackContents> contentsMap = new HashMap<>();

    public void onLevelLoad(@NotNull LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null || server.overworld() != level) {
            return;
        }

        savedData = GasCanisterPackContentsData.load(server);
        contentsMap = savedData.getContentsMap();
    }

    public GasCanisterPackContents getContents(@NotNull UUID uuid) {
        return contentsMap.computeIfAbsent(uuid, GasCanisterPackContents::new);
    }

    public void addContents(@NotNull UUID uuid, GasCanisterPackContents contents) {
        contentsMap.put(uuid, contents);
        markDirty();
    }

    public void removeContents(@NotNull UUID uuid) {
        contentsMap.remove(uuid);
        markDirty();
    }

    public Map<UUID, GasCanisterPackContents> getContentsMap() {
        return contentsMap;
    }

    public void markDirty() {
        if (savedData == null) {
            return;
        }

        savedData.setDirty();
    }
}
