package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticles.DrawPoint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticles.GlowPoint;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender.LaserBeamImpact.Contact;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamImpactParticles {
    private static final int MAX_PARTICLES = 6;
    private static final int EMISSION_INTERVAL = 4;
    private static final double VIEW_DISTANCE_SQR = 1024;
    private final List<WeakReference<LaserGlowParticle>> particles = new ArrayList<>();
    private long lastEmission = Long.MIN_VALUE;

    void clear() {
        for (WeakReference<LaserGlowParticle> reference : particles) {
            LaserGlowParticle particle = reference.get();
            if (particle == null) {
                continue;
            }

            particle.remove();
        }
        particles.clear();
        lastEmission = Long.MIN_VALUE;
    }

    void update(ClientLevel level, @Nullable PonderLevel ponderLevel, LaserBeamContext context, LaserBeamImpact impact, Vec3 camera, long tick, long seed, int stride) {
        particles.removeIf(reference -> {
            LaserGlowParticle particle = reference.get();
            return particle == null || !particle.isAlive();
        });
        Contact contact = impact.contact();
        if (contact == null || contact.opacity() <= 0 || stride <= 0 || particles.size() >= MAX_PARTICLES) {
            return;
        }

        Vec3 point = context.toWorldPosition(contact.point());
        if (ponderLevel == null && point.distanceToSqr(camera) > VIEW_DISTANCE_SQR) {
            return;
        }
        int interval = EMISSION_INTERVAL * stride;
        if (Math.floorMod(tick, interval) != Math.floorMod(seed ^ seed >>> 32, interval)) {
            return;
        }
        if (lastEmission != Long.MIN_VALUE && tick - lastEmission < interval) {
            return;
        }

        lastEmission = tick;
        RandomSource random = RandomSource.create(seed ^ tick * 0x9E3779B97F4A7C15L);
        Vec3 normal = context.toWorldPosition(contact.point().add(contact.normal())).subtract(point).normalize();
        Vec3 backward = context.toWorldPosition(contact.point().subtract(contact.direction())).subtract(point).normalize();
        Vec3 noise = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5);
        Vec3 tangent = noise.subtract(normal.scale(noise.dot(normal)));
        double size = Math.clamp(contact.width() * 0.15, 0.008, 0.022) * (0.7 + random.nextDouble() * 0.3);
        Vec3 position = point.add(normal.scale(size + 0.004)).add(tangent.scale(size));
        Vec3 velocity = normal.scale(0.008).add(backward.scale(0.008)).add(tangent.scale(0.025));
        if (velocity.dot(normal) < 0) {
            velocity = velocity.subtract(normal.scale(velocity.dot(normal)));
        }

        DrawPoint draw = new DrawPoint(new GlowPoint(position, size, 230), contact.draw().appearance(), Math.min(1, contact.opacity() * 2));
        LaserGlowParticle particle = new LaserGlowParticle(level, draw, velocity, 10 + random.nextInt(7), ponderLevel != null);
        particles.add(new WeakReference<>(particle));
        if (ponderLevel != null) {
            ponderLevel.addParticle(particle);
            return;
        }

        Minecraft.getInstance().particleEngine.add(particle);
    }
}
