package net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkMoving.Host;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LoadingNetworkMovingSerialization {
    private static final String COMPOUND_KEY_HOST = "Host";
    private static final String COMPOUND_KEY_DIMENSION = "Dimension";
    private static final String COMPOUND_KEY_POSITION = "Position";
    private static final String COMPOUND_KEY_SUPPORT = "Support";
    private static final String COMPOUND_KEY_MEMBERS = "Members";
    private static final String COMPOUND_KEY_IDENTITY = "Identity";
    private static final String COMPOUND_KEY_INDEPENDENT = "Independent";

    private LoadingNetworkMovingSerialization() {
    }

    static List<StoredHost> read(ListTag saved) {
        List<StoredHost> result = new ArrayList<>();
        for (Tag element : saved) {
            CompoundTag tag = (CompoundTag) element;
            UUID identity = tag.getUUID(COMPOUND_KEY_HOST);
            GlobalPos position = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString(COMPOUND_KEY_DIMENSION))), BlockPos.of(tag.getLong(COMPOUND_KEY_POSITION)));
            List<BlockPos> support = Arrays.stream(tag.getLongArray(COMPOUND_KEY_SUPPORT)).mapToObj(BlockPos::of).toList();
            Set<UUID> members = new HashSet<>();
            for (Tag elementMember : tag.getList(COMPOUND_KEY_MEMBERS, Tag.TAG_COMPOUND)) {
                UUID member = ((CompoundTag) elementMember).getUUID(COMPOUND_KEY_IDENTITY);
                members.add(member);
            }
            result.add(new StoredHost(identity, new Host(position, support, members, tag.getBoolean(COMPOUND_KEY_INDEPENDENT))));
        }
        return result;
    }

    static ListTag write(Map<UUID, Host> hosts) {
        ListTag result = new ListTag();
        hosts.forEach((identity, host) -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(COMPOUND_KEY_HOST, identity);
            GlobalPos position = host.position();
            tag.putString(COMPOUND_KEY_DIMENSION, position.dimension().location().toString());
            tag.putLong(COMPOUND_KEY_POSITION, position.pos().asLong());
            tag.putLongArray(COMPOUND_KEY_SUPPORT, host.support().stream().mapToLong(BlockPos::asLong).toArray());
            tag.putBoolean(COMPOUND_KEY_INDEPENDENT, host.independent());
            ListTag members = new ListTag();
            for (UUID member : host.members()) {
                CompoundTag saved = new CompoundTag();
                saved.putUUID(COMPOUND_KEY_IDENTITY, member);
                members.add(saved);
            }
            tag.put(COMPOUND_KEY_MEMBERS, members);
            result.add(tag);
        });
        return result;
    }

    record StoredHost(UUID identity, Host host) {}
}
