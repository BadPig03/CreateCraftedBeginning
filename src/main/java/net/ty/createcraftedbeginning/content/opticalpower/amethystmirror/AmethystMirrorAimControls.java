package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorAimControls {
    private static final int PREVIEW_INTERVAL = 5;
    private static final int REVALIDATION_INTERVAL = 20;
    private static final double PREVIEW_MOVEMENT_SQR = 0.00390625;
    private static final int RESPONSE_TIMEOUT = 60;
    private static final int INSTRUCTION_DURATION_TICKS = 40;

    private static @Nullable AmethystMirrorBlockEntity selectedMirror;
    private static @Nullable BlockHitResult target;
    private static @Nullable BlockHitResult requestedTarget;
    private static AmethystMirrorAimStatus status = AmethystMirrorAimStatus.NO_TARGET;
    private static @Nullable AmethystMirrorAimResult preview;
    private static int requestId;
    private static int cooldown;
    private static int refreshTicks;
    private static int waitingTicks;
    private static int instructionTicks;
    private static boolean waiting;
    private static boolean applying;

    private AmethystMirrorAimControls() {
    }

    static boolean isSelecting() {
        return selectedMirror != null;
    }

    static boolean click(Minecraft minecraft, LocalPlayer player) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            return false;
        }

        boolean sneaking = player.isShiftKeyDown();
        AmethystMirrorBlockEntity mirror = selectedMirror;
        if (mirror == null) {
            if (sneaking || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != Type.BLOCK) {
                return false;
            }

            BlockPos pos = hit.getBlockPos();
            if (!player.canInteractWithBlock(pos, 1) || !(level.getBlockEntity(pos) instanceof AmethystMirrorBlockEntity found)) {
                return false;
            }

            AmethystMirrorControls.beginAim(found);
            selectedMirror = found;
            instructionTicks = INSTRUCTION_DURATION_TICKS;
            updateTarget(level, player);
            return true;
        }

        if (sneaking) {
            clear();
            player.displayClientMessage(AmethystMirrorDisplay.getAimStatus(AmethystMirrorAimStatus.CANCELLED, false), true);
            return true;
        }

        if (applying) {
            return true;
        }

        updateTarget(level, player);
        if (selectedMirror == null || AmethystMirrorTargeting.getStatus(mirror.getBlockPos(), target) != AmethystMirrorAimStatus.READY) {
            return true;
        }

        request(level, true);
        player.displayClientMessage(AmethystMirrorDisplay.getAimStatus(status, instructionTicks > 0), true);
        return true;
    }

    static void tick(ClientLevel level, LocalPlayer player) {
        refreshTicks++;
        if (instructionTicks > 0) {
            instructionTicks--;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if (waiting && ++waitingTicks >= RESPONSE_TIMEOUT) {
            waiting = false;
            applying = false;
            requestId++;
            preview = null;
            status = AmethystMirrorAimStatus.CHECKING;
        }
        updateTarget(level, player);
        AmethystMirrorBlockEntity mirror = selectedMirror;
        if (mirror == null) {
            return;
        }

        if (!applying && !waiting && cooldown == 0 && (preview == null || refreshTicks >= REVALIDATION_INTERVAL) && AmethystMirrorTargeting.getStatus(mirror.getBlockPos(), target) == AmethystMirrorAimStatus.READY) {
            request(level, false);
        }
        AmethystMirrorAimOutlineRenderer.tick(preview);
    }

    static void accept(AmethystMirrorAimReplyPacket reply, LocalPlayer player) {
        AmethystMirrorBlockEntity mirror = selectedMirror;
        Level level = player.level();
        if (mirror == null || mirror.isRemoved() || mirror.getLevel() != level || !level.dimension().location().equals(reply.dimension()) || !mirror.getBlockPos().equals(reply.pos()) || requestId != reply.requestId() || !waiting) {
            return;
        }

        waiting = false;
        applying = false;
        preview = reply.result();
        status = preview.status();
        Quaternionf rotation = preview.rotation();
        if (reply.applied() && rotation != null) {
            clear();
            AmethystMirrorControls.previewAim(mirror, rotation);
            player.displayClientMessage(AmethystMirrorDisplay.getAimStatus(AmethystMirrorAimStatus.SUBMITTED, false), true);
            return;
        }

        player.displayClientMessage(AmethystMirrorDisplay.getAimStatus(status, instructionTicks > 0), true);
    }

    static void render() {
        AmethystMirrorBlockEntity mirror = selectedMirror;
        if (mirror == null) {
            return;
        }

        AmethystMirrorAimOutlineRenderer.render(mirror.getBlockPos(), target, status == AmethystMirrorAimStatus.READY);
    }

    static void clear() {
        selectedMirror = null;
        target = null;
        requestedTarget = null;
        status = AmethystMirrorAimStatus.NO_TARGET;
        preview = null;
        requestId++;
        cooldown = 0;
        refreshTicks = 0;
        waitingTicks = 0;
        instructionTicks = 0;
        waiting = false;
        applying = false;
        AmethystMirrorAimOutlineRenderer.clear();
    }

    private static void updateTarget(ClientLevel level, LocalPlayer player) {
        AmethystMirrorBlockEntity mirror = selectedMirror;
        if (mirror == null) {
            return;
        }

        BlockPos pos = mirror.getBlockPos();
        if (mirror.isRemoved() || mirror.getLevel() != level || !AmethystMirrorTargeting.isWithinAimRange(player, pos)) {
            clear();
            return;
        }

        if (applying) {
            player.displayClientMessage(AmethystMirrorDisplay.getAimStatus(status, instructionTicks > 0), true);
            return;
        }

        BlockHitResult next = AmethystMirrorTargeting.findTarget(level, player, pos);
        updatePreviewTarget(pos, next);
        player.displayClientMessage(AmethystMirrorDisplay.getAimStatus(status, instructionTicks > 0), true);
    }

    private static void updatePreviewTarget(BlockPos pos, @Nullable BlockHitResult next) {
        AmethystMirrorAimStatus basicStatus = AmethystMirrorTargeting.getStatus(pos, next);
        BlockHitResult previous = requestedTarget;
        boolean sameTarget = next != null && previous != null && next.getType() == previous.getType() && next.getBlockPos().equals(previous.getBlockPos()) && next.getDirection() == previous.getDirection() && next.isInside() == previous.isInside() && next.getLocation().distanceToSqr(previous.getLocation()) <= PREVIEW_MOVEMENT_SQR;
        target = next;
        if (sameTarget && basicStatus == AmethystMirrorAimStatus.READY) {
            return;
        }

        preview = null;
        waiting = false;
        requestId++;
        status = basicStatus;
        if (basicStatus != AmethystMirrorAimStatus.READY) {
            requestedTarget = null;
            return;
        }

        status = AmethystMirrorAimStatus.CHECKING;
    }

    private static void request(ClientLevel level, boolean apply) {
        AmethystMirrorAimPacket packet = prepareRequest(level.dimension().location(), apply);
        if (packet == null) {
            return;
        }

        CatnipServices.NETWORK.sendToServer(packet);
    }

    private static @Nullable AmethystMirrorAimPacket prepareRequest(ResourceLocation dimension, boolean apply) {
        AmethystMirrorBlockEntity mirror = selectedMirror;
        BlockHitResult hit = target;
        if (mirror == null || hit == null) {
            return null;
        }

        BlockPos pos = mirror.getBlockPos();
        if (AmethystMirrorTargeting.getStatus(pos, hit) != AmethystMirrorAimStatus.READY) {
            return null;
        }

        Vec3 point = hit.getLocation();
        waiting = true;
        applying = apply;
        if (apply) {
            instructionTicks = 0;
        }
        waitingTicks = 0;
        refreshTicks = 0;
        requestedTarget = hit;
        cooldown = PREVIEW_INTERVAL;
        requestId++;
        if (apply || preview == null) {
            status = AmethystMirrorAimStatus.CHECKING;
        }
        return new AmethystMirrorAimPacket(dimension, pos, hit.getBlockPos(), point.x, point.y, point.z, requestId, apply);
    }
}
