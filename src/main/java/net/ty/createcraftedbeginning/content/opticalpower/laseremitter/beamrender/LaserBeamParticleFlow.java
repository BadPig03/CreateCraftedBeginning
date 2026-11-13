package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticles.DrawPoint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticles.FlowPoint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticles.GlowPoint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticles.ShellSampler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamParticleFlow {
    private static final double CONTINUITY_TOLERANCE_SQR = 1.0E-12;
    private static final double MAX_STEP_PER_TICK = 0.25;
    private Map<Long, List<WeakReference<LaserGlowParticle>>> active = new HashMap<>();
    private Map<Long, List<WeakReference<LaserGlowParticle>>> pending = new HashMap<>();
    private @Nullable ShellSampler sampler;
    private @Nullable LaserBeamContext context;
    private long lastTick;
    private long seed;
    private int stride;

    private static List<FlowPoint> toWorld(List<FlowPoint> points, LaserBeamContext context) {
        List<FlowPoint> result = new ArrayList<>();
        for (FlowPoint flow : points) {
            DrawPoint point = flow.point();
            GlowPoint geometry = point.geometry();
            Vec3 local = geometry.position();
            Vec3 position = context.toWorldPosition(local);
            Vec3 direction = context.toWorldPosition(local.add(flow.direction())).subtract(position).normalize();
            result.add(new FlowPoint(flow.identity(), new DrawPoint(new GlowPoint(position, geometry.halfSize(), geometry.alpha()), point.appearance(), point.opacity()), context.toWorldPosition(flow.anchor()), direction));
        }
        return result;
    }

    private static void removeParticles(Map<Long, List<WeakReference<LaserGlowParticle>>> particles) {
        for (List<WeakReference<LaserGlowParticle>> group : particles.values()) {
            for (WeakReference<LaserGlowParticle> reference : group) {
                LaserGlowParticle particle = reference.get();
                if (particle == null) {
                    continue;
                }

                particle.remove();
            }
        }

        particles.clear();
    }

    void clear() {
        removeParticles(active);
        removeParticles(pending);
        sampler = null;
        context = null;
    }

    void touch() {
        for (List<WeakReference<LaserGlowParticle>> group : active.values()) {
            for (WeakReference<LaserGlowParticle> reference : group) {
                LaserGlowParticle particle = reference.get();
                if (particle == null || !particle.isFollowing()) {
                    continue;
                }

                particle.touch();
            }
        }
    }

    void detach(float partialTick) {
        for (List<WeakReference<LaserGlowParticle>> group : active.values()) {
            for (WeakReference<LaserGlowParticle> reference : group) {
                LaserGlowParticle particle = reference.get();
                if (particle == null) {
                    continue;
                }

                particle.detach(partialTick);
            }
        }

        active.clear();
        sampler = null;
        context = null;
    }

    void update(ClientLevel level, @Nullable PonderLevel ponderLevel, ShellSampler sampler, LaserBeamContext context, List<FlowPoint> points, long tick, long seed, int stride, float partialTick) {
        if (ponderLevel != null && tick < lastTick) {
            clear();
        }

        List<FlowPoint> current = toWorld(points, context);
        List<FlowPoint> predicted = current;
        ShellSampler previousSampler = this.sampler;
        LaserBeamContext previousContext = this.context;
        if (previousSampler != null && previousContext != null && (sampler != previousSampler || !context.equals(previousContext))) {
            predicted = toWorld(previousSampler.sampleFlow(tick, this.seed, this.stride), previousContext);
        }

        Map<Long, List<LaserGlowParticle>> available = new HashMap<>();
        active.forEach((identity, references) -> {
            List<LaserGlowParticle> particles = new ArrayList<>();
            for (WeakReference<LaserGlowParticle> reference : references) {
                LaserGlowParticle particle = reference.get();
                if (particle == null || !particle.isFollowing()) {
                    continue;
                }

                particles.add(particle);
            }
            available.put(identity, particles);
        });
        long elapsedTicks = Math.max(1, tick - lastTick);
        double maxStep = MAX_STEP_PER_TICK * elapsedTicks;
        Map<Long, List<Continuation>> continuations = new HashMap<>();
        for (FlowPoint prediction : predicted) {
            List<LaserGlowParticle> group = available.get(prediction.identity());
            if (group == null) {
                continue;
            }

            LaserGlowParticle closest = null;
            double closestDistance = maxStep * maxStep;
            Vec3 position = prediction.point().geometry().position();
            for (LaserGlowParticle particle : group) {
                double distance = particle.getPos().distanceToSqr(position);
                if (distance > closestDistance) {
                    continue;
                }

                closest = particle;
                closestDistance = distance;
            }
            if (closest == null) {
                continue;
            }

            group.remove(closest);
            continuations.computeIfAbsent(prediction.identity(), ignored -> new ArrayList<>()).add(new Continuation(prediction, closest));
        }

        for (FlowPoint flow : current) {
            List<Continuation> group = continuations.get(flow.identity());
            Continuation matched = null;
            if (group != null) {
                for (Continuation continuation : group) {
                    FlowPoint prediction = continuation.prediction();
                    if (prediction.anchor().distanceToSqr(flow.anchor()) > CONTINUITY_TOLERANCE_SQR || prediction.direction().distanceToSqr(flow.direction()) > CONTINUITY_TOLERANCE_SQR) {
                        continue;
                    }

                    matched = continuation;
                    break;
                }
            }

            if (matched == null) {
                LaserGlowParticle particle = new LaserGlowParticle(level, flow, seed, ponderLevel != null);
                pending.computeIfAbsent(flow.identity(), ignored -> new ArrayList<>()).add(new WeakReference<>(particle));
                if (ponderLevel != null) {
                    ponderLevel.addParticle(particle);
                }
                else {
                    Minecraft.getInstance().particleEngine.add(particle);
                }

                continue;
            }

            group.remove(matched);
            LaserGlowParticle particle = matched.particle();
            pending.computeIfAbsent(flow.identity(), ignored -> new ArrayList<>()).add(new WeakReference<>(particle));
            Vec3 adjustment = matched.prediction().point().geometry().position().subtract(flow.point().geometry().position());
            if (adjustment.lengthSqr() <= CONTINUITY_TOLERANCE_SQR) {
                adjustment = Vec3.ZERO;
            }

            particle.follow(flow, adjustment, elapsedTicks);
        }

        for (List<LaserGlowParticle> group : available.values()) {
            for (LaserGlowParticle particle : group) {
                particle.detach(partialTick);
            }
        }
        for (List<Continuation> group : continuations.values()) {
            for (Continuation continuation : group) {
                continuation.particle().detach(partialTick);
            }
        }

        Map<Long, List<WeakReference<LaserGlowParticle>>> previous = active;
        active = pending;
        pending = previous;
        pending.clear();
        this.sampler = sampler;
        this.context = context;
        this.seed = seed;
        this.stride = stride;
        lastTick = tick;
    }

    private record Continuation(FlowPoint prediction, LaserGlowParticle particle) {}
}
