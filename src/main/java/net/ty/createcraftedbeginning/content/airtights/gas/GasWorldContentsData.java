package net.ty.createcraftedbeginning.content.airtights.gas;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
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

public class GasWorldContentsData extends SavedData {
    private static final String COMPOUND_KEY_CONTENTS_LIST = "ContentsList";
    private static final String COMPOUND_KEY_BLOCK_POS = "BlockPos";
    private static final String DATA_NAME = CreateCraftedBeginning.MOD_ID + "_gas_data";

    private Map<BlockPos, GasWorldContents> contentsMap = new HashMap<>();

    private GasWorldContentsData() {
    }

    @Contract(value = " -> new", pure = true)
    public static @NotNull Factory<GasWorldContentsData> factory() {
        return new Factory<>(GasWorldContentsData::new, GasWorldContentsData::load);
    }

    public static @NotNull GasWorldContentsData load(@NotNull MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    private static @NotNull GasWorldContentsData load(@NotNull CompoundTag compoundTag, @NotNull Provider provider) {
        GasWorldContentsData data = new GasWorldContentsData();
        data.contentsMap = new HashMap<>();
        NBTHelper.iterateCompoundList(compoundTag.getList(COMPOUND_KEY_CONTENTS_LIST, Tag.TAG_COMPOUND), tag -> {
            GasWorldContents contents = GasWorldContents.read(tag);
            BlockPos blockPos = NBTHelper.readBlockPos(tag, COMPOUND_KEY_BLOCK_POS);
            data.contentsMap.put(blockPos, contents);
        });
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, @NotNull Provider provider) {
        GasWorldContentsDataManager dataManager = CreateCraftedBeginning.GAS_WORLD_CONTENTS_DATA_MANAGER;
        compoundTag.put(COMPOUND_KEY_CONTENTS_LIST, NBTHelper.writeCompoundList(dataManager.getContentsMap().values(), GasWorldContents::write));
        return compoundTag;
    }

    public Map<BlockPos, GasWorldContents> getContentsMap() {
        return contentsMap;
    }
}
