package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent.Pre;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamAppearance;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamBrightness;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticles.DrawPoint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticles.FlowPoint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticles.GlowPoint;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
final class LaserGlowParticle extends Particle {
    private static final int MIN_LIFETIME = 32;
    private static final int LIFETIME_VARIATION = 17;
    private static final float FADE_IN_TICKS = 8;
    private static final int POSITION_TRANSITION_TICKS = 8;
    private static final double DRIFT_SPEED = 0.0025;
    private static final double DRIFT_VARIATION = 0.0005;
    private static final ParticleRenderType WORLD_RENDER_TYPE = new GlowRenderType(false);
    private static final ParticleRenderType PONDER_RENDER_TYPE = new GlowRenderType(true);
    private static long frame;
    private final ParticleRenderType renderType;
    private final Vec3 driftOffset;
    private LaserBeamAppearance appearance;
    private double opacity;
    private double halfSize;
    private int initialAlpha;
    private Vec3 velocity;
    private Vec3 positionOffset = Vec3.ZERO;
    private long transitionTicks;
    private long lastSeenFrame;
    private boolean following = true;

    LaserGlowParticle(ClientLevel level, FlowPoint flow, long seed, boolean ponder) {
        super(level, flow.point().geometry().position().x, flow.point().geometry().position().y, flow.point().geometry().position().z);
        DrawPoint point = flow.point();
        GlowPoint geometry = point.geometry();
        appearance = point.appearance();
        opacity = point.opacity();
        halfSize = geometry.halfSize();
        initialAlpha = geometry.alpha();
        velocity = flow.direction().scale(DRIFT_SPEED);
        lastSeenFrame = frame;
        renderType = ponder ? PONDER_RENDER_TYPE : WORLD_RENDER_TYPE;
        RandomSource random = RandomSource.create(seed ^ flow.identity() * 0x9E3779B97F4A7C15L);
        lifetime = MIN_LIFETIME + random.nextInt(LIFETIME_VARIATION);
        driftOffset = new Vec3((random.nextDouble() * 2 - 1) * DRIFT_VARIATION, (random.nextDouble() * 2 - 1) * DRIFT_VARIATION, (random.nextDouble() * 2 - 1) * DRIFT_VARIATION);
        friction = 0.98F;
        hasPhysics = false;
    }

    @SubscribeEvent
    private static void onFrame(Pre event) {
        frame++;
    }

    private static void renderQuad(VertexConsumer buffer, Matrix4f pose, DrawPoint draw, Vec3 origin, Vec3 right, Vec3 up) {
        GlowPoint point = draw.geometry();
        LaserBeamAppearance appearance = draw.appearance();
        Vec3 center = point.position().subtract(origin);
        double halfSize = point.halfSize();
        Vec3 horizontal = right.scale(halfSize);
        Vec3 vertical = up.scale(halfSize);
        Vec3 leftCenter = center.subtract(horizontal);
        Vec3 rightCenter = center.add(horizontal);
        Vec3 firstCorner = leftCenter.subtract(vertical);
        Vec3 secondCorner = rightCenter.subtract(vertical);
        Vec3 thirdCorner = rightCenter.add(vertical);
        Vec3 fourthCorner = leftCenter.add(vertical);
        int red = (int) Math.round(255 * appearance.red());
        int green = (int) Math.round(254 * appearance.green());
        int blue = (int) Math.round(244 * appearance.blue());
        int alpha = (int) Math.round(point.alpha() * draw.opacity());
        float width = (float) (2 * LaserBeamBrightness.REFERENCE_RADIUS);
        buffer.addVertex(pose, (float) firstCorner.x, (float) firstCorner.y, (float) firstCorner.z).setColor(red, green, blue, alpha).setUv(width, 1);
        buffer.addVertex(pose, (float) secondCorner.x, (float) secondCorner.y, (float) secondCorner.z).setColor(red, green, blue, alpha).setUv(width, 1);
        buffer.addVertex(pose, (float) thirdCorner.x, (float) thirdCorner.y, (float) thirdCorner.z).setColor(red, green, blue, alpha).setUv(width, 1);
        buffer.addVertex(pose, (float) fourthCorner.x, (float) fourthCorner.y, (float) fourthCorner.z).setColor(red, green, blue, alpha).setUv(width, 1);
    }

    @Override
    public void tick() {
        if (following) {
            if (frame - lastSeenFrame <= 1) {
                xo = x;
                yo = y;
                zo = z;
                age++;
                return;
            }

            detach(1);
        }

        super.tick();
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float progress = Math.clamp(following ? (age + partialTicks) / FADE_IN_TICKS : 1 - (age + partialTicks) / lifetime, 0, 1);
        double fade = progress * progress * (3 - 2 * progress);
        int alpha = (int) Math.round(initialAlpha * fade);
        if (alpha <= 0) {
            return;
        }

        Vec3 position = new Vec3(Mth.lerp(partialTicks, xo, x), Mth.lerp(partialTicks, yo, y), Mth.lerp(partialTicks, zo, z));
        Vector3f cameraLeft = camera.getLeftVector();
        Vector3f cameraUp = camera.getUpVector();
        Vec3 right = new Vec3(-cameraLeft.x, -cameraLeft.y, -cameraLeft.z);
        Vec3 up = new Vec3(cameraUp.x, cameraUp.y, cameraUp.z);
        DrawPoint point = new DrawPoint(new GlowPoint(position, halfSize, alpha), appearance, opacity);
        renderQuad(buffer, new Matrix4f(), point, camera.getPosition(), right, up);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return renderType;
    }

    boolean isFollowing() {
        return following && isAlive();
    }

    void touch() {
        lastSeenFrame = frame;
    }

    void follow(FlowPoint flow, Vec3 adjustment, long elapsedTicks) {
        DrawPoint point = flow.point();
        GlowPoint geometry = point.geometry();
        if (transitionTicks > 0) {
            long remaining = Math.max(0, transitionTicks - elapsedTicks);
            positionOffset = positionOffset.scale((double) remaining / transitionTicks);
            transitionTicks = remaining;
        }
        if (adjustment.lengthSqr() > 0) {
            positionOffset = positionOffset.add(adjustment);
            transitionTicks = POSITION_TRANSITION_TICKS;
        }

        Vec3 position = geometry.position().add(positionOffset);
        velocity = position.subtract(getPos()).scale(1.0 / elapsedTicks);
        setPos(position.x, position.y, position.z);
        appearance = point.appearance();
        opacity = point.opacity();
        halfSize = geometry.halfSize();
        initialAlpha = geometry.alpha();
        touch();
    }

    void detach(float partialTicks) {
        if (!following) {
            return;
        }

        float progress = Math.clamp((age + partialTicks) / FADE_IN_TICKS, 0, 1);
        initialAlpha = Math.round(initialAlpha * progress * progress * (3 - 2 * progress));
        setPos(Mth.lerp(partialTicks, xo, x), Mth.lerp(partialTicks, yo, y), Mth.lerp(partialTicks, zo, z));
        xo = x;
        yo = y;
        zo = z;
        xd = velocity.x + driftOffset.x;
        yd = velocity.y + driftOffset.y;
        zd = velocity.z + driftOffset.z;
        age = 0;
        following = false;
    }

    private record GlowRenderType(boolean ponder) implements ParticleRenderType {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(() -> LaserRenderTypes.prepareLaserShader(ponder));
            return tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        }
    }
}
