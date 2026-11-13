package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags.Items;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.joml.Quaternionf;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AmethystMirrorAimPacket(ResourceLocation dimension, BlockPos pos, BlockPos targetPos, double x, double y, double z, int requestId, boolean apply) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AmethystMirrorAimPacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeBlockPos(packet.pos);
        buffer.writeBlockPos(packet.targetPos);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeVarInt(packet.requestId);
        buffer.writeBoolean(packet.apply);
    }, buffer -> new AmethystMirrorAimPacket(buffer.readResourceLocation(), buffer.readBlockPos(), buffer.readBlockPos(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt(), buffer.readBoolean()));

    public AmethystMirrorAimPacket {
        pos = pos.immutable();
        targetPos = targetPos.immutable();
    }

    @Override
    public void handle(ServerPlayer player) {
        CatnipServices.NETWORK.sendToClient(player, applyAim(player));
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AMETHYST_MIRROR_AIM;
    }

    AmethystMirrorAimReplyPacket applyAim(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.dimension().location().equals(dimension) || player.isSpectator() || !player.mayBuild() || !level.isLoaded(pos) || !AmethystMirrorTargeting.isWithinAimRange(player, pos) || !level.mayInteract(player, pos)) {
            return reply(AmethystMirrorAimResult.failure(AmethystMirrorAimStatus.INVALID), false);
        }

        if (!player.getMainHandItem().is(Items.TOOLS_WRENCH) && !player.getOffhandItem().is(Items.TOOLS_WRENCH)) {
            return reply(AmethystMirrorAimResult.failure(AmethystMirrorAimStatus.INVALID), false);
        }

        if (!(level.getBlockEntity(pos) instanceof AmethystMirrorBlockEntity mirror)) {
            return reply(AmethystMirrorAimResult.failure(AmethystMirrorAimStatus.INVALID), false);
        }

        BlockHitResult source = AmethystMirrorTargeting.clipLoaded(level, player, player.getEyePosition(), Vec3.atCenterOf(pos));
        if (source == null || source.getType() == HitResult.Type.BLOCK && !source.getBlockPos().equals(pos)) {
            return reply(AmethystMirrorAimResult.failure(AmethystMirrorAimStatus.INVALID), false);
        }

        Vec3 target = new Vec3(x, y, z);
        if (!AmethystMirrorTargeting.isValidTarget(level, player, pos, targetPos, target)) {
            return reply(AmethystMirrorAimResult.failure(AmethystMirrorAimStatus.INVALID), false);
        }

        AmethystMirrorAimResult result;
        try {
            result = AmethystMirrorReflectionPlanner.plan(level, mirror, targetPos, target);
        }
        catch (RuntimeException failure) {
            CCBAPI.LOGGER.warn("Failed to plan reflection aiming for mirror at {} in dimension '{}' toward {}; rejecting request {}.", pos, dimension, targetPos, requestId, failure);
            return reply(AmethystMirrorAimResult.failure(AmethystMirrorAimStatus.INVALID), false);
        }

        Quaternionf rotation = result.rotation();
        AmethystMirrorAimStatus status = result.status();
        boolean applied = apply && status == AmethystMirrorAimStatus.READY && rotation != null;
        if (applied) {
            mirror.applyAim(rotation);
            result = new AmethystMirrorAimResult(status, mirror.getStoredRotation(), result.incomingStart(), result.reflectionPoint(), result.targetPoint());
        }
        return reply(result, applied);
    }

    private AmethystMirrorAimReplyPacket reply(AmethystMirrorAimResult result, boolean applied) {
        return new AmethystMirrorAimReplyPacket(dimension, pos, requestId, applied, result);
    }
}
