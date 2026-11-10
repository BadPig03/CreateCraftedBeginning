package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMappedMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamPathSignature.Input;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamWork;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderSnapshot;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRigidDisplay;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRigidDisplay.Candidate;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserStaticDependencies;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceObservation;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserDisplayPacket.Mode;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RenderSource;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class LaserServerDisplayQueue {
    private static final long WORLD_QUANTUM_NS = 2000000;
    private static final int BASELINE_RESEND_INTERVAL_TICKS = 20;
    private static final Map<ServerLevel, World> WORLDS = new IdentityHashMap<>();

    private LaserServerDisplayQueue() {}

    static void request(LaserEmitterBlockEntity owner, ServerPlayer player) {
        if (!(owner.getLevel() instanceof ServerLevel level)) {
            return;
        }

        World world = WORLDS.get(level);
        if (world == null) {
            return;
        }

        Source source = world.sources.get(owner);
        if (source == null || !source.players().contains(player)) {
            return;
        }

        source.deliveryFor(player.getUUID()).requestBaseline();
    }

    static void update(LaserEmitterBlockEntity owner, @Nullable BeamPropagation propagation, @Nullable LaserTraceObservation dependencies) {
        if (!(owner.getLevel() instanceof ServerLevel level) || owner.isVirtual()) {
            return;
        }

        World world = WORLDS.computeIfAbsent(level, ignored -> new World());
        Source source = world.sources.computeIfAbsent(owner, key -> new Source(level, key));
        if (source.initialized && source.propagation == propagation && source.rigid == null) {
            return;
        }

        BeamState local = owner.createBeamSource();
        int range = owner.getLaserRange();
        if (propagation != null && source.rigid != null && local.equals(source.localSource) && range == source.range && source.matches()) {
            return;
        }

        source.initialized = true;
        source.propagation = propagation;
        source.localSource = local;
        source.range = range;
        source.revision++;
        source.clearPreparation();
        world.queue.remove(source);
        source.dependencies = null;
        source.rigid = null;
        if (propagation == null) {
            source.publish(Mode.OFF, new byte[0]);
            return;
        }

        RenderSource host = SubLevelBridge.captureRenderSource(level, Vec3.atCenterOf(owner.getBlockPos()), 1);
        if (host != null) {
            Vec3 scale = host.geometry().scale();
            Vec3 previousScale = source.lastScale;
            source.lastScale = scale;
            if (dependencies != null && (previousScale == null || previousScale.equals(scale))) {
                Candidate candidate = dependencies.captureRigid(level, owner.getBlockPos(), propagation);
                if (candidate != null) {
                    source.rigid = candidate.rigid();
                    source.dependencies = candidate.dependencies();
                    source.reference = candidate.reference();
                }
            }
        }
        else if (dependencies != null) {
            source.lastScale = null;
            source.dependencies = dependencies.staticDependencies();
            Vec3 origin = Vec3.atLowerCornerOf(owner.getBlockPos());
            source.reference = new LaserRenderSnapshot(propagation, origin, origin, new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1));
        }
        if (source.dependencies == null) {
            source.publish(Mode.LOCAL, new byte[0]);
            return;
        }

        source.publish(Mode.PREPARING, new byte[0]);
    }

    static void remove(LaserEmitterBlockEntity owner) {
        if (!(owner.getLevel() instanceof ServerLevel level)) {
            return;
        }

        World world = WORLDS.get(level);
        if (world == null) {
            return;
        }

        Source source = world.sources.remove(owner);
        if (source == null) {
            return;
        }

        world.queue.remove(source);
        source.clearPreparation();
        source.publish(Mode.OFF, new byte[0]);
    }

    @SubscribeEvent
    private static void tick(Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        World world = WORLDS.get(level);
        if (world == null) {
            return;
        }

        for (Source source : List.copyOf(world.sources.values())) {
            BlockPos pos = source.owner.getBlockPos();
            if (source.owner.isRemoved() || !level.isLoaded(pos) || level.getBlockEntity(pos) != source.owner) {
                remove(source.owner);
                continue;
            }

            List<ServerPlayer> players = source.players();
            source.deliveries.keySet().removeIf(id -> players.stream().noneMatch(player -> player.getUUID().equals(id)));
            source.send(players);
            LaserRenderSnapshot reference = source.reference;
            if (players.isEmpty() || source.work != null || source.packet.mode() != Mode.PREPARING || reference == null) {
                continue;
            }

            source.work = LaserBeamMappedMesh.beginPrepare(reference, source.rigid == null);
            world.queue.addLast(source);
        }
        long deadline = System.nanoTime() + WORLD_QUANTUM_NS;
        Map<Source, Boolean> checked = new IdentityHashMap<>();
        while (!world.queue.isEmpty() && System.nanoTime() < deadline) {
            Source source = world.queue.removeFirst();
            LaserBeamWork<LaserBeamDisplayInput> work = source.work;
            LaserStaticDependencies dependencies = source.dependencies;
            LaserRenderSnapshot reference = source.reference;
            if (work == null || dependencies == null || reference == null) {
                continue;
            }

            if (!checked.containsKey(source)) {
                if (!source.matches()) {
                    source.clearPreparation();
                    source.dependencies = null;
                    source.rigid = null;
                    source.publish(Mode.LOCAL, new byte[0]);
                    continue;
                }

                checked.put(source, true);
            }
            if (!work.step()) {
                world.queue.addLast(source);
                continue;
            }

            LaserBeamDisplayInput input = work.result();
            source.clearPreparation();
            if (source.rigid != null && !input.base().isComplete()) {
                source.publish(Mode.LOCAL, new byte[0]);
                continue;
            }

            BeamState local = source.localSource;
            BeamFrame frame = local.frame();
            LaserDisplaySnapshot snapshot = new LaserDisplaySnapshot(frame.origin(), frame.direction(), local.fluxLm(), source.range, dependencies, input, Input.from(reference), source.rigid);
            byte[] data = LaserDisplayCodec.encode(snapshot);
            if (data == null) {
                source.publish(Mode.TOO_LARGE, new byte[0]);
                continue;
            }

            source.publish(Mode.FULL, data);
        }
    }

    @SubscribeEvent
    private static void unload(Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        WORLDS.remove(level);
    }

    private static final class World {
        private final Map<LaserEmitterBlockEntity, Source> sources = new IdentityHashMap<>();
        private final ArrayDeque<Source> queue = new ArrayDeque<>();
    }

    private static final class Source {
        private final ServerLevel level;
        private final LaserEmitterBlockEntity owner;
        private final Map<UUID, Delivery> deliveries = new HashMap<>();
        private @Nullable BeamPropagation propagation;
        private @Nullable LaserStaticDependencies dependencies;
        private @Nullable LaserRigidDisplay rigid;
        private @Nullable Vec3 lastScale;
        private @Nullable LaserRenderSnapshot reference;
        private @Nullable LaserBeamWork<LaserBeamDisplayInput> work;
        private BeamState localSource;
        private int range;
        private LaserDisplayPacket packet;
        private long sequence;
        private long revision;
        private boolean initialized;

        private Source(ServerLevel level, LaserEmitterBlockEntity owner) {
            this.level = level;
            this.owner = owner;
            packet = new LaserDisplayPacket(level.dimension().location(), owner.getBlockPos(), owner.getDisplayInstance(), ++sequence, revision, level.getGameTime(), Mode.OFF, new byte[0]);
        }

        private List<ServerPlayer> players() {
            return SubLevelBridge.trackingPlayers(level, owner.getBlockPos());
        }

        private Delivery deliveryFor(UUID playerId) {
            return deliveries.computeIfAbsent(playerId, ignored -> new Delivery());
        }

        private void clearPreparation() {
            reference = null;
            work = null;
        }

        private boolean matches() {
            if (dependencies == null) {
                return false;
            }

            if (rigid != null) {
                return rigid.matches(level, owner.getBlockPos(), dependencies, 1);
            }

            return dependencies.matches(level, 1);
        }

        private void publish(Mode mode, byte[] data) {
            if (mode == Mode.LOCAL && packet.mode() == Mode.LOCAL) {
                return;
            }

            packet = new LaserDisplayPacket(level.dimension().location(), owner.getBlockPos(), owner.getDisplayInstance(), ++sequence, revision, level.getGameTime(), mode, data);
            send(players());
        }

        private void send(List<ServerPlayer> players) {
            if (players.isEmpty()) {
                return;
            }

            LaserDisplayPacket current = packet;
            long sequence = current.sequence();
            long tick = level.getGameTime();
            for (ServerPlayer player : players) {
                Delivery delivery = deliveryFor(player.getUUID());
                if (!delivery.shouldSend(sequence, tick)) {
                    continue;
                }

                CatnipServices.NETWORK.sendToClient(player, current);
                delivery.markSent(sequence, tick);
            }
        }
    }

    private static final class Delivery {
        private long sentSequence;
        private long nextBaselineTick;
        private boolean baselinePending;

        private void requestBaseline() {
            baselinePending = true;
        }

        private boolean shouldSend(long sequence, long tick) {
            return sequence != sentSequence || baselinePending && tick >= nextBaselineTick;
        }

        private void markSent(long sequence, long tick) {
            sentSequence = sequence;
            if (!baselinePending) {
                return;
            }

            baselinePending = false;
            nextBaselineTick = tick + BASELINE_RESEND_INTERVAL_TICKS;
        }
    }
}
