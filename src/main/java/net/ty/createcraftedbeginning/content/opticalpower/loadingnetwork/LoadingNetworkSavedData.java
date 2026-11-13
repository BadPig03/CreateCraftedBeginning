package net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSerialization.Contents;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LoadingNetworkSavedData extends SavedData {
    public static final int UNBOUND_COLOR = 0xFFFFFFFF;
    private static final String DATA_NAME = "createcraftedbeginning_loading_networks";
    private static final Factory<LoadingNetworkSavedData> FACTORY = new Factory<>(LoadingNetworkSavedData::new, LoadingNetworkSavedData::load);
    private final Map<UUID, Network> networks;
    private final Map<GlobalPos, Node> nodes;
    private final Map<GlobalPos, UUID> detached;
    private final Map<UUID, Node> assembled;
    private final Map<UUID, Node> trainMembers;
    private final Map<UUID, PhysicalNode> physical;
    private final Set<GlobalPos> placements = new HashSet<>();
    private final Set<GlobalPos> changed = new HashSet<>();
    private final LoadingNetworkRuntime runtime = new LoadingNetworkRuntime(this);
    private final LoadingNetworkTrains trains = new LoadingNetworkTrains();
    private final LoadingNetworkMoving moving = new LoadingNetworkMoving(this);

    LoadingNetworkSavedData() {
        this(new Contents(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new ListTag()));
    }

    private LoadingNetworkSavedData(Contents contents) {
        networks = contents.networks();
        nodes = contents.nodes();
        detached = contents.detached();
        assembled = contents.assembled();
        trainMembers = contents.trainMembers();
        physical = contents.physical();
        moving.load(contents.moving());
    }

    public static LoadingNetworkSavedData get(ServerLevel level) {
        ServerLevel storageLevel = level;
        if (level.dimension() != Level.OVERWORLD) {
            storageLevel = level.getServer().overworld();
        }
        return storageLevel.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    static LoadingNetworkSavedData load(CompoundTag tag, Provider provider) {
        return new LoadingNetworkSavedData(LoadingNetworkSerialization.read(tag));
    }

    @Override
    public CompoundTag save(CompoundTag tag, Provider provider) {
        return LoadingNetworkSerialization.write(tag, new Contents(networks, nodes, detached, assembled, trainMembers, physical, moving.save()));
    }

    public LoadingNetworkBinding create(UUID owner) {
        UUID id = UUID.randomUUID();
        String name = id.toString().substring(0, 8);
        networks.put(id, new Network(owner, name, LoadingNetworkColors.from(id), null));
        setDirty();
        return new LoadingNetworkBinding(id, name);
    }

    public boolean owns(UUID network, UUID player) {
        Network entry = networks.get(network);
        return entry != null && entry.owner().equals(player);
    }

    public @Nullable LoadingNetworkBinding binding(UUID id) {
        Network entry = networks.get(id);
        if (entry == null) {
            return null;
        }

        return new LoadingNetworkBinding(id, entry.name());
    }

    public boolean rename(UUID id, UUID player, String name) {
        String trimmed = name.strip();
        Network current = networks.get(id);
        if (current == null || !current.owner().equals(player) || trimmed.isEmpty() || trimmed.length() > LoadingNetworkBinding.MAX_NAME_LENGTH || trimmed.codePoints().anyMatch(Character::isISOControl)) {
            return false;
        }

        networks.put(id, new Network(current.owner(), trimmed, current.defaultColor(), current.customColor()));
        setDirty();
        return true;
    }

    public int color(GlobalPos pos, @Nullable UUID identity) {
        if (identity == null) {
            return UNBOUND_COLOR;
        }

        Node node = nodes.get(pos);
        if (node == null || !node.loaderId().equals(identity)) {
            PhysicalNode physicalNode = physical.get(identity);
            node = physicalNode == null ? trainMembers.get(identity) : physicalNode.node();
            if (node == null) {
                node = assembled.get(identity);
            }
        }
        if (node == null || node.networkId() == null) {
            return UNBOUND_COLOR;
        }

        return color(node.networkId());
    }

    public int color(UUID id) {
        Network network = networks.get(id);
        if (network == null) {
            return UNBOUND_COLOR;
        }

        return network.color();
    }

    public int defaultColor(UUID id) {
        Network network = networks.get(id);
        if (network == null) {
            return UNBOUND_COLOR;
        }

        return network.defaultColor();
    }

    public @Nullable Integer customColor(UUID id) {
        Network network = networks.get(id);
        if (network == null) {
            return null;
        }

        return network.customColor();
    }

    public boolean setColor(UUID id, UUID player, @Nullable Integer rgb) {
        Network current = networks.get(id);
        if (current == null || !current.owner().equals(player) || rgb != null && (rgb < 0 || rgb > 0xFFFFFF)) {
            return false;
        }

        if (Objects.equals(current.customColor(), rgb)) {
            return true;
        }

        networks.put(id, new Network(current.owner(), current.name(), current.defaultColor(), rgb));
        setDirty();
        return true;
    }

    public @Nullable Node nodeAt(GlobalPos loader) {
        return nodes.get(loader);
    }

    public boolean register(Node node) {
        GlobalPos loader = node.loader();
        UUID network = node.networkId();
        Node existing = nodes.get(loader);
        if (existing != null && !existing.antennaId().equals(node.antennaId())) {
            return false;
        }

        if (network != null && !networks.containsKey(network)) {
            return false;
        }

        if (existing != null && existing.loaderId().equals(node.loaderId())) {
            node = node.withEnabled(existing.enabled()).withBeamEnabled(existing.beamEnabled());
        }
        if (node.equals(existing)) {
            return true;
        }

        nodes.put(loader, node);
        detached.remove(loader);
        changed.add(loader);
        setDirty();
        return true;
    }

    public boolean register(PhysicalNode member) {
        Node node = member.node();
        PhysicalNode existing = physical.get(node.loaderId());
        Node previous = existing == null ? null : existing.node();
        if (previous != null && !previous.antennaId().equals(node.antennaId())) {
            return false;
        }

        UUID network = node.networkId();
        if (network != null && !networks.containsKey(network)) {
            return false;
        }

        if (previous != null) {
            node = node.withEnabled(previous.enabled()).withBeamEnabled(previous.beamEnabled());
        }
        rememberPhysical(new PhysicalNode(member.host(), node));
        return true;
    }

    public void updateName(GlobalPos loader, UUID identity, String name) {
        Node fixed = nodes.get(loader);
        if (fixed != null && fixed.loaderId().equals(identity) && !fixed.name().equals(name)) {
            nodes.put(loader, fixed.withName(name));
            setDirty();
        }
        PhysicalNode member = physical.get(identity);
        if (member == null) {
            return;
        }

        Node node = member.node();
        if (!node.loader().equals(loader) || node.name().equals(name)) {
            return;
        }

        rememberPhysical(new PhysicalNode(member.host(), node.withName(name), member.holdingChunk()));
    }

    public void removeAntenna(GlobalPos antenna, UUID identity) {
        if (physical.values().removeIf(member -> {
            Node node = member.node();
            return node.antenna().equals(antenna) && node.antennaId().equals(identity);
        })) {
            setDirty();
        }
        for (Node node : List.copyOf(nodes.values())) {
            if (!node.antenna().equals(antenna) || !node.antennaId().equals(identity)) {
                continue;
            }

            GlobalPos loader = node.loader();
            nodes.remove(loader);
            detached.put(loader, node.loaderId());
            changed.add(loader);
            setDirty();
        }
    }

    public void removeLoader(GlobalPos loader) {
        if (physical.values().removeIf(member -> member.node().loader().equals(loader))) {
            setDirty();
        }
        Node removed = nodes.remove(loader);
        UUID pending = detached.remove(loader);
        if (removed == null && pending == null) {
            return;
        }

        changed.add(loader);
        setDirty();
    }

    public LoadingNetworkRuntime runtime() {
        return runtime;
    }

    public LoadingNetworkMoving moving() {
        return moving;
    }

    public boolean reconcileFixedLoading(GlobalPos pos, @Nullable UUID identity) {
        if (placements.contains(pos)) {
            return false;
        }

        Node node = nodes.get(pos);
        if (node == null) {
            return true;
        }

        if (!node.loaderId().equals(identity)) {
            removeLoader(pos);
            return true;
        }

        return enabled(node);
    }

    public @Nullable Node trainNode(UUID identity) {
        return trainMembers.get(identity);
    }

    public boolean allowsTrain(@Nullable UUID identity) {
        Node node = trainMembers.get(identity);
        return node == null || enabled(node);
    }

    public @Nullable Node member(UUID network, UUID identity) {
        for (Node node : nodes.values()) {
            if (!node.loaderId().equals(identity) || !network.equals(node.networkId())) {
                continue;
            }

            return node;
        }
        PhysicalNode physicalNode = physical.get(identity);
        Node node = physicalNode == null ? trainMembers.get(identity) : physicalNode.node();
        if (node == null) {
            node = assembled.get(identity);
        }
        if (node == null || !network.equals(node.networkId())) {
            return null;
        }

        return node;
    }

    public List<Node> members(UUID network) {
        List<Node> members = new ArrayList<>();
        for (Node node : nodes.values()) {
            if (!network.equals(node.networkId())) {
                continue;
            }

            members.add(node);
        }
        members.sort(Comparator.comparing((Node node) -> node.loader().dimension().location().toString()).thenComparingLong(node -> node.loader().pos().asLong()));
        return List.copyOf(members);
    }

    public boolean isPlacing(GlobalPos pos) {
        return placements.contains(pos);
    }

    public List<Node> assembledMembers(UUID network) {
        Map<UUID, Node> members = new HashMap<>(assembled);
        members.putAll(trainMembers);
        for (PhysicalNode member : physical.values()) {
            Node node = member.node();
            members.put(node.loaderId(), node);
        }
        return members.values().stream().filter(node -> network.equals(node.networkId())).sorted(Comparator.comparing(Node::loaderId)).toList();
    }

    public List<PhysicalNode> physicalNodes() {
        return List.copyOf(physical.values());
    }

    public @Nullable PhysicalNode physicalNode(@Nullable UUID identity) {
        return physical.get(identity);
    }

    public boolean allowsPhysical(@Nullable UUID identity) {
        PhysicalNode member = physical.get(identity);
        return member == null || enabled(member.node());
    }

    public void rememberPhysical(PhysicalNode member) {
        Node node = member.node();
        UUID identity = node.loaderId();
        UUID host = member.host();
        PhysicalNode previous = physical.get(identity);
        if (member.holdingChunk() == null && previous != null && previous.host().equals(host) && previous.node().loader().dimension() == node.loader().dimension()) {
            member = new PhysicalNode(host, node, previous.holdingChunk());
        }
        if (member.equals(physical.put(identity, member))) {
            return;
        }

        setDirty();
    }

    public void forgetPhysical(UUID identity) {
        if (physical.remove(identity) == null) {
            return;
        }

        setDirty();
    }

    public void removeSubLevel(ResourceKey<Level> dimension, UUID host) {
        if (!physical.values().removeIf(member -> member.host().equals(host) && member.node().loader().dimension() == dimension)) {
            return;
        }

        setDirty();
    }

    public void beginPlacement(GlobalPos pos) {
        placements.add(pos);
    }

    public void endPlacement(GlobalPos pos) {
        placements.remove(pos);
    }

    public List<Node> nodes() {
        return List.copyOf(nodes.values());
    }

    public boolean setBeamEnabled(UUID network, UUID player, UUID identity, boolean enabled) {
        if (!owns(network, player)) {
            return false;
        }

        for (Entry<GlobalPos, Node> entry : nodes.entrySet()) {
            Node node = entry.getValue();
            if (!node.loaderId().equals(identity) || !network.equals(node.networkId())) {
                continue;
            }

            entry.setValue(node.withBeamEnabled(enabled));
            setDirty();
            return true;
        }
        Node ordinary = assembled.get(identity);
        if (ordinary != null && network.equals(ordinary.networkId())) {
            assembled.put(identity, ordinary.withBeamEnabled(enabled));
            setDirty();
            return true;
        }

        PhysicalNode physicalNode = physical.get(identity);
        Node physical = physicalNode == null ? null : physicalNode.node();
        if (physical != null && network.equals(physical.networkId())) {
            rememberPhysical(new PhysicalNode(physicalNode.host(), physical.withBeamEnabled(enabled), physicalNode.holdingChunk()));
            return true;
        }

        Node train = trainMembers.get(identity);
        if (train == null || !network.equals(train.networkId())) {
            return false;
        }

        trainMembers.put(identity, train.withBeamEnabled(enabled));
        setDirty();
        return true;
    }

    public boolean contraptionBeamEnabled(@Nullable UUID identity, boolean train) {
        Node member = train ? trainMembers.get(identity) : assembled.get(identity);
        return member == null || member.beamEnabled();
    }

    LoadingNetworkTrains trains() {
        return trains;
    }

    List<Node> trainMembers() {
        return List.copyOf(trainMembers.values());
    }

    void updateTrainMembers(Map<UUID, Node> members) {
        for (UUID identity : members.keySet()) {
            if (assembled.remove(identity) == null) {
                continue;
            }

            setDirty();
        }
        members.replaceAll((identity, node) -> {
            Node previous = trainMembers.get(identity);
            if (previous == null || !previous.antennaId().equals(node.antennaId()) || !Objects.equals(previous.networkId(), node.networkId())) {
                return node;
            }

            return node.withEnabled(previous.enabled()).withBeamEnabled(previous.beamEnabled());
        });
        if (trainMembers.equals(members)) {
            return;
        }

        trainMembers.clear();
        trainMembers.putAll(members);
        setDirty();
    }

    @Nullable Node assemblyNode(UUID identity) {
        return assembled.get(identity);
    }

    void rememberAssembly(Node node) {
        UUID identity = node.loaderId();
        Node previous = assembled.get(identity);
        if (previous != null && previous.antennaId().equals(node.antennaId()) && Objects.equals(previous.networkId(), node.networkId())) {
            node = node.withEnabled(previous.enabled()).withBeamEnabled(previous.beamEnabled());
        }
        if (node.equals(assembled.put(identity, node))) {
            return;
        }

        setDirty();
    }

    void forgetAssembly(UUID identity) {
        Node train = trainMembers.remove(identity);
        Node ordinary = assembled.remove(identity);
        if (train == null && ordinary == null) {
            return;
        }

        setDirty();
    }

    boolean enabled(Node node) {
        UUID network = node.networkId();
        return network == null || node.enabled() && networks.containsKey(network);
    }

    boolean setNodeEnabled(UUID network, UUID player, UUID identity, boolean enabled) {
        if (!owns(network, player)) {
            return false;
        }

        for (Node node : members(network)) {
            if (!node.loaderId().equals(identity)) {
                continue;
            }

            GlobalPos loader = node.loader();
            nodes.put(loader, node.withEnabled(enabled));
            changed.add(loader);
            setDirty();
            return true;
        }
        Node ordinary = assembled.get(identity);
        if (ordinary != null && moving.contains(identity) && network.equals(ordinary.networkId())) {
            assembled.put(identity, ordinary.withEnabled(enabled));
            setDirty();
            return true;
        }

        PhysicalNode physicalNode = physical.get(identity);
        Node node = physicalNode == null ? null : physicalNode.node();
        if (node != null && network.equals(node.networkId())) {
            rememberPhysical(new PhysicalNode(physicalNode.host(), node.withEnabled(enabled), physicalNode.holdingChunk()));
            return true;
        }

        Node train = trainMembers.get(identity);
        if (train == null || !network.equals(train.networkId())) {
            return false;
        }

        trainMembers.put(identity, train.withEnabled(enabled));
        setDirty();
        return true;
    }

    @Nullable UUID detachedAt(GlobalPos pos) {
        return detached.get(pos);
    }

    @Contract(pure = true)
    @Unmodifiable
    Set<GlobalPos> detachedPositions() {
        return Set.copyOf(detached.keySet());
    }

    void finishDetached(GlobalPos pos) {
        if (detached.remove(pos) == null) {
            return;
        }

        setDirty();
    }

    @Unmodifiable
    Set<GlobalPos> takeChanges() {
        Set<GlobalPos> result = Set.copyOf(changed);
        changed.clear();
        return result;
    }

    record Network(UUID owner, String name, int defaultColor, @Nullable Integer customColor) {
        int color() {
            if (customColor == null) {
                return defaultColor;
            }

            return 0xFF000000 | customColor;
        }
    }

    public record PhysicalNode(UUID host, Node node, @Nullable ChunkPos holdingChunk) {
        public PhysicalNode(UUID host, Node node) {
            this(host, node, null);
        }
    }

    public record Node(UUID loaderId, GlobalPos loader, UUID antennaId, GlobalPos antenna, @Nullable UUID networkId, boolean enabled, boolean beamEnabled, String name) {
        public Node {
            loader = GlobalPos.of(loader.dimension(), loader.pos().immutable());
            antenna = GlobalPos.of(antenna.dimension(), antenna.pos().immutable());
        }

        Node withEnabled(boolean enabled) {
            return new Node(loaderId, loader, antennaId, antenna, networkId, enabled, beamEnabled, name);
        }

        Node withBeamEnabled(boolean beamEnabled) {
            return new Node(loaderId, loader, antennaId, antenna, networkId, enabled, beamEnabled, name);
        }

        Node withName(String name) {
            return new Node(loaderId, loader, antennaId, antenna, networkId, enabled, beamEnabled, name);
        }
    }
}
