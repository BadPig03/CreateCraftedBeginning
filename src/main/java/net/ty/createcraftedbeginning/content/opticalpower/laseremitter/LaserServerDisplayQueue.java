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
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamPathSignature.Input;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamWork;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserCollisionContext;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderSnapshot;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRigidDisplay.Candidate;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRigidDisplay;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserStaticDependencies;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceObservation;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserDisplayPacket.Mode;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RenderSource;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class LaserServerDisplayQueue {
    private static final long WORLD_QUANTUM_NS = 2000000;
    private static final int BASELINE_RESEND_INTERVAL_TICKS = 20;
    private static final int DISPLAY_RETRY_DELAY_TICKS = 20;
    private static final byte[] NO_DATA = new byte[0];
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

    static void update(LaserEmitterBlockEntity owner, @Nullable BeamPropagation propagation, @Nullable LaserTraceObservation dependencies, @Nullable LaserCollisionContext collision) {
        if (!(owner.getLevel() instanceof ServerLevel level) || owner.isVirtual()) {
            return;
        }

        World world = WORLDS.computeIfAbsent(level, ignored -> new World());
        Source source = world.sources.computeIfAbsent(owner, key -> new Source(level, key));
        if (propagation == null) {
            source.stop(world);
            return;
        }

        if (level.getGameTime() < source.retryTick || source.propagation == propagation && source.rigid == null) {
            return;
        }

        Mode mode;
        try {
            mode = source.prepare(world, propagation, dependencies, collision);
        }
        catch (RuntimeException failure) {
            source.fail(world, failure);
            return;
        }

        if (mode == null || mode == Mode.PREPARING && source.retryTick != Long.MIN_VALUE) {
            return;
        }

        source.retryTick = Long.MIN_VALUE;
        source.publish(mode, NO_DATA);
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

        source.discard(world);
        source.publish(Mode.OFF, NO_DATA);
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
            if (players.isEmpty() || source.work != null || reference == null || source.dependencies == null) {
                continue;
            }

            try {
                source.work = LaserBeamDisplayMesh.beginPrepare(reference);
            }
            catch (RuntimeException failure) {
                source.fail(world, failure);
                continue;
            }

            world.queue.addLast(source);
        }
        long deadline = System.nanoTime() + WORLD_QUANTUM_NS;
        Map<Source, Boolean> checked = new IdentityHashMap<>();
        while (!world.queue.isEmpty() && System.nanoTime() < deadline) {
            Source source = world.queue.removeFirst();
            PreparedDisplay prepared;
            try {
                prepared = source.advance(world, checked.put(source, true) == null);
            }
            catch (RuntimeException failure) {
                source.fail(world, failure);
                continue;
            }
            if (prepared == null) {
                continue;
            }

            source.retryTick = Long.MIN_VALUE;
            source.publish(prepared.mode(), prepared.data());
        }
    }

    @SubscribeEvent
    private static void unload(Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        WORLDS.remove(level);
    }

    private record PreparedDisplay(Mode mode, byte[] data) {}

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
        private @Nullable BeamState localSource;
        private int range;
        private LaserDisplayPacket packet;
        private long sequence;
        private long revision;
        private long retryTick = Long.MIN_VALUE;
        private boolean initialized;

        private Source(ServerLevel level, LaserEmitterBlockEntity owner) {
            this.level = level;
            this.owner = owner;
            packet = new LaserDisplayPacket(level.dimension().location(), owner.getBlockPos(), owner.getDisplayInstance(), ++sequence, revision, level.getGameTime(), Mode.OFF, NO_DATA);
        }

        private @Nullable Mode prepare(World world, BeamPropagation propagation, @Nullable LaserTraceObservation observation, @Nullable LaserCollisionContext collision) {
            BeamState local = owner.createBeamSource();
            int range = owner.getLaserRange();
            if (rigid != null && local.equals(localSource) && range == this.range && matches()) {
                return null;
            }

            if (collision != null) {
                observation = LaserTraceObservation.capture(level, propagation, collision, 1, true);
            }
            discard(world);
            initialized = true;
            this.propagation = propagation;
            localSource = local;
            this.range = range;
            revision++;

            BlockPos pos = owner.getBlockPos();
            RenderSource host = SubLevelBridge.captureRenderSource(level, Vec3.atCenterOf(pos), 1);
            if (host != null) {
                Vec3 scale = host.geometry().scale();
                Vec3 previousScale = lastScale;
                lastScale = scale;
                if (observation != null && (previousScale == null || previousScale.equals(scale))) {
                    Candidate candidate = observation.captureRigid(level, pos, propagation);
                    if (candidate != null) {
                        rigid = candidate.rigid();
                        dependencies = candidate.dependencies();
                        reference = candidate.reference();
                    }
                }
            }
            else if (observation != null) {
                lastScale = null;
                dependencies = observation.staticDependencies();
                if (dependencies != null) {
                    Vec3 origin = Vec3.atLowerCornerOf(pos);
                    reference = new LaserRenderSnapshot(propagation, origin, origin, new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1));
                }
            }
            if (dependencies == null) {
                return Mode.LOCAL;
            }

            return Mode.PREPARING;
        }

        private @Nullable PreparedDisplay advance(World world, boolean checkDependencies) {
            LaserBeamWork<LaserBeamDisplayInput> work = this.work;
            LaserStaticDependencies dependencies = this.dependencies;
            LaserRenderSnapshot reference = this.reference;
            if (work == null || dependencies == null || reference == null) {
                return null;
            }

            if (checkDependencies && !matches()) {
                clearPreparation();
                this.dependencies = null;
                rigid = null;
                return new PreparedDisplay(Mode.LOCAL, NO_DATA);
            }

            if (!work.step()) {
                world.queue.addLast(this);
                return null;
            }

            LaserBeamDisplayInput input = work.result();
            clearPreparation();
            if (rigid != null && !input.base().isComplete()) {
                return new PreparedDisplay(Mode.LOCAL, NO_DATA);
            }

            BeamState local = Objects.requireNonNull(localSource, "Encoding a laser display requires its local beam source.");
            BeamFrame frame = local.frame();
            LaserDisplaySnapshot snapshot = new LaserDisplaySnapshot(frame.origin(), frame.direction(), local.fluxLm(), range, dependencies, input, Input.from(reference), rigid);
            byte[] data = LaserDisplayCodec.encode(snapshot);
            if (data == null) {
                return new PreparedDisplay(Mode.TOO_LARGE, NO_DATA);
            }

            return new PreparedDisplay(Mode.FULL, data);
        }

        private void stop(World world) {
            retryTick = Long.MIN_VALUE;
            if (initialized && propagation == null && packet.mode() == Mode.OFF) {
                return;
            }

            discard(world);
            initialized = true;
            revision++;
            publish(Mode.OFF, NO_DATA);
        }

        private void fail(World world, RuntimeException failure) {
            boolean firstFailure = retryTick == Long.MIN_VALUE;
            retryTick = level.getGameTime() + DISPLAY_RETRY_DELAY_TICKS;
            discard(world);
            if (!firstFailure) {
                return;
            }

            CCBAPI.LOGGER.warn("Failed to prepare the laser display at {} in dimension '{}'; pausing this display and retrying after {} ticks.", owner.getBlockPos(), level.dimension().location(), DISPLAY_RETRY_DELAY_TICKS, failure);
            initialized = true;
            revision++;
            publish(Mode.OFF, NO_DATA);
        }

        private void discard(World world) {
            world.queue.remove(this);
            propagation = null;
            localSource = null;
            dependencies = null;
            rigid = null;
            clearPreparation();
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
