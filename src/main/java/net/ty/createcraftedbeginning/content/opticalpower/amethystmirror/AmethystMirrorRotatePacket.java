package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.Tags.Items;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AmethystMirrorRotatePacket(ResourceLocation dimension, BlockPos pos, boolean horizontalAxis, int rotationSteps, int requestId) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AmethystMirrorRotatePacket> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC, AmethystMirrorRotatePacket::dimension, BlockPos.STREAM_CODEC, AmethystMirrorRotatePacket::pos, ByteBufCodecs.BOOL, AmethystMirrorRotatePacket::horizontalAxis, ByteBufCodecs.VAR_INT, AmethystMirrorRotatePacket::rotationSteps, ByteBufCodecs.VAR_INT, AmethystMirrorRotatePacket::requestId, AmethystMirrorRotatePacket::new);

    static final int STEPS_PER_DEGREE = 10;
    static final int FULL_TURN_STEPS = 360 * STEPS_PER_DEGREE;
    private static final int MAX_ROTATION_STEPS = FULL_TURN_STEPS / 2;

    public AmethystMirrorRotatePacket {
        pos = pos.immutable();
    }

    @Override
    public void handle(ServerPlayer player) {
        CatnipServices.NETWORK.sendToClient(player, applyRotation(player));
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AMETHYST_MIRROR_ROTATE;
    }

    AmethystMirrorRotateReplyPacket applyRotation(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.dimension().location().equals(dimension) || !level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof AmethystMirrorBlockEntity mirror)) {
            return new AmethystMirrorRotateReplyPacket(dimension, pos, requestId, false, null);
        }

        if (rotationSteps == 0 || rotationSteps < -MAX_ROTATION_STEPS || rotationSteps > MAX_ROTATION_STEPS || player.isSpectator() || !player.mayBuild() || !AmethystMirrorControlPose.canInteract(player, pos) || !level.mayInteract(player, pos)) {
            return new AmethystMirrorRotateReplyPacket(dimension, pos, requestId, false, mirror.getStoredRotation());
        }

        if (!player.getMainHandItem().is(Items.TOOLS_WRENCH) && !player.getOffhandItem().is(Items.TOOLS_WRENCH)) {
            return new AmethystMirrorRotateReplyPacket(dimension, pos, requestId, false, mirror.getStoredRotation());
        }

        BlockHitResult obstruction = AmethystMirrorRotationRay.clip(level, player, SubLevelBridge.resolve(level, pos).worldPosition());
        if (obstruction == null || obstruction.getType() != HitResult.Type.MISS && !obstruction.getBlockPos().equals(pos)) {
            return new AmethystMirrorRotateReplyPacket(dimension, pos, requestId, false, mirror.getStoredRotation());
        }

        mirror.rotateLocal(horizontalAxis, rotationSteps / (double) STEPS_PER_DEGREE);
        return new AmethystMirrorRotateReplyPacket(dimension, pos, requestId, true, mirror.getStoredRotation());
    }
}
