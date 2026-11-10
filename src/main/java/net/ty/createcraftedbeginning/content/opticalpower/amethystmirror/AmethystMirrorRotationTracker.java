package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorRotationTracker {
    private static final int REPLY_TIMEOUT_TICKS = 60;
    private static final List<Submission> SUBMISSIONS = new ArrayList<>();
    private static int nextRequestId;

    private AmethystMirrorRotationTracker() {
    }

    static AmethystMirrorRotatePacket submit(AmethystMirrorBlockEntity mirror, ResourceLocation dimension, boolean horizontalAxis, int steps) {
        AmethystMirrorRotatePacket packet = new AmethystMirrorRotatePacket(dimension, mirror.getBlockPos(), horizontalAxis, steps, ++nextRequestId);
        SUBMISSIONS.add(new Submission(mirror, packet));
        return packet;
    }

    static boolean hasPending(AmethystMirrorBlockEntity mirror) {
        for (Submission submission : SUBMISSIONS) {
            if (submission.mirror == mirror) {
                return true;
            }
        }
        return false;
    }

    static void accept(AmethystMirrorRotateReplyPacket reply, LocalPlayer player) {
        ClientLevel level = player.clientLevel;
        ResourceLocation dimension = reply.dimension();
        BlockPos pos = reply.pos();
        int requestId = reply.requestId();
        if (!level.dimension().location().equals(dimension)) {
            return;
        }

        for (Submission submission : SUBMISSIONS) {
            AmethystMirrorRotatePacket packet = submission.packet;
            if (packet.requestId() != requestId || !packet.pos().equals(pos) || !packet.dimension().equals(dimension)) {
                continue;
            }

            AmethystMirrorBlockEntity mirror = submission.mirror;
            if (mirror.isRemoved() || mirror.getLevel() != level || !level.isLoaded(pos) || level.getBlockEntity(pos) != mirror) {
                forget(mirror);
                AmethystMirrorControls.cancelRotation(mirror);
                return;
            }

            reconcile(reply, mirror);
            return;
        }
    }

    static void tick(ClientLevel level, LocalPlayer player) {
        if (SUBMISSIONS.isEmpty()) {
            return;
        }

        List<AmethystMirrorBlockEntity> expired = new ArrayList<>();
        for (Submission submission : SUBMISSIONS) {
            AmethystMirrorBlockEntity mirror = submission.mirror;
            if (!mirror.isRemoved() && mirror.getLevel() == level && AmethystMirrorControlPose.canInteract(player, mirror.getBlockPos()) && ++submission.waitTicks <= REPLY_TIMEOUT_TICKS) {
                continue;
            }

            expired.add(mirror);
        }
        for (AmethystMirrorBlockEntity mirror : expired) {
            forget(mirror);
            AmethystMirrorControls.cancelRotation(mirror);
        }
    }

    static void forget(AmethystMirrorBlockEntity mirror) {
        SUBMISSIONS.removeIf(submission -> submission.mirror == mirror);
    }

    static void clear() {
        SUBMISSIONS.clear();
    }

    private static void reconcile(AmethystMirrorRotateReplyPacket reply, AmethystMirrorBlockEntity mirror) {
        int matched = -1;
        for (int index = 0; index < SUBMISSIONS.size(); index++) {
            Submission submission = SUBMISSIONS.get(index);
            AmethystMirrorRotatePacket packet = submission.packet;
            if (submission.mirror != mirror || packet.requestId() != reply.requestId() || !packet.dimension().equals(reply.dimension()) || !packet.pos().equals(reply.pos())) {
                continue;
            }

            matched = index;
            break;
        }
        if (matched < 0) {
            return;
        }

        Quaternionf predicted = reply.rotation();
        if (predicted == null) {
            forget(mirror);
            AmethystMirrorControls.cancelRotation(mirror);
            return;
        }

        for (int index = matched; index >= 0; index--) {
            if (SUBMISSIONS.get(index).mirror != mirror) {
                continue;
            }

            SUBMISSIONS.remove(index);
        }
        for (Submission submission : SUBMISSIONS) {
            if (submission.mirror != mirror) {
                continue;
            }

            AmethystMirrorRotatePacket packet = submission.packet;
            AmethystMirrorControls.rotatePreview(predicted, packet.horizontalAxis(), packet.rotationSteps());
        }
        AmethystMirrorControls.reconcileRotation(mirror, predicted);
    }

    private static final class Submission {
        private final AmethystMirrorBlockEntity mirror;
        private final AmethystMirrorRotatePacket packet;
        private int waitTicks;

        private Submission(AmethystMirrorBlockEntity mirror, AmethystMirrorRotatePacket packet) {
            this.mirror = mirror;
            this.packet = packet;
        }
    }
}
