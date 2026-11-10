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
import net.minecraft.world.level.ChunkPos;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Network;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Node;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.PhysicalNode;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LoadingNetworkSerialization {
    private static final String COMPOUND_KEY_NETWORKS = "Networks";
    private static final String COMPOUND_KEY_ID = "Id";
    private static final String COMPOUND_KEY_OWNER = "Owner";
    private static final String COMPOUND_KEY_NAME = "Name";
    private static final String COMPOUND_KEY_CUSTOM_COLOR = "CustomColor";
    private static final String COMPOUND_KEY_DISPLAY_NAME = "DisplayName";
    private static final String COMPOUND_KEY_NODES = "Nodes";
    private static final String COMPOUND_KEY_LOADER_ID = "LoaderId";
    private static final String COMPOUND_KEY_ANTENNA_ID = "AntennaId";
    private static final String COMPOUND_KEY_DIMENSION = "Dimension";
    private static final String COMPOUND_KEY_LOADER_POS = "LoaderPos";
    private static final String COMPOUND_KEY_ANTENNA_POS = "AntennaPos";
    private static final String COMPOUND_KEY_NETWORK_ID = "NetworkId";
    private static final String COMPOUND_KEY_ENABLED = "Enabled";
    private static final String COMPOUND_KEY_BEAM_ENABLED = "BeamEnabled";
    private static final String COMPOUND_KEY_ASSEMBLED = "Assembled";
    private static final String COMPOUND_KEY_TRAINS = "Trains";
    private static final String COMPOUND_KEY_MOVING = "Moving";
    private static final String COMPOUND_KEY_PHYSICAL = "Physical";
    private static final String COMPOUND_KEY_SUB_LEVEL = "SubLevel";
    private static final String COMPOUND_KEY_HOLDING_CHUNK = "HoldingChunk";
    private static final String COMPOUND_KEY_DETACHED = "Detached";
    private static final String COMPOUND_KEY_TRANSFER = "CCBLoadingNetworkTransfer";

    private LoadingNetworkSerialization() {
    }

    static Contents read(CompoundTag compoundTag) {
        Contents data = new Contents(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), compoundTag.getList(COMPOUND_KEY_MOVING, Tag.TAG_COMPOUND));
        ListTag networks = compoundTag.getList(COMPOUND_KEY_NETWORKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < networks.size(); i++) {
            CompoundTag entry = networks.getCompound(i);
            UUID id = entry.getUUID(COMPOUND_KEY_ID);
            String name = entry.getString(COMPOUND_KEY_NAME);
            Integer customColor = entry.contains(COMPOUND_KEY_CUSTOM_COLOR, Tag.TAG_INT) ? entry.getInt(COMPOUND_KEY_CUSTOM_COLOR) : null;
            data.networks.put(id, new Network(entry.getUUID(COMPOUND_KEY_OWNER), name, NbtValues.getBooleanOrDefault(entry, COMPOUND_KEY_ENABLED, true), LoadingNetworkColors.from(id), customColor));
        }
        ListTag nodes = compoundTag.getList(COMPOUND_KEY_NODES, Tag.TAG_COMPOUND);
        for (int i = 0; i < nodes.size(); i++) {
            Node node = readNode(nodes.getCompound(i));
            data.nodes.put(node.loader(), node);
        }
        ListTag assembled = compoundTag.getList(COMPOUND_KEY_ASSEMBLED, Tag.TAG_COMPOUND);
        for (int i = 0; i < assembled.size(); i++) {
            Node node = readNode(assembled.getCompound(i));
            data.assembled.put(node.loaderId(), node);
        }
        ListTag trains = compoundTag.getList(COMPOUND_KEY_TRAINS, Tag.TAG_COMPOUND);
        for (int i = 0; i < trains.size(); i++) {
            Node node = readNode(trains.getCompound(i));
            data.trainMembers.put(node.loaderId(), node);
        }
        ListTag detached = compoundTag.getList(COMPOUND_KEY_DETACHED, Tag.TAG_COMPOUND);
        for (int i = 0; i < detached.size(); i++) {
            CompoundTag entry = detached.getCompound(i);
            GlobalPos pos = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(entry.getString(COMPOUND_KEY_DIMENSION))), BlockPos.of(entry.getLong(COMPOUND_KEY_LOADER_POS)));
            data.detached.put(pos, entry.getUUID(COMPOUND_KEY_LOADER_ID));
        }
        ListTag physical = compoundTag.getList(COMPOUND_KEY_PHYSICAL, Tag.TAG_COMPOUND);
        for (int i = 0; i < physical.size(); i++) {
            CompoundTag entry = physical.getCompound(i);
            Node node = readNode(entry);
            data.physical.put(node.loaderId(), new PhysicalNode(entry.getUUID(COMPOUND_KEY_SUB_LEVEL), node, entry.contains(COMPOUND_KEY_HOLDING_CHUNK) ? new ChunkPos(entry.getLong(COMPOUND_KEY_HOLDING_CHUNK)) : null));
        }
        return data;
    }

    static CompoundTag write(CompoundTag compoundTag, Contents data) {
        ListTag savedNetworks = new ListTag();
        data.networks.forEach((id, network) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(COMPOUND_KEY_ID, id);
            entry.putUUID(COMPOUND_KEY_OWNER, network.owner());
            entry.putString(COMPOUND_KEY_NAME, network.name());
            entry.putBoolean(COMPOUND_KEY_ENABLED, network.enabled());
            Integer customColor = network.customColor();
            if (customColor != null) {
                entry.putInt(COMPOUND_KEY_CUSTOM_COLOR, customColor);
            }
            savedNetworks.add(entry);
        });
        compoundTag.put(COMPOUND_KEY_NETWORKS, savedNetworks);
        ListTag savedNodes = new ListTag();
        for (Node node : data.nodes.values()) {
            savedNodes.add(writeNode(node));
        }
        compoundTag.put(COMPOUND_KEY_NODES, savedNodes);
        ListTag savedAssembled = new ListTag();
        for (Node node : data.assembled.values()) {
            savedAssembled.add(writeNode(node));
        }
        compoundTag.put(COMPOUND_KEY_ASSEMBLED, savedAssembled);
        ListTag savedTrains = new ListTag();
        for (Node node : data.trainMembers.values()) {
            savedTrains.add(writeNode(node));
        }
        compoundTag.put(COMPOUND_KEY_TRAINS, savedTrains);
        ListTag savedDetached = new ListTag();
        data.detached.forEach((pos, identity) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString(COMPOUND_KEY_DIMENSION, pos.dimension().location().toString());
            entry.putLong(COMPOUND_KEY_LOADER_POS, pos.pos().asLong());
            entry.putUUID(COMPOUND_KEY_LOADER_ID, identity);
            savedDetached.add(entry);
        });
        compoundTag.put(COMPOUND_KEY_DETACHED, savedDetached);
        ListTag savedPhysical = new ListTag();
        for (PhysicalNode member : data.physical.values()) {
            CompoundTag entry = writeNode(member.node());
            entry.putUUID(COMPOUND_KEY_SUB_LEVEL, member.host());
            ChunkPos holdingChunk = member.holdingChunk();
            if (holdingChunk != null) {
                entry.putLong(COMPOUND_KEY_HOLDING_CHUNK, holdingChunk.toLong());
            }
            savedPhysical.add(entry);
        }
        compoundTag.put(COMPOUND_KEY_PHYSICAL, savedPhysical);
        compoundTag.put(COMPOUND_KEY_MOVING, data.moving);
        return compoundTag;
    }

    static Node readNode(CompoundTag compoundTag) {
        ResourceLocation dimension = ResourceLocation.parse(compoundTag.getString(COMPOUND_KEY_DIMENSION));
        GlobalPos loader = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension), BlockPos.of(compoundTag.getLong(COMPOUND_KEY_LOADER_POS)));
        GlobalPos antenna = GlobalPos.of(loader.dimension(), BlockPos.of(compoundTag.getLong(COMPOUND_KEY_ANTENNA_POS)));
        UUID network = compoundTag.hasUUID(COMPOUND_KEY_NETWORK_ID) ? compoundTag.getUUID(COMPOUND_KEY_NETWORK_ID) : null;
        return new Node(compoundTag.getUUID(COMPOUND_KEY_LOADER_ID), loader, compoundTag.getUUID(COMPOUND_KEY_ANTENNA_ID), antenna, network, NbtValues.getBooleanOrDefault(compoundTag, COMPOUND_KEY_ENABLED, true), NbtValues.getBooleanOrDefault(compoundTag, COMPOUND_KEY_BEAM_ENABLED, true), compoundTag.getString(COMPOUND_KEY_DISPLAY_NAME));
    }

    static @Nullable Node readTransfer(@Nullable CompoundTag compoundTag) {
        if (compoundTag == null || !compoundTag.contains(COMPOUND_KEY_TRANSFER, Tag.TAG_COMPOUND)) {
            return null;
        }

        return readNode(compoundTag.getCompound(COMPOUND_KEY_TRANSFER));
    }

    static void writeTransfer(CompoundTag compoundTag, Node node) {
        compoundTag.put(COMPOUND_KEY_TRANSFER, writeNode(node));
    }

    private static CompoundTag writeNode(Node node) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putUUID(COMPOUND_KEY_LOADER_ID, node.loaderId());
        compoundTag.putString(COMPOUND_KEY_DISPLAY_NAME, node.name());
        compoundTag.putBoolean(COMPOUND_KEY_ENABLED, node.enabled());
        compoundTag.putBoolean(COMPOUND_KEY_BEAM_ENABLED, node.beamEnabled());
        compoundTag.putUUID(COMPOUND_KEY_ANTENNA_ID, node.antennaId());
        GlobalPos loader = node.loader();
        compoundTag.putString(COMPOUND_KEY_DIMENSION, loader.dimension().location().toString());
        compoundTag.putLong(COMPOUND_KEY_LOADER_POS, loader.pos().asLong());
        compoundTag.putLong(COMPOUND_KEY_ANTENNA_POS, node.antenna().pos().asLong());
        UUID network = node.networkId();
        if (network != null) {
            compoundTag.putUUID(COMPOUND_KEY_NETWORK_ID, network);
        }
        return compoundTag;
    }

    record Contents(Map<UUID, Network> networks, Map<GlobalPos, Node> nodes, Map<GlobalPos, UUID> detached, Map<UUID, Node> assembled, Map<UUID, Node> trainMembers, Map<UUID, PhysicalNode> physical, ListTag moving) {}
}
