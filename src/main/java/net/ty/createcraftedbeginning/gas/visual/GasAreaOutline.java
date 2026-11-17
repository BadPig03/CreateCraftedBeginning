package net.ty.createcraftedbeginning.gas.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.outliner.Outliner.OutlineEntry;
import net.createmod.catnip.render.PonderRenderTypes;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.createmod.ponder.enums.PonderSpecialTextures;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.function.BooleanSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
public final class GasAreaOutline extends AABBOutline {
    private static final float LINE_WIDTH = 0.03125F;
    private static final float MAX_OPACITY = 0.55F;
    private static final float REFERENCE_FLOW_GU_PER_TICK = 100;
    private static final int FLOW_HOLD_TICKS = 2;
    private static final int FADE_IN_TICKS = 3;
    private static final int FADE_OUT_TICKS = 8;

    private final Slot slot;
    private final Level level;
    private final Vec3 sourceCenter;
    private final boolean anchored;
    private BooleanSupplier visible;
    private float previousStrength;
    private float strength;
    private float targetStrength;
    private int ticksWithoutRefresh;

    private GasAreaOutline(Slot slot, AABB localBounds, BooleanSupplier visible) {
        super(localBounds);
        this.slot = slot;
        level = slot.level();
        this.visible = visible;
        BlockPos sourcePos = slot.sourcePos();
        sourceCenter = sourcePos.getCenter();
        anchored = SubLevelBridge.resolve(level, sourcePos).inSubLevel();
        params.lineWidth(LINE_WIDTH);
    }

    public static void show(Level level, BlockPos sourcePos, Optional<Direction> direction, AABB localBounds, int color, long amount, boolean continuous, BooleanSupplier visible) {
        Slot slot = new Slot(level, sourcePos.immutable(), direction);
        Outliner outliner = Outliner.getInstance();
        OutlineEntry entry = outliner.getOutlines().get(slot);
        GasAreaOutline outline;
        if (entry != null && entry.getOutline() instanceof GasAreaOutline existing) {
            outline = existing;
        }
        else {
            outline = new GasAreaOutline(slot, localBounds, visible);
            outliner.showOutline(slot, outline);
        }

        outline.refresh(localBounds, color, amount, continuous, visible);
        outliner.keep(slot);
    }

    @SubscribeEvent
    public static void onLevelUnload(Unload event) {
        if (!(event.getLevel() instanceof ClientLevel level)) {
            return;
        }

        Outliner outliner = Outliner.getInstance();
        for (Object key : outliner.getOutlines().keySet().stream().filter(key -> key instanceof Slot slot && slot.level() == level).toList()) {
            outliner.remove(key);
        }
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level != level || player == null || !GogglesItem.isWearingGoggles(player) || !visible.getAsBoolean()) {
            previousStrength = strength = targetStrength = 0;
            return;
        }

        previousStrength = strength;
        if (++ticksWithoutRefresh > FLOW_HOLD_TICKS) {
            targetStrength = 0;
        }

        float step = targetStrength > strength ? 1.0F / FADE_IN_TICKS : 1.0F / FADE_OUT_TICKS;
        strength = Mth.approach(strength, targetStrength, step);
        if (strength == 0 && previousStrength == 0) {
            return;
        }

        Outliner.getInstance().keep(slot);
    }

    @Override
    public void render(PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 camera, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level != level || player == null || !GogglesItem.isWearingGoggles(player) || !visible.getAsBoolean()) {
            return;
        }

        float opacity = MAX_OPACITY * Mth.lerp(partialTicks, previousStrength, strength);
        if (opacity <= 0) {
            return;
        }

        CoordinateTransform transform = SubLevelBridge.createRenderTransform(level, sourceCenter, partialTicks);
        if (anchored && !transform.inSubLevel()) {
            return;
        }

        Vec3 x = transform.transformNormal(new Vec3(1, 0, 0));
        Vec3 y = transform.transformNormal(new Vec3(0, 1, 0));
        Vec3 z = transform.transformNormal(new Vec3(0, 0, 1));
        Matrix4f basis = new Matrix4f((float) x.x, (float) x.y, (float) x.z, 0, (float) y.x, (float) y.y, (float) y.z, 0, (float) z.x, (float) z.y, (float) z.z, 0, 0, 0, 0, 1);
        Vec3 localCamera = transform.inverseTransformPosition(camera).subtract(sourceCenter);
        poseStack.pushPose();
        try {
            poseStack.mulPose(basis);
            params.loadColor(colorTemp);
            colorTemp.w *= opacity;
            AABB bounds = getBounds();
            double inflate = bounds.contains(localCamera) ? -0.0078125 : 0.0078125;
            AABB relativeBounds = bounds.move(localCamera.scale(-1));
            minPosTemp1.set((float) (relativeBounds.minX - inflate), (float) (relativeBounds.minY - inflate), (float) (relativeBounds.minZ - inflate));
            maxPosTemp1.set((float) (relativeBounds.maxX + inflate), (float) (relativeBounds.maxY + inflate), (float) (relativeBounds.maxZ + inflate));
            VertexConsumer consumer = buffer.getLateBuffer(PonderRenderTypes.outlineTranslucent(PonderSpecialTextures.BLANK.getLocation(), true));
            renderBoxEdges(poseStack, consumer, minPosTemp1, maxPosTemp1, LINE_WIDTH, colorTemp, LightTexture.FULL_BRIGHT, false);
        }
        finally {
            poseStack.popPose();
        }
    }

    private void refresh(AABB localBounds, int color, long amount, boolean continuous, BooleanSupplier visible) {
        setBounds(localBounds);
        params.colored(color);
        this.visible = visible;
        ticksWithoutRefresh = 0;
        targetStrength = (float) (amount / (amount + (double) REFERENCE_FLOW_GU_PER_TICK));
        if (continuous) {
            return;
        }

        previousStrength = strength = targetStrength = 1;
    }

    private record Slot(Level level, BlockPos sourcePos, Optional<Direction> direction) {}
}
