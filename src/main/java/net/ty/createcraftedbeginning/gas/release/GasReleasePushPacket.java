package net.ty.createcraftedbeginning.gas.release;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.gas.flow.GasFlowMotion;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasReleasePushPacket(ResourceLocation dimension, Vec3 continuous, Vec3 burst) implements ClientboundPacketPayload {
    private static final StreamCodec<ByteBuf, Vec3> VECTOR_CODEC = StreamCodec.composite(ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);
    public static final StreamCodec<ByteBuf, GasReleasePushPacket> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC, GasReleasePushPacket::dimension, VECTOR_CODEC, GasReleasePushPacket::continuous, VECTOR_CODEC, GasReleasePushPacket::burst, GasReleasePushPacket::new);

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (!player.level().dimension().location().equals(dimension)) {
            return;
        }

        GasFlowMotion motion = new GasFlowMotion(continuous, burst);
        CoordinateTransform transform = SubLevelBridge.createEntityTransform(player);
        Vec3 movement = transform.transformNormal(player.getDeltaMovement());
        Vec3 impulse = motion.impulseFor(movement);
        Vec3 nextMovement = movement.add(impulse);
        player.setDeltaMovement(transform.inverseTransformNormal(nextMovement));
        player.hasImpulse = true;
        if (impulse.y > 0 && nextMovement.y > 0) {
            player.setOnGround(false);
        }

        if (!motion.isUpdraft(impulse) || nextMovement.y < 0) {
            return;
        }

        player.fallDistance = 0;
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.GAS_RELEASE_PUSH;
    }
}
