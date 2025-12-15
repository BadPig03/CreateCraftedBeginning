package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GasCanisterPackContentsData extends SavedData {
    private static final String COMPOUND_KEY_CONTENTS_LIST = "ContentsList";
    private static final String COMPOUND_KEY_UUID = "UUID";
    private static final String DATA_NAME = CreateCraftedBeginning.MOD_ID + "_gas_canister_pack_contents_data";

    private Map<UUID, GasCanisterPackContents> contentsMap = new HashMap<>();

    private GasCanisterPackContentsData() {
    }

    @Contract(value = " -> new", pure = true)
    public static @NotNull Factory<GasCanisterPackContentsData> factory() {
        return new Factory<>(GasCanisterPackContentsData::new, GasCanisterPackContentsData::load);
    }

    public static @NotNull GasCanisterPackContentsData load(@NotNull MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    private static @NotNull GasCanisterPackContentsData load(@NotNull CompoundTag compoundTag, @NotNull Provider provider) {
        GasCanisterPackContentsData data = new GasCanisterPackContentsData();
        data.contentsMap = new HashMap<>();
        NBTHelper.iterateCompoundList(compoundTag.getList(COMPOUND_KEY_CONTENTS_LIST, Tag.TAG_COMPOUND), tag -> {
            GasCanisterPackContents contents = GasCanisterPackContents.read(tag, provider);
            UUID uuid = tag.getUUID(COMPOUND_KEY_UUID);
            data.contentsMap.put(uuid, contents);
        });
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, @NotNull Provider provider) {
        GasCanisterPackContentsDataManager dataManager = CreateCraftedBeginning.GAS_CANISTER_PACK_CONTENTS_DATA_MANAGER;
        compoundTag.put(COMPOUND_KEY_CONTENTS_LIST, NBTHelper.writeCompoundList(dataManager.getContentsMap().values(), contents -> contents.write(provider)));
        return compoundTag;
    }

    public Map<UUID, GasCanisterPackContents> getContentsMap() {
        return contentsMap;
    }
}
